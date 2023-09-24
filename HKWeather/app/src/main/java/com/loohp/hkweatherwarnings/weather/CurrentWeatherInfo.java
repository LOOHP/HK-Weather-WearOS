package com.loohp.hkweatherwarnings.weather;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

public class CurrentWeatherInfo extends WeatherInfo {

    private final String weatherStation;
    private final float currentTemperature;
    private final float currentHumidity;
    private final float uvIndex;
    private final String windDirection;
    private final float windSpeed;
    private final float gust;
    private final LocalTime sunriseTime;
    private final LocalTime sunTransitTime;
    private final LocalTime sunsetTime;
    private final LocalTime moonriseTime;
    private final LocalTime moonTransitTime;
    private final LocalTime moonsetTime;
    private final List<WeatherInfo> forecastInfo;
    private final List<HourlyWeatherInfo> hourlyWeatherInfo;

    public CurrentWeatherInfo(LocalDate date, float highestTemperature, float lowestTemperature, float maxRelativeHumidity, float minRelativeHumidity, float chanceOfRain, WeatherStatusIcon weatherIcon, String weatherStation, float currentTemperature, float currentHumidity, float uvIndex, String windDirection, float windSpeed, float gust, LocalTime sunriseTime, LocalTime sunTransitTime, LocalTime sunsetTime, LocalTime moonriseTime, LocalTime moonTransitTime, LocalTime moonsetTime, List<WeatherInfo> forecastInfo, List<HourlyWeatherInfo> hourlyWeatherInfo) {
        super(date, highestTemperature, lowestTemperature, maxRelativeHumidity, minRelativeHumidity, chanceOfRain, weatherIcon);
        this.weatherStation = weatherStation;
        this.currentTemperature = currentTemperature;
        this.currentHumidity = currentHumidity;
        this.uvIndex = uvIndex;
        this.windDirection = windDirection;
        this.windSpeed = windSpeed;
        this.gust = gust;
        this.sunriseTime = sunriseTime;
        this.sunTransitTime = sunTransitTime;
        this.sunsetTime = sunsetTime;
        this.moonriseTime = moonriseTime;
        this.moonTransitTime = moonTransitTime;
        this.moonsetTime = moonsetTime;
        this.forecastInfo = Collections.unmodifiableList(forecastInfo);
        this.hourlyWeatherInfo = Collections.unmodifiableList(hourlyWeatherInfo);
    }

    public String getWeatherStation() {
        return weatherStation;
    }

    public float getCurrentTemperature() {
        return currentTemperature;
    }

    public float getCurrentHumidity() {
        return currentHumidity;
    }

    public float getUvIndex() {
        return uvIndex;
    }

    public LocalTime getSunriseTime() {
        return sunriseTime;
    }

    public LocalTime getSunTransitTime() {
        return sunTransitTime;
    }

    public LocalTime getSunsetTime() {
        return sunsetTime;
    }

    public LocalTime getMoonriseTime() {
        return moonriseTime;
    }

    public LocalTime getMoonTransitTime() {
        return moonTransitTime;
    }

    public LocalTime getMoonsetTime() {
        return moonsetTime;
    }

    public String getWindDirection() {
        return windDirection;
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    public float getGust() {
        return gust;
    }

    public List<WeatherInfo> getForecastInfo() {
        return forecastInfo;
    }

    public List<HourlyWeatherInfo> getHourlyWeatherInfo() {
        return hourlyWeatherInfo;
    }
}
