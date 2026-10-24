package net.typho.data_util

import sun.text.normalizer.UTF16.append
import java.util.function.Predicate
import kotlin.text.Typography.section

open class ParsingIterator @JvmOverloads constructor(
    @JvmField
    val text: String,
    @JvmField
    val range: IntRange = text.indices,
    index: Int = range.first
) : ListIterator<Char> {
    @JvmField
    var index: Int = index

    constructor(text: String, trim: Boolean) : this(
        text,
        if (trim) {
            IntRange(
                text.takeWhile { it.isWhitespace() }.length,
                text.length - text.takeLastWhile { it.isWhitespace() }.length - 1
            )
        } else {
            text.indices
        }
    )

    override fun hasNext() = index in range

    override fun hasPrevious() = index in range

    override fun next(): Char {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        return text[index++]
    }

    fun skip(amount: Int) {
        if ((index + amount) !in range) {
            throw NoSuchElementException()
        }

        index += amount
    }

    fun peek(): Char {
        if (index !in range) {
            throw NoSuchElementException()
        }

        return text[index]
    }

    fun peekOrNull(): Char? {
        if (index !in range) {
            return null
        }

        return text[index]
    }

    fun currentMatches(text: String): Boolean {
        return (index + text.length) in range && this.text.startsWith(text, startIndex = index)
    }

    fun indexOfNext(char: Char) = text.indexOf(char, startIndex = index)

    fun indexOfNext(text: String) = this.text.indexOf(text, startIndex = index)

    override fun nextIndex() = index

    override fun previous(): Char {
        if (!hasPrevious()) {
            throw NoSuchElementException()
        }

        return text[index--]
    }

    override fun previousIndex() = index - 1

    operator fun get(index: Int) = text[index]

    fun takeUntil(predicate: (index: Int, char: Char) -> Boolean): String? {
        val start = index
        var i = 0

        while (index in range) {
            val char = peek()

            if (predicate(i++, char)) {
                return text.substring(start, index)
            }

            next()
        }

        return null
    }

    fun takeUntilOrEnd(predicate: (index: Int, char: Char) -> Boolean): String {
        val start = index
        var i = 0

        while (index in range) {
            val char = peek()

            if (predicate(i++, char)) {
                break
            }

            next()
        }

        return text.substring(start, index)
    }

    fun skipUntil(predicate: (index: Int, char: Char) -> Boolean): Int? {
        var skipped = 0
        var i = 0

        while (index in range) {
            val char = peek()

            if (predicate(i++, char)) {
                return skipped
            }

            next()
            skipped++
        }

        return null
    }

    fun skipUntilOrEnd(predicate: (index: Int, char: Char) -> Boolean): Int {
        var skipped = 0
        var i = 0

        while (index in range) {
            val char = peek()

            if (predicate(i++, char)) {
                break
            }

            next()
            skipped++
        }

        return skipped
    }

    @JvmOverloads
    fun fork(range: IntRange = this.range, index: Int = this.index) = Child(text, range, index)

    @JvmOverloads
    fun error(message: String, index: Int = this.index): Nothing {
        throw DataReadException(message, text, index).also {
            it.stackTrace = it.stackTrace.copyOfRange(1, it.stackTrace.size)
        }
    }

    override fun toString(): String {
        if (index in range) {
            val section = text.substring((index - 3).coerceAtLeast(range.first), index) + "[" + text[index] + "]" + text.substring(index + 1, (index + 4).coerceAtMost(range.last))
            return "ParsingIterator(index=$index range=$range position=\"${section.replace("\n", "\\n")}\""
        } else {
            return "ParsingIterator(index=$index range=$range"
        }
    }

    open inner class Child @JvmOverloads constructor(
        text: String,
        range: IntRange = text.indices,
        index: Int = range.first
    ) : ParsingIterator(text, range, index) {
        fun merge(): ParsingIterator {
            val parent = this@ParsingIterator
            parent.index = index
            return parent
        }
    }
}