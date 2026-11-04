package com.lucky.parkinglot.model;

import com.lucky.parkinglot.enums.VehicleType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Vehicle extends BaseEntity {

    private final String number;
    private final VehicleType type;
}
