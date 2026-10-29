package net.typho.data_util.xml

open class XMLText(
    @JvmField
    var text: String
) : XMLNode {
    override fun toXMLString(prettyPrint: Boolean) = XMLFormat.writeEntities(text)

    override fun toString() = "XMLText($text)"
}