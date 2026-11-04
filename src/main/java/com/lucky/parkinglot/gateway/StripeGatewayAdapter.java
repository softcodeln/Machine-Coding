package com.lucky.parkinglot.gateway;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StripeGatewayAdapter implements PaymentGateway {

    private final StripePaymentGateway gateway;

    @Override
    public boolean pay(String countryCode, Integer amount) {
        return gateway.pay(amount, countryCode);
    }
}
