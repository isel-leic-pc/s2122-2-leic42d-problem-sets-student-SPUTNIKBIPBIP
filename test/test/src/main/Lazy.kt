package main

import java.util.concurrent.atomic.AtomicReference

class Lazy<T>(private val initializer: () -> T) {

    @Volatile
    private var value: AtomicReference<T> ? = AtomicReference<T>(null)

    fun get() : T {
        if (value?.get() == null) {
            return value!!.getAndSet(initializer())
        }
        return value!!.get()
    }
}
