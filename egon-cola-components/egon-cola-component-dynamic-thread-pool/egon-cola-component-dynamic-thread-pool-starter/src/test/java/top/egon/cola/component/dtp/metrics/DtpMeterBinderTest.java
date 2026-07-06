package top.egon.cola.component.dtp.metrics;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.executor.ManagedExecutor;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;
import top.egon.cola.component.dtp.executor.adapter.BoundedVirtualThreadManagedExecutor;
import top.egon.cola.component.dtp.executor.adapter.ThreadPoolExecutorManagedExecutor;
import top.egon.cola.component.dtp.executor.virtual.BoundedVirtualThreadExecutor;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @ClassName: DtpMeterBinderTest
 * @description: 动态线程池 Micrometer 指标绑定测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-06Month-30Day
 * @Version: 1.0
 */
public class DtpMeterBinderTest {

    @Test
    public void test_shouldRegisterPlatformExecutorMeters() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 8,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100)
        );
        ManagedExecutorRegistry registry = new ManagedExecutorRegistry(List.of(
                new ThreadPoolExecutorManagedExecutor("app", "instance", "orderExecutor", executor)
        ));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        new DtpMeterBinder(registry).bindTo(meterRegistry);

        Tags tags = Tags.of(
                "appName", "app",
                "instanceId", "instance",
                "executorName", "orderExecutor",
                "executorKind", "PLATFORM_THREAD_POOL",
                "virtual", "false"
        );
        assertThat(meterRegistry.find("dtp.executor.pool.core").tags(tags).gauge()).isNotNull();
        assertThat(meterRegistry.find("dtp.executor.pool.max").tags(tags).gauge()).isNotNull();
        assertThat(meterRegistry.find("dtp.executor.queue.size").tags(tags).gauge()).isNotNull();
        assertThat(meterRegistry.find("dtp.executor.queue.remaining").tags(tags).gauge()).isNotNull();
        assertThat(meterRegistry.find("dtp.executor.active").tags(tags).gauge()).isNotNull();
        assertThat(meterRegistry.find("dtp.executor.completed").tags(tags).gauge()).isNotNull();
        assertThat(meterRegistry.find("dtp.executor.rejected").tags(tags).gauge()).isNotNull();
        assertThat(meterRegistry.get("dtp.executor.pool.core").tags(tags).gauge().value()).isEqualTo(2D);
        assertThat(meterRegistry.get("dtp.executor.pool.max").tags(tags).gauge().value()).isEqualTo(8D);
        assertThat(meterRegistry.get("dtp.executor.queue.size").tags(tags).gauge().value()).isEqualTo(0D);
    }

    @Test
    public void test_shouldRegisterVirtualExecutorMetersWithoutTraditionalPoolMeters() {
        BoundedVirtualThreadExecutor executor = new BoundedVirtualThreadExecutor("dtp-virtual", 3);
        try {
            ManagedExecutorRegistry registry = new ManagedExecutorRegistry(List.of(
                    new BoundedVirtualThreadManagedExecutor("app", "instance", "virtualExecutor", executor)
            ));
            SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

            new DtpMeterBinder(registry).bindTo(meterRegistry);

            Tags tags = Tags.of(
                    "appName", "app",
                    "instanceId", "instance",
                    "executorName", "virtualExecutor",
                    "executorKind", "VIRTUAL_THREAD_PER_TASK",
                    "virtual", "true"
            );
            assertThat(meterRegistry.find("dtp.executor.virtual.running").tags(tags).gauge()).isNotNull();
            assertThat(meterRegistry.find("dtp.executor.virtual.submitted").tags(tags).gauge()).isNotNull();
            assertThat(meterRegistry.find("dtp.executor.virtual.completed").tags(tags).gauge()).isNotNull();
            assertThat(meterRegistry.find("dtp.executor.virtual.failed").tags(tags).gauge()).isNotNull();
            assertThat(meterRegistry.find("dtp.executor.virtual.permits.available").tags(tags).gauge()).isNotNull();
            assertThat(meterRegistry.find("dtp.executor.rejected").tags(tags).gauge()).isNotNull();
            assertThat(meterRegistry.find("dtp.executor.pool.core").tags(tags).gauge()).isNull();
            assertThat(meterRegistry.find("dtp.executor.pool.max").tags(tags).gauge()).isNull();
            assertThat(meterRegistry.find("dtp.executor.queue.size").tags(tags).gauge()).isNull();
            assertThat(meterRegistry.find("dtp.executor.virtual.permits.available").tags(tags).gauge().value()).isEqualTo(3D);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void test_shouldTolerateNullableSnapshotFields() {
        ManagedExecutorRegistry registry = new ManagedExecutorRegistry(List.of(new NullableSnapshotManagedExecutor()));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        new DtpMeterBinder(registry).bindTo(meterRegistry);

        Tags tags = Tags.of(
                "appName", "",
                "instanceId", "",
                "executorName", "",
                "executorKind", "",
                "virtual", "false"
        );
        assertThat(meterRegistry.find("dtp.executor.pool.core").tags(tags).gauge()).isNotNull();
        assertThat(meterRegistry.find("dtp.executor.pool.core").tags(tags).gauge().value()).isEqualTo(0D);
        assertThat(meterRegistry.find("dtp.executor.completed").tags(tags).gauge().value()).isEqualTo(0D);
    }

    private static class NullableSnapshotManagedExecutor implements ManagedExecutor {

        @Override
        public String appName() {
            return null;
        }

        @Override
        public String instanceId() {
            return null;
        }

        @Override
        public String executorName() {
            return "nullableExecutor";
        }

        @Override
        public ExecutorKind kind() {
            return null;
        }

        @Override
        public ExecutorSnapshot snapshot() {
            return new ExecutorSnapshot();
        }

        @Override
        public UpdateResult update(ExecutorUpdateCommand command) {
            return null;
        }

        @Override
        public boolean supportsResize() {
            return false;
        }

        @Override
        public boolean supportsVirtualThread() {
            return false;
        }

        @Override
        public boolean supportsQueueMetrics() {
            return false;
        }

    }

}
