package main

class CounterModulo<T> {

    class ExchangePair<T> (var thread : T ? = null, var value: T ? = null, var isExchanged : Condition ? = null)

    private val pairs = NodeList<ExchangePair<T>>()
    private val monitor = ReentrantLock()
    private val condition: Condition = monitor.newCondition()
    private var tvalue : T ? = null

    /**
     * @value thread wich will receive the value
     * @timeout time limit for the transaction to process
     * returns null if transaction time has expired
     */
    @Throws(InterruptedException::class)
    fun exchange(value: T, timeout: Duration): T? {
        monitor.withLock {
            if (value == null)
                throw InvalidParameterException()
            //fast path
            if (pairs.isEmpty) {
                //how to do the exchange ?!
                val v : T? = tvalue
                val thread : Thread = value as Thread
                thread {
                    tvalue = v
                }
            }
            if (timeout.isZero)
                return null
            val dueTime = timeout.dueTime()
            val pair = ExchangePair(value, tvalue, condition)
            pairs.add(pair)
            try {
                while (tvalue == null)
                    pair.isExchanged?.await(dueTime)
                if (dueTime.isPast) {
                    return null
                }
                tvalue
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
