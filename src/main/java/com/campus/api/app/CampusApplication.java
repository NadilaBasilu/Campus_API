/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.campus.api.app;

/**
 *
 * @author Basilu
 */


import com.campus.api.endpoint.ApiInfoResource;
import com.campus.api.endpoint.SensorRoomResource;
import com.campus.api.endpoint.SensorResource;
import com.campus.api.exception.RoomNotEmptyExceptionMapper;
import com.campus.api.exception.LinkedResourceNotFoundExceptionMapper;
import com.campus.api.exception.SensorUnavailableExceptionMapper;
import com.campus.api.exception.FallbackExceptionMapper;
import com.campus.api.filter.RequestResponseLogger;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;


@ApplicationPath("/api/v1")
public class CampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // Resources
        classes.add(ApiInfoResource.class);
        classes.add(SensorRoomResource.class);
        classes.add(SensorResource.class);

        // Exception mappers
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(FallbackExceptionMapper.class);

        // Filters
        classes.add(RequestResponseLogger.class);

        return classes;
    }
}
