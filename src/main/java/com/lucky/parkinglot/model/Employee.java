package com.lucky.parkinglot.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Employee extends BaseEntity {

    private final String name;
    private final String contact;
    private final Gate gate;
}
