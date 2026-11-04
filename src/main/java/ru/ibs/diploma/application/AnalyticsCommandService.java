package ru.ibs.diploma.application;

import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.data.PopulationEntry;
import ru.ibs.diploma.datamanagement.ParkingCsvReader;
import ru.ibs.diploma.datamanagement.PopulationTxtReader;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Выполняет аналитику, запрошенную пунктами консольного меню.
 */
public final class AnalyticsCommandService {
    private final CliConfig config;
    private final PopulationTxtReader populationReader;
    private final ParkingCsvReader parkingReader;

    public AnalyticsCommandService(CliConfig config, PopulationTxtReader populationReader, ParkingCsvReader parkingReader) {
        this.config = config;
        this.populationReader = populationReader;
        this.parkingReader = parkingReader;
    }

    /**
     * Пункт 1: читает записи и суммирует численность населения.
     */
    public long totalPopulation() throws IOException {
        return populationReader.read(config.getPopulationFile()).stream()
                .mapToLong(PopulationEntry::getPopulation)
                .sum();
    }

    /**
     * Пункт 2: агрегирует суммы штрафов и население по ZIP-кодам и вычисляет отношение.
     */
    public Map<String, BigDecimal> finesPerCapita() throws IOException {
        if (!"csv".equals(config.getParkingFormat())) {
            throw new IOException("JSON to be implemented...");
        }
        Map<String, Long> populationByZip = new HashMap<>();
        for (PopulationEntry entry : populationReader.read(config.getPopulationFile())) {
            populationByZip.merge(entry.getZipCode(), entry.getPopulation(), Long::sum);
        }

        Map<String, BigDecimal> finesByZip = new TreeMap<>();
        List<ParkingViolation> violations = parkingReader.read(config.getParkingFile());
        for (ParkingViolation row : violations) {
            if ("PA".equals(row.getState()) && row.getZipCode() != null && row.getFine() != null) {
                finesByZip.merge(row.getZipCode(), row.getFine(), BigDecimal::add);
            }
        }

        Map<String, BigDecimal> result = new TreeMap<>();
        finesByZip.forEach((zip, amount) -> {
            Long residents = populationByZip.get(zip);
            if (residents != null && residents > 0) {
                result.put(zip, amount.divide(BigDecimal.valueOf(residents), 4, RoundingMode.DOWN));
            }
        });
        return Map.copyOf(result);
    }
}
