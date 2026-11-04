package com.lucky.parkinglot.model;

import lombok.Getter;

import java.util.List;

@Getter
public class ParkingLot extends BaseEntity {

    private final String name;
    private final List<Floor> floors;
    private final List<Gate> entryGates;
    private final List<Gate> exitGates;
    private final List<Employee> employees;

    public ParkingLot(String name, List<Floor> floors, List<Gate> entryGates,
                      List<Gate> exitGates, List<Employee> employees) {
        this.name = name;
        this.floors = List.copyOf(floors);
        this.entryGates = List.copyOf(entryGates);
        this.exitGates = List.copyOf(exitGates);
        this.employees = List.copyOf(employees);
    }
}
