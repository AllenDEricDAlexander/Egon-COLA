package top.egon.cola.component.dtp.domain;

import org.apache.commons.lang.StringUtils;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.executor.ManagedExecutor;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;

import java.util.List;

/**
 * @author 有罗敷的马同学
 * @description 动态线程池服务
 * @Date 上午 8:56 2025/4/13
 **/
public class DynamicThreadPoolService implements IDynamicThreadPoolService {

    private final ManagedExecutorRegistry managedExecutorRegistry;

    public DynamicThreadPoolService(ManagedExecutorRegistry managedExecutorRegistry) {
        this.managedExecutorRegistry = managedExecutorRegistry;
    }

    @Override
    public List<ExecutorSnapshot> queryExecutorSnapshots() {
        return managedExecutorRegistry.list().stream()
                .map(ManagedExecutor::snapshot)
                .toList();
    }

    @Override
    public ExecutorSnapshot queryExecutorSnapshot(String executorName) {
        return managedExecutorRegistry.get(executorName)
                .map(ManagedExecutor::snapshot)
                .orElse(null);
    }

    @Override
    public UpdateResult updateExecutor(ExecutorUpdateCommand command) {
        if (command == null || StringUtils.isBlank(command.getExecutorName())) {
            return failure("executorName must not be blank");
        }
        return managedExecutorRegistry.get(command.getExecutorName())
                .map(managedExecutor -> updateExecutor(managedExecutor, command))
                .orElseGet(() -> failure("executor not found: " + command.getExecutorName()));
    }

    private UpdateResult updateExecutor(ManagedExecutor managedExecutor, ExecutorUpdateCommand command) {
        UpdateResult identityResult = validateIdentity(managedExecutor, command);
        if (identityResult != null) {
            return identityResult;
        }
        return managedExecutor.update(command);
    }

    private UpdateResult validateIdentity(ManagedExecutor managedExecutor, ExecutorUpdateCommand command) {
        if (StringUtils.isBlank(command.getAppName())) {
            return failure("appName must not be blank");
        }
        if (StringUtils.isBlank(command.getInstanceId())) {
            return failure("instanceId must not be blank");
        }
        if (command.getExecutorKind() == null) {
            return failure("executorKind must not be null");
        }
        if (!command.getAppName().equals(managedExecutor.appName())) {
            return failure("appName mismatch: " + command.getAppName());
        }
        if (!command.getInstanceId().equals(managedExecutor.instanceId())) {
            return failure("instanceId mismatch: " + command.getInstanceId());
        }
        if (!command.getExecutorKind().equals(managedExecutor.kind())) {
            return failure("executorKind mismatch: " + command.getExecutorKind());
        }
        return null;
    }

    private UpdateResult failure(String message) {
        UpdateResult result = new UpdateResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

}
