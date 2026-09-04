package net.typho.data_util

import java.util.function.Function

fun interface DataWriter<T> {
    fun write(output: SingleValueOutput, value: T)

    fun <N> mapWrite(
        write: Function<N, T>
    ): DataWriter<N> {
        val parent = this
        return object : DataWriter<N> {
            override fun write(output: SingleValueOutput, value: N) {
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
            override fun write(output: SingleValueOutput, value: N) {
                parent.write(output, write.apply(value))
            }

            override fun toString(): String {
                return "$parent mapped to ${cls.name}"
            }
        }
    }
}