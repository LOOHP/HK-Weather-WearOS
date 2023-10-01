package com.loohp.hkweatherwarnings.utils


fun <T, A, B, C> Triple<T, T, T>.map(mapping: (T) -> Any?): Triple<A, B, C> {
    @Suppress("UNCHECKED_CAST")
    return Triple(mapping.invoke(first) as A, mapping.invoke(second) as B, mapping.invoke(third) as C)
}

fun <T> Triple<T, T, T>.any(predicate: (T) -> Boolean): Boolean {
    return predicate.invoke(first) || predicate.invoke(second) || predicate.invoke(third)
}