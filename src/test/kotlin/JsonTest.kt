import net.typho.tv_lib.io.impl.JsonFileFormat
import kotlin.random.Random

object JsonTest {
    val random = Random(0)

    @JvmStatic
    fun main(args: Array<String>) {
        val format = JsonFileFormat(
            allowComments = true,
            prettyPrint = true
        )
        val obj = createObject(0)
        println(obj)
        println(format.write(obj))
    }

    private fun randomString(depth: Int): String {
        val length = Random.nextInt(3, 6 + depth * 2)
        return buildString(length) {
            repeat(length) {
                append(Random.nextInt(0x61, 0x7B).toChar())
            }
        }
    }

    private fun createAny(depth: Int): Any? {
        return when (random.nextInt(if (depth < 4) 6 else 4)) {
            0 -> random.nextBoolean()
            1 -> random.nextInt()
            2 -> random.nextFloat()
            3 -> null
            4 -> createObject(depth)
            5 -> createArray(depth)
            else -> throw AssertionError()
        }
    }

    private fun createArray(depth: Int): List<Any?> {
        val list = mutableListOf<Any?>()

        repeat(random.nextInt(12)) {
            list.add(createAny(depth + 1))
        }

        return list
    }

    private fun createObject(depth: Int): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        repeat(random.nextInt(12)) {
            map.put(randomString(depth + 1), createAny(depth + 1))
        }

        return map
    }
}