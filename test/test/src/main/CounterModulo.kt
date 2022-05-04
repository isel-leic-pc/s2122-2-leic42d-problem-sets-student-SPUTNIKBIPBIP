package main

import java.util.concurrent.atomic.AtomicInteger

class CounterModulo(private val moduloValue: Int) {

    private val value = AtomicInteger(0)

    fun increment(): Int {
        do {
            val valueRead = value.get()
            if (valueRead != moduloValue - 1) return value.incrementAndGet()
            if (value.compareAndSet(valueRead, 0)) return 0
        } while (true)
    }

    fun decrement(): Int {
        do {
            val valueRead = value.get()
            if (value.get() != 1) return value.decrementAndGet()
            if (value.compareAndSet(valueRead, moduloValue - 1)) return moduloValue - 1
        } while (true)
    }
}