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

import java.util.Objects;

@Immutable
public class DisplayableInfo implements JSONSerializable {

    public static final DisplayableInfo EMPTY = new DisplayableInfo(false, "");

    public static DisplayableInfo deserialize(JSONObject jsonObject) {
        boolean isDisplay = jsonObject.optBoolean("isDisplay");
        String info = jsonObject.optString("info");
        return new DisplayableInfo(isDisplay, info);
    }

    private final boolean isDisplay;
    private final String info;

    public DisplayableInfo(boolean isDisplay, String info) {
        this.isDisplay = isDisplay;
        this.info = info;
    }

    public boolean isDisplay() {
        return isDisplay;
    }

    public String getInfo() {
        return info;
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("isDisplay", isDisplay);
        jsonObject.put("info", info);
        return jsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DisplayableInfo that = (DisplayableInfo) o;
        return isDisplay == that.isDisplay && Objects.equals(info, that.info);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isDisplay, info);
    }
}
