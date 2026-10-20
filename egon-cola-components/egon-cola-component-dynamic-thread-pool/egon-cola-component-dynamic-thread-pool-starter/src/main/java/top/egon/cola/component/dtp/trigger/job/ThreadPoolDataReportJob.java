package top.egon.cola.component.dtp.trigger.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.dtp.config.DynamicThreadPoolAutoProperties;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.scheduling.annotation.Scheduled;
import top.egon.cola.component.dtp.domain.IDynamicThreadPoolService;
import top.egon.cola.component.dtp.domain.model.entity.ExecutorSnapshot;
import top.egon.cola.component.dtp.registry.IRegistry;

import java.util.List;

/**
 * @ClassName: ThreadPoolDataReportJob
 * @description: 线程池数据上报任务
 * @author: 有罗敷的马同学
 * @datetime: 2025Year-04Month-14Day-下午 8:17
 * @Version: 1.0
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ThreadPoolDataReportJob {

    @Qualifier("dynamicThreadPollService")
    private final IDynamicThreadPoolService dynamicThreadPoolService;

    @Qualifier("redisRegistry")
    private final IRegistry registry;
    @Qualifier("egon.cola.component.dtp-top.egon.cola.component.dtp.config.DynamicThreadPoolAutoProperties")
    private final DynamicThreadPoolAutoProperties properties;
    private final Map<String, AlarmState> alarms = new HashMap<>();

    public ThreadPoolDataReportJob(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry) {
        this(dynamicThreadPoolService, registry, new DynamicThreadPoolAutoProperties());
    }

    @Scheduled(fixedDelayString = "${egon.cola.component.dtp.report.interval:20s}")
    public synchronized void execReportThreadPoolList() {
        try {
            List<ExecutorSnapshot> snapshots = dynamicThreadPoolService.queryExecutorSnapshots();
            checkPressure(snapshots);
            try { registry.reportSnapshots(snapshots); }
            catch (RuntimeException batchFailure) {
                log.warn("动态线程池批量报告失败，逐池重试 cause={}", batchFailure.getClass().getSimpleName());
                for (ExecutorSnapshot snapshot : snapshots) {
                    try { registry.reportSnapshot(snapshot); }
                    catch (RuntimeException e) {
                        log.error("动态线程池单池报告失败 app={} instance={} executor={} cause={}",
                                snapshot.getAppName(), snapshot.getInstanceId(), snapshot.getExecutorName(), e.getClass().getSimpleName());
                    }
                }
            }
            log.debug("动态线程池定时报告完成 count={}", snapshots.size());
        } catch (Exception e) {
            log.error("动态线程池，定时上报执行器快照失败", e);
        }
    }

    private void checkPressure(List<ExecutorSnapshot> snapshots) {
        var settings = properties.getObservation().getAlarm();
        if (!settings.isEnabled()) { alarms.clear(); return; }
        Set<String> active = new HashSet<>();
        for (ExecutorSnapshot snapshot : snapshots) {
            String key = snapshot.getAppName() + "/" + snapshot.getInstanceId() + "/" + snapshot.getExecutorName();
            active.add(key);
            AlarmState state = alarms.computeIfAbsent(key, ignored -> new AlarmState());
            long rejected = snapshot.getRejectedTasks() == null ? 0 : snapshot.getRejectedTasks();
            long delta = Math.max(0, rejected - state.rejected);
            state.rejected = rejected;
            long size = snapshot.getQueueSize() == null ? 0 : snapshot.getQueueSize();
            long capacity = snapshot.getRemainingCapacity() == null ? 0 : size + snapshot.getRemainingCapacity();
            boolean busy = capacity > 0 && (double) size / capacity >= settings.getQueueUsageThreshold();
            state.consecutive = busy ? Math.min(settings.getConsecutiveSamples(), state.consecutive + 1) : 0;
            long now = System.nanoTime();
            if ((delta > 0 || state.consecutive >= settings.getConsecutiveSamples())
                    && (state.lastAlarm == Long.MIN_VALUE || now - state.lastAlarm >= settings.getCooldown().toNanos())) {
                state.lastAlarm = now;
                log.warn("动态线程池压力告警 app={} instance={} executor={} queueSize={} capacity={} rejectedDelta={}",
                        snapshot.getAppName(), snapshot.getInstanceId(), snapshot.getExecutorName(), size, capacity, delta);
            }
        }
        alarms.keySet().retainAll(active);
    }

    private static final class AlarmState {
        private long rejected;
        private int consecutive;
        private long lastAlarm = Long.MIN_VALUE;
    }
}
