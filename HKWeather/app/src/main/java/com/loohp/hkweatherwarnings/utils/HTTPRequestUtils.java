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

package com.loohp.hkweatherwarnings.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

public class HTTPRequestUtils {

    public static boolean isResponseOk(String link) {
        try {
            URL url = new URL(link);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            return connection.getResponseCode() == HttpsURLConnection.HTTP_OK;
        } catch (IOException e) {
            return false;
        }
    }

    public static String getTextResponse(String link) {
        try {
            URL url = new URL(link);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public static List<JSONObject> getCSVResponse(String link) {
        return getCSVResponse(link, UnaryOperator.identity());
    }

    public static List<JSONObject> getCSVResponse(String link, UnaryOperator<String> filter) {
        try {
            String result = getTextResponse(link);
            if (result == null) {
                return null;
            }
            String[] lines = result.split("\\R");
            if (lines.length < 2) {
                return null;
            }
            String[] keys = lines[0].split(",");
            List<JSONObject> list = new ArrayList<>(lines.length - 1);
            for (int i = 1; i < lines.length; i++) {
                String[] values = lines[i].split(",");
                JSONObject obj = new JSONObject();
                for (int k = 0; k < keys.length && k < values.length; k++) {
                    obj.put(filter.apply(keys[k]), filter.apply(values[k]));
                }
                list.add(obj);
            }
            return list;
        } catch (JSONException e) {
            return null;
        }
    }

    public static JSONObject getJSONResponse(String link) {
        try {
            URL url = new URL(link);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String reply = reader.lines().collect(Collectors.joining());
                    return new JSONObject(reply);
                }
            } else {
                return null;
            }
        } catch (IOException | JSONException e) {
            return null;
        }
    }

    public static JSONObject postJSONResponse(String link, JSONObject body) {
        try {
            URL url = new URL(link);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String reply = reader.lines().collect(Collectors.joining());
                    return new JSONObject(reply);
                }
            } else {
                return null;
            }
        } catch (IOException | JSONException e) {
            return null;
        }
    }

    public static InputStream getInputStream(String link) throws IOException {
        URLConnection connection = new URL(link).openConnection();
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(20000);
        connection.setUseCaches(false);
        connection.setDefaultUseCaches(false);
        connection.addRequestProperty("User-Agent", "Mozilla/5.0");
        connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
        connection.addRequestProperty("Pragma", "no-cache");
        return connection.getInputStream();
    }

    public static byte[] download(String link) throws IOException {
        try (InputStream is = getInputStream(link)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] byteChunk = new byte[4096];
            int n;
            while ((n = is.read(byteChunk)) > 0) {
                baos.write(byteChunk, 0, n);
            }
            return baos.toByteArray();
        }
    }

    public static long getContentSize(String link) {
        try {
            URLConnection connection = new URL(link).openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setRequestMethod("HEAD");
            }
            return connection.getContentLengthLong();
        } catch (IOException e) {
            return -1;
        }
    }

    public static String getContentType(String link) {
        try {
            URLConnection connection = new URL(link).openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setRequestMethod("HEAD");
            }
            return connection.getContentType();
        } catch (IOException e) {
            return "";
        }
    }

}

