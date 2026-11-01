package ru.ibs.diploma.data;

import java.util.Objects;

/**
 * Неизменяемая запись с ZIP-кодом и численностью населения.
 */
public final class PopulationEntry {
    private final String zipCode;
    private final long population;

    public PopulationEntry(String zipCode, long population) {
        this.zipCode = zipCode;
        this.population = population;
    }

    public String getZipCode() {
        return zipCode;
    }

    public long getPopulation() {
        return population;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PopulationEntry that)) {
            return false;
        }
        return population == that.population && Objects.equals(zipCode, that.zipCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zipCode, population);
    }

    @Override
    public String toString() {
        return "PopulationEntry{" + "zipCode='" + zipCode + '\''
                + ", population=" + population + '}';
    }
}
