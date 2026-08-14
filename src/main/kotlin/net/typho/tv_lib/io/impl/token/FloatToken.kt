package net.typho.tv_lib.io.impl.token

class FloatToken(
    override val line: Int = -1,
    override val content: Float
) : PrimitiveToken<Float> {
    override fun toString(): String {
        return "Float($content)"
    }
}