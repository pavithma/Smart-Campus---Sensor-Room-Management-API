package com.westminster.smartcampus.store;

import com.westminster.smartcampus.model.SensorReading;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;


public class SensorReadingStore {

    private static final SensorReadingStore INSTANCE = new SensorReadingStore();

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SensorReading>> store =
            new ConcurrentHashMap<>();

    private SensorReadingStore() {
        seed();
    }

    public static SensorReadingStore getInstance() {
        return INSTANCE;
    }

    private void seed() {
        long now = System.currentTimeMillis();
        SensorReading r1 = new SensorReading("READ-001", now - 60_000, 22.5);
        SensorReading r2 = new SensorReading("READ-002", now, 23.1);
        addReading("TEMP-001", r1);
        addReading("TEMP-001", r2);
    }

    public List<SensorReading> findBySensorId(String sensorId) {
        CopyOnWriteArrayList<SensorReading> readings = store.get(sensorId);
        return readings != null ? readings : Collections.emptyList();
    }

    public void addReading(String sensorId, SensorReading reading) {
        store.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>()).add(reading);
    }

    public boolean deleteReading(String sensorId, String readingId) {
        CopyOnWriteArrayList<SensorReading> readings = store.get(sensorId);
        if (readings == null) return false;
        return readings.removeIf(r -> r.getId().equals(readingId));
    }

    public ConcurrentHashMap<String, CopyOnWriteArrayList<SensorReading>> getStore() {
        return store;
    }
}
