package com.lucky.parkinglot.model;

import com.lucky.parkinglot.enums.GateType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Gate extends BaseEntity {

    private final String name;
    private final GateType type;
}
