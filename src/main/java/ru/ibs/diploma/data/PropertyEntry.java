package ru.ibs.diploma.data;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Неизменяемая запись с ZIP-кодом, рыночной стоимостью и жилой площадью объекта.
 */
public final class PropertyEntry {
    private final String zipCode;
    private final BigDecimal marketValue;
    private final BigDecimal livableArea;

    public PropertyEntry(String zipCode, BigDecimal marketValue, BigDecimal livableArea) {
        this.zipCode = zipCode;
        this.marketValue = marketValue;
        this.livableArea = livableArea;
    }

    public String getZipCode() {
        return zipCode;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public BigDecimal getLivableArea() {
        return livableArea;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PropertyEntry that)) {
            return false;
        }
        return Objects.equals(zipCode, that.zipCode)
                && Objects.equals(marketValue, that.marketValue)
                && Objects.equals(livableArea, that.livableArea);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zipCode, marketValue, livableArea);
    }

    @Override
    public String toString() {
        return "PropertyEntry{" + "zipCode='" + zipCode + '\''
                + ", marketValue=" + marketValue
                + ", livableArea=" + livableArea + '}';
    }
}
