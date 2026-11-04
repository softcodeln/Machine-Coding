package com.lucky.parkinglot.strategy;

import com.lucky.parkinglot.enums.VehicleType;
import com.lucky.parkinglot.exception.NoSpotAvailableException;
import com.lucky.parkinglot.model.Gate;
import com.lucky.parkinglot.model.Spot;
import com.lucky.parkinglot.repository.SpotRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class NearestSpotAssignment implements SpotAssignment {

    private final SpotRepository spotRepository;

    // The design has no gate-to-spot distances, so floor and spot order stand in for distance from every gate.
    @Override
    public Spot assignSpot(Gate gate, VehicleType type) {
        return spotRepository.findFreeByType(type).stream()
                .map(spot -> spotRepository.occupyIfFree(spot.getId()))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new NoSpotAvailableException("No free spot for vehicle type " + type));
    }
}
