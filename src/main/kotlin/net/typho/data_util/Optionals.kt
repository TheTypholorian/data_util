package net.typho.data_util

import java.util.Optional
import java.util.function.Supplier

internal fun <T : Any> Optional<T>.or(other: Optional<T>): Optional<T> {
    return if (isPresent) this else other
}

internal fun <T : Any> Optional<T>.or(other: Supplier<Optional<T>>): Optional<T> {
    return if (isPresent) this else other.get()
}