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

    //TODO: needs some urgent refactory
    fun tryConsume(): M? =
        if (holder.get().lives > 0) {
            val observedValue = holder.get()
            val newValue = Holder(observedValue.msg, observedValue.lives - 1)
            if (holder.compareAndSet(observedValue, newValue)) {
                observedValue.msg
            }
            null
        } else {
            null
        }
}