package net.typho.tv_lib.io.impl.token

class ColonToken(
    override val line: Int
) : Token {
    override fun toString(): String {
        return "Colon"
    }
}