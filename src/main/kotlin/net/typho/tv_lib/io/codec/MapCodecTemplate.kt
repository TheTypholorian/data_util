package net.typho.tv_lib.io.codec

import java.util.function.BiConsumer
import java.util.function.Function

data class MapCodecTemplate<T>(
    @JvmField
    val constructor: () -> T,
    @JvmField
    val entries: List<Entry<T, *>>
) : CodecTemplate<T> {
    data class Entry<T, V>(
        @JvmField
        val name: String,
        @JvmField
        val codec: CodecTemplate<V>,
        @JvmField
        val getter: Function<T, V>,
        @JvmField
        val setter: BiConsumer<T, V>
    )
}