package com.loohp.hkweatherwarnings.weather;

import android.content.Context;

import com.loohp.hkweatherwarnings.shared.Registry;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

public class ForecastWeatherInfo extends WeatherInfo {

    public static ForecastWeatherInfo deserialize(JSONObject jsonObject) {
        LocalDate date = LocalDate.parse(jsonObject.optString("date"), DATE_FORMATTER);
        float highestTemperature = (float) jsonObject.optDouble("highestTemperature");
        float lowestTemperature = (float) jsonObject.optDouble("lowestTemperature");
        float maxRelativeHumidity = (float) jsonObject.optDouble("maxRelativeHumidity");
        float minRelativeHumidity = (float) jsonObject.optDouble("minRelativeHumidity");
        float chanceOfRain = (float) jsonObject.optDouble("chanceOfRain");
        WeatherStatusIcon weatherIcon = WeatherStatusIcon.valueOf(jsonObject.optString("weatherIcon"));
        String forecastWind = jsonObject.optString("forecastWind");
        String forecastWeather = jsonObject.optString("forecastWeather");
        return new ForecastWeatherInfo(date, highestTemperature, lowestTemperature, maxRelativeHumidity, minRelativeHumidity, chanceOfRain, weatherIcon, forecastWind, forecastWeather);
    }

    private final String forecastWind;
    private final String forecastWeather;

    public ForecastWeatherInfo(LocalDate date, float highestTemperature, float lowestTemperature, float maxRelativeHumidity, float minRelativeHumidity, float chanceOfRain, WeatherStatusIcon weatherIcon, String forecastWind, String forecastWeather) {
        super(date, highestTemperature, lowestTemperature, maxRelativeHumidity, minRelativeHumidity, chanceOfRain, weatherIcon);
        this.forecastWind = forecastWind;
        this.forecastWeather = forecastWeather;
    }

    public String toDisplayText(Context context) {
        StringBuilder sb = new StringBuilder();
        if (Registry.getInstance(context).getLanguage().equals("en")) {
            sb.append(getDate().format(DateTimeFormatter.ofPattern("dd MMM")))
                    .append(" (").append(getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)).append(")\n")
                    .append(getWeatherIcon().getDescriptionEn()).append("\n")
                    .append("Temperature: ").append(String.format(Locale.ENGLISH, "%.0f", getLowestTemperature())).append(" - ")
                    .append(String.format(Locale.ENGLISH, "%.0f", getHighestTemperature())).append("°C\n")
                    .append("Relative Humidity: ").append(String.format(Locale.ENGLISH, "%.0f", getMinRelativeHumidity())).append(" - ")
                    .append(String.format(Locale.ENGLISH, "%.0f", getMaxRelativeHumidity())).append("%\n")
                    .append("Chance of Rain: ").append(String.format(Locale.ENGLISH, "%.0f", getMinRelativeHumidity())).append("%\n")
                    .append(forecastWind).append("\n")
                    .append(forecastWeather);
        } else {
            sb.append(getDate().format(DateTimeFormatter.ofPattern("M月dd日")))
                    .append(" (").append(getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.TRADITIONAL_CHINESE)).append(")\n")
                    .append(getWeatherIcon().getDescriptionZh()).append("\n")
                    .append("氣溫: ").append(String.format(Locale.TRADITIONAL_CHINESE, "%.0f", getLowestTemperature())).append(" - ")
                    .append(String.format(Locale.TRADITIONAL_CHINESE, "%.0f", getHighestTemperature())).append("°C\n")
                    .append("相對濕度: ").append(String.format(Locale.TRADITIONAL_CHINESE, "%.0f", getMinRelativeHumidity())).append(" - ")
                    .append(String.format(Locale.TRADITIONAL_CHINESE, "%.0f", getMaxRelativeHumidity())).append("%\n")
                    .append("降雨概率: ").append(String.format(Locale.TRADITIONAL_CHINESE, "%.0f", getMinRelativeHumidity())).append("%\n")
                    .append(forecastWind).append("\n")
                    .append(forecastWeather);
        }
        return sb.toString();
    }

    public String getForecastWind() {
        return forecastWind;
    }

    public String getForecastWeather() {
        return forecastWeather;
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject jsonObject = super.serialize();
        jsonObject.put("forecastWind", forecastWind);
        jsonObject.put("forecastWeather", forecastWeather);
        return jsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ForecastWeatherInfo that = (ForecastWeatherInfo) o;
        return Objects.equals(forecastWind, that.forecastWind) && Objects.equals(forecastWeather, that.forecastWeather);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), forecastWind, forecastWeather);
    }
}
