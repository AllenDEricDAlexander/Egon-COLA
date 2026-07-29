package top.egon.cola.component.accessguard.store.local;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStateCleanerTest {

    @Test
    void deterministicCleanupAndCloseReleaseTheOwnedScheduler() {
        AtomicInteger cleanups = new AtomicInteger();
        LocalStateCleaner cleaner = new LocalStateCleaner(
                "access-guard-cleaner-test",
                Duration.ofHours(1),
                List.of(cleanups::incrementAndGet));

        cleaner.cleanNow();
        cleaner.close();

        assertThat(cleanups).hasValue(1);
        assertThat(cleaner.isClosed()).isTrue();
        assertThat(Thread.getAllStackTraces().keySet())
                .noneMatch(thread -> thread.isAlive() && thread.getName().equals("access-guard-cleaner-test"));
    }
}
