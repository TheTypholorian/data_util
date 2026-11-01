package net.typho.data_util.properties

import net.typho.data_util.DataReader
import net.typho.data_util.codec.ValueInput
import net.typho.data_util.codec.ValueType
import net.typho.data_util.error.ErrorStack
import net.typho.data_util.error.push
import java.util.function.BiConsumer
import java.util.function.Consumer

class PropertiesValueInput(
    @JvmField
    val path: String,
    @JvmField
    val map: Map<String, String>,
    override val errorStack: ErrorStack?
) : ValueInput {
    override val type: ValueType?
        get() = null

    private fun read(expected: String): String {
        return map[path] ?: error("Expected $expected, got unknown (could be a map, list, or a missing value)")
    }

    private fun path(path: String) = if (this.path.isEmpty()) path else "${this.path}.$path"

    override fun readBoolean() = errorStack.push("Boolean") { read("Boolean").toBoolean() }

    override fun readByte() = errorStack.push("Byte") { read("Byte").toByte() }

    override fun readShort() = errorStack.push("Short") { read("Short").toShort() }

    override fun readInt() = errorStack.push("Int") { read("Int").toInt() }

    override fun readLong() = errorStack.push("Long") { read("Long").toLong() }

    override fun readFloat() = errorStack.push("Float") { read("Float").toFloat() }

    override fun readDouble() = errorStack.push("Double") { read("Double").toDouble() }

    override fun readString() = errorStack.push("String") { read("String") }

    override fun readDynamicList(out: Consumer<ValueInput>) {
        errorStack.push("Dynamic list") {
            read("list")
                .split(',')
                .forEach { out.accept(PropertiesElementInput(it, errorStack)) }
        }
    }

    override fun readStaticList(
        size: Int,
        out: Consumer<ValueInput>
    ) {
        errorStack.push("Static list ($size entries)") {
            read("list")
                .split(',')
                .also { if (it.size != size) error("List must have $size elements, got ${it.size}") }
                .forEach { out.accept(PropertiesElementInput(it, errorStack)) }
        }
    }

    override fun readDynamicMap(out: BiConsumer<String, ValueInput>) {
        errorStack.push("Dynamic map") {
            read("list")
                .split(',')
                .associate {
                    val index = it.indexOf('=')

                    if (index == -1 || index != it.lastIndexOf('=')) {
                        error("Entry '${it}' must contain exactly one '='")
                    }

                    it.substring(0, index) to it.substring(index + 1)
                }
                .forEach { (key, value) -> out.accept(key, PropertiesElementInput(value, errorStack)) }
        }
    }

    override fun readStaticMap(
        keys: List<String>,
        out: Consumer<ValueInput>
    ) {
        errorStack.push("Static map") {
            keys.forEach { key ->
                errorStack.push("Key '$key'") {
                    out.accept(PropertiesValueInput(path(key), map, errorStack))
                }
            }
        }
    }

    override fun <V> readOptional(ifPresent: DataReader<V>): V? {
        return errorStack.push("Optional value") {
            if (map[path] == null) {
                null
            } else {
                ifPresent.read(this)
            }
        }
    }
}