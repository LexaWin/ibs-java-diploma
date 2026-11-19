package ru.ibs.diploma.datamanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.ibs.diploma.logging.FileLogger;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParkingReaderAdapterTest {
    @TempDir
    Path temp;

    @Test
    void csvAndJsonAdaptersProduceIdenticalDomainModels() throws Exception {
        Path csv = Path.of(getClass().getResource("/parking.csv").toURI());
        Path json = Path.of(getClass().getResource("/parking.json").toURI());
        var csvRows = new ParkingCsvReader(new FileLogger(temp.resolve("csv.log"))).read(csv);
        var jsonRows = new ParkingJsonReader(new FileLogger(temp.resolve("json.log"))).read(json);
        assertEquals(csvRows, jsonRows);
    }
}
