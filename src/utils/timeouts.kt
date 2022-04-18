package utils

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

inline val Duration.isZero : Boolean
    get() = this.inWholeNanoseconds == 0L

inline fun Duration.dueTime() =
    (System.currentTimeMillis().toDuration(DurationUnit.MILLISECONDS)  + this)
        .toLong(DurationUnit.MILLISECONDS)

inline val Long.remaining : Long
    get() = if(this == Long.MAX_VALUE) Long.MAX_VALUE
    else Math.max(0, this - System.currentTimeMillis())

inline fun Condition.await(dueTime : Long) {
    this.await(dueTime.remaining, TimeUnit.MILLISECONDS)
}

inline val Long.isPast : Boolean
    get() =  this < System.currentTimeMillis()


class MutableTimeout(var duration: Duration) {
    val elapsed : Boolean
        get() = nanos <= 0

    var nanos : Long
        get() = duration.inWholeNanoseconds
        set(value) {
            duration = value.toDuration(DurationUnit.NANOSECONDS)
        }
}

fun Condition.await(mutableTimeout: MutableTimeout) {
    if (mutableTimeout.duration.isInfinite())
        await()
    else {
        awaitNanos(mutableTimeout.nanos).also {
            mutableTimeout.nanos = it
        }
    }
}


// added to support convertion from java TimeUnits
fun Long.toDuration( unit : TimeUnit) : Duration {
    return unit.toMillis(this)
        .toDuration(DurationUnit.MILLISECONDS)
}