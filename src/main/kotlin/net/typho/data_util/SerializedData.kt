package net.typho.data_util

import java.nio.file.Path
import kotlin.io.path.*

abstract class SerializedData private constructor() {
    abstract fun asString(): String

    abstract fun asBytes(): ByteArray

    abstract fun save(path: Path)

    companion object {
        @JvmStatic
        @JvmName("of")
        operator fun invoke(text: String) = object : SerializedData() {
            override fun asString() = text

            override fun asBytes() = text.toByteArray()

            override fun save(path: Path) = path.writeText(text)

            override fun toString() = "SerializedData(text='$text')"
        }

        @JvmStatic
        @JvmName("of")
        operator fun invoke(bytes: ByteArray) = object : SerializedData() {
            override fun asString() = String(bytes)

            override fun asBytes() = bytes

            override fun save(path: Path) = path.writeBytes(bytes)

            override fun toString() = "SerializedData(bytes=${bytes.contentToString()})"
        }

        @JvmStatic
        @JvmName("of")
        operator fun invoke(file: Path) = object : SerializedData() {
            override fun asString() = file.readText()

            override fun asBytes() = file.readBytes()

            override fun save(path: Path) {
                file.inputStream().use { input ->
                    path.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            override fun toString() = "SerializedData(path='$file')"
        }
    }
}