package com.loohp.hkweatherwarnings.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

public class TimeUtils {

    public static LocalDateTime findClosestUnitInThePast(LocalDateTime currentDateTime, long interval, ChronoField timeUnit) {
        long unit = currentDateTime.getLong(timeUnit);
        long remainder = unit % interval;
        return currentDateTime.minus(remainder, timeUnit.getBaseUnit());
    }

}
