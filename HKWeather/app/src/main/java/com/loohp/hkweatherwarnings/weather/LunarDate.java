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
