package net.typho.data_util.codec

import java.lang.reflect.Field
import kotlin.reflect.KProperty1

class CodecReflectionConfig<T : Any>(
    @JvmField
    val cls: Class<T>
) {
    @JvmField
    val fields = mutableMapOf<String, Codec<*>>()

    fun field(name: String, codec: Codec<*>) {
        fields[name] = codec
    }

    fun field(field: Field, codec: Codec<*>) {
        fields[field.name] = codec
    }

    fun <F> field(field: KProperty1<T, F>, codec: Codec<F>) {
        fields[field.name] = codec
    }
}