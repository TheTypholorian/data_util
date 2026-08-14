import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import net.typho.tv_lib.io.impl.JsonFileFormat
import kotlin.random.Random
import kotlin.system.measureTimeMillis

object JsonTest {
    val random = Random(5)

    @JvmStatic
    fun main(args: Array<String>) {
        val format = JsonFileFormat(
            allowComments = true,
            prettyPrint = false
        )
        val obj = createObject(0)

        println("tv: " + measureTimeMillis {
            format.write(obj)
        } + " ms for write")
        val gson = toGson(obj)
        println("gson: " + measureTimeMillis {
            Gson().toJson(gson)
        } + " ms for write")

        val written = format.write(obj)
        println("tv: " + measureTimeMillis {
            format.read(written)
        } + " ms for read")
        println("gson: " + measureTimeMillis {
            JsonParser.parseString(written)
        } + " ms for read")
    }

    private fun toGson(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull.INSTANCE
            is Boolean -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value)
            is String -> JsonPrimitive(value)
            is List<*> -> JsonArray().also { value.mapTo(it.asList()) { toGson(it) } }
            is Map<*, *> -> JsonObject().also { value.mapKeys { (key, value) -> key as String }.mapValuesTo(it.asMap()) { (key, value) -> toGson(value) } }
            else -> throw AssertionError()
        }
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
            map[randomString(depth + 1)] = createAny(depth + 1)
        }

        return map
    }
}