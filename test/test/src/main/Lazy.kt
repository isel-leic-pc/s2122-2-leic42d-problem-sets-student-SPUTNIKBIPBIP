package main

import java.util.concurrent.atomic.AtomicReference

class Lazy<T>(private val initializer: () -> T) {

    private var value = AtomicReference<T>()

    fun get() : T {
        val obserValue = value.get()

        if (obserValue == null) {
            if (value.compareAndSet(obserValue, initializer()))
                return value.get()
        }
        return value.get()
    }
}
