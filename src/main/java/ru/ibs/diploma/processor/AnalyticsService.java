package ru.ibs.diploma.processor;

import ru.ibs.diploma.data.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Выполняет агрегацию исходных записей и аналитические расчёты.
 */
public class AnalyticsService {
    private final Map<String, Long> populationByZip = new HashMap<>();
    private final Map<String, BigDecimal> finesByZip = new HashMap<>();
    private final Map<String, PropertyStats> propertiesByZip = new HashMap<>();
    private final Map<PropertyMetric, PropertyAverageStrategy> strategies;

    public AnalyticsService(LoadedData data, List<PropertyAverageStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(PropertyAverageStrategy::metric, Function.identity()));
        for (PopulationEntry row : data.getPopulationEntries()) {
            populationByZip.merge(row.getZipCode(), row.getPopulation(), Long::sum);
        }
        for (ParkingViolation row : data.getParkingViolations()) {
            if ("PA".equals(row.getState()) && row.getZipCode() != null && row.getFine() != null) {
                finesByZip.merge(row.getZipCode(), row.getFine(), BigDecimal::add);
            }
        }
        for (PropertyEntry row : data.getPropertyEntries()) {
            PropertyStats stats = new PropertyStats(
                    row.getMarketValue() == null ? BigDecimal.ZERO : row.getMarketValue(),
                    row.getMarketValue() == null ? 0 : 1,
                    row.getLivableArea() == null ? BigDecimal.ZERO : row.getLivableArea(),
                    row.getLivableArea() == null ? 0 : 1);
            propertiesByZip.merge(row.getZipCode(), stats, PropertyStats::merge);
        }
    }

    /**
     * Суммирует население по всем ZIP-кодам.
     */
    public long totalPopulation() {
        return populationByZip.values().stream().mapToLong(Long::longValue).sum();
    }

    /**
     * Рассчитывает сумму штрафов на одного жителя по каждому ZIP-коду.
     */
    public NavigableMap<String, BigDecimal> finesPerCapita() {
        TreeMap<String, BigDecimal> result = new TreeMap<>();
        finesByZip.forEach((zip, fine) -> {
            Long people = populationByZip.get(zip);
            if (people != null && people > 0 && fine.signum() != 0) {
                result.put(zip, fine.divide(BigDecimal.valueOf(people), 4, RoundingMode.DOWN));
            }
        });
        return result;
    }

    public BigDecimal averageProperty(String zip, PropertyMetric metric) {
        String n = ZipCode.normalize(zip);
        PropertyStats s = n == null ? null : propertiesByZip.get(n);
        return s == null ? BigDecimal.ZERO : strategies.get(metric).calculate(s);
    }
}
