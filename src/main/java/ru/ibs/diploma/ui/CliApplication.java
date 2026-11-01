package ru.ibs.diploma.ui;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.ibs.diploma.application.AnalyticsCommandService;
import ru.ibs.diploma.application.ExitCodeHolder;
import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.datamanagement.PopulationTxtReader;
import ru.ibs.diploma.exception.CliValidationException;

/**
 * Координирует проверку параметров запуска и работу консольного меню.
 */
@Component
public class CliApplication implements ApplicationRunner {
    private final ExitCodeHolder exitCode;
    private final LocalizationService messages;
    private final MenuController menu;
    private final PopulationTxtReader populationReader;

    public CliApplication(ExitCodeHolder exitCode, LocalizationService messages, MenuController menu, PopulationTxtReader populationReader) {
        this.exitCode = exitCode;
        this.messages = messages;
        this.menu = menu;
        this.populationReader = populationReader;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        try {
            CliConfig config = CliConfig.parse(arguments.getSourceArgs());
            AnalyticsCommandService analytics = new AnalyticsCommandService(config, populationReader);
            menu.run(analytics);
        } catch (CliValidationException exception) {
            System.err.println(messages.text(exception.messageKey(), exception.arguments()));
            exitCode.set(2); // Код 2 - Ошибка аргументов командной строки
        }
    }
}
