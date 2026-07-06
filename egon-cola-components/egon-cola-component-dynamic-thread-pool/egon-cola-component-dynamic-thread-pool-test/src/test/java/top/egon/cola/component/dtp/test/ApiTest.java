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

import static org.assertj.core.api.Assertions.assertThat;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.THREAD_POOL_EXECUTOR_01;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.changeMessage;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.requireSnapshot;
import static top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support.DtpSampleTestSupport.resizeCommand;

/**
 * @ClassName: ApiTest
 * @description: test
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-01Month-05Day-21:15
 * @Version: 1.0
 */
@Slf4j
@SpringBootTest(classes = Application.class)
@EnabledIfEnvironmentVariable(named = "DTP_REDIS_TESTS", matches = "true")
public class ApiTest {

    @Resource
    private RTopic dynamicThreadPoolRedisTopic;

    @Resource
    private IDynamicThreadPoolService dynamicThreadPoolService;

    @Test
    public void test_dynamicThreadPoolRedisTopic() throws InterruptedException {
        ExecutorSnapshot beforeConfig = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);

        try {
            ExecutorUpdateCommand command = resizeCommand(beforeConfig, 10, 100);
            dynamicThreadPoolRedisTopic.publish(changeMessage(command));
            log.info("测试：发布执行器配置更新 - 核心线程数：{}, 最大线程数：{}",
                    command.getCorePoolSize(), command.getMaximumPoolSize());

            Thread.sleep(1000);

            ExecutorSnapshot afterConfig = requireSnapshot(dynamicThreadPoolService, THREAD_POOL_EXECUTOR_01);
            assertThat(afterConfig.getCorePoolSize()).isEqualTo(10);
            assertThat(afterConfig.getMaximumPoolSize()).isEqualTo(100);
        } finally {
            ExecutorUpdateCommand restoreCommand = resizeCommand(beforeConfig,
                    beforeConfig.getCorePoolSize(), beforeConfig.getMaximumPoolSize());
            dynamicThreadPoolRedisTopic.publish(changeMessage(restoreCommand));
            Thread.sleep(1000);
        }
    }

}
