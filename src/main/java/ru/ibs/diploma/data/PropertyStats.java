package ru.ibs.diploma.data;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Агрегированные суммы и количества записей недвижимости по ZIP-коду для аналитических расчётов.
 */
public final class PropertyStats {
    private final BigDecimal marketValueSum;
    private final long marketValueCount;
    private final BigDecimal livableAreaSum;
    private final long livableAreaCount;

    public PropertyStats(BigDecimal marketValueSum, long marketValueCount,
                         BigDecimal livableAreaSum, long livableAreaCount) {
        this.marketValueSum = marketValueSum == null ? BigDecimal.ZERO : marketValueSum;
        this.marketValueCount = marketValueCount;
        this.livableAreaSum = livableAreaSum == null ? BigDecimal.ZERO : livableAreaSum;
        this.livableAreaCount = livableAreaCount;
    }

    public static PropertyStats empty() {
        return new PropertyStats(BigDecimal.ZERO, 0, BigDecimal.ZERO, 0);
    }

    public BigDecimal getMarketValueSum() {
        return marketValueSum;
    }

    public long getMarketValueCount() {
        return marketValueCount;
    }

    public BigDecimal getLivableAreaSum() {
        return livableAreaSum;
    }

    public long getLivableAreaCount() {
        return livableAreaCount;
    }

    public PropertyStats merge(PropertyStats other) {
        return new PropertyStats(marketValueSum.add(other.marketValueSum),
                marketValueCount + other.marketValueCount,
                livableAreaSum.add(other.livableAreaSum), livableAreaCount + other.livableAreaCount);
    }

    public BigDecimal averageMarketValue() {
        return marketValueCount == 0 ? BigDecimal.ZERO
                : marketValueSum.divide(BigDecimal.valueOf(marketValueCount), 0, RoundingMode.DOWN);
    }

    public BigDecimal averageLivableArea() {
        return livableAreaCount == 0 ? BigDecimal.ZERO
                : livableAreaSum.divide(BigDecimal.valueOf(livableAreaCount), 0, RoundingMode.DOWN);
    }
}
