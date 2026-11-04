package ru.ibs.diploma.datamanagement;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.data.ZipCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Читает поля парковочного CSV без заголовка и преобразует их в модель ParkingViolation.
 */
@Component
public class ParkingCsvReader implements ParkingDataReader {
    @Override
    public List<ParkingViolation> read(Path file) throws IOException {
        List<ParkingViolation> result = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {
            for (CSVRecord row : parser) {
                if (row.size() != 7) continue;
                try {
                    result.add(new ParkingViolation(row.get(0).trim(), new BigDecimal(row.get(1).trim()),
                            row.get(2).trim(), row.get(3).trim(), row.get(4).trim().toUpperCase(Locale.ROOT),
                            row.get(5).trim(), ZipCode.normalize(row.get(6))));
                } catch (RuntimeException ignored) {
                    /* Некорректная запись пропускается при разборе исходного файла. */
                }
            }
        }
        return List.copyOf(result);
    }
}
