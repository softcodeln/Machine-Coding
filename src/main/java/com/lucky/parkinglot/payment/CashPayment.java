package com.lucky.parkinglot.payment;

import com.lucky.parkinglot.model.Customer;

public class CashPayment implements Payment {

    @Override
    public boolean pay(Customer customer, Integer amount) {
        return true;
    }
}
