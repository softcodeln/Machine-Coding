package com.lucky.parkinglot.gateway;

// Stand-in for the third-party Stripe SDK; it makes no real payment.
public class StripePaymentGateway {

    public boolean pay(Integer amount, String country) {
        return true;
    }
}
