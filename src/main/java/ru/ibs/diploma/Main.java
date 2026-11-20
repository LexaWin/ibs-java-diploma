package ru.ibs.diploma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Запускает Spring-контекст и передаёт управление CLI-сценарию приложения.
 * <p>
 * После завершения {@link org.springframework.boot.ApplicationRunner} контекст
 * закрывается, а ненулевой код завершения передаётся операционной системе.
 */
@SpringBootApplication
public class Main {

    /**
     * Запускает Spring Boot и завершает CLI-контекст после выполнения сценария.
     *
     * @param args аргументы командной строки, передаваемые в {@link SpringApplication}
     */
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        int exitCode = SpringApplication.exit(context);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
