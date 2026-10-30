package net.typho.data_util.xml

import net.typho.data_util.DataReader
import net.typho.data_util.codec.ValueInput
import net.typho.data_util.codec.ValueType
import java.util.function.BiConsumer
import java.util.function.Consumer

class XMLValueInput(
    private val value: XMLNode
): ValueInput {
    private var read = false

    override val type: ValueType?
        get() = null

    private fun castError(expected: String): Nothing {
        error("Expected $expected, got ${value.javaClass.name}")
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

    override fun readBoolean() = readString().toBoolean()

    override fun readByte() = readString().toByte()

    override fun readShort() = readString().toShort()

    override fun readInt() = readString().toInt()

    override fun readLong() = readString().toLong()

    override fun readFloat() = readString().toFloat()

    override fun readDouble() = readString().toDouble()

    override fun readString() = read<XMLText>("text").text

    override fun readDynamicList(out: Consumer<ValueInput>) {
        read<XMLElement>("list")
            .children
            .forEach {
                out.accept(XMLValueInput((it as XMLElement).children.single()))
            }
    }

    override fun readStaticList(size: Int, out: Consumer<ValueInput>) {
        read<XMLElement>("list")
            .children
            .also { if (it.size != size) error("List must have $size elements, got ${it.size}") }
            .forEach { out.accept(XMLValueInput((it as XMLElement).children.single())) }
    }

    override fun readDynamicMap(out: BiConsumer<String, ValueInput>) {
        read<XMLElement>("list")
            .children
            .forEach { out.accept((it as XMLElement).name, XMLValueInput(it.children.single())) }
    }

    override fun readStaticMap(keys: List<String>, out: Consumer<ValueInput>) {
        read<XMLElement>("list")
            .children
            .forEach { out.accept(XMLValueInput((it as XMLElement).children.single())) }
    }

    override fun <V> readOptional(ifPresent: DataReader<V>): V? {
        return if (value is XMLText && value.text == "null") {
            null
        } else {
            ifPresent.read(this)
        }
    }
}