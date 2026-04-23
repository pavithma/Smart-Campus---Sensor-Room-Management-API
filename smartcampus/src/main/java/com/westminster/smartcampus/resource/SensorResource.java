package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.exception.LinkedResourceNotFoundException;
import com.westminster.smartcampus.exception.SensorNotFoundException;
import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.store.RoomStore;
import com.westminster.smartcampus.store.SensorStore;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.stream.Collectors;

@Path("sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SensorStore sensorStore = SensorStore.getInstance();
    private final RoomStore roomStore = RoomStore.getInstance();

    @GET
    public Collection<Sensor> listAll(@QueryParam("type") String type) {
        Collection<Sensor> all = sensorStore.findAll();
        if (type == null || type.isBlank()) {
            return all;
        }
        return all.stream()
                .filter(s -> type.equalsIgnoreCase(s.getType()))
                .collect(Collectors.toList());
    }

    @GET
    @Path("{sensorId}")
    public Sensor getById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensorStore.findById(sensorId);
        if (sensor == null) {
            throw new SensorNotFoundException(sensorId);
        }
        return sensor;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(Sensor sensor, @Context UriInfo uriInfo) {
        if (sensor == null) {
            return Response.status(400).entity(
                    java.util.Map.of("error", "BAD_REQUEST", "message", "Request body is missing or not valid JSON")
            ).build();
        }

        if (sensor.getType() == null || sensor.getType().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(
                    java.util.Map.of("error", "VALIDATION_ERROR", "field", "type", "message", "Sensor type must not be null or blank")
            ).type(MediaType.APPLICATION_JSON).build();
        }

        String roomId = sensor.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            throw new LinkedResourceNotFoundException("roomId", roomId);
        }
        Room room = roomStore.findById(roomId);
        if (room == null) {
            throw new LinkedResourceNotFoundException("roomId", roomId);
        }

        if (sensor.getId() == null || sensor.getId().isBlank()) {
            sensor.setId(generateId());
        }

        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        sensorStore.save(sensor);
        room.getSensorIds().add(sensor.getId());

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(sensor.getId())
                .build();

        return Response.created(location).entity(sensor).build();
    }

    @Path("{sensorId}/readings")
    public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
        if (sensorStore.findById(sensorId) == null) {
            throw new SensorNotFoundException(sensorId);
        }
        return new SensorReadingResource(sensorId);
    }

    private String generateId() {
        StringBuilder sb = new StringBuilder("SENS-");
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
