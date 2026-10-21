package net.typho.data_util.impl.md

data class MarkdownLink(
    @JvmField
    val path: String,
    @JvmField
    val displayText: MarkdownMultiNode<MarkdownTextNode>,
    @JvmField
    val title: String?
) : MarkdownNode
