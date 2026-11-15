package ru.ibs.diploma.datamanagement;

import ru.ibs.diploma.config.CliConfig;
import ru.ibs.diploma.data.LoadedData;
import ru.ibs.diploma.data.ParkingViolation;
import ru.ibs.diploma.data.PopulationEntry;
import ru.ibs.diploma.data.PropertyEntry;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

/**
 * Кеширует только те разобранные исходные записи, которые понадобились в текущем запуске.
 */
public final class DataLoadingSession {
    private final CliConfig config;
    private final PopulationTxtReader populationReader;
    private final ParkingCsvReader csvReader;
    private final ParkingJsonReader jsonReader;
    private final PropertyCsvReader properties;
    private final EnumSet<DataSource> loaded = EnumSet.noneOf(DataSource.class);
    private List<ParkingViolation> parkingRows = List.of();
    private List<PopulationEntry> populationRows = List.of();
    private List<PropertyEntry> propertyRows = List.of();

    public DataLoadingSession(CliConfig config, PopulationTxtReader populationReader, ParkingCsvReader csvReader,
                              ParkingJsonReader jsonReader, PropertyCsvReader properties) {
        this.config = config;
        this.populationReader = populationReader;
        this.csvReader = csvReader;
        this.jsonReader = jsonReader;
        this.properties = properties;
    }

    public synchronized LoadedData load(EnumSet<DataSource> requested) throws IOException {
        if (requested == null) throw new IllegalArgumentException("Не задан набор источников");
        if (requested.contains(DataSource.POPULATION) && !loaded.contains(DataSource.POPULATION))
            populationRows = populationReader.read(config.getPopulationFile());
        if (requested.contains(DataSource.PARKING) && !loaded.contains(DataSource.PARKING))
            parkingRows = "csv".equals(config.getParkingFormat())
                    ? csvReader.read(config.getParkingFile())
                    : jsonReader.read(config.getParkingFile());
        if (requested.contains(DataSource.PROPERTIES) && !loaded.contains(DataSource.PROPERTIES))
            propertyRows = properties.read(config.getPropertiesFile());
        loaded.addAll(requested);
        return new LoadedData(parkingRows, propertyRows, populationRows);
    }
}
