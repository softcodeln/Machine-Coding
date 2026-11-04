package com.lucky.parkinglot.gateway;

public interface PaymentGateway {

    boolean pay(String countryCode, Integer amount);
}
