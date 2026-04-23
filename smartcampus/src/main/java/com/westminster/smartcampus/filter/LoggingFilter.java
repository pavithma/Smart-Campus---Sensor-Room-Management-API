package com.westminster.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    private static final String PROP_REQUEST_ID  = "requestId";
    private static final String PROP_START_TIME  = "startTime";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        requestContext.setProperty(PROP_REQUEST_ID, requestId);
        requestContext.setProperty(PROP_START_TIME, startTime);

        LOGGER.log(Level.INFO, "--> {0} {1}",
                new Object[]{requestContext.getMethod(), requestContext.getUriInfo().getRequestUri()});
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        String requestId = (String) requestContext.getProperty(PROP_REQUEST_ID);
        Long startTime  = (Long)   requestContext.getProperty(PROP_START_TIME);
        long elapsed = startTime != null ? System.currentTimeMillis() - startTime : -1;

        LOGGER.log(Level.INFO, "<-- {0} {1} {2} [{3}] ({4}ms)",
                new Object[]{
                        responseContext.getStatus(),
                        requestContext.getMethod(),
                        requestContext.getUriInfo().getRequestUri(),
                        requestId,
                        elapsed
                });
    }
}
