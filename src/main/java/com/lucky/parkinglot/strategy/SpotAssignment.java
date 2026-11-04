package com.lucky.parkinglot.strategy;

import com.lucky.parkinglot.enums.VehicleType;
import com.lucky.parkinglot.model.Gate;
import com.lucky.parkinglot.model.Spot;

public interface SpotAssignment {

    // Returns a spot already claimed for the caller, or throws NoSpotAvailableException.
    Spot assignSpot(Gate gate, VehicleType type);
}
