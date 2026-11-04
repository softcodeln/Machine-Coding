package com.lucky.parkinglot.service;

import com.lucky.parkinglot.enums.GateType;
import com.lucky.parkinglot.enums.TicketStatus;
import com.lucky.parkinglot.exception.ActiveTicketExistsException;
import com.lucky.parkinglot.exception.TicketNotFoundException;
import com.lucky.parkinglot.model.Customer;
import com.lucky.parkinglot.model.Gate;
import com.lucky.parkinglot.model.Spot;
import com.lucky.parkinglot.model.Ticket;
import com.lucky.parkinglot.model.Vehicle;
import com.lucky.parkinglot.payment.Payment;
import com.lucky.parkinglot.repository.SpotRepository;
import com.lucky.parkinglot.repository.TicketRepository;
import com.lucky.parkinglot.strategy.PricingStrategy;
import com.lucky.parkinglot.strategy.SpotAssignment;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ParkingService {

    private final SpotAssignment assignment;
    private final PricingStrategy pricing;
    private final TicketRepository ticketRepository;
    private final SpotRepository spotRepository;
    private final Clock clock;
    private final int lostTicketPenalty;

    private final Set<Integer> ticketsBeingSettled = ConcurrentHashMap.newKeySet();

    public Ticket parkVehicle(Vehicle vehicle, Customer customer, Gate gate) {
        if (gate.getType() != GateType.ENTRY) {
            throw new IllegalArgumentException("Tickets are generated only at an entry gate: " + gate.getName());
        }
        if (ticketRepository.findActiveByVehicleNumber(vehicle.getNumber()).isPresent()) {
            throw activeTicketExists(vehicle);
        }

        Spot spot = assignment.assignSpot(gate, vehicle.getType());
        spot.assignVehicle(vehicle);
        spotRepository.save(spot);

        Ticket ticket = new Ticket(LocalDateTime.now(clock), gate, vehicle, customer, spot);
        // Another gate may have ticketed this vehicle since the check above; if so, give the spot back.
        if (!ticketRepository.saveIfVehicleHasNoActiveTicket(ticket)) {
            freeSpot(spot);
            throw activeTicketExists(vehicle);
        }
        return ticket;
    }

    public boolean exitVehicle(Ticket ticket, Payment payment) {
        return settle(ticket.getId(), payment, 0);
    }

    public boolean exitVehicleWithLostTicket(String vehicleNumber, Payment payment) {
        return settle(findActiveTicket(vehicleNumber).getId(), payment, lostTicketPenalty);
    }

    public Ticket findActiveTicket(String vehicleNumber) {
        return ticketRepository.findActiveByVehicleNumber(vehicleNumber)
                .orElseThrow(() -> new TicketNotFoundException("No active ticket for vehicle: " + vehicleNumber));
    }

    // Only one settlement per ticket at a time, so a second exit gate cannot charge the customer twice.
    private boolean settle(Integer ticketId, Payment payment, int penalty) {
        if (!ticketsBeingSettled.add(ticketId)) {
            throw new IllegalStateException("Payment already in progress for ticket: " + ticketId);
        }
        try {
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new TicketNotFoundException("No ticket with id: " + ticketId));
            if (ticket.getStatus() == TicketStatus.PAID) {
                throw new IllegalStateException("Ticket is already paid: " + ticketId);
            }
            ticket.setExitTime(LocalDateTime.now(clock));
            ticket.setAmount(pricing.calculateFee(ticket) + penalty);
            if (!payment.pay(ticket.getCustomer(), ticket.getAmount())) {
                return false;
            }
            ticket.markPaid();
            ticketRepository.save(ticket);
            freeSpot(ticket.getSpot());
            return true;
        } finally {
            ticketsBeingSettled.remove(ticketId);
        }
    }

    private void freeSpot(Spot spot) {
        spot.free();
        spotRepository.save(spot);
    }

    private ActiveTicketExistsException activeTicketExists(Vehicle vehicle) {
        return new ActiveTicketExistsException("Vehicle already has an active ticket: " + vehicle.getNumber());
    }
}
