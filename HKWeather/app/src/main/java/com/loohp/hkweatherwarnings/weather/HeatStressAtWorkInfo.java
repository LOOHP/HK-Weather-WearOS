package com.loohp.hkweatherwarnings.weather;

import com.loohp.hkweatherwarnings.cache.JSONSerializable;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class HeatStressAtWorkInfo implements JSONSerializable {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy.HH:mm");

    public static HeatStressAtWorkInfo deserialize(JSONObject jsonObject) {
        String description = jsonObject.optString("description");
        HeatStressAtWorkWarningLevel warningsLevel = HeatStressAtWorkWarningLevel.getByName(jsonObject.optString("warningsLevel").toUpperCase());
        HeatStressAtWorkWarningAction action = HeatStressAtWorkWarningAction.valueOf(jsonObject.optString("action").toUpperCase());
        LocalDateTime effectiveTime = LocalDateTime.parse(jsonObject.optString("effectiveTime"), DATE_TIME_FORMATTER);
        LocalDateTime issueTime = LocalDateTime.parse(jsonObject.optString("issueTime"), DATE_TIME_FORMATTER);
        return new HeatStressAtWorkInfo(description, warningsLevel, action, effectiveTime, issueTime);
    }

    private final String description;
    private final HeatStressAtWorkWarningLevel warningsLevel;
    private final HeatStressAtWorkWarningAction action;
    private final LocalDateTime effectiveTime;
    private final LocalDateTime issueTime;

    public HeatStressAtWorkInfo(String description, HeatStressAtWorkWarningLevel warningsLevel, HeatStressAtWorkWarningAction action, LocalDateTime effectiveTime, LocalDateTime issueTime) {
        this.description = description;
        this.warningsLevel = warningsLevel;
        this.action = action;
        this.effectiveTime = effectiveTime;
        this.issueTime = issueTime;
    }

    public String getDescription() {
        return description;
    }

    public HeatStressAtWorkWarningLevel getWarningsLevel() {
        return warningsLevel;
    }

    public HeatStressAtWorkWarningAction getAction() {
        return action;
    }

    public LocalDateTime getEffectiveTime() {
        return effectiveTime;
    }

    public LocalDateTime getIssueTime() {
        return issueTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeatStressAtWorkInfo that = (HeatStressAtWorkInfo) o;
        return Objects.equals(description, that.description) && warningsLevel == that.warningsLevel && action == that.action && Objects.equals(effectiveTime, that.effectiveTime) && Objects.equals(issueTime, that.issueTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, warningsLevel, action, effectiveTime, issueTime);
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("description", description);
        jsonObject.put("warningsLevel", warningsLevel == null ? "" : warningsLevel.name());
        jsonObject.put("action", action.name());
        jsonObject.put("effectiveTime", DATE_TIME_FORMATTER.format(effectiveTime));
        jsonObject.put("issueTime", DATE_TIME_FORMATTER.format(issueTime));
        return jsonObject;
    }
}
