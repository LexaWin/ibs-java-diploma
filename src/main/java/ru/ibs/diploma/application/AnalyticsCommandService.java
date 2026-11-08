package ru.ibs.diploma.application;

import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.data.LoadedData;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.data.PopulationEntry;
import ru.ibs.diploma.data.PropertyEntry;
import ru.ibs.diploma.datamanagement.ParkingCsvReader;
import ru.ibs.diploma.datamanagement.ParkingJsonReader;
import ru.ibs.diploma.datamanagement.PopulationTxtReader;
import ru.ibs.diploma.datamanagement.PropertyCsvReader;
import ru.ibs.diploma.processor.AnalyticsService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Загружает необходимые записи и делегирует расчёты аналитическому сервису.
 */
public final class AnalyticsCommandService {
    private final CliConfig config;
    private final PopulationTxtReader populationReader;
    private final ParkingCsvReader csvReader;
    private final ParkingJsonReader jsonReader;
    private final PropertyCsvReader propertyReader;

    public AnalyticsCommandService(CliConfig config, PopulationTxtReader populationReader,
                                   ParkingCsvReader csvReader, ParkingJsonReader jsonReader,
                                   PropertyCsvReader propertyReader) {
        this.config = config;
        this.populationReader = populationReader;
        this.csvReader = csvReader;
        this.jsonReader = jsonReader;
        this.propertyReader = propertyReader;
    }

    private AnalyticsService analytics(List<ParkingViolation> parking, List<PropertyEntry> properties,
                                       List<PopulationEntry> population) {
        return new AnalyticsService(new LoadedData(parking, properties, population));
    }

    public long totalPopulation() throws IOException {
        return analytics(List.of(), List.of(), populationReader.read(config.getPopulationFile())).totalPopulation();
    }

    public java.util.NavigableMap<String, BigDecimal> finesPerCapita() throws IOException {
        List<ParkingViolation> parking = "csv".equals(config.getParkingFormat())
                ? csvReader.read(config.getParkingFile()) : jsonReader.read(config.getParkingFile());
        return analytics(parking, List.of(), populationReader.read(config.getPopulationFile())).finesPerCapita();
    }

    public BigDecimal averageMarketValue(String zip) throws IOException {
        return analytics(List.of(), propertyReader.read(config.getPropertiesFile()), List.of()).averageMarketValue(zip);
    }
}
