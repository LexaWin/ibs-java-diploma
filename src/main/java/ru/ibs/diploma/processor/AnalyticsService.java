package ru.ibs.diploma.processor;

import ru.ibs.diploma.data.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Выполняет агрегацию исходных записей и аналитические расчёты.
 */
public class AnalyticsService {
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1_000);
    private static final int CALCULATION_SCALE = 12;

    private final Map<String, Long> populationByZip;
    private final Map<String, BigDecimal> finesByZip;
    private final Map<String, Long> parkingViolationCountByZip;
    private final Map<String, PropertyStats> propertiesByZip;
    private final Map<PropertyMetric, PropertyAverageStrategy> strategies;
    private final ComputationCache cache;

    public AnalyticsService(LoadedData data, List<PropertyAverageStrategy> strategies, ComputationCache cache) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(PropertyAverageStrategy::metric, Function.identity()));
        this.cache = cache;
        this.populationByZip = aggregatePopulation(data.getPopulationEntries());
        this.propertiesByZip = aggregateProperties(data.getPropertyEntries());
        this.finesByZip = aggregateParkingFines(data.getParkingViolations());
        this.parkingViolationCountByZip = aggregateParkingViolationCounts(data.getParkingViolations());
    }

    private static double[] averageRanks(List<BigDecimal> values) {
        Integer[] order = new Integer[values.size()];
        for (int index = 0; index < order.length; index++) order[index] = index;
        Arrays.sort(order, Comparator.comparing(values::get));
        double[] ranks = new double[values.size()];
        int start = 0;
        while (start < order.length) {
            int end = start + 1;
            BigDecimal current = values.get(order[start]);
            while (end < order.length && current.compareTo(values.get(order[end])) == 0) end++;
            double rank = ((start + 1) + end) / 2.0;
            for (int index = start; index < end; index++) ranks[order[index]] = rank;
            start = end;
        }
        return ranks;
    }

    private static Map<String, Long> aggregatePopulation(List<PopulationEntry> entries) {
        Map<String, Long> result = new HashMap<>();
        for (PopulationEntry entry : entries) {
            if (entry.getZipCode() != null) {
                result.merge(entry.getZipCode(), entry.getPopulation(), Long::sum);
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, PropertyStats> aggregateProperties(List<PropertyEntry> entries) {
        Map<String, PropertyStats> result = new HashMap<>();
        for (PropertyEntry entry : entries) {
            PropertyStats stats = new PropertyStats(
                    entry.getMarketValue() == null ? BigDecimal.ZERO : entry.getMarketValue(),
                    entry.getMarketValue() == null ? 0 : 1,
                    entry.getLivableArea() == null ? BigDecimal.ZERO : entry.getLivableArea(),
                    entry.getLivableArea() == null ? 0 : 1);
            result.merge(entry.getZipCode(), stats, PropertyStats::merge);
        }
        return Map.copyOf(result);
    }

    private static Map<String, BigDecimal> aggregateParkingFines(List<ParkingViolation> violations) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (ParkingViolation violation : violations) {
            if ("PA".equals(violation.getState()) && violation.getZipCode() != null
                    && violation.getFine() != null) {
                result.merge(violation.getZipCode(), violation.getFine(), BigDecimal::add);
            }
        }
        return Map.copyOf(result);
    }

    /**
     * Считает все зарегистрированные нарушения по ZIP, независимо от штата регистрации автомобиля.
     */
    private static Map<String, Long> aggregateParkingViolationCounts(List<ParkingViolation> violations) {
        Map<String, Long> result = new HashMap<>();
        for (ParkingViolation violation : violations) {
            if (violation.getZipCode() != null) {
                result.merge(violation.getZipCode(), 1L, Long::sum);
            }
        }
        return Map.copyOf(result);
    }

    private static String normalizeZip(String zip) {
        return ZipCode.normalize(zip);
    }

    private static Optional<BigDecimal> calculateSpearmanCorrelation(
            NavigableMap<String, AdditionalFeatureResult.ZipMetrics> rows) {
        int size = rows.size();
        if (size < 2) return Optional.empty();
        List<AdditionalFeatureResult.ZipMetrics> metrics = new ArrayList<>(rows.values());
        List<BigDecimal> violationRates = metrics.stream()
                .map(AdditionalFeatureResult.ZipMetrics::getViolationsPer1000Residents).toList();
        List<BigDecimal> marketValues = metrics.stream()
                .map(AdditionalFeatureResult.ZipMetrics::getAverageMarketValue).toList();
        double[] violationRanks = averageRanks(violationRates);
        double[] marketRanks = averageRanks(marketValues);
        double mean = (size + 1) / 2.0;
        double covariance = 0.0;
        double violationVariance = 0.0;
        double marketVariance = 0.0;
        for (int index = 0; index < size; index++) {
            double x = violationRanks[index] - mean;
            double y = marketRanks[index] - mean;
            covariance += x * y;
            violationVariance += x * x;
            marketVariance += y * y;
        }
        if (violationVariance == 0.0 || marketVariance == 0.0) return Optional.empty();
        double coefficient = covariance / Math.sqrt(violationVariance * marketVariance);
        coefficient = Math.max(-1.0, Math.min(1.0, coefficient));
        return Optional.of(BigDecimal.valueOf(coefficient).setScale(4, RoundingMode.HALF_UP));
    }

    /**
     * Суммирует население по всем ZIP-кодам.
     */
    public long totalPopulation() {
        return cache.get(
                "population:total",
                () -> populationByZip
                        .values()
                        .stream()
                        .mapToLong(Long::longValue)
                        .sum()
        );
    }

    /**
     * Рассчитывает сумму штрафов на одного жителя по каждому ZIP-коду.
     */
    public NavigableMap<String, BigDecimal> finesPerCapita() {
        return cache.get("parking:fines-per-capita", () -> {
            TreeMap<String, BigDecimal> result = new TreeMap<>();
            finesByZip.forEach((zip, fine) -> {
                Long population = populationByZip.get(zip);
                if (fine.signum() == 0 || population == null || population == 0) {
                    return;
                }
                result.put(zip, fine.divide(BigDecimal.valueOf(population), 4, RoundingMode.DOWN));
            });
            return Collections.unmodifiableNavigableMap(result);
        });
    }

    /**
     * Рассчитывает среднее значение выбранного показателя для ZIP-кода.
     */
    public BigDecimal averageProperty(String zip, PropertyMetric metric) {
        String normalizedZip = normalizeZip(zip);
        return cache.get("property:average:" + metric + ":" + normalizedZip, () -> {
            if (normalizedZip == null) return BigDecimal.ZERO;
            PropertyStats stats = propertiesByZip.get(normalizedZip);
            return stats == null ? BigDecimal.ZERO : strategies.get(metric).calculate(stats);
        });
    }

    /**
     * Делит сумму рыночных стоимостей недвижимости ZIP-кода на численность его населения.
     */
    public BigDecimal marketValuePerCapita(String zip) {
        String normalizedZip = normalizeZip(zip);
        return cache.get("property:market-per-capita:" + normalizedZip, () -> {
            if (normalizedZip == null) return BigDecimal.ZERO;
            PropertyStats stats = propertiesByZip.get(normalizedZip);
            Long population = populationByZip.get(normalizedZip);
            if (stats == null || population == null || population == 0
                    || stats.getMarketValueSum().signum() == 0) return BigDecimal.ZERO;
            return stats.getMarketValueSum().divide(BigDecimal.valueOf(population), 0, RoundingMode.DOWN);
        });
    }

    /**
     * Сопоставляет частоту парковочных нарушений на 1 000 жителей со средней рыночной
     * стоимостью жилой недвижимости по ZIP-кодам и рассчитывает корреляцию Спирмена.
     * Учитываются нарушения с известным ZIP независимо от штата регистрации автомобиля.
     * Для ZIP без записей о нарушениях используется нулевая частота.
     */
    public AdditionalFeatureResult additionalFeature() {
        return cache.get("feature:violations-market-value-spearman:", this::calculateAdditionalFeature);
    }

    private AdditionalFeatureResult calculateAdditionalFeature() {
        TreeMap<String, AdditionalFeatureResult.ZipMetrics> rows = new TreeMap<>();

        // Используется пересечение ZIP-кодов с положительным населением и валидной стоимостью жилья.
        // Отсутствие нарушений в полном входном наборе для такого ZIP трактуется как нулевое количество.
        populationByZip.forEach((zip, population) -> {
            PropertyStats propertyStats = propertiesByZip.get(zip);
            if (population == null || population <= 0 || propertyStats == null
                    || propertyStats.getMarketValueCount() == 0) {
                return;
            }

            long violationCount = parkingViolationCountByZip.getOrDefault(zip, 0L);
            BigDecimal violationsPerThousand = BigDecimal.valueOf(violationCount)
                    .multiply(THOUSAND)
                    .divide(BigDecimal.valueOf(population), CALCULATION_SCALE, RoundingMode.HALF_UP);
            BigDecimal averageMarketValue = propertyStats.getMarketValueSum()
                    .divide(BigDecimal.valueOf(propertyStats.getMarketValueCount()),
                            CALCULATION_SCALE, RoundingMode.HALF_UP);

            rows.put(zip, new AdditionalFeatureResult.ZipMetrics(
                    violationsPerThousand, averageMarketValue));
        });

        return new AdditionalFeatureResult(rows, calculateSpearmanCorrelation(rows));
    }
}
