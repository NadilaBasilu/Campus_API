/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.campus.api.exception;

/**
 *
 * @author Basilu
 */

import com.campus.api.model.ApiError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;


@Provider
public class FallbackExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(FallbackExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        LOG.log(Level.SEVERE, "Unhandled exception intercepted by FallbackExceptionMapper", ex);
        return Response.status(500)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiError(500, "Internal Server Error",
                        "An unexpected error occurred. Please contact the system administrator."))
                .build();
    }
}
