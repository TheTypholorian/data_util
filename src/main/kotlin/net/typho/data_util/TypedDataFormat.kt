package net.typho.data_util

import net.typho.data_util.codec.Codec
import java.util.function.Consumer

interface TypedDataFormat<D, R> : DataFormat<D, R> {
    fun <T> read(codec: Codec<T>, input: D): T {
        return codec.read(createInput(read(input)))
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> write(codec: Codec<T>, value: T): D {
        var result: R? = null
        codec.write(createOutput { result = it }, value)
        return write(result as R)
    }

    fun createInput(parsed: R): SingleValueInput

    fun createOutput(out: Consumer<R>): SingleValueOutput
}