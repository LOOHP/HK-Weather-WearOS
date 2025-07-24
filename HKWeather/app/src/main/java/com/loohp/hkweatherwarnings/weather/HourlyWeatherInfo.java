/*
 * This file is part of HKWeather.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.hkweatherwarnings.weather;

import android.content.Context;

import androidx.compose.runtime.Immutable;

import com.loohp.hkweatherwarnings.cache.JSONSerializable;
import com.loohp.hkweatherwarnings.shared.Registry;
import com.loohp.hkweatherwarnings.utils.CompassUtilsKtKt;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

@Immutable
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

    public String toDisplayText(Context context) {
        DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(context);
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern(dateFormat instanceof SimpleDateFormat ? ((SimpleDateFormat) dateFormat).toPattern(): "HH:mm");
        StringBuilder sb = new StringBuilder();
        if (Registry.getInstance(context).getLanguage().equals("en")) {
            sb.append(time.format(DateTimeFormatter.ofPattern("dd MMM"))).append(" ").append(timeFormat.format(time)).append("\n")
                    .append(getWeatherIcon().getDescriptionEn()).append("\n")
                    .append("Temperature: ").append(String.format(Locale.ENGLISH, "%.1f", temperature)).append("°C\n")
                    .append("Relative Humidity: ").append(String.format(Locale.ENGLISH, "%.0f", humidity)).append("%\n")
                    .append("Wind Direction: ").append(CompassUtilsKtKt.toCardinalDirectionString(windDirection, Registry.getInstance(context).getLanguage())).append("\n")
                    .append("Wind Speed: ").append(String.format(Locale.ENGLISH, "%.1f", windSpeed)).append(" km/h");
        } else {
            sb.append(time.format(DateTimeFormatter.ofPattern("M月dd日"))).append(" ").append(timeFormat.format(time)).append("\n")
                    .append(getWeatherIcon().getDescriptionZh()).append("\n")
                    .append("氣溫: ").append(String.format(Locale.TRADITIONAL_CHINESE, "%.1f", temperature)).append("°C\n")
                    .append("相對濕度: ").append(String.format(Locale.TRADITIONAL_CHINESE, "%.0f", humidity)).append("%\n")
                    .append("風向: ").append(CompassUtilsKtKt.toCardinalDirectionString(windDirection, Registry.getInstance(context).getLanguage())).append("\n")
                    .append("風速: ").append(String.format(Locale.TRADITIONAL_CHINESE, "%.1f", windSpeed)).append("公里/小時");
        }
        return sb.toString();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HourlyWeatherInfo that = (HourlyWeatherInfo) o;
        return Float.compare(that.temperature, temperature) == 0 && Float.compare(that.humidity, humidity) == 0 && Float.compare(that.windDirection, windDirection) == 0 && Float.compare(that.windSpeed, windSpeed) == 0 && time.equals(that.time) && weatherIcon == that.weatherIcon;
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, temperature, humidity, windDirection, windSpeed, weatherIcon);
    }
}
