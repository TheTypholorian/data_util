package net.typho.data_util.impl.md

data class MarkdownHeaderInfo(
    /**
     * 1 is biggest header, 6 is smallest header
     */
    @JvmField
    val size: Int,
    @JvmField
    val id: String?
)
