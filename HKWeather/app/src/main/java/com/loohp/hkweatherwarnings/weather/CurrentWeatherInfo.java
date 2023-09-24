package com.loohp.hkweatherwarnings.weather;

import com.loohp.hkweatherwarnings.shared.Shared;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

public class CurrentWeatherInfo extends WeatherInfo {

    private final String weatherStation;
    private final float currentTemperature;
    private final float currentHumidity;
    private final float uvIndex;
    private final LocalTime sunriseTime;
    private final LocalTime sunTransitTime;
    private final LocalTime sunsetTime;
    private final List<WeatherInfo> forecastInfo;
    private final List<HourlyWeatherInfo> hourlyWeatherInfo;

    public CurrentWeatherInfo(LocalDate date, float highestTemperature, float lowestTemperature, float maxRelativeHumidity, float minRelativeHumidity, float chanceOfRain, WeatherStatusIcon weatherIcon, String weatherStation, float currentTemperature, float currentHumidity, float uvIndex, LocalTime sunriseTime, LocalTime sunTransitTime, LocalTime sunsetTime, List<WeatherInfo> forecastInfo, List<HourlyWeatherInfo> hourlyWeatherInfo) {
        super(date, highestTemperature, lowestTemperature, maxRelativeHumidity, minRelativeHumidity, chanceOfRain, weatherIcon);
        this.weatherStation = weatherStation;
        this.currentTemperature = currentTemperature;
        this.currentHumidity = currentHumidity;
        this.uvIndex = uvIndex;
        this.sunriseTime = sunriseTime;
        this.sunTransitTime = sunTransitTime;
        this.sunsetTime = sunsetTime;
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

    public HourlyWeatherInfo getCurrentHourlyWeatherInfo() {
        LocalDateTime now = LocalDateTime.now(Shared.Companion.getHK_TIMEZONE().toZoneId());
        LocalDateTime nowHourStart = now.withMinute(0);
        LocalDateTime nowHourEnd = now.plusHours(1);
        return hourlyWeatherInfo.stream().filter(h -> {
            LocalDateTime time = h.getTime();
            return (nowHourStart.isEqual(time) || nowHourStart.isBefore(time)) && nowHourEnd.isAfter(time);
        }).findFirst().orElse(null);
    }

    public float getWindDirection() {
        HourlyWeatherInfo hourly = getCurrentHourlyWeatherInfo();
        if (hourly == null) {
            return -1F;
        }
        return hourly.getWindDirection();
    }

    public float getWindSpeed() {
        HourlyWeatherInfo hourly = getCurrentHourlyWeatherInfo();
        if (hourly == null) {
            return -1F;
        }
        return hourly.getWindSpeed();
    }

    public List<WeatherInfo> getForecastInfo() {
        return forecastInfo;
    }

    public List<HourlyWeatherInfo> getHourlyWeatherInfo() {
        return hourlyWeatherInfo;
    }
}
