package top.egon.cola.component.common.id.snowflake;

import top.egon.cola.component.common.id.time.TimeSource;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe test clock whose wall time can be set or advanced explicitly.
 * Every read is counted so blocking tests can observe that a generator has
 * entered a clock-wait path before changing time or interrupting the caller.
 */
final class ControllableTimeSource implements TimeSource {

    private final AtomicLong currentTimeMillis;
    private final AtomicLong readCount = new AtomicLong();

    ControllableTimeSource(long currentTimeMillis) {
        this.currentTimeMillis = new AtomicLong(currentTimeMillis);
    }

    @Override
    public long currentTimeMillis() {
        long observedTimeMillis = currentTimeMillis.get();
        readCount.incrementAndGet();
        return observedTimeMillis;
    }

    void setCurrentTimeMillis(long currentTimeMillis) {
        this.currentTimeMillis.set(currentTimeMillis);
    }

    long advanceMillis(long millis) {
        if (millis < 0L) {
            throw new IllegalArgumentException("millis must not be negative: " + millis);
        }
        return currentTimeMillis.addAndGet(millis);
    }

    long readCount() {
        return readCount.get();
    }
}
