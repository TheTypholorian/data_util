package net.typho.data_util.xml

open class XMLDocument(
    @JvmField
    var root: XMLElement,
    @JvmField
    var metadata: MutableList<XMLStubElement> = mutableListOf()
) {
    fun toXMLString(prettyPrint: Boolean = false) = (metadata + root).joinToString(separator = "") { it.toXMLString(prettyPrint) }

    override fun toString(): String {
        return "XMLDocument(metadata=${metadata} root=$root)"
    }
}
