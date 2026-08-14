package net.typho.tv_lib.io.impl.token

class ObjectCloseToken(
    override val line: Int
) : Token {
    override fun toString(): String {
        return "ObjectClose"
    }
}