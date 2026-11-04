package ru.ibs.diploma.processor;

import ru.ibs.diploma.data.LoadedData;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.data.PopulationEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Выполняет группировку населения и штрафов по ZIP-кодам и расчёты первых двух команд.
 */
public class AnalyticsService {
    private final Map<String, Long> populationByZip = new HashMap<>();
    private final Map<String, BigDecimal> finesByZip = new HashMap<>();

    public AnalyticsService(LoadedData data) {
        for (PopulationEntry entry : data.getPopulationEntries()) {
            populationByZip.merge(entry.getZipCode(), entry.getPopulation(), Long::sum);
        }
        for (ParkingViolation violation : data.getParkingViolations()) {
            if ("PA".equals(violation.getState()) && violation.getZipCode() != null
                    && violation.getFine() != null) {
                finesByZip.merge(violation.getZipCode(), violation.getFine(), BigDecimal::add);
            }
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
        finesByZip.forEach((zip, fines) -> {
            Long population = populationByZip.get(zip);
            if (population != null && population > 0 && fines.signum() != 0) {
                result.put(zip, fines.divide(BigDecimal.valueOf(population), 4, RoundingMode.DOWN));
            }
        });
        return result;
    }
}
