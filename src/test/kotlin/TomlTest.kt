import net.typho.tv_lib.io.impl.TomlFormat

object TomlTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val format = TomlFormat()
        val testText = """
            abc="d#ef"
            jhi = 123 # test comment
            klm = '''
            # test \ncomment #2
            '''
        """
        val output = format.read(testText)
        println(output)
    }
}