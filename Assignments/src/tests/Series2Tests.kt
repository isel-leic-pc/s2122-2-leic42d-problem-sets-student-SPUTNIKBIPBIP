import Serie2.CounterModulo
import org.junit.Assert.assertEquals
import org.junit.Test


class Series2Tests {

    @Test
    fun `counter increment test`() {
        val nThreads = 6
        val expectedResult = 0
        val counter = CounterModulo(5)

        val threads = mutableListOf<Thread>()

        for (i in 0 .. nThreads) {
            val thread = Thread {
                repeat(2000) {
                    counter.increment()
                }
            }
            threads.add(thread)
        }


        threads.forEach { it.start()}
        threads.forEach { it.join()}

        assertEquals(expectedResult, counter.value.get())
    }

    @Test
    fun `counter increment and decrement to 0 test`() {
        val nThreads = 6
        val expectedResult = 0
        val counter = CounterModulo(5)


        val incThreads = mutableListOf<Thread>()
        val decThreads = mutableListOf<Thread>()

        for (i in 0 .. nThreads) {
            val thread = Thread {
                repeat(2000) {
                    counter.increment()
                }
            }
            incThreads.add(thread)
        }

        for (i in 0 .. nThreads) {
            val thread = Thread {
                repeat(2000) {
                    counter.decrement()
                }
            }
            decThreads.add(thread)
        }

        incThreads.forEach { it.start() }
        decThreads.forEach { it.start() }

        incThreads.forEach { it.join()}
        decThreads.forEach { it.join()}

        assertEquals(expectedResult, counter.value.get())
    }

}
