package net.typho.data_util.impl.xml

sealed interface XMLNode {
    fun toXMLString(): String

    fun asElement() = this as? XMLElement

    fun asText() = this as? XMLText

    fun asComment() = this as? XMLComment

    fun boxAsElement(): XMLElement
}