package ru.ibs.diploma.data;

import java.math.BigDecimal;
import java.util.*;

/**
 * Результат анализа частоты парковочных нарушений и стоимости жилья по ZIP-кодам.
 */
public final class AdditionalFeatureResult {
    private final NavigableMap<String, ZipMetrics> byZip;
    private final Optional<BigDecimal> spearmanCorrelation;

    public AdditionalFeatureResult(Map<String, ZipMetrics> byZip,
                                   Optional<BigDecimal> spearmanCorrelation) {
        this.byZip = Collections.unmodifiableNavigableMap(new TreeMap<>(byZip));
        this.spearmanCorrelation = spearmanCorrelation == null
                ? Optional.empty() : spearmanCorrelation;
    }

    /**
     * Возвращает метрики ZIP-кодов в порядке возрастания ZIP.
     */
    public NavigableMap<String, ZipMetrics> getByZip() {
        return byZip;
    }

    /**
     * Возвращает коэффициент Спирмена или пустое значение, если он не определён.
     */
    public Optional<BigDecimal> getSpearmanCorrelation() {
        return spearmanCorrelation;
    }

    /**
     * Возвращает количество ZIP-кодов, вошедших в корреляционный анализ.
     */
    public int getSampleSize() {
        return byZip.size();
    }

    /**
     * Два сопоставляемых показателя для одного ZIP-кода.
     */
    public static final class ZipMetrics {
        private final BigDecimal violationsPer1000Residents;
        private final BigDecimal averageMarketValue;

        public ZipMetrics(BigDecimal violationsPer1000Residents,
                          BigDecimal averageMarketValue) {
            if (violationsPer1000Residents == null || averageMarketValue == null) {
                throw new IllegalArgumentException("Значения аналитических показателей не должны быть null");
            }
            this.violationsPer1000Residents = violationsPer1000Residents;
            this.averageMarketValue = averageMarketValue;
        }

        /**
         * Количество зарегистрированных нарушений на 1 000 жителей.
         */
        public BigDecimal getViolationsPer1000Residents() {
            return violationsPer1000Residents;
        }

        /**
         * Средняя рыночная стоимость объекта жилой недвижимости в USD.
         */
        public BigDecimal getAverageMarketValue() {
            return averageMarketValue;
        }
    }
}
