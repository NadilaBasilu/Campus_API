/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.campus.api.endpoint;

/**
 *
 * @author Basilu
 */

import com.campus.api.exception.RoomNotEmptyException;
import com.campus.api.model.Room;
import com.campus.api.storage.CampusDataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Map;


@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorRoomResource {

    private final CampusDataStore store = CampusDataStore.getInstance();

    
    @GET
    public Response listAllRooms() {
        Collection<Room> rooms = store.getRooms().values();
        return Response.ok(rooms).build();
    }

    
    @GET
    @Path("/{id}")
    public Response fetchRoom(@PathParam("id") String id) {
        Room room = store.getRooms().get(id);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "No room found with id: " + id))
                    .build();
        }
        return Response.ok(room).build();
    }

    
    @POST
    public Response addRoom(Room room) {
        if (room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "id is required and cannot be blank"))
                    .build();
        }
        store.getRooms().put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    
    @DELETE
    @Path("/{id}")
    public Response removeRoom(@PathParam("id") String id) {
        Room room = store.getRooms().get(id);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "No room found with id: " + id))
                    .build();
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                "Cannot remove room " + id + " — it still has "
                + room.getSensorIds().size() + " sensor(s) assigned. "
                + "Reassign or delete all sensors first.");
        }
        store.getRooms().remove(id);
        return Response.noContent().build();
    }
}
