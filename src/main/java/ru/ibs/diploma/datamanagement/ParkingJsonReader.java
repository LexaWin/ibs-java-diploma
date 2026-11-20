package ru.ibs.diploma.datamanagement;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.data.ZipCode;
import ru.ibs.diploma.exception.DataReadException;
import ru.ibs.diploma.logging.ApplicationLogger;

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
 * Читает JSON-массив парковочных нарушений и преобразует его элементы
 * в {@link ParkingViolation}.
 * <p>Ошибки синтаксиса JSON и неверный тип корневого элемента считаются ошибками
 * файла; отдельные некорректные записи пропускаются.
 */
@Component
public class ParkingJsonReader implements ParkingDataReader {
    private final ApplicationLogger logger;

    /**
     * Создаёт JSON-reader с журналом открытия исходного файла.
     *
     * @param logger журнал приложения
     */
    public ParkingJsonReader(ApplicationLogger logger) {
        this.logger = logger;
    }

    /**
     * Преобразует JSON-значение в очищенную строку.
     *
     * @param value исходное значение JSON
     * @return пустая строка для {@code null}, иначе строковое представление без внешних пробелов
     */
    private static String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    /**
     * Разбирает корневой JSON-массив и преобразует его объектные элементы в нарушения.
     * Ошибка синтаксиса или корневой тип, отличный от массива, приводит к {@link DataReadException}.
     * Некорректные отдельные элементы массива пропускаются.
     *
     * @param file путь к JSON-файлу
     * @return неизменяемый список успешно разобранных нарушений
     * @throws IOException если файл невозможно открыть или прочитать
     * @throws DataReadException если JSON некорректен или его корневой элемент не является массивом
     */
    @Override
    public List<ParkingViolation> read(Path file) throws IOException {
        logger.log("FILE_OPEN parking_json=" + file);

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
