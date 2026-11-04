package ru.ibs.diploma.datamanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParkingReaderTest {
    @TempDir
    Path temp;

    @Test
    void csvAndJsonReadersProduceTheSameTicket() throws Exception {
        Path csv = Path.of(getClass().getResource("/parking.csv").toURI());
        Path json = Path.of(getClass().getResource("/parking.json").toURI());
        assertEquals(new ParkingCsvReader().read(csv), new ParkingJsonReader().read(json));
    }
}
