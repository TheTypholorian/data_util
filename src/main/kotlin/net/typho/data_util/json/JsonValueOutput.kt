package net.typho.data_util.json

import net.typho.data_util.DataWriteException
import net.typho.data_util.DataWriter
import net.typho.data_util.codec.ValueOutput
import java.util.function.Consumer

@Suppress("UNCHECKED_CAST")
class JsonValueOutput<V> : ValueOutput.WithResult<V> {
    override val areTypesWritten: Boolean
        get() = true
    private var written = false
    private var value: V? = null

    override fun finish(): V {
        if (!written) {
            throw DataWriteException("Value has not been written yet")
        }

        return value as V
    }

    private fun write(value: Any?) {
        if (written) {
            throw DataWriteException("Value has already been written")
        }

        this.value = value as V
        written = true
    }

    override fun writeBoolean(v: Boolean) = write(v)

    override fun writeByte(v: Byte) = write(v)

    override fun writeShort(v: Short) = write(v)

    override fun writeInt(v: Int) = write(v)

    override fun writeLong(v: Long) = write(v)

    override fun writeFloat(v: Float) = write(v)

    override fun writeDouble(v: Double) = write(v)

    override fun writeString(v: String) = write(v)

    override fun writeDynamicList(entries: Collection<Consumer<ValueOutput>>) = writeStaticList(entries.iterator())

    override fun writeStaticList(entries: Iterator<Consumer<ValueOutput>>) {
        val list = mutableListOf<Any?>()
        write(list)

        entries.forEach { value ->
            val output = JsonValueOutput<Any?>()
            value.accept(output)
            list.add(output.finish())
        }
    }

    override fun writeDynamicMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>) {
        val map = mutableMapOf<String, Any?>()
        write(map)

        entries.forEach { (key, value) ->
            if (map.containsKey(key)) {
                throw DataWriteException("Map has already been written key '$key'")
            }

            val output = JsonValueOutput<Any?>()
            value.accept(output)
            map[key] = output.finish()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun writeStaticMap(entries: Iterator<Pair<String, Consumer<ValueOutput>>>) {
        val map = value as? MutableMap<String, Any?> ?: mutableMapOf<String, Any?>().also { write(it) }

        entries.forEach { (key, value) ->
            if (map.containsKey(key)) {
                throw DataWriteException("Map has already been written key '$key'")
            }

            val output = JsonValueOutput<Any?>()
            value.accept(output)
            map[key] = output.finish()
        }
    }

    override fun <V> writeOptional(value: V?, ifPresent: DataWriter<V>) {
        if (value == null) {
            write(null)
        } else {
            ifPresent.write(this, value)
        }
    }
}