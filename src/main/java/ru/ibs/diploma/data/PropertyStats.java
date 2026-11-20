package ru.ibs.diploma.data;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Агрегированные суммы и количества валидных значений недвижимости для ZIP-кода.
 * <p>Отдельные счётчики позволяют рассчитывать среднюю стоимость и среднюю
 * жилую площадь по разным наборам записей, не включая отсутствующие значения.
 */
public final class PropertyStats {
    private final BigDecimal marketValueSum;
    private final long marketValueCount;
    private final BigDecimal livableAreaSum;
    private final long livableAreaCount;

    /**
     * Создаёт агрегат сумм и количеств валидных значений.
     * Нулевые суммы используются вместо переданных {@code null}; счётчики задаются отдельно.
     *
     * @param marketValueSum сумма рыночных стоимостей
     * @param marketValueCount количество учтённых рыночных стоимостей
     * @param livableAreaSum сумма жилых площадей
     * @param livableAreaCount количество учтённых жилых площадей
     */
    public PropertyStats(BigDecimal marketValueSum, long marketValueCount,
                         BigDecimal livableAreaSum, long livableAreaCount) {
        this.marketValueSum = marketValueSum == null ? BigDecimal.ZERO : marketValueSum;
        this.marketValueCount = marketValueCount;
        this.livableAreaSum = livableAreaSum == null ? BigDecimal.ZERO : livableAreaSum;
        this.livableAreaCount = livableAreaCount;
    }

    /**
     * Создаёт пустой агрегат без учтённых значений.
     *
     * @return агрегат с нулевыми суммами и счётчиками
     */
    public static PropertyStats empty() {
        return new PropertyStats(BigDecimal.ZERO, 0, BigDecimal.ZERO, 0);
    }

    /** @return сумма валидных рыночных стоимостей */
    public BigDecimal getMarketValueSum() {
        return marketValueSum;
    }

    /** @return количество валидных рыночных стоимостей */
    public long getMarketValueCount() {
        return marketValueCount;
    }

    /** @return сумма валидных значений жилой площади */
    public BigDecimal getLivableAreaSum() {
        return livableAreaSum;
    }

    /** @return количество валидных значений жилой площади */
    public long getLivableAreaCount() {
        return livableAreaCount;
    }

    /**
     * Объединяет агрегаты, складывая независимые суммы и счётчики.
     *
     * @param other второй агрегат
     * @return новый агрегат, содержащий суммы обоих объектов
     */
    public PropertyStats merge(PropertyStats other) {
        return new PropertyStats(marketValueSum.add(other.marketValueSum),
                marketValueCount + other.marketValueCount,
                livableAreaSum.add(other.livableAreaSum), livableAreaCount + other.livableAreaCount);
    }

    /**
     * Вычисляет среднюю рыночную стоимость, отбрасывая дробную часть до целого вниз к нулю.
     *
     * @return средняя стоимость либо {@link BigDecimal#ZERO}, если валидных значений нет
     */
    public BigDecimal averageMarketValue() {
        return marketValueCount == 0 ? BigDecimal.ZERO
                : marketValueSum.divide(BigDecimal.valueOf(marketValueCount), 0, RoundingMode.DOWN);
    }

    /**
     * Вычисляет среднюю жилую площадь, отбрасывая дробную часть до целого вниз к нулю.
     *
     * @return средняя площадь либо {@link BigDecimal#ZERO}, если валидных значений нет
     */
    public BigDecimal averageLivableArea() {
        return livableAreaCount == 0 ? BigDecimal.ZERO
                : livableAreaSum.divide(BigDecimal.valueOf(livableAreaCount), 0, RoundingMode.DOWN);
    }
}
