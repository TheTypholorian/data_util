package net.typho.tv_lib.io.impl.token

class BoolToken(
    override val line: Int? = null,
    override val value: Boolean
) : PrimitiveToken<Boolean> {
    override fun toString(): String {
        return "Bool($value)"
    }
}