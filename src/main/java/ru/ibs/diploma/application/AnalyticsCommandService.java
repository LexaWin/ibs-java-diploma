package ru.ibs.diploma.application;

import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.data.*;
import ru.ibs.diploma.datamanagement.ParkingCsvReader;
import ru.ibs.diploma.datamanagement.ParkingJsonReader;
import ru.ibs.diploma.datamanagement.PopulationTxtReader;
import ru.ibs.diploma.datamanagement.PropertyCsvReader;
import ru.ibs.diploma.processor.AnalyticsService;
import ru.ibs.diploma.processor.PropertyAverageStrategy;
import ru.ibs.diploma.processor.PropertyMetric;

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
    private final List<PropertyAverageStrategy> strategies;

    public AnalyticsCommandService(CliConfig config, PopulationTxtReader populationReader,
                                   ParkingCsvReader csvReader, ParkingJsonReader jsonReader,
                                   PropertyCsvReader propertyReader, List<PropertyAverageStrategy> strategies) {
        this.config = config;
        this.populationReader = populationReader;
        this.csvReader = csvReader;
        this.jsonReader = jsonReader;
        this.propertyReader = propertyReader;
        this.strategies = List.copyOf(strategies);
    }

    private AnalyticsService analytics(List<ParkingViolation> parking, List<PropertyEntry> properties,
                                       List<PopulationEntry> population) {
        return new AnalyticsService(new LoadedData(parking, properties, population), strategies);
    }

    public long totalPopulation() throws IOException {
        return analytics(List.of(), List.of(), populationReader.read(config.getPopulationFile())).totalPopulation();
    }

    private List<ParkingViolation> parking() throws IOException {
        return "csv".equals(config.getParkingFormat())
                ? csvReader.read(config.getParkingFile())
                : jsonReader.read(config.getParkingFile());
    }

    public java.util.NavigableMap<String, BigDecimal> finesPerCapita() throws IOException {
        return analytics(parking(), List.of(), populationReader.read(config.getPopulationFile())).finesPerCapita();
    }

    public BigDecimal averageProperty(String zip, PropertyMetric metric) throws IOException {
        return analytics(
                List.of(),
                propertyReader.read(config.getPropertiesFile()),
                List.of()
        ).averageProperty(zip, metric);
    }

    public BigDecimal marketValuePerCapita(String zip) throws IOException {
        return analytics(List.of(), propertyReader.read(config.getPropertiesFile()),
                populationReader.read(config.getPopulationFile())).marketValuePerCapita(zip);
    }

    public AdditionalFeatureResult additionalFeature() throws IOException {
        return analytics(parking(), propertyReader.read(config.getPropertiesFile()),
                populationReader.read(config.getPopulationFile())).additionalFeature();
    }
}
