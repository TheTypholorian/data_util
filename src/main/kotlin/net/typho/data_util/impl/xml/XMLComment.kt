package net.typho.data_util.impl.xml

data class XMLComment(
    @JvmField
    var text: String
) : XMLNode {
    override fun toXMLString() = "<!--$text-->"

    override fun toString() = "XMLComment($text)"

    override fun boxAsElement(): XMLElement {
        return XMLElement("comment", children = mutableListOf(this))
    }
}