package top.egon.cola.component.dtp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import top.egon.cola.component.dtp.domain.IDynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;
import top.egon.cola.component.dtp.metrics.DtpMeterBinder;
import top.egon.cola.component.dtp.registry.IRegistry;
import top.egon.cola.component.dtp.trigger.job.ThreadPoolDataReportJob;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @description 动态线程池自动配置单元测试
 */
public class DynamicThreadPoolAutoConfigTest {

    private final ApplicationContextRunner metricsContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DynamicThreadPoolAutoConfig.DtpMetricsConfiguration.class))
            .withUserConfiguration(ManagedExecutorRegistryConfiguration.class);

    private final ApplicationContextRunner reportContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DynamicThreadPoolAutoConfig.DtpReportConfiguration.class))
            .withBean(IDynamicThreadPoolService.class, DynamicThreadPoolAutoConfigTest::dynamicThreadPoolService)
            .withBean(IRegistry.class, () -> mock(IRegistry.class));

    @Test
    public void test_createRedisObjectMapper_serializeInstant() throws Exception {
        ObjectMapper objectMapper = DynamicThreadPoolAutoConfig.createRedisObjectMapper();

        ExecutorSnapshot snapshot = new ExecutorSnapshot();
        snapshot.setAppName("test-app");
        snapshot.setInstanceId("i-001");
        snapshot.setExecutorName("executor01");
        snapshot.setReportTime(Instant.parse("2026-06-29T10:15:30Z"));

        String json = objectMapper.writeValueAsString(snapshot);

        assertTrue(json.contains("reportTime"));
    }

    @Test
    public void test_dtpMeterBinderShouldNotCreateWithoutMeterRegistry() {
        metricsContextRunner.run(context ->
                assertThat(context).doesNotHaveBean(DtpMeterBinder.class)
        );
    }

    @Test
    public void test_dtpMeterBinderShouldCreateWithMeterRegistry() {
        metricsContextRunner.withUserConfiguration(MeterRegistryConfiguration.class)
                .run(context ->
                        assertThat(context).hasSingleBean(DtpMeterBinder.class)
                );
    }

    @Test
    public void test_threadPoolDataReportJobShouldNotCreateWhenReportDisabled() {
        reportContextRunner
                .withPropertyValues("egon.cola.component.dtp.report.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ThreadPoolDataReportJob.class);
                    assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
                });
    }

    @Test
    public void test_threadPoolDataReportJobShouldCreateWhenReportEnabled() {
        reportContextRunner
                .withPropertyValues("egon.cola.component.dtp.report.enabled=true")
                .run(context ->
                        assertThat(context).hasSingleBean(ThreadPoolDataReportJob.class)
                );
    }

    @Test
    public void test_reportSchedulingProcessorShouldCreateWhenReportEnabled() {
        reportContextRunner
                .withPropertyValues("egon.cola.component.dtp.report.enabled=true")
                .run(context ->
                        assertThat(context).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class)
                );
    }

    private static IDynamicThreadPoolService dynamicThreadPoolService() {
        IDynamicThreadPoolService dynamicThreadPoolService = mock(IDynamicThreadPoolService.class);
        when(dynamicThreadPoolService.queryExecutorSnapshots()).thenReturn(List.of());
        return dynamicThreadPoolService;
    }

    static class ManagedExecutorRegistryConfiguration {

        @Bean
        public ManagedExecutorRegistry managedExecutorRegistry() {
            return new ManagedExecutorRegistry(List.of());
        }

    }

    static class MeterRegistryConfiguration {

        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }

    }

}
