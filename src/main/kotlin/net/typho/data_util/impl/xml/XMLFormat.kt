package net.typho.data_util.impl.xml

import net.typho.data_util.DataFormat
import net.typho.data_util.DataReadException
import net.typho.data_util.ParsingIterator
import java.util.function.Predicate

open class XMLFormat(
    @JvmField
    val readComments: Boolean = false
) : DataFormat<String, XMLDocument> {
    override fun read(input: String): XMLDocument {
        val input = ParsingIterator(input, true)

        fun isValidChar(char: Char) = char in 'A'..'Z' || char in 'a'..'z' || char == '_' || char == ':'

        fun isStartChar(char: Char, metadataAllowed: Boolean) = isValidChar(char) || (char == '?' && metadataAllowed)

        fun isNextChar(char: Char) = isValidChar(char) || char in '0'..'9' || char == '-' || char == '.'

        fun readWord(metadataAllowed: Boolean): String {
            input.skipUntil { !it.isWhitespace() }
            return input.takeUntil(
                { char ->
                    if (!isStartChar(char, metadataAllowed)) {
                        input.error("Invalid XML element name (starts with illegal character $char)")
                    }

                    false
                },
                { char ->
                    if (!isNextChar(char)) {
                        input.error("Invalid XML element name (starts with illegal character $char)")
                    }

                    char.isWhitespace()
                }
            )
        }

        fun skipWhitespacesUntil(vararg expected: Char, error: (Char) -> Nothing): Char {


            while (true) {
                val char = input.next()

                when {
                    char.isWhitespace() -> continue
                    char in expected -> return char
                    else -> error(char)
                }
            }
        }

        fun peekNextNonWhitespaceChar(): Pair<Int, Char> {
            var index = charIndex

            while (true) {
                val char = input[index]

                if (!char.isWhitespace()) {
                    return index to char
                }

                index++
            }
        }

        fun readText(end: Predicate<Char>): String {
            return readEntities(buildString {
                while (true) {
                    val char = input[charIndex++]

                    when {
                        end.test(char) -> break
                        else -> append(char)
                    }
                }
            }) // TODO custom entities
        }

        fun readQuoted(): String {
            val quote = skipWhitespacesUntil('"', '\'') { throw DataReadException("Illegal open quotes character: $it") }
            return readText { it == quote }
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

            val first = input.fork()
            first.skipUntil { !it.isWhitespace() }

            if (first.current() == '/') {
                closure = 1
                charIndex = firstChar.first + 1
            } else if (input.text.startsWith("!--", startIndex = firstChar.first)) {
                val commentStart = firstChar.first + "!--".length
                val end = input.text.indexOf("--", startIndex = commentStart)

                if (input[end + 2] != '>') {
                    throw DataReadException("Cannot have dangling '--' in a comment, must signify end of comment")
                }

                charIndex = end + "-->".length

                return Tag(input.text.substring(commentStart, end), true, 0, mutableMapOf())
            }

            // read name
            val name = readWord(metadataAllowed)
            val attributes = mutableMapOf<String, String>()

            // read attributes
            while (true) {
                val char = input[charIndex++]

                when {
                    char.isWhitespace() -> continue
                    (char == '/') || char == '?' && name.startsWith('?') -> {
                        skipWhitespacesUntil('>') { throw DataReadException("Invalid XML character after ending character in element header: $it") }

                        if (closure == 1) {
                            throw DataReadException("XML tag both closes its opening tag and itself (ex. </name/>), must be one or the other.")
                        }

                        closure = 2
                        break
                    }
                    char == '>' -> break
                    else -> { // read attribute
                        charIndex--
                        val key = readWord(false)
                        skipWhitespacesUntil('=') { throw DataReadException("Invalid XML character after attribute name: $it") }
                        val value = readQuoted()

                        if (attributes.put(key, value) != null) {
                            throw DataReadException("Duplicate XML attribute '$key'")
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
                throw DataReadException("XML tag closes a nonexistent opening tag")
            }

            val element = XMLElement(start.textData, start.attributes)

            // read tag contents
            if (start.closure == 0) {
                val text = StringBuilder()

                while (true) {
                    val char = input[charIndex++]

                    if (char == '<') {
                        if (text.isNotEmpty()) {
                            element.children.add(XMLText(readEntities(text.toString())))
                            text.clear()
                        }

                        val tag = readTag(false)

                        if (!tag.isComment && tag.textData == start.textData && tag.closure == 1) {
                            if (tag.attributes.isNotEmpty()) {
                                throw DataReadException("XML end tag contains attributes, only opening tags can have attributes.")
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
            skipWhitespacesUntil('<') { throw DataReadException("Illegal starting XML character: $it") }
            val tag = readTag(true)

            if (tag.isComment) {
                continue
            }

            if (tag.textData.startsWith('?')) {
                metadata.add(XMLStubElement(tag.textData, tag.attributes))
            } else {
                root = (readNode(tag) as? XMLElement) ?: throw DataReadException("XML root value is not an element")
                break
            }
        }

        if (charIndex != input.length) {
            throw DataReadException("Trailing data after root XML element")
        }

        return XMLDocument(root, metadata)
    }

    override fun write(data: XMLDocument) = data.toXMLString()

    fun write(element: XMLElement) = element.toXMLString()

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
        fun readEntities(text: String, entities: Map<String, String> = defaultEntities): String {
            return buildString {
                var index = 0

                while (index < text.length) {
                    val char = text[index]

                    if (char == '&') {
                        val endIndex = text.indexOf(';', startIndex = index)
                        val key = text.substring(index + 1, endIndex)
                        append(entities[key] ?: throw DataReadException("Undefined entity &$key;"))
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
            val parsed = format.read("<?xml version=\"1.0\" ?><abc attribute1='123' attribute2='456'>contents &amp; &lt;<inner a1=\"123472173\">abc123<!--this is a comment--><_123/></inner>other contents</abc>")
            println(parsed)
            println(format.write(parsed))
        }
    }
}