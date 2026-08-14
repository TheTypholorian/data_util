package net.typho.tv_lib.io.impl.token

class BoolToken(
    override val line: Int = -1,
    override val content: Boolean
) : PrimitiveToken<Boolean> {
    override fun toString(): String {
        return "Bool($content)"
    }
}