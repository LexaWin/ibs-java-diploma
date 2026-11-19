package ru.ibs.diploma.datamanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.ibs.diploma.data.PopulationEntry;
import ru.ibs.diploma.logging.FileLogger;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PopulationTxtReaderTest {
    @TempDir
    Path temp;

    @Test
    void parsesValidRowsWithoutAggregatingThem() throws Exception {
        Path file = Path.of(getClass().getResource("/population.txt").toURI());
        List<PopulationEntry> entries = new PopulationTxtReader(
                new FileLogger(temp.resolve("app.log"))).read(file);

        assertEquals(3, entries.size());
        assertEquals(new PopulationEntry("19104", 10L), entries.get(0));
        assertEquals(new PopulationEntry("19104", 5L), entries.get(1));
        assertEquals(new PopulationEntry("19104", 8L), entries.get(2));
        assertEquals(23L, entries.stream().mapToLong(PopulationEntry::getPopulation).sum());
    }
}
