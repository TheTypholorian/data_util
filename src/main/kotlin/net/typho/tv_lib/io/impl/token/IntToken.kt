package net.typho.tv_lib.io.impl.token

class IntToken(
    override val line: Int = -1,
    override val content: Int
) : PrimitiveToken<Int> {
    override fun toString(): String {
        return "Int($content)"
    }
}