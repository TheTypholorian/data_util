package net.typho.data_util.codec

import net.typho.data_util.SingleValueInput
import net.typho.data_util.SingleValueOutput
import java.util.function.Function

interface VersionedCodec<V, T>: Codec<T> {
    companion object {
        @JvmOverloads
        @JvmStatic
        fun <V, T> of(key: String = "version", codec: Codec<V>, versionGetter: Function<T, V>, codecFunction: Function<V, Codec<T>>, fallback: V? = null) = object : VersionedCodec<V, T> {
            override val versionKey: String
                get() = key
            override val versionCodec: Codec<V>
                get() = codec
            override val versionGetter: Function<T, V>
                get() = versionGetter
            override val fallbackVersion: V?
                get() = fallback

            override fun getCodec(version: V): Codec<T> {
                return codecFunction.apply(version)
            }
        }
    }

    val versionKey: String
    val versionCodec: Codec<V>
    val versionGetter: Function<T, V>
    val fallbackVersion: V?

    fun getCodec(version: V): Codec<T>

    @Suppress("UNCHECKED_CAST")
    override fun read(input: SingleValueInput): T {
        val version = (input.readVersion(versionKey)?.let { versionCodec.read(it) } ?: fallbackVersion) as V
        return getCodec(version).read(input)
    }

    override fun write(output: SingleValueOutput, value: T) {
        val version = versionGetter.apply(value)
        getCodec(version).write(output, value)
    }
}