package top.egon.cola.component.dtp.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoProperties;
import top.egon.cola.component.dtp.domain.DynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ThreadPoolCreateCommand;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;
import top.egon.cola.component.dtp.executor.ThreadManage;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

class DtpTaskMetricsTest {
    @Test
    void runtimePoolsBindCountersTimersAndCapabilitiesAndReleaseOnlyOwnedMeters() throws Exception {
        var registry = new ManagedExecutorRegistry("app", "one", new DynamicThreadPoolAutoProperties());
        var meters = new SimpleMeterRegistry();
        try (var validator = new LocalValidatorFactoryBean()) {
            validator.afterPropertiesSet();
            var validation = new ValidationUtils(validator);
            var manager = new ThreadManage(registry, new DynamicThreadPoolService(registry, validation), validation);
            var binder = new DtpMeterBinder(registry);
            var external = new AtomicInteger(7);
            Tags tags = Tags.of("appName", "app", "instanceId", "one", "executorName", "pool",
                    "executorKind", "SPRING_THREAD_POOL_TASK_EXECUTOR", "virtual", "false");
            Gauge externalGauge = Gauge.builder("dtp.executor.active", external, AtomicInteger::get).tags(tags).register(meters);
            binder.bindTo(meters); binder.bindTo(meters);
            var pool = manager.newThreadPool(new ThreadPoolCreateCommand().setExecutorName("pool").setCorePoolSize(1)
                    .setMaximumPoolSize(2).setQueueCapacity(16));
            try {
                pool.submit(() -> 1).get(2, TimeUnit.SECONDS);
                var failed = pool.submit((Callable<Integer>) () -> { throw new IllegalStateException("expected"); });
                assertThatThrownBy(() -> failed.get(2, TimeUnit.SECONDS)).hasCauseInstanceOf(IllegalStateException.class);
                await().untilAsserted(() -> {
                    assertThat(meters.get("dtp.task.succeeded").tags(tags).functionCounter().count()).isEqualTo(1);
                    assertThat(meters.get("dtp.task.failed").tags(tags).functionCounter().count()).isEqualTo(1);
                });
                assertThat(meters.get("dtp.task.execution").tags(tags).timer().count()).isEqualTo(2);
                assertThat(meters.get("dtp.task.queue.wait").tags(tags).timer().count()).isEqualTo(2);
                assertThat(meters.get("dtp.executor.queue.capacity").tags(tags).gauge().value()).isEqualTo(16);
                assertThat(meters.get("dtp.executor.queue.resizable").tags(tags).gauge().value()).isEqualTo(1);
                assertThat(meters.get("dtp.executor.observation.complete").tags(tags).gauge().value()).isEqualTo(1);
                assertThat(meters.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags())
                        .noneSatisfy(tag -> assertThat(tag.getKey()).isEqualTo("traceId")));
            } finally { pool.shutdown(); manager.destroy(); binder.destroy(); }
            assertThat(meters.find("dtp.task.execution").tags(tags).timer()).isNull();
            assertThat(meters.find("dtp.executor.active").tags(tags).gauge()).isSameAs(externalGauge);
        } finally { meters.close(); }
    }
    @Test
    void pressureAlarmsRequireConsecutiveSamplesAndRespectCooldown() {
        var service = org.mockito.Mockito.mock(top.egon.cola.component.dtp.domain.IDynamicThreadPoolService.class);
        var remote = org.mockito.Mockito.mock(top.egon.cola.component.dtp.registry.IRegistry.class);
        var snapshot = new top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot();
        snapshot.setAppName("app"); snapshot.setInstanceId("one"); snapshot.setExecutorName("pool");
        snapshot.setQueueSize(9); snapshot.setRemainingCapacity(1); snapshot.setRejectedTasks(0L);
        org.mockito.Mockito.when(service.queryExecutorSnapshots()).thenReturn(java.util.List.of(snapshot));
        var properties = new DynamicThreadPoolAutoProperties();
        properties.getObservation().getAlarm().setEnabled(true);
        properties.getObservation().getAlarm().setConsecutiveSamples(2);
        var job = new top.egon.cola.component.dtp.trigger.job.ThreadPoolDataReportJob(service, remote, properties);
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(job.getClass());
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start(); logger.addAppender(appender);
        try {
            job.execReportThreadPoolList();
            assertThat(appender.list.stream().filter(event -> event.getFormattedMessage().contains("压力告警"))).isEmpty();
            job.execReportThreadPoolList(); job.execReportThreadPoolList();
            assertThat(appender.list.stream().filter(event -> event.getFormattedMessage().contains("压力告警"))).hasSize(1);
            snapshot.setRejectedTasks(1L); job.execReportThreadPoolList();
            assertThat(appender.list.stream().filter(event -> event.getFormattedMessage().contains("压力告警"))).hasSize(1);
        } finally { logger.detachAppender(appender); appender.stop(); }
    }

}
