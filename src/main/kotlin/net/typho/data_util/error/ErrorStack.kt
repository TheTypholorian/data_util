package net.typho.data_util.error

import net.typho.data_util.DataFormat
import net.typho.data_util.codec.Codec

fun <R> ErrorStack?.push(key: String, func: () -> R): R {
    return if (this == null) func() else push(key, func)
}

class ErrorStack(
    @JvmField
    var valueBeingSerialized: Any? = null,
    @JvmField
    var codec: Codec<*>? = null,
    @JvmField
    var format: DataFormat<*>? = null,
    @JvmField
    val path: MutableList<String> = mutableListOf()
) {
    fun push(key: String) {
        path.add(key)
    }

    fun <R> push(key: String, func: () -> R): R {
        push(key)
        val r = func()
        pop()
        return r
    }

    fun pop() = path.removeLast()

    fun isEmpty() = valueBeingSerialized == null && codec == null && format == null && path.isEmpty()

    override fun toString() = buildList {
        if (path.isNotEmpty()) {
            add("Path: ${path.joinToString(separator = " > ")}")
        }

        valueBeingSerialized?.let { add("Value being serialized: $it") }
        codec?.let { add("Codec: $it") }
        format?.let { add("Format: $it") }
    }.joinToString(separator = "\n")
}