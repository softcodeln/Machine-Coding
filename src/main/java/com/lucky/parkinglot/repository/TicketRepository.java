package com.lucky.parkinglot.repository;

import com.lucky.parkinglot.enums.TicketStatus;
import com.lucky.parkinglot.model.Ticket;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TicketRepository {

    private final Map<Integer, Ticket> tickets = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public Ticket save(Ticket ticket) {
        if (ticket.getId() == null) {
            ticket.setId(nextId.getAndIncrement());
        }
        tickets.put(ticket.getId(), ticket);
        return ticket;
    }

    public Optional<Ticket> findById(Integer id) {
        return Optional.ofNullable(tickets.get(id));
    }

    public List<Ticket> findAll() {
        return tickets.values().stream()
                .sorted(Comparator.comparing(Ticket::getId))
                .toList();
    }

    public Optional<Ticket> findActiveByVehicleNumber(String vehicleNumber) {
        return tickets.values().stream()
                .filter(ticket -> ticket.getStatus() == TicketStatus.ACTIVE
                        && ticket.getVehicle().getNumber().equals(vehicleNumber))
                .findFirst();
    }

    // Atomic check-and-insert, so two gates cannot both ticket the same vehicle.
    public synchronized boolean saveIfVehicleHasNoActiveTicket(Ticket ticket) {
        if (findActiveByVehicleNumber(ticket.getVehicle().getNumber()).isPresent()) {
            return false;
        }
        save(ticket);
        return true;
    }
}
