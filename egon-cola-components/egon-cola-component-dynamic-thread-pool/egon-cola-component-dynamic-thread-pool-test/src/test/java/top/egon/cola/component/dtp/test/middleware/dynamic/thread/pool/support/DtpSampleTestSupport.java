package top.egon.cola.component.dtp.test.middleware.dynamic.thread.pool.support;

import top.egon.cola.component.dtp.domain.IDynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.valobj.ExecutorKind;
import top.egon.cola.component.dtp.registry.model.DtpConfigChangeMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @ClassName: DtpSampleTestSupport
 * @description: 示例应用动态线程池测试工具
 * @author: 有罗敷的马同学
 * @datetime: 2026Year-06Month-30Day
 * @Version: 1.0
 */
public final class DtpSampleTestSupport {

    public static final String THREAD_POOL_EXECUTOR_01 = "threadPoolExecutor01";

    public static final String THREAD_POOL_EXECUTOR_02 = "threadPoolExecutor02";

    public static final String VIRTUAL_TASK_EXECUTOR = "virtualTaskExecutor";

    private DtpSampleTestSupport() {
    }

    public static void assertVirtualExecutorRegistered(List<ExecutorSnapshot> snapshots) {
        assertThat(snapshots).anyMatch(snapshot -> "virtualTaskExecutor".equals(snapshot.getExecutorName())
                && snapshot.getExecutorKind() == ExecutorKind.VIRTUAL_THREAD_PER_TASK);
    }

    public static ExecutorSnapshot requireSnapshot(IDynamicThreadPoolService dynamicThreadPoolService,
                                                   String executorName) {
        ExecutorSnapshot snapshot = dynamicThreadPoolService.queryExecutorSnapshot(executorName);
        assertThat(snapshot).isNotNull();
        return snapshot;
    }

    public static ExecutorUpdateCommand resizeCommand(ExecutorSnapshot snapshot, int corePoolSize,
                                                      int maximumPoolSize) {
        ExecutorUpdateCommand command = updateCommand(snapshot);
        command.setCorePoolSize(corePoolSize);
        command.setMaximumPoolSize(maximumPoolSize);
        return command;
    }

    public static ExecutorUpdateCommand updateCommand(ExecutorSnapshot snapshot) {
        ExecutorUpdateCommand command = new ExecutorUpdateCommand();
        command.setAppName(snapshot.getAppName());
        command.setInstanceId(snapshot.getInstanceId());
        command.setExecutorName(snapshot.getExecutorName());
        command.setExecutorKind(snapshot.getExecutorKind());
        command.setTraceId("sample-test-" + UUID.randomUUID());
        command.setRequestId("sample-test-" + UUID.randomUUID());
        command.setOperator("sample-test");
        return command;
    }

    public static DtpConfigChangeMessage changeMessage(ExecutorUpdateCommand command) {
        DtpConfigChangeMessage message = new DtpConfigChangeMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setTraceId(command.getTraceId());
        message.setRequestId(command.getRequestId());
        message.setAppName(command.getAppName());
        message.setInstanceId(command.getInstanceId());
        message.setExecutorName(command.getExecutorName());
        message.setExecutorKind(command.getExecutorKind());
        message.setPayload(command);
        message.setOperator(command.getOperator());
        message.setTimestamp(Instant.now());
        return message;
    }

}
