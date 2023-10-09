package com.loohp.hkweatherwarnings.weather;

import com.loohp.hkweatherwarnings.cache.JSONSerializable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpecialTyphoonInfo implements JSONSerializable {

    public static SpecialTyphoonInfo deserialize(JSONObject jsonObject) {
        WeatherWarningsType signalType = jsonObject.optString("signalType").isEmpty() ? null : WeatherWarningsType.valueOf(jsonObject.optString("signalType").toUpperCase());
        DisplayableInfo considerations = DisplayableInfo.deserialize(jsonObject.optJSONObject("considerations"));
        DisplayableInfo info = DisplayableInfo.deserialize(jsonObject.optJSONObject("info"));
        DisplayableInfo windsInfo = DisplayableInfo.deserialize(jsonObject.optJSONObject("windsInfo"));
        DisplayableInfo windsHighlight = DisplayableInfo.deserialize(jsonObject.optJSONObject("windsHighlight"));
        DisplayableInfo tideInfo = DisplayableInfo.deserialize(jsonObject.optJSONObject("tideInfo"));
        return new SpecialTyphoonInfo(signalType, considerations, info, windsInfo, windsHighlight, tideInfo);
    }

    private final WeatherWarningsType signalType;
    private final DisplayableInfo considerations;
    private final DisplayableInfo info;
    private final DisplayableInfo windsInfo;
    private final DisplayableInfo windsHighlight;
    private final DisplayableInfo tideInfo;

    public SpecialTyphoonInfo(WeatherWarningsType signalType, DisplayableInfo considerations, DisplayableInfo info, DisplayableInfo windsInfo, DisplayableInfo windsHighlight, DisplayableInfo tideInfo) {
        this.signalType = signalType;
        this.considerations = considerations;
        this.info = info;
        this.windsInfo = windsInfo;
        this.windsHighlight = windsHighlight;
        this.tideInfo = tideInfo;
    }

    public boolean hasAnyDisplay() {
        return considerations.isDisplay() || info.isDisplay() || windsInfo.isDisplay() || windsHighlight.isDisplay() || tideInfo.isDisplay();
    }

    public String toDisplayText() {
        List<String> text = new ArrayList<>(5);
        if (considerations.isDisplay()) {
            text.add(considerations.getInfo());
        }
        if (info.isDisplay()) {
            text.add(info.getInfo());
        }
        if (windsInfo.isDisplay()) {
            text.add(windsInfo.getInfo());
        }
        if (windsHighlight.isDisplay()) {
            text.add(windsHighlight.getInfo());
        }
        if (tideInfo.isDisplay()) {
            text.add(tideInfo.getInfo());
        }
        return String.join("\n", text);
    }

    public WeatherWarningsType getSignalType() {
        return signalType;
    }

    public DisplayableInfo getConsiderations() {
        return considerations;
    }

    public DisplayableInfo getInfo() {
        return info;
    }

    public DisplayableInfo getWindsInfo() {
        return windsInfo;
    }

    public DisplayableInfo getWindsHighlight() {
        return windsHighlight;
    }

    public DisplayableInfo getTideInfo() {
        return tideInfo;
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if (signalType != null) {
            jsonObject.put("signalType", signalType.name());
        }
        jsonObject.put("considerations", considerations.serialize());
        jsonObject.put("info", info.serialize());
        jsonObject.put("windsInfo", windsInfo.serialize());
        jsonObject.put("windsHighlight", windsHighlight.serialize());
        jsonObject.put("tideInfo", tideInfo.serialize());
        return jsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpecialTyphoonInfo that = (SpecialTyphoonInfo) o;
        return signalType == that.signalType && Objects.equals(considerations, that.considerations) && Objects.equals(info, that.info) && Objects.equals(windsInfo, that.windsInfo) && Objects.equals(windsHighlight, that.windsHighlight) && Objects.equals(tideInfo, that.tideInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signalType, considerations, info, windsInfo, windsHighlight, tideInfo);
    }
}
