package net.typho.tv_lib.io

interface DataObjectSerializer<T> {
    val type: Class<T>

    fun read(data: Any): T

    fun write(data: T): Any

    companion object {
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        operator fun <T : Any> Iterable<DataObjectSerializer<*>>.get(type: Class<T>): DataObjectSerializer<T>? {
            return firstOrNull { it.type == type }?.let { it as DataObjectSerializer<T> }
        }

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        operator fun <T : Any> Iterable<DataObjectSerializer<*>>.get(data: T?): DataObjectSerializer<T>? {
            return data?.javaClass?.let { get(it) }
        }

        @Suppress("UNCHECKED_CAST")
        fun Iterable<DataObjectSerializer<*>>.tryWrite(data: Any?): Any? {
            return data?.let { get(data)?.write(data) ?: data }
        }

        @JvmStatic
        inline fun <reified T> of(crossinline read: (data: Any) -> T, crossinline write: (data: T) -> Any) = object : DataObjectSerializer<T> {
            override val type: Class<T>
                get() = T::class.java

            override fun read(data: Any): T {
                return read.invoke(data)
            }

            override fun write(data: T): Any {
                return write.invoke(data)
            }
        }
    }
}