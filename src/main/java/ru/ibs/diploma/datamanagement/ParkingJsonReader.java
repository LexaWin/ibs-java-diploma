package ru.ibs.diploma.datamanagement;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.data.ZipCode;
import ru.ibs.diploma.exception.DataReadException;
import ru.ibs.diploma.ui.LocalizationService;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Преобразует JSON-массив нарушений в модель ParkingViolation.
 */
@Component
public class ParkingJsonReader implements ParkingDataReader {
    private static String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    @Override
    public List<ParkingViolation> read(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Object parsed;
            try {
                parsed = new JSONParser().parse(reader);
            } catch (ParseException e) {
                throw new DataReadException("error.incorrect.json", e, file.toString());
            }
            if (!(parsed instanceof JSONArray rows))
                throw new DataReadException("error.json.array", file.toString());
            List<ParkingViolation> result = new ArrayList<>();
            for (Object value : rows) {
                if (!(value instanceof JSONObject row)) continue;
                try {
                    result.add(new ParkingViolation(text(row.get("date")), new BigDecimal(text(row.get("fine"))),
                            text(row.get("violation")), text(row.get("plate_id")),
                            text(row.get("state")).toUpperCase(Locale.ROOT), text(row.get("ticket_number")),
                            ZipCode.normalize(text(row.get("zip_code")))));
                } catch (RuntimeException ignored) {
                    /* Пропускается только некорректная запись. */
                }
            }
            return List.copyOf(result);
        }
    }
}
