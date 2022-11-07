package com.udacity.catpoint.securitymodule;

import com.udacity.catpoint.securitymodule.data.Sensor;
import com.udacity.catpoint.securitymodule.data.SensorType;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestUtils {

    public static Set<Sensor> getSensors(int alarmCount, boolean isActive) {
        return IntStream.range(1, alarmCount + 1)
                .mapToObj(i -> new Sensor("sensor_" + i, SensorType.DOOR, isActive))
                .collect(Collectors.toSet());
    }
}
