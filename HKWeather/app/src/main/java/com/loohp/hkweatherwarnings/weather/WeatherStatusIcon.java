package com.loohp.hkweatherwarnings.weather;

import com.loohp.hkweatherwarnings.R;

public enum WeatherStatusIcon {
    
    _50(50, R.mipmap.pic50, "pic50", "陽光充沛", "Sunny"),
    _51(51, R.mipmap.pic51, "pic51", "間中有陽光", "Sunny Periods"),
    _52(52, R.mipmap.pic52, "pic52", "短暫陽光", "Sunny Intervals"),
    _53(53, R.mipmap.pic53, "pic53", "間中有陽光幾陣驟雨", "Sunny Periods with a Few Showers"),
    _54(54, R.mipmap.pic54, "pic54", "短暫陽光有驟雨", "Sunny Intervals with Showers"),
    _60(60, R.mipmap.pic60, "pic60", "多雲", "Cloudy"),
    _61(61, R.mipmap.pic61, "pic61", "密雲", "Overcast"),
    _62(62, R.mipmap.pic62, "pic62", "微雨", "Light Rain"),
    _63(63, R.mipmap.pic63, "pic63", "雨", "Rain"),
    _64(64, R.mipmap.pic64, "pic64", "大雨", "Heavy Rain"),
    _65(65, R.mipmap.pic65, "pic65", "雷暴", "Thunderstorms"),
    _70(70, R.mipmap.pic70, "pic70", "天色良好", "Fine"),
    _71(71, R.mipmap.pic71, "pic71", "天色良好", "Fine"),
    _72(72, R.mipmap.pic72, "pic72", "天色良好", "Fine"),
    _73(73, R.mipmap.pic73, "pic73", "天色良好", "Fine"),
    _74(74, R.mipmap.pic74, "pic74", "天色良好", "Fine"),
    _75(75, R.mipmap.pic75, "pic75", "天色良好", "Fine"),
    _76(76, R.mipmap.pic76, "pic76", "大致多雲", "Mainly Cloudy"),
    _77(77, R.mipmap.pic77, "pic77", "天色大致良好", "Mainly Fine"),
    _80(80, R.mipmap.pic80, "pic80", "大風", "Windy"),
    _81(81, R.mipmap.pic81, "pic81", "乾燥", "Dry"),
    _82(82, R.mipmap.pic82, "pic82", "潮濕", "Humid"),
    _83(83, R.mipmap.pic83, "pic83", "霧", "Fog"),
    _84(84, R.mipmap.pic84, "pic84", "薄霧", "Mist"),
    _85(85, R.mipmap.pic85, "pic85", "煙霞", "Haze"),
    _90(90, R.mipmap.pic90, "pic90", "熱", "Hot"),
    _91(91, R.mipmap.pic91, "pic91", "暖", "Warm"),
    _92(92, R.mipmap.pic92, "pic92", "涼", "Cool"),
    _93(93, R.mipmap.pic93, "pic93", "冷", "Cold");

    private static final WeatherStatusIcon[] VALUES = values();

    private final int code;
    private final int iconId;
    private final String iconName;
    private final String descriptionZh;
    private final String descriptionEn;

    WeatherStatusIcon(int code, int iconId, String iconName, String descriptionZh, String descriptionEn) {
        this.code = code;
        this.iconId = iconId;
        this.iconName = iconName;
        this.descriptionZh = descriptionZh;
        this.descriptionEn = descriptionEn;
    }

    public int getCode() {
        return code;
    }

    public int getIconId() {
        return iconId;
    }

    public String getIconName() {
        return iconName;
    }

    public String getDescriptionZh() {
        return descriptionZh;
    }

    public String getDescriptionEn() {
        return descriptionEn;
    }

    public static WeatherStatusIcon getByCode(int code) {
        for (WeatherStatusIcon icon : VALUES) {
            if (icon.getCode() == code) {
                return icon;
            }
        }
        return null;
    }
}
