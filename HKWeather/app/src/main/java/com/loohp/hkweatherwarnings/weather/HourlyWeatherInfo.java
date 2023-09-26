package com.loohp.hkweatherwarnings.weather;

import com.loohp.hkweatherwarnings.cache.JSONSerializable;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HourlyWeatherInfo implements JSONSerializable {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy.HH:mm");

    public static HourlyWeatherInfo deserialize(JSONObject jsonObject) {
        LocalDateTime time = LocalDateTime.parse(jsonObject.optString("time"), DATE_TIME_FORMATTER);
        float temperature = (float) jsonObject.optDouble("temperature");
        float humidity = (float) jsonObject.optDouble("humidity");
        float windDirection = (float) jsonObject.optDouble("windDirection");
        float windSpeed = (float) jsonObject.optDouble("windSpeed");
        WeatherStatusIcon weatherIcon = WeatherStatusIcon.valueOf(jsonObject.optString("weatherIcon"));
        return new HourlyWeatherInfo(time, temperature, humidity, windDirection, windSpeed, weatherIcon);
    }

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

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("time", time.format(DATE_TIME_FORMATTER));
        jsonObject.put("temperature", temperature);
        jsonObject.put("humidity", humidity);
        jsonObject.put("windDirection", windDirection);
        jsonObject.put("windSpeed", windSpeed);
        jsonObject.put("weatherIcon", weatherIcon.name());
        return jsonObject;
    }
}
