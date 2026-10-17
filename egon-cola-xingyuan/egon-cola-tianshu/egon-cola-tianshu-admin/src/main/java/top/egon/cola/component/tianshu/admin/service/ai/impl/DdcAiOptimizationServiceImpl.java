package top.egon.cola.component.tianshu.admin.service.ai.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.tianshu.admin.config.DdcAiOptimizationProperties;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationContextBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationPolicyBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationRunBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiScoreAggregateBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiUsageFeedbackBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionNodeBO;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDraftCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiOptimizationPolicyCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiOptimizationTriggerCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiPublishCommand;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiOptimizationPolicyResultConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiOptimizationRunResultConverter;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelResultDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiPolicyStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunErrorCodeEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunTriggerPersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiIdQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiOptimizationRunPageQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiOptimizationPolicyResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiOptimizationRunResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiPublicationResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiResourceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAiOptimizationPolicyRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiOptimizationRunRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionNodeRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionRepository;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiModelPort;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiOptimizationService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiResourceService;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiOptimizationStateTransition.Signal;
import top.egon.cola.component.tianshu.admin.service.ai.model.DdcAiModelException;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiScopeValidator;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiValidationGroups;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 优化闭环实现。评估在持有策略行锁的短事务内完成；执行阶段的模型调用在事务外，草稿替换与发布复用资源服务并以修订号 CAS 串联。
 * / Optimization loop implementation. Evaluation runs in a short transaction holding policy row locks; model calls
 * run outside transactions, and draft replacement plus publication reuse the resource service chained by revision CAS.
 */
@Slf4j
@Service("ddcAiOptimizationService")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Validated
public class DdcAiOptimizationServiceImpl implements DdcAiOptimizationService {

    static final int EVALUATION_BATCH = 50;
    static final int RECLAIM_BATCH = 100;
    static final String REQUEST_DIGEST_DOMAIN = "tianshu-ai-optimization-trigger-v1";

    @Qualifier("ddcAiOptimizationPolicyRepository")
    private final DdcAiOptimizationPolicyRepository policyRepository;

    @Qualifier("ddcAiOptimizationRunRepository")
    private final DdcAiOptimizationRunRepository runRepository;

    @Qualifier("ddcAiResourceRepository")
    private final DdcAiResourceRepository resourceRepository;

    @Qualifier("ddcAiVersionRepository")
    private final DdcAiVersionRepository versionRepository;

    @Qualifier("ddcAiVersionNodeRepository")
    private final DdcAiVersionNodeRepository versionNodeRepository;

    @Qualifier("ddcAiFeedbackService")
    private final DdcAiFeedbackService feedbackService;

    @Qualifier("ddcAiResourceService")
    private final DdcAiResourceService resourceService;

    @Qualifier("ddcAiScopeValidator")
    private final DdcAiScopeValidator scopeValidator;

    @Qualifier("ddcAiOptimizationStateTransition")
    private final DdcAiOptimizationStateTransition transition;

    @Qualifier("ddcAiOptimizerStrategyRegistry")
    private final Map<DdcAiKindEnum, DdcAiOptimizerStrategy> optimizers;

    @Qualifier("ddcAiContentStrategyRegistry")
    private final Map<DdcAiKindEnum, DdcAiContentStrategy> contentStrategies;

    @Qualifier("ddcAiModelPort")
    private final ObjectProvider<DdcAiModelPort> modelPort;

    @Qualifier("ddcAiOptimizationPolicyResultConverterImpl")
    private final DdcAiOptimizationPolicyResultConverter policyResultConverter;

    @Qualifier("ddcAiOptimizationRunResultConverterImpl")
    private final DdcAiOptimizationRunResultConverter runResultConverter;

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

    // ------------------------------------------------------------------ REST

    @Override
    public DdcAiOptimizationPolicyResult getPolicy(DdcAiIdQuery query) {
        return readTransactionTemplate.execute(status -> {
            DdcAiResourceBO resource = readableResource(query.id());
            DdcAiOptimizationPolicyBO policy = policyRepository.findActiveByResourceId(resource.getId())
                    .orElseGet(() -> defaultPolicy(resource.getId()));
            return withEvaluation(policy, resource);
        });
    }

    @Override
    public DdcAiOptimizationPolicyResult replacePolicy(DdcAiIdQuery query, DdcAiOptimizationPolicyCommand command) {
        DdcAiOptimizationPolicyBO saved = commandTransactionTemplate.execute(status -> {
            DdcAiResourceBO resource = resourceRepository.findActiveForUpdateById(query.id()).orElseThrow(this::notFound);
            scopeValidator.assertCanPublish(resource);
            Optional<DdcAiOptimizationPolicyBO> current = policyRepository.findActiveByResourceIdForUpdate(resource.getId());
            long expected = Long.parseLong(command.getExpectedRevision());
            long actual = current.map(DdcAiOptimizationPolicyBO::getVersion).orElse(0L);
            if (expected != actual) throw revisionConflict();
            DdcAiOptimizationPolicyBO base = current.orElseGet(() -> defaultPolicy(resource.getId()));
            if (Boolean.TRUE.equals(command.getRearm()) && base.getActiveRunId() != null) throw runActive();
            DdcAiOptimizationPolicyBO next = applyCommand(base, command);
            if (Boolean.TRUE.equals(command.getRearm())) {
                next.setState(transition.next(base.getState(), Signal.REARM, context(next)))
                        .setIterationsDone((short) 0).setConsecutiveFailures((short) 0);
            }
            next.setConvergedAt(next.getState() == DdcAiPolicyStatePersistenceEnum.CONVERGED ? next.getConvergedAt() : null);
            if (current.isEmpty()) return policyRepository.insert(next);
            return policyRepository.update(next).orElseThrow(this::revisionConflict);
        });
        return getPolicy(new DdcAiIdQuery(Objects.requireNonNull(saved).getResourceId()));
    }

    @Override
    public DdcAiOptimizationRunResult trigger(DdcAiIdQuery query, String requestId, DdcAiOptimizationTriggerCommand command) {
        if (modelPort.getIfAvailable() == null) {
            throw new CommonException(56322, "TIANSHU_AI_OPTIMIZATION_DISABLED", "AI optimization channel is disabled");
        }
        String digest = requestDigest(requestActor(), query.id(), command.getNote());
        try {
            DdcAiOptimizationRunBO run = commandTransactionTemplate.execute(status -> createManualRun(query, requestId,
                    digest, command));
            return runResultConverter.toTarget(run);
        } catch (DataIntegrityViolationException concurrent) {
            DdcAiOptimizationRunBO replay = readTransactionTemplate.execute(status ->
                    runRepository.findByRequestId(requestId).orElse(null));
            if (replay != null && digest.equals(replay.getRequestSha256())
                    && Objects.equals(replay.getResourceId(), query.id())) {
                return runResultConverter.toTarget(replay);
            }
            throw runActive();
        }
    }

    private DdcAiOptimizationRunBO createManualRun(DdcAiIdQuery query, String requestId, String digest,
            DdcAiOptimizationTriggerCommand command) {
        Optional<DdcAiOptimizationRunBO> replay = runRepository.findByRequestId(requestId);
        if (replay.isPresent()) {
            if (digest.equals(replay.get().getRequestSha256()) && Objects.equals(replay.get().getResourceId(), query.id())) {
                return replay.get();
            }
            throw new CommonException(56305, "TIANSHU_AI_IDEMPOTENCY_CONFLICT",
                    "Idempotency-Key was already used for a different request");
        }
        DdcAiResourceBO resource = resourceRepository.findActiveForUpdateById(query.id()).orElseThrow(this::notFound);
        scopeValidator.assertCanPublish(resource);
        DdcAiOptimizationPolicyBO policy = policyRepository.findActiveByResourceIdForUpdate(resource.getId())
                .orElseThrow(() -> new CommonException(56324, "TIANSHU_AI_POLICY_STATE",
                        "Save an optimization policy before triggering a run"));
        if (policy.getActiveRunId() != null || runRepository.findActiveRunForResource(resource.getId()).isPresent()) {
            throw runActive();
        }
        if (resource.getCurrentVersionId() == null) {
            throw new CommonException(56306, "TIANSHU_AI_NOT_PUBLISHED", "AI resource has no published version");
        }
        DdcAiOptimizationRunBO run = insertRun(policy, resource, clock.instant(), DdcAiRunTriggerPersistenceEnum.MANUAL,
                requestId, digest, command.getNote());
        policyRepository.update(policy.toBuilder().activeRunId(run.getId()).build()).orElseThrow(this::revisionConflict);
        return run;
    }

    @Override
    public DdcPageResult<DdcAiOptimizationRunResult> pageRuns(DdcAiOptimizationRunPageQuery query) {
        return readTransactionTemplate.execute(status -> {
            readableResource(query.getResourceId());
            DdcPageResult<DdcAiOptimizationRunBO> page = runRepository.page(query);
            return DdcPageResult.<DdcAiOptimizationRunResult>builder()
                    .records(runResultConverter.toTargetList(page.getRecords()))
                    .total(page.getTotal()).pageNo(page.getPageNo()).pageSize(page.getPageSize())
                    .build();
        });
    }

    @Override
    public DdcAiOptimizationRunResult getRun(DdcAiIdQuery query, Long runId) {
        return readTransactionTemplate.execute(status -> {
            readableResource(query.id());
            return runRepository.findActiveById(runId)
                    .filter(run -> Objects.equals(run.getResourceId(), query.id()))
                    .map(runResultConverter::toTarget)
                    .orElseThrow(() -> new CommonException(56325, "TIANSHU_AI_RUN_NOT_FOUND",
                            "AI optimization run not found"));
        });
    }

    // ------------------------------------------------------------------ jobs

    @Override
    public int reclaimExpiredRuns(Instant now) {
        List<DdcAiOptimizationRunBO> expired = readTransactionTemplate.execute(status ->
                runRepository.findExpiredRunning(now, RECLAIM_BATCH));
        int reclaimed = 0;
        for (DdcAiOptimizationRunBO run : expired == null ? List.<DdcAiOptimizationRunBO>of() : expired) {
            if (finishRun(run.getId(), run.getVersion(), DdcAiRunStatePersistenceEnum.FAILED,
                    DdcAiRunErrorCodeEnum.LEASE_EXPIRED, "run lease expired", null, null)) {
                reclaimed++;
            }
        }
        return reclaimed;
    }

    @Override
    public List<Long> evaluateTenant(Instant now) {
        List<Long> created = commandTransactionTemplate.execute(status -> {
            List<Long> runIds = new ArrayList<>();
            for (DdcAiOptimizationPolicyBO policy : policyRepository.findEvaluableForUpdate(EVALUATION_BATCH)) {
                evaluate(policy, now).ifPresent(runIds::add);
            }
            return runIds;
        });
        return created == null ? List.of() : created;
    }

    private Optional<Long> evaluate(DdcAiOptimizationPolicyBO policy, Instant now) {
        Optional<DdcAiResourceBO> found = resourceRepository.findActiveById(policy.getResourceId());
        if (found.isEmpty()) return Optional.empty();
        DdcAiResourceBO resource = found.get();
        DdcAiOptimizationPolicyBO next = policy.toBuilder().lastEvaluatedAt(now).build();
        if (resource.getCurrentVersionId() == null) {
            next.setState(transition.next(policy.getState(), Signal.NOT_PUBLISHED, context(policy)));
            save(next);
            return Optional.empty();
        }
        Optional<DdcAiOptimizationRunBO> lastRun = runRepository.findLatestFinished(resource.getId());
        if (policy.getState() == DdcAiPolicyStatePersistenceEnum.AWAITING_PUBLISH) {
            boolean published = lastRun.map(run -> !Objects.equals(run.getBaseVersionId(), resource.getCurrentVersionId()))
                    .orElse(true);
            next.setState(transition.next(policy.getState(),
                    published ? Signal.BASE_PUBLISHED : Signal.STILL_AWAITING, context(policy)));
            save(next);
            return Optional.empty();
        }
        Instant windowStart = now.minus(policy.getEvaluationWindow());
        DdcAiScoreAggregateBO aggregate = feedbackService.aggregate(resource.getId(), resource.getCurrentVersionId(),
                windowStart, now);
        next.setLastEvaluatedVersionId(resource.getCurrentVersionId())
                .setLastSampleCount(aggregate.getSampleCount())
                .setLastAverageScore(aggregate.getAverageScore());
        Signal signal;
        Long createdRunId = null;
        if (aggregate.getSampleCount() < policy.getMinSamples()) {
            signal = Signal.INSUFFICIENT_SAMPLES;
        } else if (aggregate.getAverageScore() != null
                && aggregate.getAverageScore().compareTo(java.math.BigDecimal.valueOf(policy.getScoreThreshold())) >= 0) {
            signal = Signal.THRESHOLD_MET;
            next.setConvergedAt(now);
        } else if (policy.getIterationsDone() >= policy.getMaxIterations()) {
            signal = Signal.ITERATIONS_EXHAUSTED;
        } else if (lastRun.map(run -> run.getFinishedAt() != null
                && run.getFinishedAt().plus(policy.getCooldown()).isAfter(now)).orElse(false)) {
            signal = Signal.COOLING_DOWN;
        } else {
            signal = Signal.RUN_CREATED;
            DdcAiOptimizationRunBO run = insertRun(policy, resource, now, DdcAiRunTriggerPersistenceEnum.SCHEDULED,
                    null, null, "");
            next.setActiveRunId(run.getId());
            createdRunId = run.getId();
        }
        next.setState(transition.next(policy.getState(), signal, context(policy)));
        save(next);
        log.info("AI_OPT_EVALUATE resourceId={} versionId={} samples={} average={} decision={}", resource.getId(),
                resource.getCurrentVersionId(), aggregate.getSampleCount(), aggregate.getAverageScore(), signal);
        return Optional.ofNullable(createdRunId);
    }

    @Override
    public List<Long> pendingRunIds(int limit) {
        List<Long> ids = readTransactionTemplate.execute(status -> runRepository.findPendingIds(limit));
        return ids == null ? List.of() : ids;
    }

    @Override
    public boolean claimRun(Long runId, String owner, Instant now) {
        Boolean claimed = commandTransactionTemplate.execute(status -> runRepository.findActiveById(runId)
                .filter(run -> run.getState() == DdcAiRunStatePersistenceEnum.PENDING)
                .flatMap(run -> runRepository.update(run.toBuilder()
                        .state(DdcAiRunStatePersistenceEnum.RUNNING)
                        .leaseOwner(owner)
                        .leaseUntil(now.plus(properties.getOptimization().getRunLease()))
                        .startedAt(now)
                        .build()))
                .isPresent());
        return Boolean.TRUE.equals(claimed);
    }

    @Override
    public void executeRun(Long runId) {
        Prepared prepared = readTransactionTemplate.execute(status -> prepare(runId));
        if (prepared == null) return;
        DdcAiOptimizationRunBO run = prepared.run();
        if (prepared.policy() == null || prepared.resource() == null || prepared.base() == null) {
            finish(run, DdcAiRunStatePersistenceEnum.SKIPPED, DdcAiRunErrorCodeEnum.NOT_PUBLISHED,
                    "resource, policy or base version is gone");
            return;
        }
        if (!Objects.equals(prepared.resource().getCurrentVersionId(), run.getBaseVersionId())) {
            finish(run, DdcAiRunStatePersistenceEnum.SKIPPED, DdcAiRunErrorCodeEnum.BASE_CHANGED,
                    "base version changed before execution");
            return;
        }
        DdcAiResourceResult draft = resourceService.get(new DdcAiIdQuery(run.getResourceId()));
        if (!draftMatchesBaseline(prepared, draft)) {
            finish(run, DdcAiRunStatePersistenceEnum.SKIPPED, DdcAiRunErrorCodeEnum.DRAFT_DIRTY,
                    "draft has unpublished edits");
            return;
        }
        DdcAiOptimizerStrategy strategy = optimizers.get(prepared.resource().getKind());
        DdcAiModelPort port = modelPort.getIfAvailable();
        if (strategy == null || port == null) {
            finish(run, DdcAiRunStatePersistenceEnum.FAILED, DdcAiRunErrorCodeEnum.MODEL_ERROR,
                    "optimization channel unavailable");
            return;
        }
        DdcAiOptimizationContextBO context = DdcAiOptimizationContextBO.builder()
                .kind(prepared.resource().getKind()).baseline(prepared.baseline())
                .instruction(prepared.policy().getOptimizerInstruction())
                .samples(prepared.samples()).maxContextBytes(properties.getJudge().getMaxContextBytes()).build();
        var request = strategy.compose(context, run.getModelAlias(), properties.getModel().getMaxOutputTokens(),
                properties.getModel().getTemperature());
        DdcAiModelResultDTO output;
        DdcAiContentDTO candidate;
        try {
            output = port.complete(request);
            if (output.truncated()) throw new IllegalArgumentException("INVALID_OUTPUT");
            candidate = strategy.parse(output.text());
            validationUtils.validate(candidate, DdcAiValidationGroups.Save.class, DdcAiValidationGroups.Publish.class);
        } catch (DdcAiModelException model) {
            finish(run, DdcAiRunStatePersistenceEnum.FAILED, model.getCode(), safe(model.getMessage()));
            return;
        } catch (IllegalArgumentException | jakarta.validation.ConstraintViolationException invalid) {
            finish(run, DdcAiRunStatePersistenceEnum.FAILED, DdcAiRunErrorCodeEnum.INVALID_OUTPUT,
                    "optimizer output rejected");
            return;
        }
        RunMetrics metrics = new RunMetrics(sha256(request.systemPrompt() + "\n" + request.userPrompt()),
                sha256(output.text()), output.promptTokens(), output.completionTokens());
        DdcAiIdQuery idQuery = new DdcAiIdQuery(run.getResourceId());
        DdcAiResourceResult replaced;
        try {
            replaced = resourceService.replaceDraft(idQuery, DdcAiDraftCommand.builder()
                    .expectedRevision(draft.getResource().getRevision())
                    .displayName(draft.getResource().getDisplayName())
                    .description(draft.getResource().getDescription() == null ? "" : draft.getResource().getDescription())
                    .draft(candidate)
                    .build());
        } catch (CommonException conflict) {
            if (conflict.getCode() != 56303) throw conflict;
            finish(run, DdcAiRunStatePersistenceEnum.SKIPPED, DdcAiRunErrorCodeEnum.DRAFT_DIRTY,
                    "draft changed while optimizing");
            return;
        }
        DdcAiPublicationResult publication = null;
        if (Boolean.TRUE.equals(prepared.policy().getAutoPublish())) {
            try {
                publication = resourceService.publish(idQuery, publishRequestId(run), DdcAiPublishCommand.builder()
                        .expectedRevision(replaced.getResource().getRevision())
                        .changeNote(changeNote(run))
                        .build());
            } catch (CommonException conflict) {
                if (conflict.getCode() != 56303) throw conflict;
                finishWithMetrics(run, DdcAiRunStatePersistenceEnum.FAILED, DdcAiRunErrorCodeEnum.PUBLISH_CONFLICT,
                        "publish revision conflict", metrics, null);
                return;
            }
        }
        finishWithMetrics(run, DdcAiRunStatePersistenceEnum.SUCCEEDED, null, null, metrics, publication);
    }

    private Prepared prepare(Long runId) {
        DdcAiOptimizationRunBO run = runRepository.findActiveById(runId)
                .filter(row -> row.getState() == DdcAiRunStatePersistenceEnum.RUNNING).orElse(null);
        if (run == null) return null;
        DdcAiOptimizationPolicyBO policy = policyRepository.findActiveByResourceId(run.getResourceId()).orElse(null);
        DdcAiResourceBO resource = resourceRepository.findActiveById(run.getResourceId()).orElse(null);
        DdcAiVersionBO base = versionRepository.findActiveById(run.getBaseVersionId()).orElse(null);
        if (policy == null || resource == null || base == null) {
            return new Prepared(run, policy, resource, base, null, List.of());
        }
        List<DdcAiNodeDTO> nodes = resource.getKind() == DdcAiKindEnum.PROMPT ? List.of()
                : versionNodeRepository.findTreeByVersionId(base.getId()).stream().map(this::node).toList();
        DdcAiContentDTO baseline = DdcAiContentDTO.builder()
                .promptText(resource.getKind() == DdcAiKindEnum.PROMPT ? base.getPromptText() : null)
                .nodes(nodes).build();
        var samples = feedbackService.worstSamples(run.getResourceId(), run.getBaseVersionId(), run.getWindowStart(),
                run.getWindowEnd(), properties.getOptimization().getSampleLimit());
        return new Prepared(run, policy, resource, base, baseline, samples);
    }

    private boolean draftMatchesBaseline(Prepared prepared, DdcAiResourceResult draft) {
        DdcAiContentStrategy strategy = contentStrategies.get(prepared.resource().getKind());
        try {
            return strategy != null && draft.getDraft() != null
                    && Objects.equals(strategy.prepare(draft.getDraft()).contentHash(), prepared.base().getContentSha256());
        } catch (RuntimeException invalidDraft) {
            return false;
        }
    }

    private DdcAiNodeDTO node(DdcAiVersionNodeBO node) {
        return DdcAiNodeDTO.builder().path(node.getPath()).nodeType(node.getNodeType()).content(node.getContent()).build();
    }

    // ------------------------------------------------------------------ finishing

    private void finish(DdcAiOptimizationRunBO run, DdcAiRunStatePersistenceEnum state, DdcAiRunErrorCodeEnum code,
            String message) {
        finishRun(run.getId(), run.getVersion(), state, code, message, null, null);
    }

    private void finishWithMetrics(DdcAiOptimizationRunBO run, DdcAiRunStatePersistenceEnum state,
            DdcAiRunErrorCodeEnum code, String message, RunMetrics metrics, DdcAiPublicationResult publication) {
        finishRun(run.getId(), run.getVersion(), state, code, message, metrics, publication);
    }

    /** 在独立事务内写运行终态与策略计数；运行已被接管时放弃。 / Writes the terminal run state and policy counters; gives up when taken over. */
    private boolean finishRun(Long runId, Long expectedVersion, DdcAiRunStatePersistenceEnum state,
            DdcAiRunErrorCodeEnum code, String message, RunMetrics metrics, DdcAiPublicationResult publication) {
        Boolean written = commandTransactionTemplate.execute(status -> {
            Optional<DdcAiOptimizationRunBO> current = runRepository.findActiveById(runId)
                    .filter(run -> Objects.equals(run.getVersion(), expectedVersion))
                    .filter(run -> !run.getState().terminal());
            if (current.isEmpty()) return false;
            Instant now = clock.instant();
            DdcAiOptimizationRunBO.DdcAiOptimizationRunBOBuilder terminal = current.get().toBuilder()
                    .state(state).errorCode(code).errorMessage(code == null ? null : safe(message))
                    .leaseUntil(null).finishedAt(now);
            if (metrics != null) {
                terminal.promptSha256(metrics.promptSha256()).outputSha256(metrics.outputSha256())
                        .promptTokens(metrics.promptTokens()).completionTokens(metrics.completionTokens());
            }
            if (publication != null) {
                terminal.publicationId(publication.getPublicationId())
                        .producedVersionId(publication.getTargetVersionId()).autoPublished(true);
            }
            if (runRepository.update(terminal.build()).isEmpty()) return false;
            policyRepository.findActiveByResourceIdForUpdate(current.get().getResourceId()).ifPresent(policy ->
                    save(afterRun(policy, state)));
            log.info("AI_OPT_RUN runId={} resourceId={} state={} errorCode={}", runId, current.get().getResourceId(),
                    state, code == null ? null : code.getValue());
            return true;
        });
        return Boolean.TRUE.equals(written);
    }

    private DdcAiOptimizationPolicyBO afterRun(DdcAiOptimizationPolicyBO policy, DdcAiRunStatePersistenceEnum state) {
        DdcAiOptimizationPolicyBO next = policy.toBuilder().activeRunId(null).build();
        switch (state) {
            case SUCCEEDED -> next.setIterationsDone((short) (policy.getIterationsDone() + 1))
                    .setConsecutiveFailures((short) 0)
                    .setState(transition.next(policy.getState(), Signal.RUN_SUCCEEDED, context(policy)));
            case FAILED -> {
                short failures = (short) (policy.getConsecutiveFailures() + 1);
                next.setConsecutiveFailures(failures).setState(transition.next(policy.getState(), Signal.RUN_FAILED,
                        new DdcAiOptimizationStateTransition.Context(Boolean.TRUE.equals(policy.getAutoPublish()), failures)));
            }
            default -> next.setState(transition.next(policy.getState(), Signal.RUN_SKIPPED, context(policy)));
        }
        next.setConvergedAt(next.getState() == DdcAiPolicyStatePersistenceEnum.CONVERGED ? policy.getConvergedAt() : null);
        return next;
    }

    @Override
    public void onResourceDeleted(Long resourceId) {
        policyRepository.findActiveByResourceIdForUpdate(resourceId).ifPresent(policyRepository::delete);
    }

    // ------------------------------------------------------------------ helpers

    private DdcAiOptimizationRunBO insertRun(DdcAiOptimizationPolicyBO policy, DdcAiResourceBO resource, Instant now,
            DdcAiRunTriggerPersistenceEnum trigger, String requestId, String requestSha256, String note) {
        Instant windowStart = now.minus(policy.getEvaluationWindow());
        DdcAiScoreAggregateBO aggregate = feedbackService.aggregate(resource.getId(), resource.getCurrentVersionId(),
                windowStart, now);
        return runRepository.insert(DdcAiOptimizationRunBO.builder()
                .resourceId(resource.getId()).policyId(policy.getId())
                .runNo(runRepository.nextRunNo(resource.getId()))
                .iterationNo((short) (policy.getIterationsDone() + 1))
                .trigger(trigger).requestId(requestId).requestSha256(requestSha256)
                .state(DdcAiRunStatePersistenceEnum.PENDING)
                .baseVersionId(resource.getCurrentVersionId())
                .sampleCount(aggregate.getSampleCount()).averageScore(aggregate.getAverageScore())
                .thresholdSnapshot(policy.getScoreThreshold())
                .windowStart(windowStart).windowEnd(now)
                .modelAlias(policy.getOptimizerModelAlias())
                .autoPublished(false).note(note == null ? "" : note)
                .build());
    }

    private void save(DdcAiOptimizationPolicyBO policy) {
        if (policyRepository.update(policy).isEmpty()) {
            throw new IllegalStateException("AI_OPTIMIZATION_POLICY_CAS_MISS");
        }
    }

    private DdcAiOptimizationPolicyResult withEvaluation(DdcAiOptimizationPolicyBO policy, DdcAiResourceBO resource) {
        DdcAiOptimizationPolicyResult result = policyResultConverter.toTarget(policy);
        Integer pending = null;
        if (resource.getCurrentVersionId() != null) {
            Instant now = clock.instant();
            pending = feedbackService.aggregate(resource.getId(), resource.getCurrentVersionId(),
                    now.minus(policy.getEvaluationWindow()), now).getPendingJudgeCount();
        }
        return result.setEvaluation(DdcAiOptimizationPolicyResult.EvaluationResult.builder()
                .evaluatedAt(policy.getLastEvaluatedAt())
                .versionId(policy.getLastEvaluatedVersionId())
                .versionNo(policy.getLastEvaluatedVersionNo())
                .sampleCount(policy.getLastSampleCount())
                .averageScore(policy.getLastAverageScore())
                .pendingJudgeCount(pending)
                .build());
    }

    private DdcAiOptimizationPolicyBO defaultPolicy(Long resourceId) {
        return DdcAiOptimizationPolicyBO.builder()
                .resourceId(resourceId).enabled(false).scoreThreshold((short) 80).minSamples(20)
                .evaluationWindow(Duration.ofHours(24)).maxIterations((short) 5).cooldown(Duration.ofHours(1))
                .autoPublish(false).judgeEnabled(true).optimizerModelAlias("").judgeModelAlias("")
                .optimizerInstruction("").state(DdcAiPolicyStatePersistenceEnum.ARMED)
                .iterationsDone((short) 0).consecutiveFailures((short) 0)
                .build();
    }

    private static DdcAiOptimizationPolicyBO applyCommand(DdcAiOptimizationPolicyBO base,
            DdcAiOptimizationPolicyCommand command) {
        return base.toBuilder()
                .enabled(command.getEnabled())
                .scoreThreshold(command.getScoreThreshold().shortValue())
                .minSamples(command.getMinSamples())
                .evaluationWindow(command.getEvaluationWindow())
                .maxIterations(command.getMaxIterations().shortValue())
                .cooldown(command.getCooldown())
                .autoPublish(command.getAutoPublish())
                .judgeEnabled(command.getJudgeEnabled())
                .optimizerModelAlias(command.getOptimizerModelAlias())
                .judgeModelAlias(command.getJudgeModelAlias())
                .optimizerInstruction(command.getOptimizerInstruction())
                .build();
    }

    private DdcAiResourceBO readableResource(Long resourceId) {
        DdcAiResourceBO resource = resourceRepository.findActiveById(resourceId).orElseThrow(this::notFound);
        scopeValidator.assertCanRead(resource);
        return resource;
    }

    private static DdcAiOptimizationStateTransition.Context context(DdcAiOptimizationPolicyBO policy) {
        return new DdcAiOptimizationStateTransition.Context(Boolean.TRUE.equals(policy.getAutoPublish()),
                policy.getConsecutiveFailures() == null ? 0 : policy.getConsecutiveFailures());
    }

    static String publishRequestId(DdcAiOptimizationRunBO run) {
        String padded = String.format("%019d", run.getId());
        return "opt-" + EgonColaTenantIdProvider.currentTenantId() + "-" + padded;
    }

    private static String changeNote(DdcAiOptimizationRunBO run) {
        String note = "ai-optimization run " + run.getRunNo() + (run.getNote().isBlank() ? "" : ": " + run.getNote());
        return note.length() > 512 ? note.substring(0, 512) : note;
    }

    private static String requestActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || authentication.getName() == null ? "anonymous" : authentication.getName();
    }

    static String requestDigest(String actor, Long resourceId, String note) {
        return sha256(REQUEST_DIGEST_DOMAIN + "\n" + actor + "\n" + resourceId + "\n" + note);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String safe(String message) {
        if (message == null) return null;
        return message.length() > 512 ? message.substring(0, 512) : message;
    }

    private CommonException notFound() {
        return new CommonException(56302, "TIANSHU_AI_NOT_FOUND", "AI resource not found");
    }

    private CommonException revisionConflict() {
        return new CommonException(56303, "TIANSHU_AI_REVISION_CONFLICT", "Optimization policy revision changed");
    }

    private CommonException runActive() {
        return new CommonException(56323, "TIANSHU_AI_RUN_ACTIVE", "An optimization run is already active");
    }

    /** 执行阶段读取的不可变快照。 / Immutable snapshot read for the execution phase. */
    private record Prepared(DdcAiOptimizationRunBO run, DdcAiOptimizationPolicyBO policy, DdcAiResourceBO resource,
            DdcAiVersionBO base, DdcAiContentDTO baseline,
            List<DdcAiUsageFeedbackBO> samples) {
    }

    /** 运行的提示与输出摘要。 / Prompt and output digests of a run. */
    private record RunMetrics(String promptSha256, String outputSha256, Integer promptTokens, Integer completionTokens) {
    }
}
