/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.campus.api.model;

/**
 *
 * @author Basilu
 */


public class Sensor {

    private String id;             // Unique sensor identifier e.g. "HUM-003"
    private String type;           // Sensor type e.g. "Humidity", "Temperature", "CO2"
    private String status;         // "ACTIVE", "MAINTENANCE", or "OFFLINE"
    private double currentValue;   // Most recent value recorded by this sensor
    private String roomId;         // Room ID where this sensor is installed

    public Sensor() {}

    public Sensor(String id, String type, String status,
                  double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
}
