package com.loohp.hkweatherwarnings.weather;

import java.time.LocalDateTime;

public class HourlyWeatherInfo {

    private final LocalDateTime time;
    private final float temperature;
    private final float humidity;
    private final float windDirection;
    private final float windSpeed;
    private final WeatherStatusIcon weatherIcon;

    public HourlyWeatherInfo(LocalDateTime time, float temperature, float humidity, float windDirection, float windSpeed, WeatherStatusIcon weatherIcon) {
        this.time = time;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windDirection = windDirection;
        this.windSpeed = windSpeed;
        this.weatherIcon = weatherIcon;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public float getTemperature() {
        return temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public float getWindDirection() {
        return windDirection;
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    public WeatherStatusIcon getWeatherIcon() {
        return weatherIcon;
    }
}
