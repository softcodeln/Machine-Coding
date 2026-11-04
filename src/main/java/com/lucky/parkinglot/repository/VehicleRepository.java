package com.lucky.parkinglot.repository;

import com.lucky.parkinglot.model.Vehicle;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class VehicleRepository {

    private final Map<Integer, Vehicle> vehicles = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    // Enforces UNIQUE(number).
    public synchronized Vehicle save(Vehicle vehicle) {
        findByNumber(vehicle.getNumber())
                .filter(existing -> !Objects.equals(existing.getId(), vehicle.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Vehicle number already exists: " + vehicle.getNumber());
                });
        if (vehicle.getId() == null) {
            vehicle.setId(nextId.getAndIncrement());
        }
        vehicles.put(vehicle.getId(), vehicle);
        return vehicle;
    }

    public Optional<Vehicle> findById(Integer id) {
        return Optional.ofNullable(vehicles.get(id));
    }

    public Optional<Vehicle> findByNumber(String number) {
        return vehicles.values().stream()
                .filter(vehicle -> vehicle.getNumber().equals(number))
                .findFirst();
    }

    public List<Vehicle> findAll() {
        return vehicles.values().stream()
                .sorted(Comparator.comparing(Vehicle::getId))
                .toList();
    }
}
