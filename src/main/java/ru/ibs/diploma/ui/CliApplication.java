package ru.ibs.diploma.ui;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.ibs.diploma.application.ExitCodeHolder;
import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.exception.CliValidationException;

/**
 * Координирует проверку параметров запуска и работу консольного меню.
 */
@Component
public class CliApplication implements ApplicationRunner {
    private final ExitCodeHolder exitCode;

    public CliApplication(ExitCodeHolder exitCode) {
        this.exitCode = exitCode;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        try {
            CliConfig.parse(arguments.getSourceArgs());
        } catch (CliValidationException exception) {
            System.err.println(exception.getMessage());
            exitCode.set(2); // Код 2 - Ошибка аргументов командной строки
        }
    }
}
