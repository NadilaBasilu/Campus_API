/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.api.storage;

/**
 *
 * @author Basilu
 */

import com.campus.api.model.Room;
import com.campus.api.model.Sensor;
import com.campus.api.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class CampusDataStore {

    private static final CampusDataStore INSTANCE = new CampusDataStore();

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private CampusDataStore() {
        seedInitialData();
    }

    public static CampusDataStore getInstance() {
        return INSTANCE;
    }

    
    private void seedInitialData() {
        // Seed rooms
        Room lecture = new Room("LH-201", "Lecture Hall 201", 120);
        Room lab     = new Room("CS-LAB-05", "Computer Science Lab 5", 40);
        rooms.put(lecture.getId(), lecture);
        rooms.put(lab.getId(), lab);

        // Seed sensors
        Sensor tempSensor = new Sensor("TMP-101", "Temperature", "ACTIVE", 21.3, "LH-201");
        Sensor humSensor  = new Sensor("HUM-202", "Humidity",    "ACTIVE", 55.0, "CS-LAB-05");

        sensors.put(tempSensor.getId(), tempSensor);
        sensors.put(humSensor.getId(),  humSensor);

        // Link sensors to rooms
        lecture.getSensorIds().add(tempSensor.getId());
        lab.getSensorIds().add(humSensor.getId());

        // Initialise empty reading histories
        readings.put(tempSensor.getId(), new ArrayList<>());
        readings.put(humSensor.getId(),  new ArrayList<>());
    }

    public ConcurrentHashMap<String, Room> getRooms() {
        return rooms;
    }

    public ConcurrentHashMap<String, Sensor> getSensors() {
        return sensors;
    }

    public ConcurrentHashMap<String, List<SensorReading>> getReadings() {
        return readings;
    }
}
