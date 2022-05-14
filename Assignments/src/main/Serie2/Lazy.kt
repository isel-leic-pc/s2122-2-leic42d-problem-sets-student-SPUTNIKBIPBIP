package Serie2

import java.util.concurrent.atomic.AtomicReference

class Lazy<T>(private val initializer: () -> T) {

    @Volatile
    private var counter = -1
    private var value = AtomicReference<T>()


    fun get() : T {
        val obserValue = value.get()
        if (obserValue == null) {
            //flag that just lets the first thread to run the initializer
            if (counter == -1) {
                counter = 0
                if (value.compareAndSet(obserValue, initializer()))
                    return value.get()
            } else {
                Thread.yield()
            }
        }
        return value.get()
    }
}
