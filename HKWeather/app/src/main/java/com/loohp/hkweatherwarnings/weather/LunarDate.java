package com.loohp.hkweatherwarnings.weather;

import androidx.annotation.NonNull;

public class LunarDate {

    private final String year;
    private final String zodiac;
    private final String date;

    public LunarDate(String year, String zodiac, String date) {
        this.year = year;
        this.zodiac = zodiac;
        this.date = date;
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

    @NonNull
    @Override
    public String toString() {
        return year + ", " + zodiac + "年 " + date;
    }
}
