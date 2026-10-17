package top.egon.cola.component.tianshu.admin.service.ai;

import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.tianshu.admin.config.DdcAiOptimizationProperties;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationPolicyBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiOptimizationRunBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiScoreAggregateBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionBO;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDraftCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiOptimizationPolicyCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiOptimizationTriggerCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiPublishCommand;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiOptimizationPolicyResultConverterImpl;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiOptimizationRunResultConverterImpl;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelResultDTO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiResourceSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiPolicyStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunErrorCodeEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunTriggerPersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiIdQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiPublicationResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiResourceResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAiOptimizationPolicyRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiOptimizationRunRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionNodeRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionRepository;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiContentStrategy;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiOptimizationServiceImpl;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiOptimizationStateTransition;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiOptimizerStrategy;
import top.egon.cola.component.tianshu.admin.service.ai.model.DdcAiModelException;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiScopeValidator;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DdcAiOptimizationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-17T05:00:00Z");
    private static final String BASE_HASH = "a".repeat(64);

    private final DdcAiOptimizationPolicyRepository policyRepository = mock(DdcAiOptimizationPolicyRepository.class);
    private final DdcAiOptimizationRunRepository runRepository = mock(DdcAiOptimizationRunRepository.class);
    private final DdcAiResourceRepository resourceRepository = mock(DdcAiResourceRepository.class);
    private final DdcAiVersionRepository versionRepository = mock(DdcAiVersionRepository.class);
    private final DdcAiVersionNodeRepository versionNodeRepository = mock(DdcAiVersionNodeRepository.class);
    private final DdcAiFeedbackService feedbackService = mock(DdcAiFeedbackService.class);
    private final DdcAiResourceService resourceService = mock(DdcAiResourceService.class);
    private final DdcAiScopeValidator scopeValidator = mock(DdcAiScopeValidator.class);
    private final DdcAiOptimizerStrategy promptOptimizer = mock(DdcAiOptimizerStrategy.class);
    private final DdcAiContentStrategy promptContent = mock(DdcAiContentStrategy.class);
    private final DdcAiModelPort modelPort = mock(DdcAiModelPort.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<DdcAiModelPort> modelProvider = mock(ObjectProvider.class);
    private final DdcAiOptimizationProperties properties = new DdcAiOptimizationProperties();
    private final TransactionTemplate transactions = new TransactionTemplate(mock(PlatformTransactionManager.class));
    private final List<DdcAiOptimizationRunBO> runWrites = new ArrayList<>();
    private final List<DdcAiOptimizationPolicyBO> policyWrites = new ArrayList<>();

    private final DdcAiOptimizationServiceImpl service = new DdcAiOptimizationServiceImpl(policyRepository,
            runRepository, resourceRepository, versionRepository, versionNodeRepository, feedbackService,
            resourceService, scopeValidator, new DdcAiOptimizationStateTransition(),
            Map.of(DdcAiKindEnum.PROMPT, promptOptimizer), Map.of(DdcAiKindEnum.PROMPT, promptContent), modelProvider,
            new DdcAiOptimizationPolicyResultConverterImpl(), new DdcAiOptimizationRunResultConverterImpl(),
            new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator()),
            transactions, transactions, properties, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        MDC.put("tenantId", "42");
        when(modelProvider.getIfAvailable()).thenReturn(modelPort);
        when(runRepository.update(any())).thenAnswer(invocation -> {
            runWrites.add(invocation.getArgument(0));
            return Optional.of(invocation.getArgument(0));
        });
        when(policyRepository.update(any())).thenAnswer(invocation -> {
            policyWrites.add(invocation.getArgument(0));
            return Optional.of(invocation.getArgument(0));
        });
        when(runRepository.insert(any())).thenAnswer(invocation ->
                ((DdcAiOptimizationRunBO) invocation.getArgument(0)).toBuilder().id(88L).version(0L).build());
        when(runRepository.nextRunNo(7L)).thenReturn(4L);
        when(resourceRepository.findActiveById(7L)).thenReturn(Optional.of(resource(99L)));
        when(resourceRepository.findActiveForUpdateById(7L)).thenReturn(Optional.of(resource(99L)));
    }

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void evaluateCreatesAPendingRunBelowThresholdWithEnoughSamples() {
        when(policyRepository.findEvaluableForUpdate(50)).thenReturn(List.of(policy(DdcAiPolicyStatePersistenceEnum.ARMED)));
        when(feedbackService.aggregate(eq(7L), eq(99L), any(), eq(NOW))).thenReturn(aggregate(25, "71.50"));

        assertThat(service.evaluateTenant(NOW)).containsExactly(88L);

        ArgumentCaptor<DdcAiOptimizationRunBO> run = ArgumentCaptor.forClass(DdcAiOptimizationRunBO.class);
        verify(runRepository).insert(run.capture());
        assertThat(run.getValue().getState()).isEqualTo(DdcAiRunStatePersistenceEnum.PENDING);
        assertThat(run.getValue().getTrigger()).isEqualTo(DdcAiRunTriggerPersistenceEnum.SCHEDULED);
        assertThat(run.getValue().getRunNo()).isEqualTo(4L);
        assertThat(run.getValue().getBaseVersionId()).isEqualTo(99L);
        assertThat(run.getValue().getWindowStart()).isEqualTo(NOW.minus(Duration.ofHours(24)));
        assertThat(lastPolicy().getActiveRunId()).isEqualTo(88L);
        assertThat(lastPolicy().getLastAverageScore()).isEqualByComparingTo("71.50");
        assertThat(lastPolicy().getState()).isEqualTo(DdcAiPolicyStatePersistenceEnum.ARMED);
    }

    @Test
    void evaluateConvergesExhaustsCoolsDownOrWaitsForSamples() {
        when(feedbackService.aggregate(eq(7L), eq(99L), any(), eq(NOW))).thenReturn(aggregate(25, "85.00"));
        when(policyRepository.findEvaluableForUpdate(50)).thenReturn(List.of(policy(DdcAiPolicyStatePersistenceEnum.ARMED)));
        assertThat(service.evaluateTenant(NOW)).isEmpty();
        assertThat(lastPolicy().getState()).isEqualTo(DdcAiPolicyStatePersistenceEnum.CONVERGED);
        assertThat(lastPolicy().getConvergedAt()).isEqualTo(NOW);

        when(feedbackService.aggregate(eq(7L), eq(99L), any(), eq(NOW))).thenReturn(aggregate(25, "50.00"));
        when(policyRepository.findEvaluableForUpdate(50)).thenReturn(List.of(policy(DdcAiPolicyStatePersistenceEnum.ARMED)
                .toBuilder().iterationsDone((short) 5).build()));
        service.evaluateTenant(NOW);
        assertThat(lastPolicy().getState()).isEqualTo(DdcAiPolicyStatePersistenceEnum.EXHAUSTED);

        when(policyRepository.findEvaluableForUpdate(50)).thenReturn(List.of(policy(DdcAiPolicyStatePersistenceEnum.ARMED)));
        when(runRepository.findLatestFinished(7L)).thenReturn(Optional.of(run(DdcAiRunStatePersistenceEnum.SUCCEEDED)
                .toBuilder().finishedAt(NOW.minus(Duration.ofMinutes(10))).build()));
        assertThat(service.evaluateTenant(NOW)).isEmpty();
        assertThat(lastPolicy().getActiveRunId()).isNull();

        when(feedbackService.aggregate(eq(7L), eq(99L), any(), eq(NOW))).thenReturn(aggregate(3, "10.00"));
        when(runRepository.findLatestFinished(7L)).thenReturn(Optional.empty());
        assertThat(service.evaluateTenant(NOW)).isEmpty();
        assertThat(lastPolicy().getLastSampleCount()).isEqualTo(3);
        verify(runRepository, never()).insert(any());
    }

    @Test
    void awaitingPublishReturnsToArmedOnceANewVersionIsPublished() {
        when(policyRepository.findEvaluableForUpdate(50))
                .thenReturn(List.of(policy(DdcAiPolicyStatePersistenceEnum.AWAITING_PUBLISH)));
        when(runRepository.findLatestFinished(7L)).thenReturn(Optional.of(run(DdcAiRunStatePersistenceEnum.SUCCEEDED)
                .toBuilder().baseVersionId(98L).build()));

        service.evaluateTenant(NOW);

        assertThat(lastPolicy().getState()).isEqualTo(DdcAiPolicyStatePersistenceEnum.ARMED);
        verifyNoInteractions(feedbackService);
    }

    @Test
    void executeRunSkipsWhenTheBaseChangedOrTheDraftIsDirty() {
        when(runRepository.findActiveById(88L)).thenReturn(Optional.of(run(DdcAiRunStatePersistenceEnum.RUNNING)
                .toBuilder().baseVersionId(98L).build()));
        stubPrepared();
        service.executeRun(88L);
        assertThat(lastRun().getState()).isEqualTo(DdcAiRunStatePersistenceEnum.SKIPPED);
        assertThat(lastRun().getErrorCode()).isEqualTo(DdcAiRunErrorCodeEnum.BASE_CHANGED);

        when(runRepository.findActiveById(88L)).thenReturn(Optional.of(run(DdcAiRunStatePersistenceEnum.RUNNING)));
        when(promptContent.prepare(any())).thenReturn(prepared("b".repeat(64)));
        service.executeRun(88L);
        assertThat(lastRun().getErrorCode()).isEqualTo(DdcAiRunErrorCodeEnum.DRAFT_DIRTY);
        assertThat(lastRun().getLeaseUntil()).isNull();
        assertThat(lastRun().getFinishedAt()).isEqualTo(NOW);
        verifyNoInteractions(modelPort);
    }

    @Test
    void executeRunAutoPublishesWithAnIdempotentRequestAndSucceeds() {
        when(runRepository.findActiveById(88L)).thenReturn(Optional.of(run(DdcAiRunStatePersistenceEnum.RUNNING)));
        stubPrepared();
        when(policyRepository.findActiveByResourceId(7L)).thenReturn(Optional.of(policy(DdcAiPolicyStatePersistenceEnum.ARMED)
                .toBuilder().autoPublish(true).build()));
        when(policyRepository.findActiveByResourceIdForUpdate(7L)).thenReturn(Optional.of(
                policy(DdcAiPolicyStatePersistenceEnum.ARMED).toBuilder().autoPublish(true).activeRunId(88L).build()));
        stubModel("{\"promptText\":\"improved\"}");
        when(resourceService.replaceDraft(any(), any())).thenReturn(resourceResult("6"));
        when(resourceService.publish(any(), anyString(), any())).thenReturn(DdcAiPublicationResult.builder()
                .publicationId(555L).targetVersionId(100L).targetVersionNo(5L).resourceRevision(7L).build());

        service.executeRun(88L);

        ArgumentCaptor<DdcAiDraftCommand> draft = ArgumentCaptor.forClass(DdcAiDraftCommand.class);
        verify(resourceService).replaceDraft(eq(new DdcAiIdQuery(7L)), draft.capture());
        assertThat(draft.getValue().getExpectedRevision()).isEqualTo("5");
        assertThat(draft.getValue().getDraft().getPromptText()).isEqualTo("improved");
        ArgumentCaptor<DdcAiPublishCommand> publish = ArgumentCaptor.forClass(DdcAiPublishCommand.class);
        verify(resourceService).publish(eq(new DdcAiIdQuery(7L)), eq("opt-42-0000000000000000088"), publish.capture());
        assertThat(publish.getValue().getExpectedRevision()).isEqualTo("6");
        assertThat(publish.getValue().getChangeNote()).isEqualTo("ai-optimization run 4");
        assertThat(lastRun().getState()).isEqualTo(DdcAiRunStatePersistenceEnum.SUCCEEDED);
        assertThat(lastRun().getPublicationId()).isEqualTo(555L);
        assertThat(lastRun().getProducedVersionId()).isEqualTo(100L);
        assertThat(lastRun().getAutoPublished()).isTrue();
        assertThat(lastRun().getPromptSha256()).hasSize(64);
        assertThat(lastPolicy().getIterationsDone()).isEqualTo((short) 1);
        assertThat(lastPolicy().getActiveRunId()).isNull();
        assertThat(lastPolicy().getState()).isEqualTo(DdcAiPolicyStatePersistenceEnum.ARMED);
    }

    @Test
    void executeRunWithoutAutoPublishLeavesADraftAndAwaitsPublication() {
        when(runRepository.findActiveById(88L)).thenReturn(Optional.of(run(DdcAiRunStatePersistenceEnum.RUNNING)));
        stubPrepared();
        when(policyRepository.findActiveByResourceIdForUpdate(7L))
                .thenReturn(Optional.of(policy(DdcAiPolicyStatePersistenceEnum.ARMED).toBuilder().activeRunId(88L).build()));
        stubModel("{\"promptText\":\"improved\"}");
        when(resourceService.replaceDraft(any(), any())).thenReturn(resourceResult("6"));

        service.executeRun(88L);

        verify(resourceService, never()).publish(any(), anyString(), any());
        assertThat(lastRun().getState()).isEqualTo(DdcAiRunStatePersistenceEnum.SUCCEEDED);
        assertThat(lastRun().getAutoPublished()).isFalse();
        assertThat(lastPolicy().getState()).isEqualTo(DdcAiPolicyStatePersistenceEnum.AWAITING_PUBLISH);
    }

    @Test
    void modelFailuresFailTheRunAndTheThirdConsecutiveFailurePauses() {
        when(runRepository.findActiveById(88L)).thenReturn(Optional.of(run(DdcAiRunStatePersistenceEnum.RUNNING)));
        stubPrepared();
        when(policyRepository.findActiveByResourceIdForUpdate(7L)).thenReturn(Optional.of(
                policy(DdcAiPolicyStatePersistenceEnum.ARMED).toBuilder().consecutiveFailures((short) 2).build()));
        when(promptOptimizer.compose(any(), anyString(), anyInt(), any(Double.class))).thenReturn(null);
        when(modelPort.complete(any())).thenThrow(new DdcAiModelException(DdcAiRunErrorCodeEnum.MODEL_TIMEOUT, 0,
                "model gateway unreachable", null));

        service.executeRun(88L);

        assertThat(lastRun().getState()).isEqualTo(DdcAiRunStatePersistenceEnum.FAILED);
        assertThat(lastRun().getErrorCode()).isEqualTo(DdcAiRunErrorCodeEnum.MODEL_TIMEOUT);
        assertThat(lastPolicy().getConsecutiveFailures()).isEqualTo((short) 3);
        assertThat(lastPolicy().getState()).isEqualTo(DdcAiPolicyStatePersistenceEnum.PAUSED);
        verify(resourceService, never()).replaceDraft(any(), any());
    }

    @Test
    void invalidOptimizerOutputFailsWithoutTouchingTheDraft() {
        when(runRepository.findActiveById(88L)).thenReturn(Optional.of(run(DdcAiRunStatePersistenceEnum.RUNNING)));
        stubPrepared();
        stubModel("not json");
        when(promptOptimizer.parse("not json")).thenThrow(new IllegalArgumentException("INVALID_OUTPUT"));

        service.executeRun(88L);

        assertThat(lastRun().getErrorCode()).isEqualTo(DdcAiRunErrorCodeEnum.INVALID_OUTPUT);
        verify(resourceService, never()).replaceDraft(any(), any());
    }

    @Test
    void claimRunMovesPendingToRunningAndReportsCasMisses() {
        when(runRepository.findActiveById(88L)).thenReturn(Optional.of(run(DdcAiRunStatePersistenceEnum.PENDING)));

        assertThat(service.claimRun(88L, "admin-1", NOW)).isTrue();
        assertThat(lastRun().getState()).isEqualTo(DdcAiRunStatePersistenceEnum.RUNNING);
        assertThat(lastRun().getLeaseUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
        assertThat(lastRun().getLeaseOwner()).isEqualTo("admin-1");

        org.mockito.Mockito.doReturn(Optional.empty()).when(runRepository).update(any());
        assertThat(service.claimRun(88L, "admin-2", NOW)).isFalse();
        when(runRepository.findActiveById(89L)).thenReturn(Optional.of(run(DdcAiRunStatePersistenceEnum.RUNNING)));
        assertThat(service.claimRun(89L, "admin-2", NOW)).isFalse();
    }

    @Test
    void reclaimExpiredRunsFailsThemWithLeaseExpired() {
        DdcAiOptimizationRunBO running = run(DdcAiRunStatePersistenceEnum.RUNNING);
        when(runRepository.findExpiredRunning(NOW, 100)).thenReturn(List.of(running));
        when(runRepository.findActiveById(88L)).thenReturn(Optional.of(running));
        when(policyRepository.findActiveByResourceIdForUpdate(7L))
                .thenReturn(Optional.of(policy(DdcAiPolicyStatePersistenceEnum.ARMED).toBuilder().activeRunId(88L).build()));

        assertThat(service.reclaimExpiredRuns(NOW)).isEqualTo(1);

        assertThat(lastRun().getErrorCode()).isEqualTo(DdcAiRunErrorCodeEnum.LEASE_EXPIRED);
        assertThat(lastPolicy().getConsecutiveFailures()).isEqualTo((short) 1);
        assertThat(lastPolicy().getActiveRunId()).isNull();
    }

    @Test
    void replacePolicyChecksRevisionAndRejectsRearmWhileARunIsActive() {
        when(policyRepository.findActiveByResourceIdForUpdate(7L))
                .thenReturn(Optional.of(policy(DdcAiPolicyStatePersistenceEnum.PAUSED).toBuilder().activeRunId(88L).build()));

        assertThatThrownBy(() -> service.replacePolicy(new DdcAiIdQuery(7L), command("2", false)))
                .isInstanceOfSatisfying(CommonException.class, failure -> assertThat(failure.getCode()).isEqualTo(56303));
        assertThatThrownBy(() -> service.replacePolicy(new DdcAiIdQuery(7L), command("3", true)))
                .isInstanceOfSatisfying(CommonException.class, failure -> assertThat(failure.getCode()).isEqualTo(56323));
    }

    @Test
    void replacePolicyRearmsAndResetsCounters() {
        when(policyRepository.findActiveByResourceIdForUpdate(7L)).thenReturn(Optional.of(
                policy(DdcAiPolicyStatePersistenceEnum.CONVERGED).toBuilder().convergedAt(NOW)
                        .iterationsDone((short) 4).consecutiveFailures((short) 1).build()));
        when(policyRepository.findActiveByResourceId(7L)).thenAnswer(invocation -> Optional.of(lastPolicy()));
        when(feedbackService.aggregate(eq(7L), eq(99L), any(), eq(NOW))).thenReturn(aggregate(0, null));

        var result = service.replacePolicy(new DdcAiIdQuery(7L), command("3", true));

        assertThat(lastPolicy().getState()).isEqualTo(DdcAiPolicyStatePersistenceEnum.ARMED);
        assertThat(lastPolicy().getIterationsDone()).isZero();
        assertThat(lastPolicy().getConvergedAt()).isNull();
        assertThat(lastPolicy().getScoreThreshold()).isEqualTo((short) 90);
        assertThat(result.getPersisted()).isTrue();
        assertThat(result.getState()).isEqualTo("ARMED");
        verify(scopeValidator).assertCanPublish(any());
    }

    @Test
    void triggerReplaysTheSameKeyAndRequiresAnEnabledChannel() {
        String key = "trigger-0123456789ab";
        when(runRepository.findByRequestId(key)).thenReturn(Optional.empty());
        when(policyRepository.findActiveByResourceIdForUpdate(7L)).thenReturn(Optional.of(policy(DdcAiPolicyStatePersistenceEnum.ARMED)));
        when(feedbackService.aggregate(eq(7L), eq(99L), any(), eq(NOW))).thenReturn(aggregate(4, "40.00"));

        var created = service.trigger(new DdcAiIdQuery(7L), key, DdcAiOptimizationTriggerCommand.builder().note("n").build());

        assertThat(created.getTrigger()).isEqualTo("MANUAL");
        ArgumentCaptor<DdcAiOptimizationRunBO> inserted = ArgumentCaptor.forClass(DdcAiOptimizationRunBO.class);
        verify(runRepository).insert(inserted.capture());
        assertThat(inserted.getValue().getRequestSha256()).hasSize(64);

        when(runRepository.findByRequestId(key)).thenReturn(Optional.of(inserted.getValue().toBuilder().id(88L).build()));
        assertThat(service.trigger(new DdcAiIdQuery(7L), key, DdcAiOptimizationTriggerCommand.builder().note("n").build())
                .getId()).isEqualTo(88L);
        assertThatThrownBy(() -> service.trigger(new DdcAiIdQuery(7L), key,
                DdcAiOptimizationTriggerCommand.builder().note("other").build()))
                .isInstanceOfSatisfying(CommonException.class, failure -> assertThat(failure.getCode()).isEqualTo(56305));

        when(modelProvider.getIfAvailable()).thenReturn(null);
        assertThatThrownBy(() -> service.trigger(new DdcAiIdQuery(7L), key,
                DdcAiOptimizationTriggerCommand.builder().note("n").build()))
                .isInstanceOfSatisfying(CommonException.class, failure -> assertThat(failure.getCode()).isEqualTo(56322));
    }

    private void stubPrepared() {
        when(policyRepository.findActiveByResourceId(7L)).thenReturn(Optional.of(policy(DdcAiPolicyStatePersistenceEnum.ARMED)));
        when(versionRepository.findActiveById(99L)).thenReturn(Optional.of(DdcAiVersionBO.builder().id(99L).resourceId(7L)
                .versionNo(4L).kind(DdcAiKindEnum.PROMPT).promptText("base").contentSha256(BASE_HASH).build()));
        when(versionRepository.findActiveById(98L)).thenReturn(Optional.of(DdcAiVersionBO.builder().id(98L).resourceId(7L)
                .versionNo(3L).kind(DdcAiKindEnum.PROMPT).promptText("old").contentSha256("c".repeat(64)).build()));
        when(resourceService.get(new DdcAiIdQuery(7L))).thenReturn(resourceResult("5"));
        when(promptContent.prepare(any())).thenReturn(prepared(BASE_HASH));
        when(feedbackService.worstSamples(eq(7L), any(), any(), any(), eq(20))).thenReturn(List.of());
    }

    private void stubModel(String text) {
        when(promptOptimizer.compose(any(), anyString(), anyInt(), any(Double.class))).thenReturn(
                new top.egon.cola.component.tianshu.admin.model.dto.DdcAiModelRequestDTO("qwen-plus", "system", "user",
                        8192, 0.2d, "OPTIMIZE"));
        when(modelPort.complete(any())).thenReturn(new DdcAiModelResultDTO(text, 100, 50, "qwen-plus", "stop"));
        when(promptOptimizer.parse("{\"promptText\":\"improved\"}")).thenReturn(
                DdcAiContentDTO.builder().promptText("improved").nodes(List.of()).build());
    }

    private DdcAiOptimizationRunBO lastRun() {
        return runWrites.get(runWrites.size() - 1);
    }

    private DdcAiOptimizationPolicyBO lastPolicy() {
        return policyWrites.get(policyWrites.size() - 1);
    }

    private static DdcAiResourceBO resource(Long currentVersionId) {
        return DdcAiResourceBO.builder().id(7L).version(5L).kind(DdcAiKindEnum.PROMPT).resourceKey("refund")
                .displayName("Refund").description("").currentVersionId(currentVersionId).build();
    }

    private static DdcAiResourceResult resourceResult(String revision) {
        return DdcAiResourceResult.builder()
                .resource(DdcAiResourceSummaryDTO.builder().id("7").revision(revision).displayName("Refund")
                        .description("").build())
                .draft(DdcAiContentDTO.builder().promptText("base").nodes(List.of()).build())
                .build();
    }

    private static DdcAiContentStrategy.PreparedContent prepared(String hash) {
        return new DdcAiContentStrategy.PreparedContent("base", List.of(), hash, Map.of(), 4L, 0);
    }

    private static DdcAiOptimizationPolicyBO policy(DdcAiPolicyStatePersistenceEnum state) {
        return DdcAiOptimizationPolicyBO.builder().id(5L).version(3L).resourceId(7L).enabled(true)
                .scoreThreshold((short) 80).minSamples(20).evaluationWindow(Duration.ofHours(24))
                .maxIterations((short) 5).cooldown(Duration.ofHours(1)).autoPublish(false).judgeEnabled(true)
                .optimizerModelAlias("qwen-plus").judgeModelAlias("qwen-turbo").optimizerInstruction("")
                .state(state).iterationsDone((short) 0).consecutiveFailures((short) 0)
                .convergedAt(state == DdcAiPolicyStatePersistenceEnum.CONVERGED ? NOW : null)
                .build();
    }

    private static DdcAiOptimizationRunBO run(DdcAiRunStatePersistenceEnum state) {
        return DdcAiOptimizationRunBO.builder().id(88L).version(1L).resourceId(7L).policyId(5L).runNo(4L)
                .iterationNo((short) 1).trigger(DdcAiRunTriggerPersistenceEnum.SCHEDULED).state(state)
                .baseVersionId(99L).sampleCount(25).thresholdSnapshot((short) 80)
                .windowStart(NOW.minus(Duration.ofHours(24))).windowEnd(NOW).modelAlias("qwen-plus")
                .autoPublished(false).note("")
                .leaseUntil(state == DdcAiRunStatePersistenceEnum.RUNNING ? NOW.plusSeconds(60) : null)
                .build();
    }

    private static DdcAiScoreAggregateBO aggregate(int samples, String average) {
        return DdcAiScoreAggregateBO.builder().sampleCount(samples)
                .averageScore(average == null ? null : new BigDecimal(average)).pendingJudgeCount(0)
                .windowStart(NOW.minus(Duration.ofHours(24))).windowEnd(NOW).build();
    }

    private static DdcAiOptimizationPolicyCommand command(String revision, boolean rearm) {
        return DdcAiOptimizationPolicyCommand.builder().expectedRevision(revision).enabled(true).scoreThreshold(90)
                .minSamples(10).evaluationWindow(Duration.ofHours(12)).maxIterations(3).cooldown(Duration.ofMinutes(30))
                .autoPublish(false).judgeEnabled(true).optimizerModelAlias("qwen-plus").judgeModelAlias("qwen-turbo")
                .optimizerInstruction("").rearm(rearm).build();
    }
}
