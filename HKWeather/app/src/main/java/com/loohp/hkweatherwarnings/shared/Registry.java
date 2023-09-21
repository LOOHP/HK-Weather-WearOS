package com.loohp.hkweatherwarnings.shared;

import android.content.Context;
import android.location.Location;
import android.util.Pair;

import androidx.wear.tiles.TileService;

import com.google.common.util.concurrent.AtomicDouble;
import com.loohp.hkweatherwarnings.R;
import com.loohp.hkweatherwarnings.tiles.WeatherOverviewTile;
import com.loohp.hkweatherwarnings.tiles.WeatherTipsTile;
import com.loohp.hkweatherwarnings.tiles.WeatherWarningsTile;
import com.loohp.hkweatherwarnings.utils.ConnectionUtils;
import com.loohp.hkweatherwarnings.utils.HTTPRequestUtils;
import com.loohp.hkweatherwarnings.utils.JsonUtils;
import com.loohp.hkweatherwarnings.utils.LocationUtils;
import com.loohp.hkweatherwarnings.weather.CurrentWeatherInfo;
import com.loohp.hkweatherwarnings.weather.HourlyWeatherInfo;
import com.loohp.hkweatherwarnings.weather.WeatherInfo;
import com.loohp.hkweatherwarnings.weather.WeatherStatusIcon;
import com.loohp.hkweatherwarnings.weather.WeatherWarningsType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Registry {

    private static Registry INSTANCE = null;

    public static synchronized Registry getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new Registry(context);
        }
        return INSTANCE;
    }

    private static final String PREFERENCES_FILE_NAME = "preferences.json";

    private static JSONObject PREFERENCES = null;

    private static List<JSONObject> WEATHER_STATIONS = null;
    private static JSONObject FORECAST_STATIONS = null;

    private Registry(Context context) {
        try {
            ensureData(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateTileService(Context context) {
        TileService.getUpdater(context).requestUpdate(WeatherOverviewTile.class);
        TileService.getUpdater(context).requestUpdate(WeatherWarningsTile.class);
        TileService.getUpdater(context).requestUpdate(WeatherTipsTile.class);
    }

    public void setLanguage(String language, Context context) {
        try {
            if (PREFERENCES == null) {
                PREFERENCES = new JSONObject();
            }
            PREFERENCES.put("language", language);
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                pw.write(PREFERENCES.toString());
                pw.flush();
            }
            updateTileService(context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLanguage() {
        if (PREFERENCES == null) {
            return "zh";
        }
        String language = PREFERENCES.optString("language");
        if (language.isEmpty()) {
            return "zh";
        }
        return language;
    }

    public Pair<String, Location> getLocation() {
        if (PREFERENCES == null) {
            return Pair.create("", null);
        }
        Object location = PREFERENCES.opt("location");
        if (location == null) {
            return Pair.create("", null);
        }
        if (location instanceof String) {
            return Pair.create((String) location, null);
        }
        JSONArray pos = (JSONArray) location;
        return Pair.create("", LocationUtils.LocationResult.fromLatLng(pos.optDouble(0), pos.optDouble(1)).getLocation());
    }

    public void setLocation(Location location, Context context) {
        try {
            if (PREFERENCES == null) {
                PREFERENCES = new JSONObject();
            }
            PREFERENCES.put("location", new JSONArray(Arrays.asList(location.getLatitude(), location.getLongitude())));
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                pw.write(PREFERENCES.toString());
                pw.flush();
            }
            updateTileService(context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void setLocationGPS(Context context) {
        try {
            if (PREFERENCES == null) {
                PREFERENCES = new JSONObject();
            }
            PREFERENCES.put("location", "GPS");
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                pw.write(PREFERENCES.toString());
                pw.flush();
            }
            updateTileService(context);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearLocation(Context context) {
        try {
            if (PREFERENCES == null) {
                return;
            }
            PREFERENCES.remove("location");
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(context.getApplicationContext().openFileOutput(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE), StandardCharsets.UTF_8))) {
                pw.write(PREFERENCES.toString());
                pw.flush();
            }
            updateTileService(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<JSONObject> getWeatherStations() {
        return WEATHER_STATIONS;
    }

    public double findDistance(double lat1, double lng1, double lat2, double lng2) {
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        lng1 = Math.toRadians(lng1);
        lng2 = Math.toRadians(lng2);

        double d_lon = lng2 - lng1;
        double d_lat = lat2 - lat1;
        double a = Math.pow(Math.sin(d_lat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(d_lon / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));

        return c * 6371;
    }

    private void ensureData(Context context) throws IOException {
        if (PREFERENCES != null) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.current_weather_report_weather_station), StandardCharsets.UTF_8))) {
            WEATHER_STATIONS = JsonUtils.toList(new JSONObject(reader.lines().collect(Collectors.joining())).optJSONArray("features"), JSONObject.class);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.forecast_stations), StandardCharsets.UTF_8))) {
            FORECAST_STATIONS = new JSONObject(reader.lines().collect(Collectors.joining())).optJSONObject("stations");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        List<String> files = Arrays.asList(context.getApplicationContext().fileList());
        if (files.contains(PREFERENCES_FILE_NAME)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getApplicationContext().openFileInput(PREFERENCES_FILE_NAME), StandardCharsets.UTF_8))) {
                PREFERENCES = new JSONObject(reader.lines().collect(Collectors.joining("\n")));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                PREFERENCES = new JSONObject();
                PREFERENCES.put("language", "zh");
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Future<CurrentWeatherInfo> getCurrentWeatherInfo(Context context, LocationUtils.LocationResult locationResult) {
        if (!ConnectionUtils.getConnectionType(context).hasConnection()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<CurrentWeatherInfo> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                LocalDate today = LocalDate.now(Shared.Companion.getHK_TIMEZONE().toZoneId());
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                DateTimeFormatter dateHourFormatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
                String lang = getLanguage().equals("en") ? "en" : "tc";
                Location location;
                String weatherStationName = null;
                if (locationResult != null && locationResult.isSuccess()) {
                    location = locationResult.getLocation();
                } else {
                    location = Shared.Companion.getDEFAULT_LOCATION().getLocation();
                    weatherStationName = getLanguage().equals("en") ? "Hong Kong" : "香港";
                }

                AtomicDouble minDistance = new AtomicDouble(Double.MAX_VALUE);
                JSONObject weatherStation = WEATHER_STATIONS.stream().min(Comparator.comparing(s -> {
                    JSONArray pos = s.optJSONObject("geometry").optJSONArray("coordinates");
                    double distance = findDistance(location.getLatitude(), location.getLongitude(), pos.optDouble(0), pos.optDouble(1));
                    if (distance < minDistance.get()) {
                        minDistance.set(distance);
                    }
                    return distance;
                })).orElseThrow(RuntimeException::new).optJSONObject("properties");

                if (minDistance.get() > 100) {
                    weatherStationName = getLanguage().equals("en") ? "Hong Kong" : "香港";
                } else if (weatherStationName == null) {
                    weatherStationName = weatherStation.optString("weather_station_" + lang);
                }

                JSONObject weatherStationData = HTTPRequestUtils.getJSONResponse(weatherStation.optString("url"));
                float currentTemperature = (float) weatherStationData.optJSONObject("Temperature").optDouble("Value");

                JSONObject currentWeatherData = HTTPRequestUtils.getJSONResponse("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=rhrread&lang=" + lang);
                float uvIndex = currentWeatherData.opt("uvindex") instanceof JSONObject ? (float) currentWeatherData.optJSONObject("uvindex").optJSONArray("data").optJSONObject(0).optDouble("value") : -1F;
                float currentHumidity = (float) currentWeatherData.optJSONObject("humidity").optJSONArray("data").optJSONObject(0).optDouble("value");
                WeatherStatusIcon weatherIcon = WeatherStatusIcon.getByCode(currentWeatherData.optJSONArray("icon").optInt(0));

                String forecastStation = StreamSupport.stream(Spliterators.spliteratorUnknownSize(FORECAST_STATIONS.keys(), Spliterator.ORDERED), false).min(Comparator.comparing(k -> {
                    JSONArray pos = FORECAST_STATIONS.optJSONArray(k);
                    return findDistance(location.getLatitude(), location.getLongitude(), pos.optDouble(0), pos.optDouble(1));
                })).orElse(null);
                JSONObject forecastStationData = HTTPRequestUtils.getJSONResponse("https://maps.weather.gov.hk/ocf/dat/" + forecastStation + ".xml");
                JSONArray forecastDailyData = forecastStationData.optJSONArray("DailyForecast");
                List<JSONObject> dailyForecastArray = JsonUtils.toList(forecastDailyData, JSONObject.class);

                String chanceOfRainStr = dailyForecastArray.get(0).optString("ForecastChanceOfRain");
                float chanceOfRain = Float.parseFloat(chanceOfRainStr.substring(0, chanceOfRainStr.length() - 1));

                List<JSONObject> sunData = HTTPRequestUtils.getCSVResponse("https://data.weather.gov.hk/weatherAPI/opendata/opendata.php?dataType=SRS&year=" + today.getYear() + "&rformat=csv", s -> s.replaceAll("[^a-zA-Z.0-9:\\-,]", ""));
                if (sunData == null) {
                    future.complete(null);
                    return;
                }
                String todayDateStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                JSONObject todaySun = sunData.stream().filter(e -> e.optString("YYYY-MM-DD").equals(todayDateStr)).findFirst().orElse(null);
                LocalTime sunriseTime = LocalTime.parse(todaySun.optString("RISE"), timeFormatter);
                LocalTime sunTransitTime = LocalTime.parse(todaySun.optString("TRAN."), timeFormatter);
                LocalTime sunsetTime = LocalTime.parse(todaySun.optString("SET"), timeFormatter);

                JSONObject forecastData = HTTPRequestUtils.getJSONResponse("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=fnd&lang=" + lang);
                if (forecastData == null) {
                    future.complete(null);
                    return;
                }

                JSONArray dayArray = forecastData.optJSONArray("weatherForecast");
                List<WeatherInfo> forecastInfo = new ArrayList<>(dayArray.length() - 1);

                JSONObject dayObj = dayArray.optJSONObject(0);
                float highestTemperature = (float) dayObj.optJSONObject("forecastMaxtemp").optDouble("value");
                float lowestTemperature = (float) dayObj.optJSONObject("forecastMintemp").optDouble("value");
                float maxRelativeHumidity = (float) dayObj.optJSONObject("forecastMaxrh").optDouble("value");
                float minRelativeHumidity = (float) dayObj.optJSONObject("forecastMinrh").optDouble("value");

                for (int i = 0; i < dayArray.length(); i++) {
                    JSONObject forecastDayObj = dayArray.optJSONObject(i);

                    String forecastDateStr = forecastDayObj.optString("forecastDate");
                    JSONObject forecastStationDayObj = dailyForecastArray.stream().filter(e -> e.optString("ForecastDate").equals(forecastDateStr)).findFirst().orElse(null);

                    LocalDate forecastDate = LocalDate.parse(forecastDateStr, dateFormatter);
                    float forecastHighestTemperature = (float) forecastDayObj.optJSONObject("forecastMaxtemp").optDouble("value");
                    float forecastLowestTemperature = (float) forecastDayObj.optJSONObject("forecastMintemp").optDouble("value");
                    float forecastMaxRelativeHumidity = (float) forecastDayObj.optJSONObject("forecastMaxrh").optDouble("value");
                    float forecastMinRelativeHumidity = (float) forecastDayObj.optJSONObject("forecastMinrh").optDouble("value");
                    WeatherStatusIcon forecastWeatherIcon = WeatherStatusIcon.getByCode(forecastDayObj.optInt("ForecastIcon"));

                    float forecastChanceOfRain;
                    if (forecastStationDayObj == null) {
                        forecastChanceOfRain = -1F;
                    } else {
                        String forecastChanceOfRainStr = forecastStationDayObj.optString("ForecastChanceOfRain");
                        forecastChanceOfRain = Float.parseFloat(forecastChanceOfRainStr.substring(0, forecastChanceOfRainStr.length() - 1));
                    }

                    forecastInfo.add(new WeatherInfo(forecastDate, forecastHighestTemperature, forecastLowestTemperature, forecastMaxRelativeHumidity, forecastMinRelativeHumidity, forecastChanceOfRain, forecastWeatherIcon));
                }

                JSONArray hourArray = forecastStationData.optJSONArray("HourlyWeatherForecast");
                List<HourlyWeatherInfo> hourlyWeatherInfo = new ArrayList<>(hourArray.length());
                WeatherStatusIcon lastHourIcon = weatherIcon;
                for (int i = 0; i < hourArray.length(); i++) {
                    JSONObject hourObj = hourArray.optJSONObject(i);

                    LocalDateTime hour = LocalDateTime.parse(hourObj.optString("ForecastHour"), dateHourFormatter);
                    float hourTemperature = (float) hourObj.optDouble("ForecastTemperature");
                    float hourHumidity = (float) hourObj.optDouble("ForecastRelativeHumidity");
                    float hourWindDirection = (float) hourObj.optDouble("ForecastWindDirection");
                    float hourWindSpeed = (float) hourObj.optDouble("ForecastWindSpeed");
                    WeatherStatusIcon hourIcon;
                    if (hourObj.has("ForecastWeather")) {
                        hourIcon = WeatherStatusIcon.getByCode(hourObj.optInt("ForecastWeather"));
                        if (hourIcon == null) {
                            hourIcon = lastHourIcon;
                        } else {
                            lastHourIcon = hourIcon;
                        }
                    } else {
                        hourIcon = lastHourIcon;
                    }

                    hourlyWeatherInfo.add(new HourlyWeatherInfo(hour, hourTemperature, hourHumidity, hourWindDirection, hourWindSpeed, hourIcon));
                }

                future.complete(new CurrentWeatherInfo(today, highestTemperature, lowestTemperature, maxRelativeHumidity, minRelativeHumidity, chanceOfRain, weatherIcon, weatherStationName, currentTemperature, currentHumidity, uvIndex, sunriseTime, sunTransitTime, sunsetTime, forecastInfo, hourlyWeatherInfo));
            } catch (Throwable e) {
                e.printStackTrace();
                future.complete(null);
            }
        }).start();
        return future;
    }

    public Future<Set<WeatherWarningsType>> getActiveWarnings(Context context) {
        if (!ConnectionUtils.getConnectionType(context).hasConnection()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Set<WeatherWarningsType>> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                JSONObject data = HTTPRequestUtils.getJSONResponse("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=warnsum&lang=tc");
                if (data == null) {
                    future.complete(null);
                    return;
                }
                Set<WeatherWarningsType> warnings = EnumSet.noneOf(WeatherWarningsType.class);
                for (Iterator<String> itr = data.keys(); itr.hasNext(); ) {
                    String key = itr.next();
                    try {
                        warnings.add(WeatherWarningsType.valueOf(data.optJSONObject(key).optString("code").toUpperCase()));
                    } catch (Throwable ignore) {}
                }
                future.complete(warnings);
            } catch (Throwable e) {
                future.complete(null);
            }
        }).start();
        return future;
    }

    public Future<List<Pair<String, Long>>> getWeatherTips(Context context) {
        if (!ConnectionUtils.getConnectionType(context).hasConnection()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<List<Pair<String, Long>>> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                String lang = getLanguage().equals("en") ? "en" : "tc";
                JSONObject data = HTTPRequestUtils.getJSONResponse("https://data.weather.gov.hk/weatherAPI/opendata/weather.php?dataType=swt&lang=" + lang);
                if (data == null) {
                    future.complete(null);
                    return;
                }
                if (!data.has("swt")) {
                    future.complete(Collections.emptyList());
                    return;
                }
                JSONArray array = data.getJSONArray("swt");
                List<Pair<String, Long>> tips = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(obj.optString("updateTime"), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    tips.add(Pair.create(obj.optString("desc"), zonedDateTime.toInstant().toEpochMilli()));
                }
                future.complete(tips);
            } catch (Throwable e) {
                future.complete(null);
            }
        }).start();
        return future;
    }

}
