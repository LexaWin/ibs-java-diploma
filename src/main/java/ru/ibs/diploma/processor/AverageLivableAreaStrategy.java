package ru.ibs.diploma.processor;

import org.springframework.stereotype.Component;
import ru.ibs.diploma.data.PropertyStats;

import java.math.BigDecimal;

/**
 * Стратегия расчёта среднего значения жилой площади по агрегированной статистике ZIP-кода.
 */
@Component
public class AverageLivableAreaStrategy implements PropertyAverageStrategy {

    /**
     * Указывает метрику, которую обслуживает эта стратегия.
     *
     * @return {@link PropertyMetric#LIVABLE_AREA}
     */
    @Override
    public PropertyMetric metric() {
        return PropertyMetric.LIVABLE_AREA;
    }

    /**
     * Возвращает среднюю жилую площадь из подготовленного агрегата.
     *
     * @param stats агрегированные значения недвижимости для ZIP-кода
     * @return средняя жилая площадь или ноль при отсутствии валидных значений
     */
    @Override
    public BigDecimal calculate(PropertyStats stats) {
        return stats.averageLivableArea();
    }
}
