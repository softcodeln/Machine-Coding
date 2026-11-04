package com.lucky.parkinglot.repository;

import com.lucky.parkinglot.model.Gate;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GateRepository {

    private final Map<Integer, Gate> gates = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public Gate save(Gate gate) {
        if (gate.getId() == null) {
            gate.setId(nextId.getAndIncrement());
        }
        gates.put(gate.getId(), gate);
        return gate;
    }

    public Optional<Gate> findById(Integer id) {
        return Optional.ofNullable(gates.get(id));
    }

    public List<Gate> findAll() {
        return gates.values().stream()
                .sorted(Comparator.comparing(Gate::getId))
                .toList();
    }
}
