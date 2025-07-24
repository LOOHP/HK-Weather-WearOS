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

import androidx.compose.runtime.Immutable;

import com.loohp.hkweatherwarnings.cache.JSONSerializable;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

@Immutable
public abstract class WeatherInfo implements JSONSerializable {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final LocalDate date;
    private final float highestTemperature;
    private final float lowestTemperature;
    private final float maxRelativeHumidity;
    private final float minRelativeHumidity;
    private final float chanceOfRain;
    private final RangeSign chanceOfRainRangeSign;
    private final WeatherStatusIcon weatherIcon;

    public WeatherInfo(LocalDate date, float highestTemperature, float lowestTemperature, float maxRelativeHumidity, float minRelativeHumidity, float chanceOfRain, RangeSign chanceOfRainRangeSign, WeatherStatusIcon weatherIcon) {
        this.date = date;
        this.highestTemperature = highestTemperature;
        this.lowestTemperature = lowestTemperature;
        this.maxRelativeHumidity = maxRelativeHumidity;
        this.minRelativeHumidity = minRelativeHumidity;
        this.chanceOfRain = chanceOfRain;
        this.chanceOfRainRangeSign = chanceOfRainRangeSign;
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

    public RangeSign getChanceOfRainRangeSign() {
        return chanceOfRainRangeSign;
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
        jsonObject.put("chanceOfRainRangeSign", chanceOfRainRangeSign.name());
        jsonObject.put("weatherIcon", weatherIcon.name());
        return jsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeatherInfo that = (WeatherInfo) o;
        return Float.compare(highestTemperature, that.highestTemperature) == 0 && Float.compare(lowestTemperature, that.lowestTemperature) == 0 && Float.compare(maxRelativeHumidity, that.maxRelativeHumidity) == 0 && Float.compare(minRelativeHumidity, that.minRelativeHumidity) == 0 && Float.compare(chanceOfRain, that.chanceOfRain) == 0 && chanceOfRainRangeSign == that.chanceOfRainRangeSign && Objects.equals(date, that.date) && weatherIcon == that.weatherIcon;
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, highestTemperature, lowestTemperature, maxRelativeHumidity, minRelativeHumidity, chanceOfRain, chanceOfRainRangeSign, weatherIcon);
    }

    public enum RangeSign {
        NONE(null), LARGER_THAN(">"), SMALLER_THAN("<");

        private final String IMPOSSIBLE_MATCH = UUID.randomUUID().toString();
        private final String symbol;

        RangeSign(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol == null ? IMPOSSIBLE_MATCH : symbol;
        }

        public String getSymbolWithSpace() {
            return symbol == null ? "" : (symbol + " ");
        }
    }
}
