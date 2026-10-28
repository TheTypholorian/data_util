package net.typho.data_util

import net.typho.data_util.codec.ValueOutput
import java.util.function.Function

fun interface DataWriter<T> {
    fun write(output: ValueOutput, value: T)

    fun <R> write(output: ValueOutput.WithResult<R>, value: T): R {
        write(output as ValueOutput, value)
        return output.finish()
    }

    fun <N> mapWrite(
        write: Function<N, T>
    ): DataWriter<N> {
        val parent = this
        return object : DataWriter<N> {
            override fun write(output: ValueOutput, value: N) {
                parent.write(output, write.apply(value))
            }

            override fun toString(): String {
                return "$parent mapped with write $write"
            }
        }
    }

    fun <N> mapWrite(
        cls: Class<N>,
        write: Function<N, T>
    ): DataWriter<N> {
        val parent = this
        return object : DataWriter<N> {
            override fun write(output: ValueOutput, value: N) {
                parent.write(output, write.apply(value))
            }

            override fun toString(): String {
                return "$parent mapped to ${cls.name}"
            }
        }
    }
}