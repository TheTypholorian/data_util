package net.typho.tv_lib.io.impl.token

enum class QuoteType(
    @JvmField
    val text: String
) {
    REGULAR("\""),
    SINGLE("'"),
    TRIPLE_REGULAR("\"\"\""),
    TRIPLE_SINGLE("'''")
}