import net.typho.tv_lib.io.impl.JsonFileFormat

object JsonTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val format = JsonFileFormat(
            allowComments = true,
            prettyPrint = true
        )
        val testText = """
            {
            // test comment
                "hello": "bye",
                "abc": /* another test comment */ 123,
                "def": 456.789, // a third test comment
                "mno": [
                    "m", "n", "o"
                ]
            }
        """
        val output = format.read(testText)
        println(output)
        println(format.write(output))
    }
}