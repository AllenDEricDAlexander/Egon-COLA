package top.egon.cola.component.tianshu.admin.service.ai.impl;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.tianshu.admin.config.DdcAiOptimizationProperties;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiScoreAggregateBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiUsageFeedbackBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionBO;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiUsageReportBatchCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiUsageReportCommand;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiEnumConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiUsageFeedbackResultConverter;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiJudgeVerdictDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiJudgeStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunErrorCodeEnum;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiUsageFeedbackPageQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiUsageFeedbackResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiUsageReportReceiptResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiUsageFeedbackRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionRepository;
import top.egon.cola.component.tianshu.admin.security.rpc.DdcServicePrincipal;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiScopeValidator;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportStatusEnum;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** 使用反馈服务实现。 / Usage-feedback service implementation. */
@Slf4j
@Service("ddcAiFeedbackService")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Validated
public class DdcAiFeedbackServiceImpl implements DdcAiFeedbackService {

    static final String REASON_INVALID_FIELD = "INVALID_FIELD";
    static final String REASON_RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    static final String REASON_VERSION_MISMATCH = "VERSION_MISMATCH";
    static final String REASON_CLOCK_SKEW = "CLOCK_SKEW";

    /** 上报允许的最大回溯时长（Spec TIME-001）。 / Maximum report age accepted (Spec TIME-001). */
    static final Duration MAX_REPORT_AGE = Duration.ofDays(7);

    @Qualifier("ddcAiUsageFeedbackRepository")
    private final DdcAiUsageFeedbackRepository feedbackRepository;

    @Qualifier("ddcAiResourceRepository")
    private final DdcAiResourceRepository resourceRepository;

    @Qualifier("ddcAiVersionRepository")
    private final DdcAiVersionRepository versionRepository;

    @Qualifier("ddcAiScopeValidator")
    private final DdcAiScopeValidator scopeValidator;

    @Qualifier("ddcAiEnumConverterImpl")
    private final DdcAiEnumConverter enumConverter;

    @Qualifier("ddcAiUsageFeedbackResultConverterImpl")
    private final DdcAiUsageFeedbackResultConverter resultConverter;

    @Qualifier("egonColaValidationUtils")
    private final ValidationUtils validationUtils;

    @Qualifier("ddcAiCommandTransactionTemplate")
    private final TransactionTemplate commandTransactionTemplate;

    @Qualifier("ddcAiReadTransactionTemplate")
    private final TransactionTemplate readTransactionTemplate;

    @Qualifier("egon.cola.component.tianshu.admin.ai-top.egon.cola.component.tianshu.admin.config.DdcAiOptimizationProperties")
    private final DdcAiOptimizationProperties properties;

    @Qualifier("ddcPublishClock")
    private final Clock clock;

    @Override
    public DdcAiUsageReportReceiptResult accept(DdcAiUsageReportBatchCommand command) {
        DdcServicePrincipal principal = currentPrincipal();
        Instant now = clock.instant();
        List<DdcAiUsageReportReceiptResult.ItemResult> items = new ArrayList<>(command.getReports().size());
        for (DdcAiUsageReportCommand report : command.getReports()) {
            items.add(acceptOne(principal, report, now));
        }
        return DdcAiUsageReportReceiptResult.builder().items(items).build();
    }

    private DdcAiUsageReportReceiptResult.ItemResult acceptOne(DdcServicePrincipal principal,
            DdcAiUsageReportCommand report, Instant now) {
        String usageId = report.getUsageId();
        if (!validationUtils.isValid(report) || containsNul(report.getInputExcerpt())
                || containsNul(report.getOutputExcerpt())) {
            return rejected(usageId, REASON_INVALID_FIELD);
        }
        if (report.getOccurredAt().isBefore(now.minus(MAX_REPORT_AGE))
                || report.getOccurredAt().isAfter(now.plus(properties.getFeedback().getMaxClockSkew()))) {
            return rejected(usageId, REASON_CLOCK_SKEW);
        }
        try {
            DdcAiUsageReportReceiptResult.ItemResult result = commandTransactionTemplate.execute(
                    status -> insertReport(principal, report));
            return Objects.requireNonNull(result, "AI_USAGE_REPORT_RESULT_MISSING");
        } catch (DataIntegrityViolationException duplicate) {
            return item(usageId, DdcAiUsageReportStatusEnum.ACCEPTED_DUPLICATE, null);
        } catch (CommonException | ConstraintViolationException rejected) {
            log.debug("AI usage report rejected usageId={} failureType={}", usageId,
                    rejected.getClass().getSimpleName());
            return rejected(usageId, REASON_RESOURCE_NOT_FOUND);
        }
    }

    private DdcAiUsageReportReceiptResult.ItemResult insertReport(DdcServicePrincipal principal,
            DdcAiUsageReportCommand report) {
        Optional<DdcAiResourceBO> resource = resourceRepository.findActiveById(report.getResourceId())
                .filter(row -> report.getResourceKey().equals(row.getResourceKey()))
                .filter(row -> report.getNamespaceCode().equals(row.getNamespaceCode()));
        if (resource.isEmpty()) {
            return rejected(report.getUsageId(), REASON_RESOURCE_NOT_FOUND);
        }
        scopeValidator.assertCanReport(resource.get());
        Optional<DdcAiVersionBO> version = versionRepository.findActiveById(report.getVersionId())
                .filter(row -> Objects.equals(row.getResourceId(), report.getResourceId()))
                .filter(row -> Objects.equals(row.getVersionNo(), report.getVersionNo()));
        if (version.isEmpty()) {
            return rejected(report.getUsageId(), REASON_VERSION_MISMATCH);
        }
        feedbackRepository.insert(DdcAiUsageFeedbackBO.builder()
                .resourceId(report.getResourceId())
                .versionId(report.getVersionId())
                .usageId(report.getUsageId())
                .reporterCredentialId(principal.credentialId())
                .bizCode(principal.bizCode() != null ? principal.bizCode() : resource.get().getBizCode())
                .envCode(principal.env() != null ? principal.env() : resource.get().getEnvCode())
                .appCode(principal.appCode() != null ? principal.appCode() : resource.get().getAppCode())
                .outcome(enumConverter.toPersistenceOutcome(report.getOutcome()))
                .explicitScore(report.getExplicitScore() == null ? null : report.getExplicitScore().shortValue())
                .judgeState(report.getExplicitScore() != null
                        ? DdcAiJudgeStatePersistenceEnum.NOT_REQUIRED : DdcAiJudgeStatePersistenceEnum.PENDING)
                .judgeAttempts((short) 0)
                .inputExcerpt(DdcAiText.truncateUtf8(report.getInputExcerpt(), properties.getFeedback().getMaxInputExcerptBytes()))
                .outputExcerpt(DdcAiText.truncateUtf8(report.getOutputExcerpt(), properties.getFeedback().getMaxOutputExcerptBytes()))
                .latencyMs(report.getLatencyMs())
                .occurredAt(report.getOccurredAt())
                .traceId(report.getTraceId())
                .build());
        return item(report.getUsageId(), DdcAiUsageReportStatusEnum.ACCEPTED, null);
    }

    @Override
    public List<DdcAiUsageFeedbackBO> claimForJudge(Set<Long> resourceIds, int limit) {
        List<DdcAiUsageFeedbackBO> claimed = commandTransactionTemplate.execute(status -> {
            Instant now = clock.instant();
            Instant leaseUntil = now.plus(properties.getJudge().getLease());
            List<DdcAiUsageFeedbackBO> rows = feedbackRepository.findClaimableForJudge(
                    resourceIds, now, properties.getJudge().getMaxAttempts(), limit);
            List<DdcAiUsageFeedbackBO> result = new ArrayList<>(rows.size());
            for (DdcAiUsageFeedbackBO row : rows) {
                feedbackRepository.update(row.toBuilder()
                                .judgeState(DdcAiJudgeStatePersistenceEnum.JUDGING)
                                .judgeAttempts((short) (row.getJudgeAttempts() + 1))
                                .judgeLeaseUntil(leaseUntil)
                                .build())
                        .ifPresent(result::add);
            }
            return result;
        });
        return claimed == null ? List.of() : claimed;
    }

    @Override
    public void completeJudge(Long id, Long version, DdcAiJudgeVerdictDTO verdict) {
        commandTransactionTemplate.executeWithoutResult(status -> {
            Optional<DdcAiUsageFeedbackBO> current = judging(id, version);
            if (current.isEmpty()) {
                log.debug("AI judge result discarded because the sample was taken over id={}", id);
                return;
            }
            Optional<DdcAiUsageFeedbackBO> written = feedbackRepository.update(current.get().toBuilder()
                    .judgeState(DdcAiJudgeStatePersistenceEnum.DONE)
                    .judgeScore((short) verdict.score())
                    .judgedAt(clock.instant())
                    .judgeLeaseUntil(null)
                    .judgeError(null)
                    .build());
            if (written.isEmpty()) log.debug("AI judge result lost a version race id={}", id);
        });
    }

    @Override
    public void failJudge(Long id, Long version, DdcAiRunErrorCodeEnum code) {
        commandTransactionTemplate.executeWithoutResult(status -> judging(id, version).ifPresent(current -> {
            boolean exhausted = current.getJudgeAttempts() >= properties.getJudge().getMaxAttempts();
            feedbackRepository.update(current.toBuilder()
                    .judgeState(exhausted ? DdcAiJudgeStatePersistenceEnum.FAILED : DdcAiJudgeStatePersistenceEnum.PENDING)
                    .judgeLeaseUntil(null)
                    .judgeError(code.getValue())
                    .build());
        }));
    }

    private Optional<DdcAiUsageFeedbackBO> judging(Long id, Long version) {
        return feedbackRepository.findActiveById(id)
                .filter(row -> Objects.equals(row.getVersion(), version))
                .filter(row -> row.getJudgeState() == DdcAiJudgeStatePersistenceEnum.JUDGING);
    }

    @Override
    public DdcAiScoreAggregateBO aggregate(Long resourceId, Long versionId, Instant windowStart, Instant windowEnd) {
        return readTransactionTemplate.execute(status ->
                feedbackRepository.aggregate(resourceId, versionId, windowStart, windowEnd));
    }

    @Override
    public List<DdcAiUsageFeedbackBO> worstSamples(Long resourceId, Long versionId, Instant windowStart,
            Instant windowEnd, int limit) {
        List<DdcAiUsageFeedbackBO> rows = readTransactionTemplate.execute(status ->
                feedbackRepository.findWorst(resourceId, versionId, windowStart, windowEnd, limit));
        return rows == null ? List.of() : rows;
    }

    @Override
    public DdcPageResult<DdcAiUsageFeedbackResult> pageFeedback(DdcAiUsageFeedbackPageQuery query) {
        return readTransactionTemplate.execute(status -> {
            DdcAiResourceBO resource = resourceRepository.findActiveById(query.getResourceId())
                    .orElseThrow(() -> new CommonException(56302, "TIANSHU_AI_NOT_FOUND", "AI resource not found"));
            scopeValidator.assertCanRead(resource);
            DdcPageResult<DdcAiUsageFeedbackBO> page = feedbackRepository.page(query);
            return DdcPageResult.<DdcAiUsageFeedbackResult>builder()
                    .records(resultConverter.toTargetList(page.getRecords()))
                    .total(page.getTotal())
                    .pageNo(page.getPageNo())
                    .pageSize(page.getPageSize())
                    .build();
        });
    }

    @Override
    public int purgeExpired(int limit) {
        Integer deleted = commandTransactionTemplate.execute(status -> feedbackRepository.softDeleteExpired(
                clock.instant().minus(properties.getFeedback().getRetention()), limit));
        return deleted == null ? 0 : deleted;
    }

    private static DdcServicePrincipal currentPrincipal() {
        try {
            return DdcServicePrincipal.current();
        } catch (IllegalStateException missingPrincipal) {
            throw new CommonException(56307, "TIANSHU_AI_SCOPE_FORBIDDEN",
                    "AI permission is required for the requested namespace");
        }
    }

    private static boolean containsNul(String value) {
        return value != null && value.indexOf('\u0000') >= 0;
    }

    private static DdcAiUsageReportReceiptResult.ItemResult rejected(String usageId, String reason) {
        return item(usageId, DdcAiUsageReportStatusEnum.REJECTED, reason);
    }

    private static DdcAiUsageReportReceiptResult.ItemResult item(String usageId, DdcAiUsageReportStatusEnum status,
            String reason) {
        return DdcAiUsageReportReceiptResult.ItemResult.builder().usageId(usageId).status(status).reason(reason).build();
    }
}
