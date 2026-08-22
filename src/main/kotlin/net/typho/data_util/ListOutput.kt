package net.typho.data_util

import java.io.DataInput
import java.util.Optional
import java.util.function.BiConsumer
import java.util.function.Function

interface ListOutput {
    fun writeNextEntry(): SingleValueOutput

    companion object {
        @JvmStatic
        fun toList(list: MutableList<Any?>): ListOutput = object : ListOutput {
            override fun writeNextEntry(): SingleValueOutput {
                return SingleValueOutput.toConsumer(list::add)
            }
        }

        @JvmOverloads
        @JvmStatic
        fun fromData(input: DataInput, size: Int = input.readInt()): ListOutput = object : ListOutput {
            override var left: Int = size

            fun decrement() {
                if (left == 0) {
                    throw DataReadException("Read too many entries from list of size $size")
                }

                left--
            }

            override fun readBoolean(): Boolean {
                decrement()
                return input.readBoolean()
            }

            override fun readByte(): Byte {
                decrement()
                return input.readByte()
            }

            override fun readShort(): Short {
                decrement()
                return input.readShort()
            }

            override fun readInt(): Int {
                decrement()
                return input.readInt()
            }

            override fun readLong(): Long {
                decrement()
                return input.readLong()
            }

            override fun readFloat(): Float {
                decrement()
                return input.readFloat()
            }

            override fun readDouble(): Double {
                decrement()
                return input.readDouble()
            }

            override fun readString(): String {
                decrement()
                return input.readUTF()
            }

            override fun readList(): ListOutput {
                decrement()
                return fromData(input)
            }

            override fun readMap(keys: List<String>): MapInput {
                decrement()
                return MapInput.fromData(keys, input)
            }

            override fun <T : Any> readOptional(ifPresent: Function<SingleValueInput, T>): Optional<T> {
                decrement()
                val present = input.readBoolean()
                return if (present) Optional.of(ifPresent.apply(SingleValueInput.fromData(input))) else Optional.empty()
            }
        }
    }
}