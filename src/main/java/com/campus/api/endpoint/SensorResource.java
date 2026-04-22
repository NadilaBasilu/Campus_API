/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.campus.api.endpoint;

/**
 *
 * @author Basilu
 */

import com.campus.api.exception.LinkedResourceNotFoundException;
import com.campus.api.model.Sensor;
import com.campus.api.storage.CampusDataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final CampusDataStore store = CampusDataStore.getInstance();

    
    @GET
    public Response listSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = store.getSensors().values();

        if (type == null || type.isBlank()) {
            return Response.ok(all).build();
        }

        List<Sensor> filtered = new ArrayList<>();
        for (Sensor sensor : all) {
            if (type.equalsIgnoreCase(sensor.getType())) {
                filtered.add(sensor);
            }
        }
        return Response.ok(filtered).build();
    }

    
    @GET
    @Path("/{id}")
    public Response fetchSensor(@PathParam("id") String id) {
        Sensor sensor = store.getSensors().get(id);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "No sensor found with id: " + id))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    
    @POST
    public Response registerSensor(Sensor sensor) {
        String roomId = sensor.getRoomId();
        if (roomId == null || !store.getRooms().containsKey(roomId)) {
            throw new LinkedResourceNotFoundException(
                "Cannot register sensor: room '" + roomId + "' does not exist in the system.");
        }
        store.getSensors().put(sensor.getId(), sensor);
        store.getRooms().get(roomId).getSensorIds().add(sensor.getId());
        store.getReadings().put(sensor.getId(), new ArrayList<>());
        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    
    @Path("/{id}/readings")
    public SensorReadingResource getReadingsResource(@PathParam("id") String id) {
        return new SensorReadingResource(id);
    }
}
