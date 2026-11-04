package com.lucky.parkinglot.repository;

import com.lucky.parkinglot.enums.SpotStatus;
import com.lucky.parkinglot.enums.VehicleType;
import com.lucky.parkinglot.model.Spot;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SpotRepository {

    private final Map<Integer, Spot> spots = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    // Enforces UNIQUE(floor_id, number).
    public synchronized Spot save(Spot spot) {
        boolean duplicate = spots.values().stream()
                .filter(existing -> !Objects.equals(existing.getId(), spot.getId()))
                .anyMatch(existing -> existing.getFloor() == spot.getFloor()
                        && existing.getNumber().equals(spot.getNumber()));
        if (duplicate) {
            throw new IllegalArgumentException("Spot number already exists on floor: " + spot.getNumber());
        }
        if (spot.getId() == null) {
            spot.setId(nextId.getAndIncrement());
        }
        spots.put(spot.getId(), spot);
        return spot;
    }

    public Optional<Spot> findById(Integer id) {
        return Optional.ofNullable(spots.get(id));
    }

    public List<Spot> findAll() {
        return spots.values().stream()
                .sorted(Comparator.comparing(Spot::getId))
                .toList();
    }

    // Ordered by floor, then spot, so the first entries are the nearest.
    public List<Spot> findFreeByType(VehicleType type) {
        return spots.values().stream()
                .filter(spot -> spot.getType() == type && spot.getStatus() == SpotStatus.FREE)
                .sorted(Comparator.comparing((Spot spot) -> spot.getFloor().getId()).thenComparing(Spot::getId))
                .toList();
    }

    // UPDATE spots SET status = 'OCCUPIED' WHERE id = ? AND status = 'FREE'
    // Empty means 0 rows updated: another gate got it, try the next spot.
    public Optional<Spot> occupyIfFree(Integer spotId) {
        return findById(spotId).filter(Spot::tryOccupy);
    }
}
