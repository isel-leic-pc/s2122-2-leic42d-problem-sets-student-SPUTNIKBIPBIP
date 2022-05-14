package Serie2

import java.util.concurrent.atomic.AtomicReference

class SafeMessageBox<M> {

    private class Holder<M>(val msg: M, initialLives: Int) {
        var lives: Int = initialLives
    }
    private var holder = AtomicReference<Holder<M>>()

    fun publish(msg: M, lives: Int) {
        holder = AtomicReference(Holder(msg, lives))
    }

    fun tryConsume(): M? {
        do {
            val observedValue = holder.get()
            if (observedValue == null || observedValue.lives == 0) {
                return null
            } else {
                val newValue = Holder(observedValue.msg, observedValue.lives - 1)
                if (holder.compareAndSet(observedValue, newValue)) {
                    observedValue.msg
                }
            }
        } while (true)
    }
}