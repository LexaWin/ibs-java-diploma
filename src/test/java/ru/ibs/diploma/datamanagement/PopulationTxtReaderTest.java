package ru.ibs.diploma.datamanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.ibs.diploma.data.PopulationEntry;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PopulationTxtReaderTest {
    @TempDir
    Path temp;

    @Test
    void normalizesZipCodesAndPreservesDuplicateRows() throws Exception {
        Path file = Path.of(getClass().getResource("/population.txt").toURI());
        List<PopulationEntry> entries = new PopulationTxtReader().read(file);
        assertEquals(3, entries.size());
        assertEquals(List.of(10L, 5L, 8L), entries.stream().map(PopulationEntry::getPopulation).toList());
        assertTrue(entries.stream().allMatch(row -> row.getZipCode().equals("19104")));
    }
}
