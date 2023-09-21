package com.loohp.hkweatherwarnings.weather;

public enum UVIndexType {

    LOW(0, 2, "低", "Low", 0xFF289500),
    MODERATE(3, 5, "中", "Moderate", 0xFFF7E400),
    HIGH(6, 7, "高", "High", 0xFFF85900),
    VERY_HIGH(8, 10, "甚高", "Very High", 0xFFD8001D),
    EXTREME(11, Integer.MAX_VALUE, "極高", "Extreme", 0xFF6B49C8);

    private static final UVIndexType[] VALUES = values();

    private final int min;
    private final int max;
    private final String zh;
    private final String en;
    private final int color;

    UVIndexType(int min, int max, String zh, String en, int color) {
        this.min = min;
        this.max = max;
        this.zh = zh;
        this.en = en;
        this.color = color;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public String getZh() {
        return zh;
    }

    public String getEn() {
        return en;
    }

    public int getColor() {
        return color;
    }

    public static UVIndexType getByValue(float value) {
        if (value < 0) {
            return null;
        }
        for (UVIndexType type : VALUES) {
            if (type.getMin() <= value && value <= type.getMax()) {
                return type;
            }
        }
        return null;
    }
}
