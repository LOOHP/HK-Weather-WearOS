package com.loohp.hkweatherwarnings.weather;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class WeatherInfo {

    private final LocalDate date;
    private final float highestTemperature;
    private final float lowestTemperature;
    private final float maxRelativeHumidity;
    private final float minRelativeHumidity;
    private final float chanceOfRain;
    private final WeatherStatusIcon weatherIcon;

    public WeatherInfo(LocalDate date, float highestTemperature, float lowestTemperature, float maxRelativeHumidity, float minRelativeHumidity, float chanceOfRain, WeatherStatusIcon weatherIcon) {
        this.date = date;
        this.highestTemperature = highestTemperature;
        this.lowestTemperature = lowestTemperature;
        this.maxRelativeHumidity = maxRelativeHumidity;
        this.minRelativeHumidity = minRelativeHumidity;
        this.chanceOfRain = chanceOfRain;
        this.weatherIcon = weatherIcon;
    }

    public LocalDate getDate() {
        return date;
    }

    public DayOfWeek getDayOfWeek() {
        return date.getDayOfWeek();
    }

    public float getHighestTemperature() {
        return highestTemperature;
    }

    public float getLowestTemperature() {
        return lowestTemperature;
    }

    public float getMaxRelativeHumidity() {
        return maxRelativeHumidity;
    }

    public float getMinRelativeHumidity() {
        return minRelativeHumidity;
    }

    public float getChanceOfRain() {
        return chanceOfRain;
    }

    public WeatherStatusIcon getWeatherIcon() {
        return weatherIcon;
    }
}
