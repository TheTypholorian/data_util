package net.typho.data_util.xml

import net.typho.data_util.DataWriteException
import net.typho.data_util.DataWriter
import net.typho.data_util.codec.ValueOutput
import java.util.function.Consumer

@Suppress("UNCHECKED_CAST")
class XMLValueOutput<V : XMLNode> : ValueOutput.WithResult<V> {
    override val areTypesWritten: Boolean
        get() = false
    private var written = false
    private var value: V? = null

    override fun finish(): V {
        if (!written) {
            throw DataWriteException("Value has not been written yet")
        }

        return value as V
    }

    private fun write(value: XMLNode) {
        if (written) {
            throw DataWriteException("Value has already been written")
        }

        this.value = value as V
        written = true
    }

    override fun writeBoolean(v: Boolean) = writeString(v.toString())

    override fun writeByte(v: Byte) = writeString(v.toString())

    override fun writeShort(v: Short) = writeString(v.toString())

    override fun writeInt(v: Int) = writeString(v.toString())

    override fun writeLong(v: Long) = writeString(v.toString())

    override fun writeFloat(v: Float) = writeString(v.toString())

    override fun writeDouble(v: Double) = writeString(v.toString())

    override fun writeString(v: String) = write(XMLText(v))

    override fun writeDynamicList(entries: Collection<Consumer<ValueOutput>>) = writeStaticList(entries.iterator())

    override fun writeStaticList(entries: Iterator<Consumer<ValueOutput>>) {
        val list = XMLElement("list")
        write(list)

        entries.forEach { value ->
            val output = XMLValueOutput<XMLNode>()
            value.accept(output)
            val result = output.finish()
            list.children.add(result as? XMLElement ?: XMLElement("element", children = mutableListOf(result)))
        }
    }

    override fun writeDynamicMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>) {
        val map = XMLElement("map")
        write(map)

        entries.forEach { (key, value) ->
            val output = XMLValueOutput<XMLNode>()
            value.accept(output)
            val result = output.finish()
            map.children.add(XMLElement(key, children = mutableListOf(result)))
        }
    }

    override fun writeStaticMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>) {
        val map = value as? XMLElement ?: XMLElement("map").also { write(it) }

        entries.forEach { (key, value) ->
            val output = XMLValueOutput<XMLNode>()
            value.accept(output)
            val result = output.finish()
            map.children.add(XMLElement(key, children = mutableListOf(result)))
        }
    }

    override fun <V> writeOptional(value: V?, ifPresent: DataWriter<V>) {
        if (value == null) {
            writeString("null")
        } else {
            ifPresent.write(this, value)
        }
    }
}