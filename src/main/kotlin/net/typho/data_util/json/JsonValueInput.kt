package net.typho.data_util.json

import net.typho.data_util.DataReader
import net.typho.data_util.codec.ValueInput
import net.typho.data_util.codec.ValueType
import net.typho.data_util.error.ErrorStack
import net.typho.data_util.error.push
import java.util.function.BiConsumer
import java.util.function.Consumer

class JsonValueInput(
    private var value: Any?,
    override val errorStack: ErrorStack?
): ValueInput {
    private var read = false

    override val type: ValueType = when (value) {
        null -> ValueType.NULL
        is Boolean -> ValueType.BOOLEAN
        is Number -> ValueType.NUMBER
        is String -> ValueType.STRING
        is List<*> -> ValueType.ARRAY
        is Map<*, *> -> ValueType.MAP
        else -> throw IllegalArgumentException(value.toString())
    }

    private fun castError(expected: String): Nothing {
        error("Expected $expected, got ${type.name.lowercase()}")
    }

    private inline fun <reified V> peek(expected: String): V {
        if (read) {
            error("Value input has already been read")
        }

        return value as? V ?: castError(expected)
    }

    private inline fun <reified V> read(expected: String): V {
        val value = peek<V>(expected)
        read = true
        return value
    }

    override fun readBoolean() = read<Boolean>("boolean")

    override fun readByte() = read<Number>("byte").toByte()

    override fun readShort() = read<Number>("short").toShort()

    override fun readInt() = read<Number>("int").toInt()

    override fun readLong() = read<Number>("long").toLong()

    override fun readFloat() = read<Number>("float").toFloat()

    override fun readDouble() = read<Number>("double").toDouble()

    override fun readString() = read<String>("string")

    fun readList() = read<List<*>>("list")

    override fun readDynamicList(out: Consumer<ValueInput>) {
        errorStack.push("Dynamic list") {
            val list = readList()
            list.forEachIndexed { index, value ->
                errorStack.push("Entry #${index + 1}") {
                    out.accept(JsonValueInput(value, errorStack))
                }
            }
        }
    }

    override fun readStaticList(size: Int, out: Consumer<ValueInput>) {
        errorStack.push("Static list ($size entries)") {
            val list = readList()

            if (list.size != size) {
                error("List must have $size elements, got ${list.size}")
            }

            list.forEachIndexed { index, value ->
                errorStack.push("Entry #${index + 1}") {
                    out.accept(JsonValueInput(value, errorStack))
                }
            }
        }
    }

    fun readMap() = read<Map<String, *>>("map")

    override fun readDynamicMap(out: BiConsumer<String, ValueInput>) {
        errorStack.push("Dynamic map") {
            readMap().forEach { (key, value) ->
                errorStack.push("Key '$key'") {
                    out.accept(key, JsonValueInput(value, errorStack))
                }
            }
        }
    }

    override fun readStaticMap(keys: List<String>, out: Consumer<ValueInput>) {
        errorStack.push("Static map") {
            val map = readMap()
            keys.forEach { key ->
                errorStack.push("Key '$key'") {
                    out.accept(JsonValueInput(map[key], errorStack))
                }
            }
        }
    }

    override fun <V> readOptional(ifPresent: DataReader<V>): V? {
        return errorStack.push("Optional value (ifPresent $ifPresent)") {
            if (value == null) {
                read = true
                null
            } else {
                ifPresent.read(this)
            }
        }
    }
}