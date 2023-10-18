/*
 * This file is part of HKWeather.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class LocationUtils {

    public static boolean checkLocationPermission(Context context) {
        return (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean checkLocationPermission(ComponentActivity activity, boolean askIfNotGranted) {
        return checkLocationPermission(activity, askIfNotGranted, r -> {});
    }

    public static boolean checkLocationPermission(ComponentActivity activity, Consumer<Boolean> callback) {
        return checkLocationPermission(activity, true, callback);
    }

    private static boolean checkLocationPermission(ComponentActivity activity, boolean askIfNotGranted, Consumer<Boolean> callback) {
        if ((ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (askIfNotGranted) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                AtomicReference<ActivityResultLauncher<String>> ref0 = new AtomicReference<>();
                ActivityResultLauncher<String> launcher0 = activity.getActivityResultRegistry().register(UUID.randomUUID().toString(), new ActivityResultContracts.RequestPermission(), result0 -> {
                    callback.accept(result0);
                    ref0.get().unregister();
                });
                ref0.set(launcher0);
                launcher0.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            } else {
                AtomicReference<ActivityResultLauncher<String>> ref = new AtomicReference<>();
                ActivityResultLauncher<String> launcher = activity.getActivityResultRegistry().register(UUID.randomUUID().toString(), new ActivityResultContracts.RequestPermission(), result -> {
                    if (result) {
                        AtomicReference<ActivityResultLauncher<String>> ref0 = new AtomicReference<>();
                        ActivityResultLauncher<String> launcher0 = activity.getActivityResultRegistry().register(UUID.randomUUID().toString(), new ActivityResultContracts.RequestPermission(), result0 -> {
                            callback.accept(result0);
                            ref0.get().unregister();
                        });
                        ref0.set(launcher0);
                        launcher0.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                        ref.get().unregister();
                    } else {
                        callback.accept(false);
                    }
                });
                ref.set(launcher);
                launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }
        return false;
    }

    public static CompletableFuture<LocationResult> getGPSLocation(Context context) {
        if (!checkLocationPermission(context)) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<LocationResult> future = new CompletableFuture<>();
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(context);
        ForkJoinPool.commonPool().execute(() -> {
            client.getLocationAvailability().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().isLocationAvailable()) {
                    client.getCurrentLocation(new CurrentLocationRequest.Builder().setMaxUpdateAgeMillis(30000).setDurationMillis(60000).build(), null).addOnCompleteListener(t -> future.complete(LocationResult.fromTask(t)));
                } else {
                    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, ForkJoinPool.commonPool(), loc -> future.complete(LocationResult.ofNullable(loc)));
                    } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, ForkJoinPool.commonPool(), loc -> future.complete(LocationResult.ofNullable(loc)));
                    } else {
                        future.complete(LocationResult.FAILED_RESULT);
                    }
                }
            });
        });
        return future;
    }

    public static CompletableFuture<LocationResult> getGPSLocation(ComponentActivity activity) {
        if (!checkLocationPermission(activity, false)) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<LocationResult> future = new CompletableFuture<>();
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(activity);
        ForkJoinPool.commonPool().execute(() -> {
            client.getLocationAvailability().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().isLocationAvailable()) {
                    client.getCurrentLocation(new CurrentLocationRequest.Builder().build(), null).addOnCompleteListener(t -> future.complete(LocationResult.fromTask(t)));
                } else {
                    LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        locationManager.getCurrentLocation(LocationManager.GPS_PROVIDER, null, ForkJoinPool.commonPool(), loc -> future.complete(LocationResult.ofNullable(loc)));
                    } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, null, ForkJoinPool.commonPool(), loc -> future.complete(LocationResult.ofNullable(loc)));
                    } else {
                        future.complete(LocationResult.FAILED_RESULT);
                    }
                }
            });
        });
        return future;
    }

    public static class LocationResult {

        public static final LocationResult FAILED_RESULT = new LocationResult(null);

        public static LocationResult fromTask(Task<Location> task) {
            if (!task.isSuccessful()) {
                return FAILED_RESULT;
            }
            return new LocationResult(task.getResult());
        }

        public static LocationResult fromLatLng(double lat, double lng) {
            Location location = new Location("custom");
            location.setLatitude(lat);
            location.setLongitude(lng);
            return new LocationResult(location);
        }

        public static LocationResult ofNullable(Location location) {
            return new LocationResult(location);
        }

        private final Location location;

        private LocationResult(Location location) {
            this.location = location;
        }

        public boolean isSuccess() {
            return location != null;
        }

        public Location getLocation() {
            return location;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LocationResult that = (LocationResult) o;
            return Objects.equals(location, that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location);
        }
    }

}
