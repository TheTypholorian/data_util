package net.typho.data_util

import java.io.DataInput
import java.util.function.Function
import kotlin.jvm.java

interface SingleValueInput {
    fun readBoolean(): Boolean

    fun readByte(): Byte

    fun readShort(): Short

    fun readInt(): Int

    fun readVarInt(): Int = readInt()

    fun readLong(): Long

    fun readFloat(): Float

    fun readDouble(): Double

    fun readString(): String

    fun <E : Enum<E>> readEnum(cls: Class<E>, caseSensitive: Boolean): E

    fun readList(): SequentialInput

    fun readDynamicMap(): Iterator<Pair<String, SingleValueInput>>

    fun readStaticMap(keys: List<String>): SequentialInput

    fun readVersion(key: String): SingleValueInput?

    fun <T> readEither(options: List<DataReader<T>>): T

    fun <T> readOptional(ifPresent: DataReader<T>): T?

    companion object {
        private object AlreadyReadValue

        @JvmStatic
        fun fromObject(value: Any?): SingleValueInput {
            var value = value

            return object : SingleValueInput {
                var used = false

                fun use() {
                    if (used) {
                        throw DataReadException("SingleValueInput was already read")
                    }

                    used = true
                }

                inline fun <reified T> castAndUse(): T {
                    use()
                    return cast()
                }

                inline fun <reified T> cast(): T {
                    return value as? T ?: throw DataReadException("Expected ${T::class.java.name}, got $value")
                }

                override fun readBoolean(): Boolean = castAndUse()

                override fun readByte(): Byte = castAndUse()

                override fun readShort(): Short = castAndUse()

                override fun readInt(): Int = castAndUse()

                override fun readLong(): Long = castAndUse()

                override fun readFloat(): Float = castAndUse()

                override fun readDouble(): Double = castAndUse()

                override fun readString(): String = castAndUse()

                override fun <E : Enum<E>> readEnum(cls: Class<E>, caseSensitive: Boolean): E {
                    val name = readString()
                    return cls.enumConstants.firstOrNull { it.name.equals(name, !caseSensitive) } ?: throw EnumConstantNotPresentException(cls, name)
                }

                override fun readList() = SequentialInput.fromList(castAndUse())

                override fun readDynamicMap(): Iterator<Pair<String, SingleValueInput>> {
                    return castAndUse<Map<String, Any?>>().map { (key, value) -> key to fromObject(value) }.iterator()
                }

                override fun readStaticMap(keys: List<String>) = SequentialInput.fromMap(keys, castAndUse())

                override fun readVersion(key: String): SingleValueInput? {
                    val map = cast<Map<String, Any?>>()
                    val r = if (map.containsKey(key)) {
                        val version = map[key]

                        if (version == AlreadyReadValue) {
                            throw DataReadException("Already read version key '$key'")
                        }

                        val input = fromObject(version)
                        input
                    } else {
                        null
                    }

                    @Suppress("AssignedValueIsNeverRead")
                    value = map.toMutableMap().also { it[key] = AlreadyReadValue }

                    return r
                }

                override fun <T> readEither(options: List<DataReader<T>>): T {
                    use()

                    if (options.isEmpty()) {
                        throw DataReadException("Options list for readEither is empty")
                    }

                    val errors = mutableListOf<Throwable>()

                    for (read in options) {
                        try {
                            used = false
                            return read.read(fromObject(value))
                        } catch (t: Throwable) {
                            errors.add(t)
                        }
                    }

                    throw DataReadException("No options worked for input value $value: ${errors.joinToString { it.message ?: "" }}")
                }

                override fun <T> readOptional(ifPresent: DataReader<T>): T? {
                    try {
                        use()
                        return if (value == null) null else ifPresent.read(fromObject(value))
                    } catch (e: RuntimeException) {
                        throw DataReadException("Error while reading optional value", e, true, false)
                    }
                }
            }
        }

        @JvmStatic
        fun fromString(value: String?): SingleValueInput = object : SingleValueInput {
            var used = false

            fun use() {
                if (used) {
                    throw DataReadException("SingleValueInput was already read")
                }

                used = true
            }

            inline fun <reified T> cast(converter: Function<String, T?>): T {
                use()

                value?.let { converter.apply(it)?.let { return it } }

                throw DataReadException("Expected ${T::class.java.name}, got '$value'")
            }

            override fun readBoolean(): Boolean = cast(String::toBooleanStrictOrNull)

            override fun readByte(): Byte = cast(String::toByteOrNull)

            override fun readShort(): Short = cast(String::toShortOrNull)

            override fun readInt(): Int = cast(String::toIntOrNull)

            override fun readLong(): Long = cast(String::toLongOrNull)

            override fun readFloat(): Float = cast(String::toFloatOrNull)

            override fun readDouble(): Double = cast(String::toDoubleOrNull)

            override fun readString(): String = cast { it }

            override fun <E : Enum<E>> readEnum(cls: Class<E>, caseSensitive: Boolean): E {
                val name = readString()
                return cls.enumConstants.firstOrNull { it.name.equals(name, !caseSensitive) } ?: throw EnumConstantNotPresentException(cls, name)
            }

            override fun readList(): SequentialInput = throw DataReadException("List values are unsupported when reading from a string")

            override fun readDynamicMap(): Iterator<Pair<String, SingleValueInput>> = throw DataReadException("Map values are unsupported when reading from a string")

            override fun readStaticMap(keys: List<String>): SequentialInput = throw DataReadException("Map values are unsupported when reading from a string")

            override fun readVersion(key: String): SingleValueInput? {
                throw DataReadException("Version values are unsupported when reading from a string")
            }

            override fun <T> readEither(options: List<DataReader<T>>): T {
                use()

                if (options.isEmpty()) {
                    throw DataReadException("Options list for readEither is empty")
                }

                val errors = mutableListOf<Throwable>()

                for (read in options) {
                    try {
                        used = false
                        return read.read(fromObject(value))
                    } catch (t: Throwable) {
                        errors.add(t)
                    }
                }

                throw DataReadException("No options worked for input value $value: ${errors.joinToString { it.message ?: "" }}")
            }

            override fun <T> readOptional(ifPresent: DataReader<T>): T? {
                try {
                    use()
                    return if (value == null) null else ifPresent.read(fromString(value))
                } catch (e: RuntimeException) {
                    throw DataReadException("Error while reading optional value", e, true, false)
                }
            }
        }

        @JvmStatic
        fun DataInput.readVarInt(): Int {
            var out = 0
            var size = 0

            var next: Int

            do {
                next = readUnsignedByte()
                out = out or ((next and 127) shl (size++ * 7))

                if (size > 5) {
                    throw DataReadException("VarInt too big")
                }
            } while ((next and 128) == 128)

            return out
        }

        @JvmStatic
        fun fromData(input: DataInput): SingleValueInput = object : SingleValueInput {
            var used = false
            val version by lazy { fromData(input) }

            fun <T> read(read: Function<DataInput, T>): T {
                if (used) {
                    throw DataReadException("SingleValueInput was already read")
                }

                used = true

                return read.apply(input)
            }

            override fun readBoolean(): Boolean = read(DataInput::readBoolean)

            override fun readByte(): Byte = read(DataInput::readByte)

            override fun readShort(): Short = read(DataInput::readShort)

            override fun readInt(): Int = read(DataInput::readInt)

            override fun readVarInt(): Int {
                return read { it.readVarInt() }
            }

            override fun readLong(): Long = read(DataInput::readLong)

            override fun readFloat(): Float = read(DataInput::readFloat)

            override fun readDouble(): Double = read(DataInput::readDouble)

            override fun readString(): String = read(DataInput::readUTF)

            override fun <E : Enum<E>> readEnum(cls: Class<E>, caseSensitive: Boolean): E = cls.enumConstants[readVarInt()]

            override fun readList(): SequentialInput = read(SequentialInput::fromData)

            override fun readDynamicMap(): Iterator<Pair<String, SingleValueInput>> {
                return read {
                    val size = it.readVarInt()
                    object : Iterator<Pair<String, SingleValueInput>> {
                        var index = 0

                        override fun hasNext() = index < size

                        override fun next(): Pair<String, SingleValueInput> {
                            if (!hasNext()) {
                                throw NoSuchElementException()
                            }

                            index++
                            return it.readUTF() to fromData(input)
                        }
                    }
                }
            }

            override fun readStaticMap(keys: List<String>): SequentialInput = read { SequentialInput.fromData(it, keys.size) }

            override fun readVersion(key: String) = version

            override fun <T> readEither(options: List<DataReader<T>>): T {
                if (options.isEmpty()) {
                    throw DataReadException("Options list for readEither is empty")
                }

                return options.first().read(this)
            }

            override fun <T> readOptional(ifPresent: DataReader<T>): T? {
                try {
                    return read {
                        val present = it.readBoolean()
                        if (present) ifPresent.read(fromData(input)) else null
                    }
                } catch (e: RuntimeException) {
                    throw DataReadException("Error while reading optional value", e, true, false)
                }
            }
        }
    }
}