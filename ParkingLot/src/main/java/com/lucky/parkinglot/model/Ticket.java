package com.lucky.parkinglot.model;

import com.lucky.parkinglot.enums.TicketStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
public class Ticket extends BaseEntity {

    private final LocalDateTime entryTime;
    private final Gate entryGate;
    private final Vehicle vehicle;
    private final Customer customer;
    private final Spot spot;
    private volatile TicketStatus status = TicketStatus.ACTIVE;
    @Setter
    private Integer amount;
    @Setter
    private LocalDateTime exitTime;

    public Ticket(LocalDateTime entryTime, Gate entryGate, Vehicle vehicle, Customer customer, Spot spot) {
        this.entryTime = entryTime;
        this.entryGate = entryGate;
        this.vehicle = vehicle;
        this.customer = customer;
        this.spot = spot;
    }

    public void markPaid() {
        status = TicketStatus.PAID;
    }
}
