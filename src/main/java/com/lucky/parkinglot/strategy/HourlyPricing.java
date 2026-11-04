package com.lucky.parkinglot.strategy;

import com.lucky.parkinglot.enums.VehicleType;
import com.lucky.parkinglot.model.Ticket;

import java.time.Duration;
import java.util.Map;

public class HourlyPricing implements PricingStrategy {

    private final Map<VehicleType, Integer> hourlyRates;

    public HourlyPricing(Map<VehicleType, Integer> hourlyRates) {
        this.hourlyRates = Map.copyOf(hourlyRates);
    }

    @Override
    public Integer calculateFee(Ticket ticket) {
        VehicleType type = ticket.getVehicle().getType();
        Integer rate = hourlyRates.get(type);
        if (rate == null) {
            throw new IllegalArgumentException("No hourly rate for vehicle type " + type);
        }
        Duration parked = Duration.between(ticket.getEntryTime(), ticket.getExitTime());
        return Math.toIntExact(roundUpToHours(parked) * rate);
    }

    private long roundUpToHours(Duration duration) {
        long hours = duration.toHours();
        return duration.equals(Duration.ofHours(hours)) ? hours : hours + 1;
    }
}
