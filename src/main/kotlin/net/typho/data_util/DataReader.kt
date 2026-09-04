package net.typho.data_util

import java.util.function.Function

fun interface DataReader<T> {
    fun read(input: SingleValueInput): T

    fun <N> mapRead(
        read: Function<T, N>
    ): DataReader<N> {
        val parent = this
        return object : DataReader<N> {
            override fun read(input: SingleValueInput): N {
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
            override fun read(input: SingleValueInput): N {
                return read.apply(parent.read(input))
            }

            override fun toString(): String {
                return "$parent mapped to ${cls.name}"
            }
        }
    }
}