package ru.ibs.diploma.processor;

import ru.ibs.diploma.data.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Выполняет агрегацию разобранных записей и расчёт аналитических показателей.
 * <p>Сервис строит индексы по ZIP-кодам при создании, применяет стратегии
 * расчёта средних значений недвижимости и использует {@link ComputationCache}
 * для повторного использования результатов.
 */
public class AnalyticsService {
    /** Множитель для представления частоты нарушений на 1 000 жителей. */
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1_000);
    /** Масштаб промежуточной точности десятичных показателей дополнительного анализа. */
    private static final int CALCULATION_SCALE = 12;

    private final Map<String, Long> populationByZip;
    private final Map<String, BigDecimal> finesByZip;
    private final Map<String, Long> parkingViolationCountByZip;
    private final Map<String, PropertyStats> propertiesByZip;
    private final Map<PropertyMetric, PropertyAverageStrategy> strategies;
    private final ComputationCache cache;

    /**
     * Строит агрегаты по ZIP-кодам для полученного снимка и подготавливает стратегии метрик.
     *
     * @param data разобранные записи парковки, недвижимости и населения
     * @param strategies стратегии вычисления средних характеристик недвижимости
     * @param cache общий кеш результатов
     */
    public AnalyticsService(LoadedData data, List<PropertyAverageStrategy> strategies, ComputationCache cache) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(PropertyAverageStrategy::metric, Function.identity()));
        this.cache = cache;
        this.populationByZip = aggregatePopulation(data.getPopulationEntries());
        this.propertiesByZip = aggregateProperties(data.getPropertyEntries());
        this.finesByZip = aggregateParkingFines(data.getParkingViolations());
        this.parkingViolationCountByZip = aggregateParkingViolationCounts(data.getParkingViolations());
    }

    /**
     * Возвращает число результатов, находящихся в общем кеше вычислений.
     *
     * @return количество сохранённых записей кеша
     */
    public int cachedResultCount() {
        return cache.size();
    }

    /**
     * Назначает ранги значениям, используя средний ранг для совпадающих значений.
     * Порядок рангов соответствует исходному порядку элементов списка.
     *
     * @param values значения, которые необходимо ранжировать
     * @return массив рангов в порядке исходного списка
     */
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

    /**
     * Суммирует численность населения из нескольких строк одного ZIP-кода.
     *
     * @param entries отдельные записи населения
     * @return неизменяемая карта ZIP-код → суммарное население
     */
    private static Map<String, Long> aggregatePopulation(List<PopulationEntry> entries) {
        Map<String, Long> result = new HashMap<>();
        for (PopulationEntry entry : entries) {
            if (entry.getZipCode() != null) {
                result.merge(entry.getZipCode(), entry.getPopulation(), Long::sum);
            }
        }
        return Map.copyOf(result);
    }

    /**
     * Собирает суммы и количества валидных значений недвижимости по ZIP-кодам.
     * Рыночная стоимость и жилая площадь учитываются независимыми счётчиками.
     *
     * @param entries разобранные записи объектов недвижимости
     * @return неизменяемая карта ZIP-код → агрегированная статистика
     */
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

    /**
     * Суммирует денежные штрафы по ZIP-кодам, учитывая только регистрацию PA.
     * Записи без ZIP-кода или валидной суммы штрафа не участвуют в сумме.
     *
     * @param violations все разобранные парковочные нарушения
     * @return неизменяемая карта ZIP-код → сумма штрафов
     */
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
     * Подсчитывает все зарегистрированные нарушения по ZIP-кодам независимо от штата регистрации.
     *
     * @param violations все разобранные парковочные нарушения
     * @return неизменяемая карта ZIP-код → число нарушений
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

    /**
     * Делегирует нормализацию ZIP-кода общей предметной утилите.
     *
     * @param zip исходное значение ZIP-кода
     * @return пятизначный ZIP-код либо {@code null}
     */
    private static String normalizeZip(String zip) {
        return ZipCode.normalize(zip);
    }

    /**
     * Рассчитывает корреляцию Пирсона между средними рангами двух показателей (Спирмен).
     * Возвращает пустой результат при выборке менее чем из двух точек либо при нулевой
     * дисперсии хотя бы одного из наборов рангов.
     *
     * @param rows показатели по ZIP-кодам, отсортированные по ZIP
     * @return коэффициент в диапазоне [-1; 1], округлённый до четырёх знаков, либо пустое значение
     */
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
     * Суммирует численность населения по всем ZIP-кодам.
     *
     * @return общее население в загруженных данных
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
     * Делит суммарный объём штрафов штата PA по каждому ZIP-коду на его население.
     * Результат округляется до четырёх знаков после запятой в сторону нуля;
     * ZIP-коды без штрафов, населения или с нулевым населением пропускаются.
     *
     * @return неизменяемая карта ZIP-код → штрафы на одного жителя
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
     * Рассчитывает среднее значение выбранного показателя по агрегату ZIP-кода.
     * Если ZIP некорректен или соответствующих записей нет, возвращается ноль.
     *
     * @param zip исходный ZIP-код
     * @param metric показатель недвижимости для расчёта среднего
     * @return средняя стоимость или жилая площадь согласно выбранной стратегии
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
     * Делит сумму валидных рыночных стоимостей объектов ZIP-кода на его население.
     * Результат округляется до целого в сторону нуля; при отсутствии данных или нулевом
     * населении возвращается ноль.
     *
     * @param zip исходный ZIP-код
     * @return совокупная рыночная стоимость на одного жителя
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
     * Возвращает таблицу частоты нарушений на 1 000 жителей и средней стоимости жилья,
     * а также коэффициент ранговой корреляции Спирмена по ZIP-кодам.
     *
     * @return результат дополнительного анализа; вычисление кешируется
     */
    public AdditionalFeatureResult additionalFeature() {
        return cache.get("feature:violations-market-value-spearman:", this::calculateAdditionalFeature);
    }

    /**
     * Формирует строки для ZIP-кодов с положительным населением и хотя бы одной валидной
     * рыночной стоимостью. При отсутствии строк нарушений в таком ZIP используется ноль.
     * Оба показателя рассчитываются с масштабом {@code CALCULATION_SCALE}.
     *
     * @return результат по ZIP-кодам с рассчитанной корреляцией
     */
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
