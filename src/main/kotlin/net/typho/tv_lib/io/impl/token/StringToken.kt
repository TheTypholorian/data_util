package net.typho.tv_lib.io.impl.token

class StringToken(
    override val line: Int = -1,
    override val content: String
) : PrimitiveToken<String> {
    override fun toString(): String {
        return "String($content)"
    }
}