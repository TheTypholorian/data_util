package net.typho.tv_lib.io.impl.token

class EqualsToken(
    override val line: Int
) : Token {
    override fun toString(): String {
        return "Equals"
    }
}