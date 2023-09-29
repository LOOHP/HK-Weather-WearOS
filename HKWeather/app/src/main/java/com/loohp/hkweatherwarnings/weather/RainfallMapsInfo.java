package com.loohp.hkweatherwarnings.weather;

import androidx.compose.runtime.Immutable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

@Immutable
public class RainfallMapsInfo {

    private final Map<LocalDateTime, String> past1HourUrls;
    private final String past24HoursUrl;
    private final String todayUrl;
    private final String yesterdayUrl;

    public RainfallMapsInfo(Map<LocalDateTime, String> past1HourUrls, String past24HoursUrl, String todayUrl, String yesterdayUrl) {
        this.past1HourUrls = Collections.unmodifiableMap(past1HourUrls instanceof SortedMap ? past1HourUrls : new TreeMap<>(past1HourUrls));
        this.past24HoursUrl = past24HoursUrl;
        this.todayUrl = todayUrl;
        this.yesterdayUrl = yesterdayUrl;
    }

    public Map<LocalDateTime, String> getPast1HourUrls() {
        return past1HourUrls;
    }

    public String getPast24HoursUrl() {
        return past24HoursUrl;
    }

    public String getTodayUrl() {
        return todayUrl;
    }

    public String getYesterdayUrl() {
        return yesterdayUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RainfallMapsInfo that = (RainfallMapsInfo) o;
        return Objects.equals(past1HourUrls, that.past1HourUrls) && Objects.equals(past24HoursUrl, that.past24HoursUrl) && Objects.equals(todayUrl, that.todayUrl) && Objects.equals(yesterdayUrl, that.yesterdayUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(past1HourUrls, past24HoursUrl, todayUrl, yesterdayUrl);
    }
}
