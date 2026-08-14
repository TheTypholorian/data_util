package net.typho.tv_lib.io.impl.token

class NewlineToken(
    override val line: Int
) : Token {
    override fun toString(): String {
        return "Newline"
    }
}