package com.loohp.hkweatherwarnings.weather;

public class TropicalCycloneInfo {

    private final int id;
    private final int displayOrder;
    private final String nameZh;
    private final String nameEn;
    private final String trackStaticImageUrl;

    public TropicalCycloneInfo(int id, int displayOrder, String nameZh, String nameEn, String trackStaticImageUrl) {
        this.id = id;
        this.displayOrder = displayOrder;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.trackStaticImageUrl = trackStaticImageUrl;
    }

    public int getId() {
        return id;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public String getNameZh() {
        return nameZh;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getTrackStaticImageUrl() {
        return trackStaticImageUrl;
    }
}
