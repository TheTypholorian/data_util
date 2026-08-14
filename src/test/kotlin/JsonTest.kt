import net.typho.tv_lib.io.impl.JsonFileFormat

object JsonTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val format = JsonFileFormat(
            allowComments = true,
            prettyPrint = true
        )
        val testText = """
        [
            "\n\r\t\u0001\uEEEE"
        ]
        """
        println(format.write(format.read(testText)))
    }
}