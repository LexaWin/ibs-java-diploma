package ru.ibs.diploma.data;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Неизменяемая запись об объекте недвижимости.
 * <p>Рыночная стоимость и жилая площадь независимы: любое из значений может
 * отсутствовать, не исключая валидное значение другого показателя из анализа.
 */
public final class PropertyEntry {
    private final String zipCode;
    private final BigDecimal marketValue;
    private final BigDecimal livableArea;

    /**
     * Создаёт запись об объекте недвижимости.
     *
     * @param zipCode нормализованный ZIP-код объекта
     * @param marketValue рыночная стоимость или {@code null}, если значение невалидно/отсутствует
     * @param livableArea жилая площадь или {@code null}, если значение невалидно/отсутствует
     */
    public PropertyEntry(String zipCode, BigDecimal marketValue, BigDecimal livableArea) {
        this.zipCode = zipCode;
        this.marketValue = marketValue;
        this.livableArea = livableArea;
    }

    /** @return ZIP-код расположения объекта */
    public String getZipCode() {
        return zipCode;
    }

    /** @return рыночная стоимость либо {@code null} */
    public BigDecimal getMarketValue() {
        return marketValue;
    }

    /** @return общая жилая площадь либо {@code null} */
    public BigDecimal getLivableArea() {
        return livableArea;
    }

    /**
     * Сравнивает записи по ZIP-коду, рыночной стоимости и жилой площади.
     *
     * @param other объект для сравнения
     * @return {@code true}, если совпадают все поля записи
     */
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

    /** @return хеш-код, согласованный с {@link #equals(Object)} */
    @Override
    public int hashCode() {
        return Objects.hash(zipCode, marketValue, livableArea);
    }

    /** @return текстовое представление значений записи о недвижимости */
    @Override
    public String toString() {
        return "PropertyEntry{" + "zipCode='" + zipCode + '\''
                + ", marketValue=" + marketValue
                + ", livableArea=" + livableArea + '}';
    }
}
