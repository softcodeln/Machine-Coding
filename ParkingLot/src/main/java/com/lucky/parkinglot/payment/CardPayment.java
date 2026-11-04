package com.lucky.parkinglot.payment;

import com.lucky.parkinglot.gateway.PaymentGateway;
import com.lucky.parkinglot.model.Customer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CardPayment implements Payment {

    private final PaymentGateway payGateway;
    private final String countryCode;

    @Override
    public boolean pay(Customer customer, Integer amount) {
        return payGateway.pay(countryCode, amount);
    }
}
