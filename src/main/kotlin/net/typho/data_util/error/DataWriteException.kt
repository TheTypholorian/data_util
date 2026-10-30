package net.typho.data_util.error

class DataWriteException : RuntimeException {
    constructor() : super()

    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(cause: Throwable) : super(cause)

    constructor(message: String, cause: Throwable, enableSuppression: Boolean, writableStackTrace: Boolean) : super(message, cause, enableSuppression, writableStackTrace)

    constructor(message: String, stack: ErrorStack?) : this(if (stack == null || stack.isEmpty()) message else "$message\n$stack")
}