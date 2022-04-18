package main

import kotlin.time.Duration

class Exchanger<T> {

    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): T? {
        return null
    }
}