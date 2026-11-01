package net.typho.data_util.properties

import net.typho.data_util.DataWriter
import net.typho.data_util.codec.ValueOutput
import net.typho.data_util.error.ErrorStack
import net.typho.data_util.error.push
import java.util.function.Consumer

class PropertiesElementOutput(
    override val errorStack: ErrorStack?
) : ValueOutput.WithResult<String> {
    override val areTypesWritten: Boolean
        get() = false
    private var value: String? = null

    override fun finish(): String {
        return value ?: error("Value has not been written yet")
    }

    private fun write(value: String) {
        if (this.value != null) {
            error("Value has already been written")
        }

        this.value = value
    }

    override fun writeBoolean(v: Boolean) = errorStack.push("Boolean '$v'") { write(v.toString()) }

    override fun writeByte(v: Byte) = errorStack.push("Byte '$v'") { write(v.toString()) }

    override fun writeShort(v: Short) = errorStack.push("Short '$v'") { write(v.toString()) }

    override fun writeInt(v: Int) = errorStack.push("Int '$v'") { write(v.toString()) }

    override fun writeLong(v: Long) = errorStack.push("Long '$v'") { write(v.toString()) }

    override fun writeFloat(v: Float) = errorStack.push("Float '$v'") { write(v.toString()) }

    override fun writeDouble(v: Double) = errorStack.push("Double '$v'") { write(v.toString()) }

    override fun writeString(v: String) = errorStack.push("String '$v'") { write(v) }

    override fun writeDynamicList(entries: Collection<Consumer<ValueOutput>>) {
        error("Nested lists/maps is not supported by .properties, switch to .json or .toml if you need to do that.")
    }

    override fun writeStaticList(entries: Iterator<Consumer<ValueOutput>>) {
        error("Nested lists/maps is not supported by .properties, switch to .json or .toml if you need to do that.")
    }

    override fun writeDynamicMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>) {
        error("Nested lists/maps is not supported by .properties, switch to .json or .toml if you need to do that.")
    }

    override fun writeStaticMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>) {
        error("Nested lists/maps is not supported by .properties, switch to .json or .toml if you need to do that.")
    }

    override fun <V> writeOptional(value: V?, ifPresent: DataWriter<V>) {
        error("Optional values in lists/maps is not supported by .properties, switch to .json or .toml if you need to do that.")
    }
}