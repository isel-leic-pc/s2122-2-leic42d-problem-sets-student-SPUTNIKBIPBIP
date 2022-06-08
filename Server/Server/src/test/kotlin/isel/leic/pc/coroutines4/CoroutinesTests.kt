package isel.leic.pc.coroutines4

import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class CoroutinesTests {

    @Test
    fun `check if job returned from builders is the same that is retrieved from coroutine context`() {
        runBlocking {
            var ctxJob : Job? = null
            val job = launch {
                ctxJob = coroutineContext.job
            }

            job.join()
            println("job= $job")
            println("ctxJob= $ctxJob")
            assertEquals(ctxJob, job)
        }
    }
}