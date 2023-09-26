package com.loohp.hkweatherwarnings.weather;

import com.loohp.hkweatherwarnings.cache.JSONSerializable;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class WeatherInfo implements JSONSerializable {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static WeatherInfo deserialize(JSONObject jsonObject) {
        LocalDate date = LocalDate.parse(jsonObject.optString("date"), DATE_FORMATTER);
        float highestTemperature = (float) jsonObject.optDouble("highestTemperature");
        float lowestTemperature = (float) jsonObject.optDouble("lowestTemperature");
        float maxRelativeHumidity = (float) jsonObject.optDouble("maxRelativeHumidity");
        float minRelativeHumidity = (float) jsonObject.optDouble("minRelativeHumidity");
        float chanceOfRain = (float) jsonObject.optDouble("chanceOfRain");
        WeatherStatusIcon weatherIcon = WeatherStatusIcon.valueOf(jsonObject.optString("weatherIcon"));
        return new WeatherInfo(date, highestTemperature, lowestTemperature, maxRelativeHumidity, minRelativeHumidity, chanceOfRain, weatherIcon);
    }

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

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("date", date.format(DATE_FORMATTER));
        jsonObject.put("highestTemperature", highestTemperature);
        jsonObject.put("lowestTemperature", lowestTemperature);
        jsonObject.put("maxRelativeHumidity", maxRelativeHumidity);
        jsonObject.put("minRelativeHumidity", minRelativeHumidity);
        jsonObject.put("chanceOfRain", chanceOfRain);
        jsonObject.put("weatherIcon", weatherIcon.name());
        return jsonObject;
    }
}
