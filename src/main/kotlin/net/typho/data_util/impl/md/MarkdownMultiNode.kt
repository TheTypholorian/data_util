package net.typho.data_util.impl.md

data class MarkdownMultiNode<N : MarkdownNode>(
    @JvmField
    val children: List<N>
) : MarkdownNode
