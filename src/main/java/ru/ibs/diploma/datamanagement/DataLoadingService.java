package ru.ibs.diploma.datamanagement;

import org.springframework.stereotype.Service;
import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.data.LoadedData;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.data.PopulationEntry;
import ru.ibs.diploma.data.PropertyEntry;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

/**
 * Читает только явно запрошенные источники и возвращает разобранные записи.
 */
@Service
public class DataLoadingService {
    private final ParkingCsvReader csvReader;
    private final ParkingJsonReader jsonReader;
    private final PropertyCsvReader propertiesReader;
    private final PopulationTxtReader populationReader;

    public DataLoadingService(ParkingCsvReader csvReader, ParkingJsonReader jsonReader,
                              PropertyCsvReader propertiesReader, PopulationTxtReader populationReader) {
        this.csvReader = csvReader;
        this.jsonReader = jsonReader;
        this.propertiesReader = propertiesReader;
        this.populationReader = populationReader;
    }

    /**
     * Создаёт пустую сессию; чтение файлов начнётся только при запросе источника.
     */
    public DataLoadingSession openSession(CliConfig config) {
        return new DataLoadingSession(config, this);
    }

    public LoadedData load(CliConfig config, EnumSet<DataSource> sources) throws IOException {
        List<ParkingViolation> parkingRows = sources.contains(DataSource.PARKING)
                ? ("csv".equals(config.getParkingFormat())
                    ? csvReader.read(config.getParkingFile())
                    : jsonReader.read(config.getParkingFile()))
                : List.of();
        List<PropertyEntry> propertyRows = sources.contains(DataSource.PROPERTIES)
                ? propertiesReader.read(config.getPropertiesFile())
                : List.of();
        List<PopulationEntry> populationRows = sources.contains(DataSource.POPULATION)
                ? populationReader.read(config.getPopulationFile())
                : List.of();
        return new LoadedData(parkingRows, propertyRows, populationRows);
    }
}
