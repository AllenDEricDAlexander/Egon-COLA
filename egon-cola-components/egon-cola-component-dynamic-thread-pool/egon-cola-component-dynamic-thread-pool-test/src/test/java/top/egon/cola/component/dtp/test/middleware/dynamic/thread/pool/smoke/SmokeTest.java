package top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.smoke;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.springframework.boot.test.context.SpringBootTest;
import top.egon.cola.component.dtp.domain.IDynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.THREAD_POOL_EXECUTOR_01;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.assertVirtualExecutorRegistered;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.changeMessage;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.requireSnapshot;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.resizeCommand;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.updateCommand;

/**
 * @ClassName: SmokeTest
 * @description: 冒烟测试 - 端到端测试
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-03Month-31Day
 * @Version: 1.0
 */
@Slf4j
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DTP_REDIS_TESTS", matches = "true")
public class SmokeTest {

    @Resource
    private RTopic dynamicThreadPoolRedisTopic;

    @Resource
    private IDynamicThreadPoolService dynamicThreadPoolService;

    /**
     * 冒烟测试 1：系统启动后基本功能验证
     */
    @Test
    public void smoke_SystemStartup_BasicFunctionality() {
        log.info("========== 冒烟测试 1：系统启动后基本功能验证 ==========");

        assertThat(dynamicThreadPoolService).isNotNull();
        assertThat(dynamicThreadPoolRedisTopic).isNotNull();

        List<ExecutorSnapshot> snapshots = dynamicThreadPoolService.queryExecutorSnapshots();
        assertThat(snapshots).isNotEmpty();
        assertVirtualExecutorRegistered(snapshots);

        log.info("系统启动正常，基本功能可用");
    }

    /**
     * 冒烟测试 2：线程池配置查询功能验证
     */
    @Test
    public void smoke_QueryConfig_BasicFunctionality() {
        log.info("========== 冒烟测试 2：线程池配置查询功能验证 ==========");

        ExecutorSnapshot snapshot = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);

        assertThat(snapshot.getAppName()).isNotBlank();
        assertThat(snapshot.getInstanceId()).isNotBlank();
        assertThat(snapshot.getExecutorName()).isEqualTo(THREAD_POOL_EXECUTOR_01);
        assertThat(snapshot.getCorePoolSize()).isPositive();
        assertThat(snapshot.getMaximumPoolSize()).isGreaterThanOrEqualTo(snapshot.getCorePoolSize());

        log.info("查询结果 - 应用：{}, 实例：{}, 执行器：{}, 核心：{}, 最大：{}",
                snapshot.getAppName(), snapshot.getInstanceId(), snapshot.getExecutorName(),
                snapshot.getCorePoolSize(), snapshot.getMaximumPoolSize());

        log.info("配置查询功能正常");
    }

    /**
     * 冒烟测试 3：动态调整功能验证
     */
    @Test
    public void smoke_DynamicAdjust_BasicFunctionality() throws InterruptedException {
        log.info("========== 冒烟测试 3：动态调整功能验证 ==========");

        ExecutorSnapshot originalConfig = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);
        int originalCoreSize = originalConfig.getCorePoolSize();
        log.info("原始配置 - 核心线程数：{}", originalCoreSize);

        try {
            int newCoreSize = originalCoreSize + 1;
            ExecutorUpdateCommand newConfig = resizeCommand(originalConfig,
                    newCoreSize, originalConfig.getMaximumPoolSize());

            log.info("发布新配置 - 核心线程数：{}", newCoreSize);
            dynamicThreadPoolRedisTopic.publish(changeMessage(newConfig));

            Thread.sleep(2000);

            ExecutorSnapshot updatedConfig = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);
            log.info("更新后配置 - 核心线程数：{}", updatedConfig.getCorePoolSize());

            assertThat(updatedConfig.getCorePoolSize()).isEqualTo(newCoreSize);
        } finally {
            ExecutorUpdateCommand restoreConfig = resizeCommand(originalConfig,
                    originalConfig.getCorePoolSize(), originalConfig.getMaximumPoolSize());
            dynamicThreadPoolRedisTopic.publish(changeMessage(restoreConfig));
            Thread.sleep(1000);

            log.info("已恢复原始配置");
        }
    }

    /**
     * 冒烟测试 4：Redis 通信验证
     */
    @Test
    public void smoke_RedisCommunication_BasicFunctionality() throws InterruptedException {
        log.info("========== 冒烟测试 4：Redis 通信验证 ==========");

        ExecutorSnapshot originalConfig = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);
        ExecutorUpdateCommand command = resizeCommand(originalConfig, 10, 50);

        try {
            long subscribers = dynamicThreadPoolRedisTopic.publish(changeMessage(command));
            log.info("消息已发布，订阅者数量：{}", subscribers);

            Thread.sleep(1000);
            List<ExecutorSnapshot> snapshots = dynamicThreadPoolService.queryExecutorSnapshots();
            assertThat(snapshots).isNotEmpty();
        } finally {
            ExecutorUpdateCommand restoreCommand = resizeCommand(originalConfig,
                    originalConfig.getCorePoolSize(), originalConfig.getMaximumPoolSize());
            dynamicThreadPoolRedisTopic.publish(changeMessage(restoreCommand));
            Thread.sleep(1000);
        }

        log.info("Redis 通信正常");
    }

    /**
     * 冒烟测试 5：数据完整性验证
     */
    @Test
    public void smoke_DataIntegrity_BasicFunctionality() {
        log.info("========== 冒烟测试 5：数据完整性验证 ==========");

        List<ExecutorSnapshot> snapshots = dynamicThreadPoolService.queryExecutorSnapshots();
        assertVirtualExecutorRegistered(snapshots);

        for (ExecutorSnapshot snapshot : snapshots) {
            assertThat(snapshot.getAppName()).isNotBlank();
            assertThat(snapshot.getInstanceId()).isNotBlank();
            assertThat(snapshot.getExecutorName()).isNotBlank();
            assertThat(snapshot.getExecutorKind()).isNotNull();
            assertThat(snapshot.getReportTime()).isNotNull();

            if (snapshot.getExecutorKind() == ExecutorKind.VIRTUAL_THREAD_PER_TASK) {
                assertThat(snapshot.isVirtual()).isTrue();
                assertThat(snapshot.getConcurrencyLimit()).isPositive();
                assertThat(snapshot.getSubmittedTasks()).isNotNegative();
                assertThat(snapshot.getRunningTasks()).isNotNegative();
                assertThat(snapshot.getFailedTasks()).isNotNegative();
                assertThat(snapshot.getRejectedTasks()).isNotNegative();
                assertThat(snapshot.getAvailablePermits()).isNotNegative();
            } else {
                assertThat(snapshot.getCorePoolSize()).isPositive();
                assertThat(snapshot.getMaximumPoolSize()).isGreaterThanOrEqualTo(snapshot.getCorePoolSize());
                assertThat(snapshot.getActiveCount()).isNotNegative();
                assertThat(snapshot.getPoolSize()).isNotNegative();
                assertThat(snapshot.getQueueSize()).isNotNegative();
                assertThat(snapshot.getRemainingCapacity()).isNotNegative();
                assertThat(snapshot.getQueueType()).isNotBlank();
            }

            log.info("执行器 {} 数据完整", snapshot.getExecutorName());
        }

        log.info("数据完整性验证通过");
    }

    /**
     * 冒烟测试 6：异常处理验证
     */
    @Test
    public void smoke_ExceptionHandling_BasicFunctionality() {
        log.info("========== 冒烟测试 6：异常处理验证 ==========");

        ExecutorSnapshot nonExistent = dynamicThreadPoolService.queryExecutorSnapshot("nonExistentPool");
        assertThat(nonExistent).isNull();
        log.info("查询不存在的执行器 - 返回 null，未抛出异常");

        UpdateResult nullUpdateResult = dynamicThreadPoolService.updateExecutor(null);
        assertThat(nullUpdateResult.isSuccess()).isFalse();
        assertThat(nullUpdateResult.getMessage()).isEqualTo("executorName must not be blank");
        log.info("更新 null 配置 - 返回失败结果，未抛出异常");

        ExecutorSnapshot snapshot = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);
        ExecutorUpdateCommand wrongAppCommand = updateCommand(snapshot);
        wrongAppCommand.setAppName("wrong-app");
        UpdateResult wrongAppResult = dynamicThreadPoolService.updateExecutor(wrongAppCommand);
        assertThat(wrongAppResult.isSuccess()).isFalse();
        assertThat(wrongAppResult.getMessage()).contains("appName mismatch");
        log.info("更新错误应用名的配置 - 返回失败结果，未抛出异常");

        log.info("异常处理正常");
    }

    /**
     * 冒烟测试 7：端到端流程验证
     */
    @Test
    public void smoke_EndToEnd_BasicFunctionality() throws InterruptedException {
        log.info("========== 冒烟测试 7：端到端流程验证 ==========");

        ExecutorSnapshot config1 = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);
        log.info("步骤 1 - 查询配置：核心={}", config1.getCorePoolSize());

        ExecutorUpdateCommand newConfig = resizeCommand(config1,
                config1.getCorePoolSize() + 5, config1.getMaximumPoolSize());

        try {
            log.info("步骤 2 - 发布配置更新");
            dynamicThreadPoolRedisTopic.publish(changeMessage(newConfig));

            Thread.sleep(2000);

            ExecutorSnapshot config2 = dynamicThreadPoolService.queryExecutorSnapshots().stream()
                    .filter(snapshot -> THREAD_POOL_EXECUTOR_01.equals(snapshot.getExecutorName()))
                    .findFirst()
                    .orElse(null);

            assertThat(config2).isNotNull();
            assertThat(config2.getCorePoolSize()).isEqualTo(newConfig.getCorePoolSize());
            log.info("步骤 3 - 验证配置：核心={}", config2.getCorePoolSize());
        } finally {
            ExecutorUpdateCommand restoreConfig = resizeCommand(config1,
                    config1.getCorePoolSize(), config1.getMaximumPoolSize());
            dynamicThreadPoolRedisTopic.publish(changeMessage(restoreConfig));
            Thread.sleep(1000);
        }

        log.info("端到端流程验证通过");
    }
}
