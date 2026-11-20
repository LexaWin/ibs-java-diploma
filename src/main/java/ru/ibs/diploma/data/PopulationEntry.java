package ru.ibs.diploma.data;

import java.util.Objects;

/**
 * Неизменяемая запись о численности населения для одного ZIP-кода.
 * <p>Несколько записей одного ZIP-кода допустимы и суммируются аналитическим слоем.
 */
public final class PopulationEntry {
    private final String zipCode;
    private final long population;

    /**
     * Создаёт запись населения.
     *
     * @param zipCode ZIP-код, нормализованный reader-компонентом
     * @param population численность населения в записи
     */
    public PopulationEntry(String zipCode, long population) {
        this.zipCode = zipCode;
        this.population = population;
    }

    /** @return ZIP-код записи */
    public String getZipCode() {
        return zipCode;
    }

    /** @return численность населения */
    public long getPopulation() {
        return population;
    }

    /**
     * Сравнивает записи по ZIP-коду и численности населения.
     *
     * @param other объект для сравнения
     * @return {@code true}, если содержимое записей совпадает
     */
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

    /** @return хеш-код, согласованный с {@link #equals(Object)} */
    @Override
    public int hashCode() {
        return Objects.hash(zipCode, population);
    }

    /** @return текстовое представление ZIP-кода и численности населения */
    @Override
    public String toString() {
        return "PopulationEntry{" + "zipCode='" + zipCode + '\''
                + ", population=" + population + '}';
    }
}
