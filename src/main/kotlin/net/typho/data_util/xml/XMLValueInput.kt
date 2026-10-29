package net.typho.data_util.xml

import net.typho.data_util.DataReadException
import net.typho.data_util.DataReader
import net.typho.data_util.codec.ValueInput
import net.typho.data_util.codec.ValueType

class XMLValueInput(
    private val value: XMLNode
): ValueInput {
    private var read = false

    override val type: ValueType?
        get() = null

    private fun castError(expected: String): Nothing {
        throw DataReadException("Expected $expected, got ${value.javaClass.name}")
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

    override fun readBoolean() = readString().toBoolean()

    override fun readByte() = readString().toByte()

    override fun readShort() = readString().toShort()

    override fun readInt() = readString().toInt()

    override fun readLong() = readString().toLong()

    override fun readFloat() = readString().toFloat()

    override fun readDouble() = readString().toDouble()

    override fun readString() = read<XMLText>("text").text

    override fun readDynamicList() = read<XMLElement>("list")
        .children
        .map { XMLValueInput((it as XMLElement).children.single()) }
        .iterator()

    override fun readStaticList(size: Int) = read<XMLElement>("list")
        .children
        .also { if (it.size != size) throw DataReadException("List must have $size elements, got ${it.size}") }
        .map { XMLValueInput((it as XMLElement).children.single()) }
        .iterator()

    override fun readDynamicMap() = read<XMLElement>("list")
        .children
        .map { (it as XMLElement).name to XMLValueInput(it.children.single()) }
        .iterator()

    override fun readStaticMap(keys: List<String>) = read<XMLElement>("list")
        .children
        .map { XMLValueInput((it as XMLElement).children.single()) }
        .iterator()

    override fun <V> readOptional(ifPresent: DataReader<V>): V? {
        return if (value is XMLText && value.text == "null") {
            null
        } else {
            ifPresent.read(this)
        }
    }
}