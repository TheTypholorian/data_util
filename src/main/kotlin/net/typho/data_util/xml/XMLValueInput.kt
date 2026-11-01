package net.typho.data_util.xml

import net.typho.data_util.DataReader
import net.typho.data_util.codec.ValueInput
import net.typho.data_util.codec.ValueType
import net.typho.data_util.error.ErrorStack
import net.typho.data_util.error.push
import java.util.function.BiConsumer
import java.util.function.Consumer

class XMLValueInput(
    private val value: XMLElement?,
    override val errorStack: ErrorStack?
): ValueInput {
    private var read = false

    override val type: ValueType?
        get() = null

    private fun castError(expected: String): Nothing {
        error("Expected $expected, got ${value?.javaClass?.name}")
    }

    private fun peek(expected: String): XMLElement {
        if (read) {
            error("Value input has already been read")
        }

        return value ?: castError(expected)
    }

    private fun read(expected: String): XMLElement {
        val value = peek(expected)
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

    override fun readString() = (read("text").children.singleOrNull() as? XMLText ?: error("Children must be a single text element, got ${value!!.children}")).text

    override fun readDynamicList(out: Consumer<ValueInput>) {
        errorStack.push("Dynamic list") {
            read("list")
                .children
                .forEachIndexed { index, value ->
                    errorStack.push("Entry #${index + 1}") {
                        out.accept(XMLValueInput(value as? XMLElement ?: error("Entry must be an xml element, got $value"), errorStack))
                    }
                }
        }
    }

    override fun readStaticList(size: Int, out: Consumer<ValueInput>) {
        errorStack.push("Static list ($size entries)") {
            read("list")
                .children
                .also { if (it.size != size) error("List must have $size elements, got ${it.size}") }
                .forEachIndexed { index, value ->
                    errorStack.push("Entry #${index + 1}") {
                        out.accept(XMLValueInput(value as? XMLElement ?: error("Entry must be an xml element, got $value"), errorStack))
                    }
                }
        }
    }

    override fun readDynamicMap(out: BiConsumer<String, ValueInput>) {
        errorStack.push("Dynamic map") {
            read("map")
                .children
                .forEach {
                    val key = (it as? XMLElement ?: error("Entry must be an xml element, got $value")).name
                    errorStack.push("Key '$key'") {
                        out.accept(key, XMLValueInput(it, errorStack))
                    }
                }
        }
    }

    override fun readStaticMap(keys: List<String>, out: Consumer<ValueInput>) {
        errorStack.push("Static map") {
            val map = peek("map")
                .children
                .filterIsInstance<XMLElement>()
                .toMutableList()

            keys.forEach { key ->
                errorStack.push("Key '$key'") {
                    out.accept(XMLValueInput(map.firstOrNull { it.name == key }, errorStack))
                }
            }
        }
    }

    override fun <V> readOptional(ifPresent: DataReader<V>): V? {
        return errorStack.push("Optional value") {
            if (value == null) {
                read = true
                null
            } else {
                ifPresent.read(this)
            }
        }
    }
}