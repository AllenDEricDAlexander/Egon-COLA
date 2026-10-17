package top.egon.cola.component.tianshu.admin.service.ai;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiPublicationBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionNodeBO;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiPublishCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiRollbackCommand;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiDraftNodeConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiPublicationConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiResourceConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiResourceResultConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiVersionConverter;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiVersionSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiPublicationOperationEnum;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiIdQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiPublicationPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiVersionPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiVersionQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiPublicationResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiVersionResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAiDraftNodeRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiPublicationRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionNodeRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionRepository;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiScopeValidator;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiContentStrategy;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcSkillContentStrategy;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiResourceServiceImpl;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiSkillDraftUploadCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiSkillPublishCommand;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiPublicationReceiptResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiVersionHeadResult;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DdcAiVersionServiceTest {

    private final top.egon.cola.component.tianshu.admin.service.ai.notification.DdcAiPublicationNotifier publicationNotifier =
            org.mockito.Mockito.mock(top.egon.cola.component.tianshu.admin.service.ai.notification.DdcAiPublicationNotifier.class);
    @SuppressWarnings("unchecked")
    private final org.springframework.beans.factory.ObjectProvider<DdcAiOptimizationService> optimizationService =
            org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);

    private static final long TENANT_ID = 1001L;
    private static final long RESOURCE_ID = 810001L;
    private static final long VERSION_ID = 820001L;
    private static final long PUBLICATION_ID = 830001L;
    private static final String REQUEST_ID = "publish_key_000001";
    private static final String PROMPT = "publish this exact prompt\n";
    private static final String HASH = "a".repeat(64);
    private static final Instant CREATED_AT = Instant.parse("2026-09-29T10:00:00Z");
    private static final DdcAiScopeDTO SCOPE = new DdcAiScopeDTO("retail", "dev-space", "dev", "assistant-app");

    @Mock private DdcAiResourceRepository resourceRepository;
    @Mock private DdcAiDraftNodeRepository draftNodeRepository;
    @Mock private DdcAiScopeValidator scopeValidator;
    @Mock private DdcAiResourceConverter resourceConverter;
    @Mock private DdcAiDraftNodeConverter draftNodeConverter;
    @Mock private DdcAiResourceResultConverter resourceResultConverter;
    @Mock private Map<DdcAiKindEnum, DdcAiContentStrategy> contentStrategyRegistry;
    @Mock private DdcAiContentStrategy promptStrategy;
    @Mock private DdcAiContentStrategy skillStrategy;
    @Mock private TransactionTemplate readTransactionTemplate;
    @Mock private TransactionTemplate commandTransactionTemplate;
    @Mock private DdcAiVersionRepository versionRepository;
    @Mock private DdcAiVersionNodeRepository versionNodeRepository;
    @Mock private DdcAiPublicationRepository publicationRepository;
    @Mock private DdcAiVersionConverter versionConverter;
    @Mock private DdcAiPublicationConverter publicationConverter;
    @Mock private Rbac3UserDetails userDetails;
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private DdcAiResourceServiceImpl service;

    @BeforeEach
    void setUp() {
        MDC.put("tenantId", Long.toString(TENANT_ID));
        service = new DdcAiResourceServiceImpl(resourceRepository, draftNodeRepository, scopeValidator,
                resourceConverter, draftNodeConverter, versionRepository, versionNodeRepository,
                publicationRepository, versionConverter, publicationConverter, resourceResultConverter,
                meterRegistry, contentStrategyRegistry, readTransactionTemplate, commandTransactionTemplate,
                publicationNotifier, optimizationService);
    }

    @AfterEach
    void clearSecurityContext() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        meterRegistry.close();
    }

    @Test
    void publishFreezesTheSnapshotAndARequestThatWaitsForTheRootLockReplaysTheReceipt() {
        prepareCommandContext();
        when(promptStrategy.kind()).thenReturn(DdcAiKindEnum.PROMPT);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 3L);
        DdcAiPublishCommand command = DdcAiPublishCommand.builder()
                .expectedRevision("3").changeNote("first stable version").build();
        DdcAiContentStrategy.PreparedContent prepared = preparedPrompt(PROMPT);
        DdcAiVersionBO version = DdcAiVersionBO.builder().resourceId(RESOURCE_ID).versionNo(3L)
                .kind(DdcAiKindEnum.PROMPT).displayName(resource.getDisplayName()).description(resource.getDescription())
                .promptText(PROMPT).contentSha256(HASH).contentBytes((long) PROMPT.getBytes(StandardCharsets.UTF_8).length)
                .nodeCount(0).changeNote(command.getChangeNote()).build();
        AtomicReference<DdcAiPublicationBO> committedReceipt = new AtomicReference<>();
        AtomicBoolean simulateConcurrentCommit = new AtomicBoolean();
        AtomicInteger requestLookups = new AtomicInteger();
        DdcAiPublicationResult receiptResult = DdcAiPublicationResult.builder()
                .publicationId(PUBLICATION_ID).resourceId(RESOURCE_ID).operation("PUBLISH")
                .previousVersionId(resource.getCurrentVersionId()).targetVersionId(VERSION_ID)
                .targetVersionNo(3L).resourceRevision(4L).targetCreated(false).createdAt(CREATED_AT).build();

        when(resourceRepository.findActiveById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(resourceRepository.findActiveForUpdateById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(resourceRepository.update(resource)).thenAnswer(invocation -> {
            resource.setVersion(resource.getVersion() + 1);
            return Optional.of(resource);
        });
        when(resourceConverter.toInternalLong("3")).thenReturn(3L);
        when(contentStrategyRegistry.get(DdcAiKindEnum.PROMPT)).thenReturn(promptStrategy);
        when(promptStrategy.prepare(any(DdcAiContentDTO.class))).thenReturn(prepared);
        when(draftNodeRepository.findActiveByResourceId(RESOURCE_ID)).thenReturn(List.of());
        when(publicationRepository.findByTenantRequestId(REQUEST_ID)).thenAnswer(invocation -> {
            if (simulateConcurrentCommit.get() && requestLookups.getAndIncrement() == 0) return Optional.empty();
            return Optional.ofNullable(committedReceipt.get());
        });
        when(versionRepository.insertVersion(any(DdcAiVersionBO.class))).thenAnswer(invocation -> {
            DdcAiVersionBO inserted = invocation.getArgument(0);
            return inserted.setId(VERSION_ID).setCreateTime(CREATED_AT);
        });
        when(versionNodeRepository.insertAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(publicationRepository.insertPublication(any(DdcAiPublicationBO.class))).thenAnswer(invocation -> {
            DdcAiPublicationBO inserted = invocation.getArgument(0);
            inserted.setId(PUBLICATION_ID).setCreateTime(CREATED_AT);
            committedReceipt.set(inserted);
            return inserted;
        });
        when(publicationConverter.toResult(any(DdcAiPublicationBO.class))).thenReturn(receiptResult);

        DdcAiPublicationResult published = service.publish(new DdcAiIdQuery(RESOURCE_ID), REQUEST_ID, command);

        assertThat(published).isSameAs(receiptResult);
        assertThat(resource.getVersion()).isEqualTo(4L);
        assertThat(resource.getCurrentVersionId()).isEqualTo(VERSION_ID);
        assertThat(resource.getCurrentVersionNo()).isEqualTo(3L);
        assertThat(resource.getLastVersionNo()).isEqualTo(3L);
        assertThat(resource.getDraftPrompt()).isEqualTo(PROMPT);
        assertThat(committedReceipt.get().getTargetVersionNo()).isEqualTo(3L);
        assertThat(committedReceipt.get().getRequestSha256()).matches("^[0-9a-f]{64}$");
        verify(publicationNotifier).enqueue(org.mockito.ArgumentMatchers.same(committedReceipt.get()),
                org.mockito.ArgumentMatchers.same(resource), org.mockito.ArgumentMatchers.eq(prepared.contentHash()),
                org.mockito.ArgumentMatchers.anyString());

        simulateConcurrentCommit.set(true);
        requestLookups.set(0);
        DdcAiPublicationResult replay = service.publish(new DdcAiIdQuery(RESOURCE_ID), REQUEST_ID, command);

        assertThat(replay).isSameAs(published);
        assertThat(resource.getVersion()).isEqualTo(4L);
        DdcAiResourceBO otherResource = resource(RESOURCE_ID + 1, 3L).setResourceKey("review-invoice");
        when(resourceRepository.findActiveById(RESOURCE_ID + 1)).thenReturn(Optional.of(otherResource));
        assertThatThrownBy(() -> service.publish(new DdcAiIdQuery(RESOURCE_ID + 1), REQUEST_ID, command))
                .isInstanceOf(CommonException.class)
                .satisfies(failure -> assertThat(((CommonException) failure).getCode()).isEqualTo(56305));
        assertThat(metricCount("PUBLISH", "success")).isEqualTo(2.0);
        assertThat(metricCount("PUBLISH", "failure")).isEqualTo(1.0);
        verify(versionRepository, times(1)).insertVersion(any(DdcAiVersionBO.class));
        verify(publicationRepository, times(1)).insertPublication(any(DdcAiPublicationBO.class));
        verify(resourceRepository, times(1)).update(resource);
        verify(resourceRepository, times(2)).findActiveForUpdateById(RESOURCE_ID);
        verify(resourceRepository, never()).findActiveForUpdateById(RESOURCE_ID + 1);

        InOrder lockOrder = inOrder(resourceRepository, scopeValidator);
        lockOrder.verify(resourceRepository).findActiveById(RESOURCE_ID);
        lockOrder.verify(scopeValidator).assertCanPublish(resource);
        lockOrder.verify(resourceRepository).findActiveForUpdateById(RESOURCE_ID);
    }

    @Test
    void reusedTenantRequestIdWithDifferentCommandConflictsBeforeRootMutation() {
        prepareCommandContext();
        DdcAiResourceBO resource = resource(RESOURCE_ID, 3L);
        DdcAiPublishCommand command = DdcAiPublishCommand.builder()
                .expectedRevision("4").changeNote("different command").build();
        DdcAiPublicationBO existing = DdcAiPublicationBO.builder().requestSha256("f".repeat(64))
                .targetVersionNo(1L).createTime(CREATED_AT).build();
        when(resourceRepository.findActiveById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(publicationRepository.findByTenantRequestId(REQUEST_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.publish(new DdcAiIdQuery(RESOURCE_ID), REQUEST_ID, command))
                .isInstanceOf(CommonException.class)
                .satisfies(failure -> assertThat(((CommonException) failure).getCode()).isEqualTo(56305));

        assertThat(metricCount("PUBLISH", "failure")).isEqualTo(1.0);
        verify(resourceRepository, never()).findActiveForUpdateById(RESOURCE_ID);
        verify(resourceRepository, never()).update(any(DdcAiResourceBO.class));
        verify(versionRepository, never()).insertVersion(any(DdcAiVersionBO.class));
        verify(publicationRepository, never()).insertPublication(any(DdcAiPublicationBO.class));
    }

    @Test
    void rollbackMovesOnlyThePointerAndPreservesDraftAndVersionCounter() {
        prepareCommandContext();
        when(promptStrategy.kind()).thenReturn(DdcAiKindEnum.PROMPT);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 3L).setDraftPrompt("draft edited after version 2")
                .setLastVersionNo(6L).setCurrentVersionId(820006L).setCurrentVersionNo(6L);
        DdcAiVersionBO target = DdcAiVersionBO.builder().id(820002L).resourceId(RESOURCE_ID).versionNo(2L)
                .kind(DdcAiKindEnum.PROMPT).displayName("Review Order").description("").promptText(PROMPT)
                .contentSha256(HASH).contentBytes((long) PROMPT.getBytes(StandardCharsets.UTF_8).length)
                .nodeCount(0).changeNote("version 2").createTime(CREATED_AT).build();
        DdcAiRollbackCommand command = DdcAiRollbackCommand.builder().expectedRevision("3").versionNo("2")
                .changeNote("restore version 2").build();
        DdcAiPublicationResult receiptResult = DdcAiPublicationResult.builder()
                .publicationId(PUBLICATION_ID).resourceId(RESOURCE_ID).operation("ROLLBACK")
                .previousVersionId(820006L).targetVersionId(820002L).targetVersionNo(2L)
                .resourceRevision(4L).targetCreated(false).createdAt(CREATED_AT).build();
        when(resourceRepository.findActiveById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(resourceRepository.findActiveForUpdateById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(resourceRepository.update(resource)).thenAnswer(invocation -> {
            resource.setVersion(resource.getVersion() + 1);
            return Optional.of(resource);
        });
        when(resourceConverter.toInternalLong("3")).thenReturn(3L);
        when(publicationRepository.findByTenantRequestId(REQUEST_ID))
                .thenReturn(Optional.empty(), Optional.empty());
        when(versionRepository.findByResourceIdAndVersionNo(RESOURCE_ID, 2L)).thenReturn(Optional.of(target));
        when(versionNodeRepository.findTreeByVersionId(820002L)).thenReturn(List.of());
        when(contentStrategyRegistry.get(DdcAiKindEnum.PROMPT)).thenReturn(promptStrategy);
        when(promptStrategy.prepare(any(DdcAiContentDTO.class))).thenReturn(preparedPrompt(PROMPT));
        when(publicationRepository.insertPublication(any(DdcAiPublicationBO.class))).thenAnswer(invocation -> {
            DdcAiPublicationBO inserted = invocation.getArgument(0);
            inserted.setId(PUBLICATION_ID).setCreateTime(CREATED_AT);
            return inserted;
        });
        when(publicationConverter.toResult(any(DdcAiPublicationBO.class))).thenReturn(receiptResult);

        DdcAiPublicationResult result = service.rollback(new DdcAiIdQuery(RESOURCE_ID), REQUEST_ID, command);

        assertThat(result).isSameAs(receiptResult);
        assertThat(resource.getVersion()).isEqualTo(4L);
        assertThat(resource.getCurrentVersionId()).isEqualTo(820002L);
        assertThat(resource.getCurrentVersionNo()).isEqualTo(2L);
        assertThat(resource.getLastVersionNo()).isEqualTo(6L);
        assertThat(resource.getDraftPrompt()).isEqualTo("draft edited after version 2");
        assertThat(metricCount("ROLLBACK", "success")).isEqualTo(1.0);
        verify(publicationNotifier).enqueue(any(DdcAiPublicationBO.class), org.mockito.ArgumentMatchers.same(resource),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(versionRepository, never()).insertVersion(any(DdcAiVersionBO.class));
        verify(versionNodeRepository, never()).insertAll(any());
        verify(draftNodeRepository, never()).findActiveByResourceId(RESOURCE_ID);
        verify(resourceRepository).update(resource);
    }

    @Test
    void getVersionReturnsTheFrozenSkillTreeWithoutReadingTheCurrentDraft() {
        executeTransactionCallbacksInline(readTransactionTemplate);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 9L).setKind(DdcAiKindEnum.SKILL)
                .setDraftPrompt("current draft header");
        List<DdcAiNodeDTO> snapshotNodes = List.of(
                DdcAiNodeDTO.builder().path("").nodeType(DdcAiNodeTypeEnum.DIRECTORY).build(),
                DdcAiNodeDTO.builder().path("README.txt").nodeType(DdcAiNodeTypeEnum.FILE)
                        .content("frozen skill text\n").build());
        DdcAiContentDTO snapshot = DdcAiContentDTO.builder().promptText(null).nodes(snapshotNodes).build();
        DdcAiVersionBO version;
        DdcAiVersionSummaryDTO summary;
        List<DdcAiVersionNodeBO> persistedNodes;
        DdcAiContentStrategy.PreparedContent prepared;
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            DdcSkillContentStrategy canonicalStrategy = new DdcSkillContentStrategy(
                    new ValidationUtils(factory.getValidator()));
            prepared = canonicalStrategy.prepare(snapshot);
            version = DdcAiVersionBO.builder().id(VERSION_ID).resourceId(RESOURCE_ID).versionNo(4L)
                    .kind(DdcAiKindEnum.SKILL).displayName("Frozen Skill").description("immutable description")
                    .promptText(null).contentSha256(prepared.contentHash()).contentBytes(prepared.contentBytes())
                    .nodeCount(prepared.nodeCount()).changeNote("version 4").createTime(CREATED_AT).build();
            summary = DdcAiVersionSummaryDTO.builder().id(VERSION_ID).resourceId(RESOURCE_ID).versionNo(4L)
                    .kind("SKILL").displayName("Frozen Skill").description("immutable description")
                    .contentSha256(prepared.contentHash()).contentBytes(prepared.contentBytes())
                    .nodeCount(prepared.nodeCount()).changeNote("version 4").createdAt(CREATED_AT).build();
            persistedNodes = List.of(
                    DdcAiVersionNodeBO.builder().versionId(VERSION_ID).path("").parentPath(null)
                            .nodeType(DdcAiNodeTypeEnum.DIRECTORY).content(null).contentSha256(null)
                            .contentBytes(0L).build(),
                    DdcAiVersionNodeBO.builder().versionId(VERSION_ID).path("README.txt").parentPath("")
                            .nodeType(DdcAiNodeTypeEnum.FILE).content("frozen skill text\n")
                            .contentSha256(prepared.fileSha256ByPath().get("README.txt"))
                            .contentBytes((long) "frozen skill text\n".getBytes(StandardCharsets.UTF_8).length)
                            .build());
            when(skillStrategy.kind()).thenReturn(DdcAiKindEnum.SKILL);
            when(skillStrategy.prepare(any(DdcAiContentDTO.class))).thenReturn(prepared);
        }
        when(resourceRepository.findActiveForShareById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(versionRepository.findByResourceIdAndVersionNo(RESOURCE_ID, 4L)).thenReturn(Optional.of(version));
        when(versionNodeRepository.findTreeByVersionId(VERSION_ID)).thenReturn(persistedNodes);
        when(contentStrategyRegistry.get(DdcAiKindEnum.SKILL)).thenReturn(skillStrategy);
        when(versionConverter.toVersionSummary(version)).thenReturn(summary);

        DdcAiVersionResult result = service.getVersion(new DdcAiVersionQuery(RESOURCE_ID, 4L));

        assertThat(result.getVersion()).isSameAs(summary);
        assertThat(result.getContent().getPromptText()).isNull();
        assertThat(result.getContent().getNodes()).containsExactlyElementsOf(snapshotNodes);
        ArgumentCaptor<DdcAiContentDTO> contentCaptor = ArgumentCaptor.forClass(DdcAiContentDTO.class);
        verify(skillStrategy).prepare(contentCaptor.capture());
        assertThat(contentCaptor.getValue().getNodes()).containsExactlyElementsOf(snapshotNodes);
        verifyNoInteractions(draftNodeRepository, draftNodeConverter);
    }

    @Test
    void getPublishedVersionSelectsCurrentOrFixedVersionFromTheSameRootWithoutReadingDrafts() {
        executeTransactionCallbacksInline(readTransactionTemplate);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 9L).setCurrentVersionId(820004L)
                .setCurrentVersionNo(4L).setLastVersionNo(4L);
        DdcAiVersionBO current = promptVersion(820004L, 4L);
        DdcAiVersionBO fixed = promptVersion(820003L, 3L);
        DdcAiVersionSummaryDTO currentSummary = promptSummary(current);
        DdcAiVersionSummaryDTO fixedSummary = promptSummary(fixed);
        when(resourceRepository.findActiveForShareById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(versionRepository.findByResourceIdAndVersionNo(RESOURCE_ID, 4L)).thenReturn(Optional.of(current));
        when(versionRepository.findByResourceIdAndVersionNo(RESOURCE_ID, 3L)).thenReturn(Optional.of(fixed));
        when(versionNodeRepository.findTreeByVersionId(820004L)).thenReturn(List.of());
        when(versionNodeRepository.findTreeByVersionId(820003L)).thenReturn(List.of());
        when(contentStrategyRegistry.get(DdcAiKindEnum.PROMPT)).thenReturn(promptStrategy);
        when(promptStrategy.kind()).thenReturn(DdcAiKindEnum.PROMPT);
        when(promptStrategy.prepare(any(DdcAiContentDTO.class))).thenReturn(preparedPrompt(PROMPT));
        when(versionConverter.toVersionSummary(current)).thenReturn(currentSummary);
        when(versionConverter.toVersionSummary(fixed)).thenReturn(fixedSummary);

        DdcAiVersionResult currentResult = service.getPublishedVersion(
                new DdcAiResourceQuery(SCOPE.namespaceCode(), "review-order", null, RESOURCE_ID),
                DdcAiKindEnum.PROMPT);
        DdcAiVersionResult fixedResult = service.getPublishedVersion(
                new DdcAiResourceQuery(SCOPE.namespaceCode(), "review-order", 3L, RESOURCE_ID),
                DdcAiKindEnum.PROMPT);

        assertThat(currentResult.getVersion()).isSameAs(currentSummary);
        assertThat(fixedResult.getVersion()).isSameAs(fixedSummary);
        assertThat(currentResult.getContent().getPromptText()).isEqualTo(PROMPT);
        assertThat(fixedResult.getContent().getPromptText()).isEqualTo(PROMPT);
        verify(resourceRepository, times(2)).findActiveForShareById(RESOURCE_ID);
        verify(scopeValidator, times(2)).assertCanRead(resource);
        verify(versionRepository).findByResourceIdAndVersionNo(RESOURCE_ID, 4L);
        verify(versionRepository).findByResourceIdAndVersionNo(RESOURCE_ID, 3L);
        verify(readTransactionTemplate, times(2)).execute(any(TransactionCallback.class));
        verifyNoInteractions(draftNodeRepository, draftNodeConverter);
    }

    @Test
    void getPublishedVersionRejectsAKeyOrKindThatDoesNotMatchTheStableRoot() {
        executeTransactionCallbacksInline(readTransactionTemplate);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 9L).setKind(DdcAiKindEnum.PROMPT);
        when(resourceRepository.findActiveForShareById(RESOURCE_ID)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> service.getPublishedVersion(
                new DdcAiResourceQuery(SCOPE.namespaceCode(), "different-key", null, RESOURCE_ID),
                DdcAiKindEnum.PROMPT))
                .isInstanceOf(CommonException.class);
        assertThatThrownBy(() -> service.getPublishedVersion(
                new DdcAiResourceQuery(SCOPE.namespaceCode(), "review-order", null, RESOURCE_ID),
                DdcAiKindEnum.SKILL))
                .isInstanceOf(CommonException.class);

        verifyNoInteractions(versionRepository, versionNodeRepository, draftNodeRepository);
    }

    @Test
    void getPublishedVersionReturnsStableNotPublishedStatusWithoutReadingDrafts() {
        executeTransactionCallbacksInline(readTransactionTemplate);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 9L)
                .setCurrentVersionId(null).setCurrentVersionNo(null);
        when(resourceRepository.findActiveForShareById(RESOURCE_ID)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> service.getPublishedVersion(
                new DdcAiResourceQuery(SCOPE.namespaceCode(), "review-order", null, RESOURCE_ID),
                DdcAiKindEnum.PROMPT))
                .isInstanceOfSatisfying(CommonException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(56306));

        verify(scopeValidator).assertCanRead(resource);
        verifyNoInteractions(versionRepository, versionNodeRepository, draftNodeRepository);
    }

    @Test
    void exhaustedVersionCounterConflictsWithoutWritingAnyVersionState() {
        prepareCommandContext();
        DdcAiResourceBO resource = resource(RESOURCE_ID, 3L).setLastVersionNo(Long.MAX_VALUE);
        DdcAiPublishCommand command = DdcAiPublishCommand.builder()
                .expectedRevision("3").changeNote("counter exhausted").build();
        when(resourceRepository.findActiveById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(resourceRepository.findActiveForUpdateById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(resourceConverter.toInternalLong("3")).thenReturn(3L);
        when(publicationRepository.findByTenantRequestId(REQUEST_ID))
                .thenReturn(Optional.empty(), Optional.empty());

        assertThatThrownBy(() -> service.publish(new DdcAiIdQuery(RESOURCE_ID), REQUEST_ID, command))
                .isInstanceOf(CommonException.class)
                .satisfies(failure -> assertThat(((CommonException) failure).getCode()).isEqualTo(56314));
        assertThat(metricCount("PUBLISH", "failure")).isEqualTo(1.0);

        verify(versionRepository, never()).insertVersion(any(DdcAiVersionBO.class));
        verify(versionNodeRepository, never()).insertAll(any());
        verify(resourceRepository, never()).update(any(DdcAiResourceBO.class));
        verify(publicationRepository, never()).insertPublication(any(DdcAiPublicationBO.class));
    }

    @Test
    void versionNodeInsertFailureDoesNotAdvanceTheRootOrWriteAReceipt() {
        prepareAuthenticatedCommandUser();
        when(promptStrategy.kind()).thenReturn(DdcAiKindEnum.PROMPT);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 3L);
        DdcAiPublishCommand command = DdcAiPublishCommand.builder()
                .expectedRevision("3").changeNote("first stable version").build();
        List<DdcAiVersionBO> visibleVersions = new ArrayList<>();
        doAnswer(invocation -> {
            int visibleCountAtStart = visibleVersions.size();
            try {
                return ((TransactionCallback<?>) invocation.getArgument(0))
                        .doInTransaction(Mockito.mock(TransactionStatus.class));
            } catch (RuntimeException failure) {
                visibleVersions.subList(visibleCountAtStart, visibleVersions.size()).clear();
                throw failure;
            }
        }).when(commandTransactionTemplate).execute(any(TransactionCallback.class));
        when(resourceRepository.findActiveById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(resourceRepository.findActiveForUpdateById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(resourceConverter.toInternalLong("3")).thenReturn(3L);
        when(contentStrategyRegistry.get(DdcAiKindEnum.PROMPT)).thenReturn(promptStrategy);
        when(promptStrategy.prepare(any(DdcAiContentDTO.class))).thenReturn(preparedPrompt(PROMPT));
        when(draftNodeRepository.findActiveByResourceId(RESOURCE_ID)).thenReturn(List.of());
        when(publicationRepository.findByTenantRequestId(REQUEST_ID))
                .thenReturn(Optional.empty(), Optional.empty());
        when(versionRepository.insertVersion(any(DdcAiVersionBO.class))).thenAnswer(invocation -> {
            DdcAiVersionBO inserted = ((DdcAiVersionBO) invocation.getArgument(0))
                    .setId(VERSION_ID).setCreateTime(CREATED_AT);
            visibleVersions.add(inserted);
            return inserted;
        });
        when(versionNodeRepository.insertAll(any())).thenThrow(new DataIntegrityViolationException("node insert failed"));

        assertThatThrownBy(() -> service.publish(new DdcAiIdQuery(RESOURCE_ID), REQUEST_ID, command))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("node insert failed");

        assertThat(metricCount("PUBLISH", "failure")).isEqualTo(1.0);
        assertThat(resource.getVersion()).isEqualTo(3L);
        assertThat(resource.getCurrentVersionId()).isEqualTo(820002L);
        assertThat(resource.getLastVersionNo()).isEqualTo(2L);
        assertThat(visibleVersions).isEmpty();
        verify(versionRepository).insertVersion(any(DdcAiVersionBO.class));
        verify(versionNodeRepository).insertAll(any());
        verify(resourceRepository, never()).update(any(DdcAiResourceBO.class));
        verify(publicationRepository, never()).insertPublication(any(DdcAiPublicationBO.class));
        verify(commandTransactionTemplate).execute(any(TransactionCallback.class));
    }

    @Test
    void pageVersionsReturnsSummariesWithoutLoadingSnapshotNodes() {
        executeTransactionCallbacksInline(readTransactionTemplate);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 9L);
        DdcAiVersionBO version = DdcAiVersionBO.builder().id(VERSION_ID).resourceId(RESOURCE_ID).versionNo(4L)
                .kind(DdcAiKindEnum.PROMPT).displayName("Review Order").description("").contentSha256(HASH)
                .contentBytes(12L).nodeCount(0).changeNote("version 4").createTime(CREATED_AT).build();
        DdcAiVersionSummaryDTO summary = DdcAiVersionSummaryDTO.builder().id(VERSION_ID).resourceId(RESOURCE_ID)
                .versionNo(4L).kind("PROMPT").displayName("Review Order").description("")
                .contentSha256(HASH).contentBytes(12L).nodeCount(0).changeNote("version 4")
                .createdAt(CREATED_AT).build();
        DdcAiVersionPageQuery query = DdcAiVersionPageQuery.builder()
                .resourceId(RESOURCE_ID).pageNo(2).pageSize(10).build();
        when(resourceRepository.findActiveForShareById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(versionRepository.pageByResourceId(RESOURCE_ID, new PageQuery(2, 10))).thenReturn(
                DdcPageResult.<DdcAiVersionBO>builder().records(List.of(version)).total(11L)
                        .pageNo(2).pageSize(10).build());
        when(versionConverter.toVersionSummary(version)).thenReturn(summary);

        DdcPageResult<DdcAiVersionSummaryDTO> page = service.pageVersions(query);

        assertThat(page.getRecords()).containsExactly(summary);
        assertThat(page.getTotal()).isEqualTo(11L);
        assertThat(page.getPageNo()).isEqualTo(2);
        verifyNoInteractions(versionNodeRepository);
    }

    @Test
    void pagePublicationsReturnsTheJoinedTargetVersionNumber() {
        executeTransactionCallbacksInline(readTransactionTemplate);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 9L);
        DdcAiPublicationBO publication = DdcAiPublicationBO.builder().id(PUBLICATION_ID).resourceId(RESOURCE_ID)
                .operation(DdcAiPublicationOperationEnum.PUBLISH).requestId(REQUEST_ID).requestSha256(HASH)
                .previousVersionId(820002L).targetVersionId(VERSION_ID).targetVersionNo(4L)
                .resultRevision(8L).targetCreated(false).createTime(CREATED_AT).build();
        DdcAiPublicationResult result = DdcAiPublicationResult.builder().publicationId(PUBLICATION_ID)
                .resourceId(RESOURCE_ID).operation("PUBLISH").previousVersionId(820002L)
                .targetVersionId(VERSION_ID).targetVersionNo(4L).resourceRevision(8L)
                .targetCreated(false).createdAt(CREATED_AT).build();
        DdcAiPublicationPageQuery query = DdcAiPublicationPageQuery.builder()
                .resourceId(RESOURCE_ID).pageNo(1).pageSize(20).build();
        when(resourceRepository.findActiveForShareById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(publicationRepository.pageByResourceId(RESOURCE_ID, null, new PageQuery(1, 20))).thenReturn(
                DdcPageResult.<DdcAiPublicationBO>builder().records(List.of(publication)).total(1L)
                        .pageNo(1).pageSize(20).build());
        when(publicationConverter.toResult(publication)).thenReturn(result);

        DdcPageResult<DdcAiPublicationResult> page = service.pagePublications(query);

        assertThat(page.getRecords()).containsExactly(result);
        assertThat(page.getRecords().get(0).getTargetVersionNo()).isEqualTo(4L);
    }

    @Test
    void getVersionHeadReflectsCurrentVersionOrUnpublishedRoot() {
        executeTransactionCallbacksInline(readTransactionTemplate);
        DdcAiResourceBO published = resource(RESOURCE_ID, 9L).setCurrentVersionId(820004L).setCurrentVersionNo(4L);
        DdcAiResourceBO unpublished = resource(RESOURCE_ID + 1, 2L).setCurrentVersionId(null).setCurrentVersionNo(null);
        when(resourceRepository.findActiveForShareById(RESOURCE_ID)).thenReturn(Optional.of(published));
        when(resourceRepository.findActiveForShareById(RESOURCE_ID + 1)).thenReturn(Optional.of(unpublished));
        when(versionRepository.findActiveById(820004L)).thenReturn(Optional.of(promptVersion(820004L, 4L)));

        DdcAiVersionHeadResult head = service.getVersionHead(
                new DdcAiResourceQuery(SCOPE.namespaceCode(), "review-order", null, RESOURCE_ID));
        DdcAiVersionHeadResult empty = service.getVersionHead(
                new DdcAiResourceQuery(SCOPE.namespaceCode(), "review-order", null, RESOURCE_ID + 1));

        assertThat(head.getPublished()).isTrue();
        assertThat(head.getVersionId()).isEqualTo(820004L);
        assertThat(head.getVersionNo()).isEqualTo(4L);
        assertThat(head.getContentSha256()).isEqualTo(HASH);
        assertThat(head.getResourceRevision()).isEqualTo(9L);
        assertThat(empty.getPublished()).isFalse();
        assertThat(empty.getVersionId()).isNull();
        assertThat(empty.getResourceRevision()).isEqualTo(2L);
        verify(scopeValidator).assertCanRead(published);
        verifyNoInteractions(draftNodeRepository);
        assertThatThrownBy(() -> service.getVersionHead(
                new DdcAiResourceQuery(SCOPE.namespaceCode(), "other-key", null, RESOURCE_ID)))
                .isInstanceOfSatisfying(CommonException.class, failure -> assertThat(failure.getCode()).isEqualTo(56302));
        assertThatThrownBy(() -> service.getVersionHead(
                new DdcAiResourceQuery(SCOPE.namespaceCode(), "review-order", 4L, RESOURCE_ID)))
                .isInstanceOfSatisfying(CommonException.class, failure -> assertThat(failure.getCode()).isEqualTo(56301));
    }

    @Test
    void replaceSkillDraftRejectsAPromptRootBeforeWritingTheDraft() {
        executeTransactionCallbacksInline(commandTransactionTemplate);
        DdcAiResourceBO prompt = resource(RESOURCE_ID, 3L);
        when(resourceRepository.findActiveById(RESOURCE_ID)).thenReturn(Optional.of(prompt));

        assertThatThrownBy(() -> service.replaceSkillDraft(DdcAiSkillDraftUploadCommand.builder()
                .namespaceCode(SCOPE.namespaceCode()).resourceKey("review-order").resourceId(RESOURCE_ID)
                .expectedRevision("3").displayName("Refund").description("")
                .content(DdcAiContentDTO.builder().nodes(List.of()).build()).build()))
                .isInstanceOfSatisfying(CommonException.class, failure -> assertThat(failure.getCode()).isEqualTo(56308));

        verify(scopeValidator).assertCanWrite(prompt);
        verify(resourceRepository, never()).findActiveForUpdateById(any());
        verify(resourceRepository, never()).update(any());
        verifyNoInteractions(draftNodeRepository);
    }

    @Test
    void publishSkillDraftChecksIdentityAndReturnsTheTargetDigestWithoutAReadPermission() {
        prepareCommandContext();
        executeTransactionCallbacksInline(readTransactionTemplate);
        String digest = "b".repeat(64);
        when(skillStrategy.kind()).thenReturn(DdcAiKindEnum.SKILL);
        DdcAiResourceBO skill = resource(RESOURCE_ID, 7L).setKind(DdcAiKindEnum.SKILL).setDraftPrompt(null)
                .setResourceKey("refund-skill");
        when(resourceRepository.findActiveById(RESOURCE_ID)).thenReturn(Optional.of(skill));
        when(resourceRepository.findActiveForUpdateById(RESOURCE_ID)).thenReturn(Optional.of(skill));
        when(resourceRepository.update(skill)).thenAnswer(invocation -> {
            skill.setVersion(skill.getVersion() + 1);
            return Optional.of(skill);
        });
        when(resourceConverter.toInternalLong("7")).thenReturn(7L);
        when(contentStrategyRegistry.get(DdcAiKindEnum.SKILL)).thenReturn(skillStrategy);
        when(skillStrategy.prepare(any(DdcAiContentDTO.class))).thenReturn(
                new DdcAiContentStrategy.PreparedContent(null, List.of(), digest, Map.of(), 0, 0));
        when(draftNodeRepository.findActiveByResourceId(RESOURCE_ID)).thenReturn(List.of());
        when(publicationRepository.findByTenantRequestId(REQUEST_ID)).thenReturn(Optional.empty());
        when(versionRepository.insertVersion(any(DdcAiVersionBO.class))).thenAnswer(invocation ->
                ((DdcAiVersionBO) invocation.getArgument(0)).setId(VERSION_ID).setCreateTime(CREATED_AT));
        when(versionNodeRepository.insertAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(publicationRepository.insertPublication(any(DdcAiPublicationBO.class))).thenAnswer(invocation ->
                ((DdcAiPublicationBO) invocation.getArgument(0)).setId(PUBLICATION_ID).setCreateTime(CREATED_AT));
        when(publicationConverter.toResult(any(DdcAiPublicationBO.class))).thenReturn(DdcAiPublicationResult.builder()
                .publicationId(PUBLICATION_ID).resourceId(RESOURCE_ID).operation("PUBLISH").targetVersionId(VERSION_ID)
                .targetVersionNo(3L).resourceRevision(8L).targetCreated(false).createdAt(CREATED_AT).build());
        when(versionRepository.findActiveById(VERSION_ID)).thenReturn(Optional.of(
                promptVersion(VERSION_ID, 3L).setKind(DdcAiKindEnum.SKILL).setContentSha256(digest)));

        DdcAiPublicationReceiptResult receipt = service.publishSkillDraft(skillPublish("refund-skill"));

        assertThat(receipt.getPublicationId()).isEqualTo(PUBLICATION_ID);
        assertThat(receipt.getTargetVersionId()).isEqualTo(VERSION_ID);
        assertThat(receipt.getTargetVersionNo()).isEqualTo(3L);
        assertThat(receipt.getResourceRevision()).isEqualTo(8L);
        assertThat(receipt.getContentSha256()).isEqualTo(digest);
        verify(publicationNotifier).enqueue(any(DdcAiPublicationBO.class), org.mockito.ArgumentMatchers.same(skill),
                org.mockito.ArgumentMatchers.eq(digest), org.mockito.ArgumentMatchers.anyString());
        verify(scopeValidator, never()).assertCanRead(any());

        assertThatThrownBy(() -> service.publishSkillDraft(skillPublish("other-key")))
                .isInstanceOfSatisfying(CommonException.class, failure -> assertThat(failure.getCode()).isEqualTo(56302));
        verify(versionRepository, times(1)).insertVersion(any(DdcAiVersionBO.class));
    }

    private DdcAiSkillPublishCommand skillPublish(String resourceKey) {
        return DdcAiSkillPublishCommand.builder().namespaceCode(SCOPE.namespaceCode()).resourceKey(resourceKey)
                .resourceId(RESOURCE_ID).expectedRevision("7").requestId(REQUEST_ID).changeNote("from authoring")
                .build();
    }

    private void prepareCommandContext() {
        prepareAuthenticatedCommandUser();
        executeTransactionCallbacksInline(commandTransactionTemplate);
    }

    private void prepareAuthenticatedCommandUser() {
        when(userDetails.identitySub()).thenReturn("user-subject-1");
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken(userDetails, "", List.of()));
        SecurityContextHolder.setContext(context);
    }

    private DdcAiResourceBO resource(Long id, Long revision) {
        return DdcAiResourceBO.builder().id(id).tenantId(TENANT_ID).version(revision).bizCode(SCOPE.bizCode())
                .namespaceId(71L).namespaceCode(SCOPE.namespaceCode()).envCode(SCOPE.env()).appCode(SCOPE.appCode())
                .kind(DdcAiKindEnum.PROMPT).resourceKey("review-order").displayName("Review Order")
                .description("").draftPrompt(PROMPT).lastVersionNo(2L).currentVersionId(820002L)
                .currentVersionNo(2L).createTime(CREATED_AT).updateTime(CREATED_AT).build();
    }

    private DdcAiVersionBO promptVersion(Long id, Long versionNo) {
        return DdcAiVersionBO.builder().id(id).resourceId(RESOURCE_ID).versionNo(versionNo)
                .kind(DdcAiKindEnum.PROMPT).displayName("Review Order").description("")
                .promptText(PROMPT).contentSha256(HASH)
                .contentBytes((long) PROMPT.getBytes(StandardCharsets.UTF_8).length)
                .nodeCount(0).changeNote("published prompt").createTime(CREATED_AT).build();
    }

    private DdcAiVersionSummaryDTO promptSummary(DdcAiVersionBO version) {
        return DdcAiVersionSummaryDTO.builder().id(version.getId()).resourceId(version.getResourceId())
                .versionNo(version.getVersionNo()).kind(DdcAiKindEnum.PROMPT.name())
                .displayName(version.getDisplayName()).description(version.getDescription())
                .contentSha256(version.getContentSha256()).contentBytes(version.getContentBytes())
                .nodeCount(version.getNodeCount()).changeNote(version.getChangeNote())
                .createdAt(CREATED_AT).createdBy("operator").build();
    }

    private DdcAiContentStrategy.PreparedContent preparedPrompt(String text) {
        return new DdcAiContentStrategy.PreparedContent(text, List.of(), HASH, Map.of(),
                text.getBytes(StandardCharsets.UTF_8).length, 0);
    }

    private double metricCount(String operation, String result) {
        return meterRegistry.find("tianshu.ai.commands").tag("operation", operation)
                .tag("result", result).counter().count();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void executeTransactionCallbacksInline(TransactionTemplate template) {
        doAnswer(invocation -> ((TransactionCallback) invocation.getArgument(0))
                .doInTransaction(Mockito.mock(TransactionStatus.class)))
                .when(template).execute(any(TransactionCallback.class));
    }
}
