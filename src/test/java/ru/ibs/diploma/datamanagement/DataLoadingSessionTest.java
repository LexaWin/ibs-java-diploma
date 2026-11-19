package ru.ibs.diploma.datamanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.data.LoadedData;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.data.PopulationEntry;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DataLoadingSessionTest {
    @TempDir
    Path temp;

    @Test
    void readsARequestedSourceOnlyOnceAndAddsMissingSourcesLater() throws Exception {
        CliConfig config = config();
        DataLoadingService loader = mock(DataLoadingService.class);
        LoadedData populationOnly = new LoadedData(List.of(), List.of(),
                List.of(new PopulationEntry("19104", 100L)));
        LoadedData parkingOnly = new LoadedData(List.of(
                new ParkingViolation("date", BigDecimal.TEN, "type", "car", "PA", "ticket", "19104")),
                List.of(), List.of());

        when(loader.load(eq(config), eq(EnumSet.of(DataSource.POPULATION)))).thenReturn(populationOnly);
        when(loader.load(eq(config), eq(EnumSet.of(DataSource.PARKING)))).thenReturn(parkingOnly);

        DataLoadingSession session = new DataLoadingSession(config, loader);
        session.load(EnumSet.of(DataSource.POPULATION));
        session.load(EnumSet.of(DataSource.POPULATION));
        LoadedData combined = session.load(EnumSet.of(DataSource.PARKING, DataSource.POPULATION));

        assertEquals(100L, combined.getPopulationEntries().get(0).getPopulation());
        assertEquals(1, combined.getParkingViolations().size());
        verify(loader, times(1)).load(config, EnumSet.of(DataSource.POPULATION));
        verify(loader, times(1)).load(config, EnumSet.of(DataSource.PARKING));
        verifyNoMoreInteractions(loader);
    }

    private CliConfig config() throws Exception {
        Path parking = Files.writeString(temp.resolve("parking.csv"), "");
        Path properties = Files.writeString(temp.resolve("properties.csv"), "");
        Path population = Files.writeString(temp.resolve("population.txt"), "");
        return CliConfig.parse(new String[]{"csv", parking.toString(), properties.toString(),
                population.toString(), temp.resolve("app.log").toString()});
    }
}
