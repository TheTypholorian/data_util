package net.typho.data_util.impl.md

sealed interface MarkdownTextNode : MarkdownNode {
    val text: String
}