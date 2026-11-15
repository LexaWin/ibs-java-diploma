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
    private final List<PropertyAverageStrategy> strategies;
    private final ComputationCache cache;
    private final DataLoadingService loader;

    public CliApplication(ExitCodeHolder exitCode, LocalizationService messages, MenuController menu,
                          List<PropertyAverageStrategy> strategies, ComputationCache cache,
                          DataLoadingService loader) {
        this.exitCode = exitCode;
        this.messages = messages;
        this.menu = menu;
        this.strategies = List.copyOf(strategies);
        this.cache = cache;
        this.loader = loader;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        try {
            CliConfig config = CliConfig.parse(arguments.getSourceArgs());
            DataLoadingSession session = loader.openSession(config);
            AnalyticsCommandService analytics = new AnalyticsCommandService(session, strategies, cache);
            menu.run(analytics);
        } catch (CliValidationException exception) {
            System.err.println(messages.text(exception.messageKey(), (Object) exception.arguments()));
            exitCode.set(2); // Код 2 - Ошибка аргументов командной строки
        }
    }
}
