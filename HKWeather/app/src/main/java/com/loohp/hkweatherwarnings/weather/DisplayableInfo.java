package com.loohp.hkweatherwarnings.weather;

import com.loohp.hkweatherwarnings.cache.JSONSerializable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

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
