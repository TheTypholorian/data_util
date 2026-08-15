package net.typho.data_util

import java.io.DataInput
import java.io.DataOutput

interface DataSerializer<D, R> {
    fun read(input: D): R

    /**
     * **Note**: It is the caller's responsibility to close this stream.
     */
    fun read(bytes: Int, input: DataInput): R

    fun write(data: R): D

    /**
     * **Note**: It is the caller's responsibility to close this stream.
     */
    fun write(data: R, output: DataOutput)
}