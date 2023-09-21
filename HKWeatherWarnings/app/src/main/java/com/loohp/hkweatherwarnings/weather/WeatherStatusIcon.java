package com.loohp.hkweatherwarnings.weather;

import com.loohp.hkweatherwarnings.R;

public enum WeatherStatusIcon {
    
    _50(50, R.mipmap.pic50, "pic50"),
    _51(51, R.mipmap.pic51, "pic51"),
    _52(52, R.mipmap.pic52, "pic52"),
    _53(53, R.mipmap.pic53, "pic53"),
    _54(54, R.mipmap.pic54, "pic54"),
    _60(60, R.mipmap.pic60, "pic60"),
    _61(61, R.mipmap.pic61, "pic61"),
    _62(62, R.mipmap.pic62, "pic62"),
    _63(63, R.mipmap.pic63, "pic63"),
    _64(64, R.mipmap.pic64, "pic64"),
    _65(65, R.mipmap.pic65, "pic65"),
    _70(70, R.mipmap.pic70, "pic70"),
    _71(71, R.mipmap.pic71, "pic71"),
    _72(72, R.mipmap.pic72, "pic72"),
    _73(73, R.mipmap.pic73, "pic73"),
    _74(74, R.mipmap.pic74, "pic74"),
    _75(75, R.mipmap.pic75, "pic75"),
    _76(76, R.mipmap.pic76, "pic76"),
    _77(77, R.mipmap.pic77, "pic77"),
    _80(80, R.mipmap.pic80, "pic80"),
    _81(81, R.mipmap.pic81, "pic81"),
    _82(82, R.mipmap.pic82, "pic82"),
    _83(83, R.mipmap.pic83, "pic83"),
    _84(84, R.mipmap.pic84, "pic84"),
    _85(85, R.mipmap.pic85, "pic85"),
    _90(90, R.mipmap.pic90, "pic90"),
    _91(91, R.mipmap.pic91, "pic91"),
    _92(92, R.mipmap.pic92, "pic92"),
    _93(93, R.mipmap.pic93, "pic93");

    private static final WeatherStatusIcon[] VALUES = values();

    private final int code;
    private final int iconId;
    private final String iconName;

    WeatherStatusIcon(int code, int iconId, String iconName) {
        this.code = code;
        this.iconId = iconId;
        this.iconName = iconName;
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

    public static WeatherStatusIcon getByCode(int code) {
        for (WeatherStatusIcon icon : VALUES) {
            if (icon.getCode() == code) {
                return icon;
            }
        }
        return null;
    }
}
