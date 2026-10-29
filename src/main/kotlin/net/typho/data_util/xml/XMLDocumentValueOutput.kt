package net.typho.data_util.xml

import net.typho.data_util.DataWriter
import net.typho.data_util.codec.ValueOutput
import java.util.function.Consumer

class XMLDocumentValueOutput(
    @JvmField
    var metadata: MutableList<XMLStubElement> = mutableListOf()
) : ValueOutput.WithResult<XMLDocument> {
    private val delegate = XMLValueOutput<XMLNode>()
    override val areTypesWritten: Boolean
        get() = delegate.areTypesWritten

    override fun finish() = XMLDocument(delegate.finish().let { it as? XMLElement ?: XMLElement("root", children = mutableListOf(it)) }, metadata)

    override fun writeBoolean(v: Boolean) = delegate.writeBoolean(v)

    override fun writeByte(v: Byte) = delegate.writeByte(v)

    override fun writeShort(v: Short) = delegate.writeShort(v)

    override fun writeInt(v: Int) = delegate.writeInt(v)

    override fun writeLong(v: Long) = delegate.writeLong(v)

    override fun writeFloat(v: Float) = delegate.writeFloat(v)

    override fun writeDouble(v: Double) = delegate.writeDouble(v)

    override fun writeString(v: String) = delegate.writeString(v)

    override fun writeDynamicList(entries: Collection<Consumer<ValueOutput>>) = delegate.writeDynamicList(entries)

    override fun writeStaticList(entries: Iterator<Consumer<ValueOutput>>) = delegate.writeStaticList(entries)

    override fun writeDynamicMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>) = delegate.writeDynamicMap(entries)

    override fun writeStaticMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>) = delegate.writeStaticMap(entries)

    override fun <V> writeOptional(value: V?, ifPresent: DataWriter<V>) = delegate.writeOptional(value, ifPresent)
}