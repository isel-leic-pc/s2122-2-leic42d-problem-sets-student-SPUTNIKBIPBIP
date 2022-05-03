package test
import mu.KLogger
import mu.KotlinLogging
import org.junit.Assert.*
import org.junit.Test
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


private val logger : KLogger? = KotlinLogging.logger {}

