package ru.ibs.diploma.processor;

import org.junit.jupiter.api.Test;
import ru.ibs.diploma.data.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsServiceTest {
    private static AnalyticsService correlationService() {
        List<ParkingViolation> tickets = List.of(
                ticket("1", "PA", "19103"),
                ticket("1", "NJ", "19104"),
                ticket("1", "PA", "19104"),
                ticket("1", "PA", "19105"),
                ticket("1", "NY", "19105"),
                ticket("1", "PA", "19105"));
        List<PropertyEntry> properties = List.of(
                property("19102", "100", null),
                property("19103", "200", null),
                property("19104", "300", null),
                property("19105", "300", null));
        List<PopulationEntry> population = List.of(
                new PopulationEntry("19102", 100),
                new PopulationEntry("19103", 100),
                new PopulationEntry("19104", 100),
                new PopulationEntry("19105", 100));
        return new AnalyticsService(
                new LoadedData(tickets, properties, population),
                strategies(),
                new ComputationCache()
        );
    }

    private static List<PropertyAverageStrategy> strategies() {
        return List.of(new AverageMarketValueStrategy(), new AverageLivableAreaStrategy());
    }

    private static ParkingViolation ticket(String fine, String state, String zip) {
        return new ParkingViolation("d", new BigDecimal(fine), "A", "1", state, "1", zip);
    }

    private static PropertyEntry property(String zip, String market, String area) {
        return new PropertyEntry(zip,
                market == null ? null : new BigDecimal(market),
                area == null ? null : new BigDecimal(area));
    }

    private AnalyticsService service() {
        List<ParkingViolation> tickets = List.of(
                ticket("10.123456", "PA", "19104"),
                ticket("5.123456", "PA", "19104"),
                ticket("999", "NJ", "19104"),
                ticket("3", "PA", "19103"),
                ticket("2.00009", "PA", "19102"),
                ticket("10", "PA", "19105"),
                ticket("-10", "PA", "19105"),
                ticket("100", "PA", "19106"));
        List<PropertyEntry> properties = List.of(
                property("19104", "100", "1000"),
                property("19104", null, "2000"),
                property("19102", "0", null),
                property("19103", "1000", "1000"));
        List<PopulationEntry> population = List.of(
                new PopulationEntry("19104", 3),
                new PopulationEntry("19103", 0),
                new PopulationEntry("19102", 3),
                new PopulationEntry("19105", 2));
        LoadedData data = new LoadedData(tickets, properties, population);
        return new AnalyticsService(
                data,
                List.of(new AverageMarketValueStrategy(), new AverageLivableAreaStrategy()),
                new ComputationCache()
        );
    }

    @Test
    void calculatesAllRequiredMetricsWithTruncationAndZeroRules() {
        AnalyticsService service = service();
        assertEquals(8L, service.totalPopulation());
        assertEquals("0.6666", service.finesPerCapita().get("19102").toPlainString());
        assertEquals("5.0823", service.finesPerCapita().get("19104").toPlainString());
        assertEquals(List.of("19102", "19104"), List.copyOf(service.finesPerCapita().keySet()));
        assertFalse(service.finesPerCapita().containsKey("19103"));
        assertFalse(service.finesPerCapita().containsKey("19105"));
        assertFalse(service.finesPerCapita().containsKey("19106"));
        assertEquals("100", service.averageProperty("19104", PropertyMetric.MARKET_VALUE).toPlainString());
        assertEquals("1500", service.averageProperty("19104", PropertyMetric.LIVABLE_AREA).toPlainString());
        assertEquals("33", service.marketValuePerCapita("19104").toPlainString());
        assertEquals(BigDecimal.ZERO, service.marketValuePerCapita("19102"));
        assertEquals(BigDecimal.ZERO, service.marketValuePerCapita("19103"));
        assertEquals(BigDecimal.ZERO, service.marketValuePerCapita("19999"));
    }

    @Test
    void additionalFeatureUsesAllThreeSourcesAndIncludesZeroViolationZip() {
        AdditionalFeatureResult result = correlationService().additionalFeature();

        assertEquals(List.of("19102", "19103", "19104", "19105"),
                List.copyOf(result.getByZip().keySet()));
        assertEquals("0.000000000000",
                result.getByZip().get("19102").getViolationsPer1000Residents().toPlainString());
        assertEquals("10.000000000000",
                result.getByZip().get("19103").getViolationsPer1000Residents().toPlainString());
        assertEquals("20.000000000000",
                result.getByZip().get("19104").getViolationsPer1000Residents().toPlainString());
        assertEquals("30.000000000000",
                result.getByZip().get("19105").getViolationsPer1000Residents().toPlainString());
        assertEquals("300.000000000000",
                result.getByZip().get("19105").getAverageMarketValue().toPlainString());
        assertEquals(Optional.of(new BigDecimal("0.9487")), result.getSpearmanCorrelation());
        assertEquals(4, result.getSampleSize());
    }

    @Test
    void additionalFeatureLeavesCorrelationUndefinedForOneZip() {
        LoadedData data = new LoadedData(
                List.of(ticket("1", "NJ", "19104")),
                List.of(property("19104", "500", null)),
                List.of(new PopulationEntry("19104", 100)));
        AnalyticsService service = new AnalyticsService(data, strategies(), new ComputationCache());

        AdditionalFeatureResult result = service.additionalFeature();

        assertEquals(1, result.getSampleSize());
        assertTrue(result.getSpearmanCorrelation().isEmpty());
        assertEquals("10.000000000000",
                result.getByZip().get("19104").getViolationsPer1000Residents().toPlainString());
    }

    @Test
    void memoizesEveryResultFamily() {
        AnalyticsService service = service();
        service.totalPopulation();
        service.finesPerCapita();
        service.averageProperty("19104", PropertyMetric.MARKET_VALUE);
        service.averageProperty("19104", PropertyMetric.LIVABLE_AREA);
        service.marketValuePerCapita("19104");
        service.additionalFeature();
        int count = service.cachedResultCount();
        service.additionalFeature();
        service.marketValuePerCapita("19104");
        assertEquals(count, service.cachedResultCount());
    }
}
