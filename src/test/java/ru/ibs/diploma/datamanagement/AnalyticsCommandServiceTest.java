package ru.ibs.diploma.datamanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.ibs.diploma.application.AnalyticsCommandService;
import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.data.*;
import ru.ibs.diploma.processor.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AnalyticsCommandServiceTest {
    @TempDir
    Path temp;

    @Test
    void totalPopulationRequestsOnlyPopulationFile() throws Exception {
        DataLoadingService loader = loader();
        assertEquals(100L, commands(loader).totalPopulation());
        verify(loader).load(any(CliConfig.class), eq(EnumSet.of(DataSource.POPULATION)));
    }

    @Test
    void propertyAverageRequestsOnlyPropertiesFile() throws Exception {
        DataLoadingService loader = loader();
        assertEquals("500", commands(loader).averageProperty("19104", PropertyMetric.MARKET_VALUE).toPlainString());
        verify(loader).load(any(CliConfig.class), eq(EnumSet.of(DataSource.PROPERTIES)));
    }

    @Test
    void finesPerCapitaRequestsParkingAndPopulationFiles() throws Exception {
        DataLoadingService loader = loader();
        assertEquals("0.1000", commands(loader).finesPerCapita().get("19104").toPlainString());
        verify(loader).load(any(CliConfig.class), eq(EnumSet.of(DataSource.PARKING, DataSource.POPULATION)));
    }

    @Test
    void marketValuePerCapitaRequestsPropertiesAndPopulationFiles() throws Exception {
        DataLoadingService loader = loader();
        assertEquals("10", commands(loader).marketValuePerCapita("19104").toPlainString());
        verify(loader).load(any(CliConfig.class), eq(EnumSet.of(DataSource.PROPERTIES, DataSource.POPULATION)));
    }

    @Test
    void additionalFeatureRequestsAllThreeFiles() throws Exception {
        DataLoadingService loader = loader();
        AdditionalFeatureResult result = commands(loader).additionalFeature();
        assertEquals("10.000000000000",
                result.getByZip().get("19104").getViolationsPer1000Residents().toPlainString());
        assertEquals("500.000000000000",
                result.getByZip().get("19104").getAverageMarketValue().toPlainString());
        assertTrue(result.getSpearmanCorrelation().isEmpty());
        verify(loader).load(any(CliConfig.class), eq(EnumSet.allOf(DataSource.class)));
    }

    private AnalyticsCommandService commands(DataLoadingService loader) throws Exception {
        CliConfig config = config();
        DataLoadingSession session = new DataLoadingSession(config, loader);
        List<PropertyAverageStrategy> strategies = List.of(
                new AverageMarketValueStrategy(), new AverageLivableAreaStrategy());
        return new AnalyticsCommandService(session, strategies, new ComputationCache());
    }

    private DataLoadingService loader() throws Exception {
        DataLoadingService loader = mock(DataLoadingService.class);
        when(loader.load(any(CliConfig.class), any())).thenAnswer(invocation -> {
            EnumSet<DataSource> requested = invocation.getArgument(1);
            List<ParkingViolation> parking = requested.contains(DataSource.PARKING)
                    ? List.of(new ParkingViolation("date", new BigDecimal("10"), "type", "car", "PA", "ticket", "19104"))
                    : List.of();
            List<PropertyEntry> properties = requested.contains(DataSource.PROPERTIES)
                    ? List.of(new PropertyEntry("19104", new BigDecimal("500"), new BigDecimal("1000")),
                    new PropertyEntry("19104", new BigDecimal("500"), new BigDecimal("1000")))
                    : List.of();
            List<PopulationEntry> population = requested.contains(DataSource.POPULATION)
                    ? List.of(new PopulationEntry("19104", 100L)) : List.of();
            return new LoadedData(parking, properties, population);
        });
        return loader;
    }

    private CliConfig config() throws Exception {
        Path parking = Files.writeString(temp.resolve("parking.csv"), "");
        Path properties = Files.writeString(temp.resolve("properties.csv"), "");
        Path population = Files.writeString(temp.resolve("population.txt"), "");
        return CliConfig.parse(new String[]{"csv", parking.toString(), properties.toString(),
                population.toString(), temp.resolve("app.log").toString()});
    }
}
