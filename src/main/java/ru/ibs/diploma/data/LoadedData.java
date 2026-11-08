package ru.ibs.diploma.data;

import java.util.List;

/**
 * Неизменяемый снимок разобранных записей входных файлов.
 */
public final class LoadedData {
    private final List<ParkingViolation> parkingViolations;
    private final List<PropertyEntry> propertyEntries;
    private final List<PopulationEntry> populationEntries;

    public LoadedData(List<ParkingViolation> parkingViolations, List<PropertyEntry> propertyEntries,
                      List<PopulationEntry> populationEntries) {
        this.parkingViolations = List.copyOf(parkingViolations);
        this.propertyEntries = List.copyOf(propertyEntries);
        this.populationEntries = List.copyOf(populationEntries);
    }

    public List<ParkingViolation> getParkingViolations() {
        return parkingViolations;
    }

    public List<PropertyEntry> getPropertyEntries() {
        return propertyEntries;
    }

    public List<PopulationEntry> getPopulationEntries() {
        return populationEntries;
    }
}
