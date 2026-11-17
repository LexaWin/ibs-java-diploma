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
 * Читает данные о населении и нормализует отдельные записи индекса.
 */
@Component
public class PopulationTxtReader {
    private final ApplicationLogger logger;

    public PopulationTxtReader(ApplicationLogger logger) {
        this.logger = logger;
    }

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
