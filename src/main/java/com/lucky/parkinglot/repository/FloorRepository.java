package com.lucky.parkinglot.repository;

import com.lucky.parkinglot.model.Floor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FloorRepository {

    private final Map<Integer, Floor> floors = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public Floor save(Floor floor) {
        if (floor.getId() == null) {
            floor.setId(nextId.getAndIncrement());
        }
        floors.put(floor.getId(), floor);
        return floor;
    }

    public Optional<Floor> findById(Integer id) {
        return Optional.ofNullable(floors.get(id));
    }

    public List<Floor> findAll() {
        return floors.values().stream()
                .sorted(Comparator.comparing(Floor::getId))
                .toList();
    }
}
