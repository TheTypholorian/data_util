package net.typho.tv_lib.io

import java.io.InputStream
import java.io.OutputStream

interface DataFileFormat<D, P> {
    val extension: String
    val serializers: MutableList<DataObjectSerializer<Any>>

    fun read(input: D): P

    /**
     * **Note**: It is the caller's responsibility to close this stream.
     */
    fun read(input: InputStream): P

    fun write(data: P): D

    /**
     * **Note**: It is the caller's responsibility to close this stream.
     */
    fun write(data: P, output: OutputStream)

    companion object {
        @JvmStatic
        fun applyEscapeSequences(text: String): String {
            return text.replace("\\b", "\b")
                .replace("\\t", "\t")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }

        @JvmStatic
        fun trimQuotes(text: String): String {
            return if (text.startsWith('"') && text.endsWith('"')) {
                text.substring(1, text.length - 1)
            } else if (text.startsWith('\'') && text.endsWith('\'')) {
                text.substring(1, text.length - 1)
            } else {
                text
            }
        }
    }
}