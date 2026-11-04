package com.lucky.parkinglot.repository;

import com.lucky.parkinglot.model.Customer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomerRepository {

    private final Map<Integer, Customer> customers = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public Customer save(Customer customer) {
        if (customer.getId() == null) {
            customer.setId(nextId.getAndIncrement());
        }
        customers.put(customer.getId(), customer);
        return customer;
    }

    public Optional<Customer> findById(Integer id) {
        return Optional.ofNullable(customers.get(id));
    }

    public List<Customer> findAll() {
        return customers.values().stream()
                .sorted(Comparator.comparing(Customer::getId))
                .toList();
    }
}
