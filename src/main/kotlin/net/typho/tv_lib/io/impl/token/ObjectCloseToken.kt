package net.typho.tv_lib.io.impl.token

class ObjectCloseToken(
    override val line: Int = -1
) : Token {
    override fun toString(): String {
        return "ObjectClose"
    }
}