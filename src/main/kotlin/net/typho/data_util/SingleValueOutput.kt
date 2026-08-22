package net.typho.data_util

import java.io.DataOutput
import java.util.Optional
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Function

interface SingleValueOutput {
    fun writeBoolean(v: Boolean)

    fun writeByte(v: Byte)

    fun writeShort(v: Short)

    fun writeInt(v: Int)

    fun writeLong(v: Long)

    fun writeFloat(v: Float)

    fun writeDouble(v: Double)

    fun writeString(v: String)

    fun writeList(size: Int): ListOutput

    fun writeMap(keys: List<String>): MapOutput

    fun <T : Any> writeOptional(optional: Optional<T>, ifPresent: BiConsumer<SingleValueOutput, T>)

    companion object {
        @JvmStatic
        fun toConsumer(out: Consumer<Any?>): SingleValueOutput = object : SingleValueOutput {
            var used = false

            fun <T> write(value: T) {
                if (used) {
                    throw DataReadException("SingleValueOutput was already written")
                }

                used = true
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

            override fun writeList(size: Int): ListOutput {
                val list = mutableListOf<Any?>()
                write(list)
                return ListOutput.toList(list)
            }

            override fun writeMap(keys: List<String>): MapOutput {
                val map = mutableMapOf<String, Any?>()
                write(map)
                return MapOutput.toMap(keys, map)
            }

            override fun <T : Any> writeOptional(optional: Optional<T>, ifPresent: BiConsumer<SingleValueOutput, T>) {
                if (optional.isPresent) {
                    ifPresent.accept(this, optional.get())
                } else {
                    write(null)
                }
            }
        }

        @JvmStatic
        fun toData(output: DataOutput): SingleValueOutput = object : SingleValueOutput {
            var used = false

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

            override fun writeList(size: Int): ListOutput {
                return write { ListOutput.toData(size, it) }
            }

            override fun writeMap(keys: List<String>): MapOutput {
                return write { MapOutput.toData(keys, it) }
            }

            override fun <T : Any> writeOptional(optional: Optional<T>, ifPresent: BiConsumer<SingleValueOutput, T>) {
                write {
                    if (optional.isPresent) {
                        it.writeBoolean(true)
                        ifPresent.accept(this, optional.get())
                    } else {
                        it.writeBoolean(false)
                    }
                }
            }
        }
    }
}