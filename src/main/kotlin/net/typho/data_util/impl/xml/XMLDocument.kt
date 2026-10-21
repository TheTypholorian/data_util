package net.typho.data_util.impl.xml

open class XMLDocument(
    @JvmField
    var root: XMLElement,
    @JvmField
    var metadata: MutableList<XMLStubElement> = mutableListOf()
) {
    fun toXMLString() = (metadata + root).joinToString(separator = "") { it.toXMLString() }
}
