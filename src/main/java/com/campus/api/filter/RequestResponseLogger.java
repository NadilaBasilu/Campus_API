/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.api.filter;

/**
 *
 * @author Basilu
 */

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;


@Provider
public class RequestResponseLogger
        implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(RequestResponseLogger.class.getName());

    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        LOG.info(String.format(">>> Incoming  [%s] %s",
                req.getMethod(),
                req.getUriInfo().getRequestUri()));
    }

    @Override
    public void filter(ContainerRequestContext req,
                       ContainerResponseContext res) throws IOException {
        LOG.info(String.format("<<< Outgoing  [HTTP %d] for [%s] %s",
                res.getStatus(),
                req.getMethod(),
                req.getUriInfo().getRequestUri()));
    }
}
