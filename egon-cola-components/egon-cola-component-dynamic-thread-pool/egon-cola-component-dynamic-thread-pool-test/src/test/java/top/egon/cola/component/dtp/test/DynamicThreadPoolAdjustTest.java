package top.egon.cola.component.dtp.test;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.springframework.boot.test.context.SpringBootTest;
import top.egon.cola.component.dtp.domain.IDynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.THREAD_POOL_EXECUTOR_01;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.assertVirtualExecutorRegistered;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.changeMessage;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.requireSnapshot;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.resizeCommand;

/**
 * @ClassName: DynamicThreadPoolAdjustTest
 * @description: 动态线程池调整测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-03Month-31Day
 * @Version: 1.0
 */
@Slf4j
@SpringBootTest(classes = Application.class)
@EnabledIfEnvironmentVariable(named = "DTP_REDIS_TESTS", matches = "true")
public class DynamicThreadPoolAdjustTest {

    @Resource
    private RTopic dynamicThreadPoolRedisTopic;

    @Resource
    private IDynamicThreadPoolService dynamicThreadPoolService;

    /**
     * 测试 1：查询执行器列表
     */
    @Test
    public void test_queryThreadPoolList() {
        log.info("========== 测试 1：查询执行器列表 ==========");
        List<ExecutorSnapshot> snapshots = dynamicThreadPoolService.queryExecutorSnapshots();

        assertThat(snapshots).isNotEmpty();
        assertVirtualExecutorRegistered(snapshots);

        for (ExecutorSnapshot snapshot : snapshots) {
            log.info("执行器名称：{}, 类型：{}, 核心线程数：{}, 最大线程数：{}, 活跃线程数：{}, 队列大小：{}, 并发上限：{}",
                    snapshot.getExecutorName(),
                    snapshot.getExecutorKind(),
                    snapshot.getCorePoolSize(),
                    snapshot.getMaximumPoolSize(),
                    snapshot.getActiveCount(),
                    snapshot.getQueueSize(),
                    snapshot.getConcurrencyLimit());
        }

        log.info("执行器总数：{}", snapshots.size());
    }

    /**
     * 测试 2：根据名称查询执行器快照
     */
    @Test
    public void test_queryThreadPoolConfigByName() {
        log.info("========== 测试 2：根据名称查询执行器快照 ==========");
        ExecutorSnapshot snapshot = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);

        assertThat(snapshot.getAppName()).isNotBlank();
        assertThat(snapshot.getInstanceId()).isNotBlank();
        assertThat(snapshot.getCorePoolSize()).isPositive();
        assertThat(snapshot.getMaximumPoolSize()).isGreaterThanOrEqualTo(snapshot.getCorePoolSize());

        log.info("执行器名称：{}, 应用名：{}, 实例：{}, 核心线程数：{}, 最大线程数：{}",
                snapshot.getExecutorName(),
                snapshot.getAppName(),
                snapshot.getInstanceId(),
                snapshot.getCorePoolSize(),
                snapshot.getMaximumPoolSize());
    }

    /**
     * 测试 3：通过 Redis Topic 动态调整线程池配置
     */
    @Test
    public void test_adjustThreadPoolConfig() throws InterruptedException {
        log.info("========== 测试 3：动态调整线程池配置 ==========");

        ExecutorSnapshot beforeConfig = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);
        log.info("调整前 - 核心线程数：{}, 最大线程数：{}",
                beforeConfig.getCorePoolSize(), beforeConfig.getMaximumPoolSize());

        try {
            ExecutorUpdateCommand newConfig = resizeCommand(beforeConfig, 30, 80);

            log.info("发布新配置 - 核心线程数：{}, 最大线程数：{}",
                    newConfig.getCorePoolSize(), newConfig.getMaximumPoolSize());
            dynamicThreadPoolRedisTopic.publish(changeMessage(newConfig));

            Thread.sleep(2000);

            ExecutorSnapshot afterConfig = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);
            log.info("调整后 - 核心线程数：{}, 最大线程数：{}",
                    afterConfig.getCorePoolSize(), afterConfig.getMaximumPoolSize());

            assertThat(afterConfig.getCorePoolSize()).isEqualTo(30);
            assertThat(afterConfig.getMaximumPoolSize()).isEqualTo(80);
        } finally {
            ExecutorUpdateCommand restoreConfig = resizeCommand(beforeConfig,
                    beforeConfig.getCorePoolSize(), beforeConfig.getMaximumPoolSize());
            dynamicThreadPoolRedisTopic.publish(changeMessage(restoreConfig));
            Thread.sleep(1000);
        }
    }

    /**
     * 测试 4：查询不存在的执行器
     */
    @Test
    public void test_queryNonExistentThreadPool() {
        log.info("========== 测试 4：查询不存在的执行器 ==========");
        ExecutorSnapshot snapshot = dynamicThreadPoolService.queryExecutorSnapshot("nonExistentPool");

        assertThat(snapshot).isNull();
        log.info("查询不存在的执行器返回 null");
    }

}
