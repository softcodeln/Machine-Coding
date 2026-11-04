package com.lucky.parkinglot.model;

import com.lucky.parkinglot.enums.SpotStatus;
import com.lucky.parkinglot.enums.VehicleType;
import lombok.Getter;

@Getter
public class Spot extends BaseEntity {

    private final String number;
    private final VehicleType type;
    private final Floor floor;
    private volatile SpotStatus status = SpotStatus.FREE;
    private volatile Vehicle vehicle;

    Spot(String number, VehicleType type, Floor floor) {
        this.number = number;
        this.type = type;
        this.floor = floor;
    }

    public synchronized boolean tryOccupy() {
        if (status != SpotStatus.FREE) {
            return false;
        }
        status = SpotStatus.OCCUPIED;
        return true;
    }

    public synchronized void assignVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public synchronized void free() {
        status = SpotStatus.FREE;
        vehicle = null;
    }

    public synchronized boolean startMaintenance() {
        if (status != SpotStatus.FREE) {
            return false;
        }
        status = SpotStatus.MAINTENANCE;
        return true;
    }

    public synchronized boolean endMaintenance() {
        if (status != SpotStatus.MAINTENANCE) {
            return false;
        }
        status = SpotStatus.FREE;
        return true;
    }
}
