package com.lucky.parkinglot.payment;

import com.lucky.parkinglot.model.Customer;

public interface Payment {

    boolean pay(Customer customer, Integer amount);
}
