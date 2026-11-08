package ru.ibs.diploma.processor;

import ru.ibs.diploma.data.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Выполняет агрегацию записей и бизнес-расчёты; reader-ы не вычисляют показатели.
 */
public class AnalyticsService {
    private final Map<String, Long> populationByZip = new HashMap<>();
    private final Map<String, BigDecimal> finesByZip = new HashMap<>();
    private final Map<String, PropertyStats> propertiesByZip = new HashMap<>();

    public AnalyticsService(LoadedData data) {
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
                    row.getMarketValue() == null ? BigDecimal.ZERO : row.getMarketValue(), row.getMarketValue() == null ? 0 : 1,
                    row.getLivableArea() == null ? BigDecimal.ZERO : row.getLivableArea(), row.getLivableArea() == null ? 0 : 1);
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

    /**
     * Рассчитывает среднюю рыночную стоимость объектов жилой недвижимости по ZIP-коду.
     */
    public BigDecimal averageMarketValue(String zip) {
        String normalized = ZipCode.normalize(zip);
        PropertyStats stats = normalized == null ? null : propertiesByZip.get(normalized);
        return stats == null ? BigDecimal.ZERO : stats.averageMarketValue();
    }
}
