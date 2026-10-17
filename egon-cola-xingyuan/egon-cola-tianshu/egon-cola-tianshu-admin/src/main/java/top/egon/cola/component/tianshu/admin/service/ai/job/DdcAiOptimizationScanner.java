package top.egon.cola.component.tianshu.admin.service.ai.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import top.egon.cola.component.tianshu.admin.security.tenancy.DdcTenantBindingDTO;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiOptimizationService;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiSystemActor;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;

/**
 * JOB-002：逐租户回收过期运行、评估策略、认领待执行运行并在有界执行器上执行。
 * / JOB-002: per tenant, reclaims expired runs, evaluates policies, claims pending runs and executes them on a
 * bounded executor.
 */
@Slf4j
public class DdcAiOptimizationScanner {

    static final int PENDING_LIMIT = 50;

    private final DdcAiOptimizationService optimizationService;
    private final ExecutorService executor;
    private final DdcAiTenantJobRunner tenantJobRunner;
    private final Semaphore slots;
    private final String instanceId;
    private final Clock clock;

    public DdcAiOptimizationScanner(DdcAiOptimizationService optimizationService, ExecutorService executor,
            DdcAiTenantJobRunner tenantJobRunner, int maxConcurrentRuns, String instanceId, Clock clock) {
        if (maxConcurrentRuns < 1) throw new IllegalArgumentException("maxConcurrentRuns must be positive");
        this.optimizationService = Objects.requireNonNull(optimizationService, "optimizationService");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.tenantJobRunner = Objects.requireNonNull(tenantJobRunner, "tenantJobRunner");
        this.slots = new Semaphore(maxConcurrentRuns);
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Scheduled(fixedDelayString = "${egon.cola.component.tianshu.admin.ai.optimization.scan-interval:60s}",
            initialDelayString = "${egon.cola.component.tianshu.admin.ai.optimization.scan-interval:60s}")
    public int scanOptimization() {
        int submitted = 0;
        for (DdcTenantBindingDTO configured : tenantJobRunner.bindings()) {
            if (configured == null || !configured.isEnabled()) continue;
            try {
                submitted += scanTenant(configured);
            } catch (RuntimeException failure) {
                log.error("AI optimization scan failed for tenantId={}", configured.getTenantId(), failure);
            }
        }
        return submitted;
    }

    private int scanTenant(DdcTenantBindingDTO tenant) {
        Instant now = clock.instant();
        List<Long> candidates = tenantJobRunner.inTenant(tenant, DdcAiSystemActor.OPTIMIZER, () -> {
            int reclaimed = optimizationService.reclaimExpiredRuns(now);
            if (reclaimed > 0) log.info("AI_OPT_RECLAIM tenantId={} reclaimed={}", tenant.getTenantId(), reclaimed);
            Set<Long> runIds = new LinkedHashSet<>(optimizationService.evaluateTenant(now));
            runIds.addAll(optimizationService.pendingRunIds(PENDING_LIMIT));
            return List.copyOf(runIds);
        });
        int submitted = 0;
        for (Long runId : candidates) {
            if (!slots.tryAcquire()) {
                log.info("AI_OPT executor saturated; remaining runs wait for the next scan tenantId={}",
                        tenant.getTenantId());
                break;
            }
            if (!claim(tenant, runId)) {
                slots.release();
                continue;
            }
            try {
                executor.execute(() -> runClaimed(tenant, runId));
                submitted++;
            } catch (RejectedExecutionException rejected) {
                slots.release();
                log.warn("AI_OPT executor rejected runId={}; its lease will expire and be reclaimed", runId);
                break;
            }
        }
        return submitted;
    }

    private boolean claim(DdcTenantBindingDTO tenant, Long runId) {
        try {
            return tenantJobRunner.inTenant(tenant, DdcAiSystemActor.OPTIMIZER,
                    () -> optimizationService.claimRun(runId, instanceId, clock.instant()));
        } catch (RuntimeException failure) {
            log.warn("AI_OPT claim failed runId={}", runId, failure);
            return false;
        }
    }

    private void runClaimed(DdcTenantBindingDTO tenant, Long runId) {
        try {
            tenantJobRunner.inTenant(tenant, DdcAiSystemActor.OPTIMIZER, () -> {
                optimizationService.executeRun(runId);
                return null;
            });
        } catch (RuntimeException failure) {
            log.error("AI_OPT_RUN execution failed runId={}", runId, failure);
        } finally {
            slots.release();
        }
    }
}
