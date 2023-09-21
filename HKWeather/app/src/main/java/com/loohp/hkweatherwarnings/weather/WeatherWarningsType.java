package com.loohp.hkweatherwarnings.weather;

import com.loohp.hkweatherwarnings.R;

public enum WeatherWarningsType {

    WFIREY(WeatherWarningsCategory.WFIRE, R.mipmap.fireyellow, "fireyellow"),
    WFIRER(WeatherWarningsCategory.WFIRE, R.mipmap.firered, "firered"),
    WFROST(WeatherWarningsCategory.WFROST, R.mipmap.frost, "frost"),
    WHOT(WeatherWarningsCategory.WHOT, R.mipmap.hot, "hot"),
    WCOLD(WeatherWarningsCategory.WCOLD, R.mipmap.cold, "cold"),
    WMSGNL(WeatherWarningsCategory.WMSGNL, R.mipmap.sms, "sms"),
    WRAINA(WeatherWarningsCategory.WRAIN, R.mipmap.rainamber, "rainamber"),
    WRAINR(WeatherWarningsCategory.WRAIN, R.mipmap.rainred, "rainred"),
    WRAINB(WeatherWarningsCategory.WRAIN, R.mipmap.rainblack, "rainblack"),
    WFNTSA(WeatherWarningsCategory.WFNTSA, R.mipmap.northflood, "northflood"),
    WL(WeatherWarningsCategory.WL, R.mipmap.ls, "ls"),
    TC1(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc1, "tc1"),
    TC3(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc3, "tc3"),
    TC8NE(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc08ne, "tc08ne"),
    TC8SE(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc08se, "tc08se"),
    TC8NW(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc08nw, "tc08nw"),
    TC8SW(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc08sw, "tc08sw"),
    TC9(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc09, "tc09"),
    TC10(WeatherWarningsCategory.WTCSGNL, R.mipmap.tc10, "tc10"),
    WTMW(WeatherWarningsCategory.WTMW, R.mipmap.tsunami, "tsunami"),
    WTS(WeatherWarningsCategory.WTS, R.mipmap.ts, "ts");

    private final WeatherWarningsCategory category;
    private final int iconId;
    private final String iconName;

    WeatherWarningsType(WeatherWarningsCategory category, int iconId, String iconName) {
        this.category = category;
        this.iconId = iconId;
        this.iconName = iconName;
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
}
