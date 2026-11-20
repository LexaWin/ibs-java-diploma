package ru.ibs.diploma.application;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * Сохраняет код завершения CLI-сценария и предоставляет его Spring Boot.
 * <p>Нулевое значение означает успешное завершение; ненулевое значение
 * используется для обозначения ошибки запуска или выполнения.
 */
@Component
public class ExitCodeHolder implements ExitCodeGenerator {
    private int exitCode;

    /**
     * Устанавливает код, который Spring Boot вернёт при завершении приложения.
     *
     * @param code код завершения; ноль означает успешное завершение
     */
    public void set(int code) {
        exitCode = code;
    }

    /**
     * Возвращает текущий код завершения для механизма Spring Boot.
     *
     * @return установленный код завершения
     */
    @Override
    public int getExitCode() {
        return exitCode;
    }
}
