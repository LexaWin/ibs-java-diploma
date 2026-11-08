package ru.ibs.diploma.datamanagement;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import ru.ibs.diploma.data.PropertyEntry;
import ru.ibs.diploma.data.ZipCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Читает properties.csv, находит обязательные столбцы и преобразует строки в записи недвижимости.
 */
@Component
public class PropertyCsvReader {
    private static String cell(CSVRecord row, int index) {
        return index < row.size() ? row.get(index) : "";
    }

    private static BigDecimal decimal(String raw) {
        try {
            return raw == null || raw.isBlank() ? null : new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public List<PropertyEntry> read(Path file) throws IOException {
        List<PropertyEntry> result = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true).get().parse(reader)) {
            Map<String, Integer> columns = new HashMap<>();
            if (parser.getHeaderMap() == null || parser.getHeaderMap().isEmpty()) {
                throw new IOException("В properties.csv отсутствует строка заголовков");
            }
            parser.getHeaderMap().forEach((name, index) -> columns.put(
                    name.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT), index));
            Integer zipIndex = columns.get("zip_code");
            Integer marketIndex = columns.get("market_value");
            Integer areaIndex = columns.get("total_livable_area");
            if (zipIndex == null || marketIndex == null || areaIndex == null) {
                throw new IOException("В properties.csv отсутствуют обязательные столбцы");
            }
            for (CSVRecord row : parser) {
                String zip = ZipCode.normalize(cell(row, zipIndex));
                if (zip == null) continue;
                BigDecimal market = decimal(cell(row, marketIndex));
                BigDecimal area = decimal(cell(row, areaIndex));
                if (market == null && area == null) continue;
                result.add(new PropertyEntry(zip, market, area));
            }
        }
        return List.copyOf(result);
    }
}
