package com.loadtest.exception;

public class ScenarioNotFoundException extends RuntimeException {
    public ScenarioNotFoundException(String id) {
        super("Scenario not found with ID: " + id);
    }
}
