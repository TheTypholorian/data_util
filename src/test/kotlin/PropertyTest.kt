import net.typho.data_util.impl.PropertiesFormat

object PropertyTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val format = PropertiesFormat()
        val testText = """
            abc=def
             ghi=jkl
            jkl = mno 
            mno = p\
                q\
                r
            pqr=s\nt\nv
        """
        val output = format.read(testText)
        println(format.write(output))
    }
}