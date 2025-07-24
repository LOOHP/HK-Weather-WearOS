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

import com.loohp.hkweatherwarnings.R;

public enum HeatStressAtWorkWarningLevel {

    AMBER(R.mipmap.hswwa, "hswwa", "黃色工作暑熱警告", "Amber Heat Stress at Work Warning", 0xFFFFEF00),
    RED(R.mipmap.hswwr, "hswwr", "紅色工作暑熱警告", "Red Heat Stress at Work Warning", 0xFFE40013),
    BLACK(R.mipmap.hswwb, "hswwb", "黑色工作暑熱警告", "Black Heat Stress at Work Warning", 0xFFC7C8C9);

    private static final HeatStressAtWorkWarningLevel[] VALUES = values();

    private final int iconId;
    private final String iconName;
    private final String nameZh;
    private final String nameEn;
    private final int color;

    HeatStressAtWorkWarningLevel(int iconId, String iconName, String nameZh, String nameEn, int color) {
        this.iconId = iconId;
        this.iconName = iconName;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.color = color;
    }

    public int getIconId() {
        return iconId;
    }

    public String getIconName() {
        return iconName;
    }

    public String getNameZh() {
        return nameZh;
    }

    public String getNameEn() {
        return nameEn;
    }

    public int getColor() {
        return color;
    }

    public static HeatStressAtWorkWarningLevel getByName(String name) {
        for (HeatStressAtWorkWarningLevel level : VALUES) {
            if (level.name().equalsIgnoreCase(name)) {
                return level;
            }
        }
        return null;
    }

}