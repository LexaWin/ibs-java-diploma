package ru.ibs.diploma.processor;

import org.springframework.stereotype.Component;
import ru.ibs.diploma.data.PropertyStats;

import java.math.BigDecimal;

/**
 * Рассчитывает среднюю жилую площадь недвижимости.
 */
@Component
public class AverageLivableAreaStrategy implements PropertyAverageStrategy {
    @Override
    public PropertyMetric metric() {
        return PropertyMetric.LIVABLE_AREA;
    }

    @Override
    public BigDecimal calculate(PropertyStats stats) {
        return stats.averageLivableArea();
    }
}
