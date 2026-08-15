package net.typho.data_util.codec

import net.typho.data_util.DataReadException
import java.util.Optional
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Supplier

interface MapDataCodec<T : Any> : DataCodec<T> {
    data class Entry<T : Any, V : Any> @JvmOverloads constructor(
        @JvmField
        val key: String,
        @JvmField
        val codec: DataCodec<V>,
        @JvmField
        val getter: Function<T, V>,
        @JvmField
        val setter: BiConsumer<T, V>,
        @JvmField
        val fallback: Optional<V> = Optional.empty<V>()
    ) {
        fun read(value: T, input: MapInput) {
            val result = input.readEntry(key).map { codec.read(it) }
            setter.accept(value, if (result.isPresent) {
                result.get()
            } else if (fallback.isPresent) {
                fallback.get()
            } else {
                throw DataReadException("Missing value") // TODO add useful info
            })
        }

        fun write(value: T, output: MapOutput) {
            codec.write(getter.apply(value), output.writeEntry(key))
        }
    }

    fun read(input: MapInput): T

    override fun read(input: SingleValueInput): T {
        return read(input.readMap())
    }

    fun write(value: T, output: MapOutput)

    override fun write(value: T, output: SingleValueOutput) {
        write(value, output.writeMap())
    }

    companion object {
        @JvmStatic
        fun <T : Any> of(
            constructor: Supplier<T>,
            entries: List<Entry<T, *>>
        ) = object : MapDataCodec<T> {
            override fun read(input: MapInput): T {
                val value = constructor.get()

                for (entry in entries) {
                    entry.read(value, input)
                }

                return value
            }

            override fun write(value: T, output: MapOutput) {
                for (entry in entries) {
                    entry.write(value, output)
                }
            }
        }
    }
}