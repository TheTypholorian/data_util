import net.typho.tv_lib.io.impl.JsonFileFormat
import kotlin.random.Random
import kotlin.system.measureTimeMillis

object JsonTest {
    val random = Random(5)

    @JvmStatic
    fun main(args: Array<String>) {
        val format = JsonFileFormat(
            allowComments = true,
            prettyPrint = true
        )
        val obj = createObject(0)
        val written: String
        println(measureTimeMillis {
            written = format.write(obj)
        })
        //println(written)
    }

    private fun randomString(depth: Int): String {
        val length = random.nextInt(3, 6 + depth * 2)
        return buildString(length) {
            repeat(length) {
                //append(random.nextInt().toChar())
                append(random.nextInt(0x61, 0x7B).toChar())
            }
        }
    }

    private fun createAny(depth: Int): Any? {
        return when (random.nextInt(if (depth < 12) 7 else 5)) {
            0 -> random.nextBoolean()
            1 -> random.nextInt()
            2 -> random.nextFloat()
            3 -> null
            4 -> randomString(depth)
            5 -> createObject(depth)
            6 -> createArray(depth)
            else -> throw AssertionError()
        }
    }

    private fun createArray(depth: Int): List<Any?> {
        val list = mutableListOf<Any?>()

        repeat(random.nextInt(20)) {
            list.add(createAny(depth + 1))
        }

        return list
    }

    private fun createObject(depth: Int): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        repeat(random.nextInt(20)) {
            map.put(randomString(depth + 1), createAny(depth + 1))
        }

        return map
    }
}