package net.typho.data_util.impl.xml

data class XMLText(
    @JvmField
    var text: String
) : XMLNode {
    override fun toXMLString() = XMLFormat.writeEntities(text)

    override fun toString() = "XMLText($text)"
}