package net.typho.data_util

import java.io.DataOutput
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

interface SingleValueOutput {
    fun writeBoolean(v: Boolean)

    fun writeByte(v: Byte)

    fun writeShort(v: Short)

    fun writeInt(v: Int)

    fun writeLong(v: Long)

    fun writeFloat(v: Float)

    fun writeDouble(v: Double)

    fun writeString(v: String)

    fun writeList(size: Int): SequentialOutput

    fun writeStaticMap(keys: List<String>): SequentialOutput

    fun <T> writeOptional(v: T?, ifPresent: BiConsumer<SingleValueOutput, T>)

    companion object {
        @JvmStatic
        fun toConsumer(out: Consumer<Any?>): SingleValueOutput = object : SingleValueOutput {
            var used = false

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

            override fun writeList(size: Int): SequentialOutput {
                val list = mutableListOf<Any?>()
                write(list)
                return SequentialOutput.toList(size, list)
            }

            override fun writeStaticMap(keys: List<String>): SequentialOutput {
                val map = mutableMapOf<String, Any?>()
                write(map)
                return SequentialOutput.toMap(keys, map)
            }

            override fun <T> writeOptional(v: T?, ifPresent: BiConsumer<SingleValueOutput, T>) {
                use()

                if (v == null) {
                    out.accept(null)
                } else {
                    ifPresent.accept(toConsumer(out), v)
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

            override fun writeList(size: Int): SequentialOutput {
                return write { SequentialOutput.toData(size, it) }
            }

            override fun writeStaticMap(keys: List<String>): SequentialOutput {
                return write { SequentialOutput.toData(keys.size, it, false) }
            }

            override fun <T> writeOptional(v: T?, ifPresent: BiConsumer<SingleValueOutput, T>) {
                write {
                    if (v == null) {
                        it.writeBoolean(false)
                    } else {
                        it.writeBoolean(true)
                        ifPresent.accept(toData(output), v)
                    }
                }
            }
        }
    }
}