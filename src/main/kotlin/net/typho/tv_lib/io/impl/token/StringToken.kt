package net.typho.tv_lib.io.impl.token

class StringToken(
    override val line: Int,
    override val value: String
) : PrimitiveToken<String> {
    override fun toString(): String {
        return "String($value)"
    }
}