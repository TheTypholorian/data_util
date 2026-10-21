package net.typho.data_util.impl.md

data class MarkdownImage(
    @JvmField
    val path: String,
    @JvmField
    val altText: MarkdownMultiNode<MarkdownTextNode>
) : MarkdownNode
