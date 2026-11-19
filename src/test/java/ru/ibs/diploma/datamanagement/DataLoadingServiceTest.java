package ru.ibs.diploma.datamanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.data.PopulationEntry;
import ru.ibs.diploma.logging.ApplicationLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DataLoadingServiceTest {
    @TempDir
    Path temp;

    @Test
    void openingSessionDoesNotParseAnyInputFile() throws Exception {
        Path parking = Files.writeString(temp.resolve("parking.csv"), "deliberately malformed content");
        Path properties = Files.writeString(temp.resolve("properties.csv"), "deliberately malformed content");
        Path population = Files.writeString(temp.resolve("population.txt"), "deliberately malformed content");
        CliConfig config = new CliConfigForTest().parse(parking, properties, population, temp);

        ParkingCsvReader csvReader = mock(ParkingCsvReader.class);
        ParkingJsonReader jsonReader = mock(ParkingJsonReader.class);
        PropertyCsvReader propertyReader = mock(PropertyCsvReader.class);
        PopulationTxtReader populationReader = mock(PopulationTxtReader.class);
        ApplicationLogger logger = mock(ApplicationLogger.class);
        DataLoadingService service = new DataLoadingService(csvReader, jsonReader, propertyReader, populationReader, logger);

        service.openSession(config);
        verifyNoInteractions(csvReader, jsonReader, propertyReader, populationReader);
    }

    @Test
    void readsOnlyTheRequestedSource() throws Exception {
        Path parking = Files.writeString(temp.resolve("parking.csv"), "not parsed in this test");
        Path properties = Files.writeString(temp.resolve("properties.csv"), "not parsed in this test");
        Path population = Files.writeString(temp.resolve("population.txt"), "19104 100");
        CliConfig config = new CliConfigForTest().parse(parking, properties, population, temp);

        ParkingCsvReader csvReader = mock(ParkingCsvReader.class);
        ParkingJsonReader jsonReader = mock(ParkingJsonReader.class);
        PropertyCsvReader propertyReader = mock(PropertyCsvReader.class);
        PopulationTxtReader populationReader = mock(PopulationTxtReader.class);
        ApplicationLogger logger = mock(ApplicationLogger.class);
        when(populationReader.read(population)).thenReturn(List.of(new PopulationEntry("19104", 100L)));

        DataLoadingService service = new DataLoadingService(csvReader, jsonReader, propertyReader, populationReader, logger);
        var result = service.load(config, EnumSet.of(DataSource.POPULATION));

        assertEquals(100L, result.getPopulationEntries().get(0).getPopulation());
        verify(populationReader).read(population);
        verifyNoInteractions(csvReader, jsonReader, propertyReader);
    }

    private static class CliConfigForTest {
        CliConfig parse(Path parking, Path properties, Path population, Path temp) throws Exception {
            return CliConfig.parse(new String[]{"csv", parking.toString(), properties.toString(),
                    population.toString(), temp.resolve("app.log").toString()});
        }
    }
}
