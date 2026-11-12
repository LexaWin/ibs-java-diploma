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

    private final Map<String, Long> populationByZip = new HashMap<>();
    private final Map<String, BigDecimal> finesByZip = new HashMap<>();
    private final Map<String, Long> parkingViolationCountByZip = new HashMap<>();
    private final Map<String, PropertyStats> propertiesByZip = new HashMap<>();
    private final Map<PropertyMetric, PropertyAverageStrategy> strategies;

    public AnalyticsService(LoadedData data, List<PropertyAverageStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(PropertyAverageStrategy::metric, Function.identity()));
        for (PopulationEntry row : data.getPopulationEntries()) {
            populationByZip.merge(row.getZipCode(), row.getPopulation(), Long::sum);
        }
        for (ParkingViolation row : data.getParkingViolations()) {
            if (row.getZipCode() == null) continue;

            parkingViolationCountByZip.merge(row.getZipCode(), 1L, Long::sum);

            if ("PA".equals(row.getState()) && row.getFine() != null) {
                finesByZip.merge(row.getZipCode(), row.getFine(), BigDecimal::add);
            }
        }
        for (PropertyEntry row : data.getPropertyEntries()) {
            PropertyStats stats = new PropertyStats(
                    row.getMarketValue() == null ? BigDecimal.ZERO : row.getMarketValue(),
                    row.getMarketValue() == null ? 0 : 1,
                    row.getLivableArea() == null ? BigDecimal.ZERO : row.getLivableArea(),
                    row.getLivableArea() == null ? 0 : 1);
            propertiesByZip.merge(row.getZipCode(), stats, PropertyStats::merge);
        }
    }

    /**
     * Суммирует население по всем ZIP-кодам.
     */
    public long totalPopulation() {
        return populationByZip.values().stream().mapToLong(Long::longValue).sum();
    }

    /**
     * Рассчитывает сумму штрафов на одного жителя по каждому ZIP-коду.
     */
    public NavigableMap<String, BigDecimal> finesPerCapita() {
        TreeMap<String, BigDecimal> result = new TreeMap<>();
        finesByZip.forEach((zip, fine) -> {
            Long people = populationByZip.get(zip);
            if (people != null && people > 0 && fine.signum() != 0) {
                result.put(zip, fine.divide(BigDecimal.valueOf(people), 4, RoundingMode.DOWN));
            }
        });
        return result;
    }

    public BigDecimal averageProperty(String zip, PropertyMetric metric) {
        String n = ZipCode.normalize(zip);
        PropertyStats s = n == null ? null : propertiesByZip.get(n);
        return s == null ? BigDecimal.ZERO : strategies.get(metric).calculate(s);
    }

    public BigDecimal marketValuePerCapita(String zip) {
        String n = ZipCode.normalize(zip);
        if (n == null)
            return BigDecimal.ZERO;
        PropertyStats s = propertiesByZip.get(n);
        Long p = populationByZip.get(n);
        if (s == null || p == null || p == 0 || s.getMarketValueSum().signum() == 0)
            return BigDecimal.ZERO;
        return s.getMarketValueSum().divide(BigDecimal.valueOf(p), 0, RoundingMode.DOWN);
    }

    /**
     * Сопоставляет частоту парковочных нарушений на 1 000 жителей со средней рыночной
     * стоимостью жилой недвижимости по ZIP-кодам и рассчитывает корреляцию Спирмена.
     * Учитываются нарушения с известным ZIP независимо от штата регистрации автомобиля.
     * Для ZIP без записей о нарушениях используется нулевая частота.
     */
    public AdditionalFeatureResult additionalFeature() {
        TreeMap<String, AdditionalFeatureResult.ZipMetrics> rows = new TreeMap<>();
        populationByZip.forEach((zip, population) -> {
            PropertyStats propertyStats = propertiesByZip.get(zip);
            if (population == null || population <= 0 || propertyStats == null
                    || propertyStats.getMarketValueCount() == 0) return;

            long violations = parkingViolationCountByZip.getOrDefault(zip, 0L);
            BigDecimal rate = BigDecimal.valueOf(violations).multiply(THOUSAND)
                    .divide(BigDecimal.valueOf(population), CALCULATION_SCALE, RoundingMode.HALF_UP);
            BigDecimal averageMarketValue = propertyStats.getMarketValueSum()
                    .divide(BigDecimal.valueOf(propertyStats.getMarketValueCount()),
                            CALCULATION_SCALE, RoundingMode.HALF_UP);
            rows.put(zip, new AdditionalFeatureResult.ZipMetrics(rate, averageMarketValue));
        });
        return new AdditionalFeatureResult(rows, calculateSpearmanCorrelation(rows));
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
}
