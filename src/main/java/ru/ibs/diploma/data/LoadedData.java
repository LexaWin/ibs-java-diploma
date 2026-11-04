package ru.ibs.diploma.data;

import java.util.List;

/**
 * Неизменяемый снимок разобранных записей, необходимых аналитике.
 */
public final class LoadedData {
    private final List<ParkingViolation> parkingViolations;
    private final List<PopulationEntry> populationEntries;

    public LoadedData(List<ParkingViolation> parkingViolations, List<PopulationEntry> populationEntries) {
        this.parkingViolations = List.copyOf(parkingViolations);
        this.populationEntries = List.copyOf(populationEntries);
    }

    public List<ParkingViolation> getParkingViolations() {
        return parkingViolations;
    }

    public List<PopulationEntry> getPopulationEntries() {
        return populationEntries;
    }
}
