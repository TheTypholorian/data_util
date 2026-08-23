package net.typho.data_util.codec

interface OptionalCodec<T> : Codec<T> {
    val default: T
}