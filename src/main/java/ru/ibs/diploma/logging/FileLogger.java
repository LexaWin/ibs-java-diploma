package ru.ibs.diploma.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Журнал для добавления записей. Каждое событие содержит метку времени в миллисекундах.
 */
public class FileLogger implements ApplicationLogger {
    private final Path file;

    public FileLogger(Path file) {
        this.file = file;
    }

    @Override
    public synchronized void log(String message) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            String safeMessage = message == null ? "" : message.replace("\r", " ").replace("\n", " ");
            String line = System.currentTimeMillis() + " | " + safeMessage + System.lineSeparator();
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot append to log file: " + file, ex);
        }
    }
}
