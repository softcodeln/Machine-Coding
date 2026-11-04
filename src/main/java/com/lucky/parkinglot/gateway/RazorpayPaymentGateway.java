package com.lucky.parkinglot.gateway;

// Stand-in for the third-party Razorpay SDK; it makes no real payment.
public class RazorpayPaymentGateway {

    public boolean paying(String countryCode, Integer amount) {
        return true;
    }
}
