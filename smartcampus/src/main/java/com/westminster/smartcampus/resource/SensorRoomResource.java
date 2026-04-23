package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.exception.RoomNotFoundException;
import com.westminster.smartcampus.exception.RoomNotEmptyException;
import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.store.RoomStore;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;


@Path("rooms")
@Produces(MediaType.APPLICATION_JSON)
public class SensorRoomResource {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RoomStore store = RoomStore.getInstance();

    @GET
    public Collection<Room> listAll() {
        return store.findAll();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(Room room, @Context UriInfo uriInfo) {
        if (room == null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "BAD_REQUEST");
            err.put("message", "Request body is missing or not valid JSON");
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(err).build();
        }

        if (room.getName() == null || room.getName().isBlank()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "VALIDATION_ERROR");
            err.put("field", "name");
            err.put("message", "Room name must not be null or blank");
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(err).build();
        }

        if (room.getCapacity() <= 0) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "VALIDATION_ERROR");
            err.put("field", "capacity");
            err.put("message", "Room capacity must be greater than zero");
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(err).build();
        }

        if (room.getId() == null || room.getId().isBlank()) {
            room.setId(generateId());
        }

        if (store.findById(room.getId()) != null) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "CONFLICT");
            err.put("message", "A room with id '" + room.getId() + "' already exists");
            err.put("roomId", room.getId());
            return Response.status(Response.Status.CONFLICT)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(err).build();
        }

        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }

        store.save(room);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(room.getId())
                .build();

        return Response.created(location).entity(room).build();
    }

    @GET
    @Path("{roomId}")
    public Room getById(@PathParam("roomId") String roomId) {
        Room room = store.findById(roomId);
        if (room == null) {
            throw new RoomNotFoundException(roomId);
        }
        return room;
    }

    @DELETE
    @Path("{roomId}")
    public Response delete(@PathParam("roomId") String roomId) {
        Room room = store.findById(roomId);
        if (room == null) {
            throw new RoomNotFoundException(roomId);
        }

        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }

        store.delete(roomId);
        return Response.noContent().build();
    }

    private String generateId() {
        StringBuilder sb = new StringBuilder("ROOM-");
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
