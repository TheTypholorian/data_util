package net.typho.tv_lib.io

import java.io.InputStream
import java.io.OutputStream

interface StringDataFileFormat<P> : DataFileFormat<String, P> {
    override fun read(input: InputStream): P {
        return read(String(input.readBytes(), Charsets.UTF_8))
    }

    override fun write(data: P, output: OutputStream) {
        output.write(write(data).toByteArray(Charsets.UTF_8))
    }
}