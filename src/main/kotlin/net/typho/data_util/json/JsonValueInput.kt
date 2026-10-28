package net.typho.data_util.json

import net.typho.data_util.DataReadException
import net.typho.data_util.DataReader
import net.typho.data_util.codec.ValueInput
import net.typho.data_util.codec.ValueType

class JsonValueInput(
    private var value: Any?
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
        throw DataReadException("Expected $expected, got ${type.name.lowercase()}")
    }

    private inline fun <reified V> peek(expected: String): V {
        if (read) {
            throw DataReadException("Value input has already been read")
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

    override fun readDynamicList() = readList()
        .map { JsonValueInput(it) }
        .iterator()

    override fun readStaticList(size: Int) = readList()
        .also { if (it.size != size) throw DataReadException("List must have $size elements, got ${it.size}") }
        .map { JsonValueInput(it) }
        .iterator()

    fun readMap() = read<Map<String, *>>("map")

    override fun readDynamicMap() = readMap()
        .entries
        .map { (key, value) -> key to JsonValueInput(value) }
        .iterator()

    override fun readStaticMap(keys: List<String>): Iterator<ValueInput> {
        val map = readMap()
        return keys.map { key -> JsonValueInput(map[key]) }.iterator()
    }

    override fun <V> readOptional(ifPresent: DataReader<V>): V? {
        return if (value == null) {
            read = true
            null
        } else {
            ifPresent.read(this)
        }
    }
}