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

package com.loohp.hkweatherwarnings.shared

import android.content.Context
import com.loohp.hkweatherwarnings.utils.CompletableFutureWithIntermediateValue
import com.loohp.hkweatherwarnings.utils.FutureWithIntermediateValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future


class UpdateResult<T> private constructor(
    val isSuccessful: Boolean,
    val value: T?
) {
    companion object {

        fun <T> success(value: T): UpdateResult<T> {
            return UpdateResult(true, value)
        }

        fun <T> failed(): UpdateResult<T> {
            return UpdateResult(false, null)
        }

    }
}


class MapValueState<K, V>(
    private val map: MutableMap<K, V>,
    private val stateMap: MutableMap<K, MutableStateFlow<V?>>,
    private val fetchFunction: (K, Context, MapValueState<K, V>) -> UpdateResult<V>
) {

    constructor(map: MutableMap<K, V>, stateMap: MutableMap<K, MutableStateFlow<V?>>, fetchFunction: (K, Context, MapValueState<K, V>, Nothing?) -> UpdateResult<Map<K, V>>): this(map, stateMap, { key, context, state ->
        val result = fetchFunction.invoke(key, context, state, null)
        if (result.isSuccessful) {
            for ((k, v) in result.value!!) {
                state.map[k] = v
                stateMap.compute(k) { _, s ->
                    if (s == null) {
                        MutableStateFlow(v)
                    } else {
                        s.value = v
                        s
                    }
                }
            }
            val value = state.map[key]
            if (value == null) {
                UpdateResult.failed()
            } else {
                UpdateResult.success(value)
            }
        } else {
            UpdateResult.failed()
        }
    })

    fun getValue(key: K, context: Context, executor: ExecutorService): Future<V> {
        val state = stateMap.computeIfAbsent(key) { MutableStateFlow(map[key]) }
        val value = map[key]
        return if (value == null) {
            executor.submit(Callable {
                val newValue = try {
                    val result = fetchFunction.invoke(key, context, this)
                    if (result.isSuccessful) result.value else null
                } catch (e: Throwable) {
                    e.printStackTrace()
                    null
                }
                if (newValue != null) {
                    map[key] = newValue
                    state.value = newValue
                }
                return@Callable newValue
            })
        } else {
            state.value = value
            CompletableFuture.completedFuture(value)
        }
    }

    fun getValueState(key: K): StateFlow<V?> {
        return stateMap.computeIfAbsent(key) { MutableStateFlow(map[key]) }
    }

}


data class DataStateInitializeResult<T> (
    val initialValue: T,
    val lastSuccessfulUpdateTime: Long,
    val isLastUpdateSuccessful: Boolean,
    val isCurrentlyUpdating: Boolean
) {
    companion object {

        fun <T> defaultEmpty(initialValue: T): DataStateInitializeResult<T> {
            return DataStateInitializeResult(
                initialValue = initialValue,
                lastSuccessfulUpdateTime = 1,
                isLastUpdateSuccessful = false,
                isCurrentlyUpdating = true
            )
        }

    }
}


class DataState<T>(
    private val defaultValue: T,
    private val initializer: (Context) -> DataStateInitializeResult<T>,
    private val resetCallback: (Context) -> Unit,
    private val freshness: (Context) -> Long,
    private val updateFunction: (Context, DataState<T>, MutableStateFlow<Float>) -> UpdateResult<T>,
    private val updateSuccessCallback: (Context, DataState<T>, T) -> Unit = { _, _, _ -> },
    private val updateFailedCallback: (Context, DataState<T>) -> Unit = { _, _ -> }
) {

    private var state: MutableStateFlow<T>? = null
    private var lastSuccessfulUpdateTime: MutableStateFlow<Long>? = null
    private var isLastUpdateSuccessful: MutableStateFlow<Boolean>? = null
    private var isCurrentlyUpdating: MutableStateFlow<Boolean>? = null

    private var updateProgress: MutableStateFlow<Float>? = null
    private var latestFuture: FutureWithIntermediateValue<T>? = null

    private fun initializeStateIfNotAlready(context: Context) {
        synchronized (this) {
            if (state == null) {
                val (v, t, s, c) = initializer.invoke(context)
                state = MutableStateFlow(v)
                lastSuccessfulUpdateTime = MutableStateFlow(t)
                isLastUpdateSuccessful = MutableStateFlow(s)
                isCurrentlyUpdating = MutableStateFlow(c)
                updateProgress = MutableStateFlow(if (s) 1F else 0F)
            }
        }
    }

    fun getLatestValue(context: Context, executor: ExecutorService, forceReload: Boolean = freshness.invoke(context) >= Shared.NEVER_REFRESH_INTERVAL): FutureWithIntermediateValue<T> {
        initializeStateIfNotAlready(context)
        synchronized (this) {
            latestFuture?.let { if (!it.isDone) return it }
            latestFuture = if (forceReload || System.currentTimeMillis() - lastSuccessfulUpdateTime!!.value > freshness.invoke(context)) {
                updateProgress!!.value = 0F
                update(context, executor)
            } else {
                CompletableFutureWithIntermediateValue.completedFuture(state!!.value)
            }
            return latestFuture!!
        }
    }

    private fun update(context: Context, executor: ExecutorService): FutureWithIntermediateValue<T> {
        isCurrentlyUpdating!!.value = true
        val future: CompletableFutureWithIntermediateValue<T> = CompletableFutureWithIntermediateValue(getCachedValue(context))
        executor.execute {
            try {
                val result = updateFunction.invoke(context, this, updateProgress!!)
                if (result.isSuccessful) {
                    state!!.value = result.value!!
                    lastSuccessfulUpdateTime!!.value = System.currentTimeMillis()
                    isLastUpdateSuccessful!!.value = true
                    updateProgress!!.value = 1F
                    updateSuccessCallback.invoke(context, this, result.value)
                } else {
                    isLastUpdateSuccessful!!.value = false
                    updateFailedCallback.invoke(context, this)
                }
                future.complete(state!!.value)
            } finally {
                isCurrentlyUpdating!!.value = false
            }
        }
        return future
    }

    fun reset(context: Context) {
        initializeStateIfNotAlready(context)
        state!!.value = defaultValue
        lastSuccessfulUpdateTime!!.value = 0
        isLastUpdateSuccessful!!.value = false
        isCurrentlyUpdating!!.value = true
        updateProgress!!.value = 0F
        resetCallback.invoke(context)
    }

    fun getCurrentProgress(context: Context): Float {
        initializeStateIfNotAlready(context)
        return updateProgress!!.value
    }

    fun getCurrentProgressState(context: Context): StateFlow<Float> {
        initializeStateIfNotAlready(context)
        return updateProgress!!
    }

    fun getCachedValue(context: Context): T {
        initializeStateIfNotAlready(context)
        return state!!.value
    }

    fun getLastSuccessfulUpdateTime(context: Context): Long {
        initializeStateIfNotAlready(context)
        return lastSuccessfulUpdateTime!!.value
    }

    fun isLastUpdateSuccess(context: Context): Boolean {
        initializeStateIfNotAlready(context)
        return isLastUpdateSuccessful!!.value
    }

    fun isCurrentlyUpdating(context: Context): Boolean {
        initializeStateIfNotAlready(context)
        return isCurrentlyUpdating!!.value
    }

    fun getLastSuccessfulUpdateTimeState(context: Context): StateFlow<Long> {
        initializeStateIfNotAlready(context)
        return lastSuccessfulUpdateTime!!
    }

    fun getLastUpdateSuccessState(context: Context): StateFlow<Boolean> {
        initializeStateIfNotAlready(context)
        return isLastUpdateSuccessful!!
    }

    fun getCurrentlyUpdatingState(context: Context): StateFlow<Boolean> {
        initializeStateIfNotAlready(context)
        return isCurrentlyUpdating!!
    }

    fun getState(context: Context): StateFlow<T> {
        initializeStateIfNotAlready(context)
        return state!!
    }

}