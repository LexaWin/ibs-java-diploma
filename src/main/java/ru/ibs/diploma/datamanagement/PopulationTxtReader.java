package ru.ibs.diploma.datamanagement;

import org.springframework.stereotype.Component;
import ru.ibs.diploma.data.PopulationEntry;
import ru.ibs.diploma.data.ZipCode;
import ru.ibs.diploma.logging.ApplicationLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Читает текстовый файл населения, в котором ZIP-код и численность разделены
 * пробелами или табуляцией.
 * <p>Reader возвращает отдельные валидные строки без группировки одинаковых ZIP-кодов;
 * суммирование выполняется аналитическим слоем.
 */
@Component
public class PopulationTxtReader {
    private final ApplicationLogger logger;

    /**
     * Создаёт reader текстового файла населения.
     *
     * @param logger журнал приложения
     */
    public PopulationTxtReader(ApplicationLogger logger) {
        this.logger = logger;
    }

    /**
     * Разбирает строки формата {@code ZIP population}, разделённые пробелами или табуляцией.
     * Неверные строки, ZIP-коды и числовые значения пропускаются.
     *
     * @param file путь к текстовому файлу населения
     * @return неизменяемый список корректных записей без суммирования повторяющихся ZIP-кодов
     * @throws IOException если файл невозможно открыть или прочитать
     */
    public List<PopulationEntry> read(Path file) throws IOException {
        logger.log("FILE_OPEN population=" + file);

        List<PopulationEntry> result = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.trim().split("\\s+");
                if (columns.length != 2) continue;
                String zip = ZipCode.normalize(columns[0]);
                if (zip == null) continue;
                try {
                    result.add(new PopulationEntry(zip, Long.parseLong(columns[1])));
                } catch (NumberFormatException ignored) {
                    // Некорректное числовое значение не образует запись.
                }
            }
        }
        return List.copyOf(result);
    }
}
