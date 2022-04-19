package main

import utils.await
import utils.dueTime
import utils.isPast
import utils.isZero
import java.util.LinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class Exchanger<T> {

    class ExchangePair<T> (var thread : Thread ? = null, var value: T ? = null, val isDone: Condition)

    private val list = LinkedList<ExchangePair<T>>()
    private val mutex = ReentrantLock()
    private val isExchanged: Condition = mutex.newCondition()

    /**
     * @value thread wich will receive the value
     * @timeout time limit for the transaction to process
     * returns null if transaction time has expired
     */
    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): T? {
        mutex.withLock {
            //fast path
            if (list.isEmpty()) {
                val thread = value as Thread

            }
            if (timeout.isZero)
                return null
            val dueTime = timeout.dueTime()
            val exchangePair = list.removeFirst()
            try {
                //while value
                while (value == null)
                    isExchanged.await(dueTime)
                if (dueTime.isPast) {
                    return null
                }
                value
            }
            catch(e: InterruptedException) {
                //set aggain the flag
                Thread.interrupted()

                throw e
            }
            //to delete after
            return null
        }
    }
}