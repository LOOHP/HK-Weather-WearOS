/*
 * This file is part of HKWeather.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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

import androidx.annotation.NonNull;
import androidx.compose.runtime.Immutable;

import java.util.Objects;

@Immutable
public class LunarDate {

    private final String year;
    private final String zodiac;
    private final String date;
    private final String climatology;

    public LunarDate(String year, String zodiac, String date, String climatology) {
        this.year = year;
        this.zodiac = zodiac;
        this.date = date;
        this.climatology = climatology;
    }

    public String getYear() {
        return year;
    }

    public String getZodiac() {
        return zodiac;
    }

    public String getDate() {
        return date;
    }

    public boolean hasClimatology() {
        return climatology != null;
    }

    public String getClimatology() {
        return climatology;
    }

    @NonNull
    @Override
    public String toString() {
        return year + ", " + zodiac + "年 " + date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LunarDate lunarDate = (LunarDate) o;
        return Objects.equals(year, lunarDate.year) && Objects.equals(zodiac, lunarDate.zodiac) && Objects.equals(date, lunarDate.date) && Objects.equals(climatology, lunarDate.climatology);
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, zodiac, date, climatology);
    }
}
