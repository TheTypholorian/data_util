package net.typho.tv_lib.io.impl.token

class ObjectOpenToken(
    override val line: Int
) : Token {
    override fun toString(): String {
        return "ObjectOpen"
    }
}