package com.lucky.parkinglot.gateway;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RazorpayGatewayAdapter implements PaymentGateway {

    private final RazorpayPaymentGateway gateway;

    @Override
    public boolean pay(String countryCode, Integer amount) {
        return gateway.paying(countryCode, amount);
    }
}
