package com.westminster.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> discover() {
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("apiName", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("contact", contact());
        response.put("resources", resources());
        response.put("timestamp", System.currentTimeMillis());

        return response;
    }

    private Map<String, Object> contact() {
        Map<String, Object> contact = new LinkedHashMap<>();
        contact.put("name", "Pavithma");
        contact.put("email", "pavithma.20240349@iit.ac.lk");
        contact.put("team", "Smart Campus Backend");
        return contact;
    }

    private Map<String, Object> resources() {
        Map<String, Object> resources = new LinkedHashMap<>();

        resources.put("rooms", link("/api/v1/rooms", "GET", "POST"));
        resources.put("roomById", link("/api/v1/rooms/{roomId}", "GET", "DELETE"));

        Map<String, Object> sensors = link("/api/v1/sensors", "GET", "POST");
        sensors.put("queryParams", List.of("type"));
        resources.put("sensors", sensors);

        resources.put("sensorReadings", link("/api/v1/sensors/{sensorId}/readings", "GET", "POST"));

        return resources;
    }

    private Map<String, Object> link(String href, String... methods) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("href", href);
        entry.put("methods", Arrays.asList(methods));
        return entry;
    }
}
