package ru.ibs.diploma.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.ibs.diploma.logging.ApplicationLogger;
import ru.ibs.diploma.logging.FileLogger;

import java.nio.file.Path;

/**
 * Создаёт инфраструктурные компоненты приложения, конфигурация которых определяется аргументами CLI.
 */
@Configuration
public class BeansConfiguration {
    @Bean
    public ApplicationLogger applicationLogger(ApplicationArguments arguments) {
        String[] args = arguments.getSourceArgs();
        // Резервный журнал позволяет сообщить о неверном количестве аргументов, не прерывая создание компонентов Spring.
        Path path = args.length == 5 ? safePath(args[4]) : Path.of("application.log");
        return new FileLogger(path);
    }

    private Path safePath(String logName) {
        try {
            return Path.of(logName);
        } catch (RuntimeException e) {
            return Path.of("application.log");
        }
    }
}
