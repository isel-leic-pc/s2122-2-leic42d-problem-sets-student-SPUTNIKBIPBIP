package main

import java.util.concurrent.atomic.AtomicReference

class Lazy<T>(private val initializer: () -> T) {

    private var value = AtomicReference<T>()

    //TODO: implement some kind of flag in order to just let the first thread to run the initializar
    fun get() : T {
        val obserValue = value.get()
        if (obserValue == null) {
            if (value.compareAndSet(obserValue, initializer()))
                return value.get()
        }
        return value.get()
    }
}
