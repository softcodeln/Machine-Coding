package com.lucky.parkinglot.strategy;

import com.lucky.parkinglot.model.Ticket;

public interface PricingStrategy {

    Integer calculateFee(Ticket ticket);
}
