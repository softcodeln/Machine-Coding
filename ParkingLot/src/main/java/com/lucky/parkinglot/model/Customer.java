package com.lucky.parkinglot.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Customer extends BaseEntity {

    private final String name;
}
