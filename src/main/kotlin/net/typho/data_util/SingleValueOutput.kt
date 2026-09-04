package net.typho.data_util

import java.io.DataOutput
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Function

interface SingleValueOutput {
    fun writeBoolean(v: Boolean)

    fun writeByte(v: Byte)

    fun writeShort(v: Short)

    fun writeInt(v: Int)

    fun writeVarInt(v: Int) = writeInt(v)

    fun writeLong(v: Long)

    fun writeFloat(v: Float)

    fun writeDouble(v: Double)

    fun writeString(v: String)

    fun <E : Enum<E>> writeEnum(v: E)

    fun writeList(size: Int): SequentialOutput

    fun writeDynamicMap(entries: List<Pair<String, Consumer<SingleValueOutput>>>)

    fun writeStaticMap(keys: List<String>): SequentialOutput

    fun writeVersion(key: String): SingleValueOutput

    fun <T> writeOptional(v: T?, ifPresent: DataWriter<T>)

    companion object {
        @JvmStatic
        fun toConsumer(out: Consumer<Any?>): SingleValueOutput = object : SingleValueOutput {
            var used = false
            var map: MutableMap<String, Any?>? = null

            fun use() {
                if (used) {
                    throw DataReadException("SingleValueOutput was already written")
                }

                used = true
            }

            fun <T> write(value: T) {
                use()
                out.accept(value)
            }

            override fun writeBoolean(v: Boolean) {
                write(v)
            }

            override fun writeByte(v: Byte) {
                write(v)
            }

            override fun writeShort(v: Short) {
                write(v)
            }

            override fun writeInt(v: Int) {
                write(v)
            }

            override fun writeLong(v: Long) {
                write(v)
            }

            override fun writeFloat(v: Float) {
                write(v)
            }

            override fun writeDouble(v: Double) {
                write(v)
            }

            override fun writeString(v: String) {
                write(v)
            }

            override fun <E : Enum<E>> writeEnum(v: E) {
                writeString(v.name)
            }

            override fun writeList(size: Int): SequentialOutput {
                val list = mutableListOf<Any?>()
                write(list)
                return SequentialOutput.toList(size, list)
            }

            override fun writeDynamicMap(entries: List<Pair<String, Consumer<SingleValueOutput>>>) {
                val map = map ?: mutableMapOf<String, Any?>().also {
                    write(it)
                    map = it
                }

                entries.forEach { (key, value) -> value.accept(toConsumer { map[key] = it }) }
            }

            override fun writeStaticMap(keys: List<String>): SequentialOutput {
                return SequentialOutput.toMap(keys, map ?: mutableMapOf<String, Any?>().also {
                    write(it)
                    map = it
                })
            }

            override fun writeVersion(key: String): SingleValueOutput {
                val map = map ?: mutableMapOf<String, Any?>().also {
                    write(it)
                    map = it
                }

                if (map.containsKey(key)) {
                    throw DataWriteException("Already wrote version key '$key'")
                }

                return toConsumer { map[key] = it }
            }

            override fun <T> writeOptional(v: T?, ifPresent: DataWriter<T>) {
                try {
                    use()

                    if (v == null) {
                        out.accept(null)
                    } else {
                        ifPresent.write(toConsumer(out), v)
                    }
                } catch (e: RuntimeException) {
                    throw DataWriteException("Error while writing optional value $v", e, true, false)
                }
            }
        }

        @JvmStatic
        fun DataOutput.writeVarInt(v: Int) {
            var v = v

            while ((v and -128) != 0) {
                writeByte(v and 127 or 128)
            }

            v = v ushr 7
            writeByte(v)
        }

        @JvmStatic
        fun toData(output: DataOutput): SingleValueOutput = object : SingleValueOutput {
            var used = false
            val version by lazy { toData(output) }

            fun <T> write(value: T, write: BiConsumer<DataOutput, T>) {
                if (used) {
                    throw DataReadException("SingleValueOutput was already written")
                }

                used = true

                write.accept(output, value)
            }

            fun <T> write(write: Function<DataOutput, T>): T {
                if (used) {
                    throw DataReadException("SingleValueOutput was already written")
                }

                used = true

                return write.apply(output)
            }

            override fun writeBoolean(v: Boolean) {
                write(v, DataOutput::writeBoolean)
            }

            override fun writeByte(v: Byte) {
                write(v.toInt(), DataOutput::writeByte)
            }

            override fun writeShort(v: Short) {
                write(v.toInt(), DataOutput::writeShort)
            }

            override fun writeInt(v: Int) {
                write(v, DataOutput::writeInt)
            }

            override fun writeVarInt(v: Int) {
                write { it.writeVarInt(v) }
            }

            override fun writeLong(v: Long) {
                write(v, DataOutput::writeLong)
            }

            override fun writeFloat(v: Float) {
                write(v, DataOutput::writeFloat)
            }

            override fun writeDouble(v: Double) {
                write(v, DataOutput::writeDouble)
            }

            override fun writeString(v: String) {
                write(v, DataOutput::writeUTF)
            }

            override fun <E : Enum<E>> writeEnum(v: E) {
                writeInt(v.ordinal)
            }

            override fun writeList(size: Int): SequentialOutput {
                return write { SequentialOutput.toData(size, it) }
            }

            override fun writeDynamicMap(entries: List<Pair<String, Consumer<SingleValueOutput>>>) {
                write {
                    it.writeVarInt(entries.size)

                    entries.forEach { (key, value) ->
                        it.writeUTF(key)
                        value.accept(toData(it))
                    }
                }
            }

            override fun writeStaticMap(keys: List<String>): SequentialOutput {
                return write { SequentialOutput.toData(keys.size, it, false) }
            }

            override fun writeVersion(key: String) = version

            override fun <T> writeOptional(v: T?, ifPresent: DataWriter<T>) {
                try {
                    write {
                        if (v == null) {
                            it.writeBoolean(false)
                        } else {
                            it.writeBoolean(true)
                            ifPresent.write(toData(output), v)
                        }
                    }
                } catch (e: RuntimeException) {
                    throw DataWriteException("Error while writing optional value $v", e, true, false)
                }
            }
        }
    }
}