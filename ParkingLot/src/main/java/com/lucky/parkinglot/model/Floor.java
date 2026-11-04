package com.lucky.parkinglot.model;

import com.lucky.parkinglot.enums.VehicleType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class Floor extends BaseEntity {

    private final String name;
    private final List<Spot> spots = new ArrayList<>();

    public Spot addSpot(String number, VehicleType type) {
        Spot spot = new Spot(number, type, this);
        spots.add(spot);
        return spot;
    }

    public List<Spot> getSpots() {
        return Collections.unmodifiableList(spots);
    }
}
