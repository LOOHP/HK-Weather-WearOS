package com.loohp.hkweatherwarnings.weather;

import com.loohp.hkweatherwarnings.R;

public enum WeatherWarningsType {

    WFIREY(WeatherWarningsCategory.WFIRE, R.mipmap.fireyellow, "fireyellow", "黃色火災危險警告", "Yellow Fire Danger Warning", 0xFFF6C43D),
    WFIRER(WeatherWarningsCategory.WFIRE, R.mipmap.firered, "firered", "紅色火災危險警告", "Red Fire Danger Warning", 0xFFB50000),
    WFROST(WeatherWarningsCategory.WFROST, R.mipmap.frost, "frost", "霜凍警告", "Frost Warning", 0xFFFF7E7E),
    WHOT(WeatherWarningsCategory.WHOT, R.mipmap.hot, "hot", "酷熱天氣警告", "Very Hot Weather Warning", 0xFFFF0000),
    WCOLD(WeatherWarningsCategory.WCOLD, R.mipmap.cold, "cold", "寒冷天氣警告", "Cold Weather Warning", 0xFF0069FF),
    WMSGNL(WeatherWarningsCategory.WMSGNL, R.mipmap.sms, "sms", "強烈季候風信號", "Strong Monsoon Signal", 0xFFFF0000),
    WRAINA(WeatherWarningsCategory.WRAIN, R.mipmap.rainamber, "rainamber", "黃色暴雨警告信號", "Amber Rainstorm Warning Signal", 0xFFFFCC00),
    WRAINR(WeatherWarningsCategory.WRAIN, R.mipmap.rainred, "rainred", "紅色暴雨警告信號", "Red Rainstorm Warning Signal", 0xFFFF0000),
    WRAINB(WeatherWarningsCategory.WRAIN, R.mipmap.rainblack, "rainblack", "黑色暴雨警告信號", "Black Rainstorm Warning Signal", 0xFF3D3D3D),
    WFNTSA(WeatherWarningsCategory.WFNTSA, R.mipmap.northflood, "northflood", "新界北部水浸特別報告", "Special Announcement on Flooding in Northern New Territories", 0xFF40E693),
    WL(WeatherWarningsCategory.WL, R.mipmap.ls, "ls", "山泥傾瀉警告", "Landslip Warning", 0xFF7F6633),
    TC1(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc1, "tc1", "一號戒備信號", "Standby Signal No. 1", 0xFFCECECE),
    TC3(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc3, "tc3", "三號強風信號", "Strong Wind Signal No. 3", 0xFFCECECE),
    TC8NE(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc08ne, "tc08ne", "八號東北烈風或暴風信號", "No. 8 Northeast Gale or Storm Signal", 0xFFFFFFFF),
    TC8SE(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc08se, "tc08se", "八號東南烈風或暴風信號", "No. 8 Southeast Gale or Storm Signal", 0xFFFFFFFF),
    TC8NW(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc08nw, "tc08nw", "八號西北烈風或暴風信號", "No. 8 Northwest Gale or Storm Signal", 0xFFFFFFFF),
    TC8SW(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc08sw, "tc08sw", "八號西南烈風或暴風信號", "No. 8 Southwest Gale or Storm Signal", 0xFFFFFFFF),
    TC9(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc09, "tc09", "九號烈風或暴風風力增強信號", "Increasing Gale or Storm Signal No. 9", 0xFFFFFFFF),
    TC10(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc10, "tc10", "十號颶風信號", "Hurricane Signal No. 10", 0xFFFFFFFF),
    WTMW(WeatherWarningsCategory.WTMW, R.mipmap.tsunami, "tsunami", "海嘯警告", "Tsunami Warning", 0xFF6EB2E2),
    WTS(WeatherWarningsCategory.WTS, R.mipmap.ts, "ts", "雷暴警告", "Thunderstorm Warning", 0xFFFFBA00);

    private final WeatherWarningsCategory category;
    private final int iconId;
    private final String iconName;
    private final String nameZh;
    private final String nameEn;
    private final int color;

    WeatherWarningsType(WeatherWarningsCategory category, int iconId, String iconName, String nameZh, String nameEn, int color) {
        this.category = category;
        this.iconId = iconId;
        this.iconName = iconName;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.color = color;
    }

    public WeatherWarningsCategory getCategory() {
        return category;
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
}
