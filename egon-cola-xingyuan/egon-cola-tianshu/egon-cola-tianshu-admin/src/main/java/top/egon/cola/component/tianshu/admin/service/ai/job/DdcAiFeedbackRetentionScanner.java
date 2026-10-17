package top.egon.cola.component.tianshu.admin.service.ai.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiSystemActor;

import java.util.Objects;

/**
 * JOB-003：逐租户分批软删除超出保留期的使用反馈。
 * / JOB-003: per tenant, soft-deletes usage feedback past retention in batches.
 */
@Slf4j
public class DdcAiFeedbackRetentionScanner {

    static final int BATCH = 1000;
    static final int MAX_ROUNDS = 20;

    private final DdcAiFeedbackService feedbackService;
    private final DdcAiTenantJobRunner tenantJobRunner;

    public DdcAiFeedbackRetentionScanner(DdcAiFeedbackService feedbackService, DdcAiTenantJobRunner tenantJobRunner) {
        this.feedbackService = Objects.requireNonNull(feedbackService, "feedbackService");
        this.tenantJobRunner = Objects.requireNonNull(tenantJobRunner, "tenantJobRunner");
    }

    @Scheduled(fixedDelayString = "${egon.cola.component.tianshu.admin.ai.feedback.retention-scan-interval:1h}",
            initialDelayString = "${egon.cola.component.tianshu.admin.ai.feedback.retention-scan-interval:1h}")
    public int scanRetention() {
        return tenantJobRunner.forEachTenant(DdcAiSystemActor.RETENTION, "AI feedback retention", this::purgeTenant);
    }

    private int purgeTenant() {
        int total = 0;
        for (int round = 0; round < MAX_ROUNDS; round++) {
            int purged = feedbackService.purgeExpired(BATCH);
            total += purged;
            if (purged < BATCH) break;
        }
        if (total > 0) log.info("AI_FEEDBACK_RETENTION purged={}", total);
        return total;
    }
}
