package ru.ibs.diploma.datamanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.ibs.diploma.logging.FileLogger;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParkingJsonReaderTest {
    @TempDir
    Path temp;

    @Test
    void mapsJsonToTheSameDomainModel() throws Exception {
        Path json = Path.of(getClass().getResource("/parking.json").toURI());
        var rows = new ParkingJsonReader(new FileLogger(temp.resolve("app.log"))).read(json);
        assertEquals(2, rows.size());
        assertEquals("19104", rows.get(0).getZipCode());
        assertEquals("36", rows.get(0).getFine().toPlainString());
        assertNull(rows.get(1).getZipCode());
    }
}
