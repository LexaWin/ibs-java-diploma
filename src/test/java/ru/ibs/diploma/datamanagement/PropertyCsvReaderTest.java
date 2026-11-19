package ru.ibs.diploma.datamanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.ibs.diploma.data.PropertyEntry;
import ru.ibs.diploma.logging.FileLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PropertyCsvReaderTest {
    @TempDir
    Path temp;

    @Test
    void discoversReorderedHeadersHandlesQuotedCommasAndReturnsParsedRows() throws Exception {
        Path file = Path.of(getClass().getResource("/properties.csv").toURI());
        List<PropertyEntry> entries = new PropertyCsvReader(new FileLogger(temp.resolve("app.log"))).read(file);

        assertEquals(4, entries.size());
        assertEquals("19104", entries.get(0).getZipCode());
        assertEquals(0, entries.get(0).getMarketValue().compareTo(new java.math.BigDecimal("100000")));
        assertEquals(0, entries.get(0).getLivableArea().compareTo(new java.math.BigDecimal("1200")));
        assertEquals("19104", entries.get(1).getZipCode());
        assertNull(entries.get(1).getLivableArea());
        assertEquals("19104", entries.get(2).getZipCode());
        assertNull(entries.get(2).getMarketValue());
        assertEquals("19104", entries.get(3).getZipCode());
    }

    @Test
    void failsWhenRequiredHeaderIsMissing() throws Exception {
        Path file = temp.resolve("bad.csv");
        Files.writeString(file, "zip_code,market_value\n19104,100\n");
        assertThrows(java.io.IOException.class,
                () -> new PropertyCsvReader(new FileLogger(temp.resolve("app.log"))).read(file));
    }
}
