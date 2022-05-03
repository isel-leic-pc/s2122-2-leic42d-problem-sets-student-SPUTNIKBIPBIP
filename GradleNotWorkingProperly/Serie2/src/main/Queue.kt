package main

import java.util.concurrent.atomic.AtomicReference

class Queue<T>() {


    private class Node<T> (var value : T? = null) {
        val next = AtomicReference<Node<T>?>()
    }

    private val head : AtomicReference<Node<T>>
    private val tail : AtomicReference<Node<T>>

    init {
        val dummy = Node<T>()
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    fun put(elem : T) {
        val newNode = Node(elem)
        do {
            val obsTail = tail.get()
            val obsTailNext = obsTail.next.get()
            if (obsTail == tail.get()) {    // just check that obsTail and obsTailNext are consistent
                if (obsTailNext == null) {  // the quiescent (stable) state
                    if (obsTail.next.compareAndSet(obsTailNext, newNode)) { // try do the insertion
                        tail.compareAndSet(obsTail, newNode) // and adjust tail
                        return
                    }
                } else {
                    tail.compareAndSet(obsTail, obsTailNext) // try adjust tail
                }
            }
        }
        while(true)
    }

    fun poll() : T? {
        while(true) {
            val currentHead = head.get()
            val currentTail = tail.get()
            val next = currentHead.next.get()
            if (currentHead == head.get()) {
                if (currentHead == currentTail) {
                    if (next == null) {
                        return null
                    } else {
                        //and insertion was made, it's necessary to update the tail
                        tail.compareAndSet(currentTail, next)
                    }
                }
            } else {
                if (head.compareAndSet(currentHead, next)) {
                    val item = next?.value
                    next?.value = null
                    return item!!
                }
            }
        }
    }

}