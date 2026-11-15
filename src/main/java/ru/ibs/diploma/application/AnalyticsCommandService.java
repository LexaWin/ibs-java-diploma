package ru.ibs.diploma.application;

import ru.ibs.diploma.data.AdditionalFeatureResult;
import ru.ibs.diploma.datamanagement.DataLoadingSession;
import ru.ibs.diploma.datamanagement.DataSource;
import ru.ibs.diploma.processor.AnalyticsService;
import ru.ibs.diploma.processor.ComputationCache;
import ru.ibs.diploma.processor.PropertyAverageStrategy;
import ru.ibs.diploma.processor.PropertyMetric;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.NavigableMap;

/**
 * Загружает необходимые записи и делегирует расчёты аналитическому сервису.
 */
public final class AnalyticsCommandService {
    private final DataLoadingSession session;
    private final List<PropertyAverageStrategy> strategies;
    private final ComputationCache cache;

    public AnalyticsCommandService(DataLoadingSession session, List<PropertyAverageStrategy> strategies, ComputationCache cache) {
        this.session = session;
        this.strategies = List.copyOf(strategies);
        this.cache = cache;
    }

    private AnalyticsService useAnalyticsCommandService(EnumSet<DataSource> sources) throws IOException {
        return new AnalyticsService(session.load(sources), strategies, cache);
    }

    public long totalPopulation() throws IOException {
        return useAnalyticsCommandService(EnumSet.of(DataSource.POPULATION)).totalPopulation();
    }

    public NavigableMap<String, BigDecimal> finesPerCapita() throws IOException {
        return useAnalyticsCommandService(EnumSet.of(DataSource.PARKING, DataSource.POPULATION)).finesPerCapita();
    }

    public BigDecimal averageProperty(String zip, PropertyMetric metric) throws IOException {
        return useAnalyticsCommandService(EnumSet.of(DataSource.PROPERTIES)).averageProperty(zip, metric);
    }

    public BigDecimal marketValuePerCapita(String zip) throws IOException {
        return useAnalyticsCommandService(EnumSet.of(DataSource.PROPERTIES, DataSource.POPULATION)).marketValuePerCapita(zip);
    }

    public AdditionalFeatureResult additionalFeature() throws IOException {
        return useAnalyticsCommandService(EnumSet.allOf(DataSource.class)).additionalFeature();
    }
}
