package Serie2

import java.util.concurrent.atomic.AtomicInteger

class CounterModulo(private val moduloValue: Int) {

    val value = AtomicInteger(0)

    fun increment(): Int {
        do {
            val valueRead = value.get()
            val nextVal = if (valueRead == moduloValue - 1) 0 else valueRead + 1
            if (value.compareAndSet(valueRead, nextVal))
                return nextVal
        } while (true)
    }

    fun decrement(): Int {
        do {
            val valueRead = value.get()
            val nextVal = if (valueRead == 0) moduloValue - 1 else valueRead - 1
            if (value.compareAndSet(valueRead, nextVal)) {
                return nextVal
            }
        } while (true)
    }
}