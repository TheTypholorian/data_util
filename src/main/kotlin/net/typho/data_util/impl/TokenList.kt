package net.typho.data_util.impl

class TokenList : ArrayList<Any?>() {
    @JvmField
    val newlines = mutableListOf<Int>()

    fun lastLineNumber() = newlines.size

    fun lineNumber(at: Int): Int {
        var line = 0

        println("line number for $at ${get(at)}")

        for (index in newlines) {
            println("$index ${getOrNull(index)}")
            if (index > at) {
                break
            }

            line++
        }

        return line
    }

    fun newLine() {
        newlines.add(size)
    }

    fun newLine(line: Int) {
        repeat(line) {
            newLine()
        }
    }
}