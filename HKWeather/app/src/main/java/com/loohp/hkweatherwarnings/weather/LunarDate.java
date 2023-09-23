package com.loohp.hkweatherwarnings.weather;

import androidx.annotation.NonNull;

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
}
