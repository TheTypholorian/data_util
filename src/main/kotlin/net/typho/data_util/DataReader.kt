package net.typho.data_util

import net.typho.data_util.codec.ValueInput
import java.util.function.Function

fun interface DataReader<T> {
    fun read(input: ValueInput): T

    fun <N> mapRead(
        read: Function<T, N>
    ): DataReader<N> {
        val parent = this
        return object : DataReader<N> {
            override fun read(input: ValueInput): N {
                return read.apply(parent.read(input))
            }

            override fun toString(): String {
                return "$parent mapped with read $read"
            }
        }
    }

    fun <N> mapRead(
        cls: Class<N>,
        read: Function<T, N>
    ): DataReader<N> {
        val parent = this
        return object : DataReader<N> {
            override fun read(input: ValueInput): N {
                return read.apply(parent.read(input))
            }

            override fun toString(): String {
                return "$parent mapped to ${cls.name}"
            }
        }
    }
}