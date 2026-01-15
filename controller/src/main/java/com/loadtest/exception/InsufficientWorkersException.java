package com.loadtest.exception;

public class InsufficientWorkersException extends RuntimeException {
    public InsufficientWorkersException(int required, int available) {
        super("Insufficient workers. Required: " + required + ", Available: " + available);
    }
}
