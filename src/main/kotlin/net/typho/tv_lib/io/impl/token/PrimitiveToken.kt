package net.typho.tv_lib.io.impl.token

sealed interface PrimitiveToken<V> : Token {
    val value: V
}