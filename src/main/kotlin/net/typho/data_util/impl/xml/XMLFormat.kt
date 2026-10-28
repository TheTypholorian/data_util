package net.typho.data_util.impl.xml

import net.typho.data_util.DataFormat
import net.typho.data_util.ParsingIterator
import net.typho.data_util.SerializedData
import net.typho.data_util.codec.ValueInput
import net.typho.data_util.codec.ValueOutput

open class XMLFormat(
    @JvmField
    val readComments: Boolean = false,
    @JvmField
    val prettyPrint: Boolean = false
) : DataFormat<XMLDocument> {
    override fun createInput(data: XMLDocument): ValueInput {
        TODO("Not yet implemented")
    }

    override fun createOutput(): ValueOutput.WithResult<XMLDocument> {
        TODO("Not yet implemented")
    }

    override fun read(input: SerializedData): XMLDocument {
        val input = ParsingIterator(input.asString(), true)

        fun readWord(metadataAllowed: Boolean): String {
            fun isValidChar(char: Char) = char in 'A'..'Z' || char in 'a'..'z' || char == '_' || char == ':'

            input.skipUntil { index, char -> !char.isWhitespace() }
            return input.takeUntil { index, char ->
                if (index == 0) {
                    if (!(isValidChar(char) || (char == '?' && metadataAllowed))) {
                        input.error("Invalid XML element name (starts with illegal character $char)")
                    }

                    false
                } else {
                    !(isValidChar(char) || char in '0'..'9' || char == '-' || char == '.')
                }
            } ?: input.error("Unterminated XML word")
        }

        fun skipOnlyWhitespacesUntil(vararg expected: Char, error: (Char) -> Nothing): Char {
            input.skipUntil { index, char ->
                if (char in expected) {
                    return@skipUntil true
                }

                if (!char.isWhitespace()) {
                    error(char)
                }

                return@skipUntil false
            }
            return input.next()
        }

        data class Tag(
            // name for elements, content of comments
            @JvmField
            val textData: String,
            // TODO separate handling for comments
            @JvmField
            val isComment: Boolean,
            // integer cus we can't have a local enum class (ugh)
            // 0 for <tag>, 1 </tag>, 2 for <tag/>
            @JvmField
            val closure: Int,
            @JvmField
            val attributes: MutableMap<String, String>
        )

        fun readTag(metadataAllowed: Boolean): Tag {
            var closure = 0

            val opening = input.fork()
            opening.skipUntil { index, char -> !char.isWhitespace() }

            if (opening.peek() == '/') {
                closure = 1
                opening.next()
                opening.merge()
            } else if (opening.currentMatches("!--")) {
                opening.skip("!--".length)
                val end = opening.indexOfNext("--")

                if (end == -1) {
                    input.error("Comment is never ended")
                }

                if (opening[end + 2] != '>') {
                    input.error("Cannot have dangling '--' in a comment, must signify end of comment")
                }

                val comment = opening.text.substring(opening.index, end)
                input.index = end + "-->".length

                return Tag(comment, true, 0, mutableMapOf())
            }

            // read name
            val name = readWord(metadataAllowed)
            val attributes = mutableMapOf<String, String>()

            // read attributes
            while (true) {
                val char = input.peek()

                when {
                    char.isWhitespace() -> {
                        input.next()
                        continue
                    }
                    (char == '/') || char == '?' && name.startsWith('?') -> {
                        input.next()
                        skipOnlyWhitespacesUntil('>') { input.error("Invalid XML character after ending character in element header: $it") }

                        if (closure == 1) {
                            input.error("XML tag both closes its opening tag and itself (ex. </name/>), must be one or the other.")
                        }

                        closure = 2
                        break
                    }
                    char == '>' -> {
                        input.next()
                        break
                    }
                    else -> { // read attribute
                        val key = readWord(false)
                        skipOnlyWhitespacesUntil('=') { input.error("Invalid XML character after attribute name: $it") }
                        val quote = skipOnlyWhitespacesUntil('"', '\'') { input.error("Illegal open quotes character: $it") }
                        val value = readEntities(buildString {
                            for (char in input) {
                                if (char == quote) {
                                    break
                                } else {
                                    this.append(char)
                                }
                            }
                        }, { input.error("Undefined entity &$it;") }) // TODO custom entities

                        if (attributes.put(key, value) != null) {
                            input.error("Duplicate XML attribute '$key'")
                        }
                    }
                }
            }

            return Tag(name, false, closure, attributes)
        }

        fun readNode(start: Tag): XMLNode? {
            if (start.isComment) {
                return if (readComments) XMLComment(start.textData) else null
            }

            if (start.closure == 1) {
                input.error("XML tag closes a nonexistent opening tag")
            }

            val element = XMLElement(start.textData, start.attributes)

            // read tag contents
            if (start.closure == 0) {
                val text = StringBuilder()

                while (true) {
                    val char = input.next()

                    if (char == '<') {
                        if (text.isNotEmpty()) {
                            element.children.add(XMLText(readEntities(text.toString(), { input.error("Undefined entity &$it;") })))
                            text.clear()
                        }

                        val tag = readTag(false)

                        if (!tag.isComment && tag.textData == start.textData && tag.closure == 1) {
                            if (tag.attributes.isNotEmpty()) {
                                input.error("XML end tag contains attributes, only opening tags can have attributes.")
                            }

                            return element
                        } else {
                            readNode(tag)?.let { element.children.add(it) }
                        }
                    } else {
                        text.append(char)
                    }
                }
            }

            return element
        }

        val metadata = mutableListOf<XMLStubElement>()
        lateinit var root: XMLElement

        while (true) {
            skipOnlyWhitespacesUntil('<') { input.error("Illegal starting XML character: $it") }
            val tag = readTag(true)

            if (tag.isComment) {
                continue
            }

            if (tag.textData.startsWith('?')) {
                metadata.add(XMLStubElement(tag.textData, tag.attributes))
            } else {
                root = (readNode(tag) as? XMLElement) ?: input.error("XML root value is not an element")
                break
            }
        }

        /*
        if (charIndex != input.length) {
            input.error("Trailing data after root XML element")
        }
         */

        return XMLDocument(root, metadata)
    }

    override fun write(data: XMLDocument) = SerializedData(data.toXMLString(prettyPrint))

    fun write(element: XMLElement) = element.toXMLString(prettyPrint)

    companion object Default : XMLFormat() {
        @JvmField
        val defaultEntities = mapOf(
            "amp" to "&",
            "lt" to "<",
            "gt" to ">",
            "quot" to "\"",
            "apos" to "'"
        )

        @JvmStatic
        fun readEntities(text: String, error: (key: String) -> Nothing, entities: Map<String, String> = defaultEntities): String {
            return buildString {
                var index = 0

                while (index < text.length) {
                    val char = text[index]

                    if (char == '&') {
                        val endIndex = text.indexOf(';', startIndex = index)
                        val key = text.substring(index + 1, endIndex)
                        append(entities[key] ?: error(key))
                        index = endIndex + 1
                    } else {
                        append(char)
                    }

                    index++
                }
            }
        }

        @JvmStatic
        fun writeEntities(text: String, entities: Map<String, String> = defaultEntities): String {
            return buildString {
                var index = 0

                while (index < text.length) {
                    val char = text[index]

                    val entity = entities.filter { (key, value) -> text.startsWith(value, startIndex = index) }
                        .maxByOrNull { (key, value) -> value.length }

                    if (entity == null) {
                        append(char)
                        index++
                    } else {
                        append("&${entity.key};")
                        index += entity.value.length
                    }
                }
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val format = XMLFormat
            val xml = XMLElement("root", children = mutableListOf(
                XMLElement("abc"),
                XMLElement("def", attributes = mutableMapOf("attribute1" to "123", "attribute2" to "4\"5\"6")),
                XMLElement("ghi", children = mutableListOf(
                    XMLElement("abc1"),
                    XMLElement("def1", attributes = mutableMapOf("attribute1" to "123", "attribute2" to "4\"5\"6")),
                    XMLElement("ghi1")
                )),
            ))
            println(format.write(xml))
            println()
            val parsed = format.read(SerializedData("<?xml version=\"1.0\" ?><abc attribute1='123' attribute2='456'>contents &amp; &lt;<inner a1=\"123472173\">abc123<!--this is a comment--><_123/></inner>other contents</abc>"))
            println(parsed)
            println(format.write(parsed))
        }
    }
}