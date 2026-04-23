package com.westminster.smartcampus.store;

import com.westminster.smartcampus.model.Sensor;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class SensorStore {

    private static final SensorStore INSTANCE = new SensorStore();

    private final ConcurrentHashMap<String, Sensor> store = new ConcurrentHashMap<>();

    private SensorStore() {
        seed();
    }

    public static SensorStore getInstance() {
        return INSTANCE;
    }

    private void seed() {
        Sensor temp001 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor co2007 = new Sensor("CO2-007", "CO2", "MAINTENANCE", 0.0, "LAB-101");
        store.put(temp001.getId(), temp001);
        store.put(co2007.getId(), co2007);
    }

    public Collection<Sensor> findAll() {
        return store.values();
    }

    public Sensor findById(String id) {
        return store.get(id);
    }

    public Sensor save(Sensor sensor) {
        store.put(sensor.getId(), sensor);
        return sensor;
    }

    public boolean delete(String id) {
        return store.remove(id) != null;
    }
}
