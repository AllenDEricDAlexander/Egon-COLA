package top.egon.cola.component.dtp.executor.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorUpdateCommand;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @description 同一实际执行器上的完整验证、串行调参与真实参数同步
 */
@Slf4j
public final class ThreadPoolResizeSupport {

    private ThreadPoolResizeSupport() { }

    public static void resize(ThreadPoolExecutor executor, int core, int max) {
        if (core <= 0 || max <= 0) {
            throw new IllegalArgumentException("corePoolSize and maximumPoolSize must be positive");
        }
        if (core > max) { throw new IllegalArgumentException("corePoolSize must <= maximumPoolSize"); }
        synchronized (executor) {
            if (max < executor.getCorePoolSize()) {
                executor.setCorePoolSize(core);
                executor.setMaximumPoolSize(max);
            } else {
                executor.setMaximumPoolSize(max);
                executor.setCorePoolSize(core);
            }
        }
    }

    public static void update(ThreadPoolExecutor executor, ThreadPoolTaskExecutor spring, ExecutorUpdateCommand command) {
        Objects.requireNonNull(command, "command");
        synchronized (executor) {
            boolean hasCore = command.getCorePoolSize() != null;
            if (hasCore != (command.getMaximumPoolSize() != null)) {
                throw new IllegalArgumentException("corePoolSize and maximumPoolSize must be provided together");
            }
            int core = hasCore ? command.getCorePoolSize() : executor.getCorePoolSize();
            int max = hasCore ? command.getMaximumPoolSize() : executor.getMaximumPoolSize();
            long keep = command.getKeepAliveSeconds() == null ? executor.getKeepAliveTime(TimeUnit.SECONDS) : command.getKeepAliveSeconds();
            boolean timeout = command.getAllowCoreThreadTimeOut() == null ? executor.allowsCoreThreadTimeOut() : command.getAllowCoreThreadTimeOut();
            if (hasCore && core <= 0 || max <= 0 || core > max) {
                throw new IllegalArgumentException("corePoolSize and maximumPoolSize must be positive and core <= max");
            }
            if (keep < 0) { throw new IllegalArgumentException("keepAliveSeconds must be >= 0"); }
            if (timeout && keep == 0) { throw new IllegalArgumentException("keepAliveSeconds must be > 0 when allowCoreThreadTimeOut is true"); }
            if (spring != null && keep > Integer.MAX_VALUE) { throw new IllegalArgumentException("keepAliveSeconds must be <= " + Integer.MAX_VALUE); }
            if (command.getConcurrencyLimit() != null) {
                throw new IllegalArgumentException("concurrencyLimit is only supported by virtual executors");
            }
            Integer capacity = command.getQueueCapacity();
            if (capacity != null && !(executor.getQueue() instanceof ResizableCapacityBlockingQueue<?>)) {
                throw new UnsupportedOperationException("queue does not support capacity changes");
            }
            int oldCore = executor.getCorePoolSize();
            int oldMax = executor.getMaximumPoolSize();
            long oldKeepNanos = executor.getKeepAliveTime(TimeUnit.NANOSECONDS);
            boolean oldTimeout = executor.allowsCoreThreadTimeOut();
            Runnable update = () -> {
                try {
                    apply(executor, spring, core, max, keep, TimeUnit.SECONDS, timeout);
                    if (capacity != null && spring != null) { spring.setQueueCapacity(capacity); }
                } catch (RuntimeException e) {
                    try { apply(executor, spring, oldCore, oldMax, oldKeepNanos, TimeUnit.NANOSECONDS, oldTimeout); }
                    catch (RuntimeException restoreFailure) {
                        e.addSuppressed(restoreFailure);
                        log.error("动态线程池参数恢复失败 cause={}", restoreFailure.getClass().getSimpleName());
                    }
                    throw e;
                }
            };
            if (capacity == null) { update.run(); }
            else { ((ResizableCapacityBlockingQueue<?>) executor.getQueue()).resize(capacity, update); }
        }
    }

    private static void apply(ThreadPoolExecutor executor, ThreadPoolTaskExecutor spring,
                              int core, int max, long keep, TimeUnit unit, boolean timeout) {
        executor.allowCoreThreadTimeOut(false);
        if (spring == null) {
            if (core != executor.getCorePoolSize() || max != executor.getMaximumPoolSize()) { resize(executor, core, max); }
        } else {
            spring.setAllowCoreThreadTimeOut(false);
            if (core == executor.getCorePoolSize() && max == executor.getMaximumPoolSize()) {
                // 原参数不变时不以更新路径的 core>0 约束拒绝历史 core=0 执行器。
            } else if (max < executor.getCorePoolSize()) {
                spring.setCorePoolSize(core);
                spring.setMaxPoolSize(max);
            } else {
                spring.setMaxPoolSize(max);
                spring.setCorePoolSize(core);
            }
            spring.setKeepAliveSeconds(Math.toIntExact(unit.toSeconds(keep)));
            spring.setAllowCoreThreadTimeOut(timeout);
        }
        executor.setKeepAliveTime(keep, unit);
        executor.allowCoreThreadTimeOut(timeout);
    }

    public static void queueSnapshot(BlockingQueue<Runnable> queue, ExecutorSnapshot snapshot) {
        snapshot.setQueueType(queue.getClass().getSimpleName());
        if (queue instanceof ResizableCapacityBlockingQueue<?> resizable) {
            var state = resizable.snapshot();
            snapshot.setQueueSize(state.size());
            snapshot.setRemainingCapacity(state.capacity() - state.size());
        } else {
            snapshot.setQueueSize(queue.size());
            snapshot.setRemainingCapacity(queue.remainingCapacity());
        }
    }
}
