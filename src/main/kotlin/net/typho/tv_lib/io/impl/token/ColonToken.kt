package net.typho.tv_lib.io.impl.token

class ColonToken(
    override val line: Int? = null
) : Token {
    override fun toString(): String {
        return "Colon"
    }
}