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

import android.content.Context;

import androidx.compose.runtime.Immutable;

import com.loohp.hkweatherwarnings.cache.JSONSerializable;
import com.loohp.hkweatherwarnings.shared.Registry;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

@Immutable
public class LocalForecastInfo implements JSONSerializable {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy.HH:mm");

    public static LocalForecastInfo deserialize(JSONObject jsonObject) {
        String generalSituation = jsonObject.optString("generalSituation");
        String tcInfo = jsonObject.optString("tcInfo");
        String fireDangerWarning = jsonObject.optString("fireDangerWarning");
        String forecastPeriod = jsonObject.optString("forecastPeriod");
        String forecastDesc = jsonObject.optString("forecastDesc");
        String outlook = jsonObject.optString("outlook");
        LocalDateTime updateTime = LocalDateTime.parse(jsonObject.optString("updateTime"), DATE_TIME_FORMATTER);
        return new LocalForecastInfo(generalSituation, tcInfo, fireDangerWarning, forecastPeriod, forecastDesc, outlook, updateTime);
    }

    private final String generalSituation;
    private final String tcInfo;
    private final String fireDangerWarning;
    private final String forecastPeriod;
    private final String forecastDesc;
    private final String outlook;
    private final LocalDateTime updateTime;

    public LocalForecastInfo(String generalSituation, String tcInfo, String fireDangerWarning, String forecastPeriod, String forecastDesc, String outlook, LocalDateTime updateTime) {
        this.generalSituation = generalSituation;
        this.tcInfo = tcInfo;
        this.fireDangerWarning = fireDangerWarning;
        this.forecastPeriod = forecastPeriod;
        this.forecastDesc = forecastDesc;
        this.outlook = outlook;
        this.updateTime = updateTime;
    }

    public String toDisplayText(Context context) {
        StringBuilder sb = new StringBuilder();
        if (!generalSituation.isEmpty()) {
            sb.append(generalSituation).append("\n");
        }
        if (!tcInfo.isEmpty()) {
            sb.append(tcInfo).append("\n");
        }
        if (!fireDangerWarning.isEmpty()) {
            sb.append(fireDangerWarning).append("\n");
        }
        if (!forecastPeriod.isEmpty()) {
            sb.append(forecastPeriod).append("\n");
        }
        if (!forecastDesc.isEmpty()) {
            sb.append(forecastDesc).append("\n");
        }
        if (!outlook.isEmpty()) {
            sb.append(outlook).append("\n");
        }
        if (Registry.getInstance(context).getLanguage().equals("en")) {
            sb.append("Updated at ").append(updateTime.format(DateTimeFormatter.ofPattern("HH:mm' HKT 'dd/MM/yyyy", Locale.ENGLISH)));
        } else {
            sb.append(updateTime.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH時mm分更新", Locale.TRADITIONAL_CHINESE)));
        }
        return sb.toString();
    }

    public String toDisplayFooter(Context context) {
        StringBuilder sb = new StringBuilder();
        if (Registry.getInstance(context).getLanguage().equals("en")) {
            sb.append("(The above forecast period is valid up to 23:59 HKT Today, ")
                    .append(updateTime.format(DateTimeFormatter.ofPattern("dd/MMMM/yyyy", Locale.ENGLISH)))
                    .append("; Regular update at: around 45 minutes past each hour, 16:15 HKT and 23:15 HKT)");
        } else {
            sb.append("(以上預測有效期至今日(")
                    .append(updateTime.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.TRADITIONAL_CHINESE)))
                    .append(")23時59分；定期更新時間：約每小時45分，16時15分及23時15分)");
        }
        return sb.toString();
    }

    public String getGeneralSituation() {
        return generalSituation;
    }

    public String getTcInfo() {
        return tcInfo;
    }

    public String getFireDangerWarning() {
        return fireDangerWarning;
    }

    public String getForecastPeriod() {
        return forecastPeriod;
    }

    public String getForecastDesc() {
        return forecastDesc;
    }

    public String getOutlook() {
        return outlook;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    @Override
    public JSONObject serialize() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("generalSituation", generalSituation);
        jsonObject.put("tcInfo", tcInfo);
        jsonObject.put("fireDangerWarning", fireDangerWarning);
        jsonObject.put("forecastPeriod", forecastPeriod);
        jsonObject.put("forecastDesc", forecastDesc);
        jsonObject.put("outlook", outlook);
        jsonObject.put("updateTime", updateTime.format(DATE_TIME_FORMATTER));
        return jsonObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalForecastInfo that = (LocalForecastInfo) o;
        return Objects.equals(generalSituation, that.generalSituation) && Objects.equals(tcInfo, that.tcInfo) && Objects.equals(fireDangerWarning, that.fireDangerWarning) && Objects.equals(forecastPeriod, that.forecastPeriod) && Objects.equals(forecastDesc, that.forecastDesc) && Objects.equals(outlook, that.outlook) && Objects.equals(updateTime, that.updateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(generalSituation, tcInfo, fireDangerWarning, forecastPeriod, forecastDesc, outlook, updateTime);
    }
}
