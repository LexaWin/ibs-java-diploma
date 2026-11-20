package ru.ibs.diploma.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.ibs.diploma.logging.ApplicationLogger;
import ru.ibs.diploma.logging.FileLogger;

import java.nio.file.Path;

/**
 * Конфигурирует инфраструктурные Spring-компоненты, зависящие от аргументов
 * командной строки. В частности, создаёт файловый журнал до проверки CLI-аргументов.
 */
@Configuration
public class BeansConfiguration {

    /**
     * Создаёт журнал по пятому CLI-аргументу либо использует резервный путь.
     * Резервный путь позволяет журналировать ошибку, даже если количество аргументов неверно.
     *
     * @param arguments разобранные Spring Boot аргументы запуска
     * @return файловая реализация журнала приложения
     */
    @Bean
    public ApplicationLogger applicationLogger(ApplicationArguments arguments) {
        String[] args = arguments.getSourceArgs();
        // Резервный журнал позволяет сообщить о неверном количестве аргументов, не прерывая создание компонентов Spring.
        Path path = args.length == 5 ? safePath(args[4]) : Path.of("application.log");
        return new FileLogger(path);
    }

    /**
     * Преобразует строку пути в {@link Path}; при некорректном пути выбирает резервный файл.
     *
     * @param logName имя или путь файла журнала из командной строки
     * @return корректный путь к файлу журнала
     */
    private Path safePath(String logName) {
        try {
            return Path.of(logName);
        } catch (RuntimeException e) {
            return Path.of("application.log");
        }
    }
}
