package ru.ibs.diploma.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Реализация {@link ApplicationLogger}, дописывающая события в UTF-8-файл.
 * <p>Каждая строка содержит Unix-время в миллисекундах и текст события;
 * запись синхронизирована, чтобы параллельные загрузчики не смешивали строки.
 */
public class FileLogger implements ApplicationLogger {
    private final Path file;

    /**
     * Создаёт журнал, который дописывает записи в указанный файл.
     *
     * @param file путь к файлу журнала
     */
    public FileLogger(Path file) {
        this.file = file;
    }

    /**
     * Добавляет строку с текущим временем в миллисекундах и очищенным текстом сообщения.
     * При необходимости родительские каталоги создаются автоматически.
     *
     * @param message текст события; переводы строк заменяются пробелами
     * @throws IllegalStateException если каталог или файл журнала не удалось создать/обновить
     */
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
