package ru.ibs.diploma.ui;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.ibs.diploma.application.AnalyticsCommandService;
import ru.ibs.diploma.application.ExitCodeHolder;
import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.datamanagement.ParkingCsvReader;
import ru.ibs.diploma.datamanagement.ParkingJsonReader;
import ru.ibs.diploma.datamanagement.PopulationTxtReader;
import ru.ibs.diploma.datamanagement.PropertyCsvReader;
import ru.ibs.diploma.exception.CliValidationException;

@Component
public class CliApplication implements ApplicationRunner {
    private final ExitCodeHolder exitCode;
    private final LocalizationService messages;
    private final MenuController menu;
    private final PopulationTxtReader populationReader;
    private final ParkingCsvReader csvReader;
    private final ParkingJsonReader jsonReader;
    private final PropertyCsvReader properties;

    public CliApplication(ExitCodeHolder exitCode, LocalizationService messages, MenuController menu,
                          PopulationTxtReader populationReader, ParkingCsvReader csvReader, ParkingJsonReader jsonReader, PropertyCsvReader properties) {
        this.exitCode = exitCode;
        this.messages = messages;
        this.menu = menu;
        this.populationReader = populationReader;
        this.csvReader = csvReader;
        this.jsonReader = jsonReader;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        try {
            CliConfig config = CliConfig.parse(arguments.getSourceArgs());
            AnalyticsCommandService analytics
                    = new AnalyticsCommandService(config, populationReader, csvReader, jsonReader, properties);
            menu.run(analytics);
        } catch (CliValidationException exception) {
            System.err.println(messages.text(exception.messageKey(), exception.arguments()));
            exitCode.set(2); // Код 2 - Ошибка аргументов командной строки
        }
    }
}
