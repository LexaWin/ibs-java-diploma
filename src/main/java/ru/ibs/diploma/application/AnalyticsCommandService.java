package ru.ibs.diploma.application;

import ru.ibs.diploma.data.AdditionalFeatureResult;
import ru.ibs.diploma.data.LoadedData;
import ru.ibs.diploma.datamanagement.DataLoadingSession;
import ru.ibs.diploma.datamanagement.DataSource;
import ru.ibs.diploma.processor.AnalyticsService;
import ru.ibs.diploma.processor.ComputationCache;
import ru.ibs.diploma.processor.PropertyAverageStrategy;
import ru.ibs.diploma.processor.PropertyMetric;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Загружает необходимые записи и делегирует расчёты аналитическому сервису.
 */
public final class AnalyticsCommandService {
    private final DataLoadingSession session;
    private final List<PropertyAverageStrategy> strategies;
    private final ComputationCache cache;
    private final Map<Set<DataSource>, AnalyticsService> services = new HashMap<>();

    public AnalyticsCommandService(DataLoadingSession session, List<PropertyAverageStrategy> strategies, ComputationCache cache) {
        this.session = session;
        this.strategies = List.copyOf(strategies);
        this.cache = cache;
    }

    private AnalyticsService useAnalyticsCommandService(EnumSet<DataSource> sources) throws IOException {
        Set<DataSource> key = Set.copyOf(sources);
        AnalyticsService existing = services.get(key);
        if (existing != null)
            return existing;
        LoadedData data = session.load(sources);
        AnalyticsService service = new AnalyticsService(data, strategies, cache);
        services.put(key, service);
        return service;
    }

    public synchronized long totalPopulation() throws IOException {
        return useAnalyticsCommandService(EnumSet.of(DataSource.POPULATION)).totalPopulation();
    }

    public synchronized NavigableMap<String, BigDecimal> finesPerCapita() throws IOException {
        return useAnalyticsCommandService(EnumSet.of(DataSource.PARKING, DataSource.POPULATION)).finesPerCapita();
    }

    public synchronized BigDecimal averageProperty(String zip, PropertyMetric metric) throws IOException {
        return useAnalyticsCommandService(EnumSet.of(DataSource.PROPERTIES)).averageProperty(zip, metric);
    }

    public synchronized BigDecimal marketValuePerCapita(String zip) throws IOException {
        return useAnalyticsCommandService(EnumSet.of(DataSource.PROPERTIES, DataSource.POPULATION)).marketValuePerCapita(zip);
    }

    public synchronized AdditionalFeatureResult additionalFeature() throws IOException {
        return useAnalyticsCommandService(EnumSet.allOf(DataSource.class)).additionalFeature();
    }
}
