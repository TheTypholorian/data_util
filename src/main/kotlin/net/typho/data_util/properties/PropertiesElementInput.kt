package net.typho.data_util.properties

import net.typho.data_util.DataReader
import net.typho.data_util.codec.ValueInput
import net.typho.data_util.codec.ValueType
import net.typho.data_util.error.ErrorStack
import net.typho.data_util.error.push
import java.util.function.BiConsumer
import java.util.function.Consumer

class PropertiesElementInput(
    @JvmField
    val value: String,
    override val errorStack: ErrorStack?
) : ValueInput {
    override val type: ValueType?
        get() = null

    override fun readBoolean() = errorStack.push("Boolean") { value.toBoolean() }

    override fun readByte() = errorStack.push("Byte") { value.toByte() }

    override fun readShort() = errorStack.push("Short") { value.toShort() }

    override fun readInt() = errorStack.push("Int") { value.toInt() }

    override fun readLong() = errorStack.push("Long") { value.toLong() }

    override fun readFloat() = errorStack.push("Float") { value.toFloat() }

    override fun readDouble() = errorStack.push("Double") { value.toDouble() }

    override fun readString() = value

    override fun readDynamicList(out: Consumer<ValueInput>) {
        error("Nested lists/maps is not supported by .properties, switch to .json or .toml if you need to do that.")
    }

    override fun readStaticList(
        size: Int,
        out: Consumer<ValueInput>
    ) {
        error("Nested lists/maps is not supported by .properties, switch to .json or .toml if you need to do that.")
    }

    override fun readDynamicMap(out: BiConsumer<String, ValueInput>) {
        error("Nested lists/maps is not supported by .properties, switch to .json or .toml if you need to do that.")
    }

    override fun readStaticMap(
        keys: List<String>,
        out: Consumer<ValueInput>
    ) {
        error("Nested lists/maps is not supported by .properties, switch to .json or .toml if you need to do that.")
    }

    override fun <V> readOptional(ifPresent: DataReader<V>): V? {
        error("Optional values in lists/maps is not supported by .properties, switch to .json or .toml if you need to do that.")
    }
}