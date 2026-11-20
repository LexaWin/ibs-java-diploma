package ru.ibs.diploma.processor;

import org.springframework.stereotype.Component;
import ru.ibs.diploma.data.PropertyStats;

import java.math.BigDecimal;

/**
 * Стратегия расчёта средней рыночной стоимости недвижимости по агрегированной статистике ZIP-кода.
 */
@Component
public class AverageMarketValueStrategy implements PropertyAverageStrategy {

    /**
     * Указывает метрику, которую обслуживает эта стратегия.
     *
     * @return {@link PropertyMetric#MARKET_VALUE}
     */
    @Override
    public PropertyMetric metric() {
        return PropertyMetric.MARKET_VALUE;
    }

    /**
     * Возвращает среднюю рыночную стоимость из подготовленного агрегата.
     *
     * @param stats агрегированные значения недвижимости для ZIP-кода
     * @return средняя рыночная стоимость или ноль при отсутствии валидных значений
     */
    @Override
    public BigDecimal calculate(PropertyStats stats) {
        return stats.averageMarketValue();
    }
}
