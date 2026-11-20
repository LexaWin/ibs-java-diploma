package ru.ibs.diploma.datamanagement;

import ru.ibs.diploma.data.ParkingViolation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Общий контракт чтения парковочных нарушений независимо от формата исходного файла.
 * Реализации преобразуют данные в единую предметную модель {@link ParkingViolation}.
 */
public interface ParkingDataReader {
    /**
     * Читает парковочные нарушения из файла указанного формата.
     *
     * @param file путь к входному файлу
     * @return список преобразованных записей
     * @throws IOException если файл невозможно прочитать или его структура недопустима
     */
    List<ParkingViolation> read(Path file) throws IOException;
}
