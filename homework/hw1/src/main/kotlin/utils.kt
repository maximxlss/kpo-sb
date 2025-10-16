package ru.msk.xls

sealed class AskResult<T> {
    data class Success<T>(val value: T) : AskResult<T>()
    data class Failure<T>(val message: String?) : AskResult<T>()

    companion object {
        fun <T> ok(value: T): AskResult<T> = Success(value)
        fun <T> fail(message: String? = null): AskResult<T> = Failure(message)
    }
}


inline fun <T> getOrRetry(get: () -> AskResult<T>): T {
    while (true) {
        when (val v = get()) {
            is AskResult.Success -> {
                return v.value
            }

            is AskResult.Failure -> {
                if (v.message != null) {
                    println(v.message)
                }
            }
        }
    }
}
