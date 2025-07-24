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

package com.loohp.hkweatherwarnings.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    public static String capitalize(String str) {
        return capitalize(str, true);
    }

    public static String capitalize(String str, boolean lower) {
        if (lower) {
            str = str.toLowerCase();
        }
        StringBuffer sb = new StringBuffer();
        Matcher matcher = Pattern.compile("(?:^|\\s|[\"'(\\[{])+\\S").matcher(str);
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group().toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String padStart(String inputString, int length, char c) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append(c);
        }
        sb.append(inputString);

        return sb.toString();
    }

    public static int scaledSize(int size, Context context) {
        return Math.round(scaledSize((float) size, context));
    }

    public static float scaledSize(float size, Context context) {
        int dimension = ScreenSizeUtils.getMinScreenSize(context);
        float scale = dimension / 454F;
        return size * scale;
    }

    public static float scaledHeight(float size, Context context) {
        int dimension = ScreenSizeUtils.getScreenHeight(context);
        float scale = dimension / 454F;
        return size * scale;
    }

    public static float scaledWidth(float size, Context context) {
        int dimension = ScreenSizeUtils.getScreenWidth(context);
        float scale = dimension / 454F;
        return size * scale;
    }

    public static float findOptimalSp(Context context, String text, int targetWidth, int maxLines, float minSp, float maxSp) {
        TextPaint paint = new TextPaint();
        paint.density = context.getResources().getDisplayMetrics().density;
        for (float sp = maxSp; sp >= minSp; sp--) {
            paint.setTextSize(UnitUtils.spToPixels(context, sp));
            paint.setTypeface(Typeface.DEFAULT);

            StaticLayout staticLayout = StaticLayout.Builder.obtain(text, 0, text.length(), paint, targetWidth)
                    .setMaxLines(Integer.MAX_VALUE)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build();

            if (staticLayout.getLineCount() <= maxLines) {
                return sp;
            }
        }
        return minSp;
    }

    public static float findOptimalSpForHeight(Context context, String text, int targetWidth, int targetHeight, float minSp, float maxSp) {
        TextPaint paint = new TextPaint();
        paint.density = context.getResources().getDisplayMetrics().density;
        for (float sp = maxSp; sp >= minSp; sp--) {
            paint.setTextSize(UnitUtils.spToPixels(context, sp));
            paint.setTypeface(Typeface.DEFAULT);

            StaticLayout staticLayout = StaticLayout.Builder.obtain(text, 0, text.length(), paint, targetWidth)
                    .setMaxLines(Integer.MAX_VALUE)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .build();

            if (staticLayout.getHeight() < targetHeight) {
                return sp;
            }
        }
        return minSp;
    }

}
