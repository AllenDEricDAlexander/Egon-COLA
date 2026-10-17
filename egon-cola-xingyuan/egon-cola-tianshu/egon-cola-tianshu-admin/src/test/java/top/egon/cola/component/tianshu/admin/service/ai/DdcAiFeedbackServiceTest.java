package top.egon.cola.component.tianshu.admin.service.ai;

import io.grpc.Context;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.tianshu.admin.config.DdcAiOptimizationProperties;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiUsageFeedbackBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionBO;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiUsageReportBatchCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiUsageReportCommand;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiEnumConverterImpl;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiUsageFeedbackResultConverterImpl;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiJudgeVerdictDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiJudgeStatePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiOutcomePersistenceEnum;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiRunErrorCodeEnum;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiUsageReportReceiptResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiUsageFeedbackRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionRepository;
import top.egon.cola.component.tianshu.admin.security.rpc.DdcServicePrincipal;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiFeedbackServiceImpl;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiScopeValidator;
import top.egon.cola.component.tianshu.model.ai.DdcAiOutcomeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportStatusEnum;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcAiFeedbackServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-17T05:00:00Z");

    private final DdcAiUsageFeedbackRepository feedbackRepository = mock(DdcAiUsageFeedbackRepository.class);
    private final DdcAiResourceRepository resourceRepository = mock(DdcAiResourceRepository.class);
    private final DdcAiVersionRepository versionRepository = mock(DdcAiVersionRepository.class);
    private final DdcAiScopeValidator scopeValidator = mock(DdcAiScopeValidator.class);
    private final DdcAiOptimizationProperties properties = new DdcAiOptimizationProperties();
    private final TransactionTemplate transactions = new TransactionTemplate(mock(PlatformTransactionManager.class));
    private final DdcAiFeedbackServiceImpl service = new DdcAiFeedbackServiceImpl(feedbackRepository,
            resourceRepository, versionRepository, scopeValidator, new DdcAiEnumConverterImpl(),
            new DdcAiUsageFeedbackResultConverterImpl(),
            new ValidationUtils(Validation.buildDefaultValidatorFactory().getValidator()),
            transactions, transactions, properties, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void knownResourceAndVersion() {
        MDC.put("tenantId", "42");
        when(resourceRepository.findActiveById(7L)).thenReturn(Optional.of(DdcAiResourceBO.builder().id(7L)
                .resourceKey("refund").namespaceCode("prod-space").bizCode("retail").envCode("prod")
                .appCode("assistant-app").build()));
        when(versionRepository.findActiveById(9L)).thenReturn(Optional.of(
                DdcAiVersionBO.builder().id(9L).resourceId(7L).versionNo(4L).build()));
        when(feedbackRepository.insert(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void acceptStoresReporterIdentityTruncatesExcerptsAndMarksPendingJudge() {
        properties.getFeedback().setMaxInputExcerptBytes(256);
        String longInput = "退".repeat(200);

        DdcAiUsageReportReceiptResult receipt = accept(report("01JAX0M7Q9K2R5T8V1W4X7Y0ZB").toBuilder()
                .inputExcerpt(longInput).build());

        assertThat(receipt.getItems()).singleElement().extracting(DdcAiUsageReportReceiptResult.ItemResult::getStatus)
                .isEqualTo(DdcAiUsageReportStatusEnum.ACCEPTED);
        ArgumentCaptor<DdcAiUsageFeedbackBO> inserted = ArgumentCaptor.forClass(DdcAiUsageFeedbackBO.class);
        verify(feedbackRepository).insert(inserted.capture());
        assertThat(inserted.getValue().getJudgeState()).isEqualTo(DdcAiJudgeStatePersistenceEnum.PENDING);
        assertThat(inserted.getValue().getOutcome()).isEqualTo(DdcAiOutcomePersistenceEnum.FAILURE);
        assertThat(inserted.getValue().getReporterCredentialId()).isEqualTo("runtime");
        assertThat(inserted.getValue().getAppCode()).isEqualTo("assistant-app");
        assertThat(inserted.getValue().getInputExcerpt()).hasSize(85);
        assertThat(inserted.getValue().getInputExcerpt().getBytes(java.nio.charset.StandardCharsets.UTF_8).length)
                .isLessThanOrEqualTo(256);
    }

    @Test
    void unscopedReporterIsAttributedToTheAuthorizedResourceScope() {
        DdcServicePrincipal unscoped = new DdcServicePrincipal("runtime", "SDK", Set.of("*"), Set.of("*"),
                Set.of("*"), Set.of("AI_REPORT"), null, null, null, 42L, "tenant-42", Set.of("*"));
        Context context = unscoped.bind(Context.current());
        Context previous = context.attach();
        try {
            service.accept(DdcAiUsageReportBatchCommand.builder()
                    .reports(List.of(report("01JAX0M7Q9K2R5T8V1W4X7Y0ZB"))).build());
        } finally {
            context.detach(previous);
        }

        ArgumentCaptor<DdcAiUsageFeedbackBO> inserted = ArgumentCaptor.forClass(DdcAiUsageFeedbackBO.class);
        verify(feedbackRepository).insert(inserted.capture());
        assertThat(inserted.getValue().getBizCode()).isEqualTo("retail");
        assertThat(inserted.getValue().getEnvCode()).isEqualTo("prod");
        assertThat(inserted.getValue().getAppCode()).isEqualTo("assistant-app");
    }

    @Test
    void explicitScoreSkipsJudging() {
        accept(report("01JAX0M7Q9K2R5T8V1W4X7Y0ZB").toBuilder().explicitScore(30).build());

        ArgumentCaptor<DdcAiUsageFeedbackBO> inserted = ArgumentCaptor.forClass(DdcAiUsageFeedbackBO.class);
        verify(feedbackRepository).insert(inserted.capture());
        assertThat(inserted.getValue().getJudgeState()).isEqualTo(DdcAiJudgeStatePersistenceEnum.NOT_REQUIRED);
        assertThat(inserted.getValue().getExplicitScore()).isEqualTo((short) 30);
    }

    @Test
    void acceptMapsUniqueViolationToDuplicateAndKeepsOrder() {
        when(feedbackRepository.insert(any()))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new DuplicateKeyException("uk_ai_usage_feedback_usage"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DdcAiUsageReportReceiptResult receipt = accept(report("01JAX0M7Q9K2R5T8V1W4X7Y0Z1"),
                report("01JAX0M7Q9K2R5T8V1W4X7Y0Z1"), report("01JAX0M7Q9K2R5T8V1W4X7Y0Z3"));

        assertThat(receipt.getItems()).extracting(DdcAiUsageReportReceiptResult.ItemResult::getStatus).containsExactly(
                DdcAiUsageReportStatusEnum.ACCEPTED, DdcAiUsageReportStatusEnum.ACCEPTED_DUPLICATE,
                DdcAiUsageReportStatusEnum.ACCEPTED);
    }

    @Test
    void acceptRejectsClockSkewInvalidFieldsAndForeignVersionsPerItem() {
        DdcAiUsageReportReceiptResult receipt = accept(
                report("01JAX0M7Q9K2R5T8V1W4X7Y0Z1").toBuilder().occurredAt(NOW.plus(Duration.ofMinutes(10))).build(),
                report("01JAX0M7Q9K2R5T8V1W4X7Y0Z2").toBuilder().occurredAt(NOW.minus(Duration.ofDays(8))).build(),
                report("bad").toBuilder().build(),
                report("01JAX0M7Q9K2R5T8V1W4X7Y0Z4").toBuilder().versionNo(5L).build(),
                report("01JAX0M7Q9K2R5T8V1W4X7Y0Z5").toBuilder().resourceKey("other").build(),
                report("01JAX0M7Q9K2R5T8V1W4X7Y0Z6").toBuilder().outputExcerpt("a\u0000b").build());

        assertThat(receipt.getItems()).extracting(DdcAiUsageReportReceiptResult.ItemResult::getReason).containsExactly(
                "CLOCK_SKEW", "CLOCK_SKEW", "INVALID_FIELD", "VERSION_MISMATCH", "RESOURCE_NOT_FOUND", "INVALID_FIELD");
        assertThat(receipt.getItems()).extracting(DdcAiUsageReportReceiptResult.ItemResult::getUsageId)
                .contains("bad");
        verify(feedbackRepository, never()).insert(any());
    }

    @Test
    void claimForJudgeMovesRowsToJudgingWithLeaseAndAttempt() {
        DdcAiUsageFeedbackBO pending = sample(DdcAiJudgeStatePersistenceEnum.PENDING, 0);
        when(feedbackRepository.findClaimableForJudge(Set.of(7L), NOW, 3, 50)).thenReturn(List.of(pending));
        when(feedbackRepository.update(any())).thenAnswer(invocation -> Optional.of(invocation.getArgument(0)));

        List<DdcAiUsageFeedbackBO> claimed = service.claimForJudge(Set.of(7L), 50);

        assertThat(claimed).singleElement().satisfies(row -> {
            assertThat(row.getJudgeState()).isEqualTo(DdcAiJudgeStatePersistenceEnum.JUDGING);
            assertThat(row.getJudgeAttempts()).isEqualTo((short) 1);
            assertThat(row.getJudgeLeaseUntil()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        });
    }

    @Test
    void completeJudgeWritesDoneAndIgnoresTakenOverSamples() {
        AtomicReference<DdcAiUsageFeedbackBO> written = new AtomicReference<>();
        when(feedbackRepository.update(any())).thenAnswer(invocation -> {
            written.set(invocation.getArgument(0));
            return Optional.of(invocation.getArgument(0));
        });
        when(feedbackRepository.findActiveById(1L)).thenReturn(Optional.of(sample(DdcAiJudgeStatePersistenceEnum.JUDGING, 1)));

        service.completeJudge(1L, 3L, new DdcAiJudgeVerdictDTO(66, "ok"));

        assertThat(written.get().getJudgeState()).isEqualTo(DdcAiJudgeStatePersistenceEnum.DONE);
        assertThat(written.get().getJudgeScore()).isEqualTo((short) 66);
        assertThat(written.get().getJudgeLeaseUntil()).isNull();
        assertThat(written.get().getJudgedAt()).isEqualTo(NOW);

        written.set(null);
        service.completeJudge(1L, 4L, new DdcAiJudgeVerdictDTO(66, "ok"));
        assertThat(written.get()).isNull();
    }

    @Test
    void failJudgeReturnsToPendingUntilTheAttemptLimit() {
        AtomicReference<DdcAiUsageFeedbackBO> written = new AtomicReference<>();
        when(feedbackRepository.update(any())).thenAnswer(invocation -> {
            written.set(invocation.getArgument(0));
            return Optional.of(invocation.getArgument(0));
        });
        when(feedbackRepository.findActiveById(1L)).thenReturn(Optional.of(sample(DdcAiJudgeStatePersistenceEnum.JUDGING, 1)));
        service.failJudge(1L, 3L, DdcAiRunErrorCodeEnum.MODEL_TIMEOUT);
        assertThat(written.get().getJudgeState()).isEqualTo(DdcAiJudgeStatePersistenceEnum.PENDING);
        assertThat(written.get().getJudgeError()).isEqualTo("MODEL_TIMEOUT");
        assertThat(written.get().getJudgeLeaseUntil()).isNull();

        when(feedbackRepository.findActiveById(1L)).thenReturn(Optional.of(sample(DdcAiJudgeStatePersistenceEnum.JUDGING, 3)));
        service.failJudge(1L, 3L, DdcAiRunErrorCodeEnum.INVALID_OUTPUT);
        assertThat(written.get().getJudgeState()).isEqualTo(DdcAiJudgeStatePersistenceEnum.FAILED);
    }

    @Test
    void purgeExpiredUsesTheConfiguredRetention() {
        when(feedbackRepository.softDeleteExpired(NOW.minus(Duration.ofDays(30)), 1000)).thenReturn(7);

        assertThat(service.purgeExpired(1000)).isEqualTo(7);
    }

    private DdcAiUsageReportReceiptResult accept(DdcAiUsageReportCommand... reports) {
        DdcServicePrincipal principal = new DdcServicePrincipal("runtime", "SDK", Set.of("assistant-app"),
                Set.of("prod"), Set.of("retail"), Set.of("AI_REPORT"), "assistant-app", "prod", "retail", 42L,
                "tenant-42", Set.of("prod-*"));
        Context context = principal.bind(Context.current());
        Context previous = context.attach();
        try {
            return service.accept(DdcAiUsageReportBatchCommand.builder().reports(List.of(reports)).build());
        } finally {
            context.detach(previous);
        }
    }

    private static DdcAiUsageReportCommand report(String usageId) {
        return DdcAiUsageReportCommand.builder()
                .usageId(usageId).namespaceCode("prod-space").resourceKey("refund").resourceId(7L).versionId(9L)
                .versionNo(4L).outcome(DdcAiOutcomeEnum.FAILURE).occurredAt(NOW.minusSeconds(30)).build();
    }

    private static DdcAiUsageFeedbackBO sample(DdcAiJudgeStatePersistenceEnum state, int attempts) {
        return DdcAiUsageFeedbackBO.builder().id(1L).version(3L).resourceId(7L).versionId(9L)
                .usageId("01JAX0M7Q9K2R5T8V1W4X7Y0ZB").outcome(DdcAiOutcomePersistenceEnum.FAILURE)
                .judgeState(state).judgeAttempts((short) attempts)
                .judgeLeaseUntil(state == DdcAiJudgeStatePersistenceEnum.JUDGING ? NOW : null)
                .occurredAt(NOW).build();
    }
}
