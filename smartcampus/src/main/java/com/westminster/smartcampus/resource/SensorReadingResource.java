package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.exception.SensorUnavailableException;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.model.SensorReading;
import com.westminster.smartcampus.store.SensorReadingStore;
import com.westminster.smartcampus.store.SensorStore;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.logging.Logger;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final SensorStore sensorStore = SensorStore.getInstance();
    private final SensorReadingStore readingStore = SensorReadingStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public List<SensorReading> listReadings() {
        return readingStore.findBySensorId(sensorId).stream()
                .sorted(Comparator.comparingLong(SensorReading::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading body, @Context UriInfo uriInfo) {
        Sensor sensor = sensorStore.findById(sensorId);

        String status = sensor.getStatus();
        if ("MAINTENANCE".equalsIgnoreCase(status) || "OFFLINE".equalsIgnoreCase(status)) {
            throw new SensorUnavailableException(sensorId, status);
        }

        SensorReading reading = new SensorReading(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                body.getValue()
        );

        readingStore.addReading(sensorId, reading);
        sensor.setCurrentValue(reading.getValue());

        URI location = uriInfo.getBaseUriBuilder()
                .path("sensors")
                .path(sensorId)
                .path("readings")
                .path(reading.getId())
                .build();

        return Response.created(location).entity(reading).build();
    }
}
