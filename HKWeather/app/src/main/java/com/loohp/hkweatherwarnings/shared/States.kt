package com.loohp.hkweatherwarnings.shared

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
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
    private val stateMap: MutableMap<K, MutableState<V?>>,
    private val fetchFunction: (K, Context, MapValueState<K, V>) -> UpdateResult<V>
) {

    constructor(map: MutableMap<K, V>, stateMap: MutableMap<K, MutableState<V?>>, fetchFunction: (K, Context, MapValueState<K, V>, Nothing?) -> UpdateResult<Map<K, V>>): this(map, stateMap, { key, context, state ->
        val result = fetchFunction.invoke(key, context, state, null)
        if (result.isSuccessful) {
            for ((k, v) in result.value!!) {
                state.map[k] = v
                stateMap.compute(k) { _, s ->
                    if (s == null) {
                        mutableStateOf(v)
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
        val state = stateMap.computeIfAbsent(key) { mutableStateOf(map[key]) }
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

    fun getValueState(key: K): State<V?> {
        return stateMap.computeIfAbsent(key) { mutableStateOf(map[key]) }
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
    private val updateFunction: (Context, DataState<T>) -> UpdateResult<T>,
    private val updateSuccessCallback: (Context, DataState<T>, T) -> Unit = { _, _, _ -> }
) {

    private var state: MutableState<T>? = null
    private var lastSuccessfulUpdateTime: MutableState<Long>? = null
    private var isLastUpdateSuccessful: MutableState<Boolean>? = null
    private var isCurrentlyUpdating: MutableState<Boolean>? = null

    private var latestFuture: Future<T>? = null

    private fun initializeStateIfNotAlready(context: Context) {
        synchronized (this) {
            if (state == null) {
                val (v, t, s, c) = initializer.invoke(context)
                state = mutableStateOf(v)
                lastSuccessfulUpdateTime = mutableStateOf(t)
                isLastUpdateSuccessful = mutableStateOf(s)
                isCurrentlyUpdating = mutableStateOf(c)
            }
        }
    }

    fun getLatestValue(context: Context, executor: ExecutorService, forceReload: Boolean = freshness.invoke(context) >= Shared.NEVER_REFRESH_INTERVAL): Future<T> {
        initializeStateIfNotAlready(context)
        synchronized (this) {
            latestFuture?.let { if (!it.isDone) return it }
            latestFuture = if (forceReload || System.currentTimeMillis() - lastSuccessfulUpdateTime!!.value > freshness.invoke(context)) {
                update(context, executor)
            } else {
                CompletableFuture.completedFuture(state!!.value)
            }
            return latestFuture!!
        }
    }

    private fun update(context: Context, executor: ExecutorService): Future<T> {
        isCurrentlyUpdating!!.value = true
        return executor.submit(Callable {
            try {
                val result = updateFunction.invoke(context, this)
                if (result.isSuccessful) {
                    state!!.value = result.value!!
                    lastSuccessfulUpdateTime!!.value = System.currentTimeMillis()
                    isLastUpdateSuccessful!!.value = true
                    updateSuccessCallback.invoke(context, this, result.value)
                } else {
                    isLastUpdateSuccessful!!.value = false
                }
                return@Callable state!!.value
            } finally {
                isCurrentlyUpdating!!.value = false
            }
        })
    }

    fun reset(context: Context) {
        initializeStateIfNotAlready(context)
        state!!.value = defaultValue
        lastSuccessfulUpdateTime!!.value = 0
        isLastUpdateSuccessful!!.value = false
        isCurrentlyUpdating!!.value = true
        resetCallback.invoke(context)
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

    fun getLastSuccessfulUpdateTimeState(context: Context): State<Long> {
        initializeStateIfNotAlready(context)
        return lastSuccessfulUpdateTime!!
    }

    fun getLastUpdateSuccessState(context: Context): State<Boolean> {
        initializeStateIfNotAlready(context)
        return isLastUpdateSuccessful!!
    }

    fun getCurrentlyUpdatingState(context: Context): State<Boolean> {
        initializeStateIfNotAlready(context)
        return isCurrentlyUpdating!!
    }

    fun getState(context: Context): State<T> {
        initializeStateIfNotAlready(context)
        return state!!
    }

}