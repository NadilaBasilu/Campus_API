/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.campus.api.endpoint;
/**
 *
 * @author Basilu
 */

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;


@Path("/")
public class ApiInfoResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("api", "Campus Sensor Management API");
        info.put("version", "1.0");
        info.put("maintainer", "campus-admin@university.ac.uk");
        info.put("status", "running");

        Map<String, String> links = new LinkedHashMap<>();
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        info.put("endpoints", links);

        return Response.ok(info).build();
    }
}
