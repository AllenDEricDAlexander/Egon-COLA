package top.egon.cola.component.dtp.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author      有罗敷的马同学
 * @description 托管执行器注册表
 * @Date        上午8:55 2026/6/29
 **/
public class ManagedExecutorRegistry {

    private final Map<String, ManagedExecutor> managedExecutorMap;

    public ManagedExecutorRegistry(Collection<ManagedExecutor> managedExecutors) {
        Map<String, ManagedExecutor> executorMap = new LinkedHashMap<>();
        if (managedExecutors != null) {
            for (ManagedExecutor managedExecutor : managedExecutors) {
                if (managedExecutor == null) {
                    continue;
                }
                executorMap.put(managedExecutor.executorName(), managedExecutor);
            }
        }
        this.managedExecutorMap = Collections.unmodifiableMap(executorMap);
    }

    public Optional<ManagedExecutor> get(String executorName) {
        return Optional.ofNullable(managedExecutorMap.get(executorName));
    }

    public Collection<ManagedExecutor> list() {
        List<ManagedExecutor> managedExecutors = new ArrayList<>(managedExecutorMap.values());
        return Collections.unmodifiableList(managedExecutors);
    }

}
