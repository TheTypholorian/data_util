package net.typho.data_util.xml

import net.typho.data_util.DataWriter
import net.typho.data_util.codec.ValueOutput
import net.typho.data_util.error.ErrorStack
import net.typho.data_util.error.push
import java.util.function.Consumer

class XMLValueOutput(
    override val errorStack: ErrorStack?
) : ValueOutput.WithResult<XMLNode> {
    override val areTypesWritten: Boolean
        get() = false
    private var written = false
    private var value: XMLNode? = null

    override fun finish(): XMLNode {
        if (!written) {
            error("Value has not been written yet")
        }

        return value!!
    }

    private fun write(value: XMLNode) {
        if (written) {
            error("Value has already been written")
        }

        this.value = value
        written = true
    }

    override fun writeBoolean(v: Boolean) = errorStack.push("Boolean '$v'") { write(XMLText(v.toString())) }

    override fun writeByte(v: Byte) = errorStack.push("Byte '$v'") { write(XMLText(v.toString())) }

    override fun writeShort(v: Short) = errorStack.push("Short '$v'") { write(XMLText(v.toString())) }

    override fun writeInt(v: Int) = errorStack.push("Int '$v'") { write(XMLText(v.toString())) }

    override fun writeLong(v: Long) = errorStack.push("Long '$v'") { write(XMLText(v.toString())) }

    override fun writeFloat(v: Float) = errorStack.push("Float '$v'") { write(XMLText(v.toString())) }

    override fun writeDouble(v: Double) = errorStack.push("Double '$v'") { write(XMLText(v.toString())) }

    override fun writeString(v: String) = errorStack.push("String '$v'") { write(XMLText(v)) }

    override fun writeDynamicList(entries: Collection<Consumer<ValueOutput>>) {
        errorStack.push("Dynamic list (${entries.size} entries)") {
            val list = XMLElement("list")
            write(list)

            entries.forEachIndexed { index, value ->
                errorStack.push("Entry #${index + 1}") {
                    val output = XMLValueOutput(errorStack)
                    value.accept(output)
                    val result = output.finish()
                    list.children.add(result as? XMLElement ?: XMLElement("element", children = mutableListOf(result)))
                }
            }
        }
    }

    override fun writeStaticList(entries: Iterator<Consumer<ValueOutput>>) {
        errorStack.push("Static list") {
            val list = XMLElement("list")
            write(list)
            var index = 0

            entries.forEach { value ->
                errorStack.push("Entry #${index + 1}") {
                    val output = XMLValueOutput(errorStack)
                    value.accept(output)
                    val result = output.finish()
                    list.children.add(result as? XMLElement ?: XMLElement("element", children = mutableListOf(result)))
                }

                index++
            }
        }
    }

    override fun writeDynamicMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>) {
        errorStack.push("Dynamic map") {
            val map = XMLElement("map")
            write(map)

            entries.forEach { (key, value) ->
                errorStack.push("Key '$key'") {
                    val output = XMLValueOutput(errorStack)
                    value.accept(output)
                    val result = output.finish()
                    map.children.add(XMLElement(key, children = (result as? XMLElement)?.children ?: mutableListOf(result)))
                }
            }
        }
    }

    override fun writeStaticMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>) {
        errorStack.push("Static map") {
            val map = value as? XMLElement ?: XMLElement("map").also { write(it) }

            entries.forEach { (key, value) ->
                errorStack.push("Key '$key'") {
                    val output = XMLValueOutput(errorStack)
                    value.accept(output)
                    val result = output.finish()
                    map.children.add(XMLElement(key, children = (result as? XMLElement)?.children ?: mutableListOf(result)))
                }
            }
        }
    }

    override fun <XMLNode> writeOptional(value: XMLNode?, ifPresent: DataWriter<XMLNode>) {
        errorStack.push("Optional value '$value' (ifPresent $ifPresent)") {
            if (value == null) {
                writeString("null")
            } else {
                ifPresent.write(this, value)
            }
        }
    }
}