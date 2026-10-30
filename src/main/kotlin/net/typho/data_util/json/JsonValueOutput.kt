package net.typho.data_util.json

import net.typho.data_util.DataWriter
import net.typho.data_util.codec.ValueOutput
import net.typho.data_util.error.ErrorStack
import net.typho.data_util.error.push
import java.util.function.Consumer

@Suppress("UNCHECKED_CAST")
class JsonValueOutput<V>(
    override val errorStack: ErrorStack?
) : ValueOutput.WithResult<V> {
    override val areTypesWritten: Boolean
        get() = true
    private var written = false
    private var value: V? = null

    override fun finish(): V {
        if (!written) {
            error("Value has not been written yet")
        }

        return value as V
    }

    private fun write(value: Any?) {
        if (written) {
            error("Value has already been written")
        }

        this.value = value as V
        written = true
    }

    override fun writeBoolean(v: Boolean) = errorStack.push("Boolean '$v'") { write(v) }

    override fun writeByte(v: Byte) = errorStack.push("Byte '$v'") { write(v) }

    override fun writeShort(v: Short) = errorStack.push("Short '$v'") { write(v) }

    override fun writeInt(v: Int) = errorStack.push("Int '$v'") { write(v) }

    override fun writeLong(v: Long) = errorStack.push("Long '$v'") { write(v) }

    override fun writeFloat(v: Float) = errorStack.push("Float '$v'") { write(v) }

    override fun writeDouble(v: Double) = errorStack.push("Double '$v'") { write(v) }

    override fun writeString(v: String) = errorStack.push("String '$v'") { write(v) }

    override fun writeDynamicList(entries: Collection<Consumer<ValueOutput>>) {
        errorStack.push("Dynamic list (${entries.size} entries)") {
            val list = mutableListOf<Any?>()
            write(list)

            entries.forEachIndexed { index, value ->
                errorStack.push("Entry #${index + 1}") {
                    val output = JsonValueOutput<Any?>(errorStack)
                    value.accept(output)
                    list.add(output.finish())
                }
            }
        }
    }

    override fun writeStaticList(entries: Iterator<Consumer<ValueOutput>>) {
        errorStack.push("Static list") {
            val list = mutableListOf<Any?>()
            write(list)
            var index = 0

            entries.forEach { value ->
                errorStack.push("Entry #${index + 1}") {
                    val output = JsonValueOutput<Any?>(errorStack)
                    value.accept(output)
                    list.add(output.finish())
                }

                index++
            }
        }
    }

    override fun writeDynamicMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>) {
        errorStack.push("Dynamic map") {
            val map = mutableMapOf<String, Any?>()
            write(map)

            entries.forEach { (key, value) ->
                errorStack.push("Key '$key'") {
                    if (map.containsKey(key)) {
                        error("Map has already been written key '$key'")
                    }

                    val output = JsonValueOutput<Any?>(errorStack)
                    value.accept(output)
                    map[key] = output.finish()
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun writeStaticMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>) {
        errorStack.push("Static map") {
            val map = value as? MutableMap<String, Any?> ?: mutableMapOf<String, Any?>().also { write(it) }

            entries.forEach { (key, value) ->
                errorStack.push("Key '$key'") {
                    if (map.containsKey(key)) {
                        error("Map has already been written key '$key'")
                    }

                    val output = JsonValueOutput<Any?>(errorStack)
                    value.accept(output)
                    map[key] = output.finish()
                }
            }
        }
    }

    override fun <V> writeOptional(value: V?, ifPresent: DataWriter<V>) {
        errorStack.push("Optional value '$value' (ifPresent $ifPresent)") {
            if (value == null) {
                write(null)
            } else {
                ifPresent.write(this, value)
            }
        }
    }
}