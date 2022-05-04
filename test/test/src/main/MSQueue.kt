package main

import java.util.concurrent.atomic.AtomicReference

class MSQueue<T>() {

    private class Node<T> (var value : T? = null) {
        var next = AtomicReference<Node<T>>()
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

    //TODO: should i ignore the dummy node ?
    /**
     * This function goes in the head of the queue, and removes the node at first
     * position retrieving the associated value
     * returns @T value returned from the node taken, may be null in case of the queue is empty
     */
    fun poll() : T ? {
        while(true) {
            val dummy = head.get()
            val obsFirstElem = dummy.next.get()
            //if next is null, then queue is empty, in this case dummy doesn't have next node
            val next = obsFirstElem.next.get() ?: return null
            if (obsFirstElem == dummy.next.get()) {
                if (dummy.next.compareAndSet(obsFirstElem, next)) {
                    /*
                    TODO:
                    should i check if another thread has already removed the node, should i worry about it since
                    the cas operation is atomic ?
                     */
                    val item = obsFirstElem.value
                    //delete node from queue
                    next.value = null
                    return item
                }
            }
        }
        /* another implementation, ignoring the dummy node will lead head and tail to point to null when queue is empty
           while(true) {
            val currentHead = head.get()
            //if next is null, then queue is empty
            val next = currentHead.next.get() ?: return null
            if (currentHead == head.get()) {
                if (head.compareAndSet(currentHead, next)) {
                    val item = next.value
                    //delete node from queue
                    next.value = null
                    return item
                }
            }
         }
        }
        */
    }

}