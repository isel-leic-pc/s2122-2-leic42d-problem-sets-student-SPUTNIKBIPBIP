package main

import java.util.concurrent.atomic.AtomicReference

class Lazy<T>(private val initializer: () -> T) {

    private var value: AtomicReference<T> ? = AtomicReference<T>(null)

    fun get() : T {
        if (value?.get() == null) {
            if (value.)
            return value!!.getAndSet(initializer())
        }
        return value!!.get()
    }
}
