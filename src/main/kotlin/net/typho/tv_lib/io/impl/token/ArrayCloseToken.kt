package net.typho.tv_lib.io.impl.token

class ArrayCloseToken(
    override val line: Int? = null
) : Token {
    override fun toString(): String {
        return "ArrayClose"
    }
}