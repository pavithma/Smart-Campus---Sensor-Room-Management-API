package com.westminster.smartcampus.store;

import com.westminster.smartcampus.model.Room;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class RoomStore {

    private static final RoomStore INSTANCE = new RoomStore();

    private final ConcurrentHashMap<String, Room> store = new ConcurrentHashMap<>();

    private RoomStore() {
        seed();
    }

    public static RoomStore getInstance() {
        return INSTANCE;
    }

    private void seed() {
        Room lib301 = new Room("LIB-301", "Library Room 301", 50,
                new ArrayList<>(Arrays.asList("TEMP-001")));
        Room lab101 = new Room("LAB-101", "Computer Lab 101", 30,
                new ArrayList<>(Arrays.asList("CO2-007")));
        store.put(lib301.getId(), lib301);
        store.put(lab101.getId(), lab101);
    }

    public Collection<Room> findAll() {
        return store.values();
    }

    public Room findById(String id) {
        return store.get(id);
    }

    public Room save(Room room) {
        store.put(room.getId(), room);
        return room;
    }

    public boolean delete(String id) {
        return store.remove(id) != null;
    }
}
