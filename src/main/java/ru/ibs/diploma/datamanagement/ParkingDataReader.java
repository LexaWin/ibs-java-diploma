package ru.ibs.diploma.datamanagement;

import ru.ibs.diploma.data.ParkingViolation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Контракт адаптера чтения парковочных нарушений.
 */
public interface ParkingDataReader {
    List<ParkingViolation> read(Path file) throws IOException;
}
