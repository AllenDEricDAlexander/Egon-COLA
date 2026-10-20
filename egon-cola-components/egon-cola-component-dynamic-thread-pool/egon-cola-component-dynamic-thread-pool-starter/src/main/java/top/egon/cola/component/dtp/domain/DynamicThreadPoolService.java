package top.egon.cola.component.dtp.domain;

import org.springframework.util.StringUtils;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;
import top.egon.cola.component.dtp.domain.model.entity.UpdateResult;
import top.egon.cola.component.dtp.executor.ManagedExecutor;
import top.egon.cola.component.dtp.executor.ManagedExecutorRegistry;

import java.util.List;
import java.util.ArrayList;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.common.core.validation.ValidationUtils;

/**
 * @author 有罗敷的马同学
 * @description 动态线程池服务
 * @Date 上午 8:56 2025/4/13
 **/
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DynamicThreadPoolService implements IDynamicThreadPoolService {

    @Qualifier("managedExecutorRegistry")
    private final ManagedExecutorRegistry managedExecutorRegistry;

    @Qualifier("dtpValidationUtils")
    private final ValidationUtils validation;

    public DynamicThreadPoolService(ManagedExecutorRegistry managedExecutorRegistry) {
        this(managedExecutorRegistry, null);
    }

    @Override
    public List<ExecutorSnapshot> queryExecutorSnapshots() {
        List<ExecutorSnapshot> snapshots = new ArrayList<>();
        for (ManagedExecutor executor : managedExecutorRegistry.list()) {
            try { snapshots.add(executor.snapshot()); }
            catch (RuntimeException e) {
                log.warn("动态线程池快照失败 executor={} cause={}", executor.executorName(), e.getClass().getSimpleName());
            }
        }
        return List.copyOf(snapshots);
    }

    @Override
    public ExecutorSnapshot queryExecutorSnapshot(String executorName) {
        return managedExecutorRegistry.get(executorName)
                .map(ManagedExecutor::snapshot)
                .orElse(null);
    }

    @Override
    public UpdateResult updateExecutor(ExecutorUpdateCommand command) {
        if (command == null || !StringUtils.hasText(command.getExecutorName())) {
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
        if (validation != null) {
            try {
                validation.validate(command, managedExecutor.supportsVirtualThread()
                        ? ExecutorUpdateCommand.VirtualLimit.class : ExecutorUpdateCommand.PlatformResize.class);
            } catch (ConstraintViolationException e) {
                return failure("invalid executor update configuration");
            }
        }
        return managedExecutor.update(command);
    }

    private UpdateResult validateIdentity(ManagedExecutor managedExecutor, ExecutorUpdateCommand command) {
        if (!StringUtils.hasText(command.getAppName())) {
            return failure("appName must not be blank");
        }
        if (!StringUtils.hasText(command.getInstanceId())) {
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
