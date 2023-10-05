package com.loohp.hkweatherwarnings.weather;

import androidx.compose.runtime.Immutable;

import java.util.Objects;

@Immutable
public class TropicalCycloneInfo {

    private final int id;
    private final int displayOrder;
    private final String nameZh;
    private final String nameEn;
    private final String trackStaticImageUrl;
    private final String trackStaticZoomImageUrl;

    public TropicalCycloneInfo(int id, int displayOrder, String nameZh, String nameEn, String trackStaticImageUrl, String trackStaticZoomImageUrl) {
        this.id = id;
        this.displayOrder = displayOrder;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.trackStaticImageUrl = trackStaticImageUrl;
        this.trackStaticZoomImageUrl = trackStaticZoomImageUrl;
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

    public String getTrackStaticZoomImageUrl() {
        return trackStaticZoomImageUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TropicalCycloneInfo that = (TropicalCycloneInfo) o;
        return id == that.id && displayOrder == that.displayOrder && Objects.equals(nameZh, that.nameZh) && Objects.equals(nameEn, that.nameEn) && Objects.equals(trackStaticImageUrl, that.trackStaticImageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayOrder, nameZh, nameEn, trackStaticImageUrl);
    }
}
