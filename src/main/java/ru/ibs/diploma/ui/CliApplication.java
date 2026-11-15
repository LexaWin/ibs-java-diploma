package ru.ibs.diploma.ui;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.ibs.diploma.application.AnalyticsCommandService;
import ru.ibs.diploma.application.ExitCodeHolder;
import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.datamanagement.*;
import ru.ibs.diploma.exception.CliValidationException;
import ru.ibs.diploma.processor.ComputationCache;
import ru.ibs.diploma.processor.PropertyAverageStrategy;

import java.util.List;

@Component
public class CliApplication implements ApplicationRunner {
    private final ExitCodeHolder exitCode;
    private final LocalizationService messages;
    private final MenuController menu;
    private final PopulationTxtReader populationReader;
    private final ParkingCsvReader csvReader;
    private final ParkingJsonReader jsonReader;
    private final PropertyCsvReader properties;
    private final List<PropertyAverageStrategy> strategies;
    private final ComputationCache cache;

    public CliApplication(ExitCodeHolder exitCode, LocalizationService messages, MenuController menu,
                          PopulationTxtReader populationReader, ParkingCsvReader csvReader,
                          ParkingJsonReader jsonReader, PropertyCsvReader properties,
                          List<PropertyAverageStrategy> strategies, ComputationCache cache) {
        this.exitCode = exitCode;
        this.messages = messages;
        this.menu = menu;
        this.populationReader = populationReader;
        this.csvReader = csvReader;
        this.jsonReader = jsonReader;
        this.properties = properties;
        this.strategies = List.copyOf(strategies);
        this.cache = cache;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        try {
            CliConfig config = CliConfig.parse(arguments.getSourceArgs());
            DataLoadingSession session = new DataLoadingSession(config, populationReader, csvReader, jsonReader, properties);
            AnalyticsCommandService analytics = new AnalyticsCommandService(session, strategies, cache);
            menu.run(analytics);
        } catch (CliValidationException exception) {
            System.err.println(messages.text(exception.messageKey(), (Object) exception.arguments()));
            exitCode.set(2); // Код 2 - Ошибка аргументов командной строки
        }
    }
}
