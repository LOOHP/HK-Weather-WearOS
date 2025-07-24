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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public class JsonUtils {

    public static int indexOf(JSONArray array, Object obj) {
        for (int i = 0; i < array.length(); i++) {
            if (Objects.equals(array.opt(i), obj)) {
                return i;
            }
        }
        return -1;
    }

    public static <T> List<T> mapToList(JSONArray array, Function<Object, T> mapping) {
        List<T> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            list.add(mapping.apply(array.opt(i)));
        }
        return list;
    }

    public static boolean contains(JSONArray array, Object obj) {
        return indexOf(array, obj) >= 0;
    }

    public static boolean containsKey(JSONObject object, Object obj) {
        return StreamSupport.stream(((Iterable<String>) object::keys).spliterator(), false).anyMatch(e -> Objects.equals(e, obj));
    }

    /** @noinspection unchecked*/
    public static <T> List<T> toList(JSONArray array, Class<T> type) {
        List<T> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            list.add((T) array.opt(i));
        }
        return list;
    }

    /** @noinspection unchecked*/
    public static <T> Set<T> toSet(JSONArray array, Class<T> type) {
        Set<T> set = new LinkedHashSet<>();
        for (int i = 0; i < array.length(); i++) {
            set.add((T) array.opt(i));
        }
        return set;
    }

    public static <T> JSONArray fromCollection(Collection<T> list) {
        JSONArray array = new JSONArray();
        for (T t : list) {
            array.put(t);
        }
        return array;
    }

}
