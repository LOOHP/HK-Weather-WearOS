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