package ru.ibs.diploma.application;

import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.data.PopulationEntry;
import ru.ibs.diploma.datamanagement.PopulationTxtReader;

import java.io.IOException;

/**
 * Выполняет аналитику, запрошенную пунктами консольного меню.
 */
public final class AnalyticsCommandService {
    private final CliConfig config;
    private final PopulationTxtReader populationReader;

    public AnalyticsCommandService(CliConfig config, PopulationTxtReader populationReader) {
        this.config = config;
        this.populationReader = populationReader;
    }

    /**
     * Пункт 1: читает записи и суммирует численность населения.
     */
    public long totalPopulation() throws IOException {
        return populationReader.read(config.getPopulationFile()).stream()
                .mapToLong(PopulationEntry::getPopulation)
                .sum();
    }
}
