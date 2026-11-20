package ru.ibs.diploma.data;

import java.math.BigDecimal;
import java.util.*;

/**
 * Неизменяемый результат сопоставления частоты парковочных нарушений
 * и средней рыночной стоимости недвижимости по ZIP-кодам.
 * <p>Данные хранятся в отсортированной по ZIP-коду карте. Коэффициент Спирмена
 * отсутствует, если его невозможно вычислить для полученной выборки.
 */
public final class AdditionalFeatureResult {
    private final NavigableMap<String, ZipMetrics> byZip;
    private final Optional<BigDecimal> spearmanCorrelation;

    /**
     * Создаёт результат дополнительного анализа и формирует неизменяемую карту по ZIP-кодам.
     *
     * @param byZip рассчитанные метрики; порядок входной карты не влияет на порядок результата
     * @param spearmanCorrelation коэффициент Спирмена либо {@link Optional#empty()}, если он не определён
     */
    public AdditionalFeatureResult(Map<String, ZipMetrics> byZip,
                                   Optional<BigDecimal> spearmanCorrelation) {
        this.byZip = Collections.unmodifiableNavigableMap(new TreeMap<>(byZip));
        this.spearmanCorrelation = spearmanCorrelation == null
                ? Optional.empty() : spearmanCorrelation;
    }

    /**
     * Возвращает метрики в порядке возрастания ZIP-кодов.
     *
     * @return неизменяемая отсортированная карта ZIP-код → метрики
     */
    public NavigableMap<String, ZipMetrics> getByZip() {
        return byZip;
    }

    /**
     * Возвращает коэффициент ранговой корреляции Спирмена, если он определён.
     *
     * @return {@link Optional} со значением коэффициента или пустой результат
     */
    public Optional<BigDecimal> getSpearmanCorrelation() {
        return spearmanCorrelation;
    }

    /**
     * Возвращает число ZIP-кодов, включённых в набор для корреляционного анализа.
     *
     * @return количество строк метрик
     */
    public int getSampleSize() {
        return byZip.size();
    }

    /**
     * Пара аналитических показателей, рассчитанных для одного ZIP-кода.
     * Используется как строка результата дополнительного анализа.
     */
    public static final class ZipMetrics {
        private final BigDecimal violationsPer1000Residents;
        private final BigDecimal averageMarketValue;

        /**
         * Создаёт пару показателей для одного ZIP-кода.
         *
         * @param violationsPer1000Residents число нарушений на 1 000 жителей
         * @param averageMarketValue средняя рыночная стоимость недвижимости в USD
         * @throws IllegalArgumentException если один из показателей равен {@code null}
         */
        public ZipMetrics(BigDecimal violationsPer1000Residents,
                          BigDecimal averageMarketValue) {
            if (violationsPer1000Residents == null || averageMarketValue == null) {
                throw new IllegalArgumentException("Значения аналитических показателей не должны быть null");
            }
            this.violationsPer1000Residents = violationsPer1000Residents;
            this.averageMarketValue = averageMarketValue;
        }

        /** @return количество зарегистрированных парковочных нарушений на 1 000 жителей */
        public BigDecimal getViolationsPer1000Residents() {
            return violationsPer1000Residents;
        }

        /** @return средняя рыночная стоимость недвижимости в USD */
        public BigDecimal getAverageMarketValue() {
            return averageMarketValue;
        }
    }
}
