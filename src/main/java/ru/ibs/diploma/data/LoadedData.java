package ru.ibs.diploma.data;

import java.util.List;

/**
 * Неизменяемый контейнер разобранных записей всех трёх источников.
 * <p>Списки копируются при создании объекта, поэтому вызывающий код не может
 * изменить снимок данных через исходные коллекции.
 */
public final class LoadedData {
    private final List<ParkingViolation> parkingViolations;
    private final List<PropertyEntry> propertyEntries;
    private final List<PopulationEntry> populationEntries;

    /**
     * Создаёт неизменяемый снимок списков, полученных от readers.
     *
     * @param parkingViolations записи о парковочных нарушениях
     * @param propertyEntries записи об объектах недвижимости
     * @param populationEntries записи о населении
     */
    public LoadedData(List<ParkingViolation> parkingViolations, List<PropertyEntry> propertyEntries,
                      List<PopulationEntry> populationEntries) {
        this.parkingViolations = List.copyOf(parkingViolations);
        this.propertyEntries = List.copyOf(propertyEntries);
        this.populationEntries = List.copyOf(populationEntries);
    }

    /** @return неизменяемый список парковочных нарушений */
    public List<ParkingViolation> getParkingViolations() {
        return parkingViolations;
    }

    /** @return неизменяемый список записей о недвижимости */
    public List<PropertyEntry> getPropertyEntries() {
        return propertyEntries;
    }

    /** @return неизменяемый список записей о населении */
    public List<PopulationEntry> getPopulationEntries() {
        return populationEntries;
    }
}
