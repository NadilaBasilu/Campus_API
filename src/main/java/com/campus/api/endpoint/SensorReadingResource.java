/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.campus.api.endpoint;

/**
 *
 * @author Basilu
 */

import com.campus.api.exception.SensorUnavailableException;
import com.campus.api.model.Sensor;
import com.campus.api.model.SensorReading;
import com.campus.api.storage.CampusDataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final CampusDataStore store = CampusDataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadingHistory() {
        List<SensorReading> history = store.getReadings()
                .getOrDefault(sensorId, new ArrayList<>());
        return Response.ok(history).build();
    }

    
    @POST
    public Response recordReading(SensorReading incoming) {
        Sensor sensor = store.getSensors().get(sensorId);

        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Sensor not found: " + sensorId))
                    .build();
        }

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor " + sensorId + " is currently under maintenance "
                + "and cannot accept new readings.");
        }

        SensorReading recorded = new SensorReading(incoming.getValue());

        store.getReadings()
             .computeIfAbsent(sensorId, k -> new ArrayList<>())
             .add(recorded);

        
        sensor.setCurrentValue(recorded.getValue());

        return Response.status(Response.Status.CREATED).entity(recorded).build();
    }
}
