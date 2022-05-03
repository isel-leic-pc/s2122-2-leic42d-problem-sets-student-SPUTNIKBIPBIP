package main

import java.util.concurrent.atomic.AtomicReference

class UnsafeMessageBox<M> {

    private class Holder<M>(val msg: M, initialLives: Int) {
        var lives: Int = initialLives
    }
    private var holder = AtomicReference<Holder<M>>()

    fun publish(msg: M, lives: Int) {
        holder = AtomicReference(Holder(msg, lives))
    }

    fun tryConsume(): M? =
        val observedValue = holder.get()
        if (observedValue.lives > 0) {
            val newValue = Holder(observedValue.msg, observedValue.lives - 1)
            if (holder.compareAndSet(observedValue, newValue)) {
                observedValue.msg
            }
            null
        } else {
            null
        }
}