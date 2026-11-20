package ru.ibs.diploma.data;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Неизменяемая предметная модель одной записи о парковочном нарушении.
 * <p>Поля соответствуют семи атрибутам исходных CSV/JSON-данных; ZIP-код
 * хранится в нормализованном пятизначном виде, если его удалось распознать.
 */
public final class ParkingViolation {
    private final String date;
    private final BigDecimal fine;
    private final String violation;
    private final String plateId;
    private final String state;
    private final String ticketNumber;
    private final String zipCode;

    /**
     * Создаёт модель парковочного нарушения.
     *
     * @param date дата или исходное текстовое представление даты нарушения
     * @param fine сумма штрафа; может быть {@code null}, если источник не предоставил корректное значение
     * @param violation описание или код нарушения
     * @param plateId идентификатор номерного знака
     * @param state штат регистрации автомобиля
     * @param ticketNumber номер штрафной квитанции
     * @param zipCode нормализованный ZIP-код либо {@code null}, если его не удалось распознать
     */
    public ParkingViolation(String date, BigDecimal fine, String violation, String plateId,
                            String state, String ticketNumber, String zipCode) {
        this.date = date;
        this.fine = fine;
        this.violation = violation;
        this.plateId = plateId;
        this.state = state;
        this.ticketNumber = ticketNumber;
        this.zipCode = zipCode;
    }

    /** @return исходное значение даты нарушения */
    public String getDate() {
        return date;
    }

    /** @return сумма штрафа или {@code null}, если значение отсутствовало либо было некорректным */
    public BigDecimal getFine() {
        return fine;
    }

    /** @return описание или код нарушения */
    public String getViolation() {
        return violation;
    }

    /** @return идентификатор номерного знака */
    public String getPlateId() {
        return plateId;
    }

    /** @return штат регистрации автомобиля */
    public String getState() {
        return state;
    }

    /** @return номер штрафной квитанции */
    public String getTicketNumber() {
        return ticketNumber;
    }

    /** @return нормализованный пятизначный ZIP-код либо {@code null} */
    public String getZipCode() {
        return zipCode;
    }

    /**
     * Сравнивает записи по всем семи предметным полям.
     *
     * @param o объект для сравнения
     * @return {@code true}, если передан эквивалентный {@link ParkingViolation}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParkingViolation p)) return false;
        return Objects.equals(date, p.date) && Objects.equals(fine, p.fine) && Objects.equals(violation, p.violation)
                && Objects.equals(plateId, p.plateId) && Objects.equals(state, p.state) && Objects.equals(ticketNumber, p.ticketNumber)
                && Objects.equals(zipCode, p.zipCode);
    }

    /**
     * Вычисляет хеш-код на основе тех же полей, что и {@link #equals(Object)}.
     *
     * @return хеш-код записи
     */
    @Override
    public int hashCode() {
        return Objects.hash(date, fine, violation, plateId, state, ticketNumber, zipCode);
    }
}
