package net.typho.data_util.codec

interface CodecConverter<C : Any> {
    fun convert(codec: DataCodec<*>): C

    fun convert(codec: C): DataCodec<*>

    @Suppress("UNCHECKED_CAST")
    fun castConvert(codec: Any) = convert(codec as C)

    companion object {
        private val CONVERTERS = mutableMapOf<Class<*>, CodecConverter<*>>()

        @JvmStatic
        fun <C : Any> register(cls: Class<C>, converter: CodecConverter<C>) {
            CONVERTERS[cls] = converter
        }

        @Suppress("UNCHECKED_CAST")
        fun <C : Any> convert(codec: DataCodec<*>, to: Class<C>): C {
            return (CONVERTERS[to] ?: throw NullPointerException("$to is not a codec, and no CodecConverter has been registered for it")).convert(codec) as C
        }

        fun convert(codec: Any): DataCodec<*> {
            if (codec is DataCodec<*>) {
                return codec
            }

            var cls = codec.javaClass

            while (cls.isAnonymousClass) {
                cls = cls.superclass
            }

            return (CONVERTERS[cls] ?: throw NullPointerException("$codec is not a codec, and no CodecConverter has been registered for it")).castConvert(codec)
        }
    }
}
