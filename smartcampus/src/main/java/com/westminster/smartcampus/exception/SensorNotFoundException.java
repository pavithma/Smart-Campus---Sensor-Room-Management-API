package com.westminster.smartcampus.exception;

public class SensorNotFoundException extends RuntimeException {

    private final String sensorId;

    public SensorNotFoundException(String sensorId) {
        super("Sensor not found: " + sensorId);
        this.sensorId = sensorId;
    }

    public String getSensorId() {
        return sensorId;
    }
}
