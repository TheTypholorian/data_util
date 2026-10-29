package net.typho.data_util.xml

import net.typho.data_util.ParsingIterator
import java.io.File

object MarkdownUtil {
    @JvmField
    val escapableChars = setOf('\\', '`', '*', '_', '{', '}', '[', ']', '<', '>', '(', ')', '#', '+', '-', '.', '!', '|')
    @JvmField
    val ruleChars = setOf('_', '-', '*')
    @JvmField
    val emphasisChars = setOf('_', '*')

    @JvmStatic
    fun markdownToHtml(input: String): List<XMLNode> {
        val input = ParsingIterator(input, trim = false)
        val result = mutableListOf<XMLNode>()
        val buffer = StringBuilder()

        fun flush() {
            if (!buffer.isEmpty()) {
                result.add(XMLText(buffer.toString()))
                buffer.clear()
            }
        }

        fun readEmphasis(name: String, char: Char, size: Int): Boolean {
            val fork = input.fork()
            val expected = char.toString().repeat(size)

            repeat(size - 1) {
                if (!fork.hasNext()) {
                    return false
                }

                if (fork.next() != char) {
                    return false
                }
            }

            val text = fork.takeUntil { index, char ->
                when (char) {
                    '\n' -> return@takeUntil false
                    char -> fork.currentMatches(expected)
                    else -> false
                }
            } ?: return false

            repeat(size) {
                if (!fork.hasNext()) {
                    return false
                }

                if (fork.next() != char) {
                    return false
                }
            }

            fork.merge()
            flush()
            result.add(XMLElement(name, children = markdownToHtml(text).toMutableList()))
            return true
        }

        while (input.hasNext()) {
            val char = input.next()

            when (char) {
                '\\' -> {
                    val escaped = input.next()

                    if (escaped !in escapableChars) {
                        input.error("Trailing backslash")
                    }

                    buffer.append(escaped)
                    continue
                }
                '#' -> {
                    flush()
                    val size = input.skipUntilOrEnd { index, char -> char != '#' } + 1
                    input.skipUntil { index, char -> !char.isWhitespace() }
                    val header = input.takeUntilOrEnd { index, char -> char == '\n' }

                    if (input.peekOrNull() == '\n') {
                        input.next() // skip newline
                    }

                    result.add(XMLElement(name = "h$size", children = markdownToHtml(header).toMutableList()))
                    continue
                }
                '>' -> {
                    flush()
                    val size = input.skipUntilOrEnd { index, char -> char != '>' }
                    input.skipUntil { index, char -> !char.isWhitespace() }
                    val blockquote = input.takeUntilOrEnd { index, char -> char == '\n' }

                    if (input.peekOrNull() == '\n') {
                        input.next() // skip newline
                    }

                    var element = XMLElement(name = "blockquote", children = markdownToHtml(blockquote).toMutableList())

                    repeat(size) {
                        element = XMLElement(name = "blockquote", children = mutableListOf(element))
                    }

                    result.add(element)
                    continue
                }
            }

            if (char in ruleChars) {
                val fork = input.fork()

                if (fork.skipUntilOrEnd { index, char1 -> char1 != char } >= 2) {
                    flush()
                    fork.merge()
                    result.add(XMLElement("hr"))
                    continue
                }
            }

            if (char in emphasisChars) {
                val bold = input.peek() == char

                if (readEmphasis(if (bold) "strong" else "em", char, if (bold) 2 else 1)) {
                    continue
                }
            }

            if (char == '~') {
                if (readEmphasis("strike", char, 2)) {
                    continue
                } else if (readEmphasis("sub", char, 1)) {
                    continue
                }
            }

            if (char == '^') {
                if (readEmphasis("sup", char, 1)) {
                    continue
                }
            }

            buffer.append(char)
        }

        result.add(XMLText(buffer.toString())) // TODO

        return result
    }

    @JvmStatic
    fun markdownToHtml(element: XMLElement): XMLElement {
        return XMLElement(element.name, element.attributes.toMutableMap(), element.children.flatMapTo(mutableListOf()) { node ->
            when (node) {
                is XMLText -> markdownToHtml(node.text)
                is XMLElement -> listOf(markdownToHtml(node))
                else -> listOf(node)
            }
        })
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val markdown = """
            # h1 Heading 8-)
            ## h2 Heading
            ### h3 Heading
            #### h4 Heading
            ##### h5 Heading
            ###### h6 Heading


            ## Horizontal Rules

            ___

            ---

            ***


            ## Typographic replacements

            Enable typographer option to see result.

            (c) (C) (r) (R) (tm) (TM) +-

            test.. test... test..... test?..... test!....

            !!!!!! ???? ,,  -- ---

            "Smartypants, double quotes" and 'single quotes'


            ## Emphasis

            **This is bold text**

            __This is bold text__

            *This is italic text*

            _This is italic text_

            ~~Strikethrough~~


            ## Blockquotes


            > Blockquotes can also be nested...
            >> ...by using additional greater-than signs right next to each other...
            > > > ...or with spaces between arrows.


            ## Lists

            Unordered

            + Create a list by starting a line with `+`, `-`, or `*`
            + Sub-lists are made by indenting 2 spaces:
              - Marker character change forces new list start:
                * Ac tristique libero volutpat at
                + Facilisis in pretium nisl aliquet
                - Nulla volutpat aliquam velit
            + Very easy!

            Ordered

            1. Lorem ipsum dolor sit amet
            2. Consectetur adipiscing elit
            3. Integer molestie lorem at massa


            1. You can use sequential numbers...
            1. ...or keep all the numbers as `1.`

            Start numbering with offset:

            57. foo
            1. bar


            ## Code

            Inline `code`

            Indented code

                // Some comments
                line 1 of code
                line 2 of code
                line 3 of code


            Block code "fences"

            ```
            Sample text here...
            ```

            Syntax highlighting

            ``` js
            const foo = function (bar) {
              return bar++;
            };

            console.log(foo(5));
            ```

            ## Tables

            | Option | Description |
            | ------ | ----------- |
            | data   | path to data files to supply the data that will be passed into templates. |
            | engine | engine to be used for processing templates. Handlebars is the default. |
            | ext    | extension to be used for dest files. |

            Right aligned columns

            | Option | Description |
            | ------:| -----------:|
            | data   | path to data files to supply the data that will be passed into templates. |
            | engine | engine to be used for processing templates. Handlebars is the default. |
            | ext    | extension to be used for dest files. |


            ## Links

            [link text](http://dev.nodeca.com)

            [link with title](http://nodeca.github.io/pica/demo/ "title text!")

            Autoconverted link https://github.com/nodeca/pica (enable linkify to see)


            ## Images

            ![Minion](https://octodex.github.com/images/minion.png)
            ![Stormtroopocat](https://octodex.github.com/images/stormtroopocat.jpg "The Stormtroopocat")

            Like links, Images also have a footnote style syntax

            ![Alt text][id]

            With a reference later in the document defining the URL location:

            [id]: https://octodex.github.com/images/dojocat.jpg  "The Dojocat"


            ## Plugins

            The killer feature of `markdown-it` is very effective support of
            [syntax plugins](https://www.npmjs.org/browse/keyword/markdown-it-plugin).


            ### [Emojies](https://github.com/markdown-it/markdown-it-emoji)

            > Classic markup: :wink: :cry: :laughing: :yum:
            >
            > Shortcuts (emoticons): :-) :-( 8-) ;)

            see [how to change output](https://github.com/markdown-it/markdown-it-emoji#change-output) with twemoji.


            ### [Subscript](https://github.com/markdown-it/markdown-it-sub) / [Superscript](https://github.com/markdown-it/markdown-it-sup)

            - 19^th^
            - H~2~O


            ### [\<ins>](https://github.com/markdown-it/markdown-it-ins)

            ++Inserted text++


            ### [\<mark>](https://github.com/markdown-it/markdown-it-mark)

            ==Marked text==


            ### [Footnotes](https://github.com/markdown-it/markdown-it-footnote)

            Footnote 1 link[^first].

            Footnote 2 link[^second].

            Inline footnote^[Text of inline footnote] definition.

            Duplicated footnote reference[^second].

            [^first]: Footnote **can have markup**

                and multiple paragraphs.

            [^second]: Footnote text.


            ### [Definition lists](https://github.com/markdown-it/markdown-it-deflist)

            Term 1

            :   Definition 1
            with lazy continuation.

            Term 2 with *inline markup*

            :   Definition 2

                    { some code, part of Definition 2 }

                Third paragraph of definition 2.

            _Compact style:_

            Term 1
              ~ Definition 1

            Term 2
              ~ Definition 2a
              ~ Definition 2b
        """.trimIndent()
        println("HTML:")
        /*
        XMLFormat.read("""
            <html lang="en">
            <head>
              <meta charset="utf-8" />
            </head>

            <body>
            $markdown
            </body>
            </html>
        """.trimIndent()).root
         */
        val html = XMLElement(
            "html",
            attributes = mutableMapOf("lang" to "en"),
            children = mutableListOf(
                XMLElement(
                    "head",
                    children = mutableListOf(
                        XMLElement(
                            "meta",
                            attributes = mutableMapOf("charset" to "utf-8")
                        )
                    )
                ),
                XMLElement(
                    "body",
                    children = markdownToHtml(markdown).toMutableList()
                )
            )
        ).toXMLString(true)
        println(html)
        File("test.html").writeText(html)
    }
}