package ru.ibs.diploma.data;

import java.math.BigDecimal;

/**
 * Единая модель парковочного нарушения.
 */
public final class ParkingViolation {
    private final String date;
    private final BigDecimal fine;
    private final String violation;
    private final String plateId;
    private final String state;
    private final String ticketNumber;
    private final String zipCode;

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

    public String getDate() {
        return date;
    }

    public BigDecimal getFine() {
        return fine;
    }

    public String getViolation() {
        return violation;
    }

    public String getPlateId() {
        return plateId;
    }

    public String getState() {
        return state;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public String getZipCode() {
        return zipCode;
    }
}
