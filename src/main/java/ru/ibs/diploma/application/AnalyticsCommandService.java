package ru.ibs.diploma.application;

import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.data.PopulationEntry;
import ru.ibs.diploma.datamanagement.ParkingCsvReader;
import ru.ibs.diploma.datamanagement.ParkingJsonReader;
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
    private final ParkingCsvReader csvReader;
    private final ParkingJsonReader jsonReader;

    public AnalyticsCommandService(CliConfig config, PopulationTxtReader populationReader,
                                   ParkingCsvReader csvReader, ParkingJsonReader jsonReader) {
        this.config = config;
        this.populationReader = populationReader;
        this.csvReader = csvReader;
        this.jsonReader = jsonReader;
    }

    public long totalPopulation() throws IOException {
        return populationReader.read(config.getPopulationFile()).stream()
                .mapToLong(PopulationEntry::getPopulation).sum();
    }

    public Map<String, BigDecimal> finesPerCapita() throws IOException {
        Map<String, Long> populationByZip = new HashMap<>();
        for (PopulationEntry entry : populationReader.read(config.getPopulationFile())) {
            populationByZip.merge(entry.getZipCode(), entry.getPopulation(), Long::sum);
        }
        List<ParkingViolation> parking = "csv".equals(config.getParkingFormat())
                ? csvReader.read(config.getParkingFile()) : jsonReader.read(config.getParkingFile());
        Map<String, BigDecimal> finesByZip = new TreeMap<>();
        for (ParkingViolation row : parking) {
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
