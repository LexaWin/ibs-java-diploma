package ru.ibs.diploma.processor;

import ru.ibs.diploma.data.PropertyStats;

import java.math.BigDecimal;

/**
 * Стратегия вычисления среднего значения одного поля недвижимости.
 */
public interface PropertyAverageStrategy {
    PropertyMetric metric();

    BigDecimal calculate(PropertyStats stats);
}
