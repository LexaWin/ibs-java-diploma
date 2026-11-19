package ru.ibs.diploma.data;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertyStatsTest {
    @Test
    void keepsIndependentCountsAndTruncatesTowardZero() {
        PropertyStats first = new PropertyStats(new BigDecimal("100.9"), 1, BigDecimal.ZERO, 0);
        PropertyStats second = new PropertyStats(BigDecimal.ZERO, 0, new BigDecimal("-5.8"), 1);
        PropertyStats merged = first.merge(second);
        assertEquals(1L, merged.getMarketValueCount());
        assertEquals(1L, merged.getLivableAreaCount());
        assertEquals("100", merged.averageMarketValue().toPlainString());
        assertEquals("-5", merged.averageLivableArea().toPlainString());
    }
}
