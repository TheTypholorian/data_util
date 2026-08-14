package net.typho.tv_lib.io.impl.token

class CommaToken(
    override val line: Int? = null
) : Token {
    override fun toString(): String {
        return "Comma"
    }
}