package ru.ibs.diploma.application;

import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.data.LoadedData;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.data.PopulationEntry;
import ru.ibs.diploma.datamanagement.ParkingCsvReader;
import ru.ibs.diploma.datamanagement.ParkingJsonReader;
import ru.ibs.diploma.datamanagement.PopulationTxtReader;
import ru.ibs.diploma.processor.AnalyticsService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.NavigableMap;

/**
 * Загружает необходимые записи и делегирует расчёты аналитическому сервису.
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
        LoadedData data = new LoadedData(List.of(), populationReader.read(config.getPopulationFile()));
        return new AnalyticsService(data).totalPopulation();
    }

    public NavigableMap<String, BigDecimal> finesPerCapita() throws IOException {
        List<PopulationEntry> population = populationReader.read(config.getPopulationFile());
        List<ParkingViolation> parking = "csv".equals(config.getParkingFormat())
                ? csvReader.read(config.getParkingFile()) : jsonReader.read(config.getParkingFile());
        return new AnalyticsService(new LoadedData(parking, population)).finesPerCapita();
    }
}
