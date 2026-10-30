package net.typho.data_util.xml

import net.typho.data_util.DataReader
import net.typho.data_util.codec.ValueInput
import net.typho.data_util.codec.ValueType
import net.typho.data_util.error.ErrorStack
import net.typho.data_util.error.push
import java.util.function.BiConsumer
import java.util.function.Consumer

class XMLValueInput(
    private val value: XMLNode,
    override val errorStack: ErrorStack?
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
        errorStack.push("Dynamic list") {
            read<XMLElement>("list")
                .children
                .forEachIndexed { index, value ->
                    errorStack.push("Entry #${index + 1}") {
                        out.accept(XMLValueInput((value as XMLElement).children.single(), errorStack))
                    }
                }
        }
    }

    override fun readStaticList(size: Int, out: Consumer<ValueInput>) {
        errorStack.push("Static list ($size entries)") {
            read<XMLElement>("list")
                .children
                .also { if (it.size != size) error("List must have $size elements, got ${it.size}") }
                .forEachIndexed { index, value ->
                    errorStack.push("Entry #${index + 1}") {
                        out.accept(XMLValueInput((value as XMLElement).children.single(), errorStack))
                    }
                }
        }
    }

    override fun readDynamicMap(out: BiConsumer<String, ValueInput>) {
        errorStack.push("Dynamic map") {
            read<XMLElement>("list")
                .children
                .forEach {
                    val key = (it as XMLElement).name
                    errorStack.push(
                        "Key '$key'") {
                        out.accept(key, XMLValueInput(it.children.single(), errorStack))
                    }
                }
        }
    }

    override fun readStaticMap(keys: List<String>, out: Consumer<ValueInput>) {
        errorStack.push("Static map") {
            val list = read<XMLElement>("list")
                .children
                .filterIsInstance<XMLElement>()
                .toMutableList()

            keys.forEach { key ->
                errorStack.push("Key '$key'") {
                    val index = list.indexOfFirst { it.name == key }

                    if (index == -1) {
                        error("Map does not contain key '$key'")
                    }

                    out.accept(XMLValueInput(list.removeAt(index).children.single(), errorStack))
                }
            }
        }
    }

    override fun <V> readOptional(ifPresent: DataReader<V>): V? {
        return errorStack.push("Optional value (ifPresent $ifPresent)") {
            if (value is XMLText && value.text == "null") {
                read = true
                null
            } else {
                ifPresent.read(this)
            }
        }
    }
}