package ru.ibs.diploma.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileLoggerTest {
    @TempDir
    Path temp;

    @Test
    void appendsTimestampedEntriesWithoutOverwriting() throws Exception {
        Path file = temp.resolve("app.log");
        FileLogger logger = new FileLogger(file);
        logger.log("first");
        logger.log("second");
        var rows = Files.readAllLines(file);
        assertEquals(2, rows.size());
        assertTrue(rows.get(0).matches("\\d+ \\| first"));
        assertTrue(rows.get(1).matches("\\d+ \\| second"));
    }
}
