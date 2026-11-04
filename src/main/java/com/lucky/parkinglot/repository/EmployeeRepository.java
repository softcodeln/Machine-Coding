package com.lucky.parkinglot.repository;

import com.lucky.parkinglot.model.Employee;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class EmployeeRepository {

    private final Map<Integer, Employee> employees = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public Employee save(Employee employee) {
        if (employee.getId() == null) {
            employee.setId(nextId.getAndIncrement());
        }
        employees.put(employee.getId(), employee);
        return employee;
    }

    public Optional<Employee> findById(Integer id) {
        return Optional.ofNullable(employees.get(id));
    }

    public List<Employee> findAll() {
        return employees.values().stream()
                .sorted(Comparator.comparing(Employee::getId))
                .toList();
    }
}
