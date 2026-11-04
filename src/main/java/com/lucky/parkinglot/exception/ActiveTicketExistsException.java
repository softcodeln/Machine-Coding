package com.lucky.parkinglot.exception;

public class ActiveTicketExistsException extends RuntimeException {

    public ActiveTicketExistsException(String message) {
        super(message);
    }
}
