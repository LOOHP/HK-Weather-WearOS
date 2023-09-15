package com.loohp.hkweatherwarnings.shared;

import android.content.Context;
import android.util.Pair;

import androidx.wear.tiles.TileService;

import com.loohp.hkweatherwarnings.tiles.WeatherTipsTile;
import com.loohp.hkweatherwarnings.tiles.WeatherWarningsTile;
import com.loohp.hkweatherwarnings.utils.ConnectionUtils;
import com.loohp.hkweatherwarnings.utils.HTTPRequestUtils;
import com.loohp.hkweatherwarnings.warnings.WeatherWarningsType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

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

    private Registry(Context context) {
        try {
            ensureData(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void updateTileService(Context context) {
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

    private void ensureData(Context context) throws IOException {
        if (PREFERENCES != null) {
            return;
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
