package ru.ibs.diploma.datamanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.logging.FileLogger;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParkingCsvReaderTest {
    @TempDir
    Path temp;

    @Test
    void readsSevenCsvFieldsAndSkipsInvalidFine() throws Exception {
        Path csv = Path.of(getClass().getResource("/parking.csv").toURI());
        List<ParkingViolation> rows = new ParkingCsvReader(new FileLogger(temp.resolve("app.log"))).read(csv);
        assertEquals(2, rows.size());
        assertEquals("19104", rows.get(0).getZipCode());
        assertEquals("36", rows.get(0).getFine().toPlainString());
        assertNull(rows.get(1).getZipCode());
    }
}
