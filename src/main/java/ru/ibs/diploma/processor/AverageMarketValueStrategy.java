package ru.ibs.diploma.processor;

import org.springframework.stereotype.Component;
import ru.ibs.diploma.data.PropertyStats;

import java.math.BigDecimal;

/**
 * Рассчитывает среднюю рыночную стоимость недвижимости.
 */
@Component
public class AverageMarketValueStrategy implements PropertyAverageStrategy {
    @Override
    public PropertyMetric metric() {
        return PropertyMetric.MARKET_VALUE;
    }

    @Override
    public BigDecimal calculate(PropertyStats stats) {
        return stats.averageMarketValue();
    }
}
