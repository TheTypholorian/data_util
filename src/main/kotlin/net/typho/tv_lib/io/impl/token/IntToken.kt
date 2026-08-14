package net.typho.tv_lib.io.impl.token

class IntToken(
    override val line: Int? = null,
    override val value: Int
) : PrimitiveToken<Int> {
    override fun toString(): String {
        return "Int($value)"
    }
}