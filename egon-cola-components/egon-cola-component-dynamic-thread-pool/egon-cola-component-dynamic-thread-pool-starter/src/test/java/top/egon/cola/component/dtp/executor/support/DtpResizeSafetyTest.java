package top.egon.cola.component.dtp.executor.support;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoProperties;
import top.egon.cola.component.dtp.context.DtpTaskObserver;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.executor.adapter.ThreadPoolTaskExecutorManagedExecutor;
import top.egon.cola.component.dtp.executor.managed.DtpThreadPoolTaskExecutor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class DtpResizeSafetyTest {
    @Test
    void shrinkingBelowBacklogRejectsBeforeAnyThreadConfigurationChanges() throws Exception {
        var pool = new DtpThreadPoolTaskExecutor(new DtpTaskObserver("app", "one", "resize", new DynamicThreadPoolAutoProperties.Observation(), true));
        pool.setCorePoolSize(1); pool.setMaxPoolSize(1); pool.setQueueCapacity(4); pool.initialize();
        var adapter = new ThreadPoolTaskExecutorManagedExecutor("app", "one", "resize", pool);
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        Future<?> first = pool.submit(() -> { started.countDown(); release.await(); return null; });
        try {
            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
            var second = pool.submit(() -> { }); var third = pool.submit(() -> { });
            ExecutorUpdateCommand change = new ExecutorUpdateCommand();
            change.setCorePoolSize(2); change.setMaximumPoolSize(3); change.setQueueCapacity(1);
            assertThat(adapter.update(change).isSuccess()).isFalse();
            assertThat(pool.getCorePoolSize()).isEqualTo(1);
            assertThat(pool.getMaxPoolSize()).isEqualTo(1);
            assertThat(pool.getQueueCapacity()).isEqualTo(4);
            release.countDown();
            first.get(2, TimeUnit.SECONDS); second.get(2, TimeUnit.SECONDS); third.get(2, TimeUnit.SECONDS);
        } finally { release.countDown(); pool.shutdown(); }
    }

    @Test
    void fixedQueuesAndOutOfRangeKeepAliveNeverPartiallyResize() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(1); pool.setMaxPoolSize(2); pool.setQueueCapacity(4); pool.initialize();
        try {
            var adapter = new ThreadPoolTaskExecutorManagedExecutor("app", "one", "fixed", pool);
            ExecutorUpdateCommand change = new ExecutorUpdateCommand();
            change.setCorePoolSize(3); change.setMaximumPoolSize(4); change.setQueueCapacity(10);
            assertThat(adapter.update(change).isSuccess()).isFalse();
            assertThat(pool.getCorePoolSize()).isEqualTo(1);
            change.setQueueCapacity(null); change.setKeepAliveSeconds((long) Integer.MAX_VALUE + 1);
            assertThat(adapter.update(change).isSuccess()).isFalse();
            assertThat(pool.getMaxPoolSize()).isEqualTo(2);
            change.setKeepAliveSeconds(2L); change.setAllowCoreThreadTimeOut(true);
            assertThat(adapter.update(change).isSuccess()).isTrue();
            assertThat(pool.getThreadPoolExecutor().allowsCoreThreadTimeOut()).isTrue();
            assertThat(pool.getKeepAliveSeconds()).isEqualTo(2);
            assertThat(pool.getThreadPoolExecutor().getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(2);
        } finally { pool.shutdown(); }
    }

    @Test
    void historicalZeroCorePoolsCanStillUpdateOnlyKeepAlive() {
        try (var pool = new ThreadPoolExecutor(0, 4, 60, TimeUnit.SECONDS, new SynchronousQueue<>())) {
            ExecutorUpdateCommand change = new ExecutorUpdateCommand(); change.setKeepAliveSeconds(5L);
            ThreadPoolResizeSupport.update(pool, null, change);
            assertThat(pool.getCorePoolSize()).isZero();
            assertThat(pool.getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(5);
        }
    }
}
