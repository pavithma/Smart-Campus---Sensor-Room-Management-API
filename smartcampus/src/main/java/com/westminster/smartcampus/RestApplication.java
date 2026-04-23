package com.westminster.smartcampus;

import com.westminster.smartcampus.filter.LoggingFilter;
import com.westminster.smartcampus.mapper.GenericThrowableMapper;
import com.westminster.smartcampus.mapper.LinkedResourceNotFoundExceptionMapper;
import com.westminster.smartcampus.mapper.NotFoundMapper;
import com.westminster.smartcampus.mapper.RoomNotEmptyExceptionMapper;
import com.westminster.smartcampus.mapper.RoomNotFoundExceptionMapper;
import com.westminster.smartcampus.mapper.SensorNotFoundExceptionMapper;
import com.westminster.smartcampus.mapper.SensorUnavailableExceptionMapper;
import com.westminster.smartcampus.resource.DiscoveryResource;
import com.westminster.smartcampus.resource.SensorRoomResource;
import com.westminster.smartcampus.resource.SensorResource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api/v1")
public class RestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        classes.add(DiscoveryResource.class);
        classes.add(SensorRoomResource.class);
        classes.add(SensorResource.class);

        classes.add(RoomNotFoundExceptionMapper.class);
        classes.add(SensorNotFoundExceptionMapper.class);
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(NotFoundMapper.class);
        classes.add(GenericThrowableMapper.class);

        classes.add(LoggingFilter.class);

        return classes;
    }
}
