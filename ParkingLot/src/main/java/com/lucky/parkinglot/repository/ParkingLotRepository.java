package com.lucky.parkinglot.repository;

import com.lucky.parkinglot.model.ParkingLot;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ParkingLotRepository {

    private final Map<Integer, ParkingLot> parkingLots = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public ParkingLot save(ParkingLot parkingLot) {
        if (parkingLot.getId() == null) {
            parkingLot.setId(nextId.getAndIncrement());
        }
        parkingLots.put(parkingLot.getId(), parkingLot);
        return parkingLot;
    }

    public Optional<ParkingLot> findById(Integer id) {
        return Optional.ofNullable(parkingLots.get(id));
    }

    public List<ParkingLot> findAll() {
        return parkingLots.values().stream()
                .sorted(Comparator.comparing(ParkingLot::getId))
                .toList();
    }
}
