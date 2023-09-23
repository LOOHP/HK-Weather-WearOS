package com.loohp.hkweatherwarnings.shared

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future


data class UpdateResult<T>(
    val isSuccessful: Boolean,
    val value: T
)


class MapValueState<K, V>(
    private val map: MutableMap<K, V>,
    private val fetchFunction: (K, Context, MapValueState<K, V>) -> V
) {

    fun getValue(key: K, context: Context, executor: ExecutorService, callable: (V) -> Unit = {}): Future<V> {
        val value = map[key]
        return if (value == null) {
            executor.submit(Callable {
                try {
                    val result = fetchFunction.invoke(key, context, this)
                    callable.invoke(result)
                    result
                } catch (e: Throwable) {
                    null
                }
            })
        } else {
            CompletableFuture.completedFuture(value)
        }
    }

    fun getValueState(key: K, context: Context, executor: ExecutorService): MutableState<V?> {
        val value = map[key]
        val state = mutableStateOf(value)
        return if (value == null) {
            getValue(key, context, executor) { state.value = it }
            state
        } else {
            state
        }
    }

}


class DataState<T>(
    private val initialValue: T,
    private val freshness: () -> Long,
    private val updateFunction: (Context, DataState<T>) -> UpdateResult<T>,
    private val updateSuccessCallback: (Context, DataState<T>) -> Unit = { _, _ -> }
) {

    private var state = mutableStateOf(initialValue)
    private var lastSuccessfulUpdateTime: Long = 1
    private var isLastUpdateSuccessful: Boolean = false

    fun getLatestValue(context: Context, executor: ExecutorService, forceReload: Boolean = false, suppressUpdateSuccessCallback: Boolean = false): Future<T> {
        return if (forceReload || System.currentTimeMillis() - lastSuccessfulUpdateTime > freshness.invoke()) {
            update(context, executor, suppressUpdateSuccessCallback)
        } else {
            CompletableFuture.completedFuture(state.value)
        }
    }

    private fun update(context: Context, executor: ExecutorService, suppressUpdateSuccessCallback: Boolean): Future<T> {
        return executor.submit(Callable {
            val result = updateFunction.invoke(context, this)
            if (result.isSuccessful) {
                state.value = result.value
                lastSuccessfulUpdateTime = System.currentTimeMillis()
                isLastUpdateSuccessful = true
                if (!suppressUpdateSuccessCallback) {
                    updateSuccessCallback.invoke(context, this)
                }
            } else {
                isLastUpdateSuccessful = false
            }
            return@Callable state.value
        })
    }

    fun reset() {
        state.value = initialValue
        lastSuccessfulUpdateTime = 0
        isLastUpdateSuccessful = false
    }

    fun getRawValue(): T {
        return state.value
    }

    fun getLastSuccessfulUpdateTime(): Long {
        return lastSuccessfulUpdateTime
    }

    fun isLastUpdateSuccess(): Boolean {
        return isLastUpdateSuccessful
    }

    fun getState(context: Context, executor: ExecutorService): MutableState<T> {
        getLatestValue(context, executor)
        return state
    }

}