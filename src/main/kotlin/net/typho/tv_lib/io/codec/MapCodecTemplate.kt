package net.typho.tv_lib.io.codec

import net.typho.tv_lib.io.DataReadException
import java.util.Optional
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Supplier

interface MapCodecTemplate<T : Any> : CodecTemplate<T> {
    data class Entry<T : Any, V : Any> @JvmOverloads constructor(
        @JvmField
        val key: String,
        @JvmField
        val codec: CodecTemplate<V>,
        @JvmField
        val getter: Function<T, V>,
        @JvmField
        val setter: BiConsumer<T, V>,
        @JvmField
        val fallback: Optional<V> = Optional.empty<V>()
    ) {
        fun read(value: T, input: CodecTemplate.MapInput) {
            val result = input.readEntry(key).map { codec.read(it) }
            setter.accept(value, if (result.isPresent) {
                result.get()
            } else if (fallback.isPresent) {
                fallback.get()
            } else {
                throw DataReadException("Missing value") // TODO add useful info
            })
        }

        fun write(value: T, output: CodecTemplate.MapOutput) {
            codec.write(getter.apply(value), output.writeEntry(key))
        }
    }

    fun read(input: CodecTemplate.MapInput): T

    override fun read(input: CodecTemplate.ValueInput): T {
        return read(input.readMap())
    }

    fun write(value: T, output: CodecTemplate.MapOutput)

    override fun write(value: T, output: CodecTemplate.ValueOutput) {
        write(value, output.writeMap())
    }

    companion object {
        @JvmStatic
        fun <T : Any> of(
            constructor: Supplier<T>,
            entries: List<Entry<T, *>>
        ) = object : MapCodecTemplate<T> {
            override fun read(input: CodecTemplate.MapInput): T {
                val value = constructor.get()

                for (entry in entries) {
                    entry.read(value, input)
                }

                return value
            }

            override fun write(value: T, output: CodecTemplate.MapOutput) {
                for (entry in entries) {
                    entry.write(value, output)
                }
            }
        }
    }
}