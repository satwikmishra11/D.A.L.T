package com.loadtest.model;

public enum ProfileType {
    CONSTANT,  // Fixed RPS
    RAMP,      // Gradual increase
    BURST,     // Sudden spikes
    SPIKE      // Single large spike
}
