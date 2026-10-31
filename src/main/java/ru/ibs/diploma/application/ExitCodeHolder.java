package ru.ibs.diploma.application;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/** Хранит код завершения, выбранный командным интерфейсом. */
@Component
public class ExitCodeHolder implements ExitCodeGenerator {
    private int exitCode;

    public void set(int code) { exitCode = code; }

    @Override
    public int getExitCode() { return exitCode; }
}
