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
