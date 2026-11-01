package net.typho.data_util.properties

import net.typho.data_util.DataWriter
import net.typho.data_util.codec.ValueOutput
import net.typho.data_util.error.ErrorStack
import net.typho.data_util.error.push
import java.util.function.Consumer

class PropertiesValueOutput(
    @JvmField
    val path: String,
    @JvmField
    val map: MutableMap<String, String>,
    override val errorStack: ErrorStack?
) : ValueOutput.WithResult<Map<String, String>> {
    override val areTypesWritten: Boolean
        get() = false

    override fun finish() = map

    private fun write(v: String) {
        if (map.containsKey(path)) {
            error("Key '$path' has already been written")
        }

        map[path] = v
    }

    private fun path(path: String) = if (this.path.isEmpty()) path else "${this.path}.$path"

    override fun writeBoolean(v: Boolean) = errorStack.push("Boolean '$v'") { write(v.toString()) }

    override fun writeByte(v: Byte) = errorStack.push("Byte '$v'") { write(v.toString()) }

    override fun writeShort(v: Short) = errorStack.push("Short '$v'") { write(v.toString()) }

    override fun writeInt(v: Int) = errorStack.push("Int '$v'") { write(v.toString()) }

    override fun writeLong(v: Long) = errorStack.push("Long '$v'") { write(v.toString()) }

    override fun writeFloat(v: Float) = errorStack.push("Float '$v'") { write(v.toString()) }

    override fun writeDouble(v: Double) = errorStack.push("Double '$v'") { write(v.toString()) }

    override fun writeString(v: String) = errorStack.push("String '$v'") { write(v) }

    override fun writeDynamicList(entries: Collection<Consumer<ValueOutput>>) {
        errorStack.push("Dynamic list (${entries.size} entries)") {
            val entries = entries.map {
                val output = PropertiesElementOutput(errorStack)
                it.accept(output)
                output.finish().also { if (it.contains(',')) error("List entry '$it' must not contain ','") }
            }
            write(entries.joinToString(separator = ","))
        }
    }

    override fun writeStaticList(entries: Iterator<Consumer<ValueOutput>>) {
        errorStack.push("Static list") {
            val entries = entries.asSequence().map {
                val output = PropertiesElementOutput(errorStack)
                it.accept(output)
                output.finish().also { if (it.contains(',')) error("List entry '$it' must not contain ','") }
            }
            write(entries.joinToString(separator = ","))
        }
    }

    override fun writeDynamicMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>) {
        errorStack.push("Dynamic map") {
            val entries = entries.asSequence().map { (key, value) ->
                if (key.contains('=') || key.contains(',')) {
                    error("Map key '$key' must not contain '=' or ','")
                }

                val output = PropertiesElementOutput(errorStack)
                value.accept(output)
                key + "=" + output.finish().also { if (it.contains(',')) error("Map value '$it' must not contain ','") }
            }
            write(entries.joinToString(separator = ","))
        }
    }

    override fun writeStaticMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>) {
        errorStack.push("Static map") {
            entries.forEach { (key, value) ->
                value.accept(PropertiesValueOutput(path(key), map, errorStack))
            }
        }
    }

    override fun <V> writeOptional(value: V?, ifPresent: DataWriter<V>) {
        errorStack.push("Optional value") {
            if (value != null) {
                ifPresent.write(this, value)
            }
        }
    }
}