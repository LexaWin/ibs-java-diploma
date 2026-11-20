package ru.ibs.diploma.processor;

import ru.ibs.diploma.data.PropertyStats;

import java.math.BigDecimal;

/**
 * Стратегия вычисления среднего значения одного из показателей недвижимости.
 * Реализация связывает тип метрики с формулой вычисления по {@link PropertyStats}.
 */
public interface PropertyAverageStrategy {
    /**
     * Возвращает идентификатор показателя, для которого предназначена стратегия.
     *
     * @return тип метрики недвижимости
     */
    PropertyMetric metric();

    /**
     * Рассчитывает среднее значение показателя из агрегированной статистики.
     *
     * @param stats суммы и количества валидных значений для ZIP-кода
     * @return среднее значение, определяемое конкретной реализацией стратегии
     */
    BigDecimal calculate(PropertyStats stats);
}
