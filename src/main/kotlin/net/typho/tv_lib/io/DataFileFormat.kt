package net.typho.tv_lib.io

import java.io.InputStream
import java.io.OutputStream

interface DataFileFormat {
    val extension: String
    val serializers: MutableList<DataObjectSerializer<Any>>

    fun read(input: String): Any

    /**
     * **Note**: It is the caller's responsibility to close this stream.
     */
    fun read(input: InputStream) = read(String(input.readBytes(), Charsets.UTF_8))

    fun write(data: Any): String

    /**
     * **Note**: It is the caller's responsibility to close this stream.
     */
    fun write(data: Any, output: OutputStream) = output.write(write(data).toByteArray(Charsets.UTF_8))

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