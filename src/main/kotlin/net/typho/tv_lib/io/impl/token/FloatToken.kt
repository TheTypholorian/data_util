package net.typho.tv_lib.io.impl.token

class FloatToken(
    override val line: Int,
    override val value: Float
) : PrimitiveToken<Float> {
    override fun toString(): String {
        return "Float($value)"
    }
}