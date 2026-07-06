package top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.integration;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.springframework.boot.test.context.SpringBootTest;
import top.egon.cola.component.dtp.domain.IDynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.THREAD_POOL_EXECUTOR_01;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.THREAD_POOL_EXECUTOR_02;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.assertVirtualExecutorRegistered;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.changeMessage;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.requireSnapshot;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.resizeCommand;

/**
 * @ClassName: ThreadPoolIntegrationTest
 * @description: 线程池集成测试 - 需要 Redis 环境
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-03Month-31Day
 * @Version: 1.0
 */
@Slf4j
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DTP_REDIS_TESTS", matches = "true")
public class ThreadPoolIntegrationTest {

    @Resource
    private RTopic dynamicThreadPoolRedisTopic;

    @Resource
    private IDynamicThreadPoolService dynamicThreadPoolService;

    /**
     * 测试 1：查询执行器列表集成测试
     */
    @Test
    public void test_queryThreadPoolList_Integration() {
        log.info("========== 集成测试：查询执行器列表 ==========");

        List<ExecutorSnapshot> snapshots = dynamicThreadPoolService.queryExecutorSnapshots();

        assertThat(snapshots).isNotEmpty();
        assertVirtualExecutorRegistered(snapshots);
        log.info("查询到 {} 个执行器", snapshots.size());

        for (ExecutorSnapshot snapshot : snapshots) {
            log.info("执行器：{} - 类型：{}, 核心：{}, 最大：{}, 活跃：{}, 队列：{}, 并发上限：{}",
                    snapshot.getExecutorName(),
                    snapshot.getExecutorKind(),
                    snapshot.getCorePoolSize(),
                    snapshot.getMaximumPoolSize(),
                    snapshot.getActiveCount(),
                    snapshot.getQueueSize(),
                    snapshot.getConcurrencyLimit());

            assertThat(snapshot.getAppName()).isNotBlank();
            assertThat(snapshot.getInstanceId()).isNotBlank();
            assertThat(snapshot.getExecutorName()).isNotBlank();
            assertThat(snapshot.getExecutorKind()).isNotNull();
            if (snapshot.getExecutorKind() == ExecutorKind.VIRTUAL_THREAD_PER_TASK) {
                assertThat(snapshot.isVirtual()).isTrue();
                assertThat(snapshot.getConcurrencyLimit()).isPositive();
                assertThat(snapshot.getAvailablePermits()).isNotNegative();
            } else {
                assertThat(snapshot.getCorePoolSize()).isPositive();
                assertThat(snapshot.getMaximumPoolSize()).isGreaterThanOrEqualTo(snapshot.getCorePoolSize());
            }
        }
    }

    /**
     * 测试 2：动态调整线程池配置集成测试
     */
    @Test
    public void test_adjustThreadPoolConfig_Integration() throws InterruptedException {
        log.info("========== 集成测试：动态调整线程池配置 ==========");

        ExecutorSnapshot beforeConfig = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);
        log.info("调整前 - 核心线程数：{}, 最大线程数：{}",
                beforeConfig.getCorePoolSize(), beforeConfig.getMaximumPoolSize());

        try {
            ExecutorUpdateCommand newConfig = resizeCommand(beforeConfig,
                    beforeConfig.getCorePoolSize() + 5, beforeConfig.getMaximumPoolSize() + 10);

            log.info("发布新配置 - 核心线程数：{}, 最大线程数：{}",
                    newConfig.getCorePoolSize(), newConfig.getMaximumPoolSize());

            dynamicThreadPoolRedisTopic.publish(changeMessage(newConfig));

            Thread.sleep(2000);

            ExecutorSnapshot afterConfig = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);
            log.info("调整后 - 核心线程数：{}, 最大线程数：{}",
                    afterConfig.getCorePoolSize(), afterConfig.getMaximumPoolSize());

            assertThat(afterConfig.getCorePoolSize()).isEqualTo(newConfig.getCorePoolSize());
            assertThat(afterConfig.getMaximumPoolSize()).isEqualTo(newConfig.getMaximumPoolSize());
        } finally {
            ExecutorUpdateCommand restoreConfig = resizeCommand(beforeConfig,
                    beforeConfig.getCorePoolSize(), beforeConfig.getMaximumPoolSize());
            dynamicThreadPoolRedisTopic.publish(changeMessage(restoreConfig));
            Thread.sleep(1000);

            log.info("已恢复原始配置");
        }
    }

    /**
     * 测试 3：线程池状态实时监控集成测试
     */
    @Test
    public void test_threadPoolStatusMonitor_Integration() {
        log.info("========== 集成测试：线程池状态实时监控 ==========");

        ExecutorSnapshot initialConfig = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);
        log.info("初始状态 - 活跃线程：{}, 队列任务：{}, 剩余容量：{}",
                initialConfig.getActiveCount(),
                initialConfig.getQueueSize(),
                initialConfig.getRemainingCapacity());

        assertThat(initialConfig.getActiveCount()).isNotNegative();
        assertThat(initialConfig.getQueueSize()).isNotNegative();
        assertThat(initialConfig.getRemainingCapacity()).isNotNegative();
        assertThat(initialConfig.getQueueType()).isNotBlank();

        log.info("状态监控正常");
    }

    /**
     * 测试 4：配置变更监听器集成测试
     */
    @Test
    public void test_configAdjustListener_Integration() throws InterruptedException {
        log.info("========== 集成测试：配置变更监听器 ==========");

        ExecutorSnapshot originalConfig = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_02);
        log.info("原始配置 - 核心：{}, 最大：{}",
                originalConfig.getCorePoolSize(), originalConfig.getMaximumPoolSize());

        try {
            ExecutorUpdateCommand newConfig = resizeCommand(originalConfig, 25, 75);

            log.info("发布配置变更");
            dynamicThreadPoolRedisTopic.publish(changeMessage(newConfig));

            Thread.sleep(3000);

            ExecutorSnapshot updatedConfig = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_02);
            log.info("更新后配置 - 核心：{}, 最大：{}",
                    updatedConfig.getCorePoolSize(), updatedConfig.getMaximumPoolSize());

            assertThat(updatedConfig.getCorePoolSize()).isEqualTo(25);
            assertThat(updatedConfig.getMaximumPoolSize()).isEqualTo(75);
        } finally {
            ExecutorUpdateCommand restoreConfig = resizeCommand(originalConfig,
                    originalConfig.getCorePoolSize(), originalConfig.getMaximumPoolSize());
            dynamicThreadPoolRedisTopic.publish(changeMessage(restoreConfig));
            Thread.sleep(1000);
        }
    }

    /**
     * 测试 5：并发配置更新测试
     */
    @Test
    public void test_concurrentConfigUpdate_Integration() throws InterruptedException {
        log.info("========== 集成测试：并发配置更新 ==========");

        ExecutorSnapshot originalConfig = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);
        CountDownLatch latch = new CountDownLatch(3);
        AtomicReference<Throwable> childFailure = new AtomicReference<>();

        try {
            for (int i = 0; i < 3; i++) {
                final int index = i;
                new Thread(() -> {
                    try {
                        ExecutorUpdateCommand command = resizeCommand(originalConfig,
                                10 + index * 5, 50 + index * 10);

                        dynamicThreadPoolRedisTopic.publish(changeMessage(command));
                        log.info("发布配置 #{}: 核心={}, 最大={}",
                                index, command.getCorePoolSize(), command.getMaximumPoolSize());
                    } catch (Throwable e) {
                        childFailure.compareAndSet(null, e);
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(childFailure.get()).isNull();
            Thread.sleep(2000);

            ExecutorSnapshot finalConfig = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);
            log.info("最终配置 - 核心：{}, 最大：{}",
                    finalConfig.getCorePoolSize(), finalConfig.getMaximumPoolSize());

            assertThat(finalConfig).isNotNull();
        } finally {
            ExecutorUpdateCommand restoreConfig = resizeCommand(originalConfig,
                    originalConfig.getCorePoolSize(), originalConfig.getMaximumPoolSize());
            dynamicThreadPoolRedisTopic.publish(changeMessage(restoreConfig));
            Thread.sleep(1000);
        }

        log.info("并发更新处理完成");
    }
}
