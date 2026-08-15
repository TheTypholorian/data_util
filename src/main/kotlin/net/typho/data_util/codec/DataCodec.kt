package net.typho.data_util.codec

import java.util.function.BiConsumer
import java.util.function.Function

interface DataCodec<T : Any> {
    companion object {
        private fun <T : Any> simple(read: Function<SingleValueInput, T>, write: BiConsumer<SingleValueOutput, T>) = object : DataCodec<T> {
            override fun read(input: SingleValueInput): T {
                return read.apply(input)
            }

            override fun write(value: T, output: SingleValueOutput) {
                write.accept(output, value)
            }
        }

        @JvmField
        val BOOL = simple(SingleValueInput::readBoolean, SingleValueOutput::writeBoolean)
        @JvmField
        val BYTE = simple(SingleValueInput::readByte, SingleValueOutput::writeByte)
        @JvmField
        val SHORT = simple(SingleValueInput::readShort, SingleValueOutput::writeShort)
        @JvmField
        val INT = simple(SingleValueInput::readInt, SingleValueOutput::writeInt)
        @JvmField
        val LONG = simple(SingleValueInput::readLong, SingleValueOutput::writeLong)
        @JvmField
        val FLOAT = simple(SingleValueInput::readFloat, SingleValueOutput::writeFloat)
        @JvmField
        val DOUBLE = simple(SingleValueInput::readDouble, SingleValueOutput::writeDouble)
        @JvmField
        val STRING = simple(SingleValueInput::readString, SingleValueOutput::writeString)
    }

    fun read(input: SingleValueInput): T

    fun write(value: T, output: SingleValueOutput)
}