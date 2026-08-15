package net.typho.data_util

import java.io.DataInput
import java.io.DataOutput

interface StringDataFormat<P> : DataFormat<String, P> {
    override fun read(bytes: Int, input: DataInput): P {
        val array = ByteArray(bytes)
        input.readFully(array)
        return read(String(array, Charsets.UTF_8))
    }

    override fun write(data: P, output: DataOutput) {
        output.write(write(data).toByteArray(Charsets.UTF_8))
    }
}