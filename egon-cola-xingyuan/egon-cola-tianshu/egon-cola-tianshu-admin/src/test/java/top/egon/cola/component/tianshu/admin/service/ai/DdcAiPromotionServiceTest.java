package top.egon.cola.component.tianshu.admin.service.ai;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiDraftNodeBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiPublicationBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionNodeBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceEnvAppBindingBO;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiPromotionCommand;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiDraftNodeConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiPublicationConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiResourceConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiResourceResultConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiVersionConverter;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiVersionSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.enums.DdcAiPublicationOperationEnum;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiIdQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiPublicationResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAiDraftNodeRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiPublicationRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionNodeRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiContentStrategy;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiResourceServiceImpl;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcSkillContentStrategy;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiScopeValidator;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DdcAiPromotionServiceTest {

    private final top.egon.cola.component.tianshu.admin.service.ai.notification.DdcAiPublicationNotifier publicationNotifier =
            org.mockito.Mockito.mock(top.egon.cola.component.tianshu.admin.service.ai.notification.DdcAiPublicationNotifier.class);
    @SuppressWarnings("unchecked")
    private final org.springframework.beans.factory.ObjectProvider<DdcAiOptimizationService> optimizationService =
            org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);

    private static final long TENANT_ID = 1001L;
    private static final long SOURCE_RESOURCE_ID = 810001L;
    private static final long EXISTING_TARGET_ID = 810002L;
    private static final long NEW_TARGET_ID = 810003L;
    private static final long SOURCE_VERSION_ID = 820102L;
    private static final long TARGET_VERSION_ID = 820201L;
    private static final long PUBLICATION_ID = 830201L;
    private static final long SOURCE_NAMESPACE_ID = 71L;
    private static final long TARGET_NAMESPACE_ID = 72L;
    private static final String REQUEST_ID = "promote_order_0001";
    private static final String FILE_TEXT = "frozen skill text\n";
    private static final String HASH = "b".repeat(64);
    private static final Instant CREATED_AT = Instant.parse("2026-09-29T11:00:00Z");
    private static final DdcAiScopeDTO SOURCE_SCOPE =
            new DdcAiScopeDTO("retail", "dev-space", "dev", "assistant-app");
    private static final DdcAiScopeDTO TARGET_SCOPE =
            new DdcAiScopeDTO("retail", "prod-space", "prod", "assistant-app");

    @Mock private DdcAiResourceRepository resourceRepository;
    @Mock private DdcAiDraftNodeRepository draftNodeRepository;
    @Mock private DdcAiScopeValidator scopeValidator;
    @Mock private DdcAiResourceConverter resourceConverter;
    @Mock private DdcAiDraftNodeConverter draftNodeConverter;
    @Mock private DdcAiVersionRepository versionRepository;
    @Mock private DdcAiVersionNodeRepository versionNodeRepository;
    @Mock private DdcAiPublicationRepository publicationRepository;
    @Mock private DdcAiVersionConverter versionConverter;
    @Mock private DdcAiPublicationConverter publicationConverter;
    @Mock private DdcAiResourceResultConverter resourceResultConverter;
    @Mock private Map<DdcAiKindEnum, DdcAiContentStrategy> strategyRegistry;
    @Mock private DdcAiContentStrategy skillStrategy;
    @Mock private TransactionTemplate readTransactionTemplate;
    @Mock private TransactionTemplate commandTransactionTemplate;
    @Mock private Rbac3UserDetails userDetails;
    @Mock private DdcScopeGate scopeGate;
    @Mock private DdcNamespaceRepository namespaceRepository;
    @Mock private DdcNamespaceEnvAppBindingRepository bindingRepository;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private DdcAiResourceServiceImpl service;

    @BeforeEach
    void setUp() {
        MDC.put("tenantId", Long.toString(TENANT_ID));
        service = new DdcAiResourceServiceImpl(resourceRepository, draftNodeRepository, scopeValidator,
                resourceConverter, draftNodeConverter, versionRepository, versionNodeRepository,
                publicationRepository, versionConverter, publicationConverter, resourceResultConverter,
                meterRegistry, strategyRegistry, readTransactionTemplate, commandTransactionTemplate,
                publicationNotifier, optimizationService);
    }

    @AfterEach
    void clearContext() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        meterRegistry.close();
    }

    @Test
    void existingTargetPromotionCopiesTheExplicitSkillVersionPreservesDraftAndReplaysAfterRootLocks() {
        prepareCommandContext();
        DdcAiResourceBO source = resource(SOURCE_RESOURCE_ID, SOURCE_NAMESPACE_ID, SOURCE_SCOPE,
                "review-order", 11L, 5L, 820005L, 5L, null, "Source draft stays untouched");
        DdcAiResourceBO target = resource(EXISTING_TARGET_ID, TARGET_NAMESPACE_ID, TARGET_SCOPE,
                "review-order", 7L, 4L, 820104L, 4L, "Target Title", "Target Description")
                .setDraftPrompt(null);
        DdcAiContentStrategy.PreparedContent snapshot = preparedSkill(FILE_TEXT);
        DdcAiVersionBO sourceVersion = sourceVersion(snapshot);
        List<DdcAiVersionNodeBO> sourceNodes = versionNodes(SOURCE_VERSION_ID, snapshot);
        DdcAiPromotionCommand command = promotion("2", "7", "promote verified skill");
        AtomicReference<DdcAiPublicationBO> committedReceipt = new AtomicReference<>();
        AtomicBoolean simulateConcurrentCommit = new AtomicBoolean();
        AtomicInteger requestLookups = new AtomicInteger();
        DdcAiPublicationResult receiptResult = publicationResult(EXISTING_TARGET_ID, TARGET_VERSION_ID, 5L,
                false, 8L);

        when(userDetails.identitySub()).thenReturn("operator-subject-1");
        when(resourceRepository.findActiveById(SOURCE_RESOURCE_ID)).thenReturn(Optional.of(source));
        when(resourceRepository.findByBusinessKey(TARGET_SCOPE.bizCode(), TARGET_NAMESPACE_ID, TARGET_SCOPE.env(),
                TARGET_SCOPE.appCode(), DdcAiKindEnum.SKILL, "review-order")).thenReturn(Optional.of(target));
        when(resourceRepository.findActiveForUpdateById(SOURCE_RESOURCE_ID)).thenReturn(Optional.of(source));
        when(resourceRepository.findActiveForUpdateById(EXISTING_TARGET_ID)).thenReturn(Optional.of(target));
        when(resourceRepository.update(target)).thenAnswer(invocation -> {
            target.setVersion(target.getVersion() + 1);
            return Optional.of(target);
        });
        when(resourceConverter.toInternalLong("2")).thenReturn(2L);
        when(resourceConverter.toInternalLong("7")).thenReturn(7L);
        when(scopeValidator.resolveNamespaceIdForPublish(TARGET_SCOPE)).thenReturn(TARGET_NAMESPACE_ID);
        when(versionRepository.findByResourceIdAndVersionNo(SOURCE_RESOURCE_ID, 2L))
                .thenReturn(Optional.of(sourceVersion));
        when(versionNodeRepository.findTreeByVersionId(SOURCE_VERSION_ID)).thenReturn(sourceNodes);
        when(strategyRegistry.get(DdcAiKindEnum.SKILL)).thenReturn(skillStrategy);
        when(skillStrategy.kind()).thenReturn(DdcAiKindEnum.SKILL);
        when(skillStrategy.prepare(any(DdcAiContentDTO.class))).thenReturn(snapshot);
        when(publicationRepository.findByTenantRequestId(REQUEST_ID)).thenAnswer(invocation -> {
            if (simulateConcurrentCommit.get() && requestLookups.getAndIncrement() == 0) return Optional.empty();
            return Optional.ofNullable(committedReceipt.get());
        });
        when(publicationRepository.findByTenantRequestId("promote_stale_target_0001"))
                .thenReturn(Optional.empty(), Optional.empty());
        when(versionRepository.insertVersion(any(DdcAiVersionBO.class))).thenAnswer(invocation ->
                ((DdcAiVersionBO) invocation.getArgument(0)).setId(TARGET_VERSION_ID).setCreateTime(CREATED_AT));
        when(versionNodeRepository.insertAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(publicationRepository.insertPublication(any(DdcAiPublicationBO.class))).thenAnswer(invocation -> {
            DdcAiPublicationBO inserted = invocation.getArgument(0);
            inserted.setId(PUBLICATION_ID).setCreateTime(CREATED_AT);
            committedReceipt.set(inserted);
            return inserted;
        });
        when(publicationConverter.toResult(any(DdcAiPublicationBO.class))).thenReturn(receiptResult);

        DdcAiPublicationResult promoted = service.promote(new DdcAiIdQuery(SOURCE_RESOURCE_ID), command, REQUEST_ID);

        assertThat(promoted).isSameAs(receiptResult);
        assertThat(source.getCurrentVersionId()).isEqualTo(820005L);
        assertThat(source.getDescription()).isEqualTo("Source draft stays untouched");
        assertThat(target.getDisplayName()).isEqualTo("Target Title");
        assertThat(target.getDescription()).isEqualTo("Target Description");
        assertThat(target.getDraftPrompt()).isNull();
        assertThat(target.getCurrentVersionId()).isEqualTo(TARGET_VERSION_ID);
        assertThat(target.getCurrentVersionNo()).isEqualTo(5L);
        assertThat(target.getLastVersionNo()).isEqualTo(5L);
        assertThat(target.getVersion()).isEqualTo(8L);

        ArgumentCaptor<DdcAiVersionBO> versionCaptor = ArgumentCaptor.forClass(DdcAiVersionBO.class);
        verify(versionRepository).insertVersion(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getSourceVersionId()).isEqualTo(SOURCE_VERSION_ID);
        assertThat(versionCaptor.getValue().getResourceId()).isEqualTo(EXISTING_TARGET_ID);
        assertThat(versionCaptor.getValue().getVersionNo()).isEqualTo(5L);
        assertThat(versionCaptor.getValue().getContentSha256()).isEqualTo(snapshot.contentHash());
        ArgumentCaptor<List<DdcAiVersionNodeBO>> nodesCaptor = listCaptor();
        verify(versionNodeRepository).insertAll(nodesCaptor.capture());
        assertThat(nodesCaptor.getValue()).hasSize(2);
        assertThat(nodesCaptor.getValue()).extracting(DdcAiVersionNodeBO::getVersionId)
                .containsOnly(TARGET_VERSION_ID);
        assertThat(committedReceipt.get().getOperation()).isEqualTo(DdcAiPublicationOperationEnum.PROMOTE);
        assertThat(committedReceipt.get().getSourceVersionId()).isEqualTo(SOURCE_VERSION_ID);
        assertThat(committedReceipt.get().getTargetCreated()).isFalse();

        simulateConcurrentCommit.set(true);
        requestLookups.set(0);
        DdcAiPublicationResult replay = service.promote(new DdcAiIdQuery(SOURCE_RESOURCE_ID), command, REQUEST_ID);

        assertThat(replay).isSameAs(promoted);

        DdcAiPromotionCommand otherTarget = promotion("2", "7", "promote verified skill")
                .setTargetResourceKey("review-invoice");
        assertThatThrownBy(() -> service.promote(new DdcAiIdQuery(SOURCE_RESOURCE_ID), otherTarget, REQUEST_ID))
                .isInstanceOf(CommonException.class)
                .satisfies(failure -> assertThat(((CommonException) failure).getCode()).isEqualTo(56305));
        DdcAiPromotionCommand staleTargetRevision = promotion("2", "7", "stale target revision");
        assertThatThrownBy(() -> service.promote(new DdcAiIdQuery(SOURCE_RESOURCE_ID), staleTargetRevision,
                "promote_stale_target_0001"))
                .isInstanceOf(CommonException.class)
                .satisfies(failure -> assertThat(((CommonException) failure).getCode()).isEqualTo(56303));

        verify(versionRepository, times(1)).insertVersion(any(DdcAiVersionBO.class));
        verify(publicationRepository, times(1)).insertPublication(any(DdcAiPublicationBO.class));
        verify(resourceRepository, times(1)).update(target);
        verify(scopeValidator, times(6)).assertCanRead(source);
        verify(scopeValidator, times(4)).resolveNamespaceIdForPublish(TARGET_SCOPE);
        verify(scopeValidator, never()).resolveNamespaceIdForWrite(TARGET_SCOPE);
        verifyNoInteractions(draftNodeRepository, draftNodeConverter);
        InOrder roots = inOrder(resourceRepository);
        roots.verify(resourceRepository).findActiveForUpdateById(SOURCE_RESOURCE_ID);
        roots.verify(resourceRepository).findActiveForUpdateById(EXISTING_TARGET_ID);
    }

    @Test
    void newTargetPromotionRequiresWriteAndInitializesAnIndependentDraftAndVersion() {
        prepareCommandContext();
        DdcAiResourceBO source = resource(SOURCE_RESOURCE_ID, SOURCE_NAMESPACE_ID, SOURCE_SCOPE,
                "review-order", 11L, 5L, 820005L, 5L, null, null);
        DdcAiContentStrategy.PreparedContent snapshot = preparedSkill(FILE_TEXT);
        DdcAiVersionBO sourceVersion = sourceVersion(snapshot);
        List<DdcAiVersionNodeBO> sourceNodes = versionNodes(SOURCE_VERSION_ID, snapshot);
        DdcAiPromotionCommand command = promotion("2", null, "promote into new target");
        AtomicReference<DdcAiResourceBO> createdTarget = new AtomicReference<>();
        AtomicReference<DdcAiPublicationBO> committedReceipt = new AtomicReference<>();
        DdcAiPublicationResult receiptResult = publicationResult(NEW_TARGET_ID, TARGET_VERSION_ID, 1L,
                true, 1L);

        when(userDetails.identitySub()).thenReturn("operator-subject-1");
        when(resourceRepository.findActiveById(SOURCE_RESOURCE_ID)).thenReturn(Optional.of(source));
        when(resourceRepository.findByBusinessKey(TARGET_SCOPE.bizCode(), TARGET_NAMESPACE_ID, TARGET_SCOPE.env(),
                TARGET_SCOPE.appCode(), DdcAiKindEnum.SKILL, "review-order")).thenReturn(Optional.empty());
        when(resourceRepository.findActiveForUpdateById(SOURCE_RESOURCE_ID)).thenReturn(Optional.of(source));
        when(resourceRepository.insert(any(DdcAiResourceBO.class))).thenAnswer(invocation -> {
            DdcAiResourceBO inserted = ((DdcAiResourceBO) invocation.getArgument(0)).setId(NEW_TARGET_ID)
                    .setTenantId(TENANT_ID).setVersion(0L).setCreateTime(CREATED_AT);
            createdTarget.set(inserted);
            return inserted;
        });
        when(resourceRepository.update(any(DdcAiResourceBO.class))).thenAnswer(invocation -> {
            DdcAiResourceBO updated = invocation.getArgument(0);
            updated.setVersion(updated.getVersion() + 1);
            return Optional.of(updated);
        });
        when(resourceConverter.toInternalLong("2")).thenReturn(2L);
        when(scopeValidator.resolveNamespaceIdForPublish(TARGET_SCOPE)).thenReturn(TARGET_NAMESPACE_ID);
        when(scopeValidator.resolveNamespaceIdForWrite(TARGET_SCOPE)).thenReturn(TARGET_NAMESPACE_ID);
        when(versionRepository.findByResourceIdAndVersionNo(SOURCE_RESOURCE_ID, 2L))
                .thenReturn(Optional.of(sourceVersion));
        when(versionNodeRepository.findTreeByVersionId(SOURCE_VERSION_ID)).thenReturn(sourceNodes);
        when(strategyRegistry.get(DdcAiKindEnum.SKILL)).thenReturn(skillStrategy);
        when(skillStrategy.kind()).thenReturn(DdcAiKindEnum.SKILL);
        when(skillStrategy.prepare(any(DdcAiContentDTO.class))).thenReturn(snapshot);
        when(publicationRepository.findByTenantRequestId(REQUEST_ID))
                .thenReturn(Optional.empty(), Optional.empty());
        when(draftNodeConverter.toBusiness(any(DdcAiNodeDTO.class))).thenAnswer(invocation -> {
            DdcAiNodeDTO node = invocation.getArgument(0);
            return DdcAiDraftNodeBO.builder().path(node.getPath()).nodeType(node.getNodeType())
                    .content(node.getContent()).build();
        });
        when(draftNodeRepository.insert(any(DdcAiDraftNodeBO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.insertVersion(any(DdcAiVersionBO.class))).thenAnswer(invocation ->
                ((DdcAiVersionBO) invocation.getArgument(0)).setId(TARGET_VERSION_ID).setCreateTime(CREATED_AT));
        when(versionNodeRepository.insertAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(publicationRepository.insertPublication(any(DdcAiPublicationBO.class))).thenAnswer(invocation -> {
            DdcAiPublicationBO inserted = invocation.getArgument(0);
            inserted.setId(PUBLICATION_ID).setCreateTime(CREATED_AT);
            committedReceipt.set(inserted);
            return inserted;
        });
        when(publicationConverter.toResult(any(DdcAiPublicationBO.class))).thenReturn(receiptResult);

        DdcAiPublicationResult promoted = service.promote(new DdcAiIdQuery(SOURCE_RESOURCE_ID), command, REQUEST_ID);

        assertThat(promoted).isSameAs(receiptResult);
        DdcAiResourceBO target = createdTarget.get();
        assertThat(target).isNotNull();
        assertThat(target.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(target.getNamespaceId()).isEqualTo(TARGET_NAMESPACE_ID);
        assertThat(target.getNamespaceCode()).isEqualTo(TARGET_SCOPE.namespaceCode());
        assertThat(target.getResourceKey()).isEqualTo("review-order");
        assertThat(target.getDisplayName()).isEqualTo(sourceVersion.getDisplayName());
        assertThat(target.getDescription()).isEqualTo(sourceVersion.getDescription());
        assertThat(target.getLastVersionNo()).isEqualTo(1L);
        assertThat(target.getCurrentVersionId()).isEqualTo(TARGET_VERSION_ID);
        assertThat(target.getCurrentVersionNo()).isEqualTo(1L);
        assertThat(committedReceipt.get().getPreviousVersionId()).isNull();
        assertThat(committedReceipt.get().getTargetCreated()).isTrue();
        assertThat(committedReceipt.get().getSourceVersionId()).isEqualTo(SOURCE_VERSION_ID);
        ArgumentCaptor<DdcAiDraftNodeBO> draftNodes = ArgumentCaptor.forClass(DdcAiDraftNodeBO.class);
        verify(draftNodeRepository, times(2)).insert(draftNodes.capture());
        assertThat(draftNodes.getAllValues()).extracting(DdcAiDraftNodeBO::getPath).containsExactly("", "README.txt");
        verify(scopeValidator).resolveNamespaceIdForPublish(TARGET_SCOPE);
        verify(scopeValidator).resolveNamespaceIdForWrite(TARGET_SCOPE);
        verify(resourceRepository, never()).update(source);
    }

    @Test
    void targetRevisionMismatchAndCrossTenantSourceAreRejectedWithoutWrites() {
        prepareCommandContext();
        DdcAiResourceBO source = resource(SOURCE_RESOURCE_ID, SOURCE_NAMESPACE_ID, SOURCE_SCOPE,
                "review-order", 11L, 5L, 820005L, 5L, null, null);
        long lowerTargetId = SOURCE_RESOURCE_ID - 1;
        DdcAiResourceBO target = resource(lowerTargetId, TARGET_NAMESPACE_ID, TARGET_SCOPE,
                "review-order", 8L, 4L, 820104L, 4L, "Preserved", "Description");
        DdcAiPromotionCommand mismatchedRevision = promotion("2", "7", "promotion");
        when(userDetails.identitySub()).thenReturn("operator-subject-1");
        when(resourceRepository.findActiveById(SOURCE_RESOURCE_ID)).thenReturn(Optional.of(source));
        when(resourceRepository.findByBusinessKey(TARGET_SCOPE.bizCode(), TARGET_NAMESPACE_ID, TARGET_SCOPE.env(),
                TARGET_SCOPE.appCode(), DdcAiKindEnum.SKILL, "review-order")).thenReturn(Optional.of(target));
        when(scopeValidator.resolveNamespaceIdForPublish(TARGET_SCOPE)).thenReturn(TARGET_NAMESPACE_ID);
        when(publicationRepository.findByTenantRequestId(REQUEST_ID)).thenReturn(Optional.empty(), Optional.empty());
        when(resourceRepository.findActiveForUpdateById(SOURCE_RESOURCE_ID)).thenReturn(Optional.of(source));
        when(resourceRepository.findActiveForUpdateById(lowerTargetId)).thenReturn(Optional.of(target));
        when(resourceConverter.toInternalLong("2")).thenReturn(2L);
        when(resourceConverter.toInternalLong("7")).thenReturn(7L);

        assertThatThrownBy(() -> service.promote(new DdcAiIdQuery(SOURCE_RESOURCE_ID), mismatchedRevision, REQUEST_ID))
                .isInstanceOf(CommonException.class)
                .satisfies(failure -> assertThat(((CommonException) failure).getCode()).isEqualTo(56303));
        assertThat(target.getCurrentVersionId()).isEqualTo(820104L);
        verify(resourceRepository, never()).update(any(DdcAiResourceBO.class));
        verify(versionRepository, never()).insertVersion(any(DdcAiVersionBO.class));
        verify(publicationRepository, never()).insertPublication(any(DdcAiPublicationBO.class));

        when(resourceRepository.findActiveById(SOURCE_RESOURCE_ID)).thenReturn(Optional.empty());
        DdcAiPromotionCommand command = promotion("2", null, "cross-tenant source");
        assertThat(Arrays.stream(DdcAiPromotionCommand.class.getDeclaredFields()).map(java.lang.reflect.Field::getName))
                .doesNotContain("tenantId");
        assertThatThrownBy(() -> service.promote(new DdcAiIdQuery(SOURCE_RESOURCE_ID), command, "promote_other_0001"))
                .isInstanceOf(CommonException.class)
                .satisfies(failure -> assertThat(((CommonException) failure).getCode()).isEqualTo(56302));
        verify(scopeValidator, times(1)).resolveNamespaceIdForPublish(TARGET_SCOPE);
        InOrder roots = inOrder(resourceRepository);
        roots.verify(resourceRepository).findActiveForUpdateById(lowerTargetId);
        roots.verify(resourceRepository).findActiveForUpdateById(SOURCE_RESOURCE_ID);
    }

    @Test
    void targetCreatedAfterInitialLookupIsRejectedBeforePromotionWrites() {
        prepareCommandContext();
        DdcAiResourceBO source = resource(SOURCE_RESOURCE_ID, SOURCE_NAMESPACE_ID, SOURCE_SCOPE,
                "review-order", 11L, 5L, 820005L, 5L, null, null);
        DdcAiResourceBO concurrentTarget = resource(EXISTING_TARGET_ID, TARGET_NAMESPACE_ID, TARGET_SCOPE,
                "review-order", 1L, 0L, null, null, "Concurrent target", "Created concurrently");
        when(userDetails.identitySub()).thenReturn("operator-subject-1");
        when(resourceRepository.findActiveById(SOURCE_RESOURCE_ID)).thenReturn(Optional.of(source));
        when(scopeValidator.resolveNamespaceIdForPublish(TARGET_SCOPE)).thenReturn(TARGET_NAMESPACE_ID);
        when(scopeValidator.resolveNamespaceIdForWrite(TARGET_SCOPE)).thenReturn(TARGET_NAMESPACE_ID);
        when(publicationRepository.findByTenantRequestId(REQUEST_ID)).thenReturn(Optional.empty(), Optional.empty());
        when(resourceRepository.findByBusinessKey(TARGET_SCOPE.bizCode(), TARGET_NAMESPACE_ID, TARGET_SCOPE.env(),
                TARGET_SCOPE.appCode(), DdcAiKindEnum.SKILL, "review-order"))
                .thenReturn(Optional.empty(), Optional.of(concurrentTarget));
        when(resourceRepository.findActiveForUpdateById(SOURCE_RESOURCE_ID)).thenReturn(Optional.of(source));
        when(resourceConverter.toInternalLong("2")).thenReturn(2L);

        assertThatThrownBy(() -> service.promote(new DdcAiIdQuery(SOURCE_RESOURCE_ID),
                promotion("2", null, "concurrent target"), REQUEST_ID))
                .isInstanceOf(CommonException.class)
                .satisfies(failure -> assertThat(((CommonException) failure).getCode()).isEqualTo(56303));

        verify(resourceRepository, never()).insert(any(DdcAiResourceBO.class));
        verify(resourceRepository, never()).update(any(DdcAiResourceBO.class));
        verify(versionRepository, never()).insertVersion(any(DdcAiVersionBO.class));
        verify(publicationRepository, never()).insertPublication(any(DdcAiPublicationBO.class));
        verifyNoInteractions(draftNodeRepository, draftNodeConverter, versionNodeRepository);
    }

    @Test
    void promotionCommandRequiresRevisionFieldButAllowsExplicitNull() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String explicitNull = """
                {"sourceVersionNo":"2","targetScope":{"bizCode":"retail","namespaceCode":"prod-space",
                "env":"prod","appCode":"assistant-app"},"targetResourceKey":"review-order",
                "expectedTargetRevision":null,"changeNote":"promote"}
                """;
        String missingRevision = """
                {"sourceVersionNo":"2","targetScope":{"bizCode":"retail","namespaceCode":"prod-space",
                "env":"prod","appCode":"assistant-app"},"targetResourceKey":"review-order",
                "changeNote":"promote"}
                """;

        DdcAiPromotionCommand parsed = objectMapper.readValue(explicitNull, DdcAiPromotionCommand.class);

        assertThat(parsed.getExpectedTargetRevision()).isNull();
        assertThatThrownBy(() -> objectMapper.readValue(missingRevision, DdcAiPromotionCommand.class))
                .isInstanceOf(JsonMappingException.class);
    }

    @Test
    void publishScopeResolverAllowsExistingTargetPUBLISHWithoutWRITE() {
        DdcNamespaceBO namespace = DdcNamespaceBO.builder().id(TARGET_NAMESPACE_ID)
                .publicId("namespace-public-72").bizCode(TARGET_SCOPE.bizCode())
                .namespaceCode(TARGET_SCOPE.namespaceCode()).enabled(true).build();
        when(namespaceRepository.findByBizCodeAndNamespaceCode(TARGET_SCOPE.bizCode(), TARGET_SCOPE.namespaceCode()))
                .thenReturn(Optional.of(namespace));
        when(bindingRepository.findAll(TARGET_SCOPE.bizCode(), TARGET_SCOPE.namespaceCode(),
                TARGET_SCOPE.env(), TARGET_SCOPE.appCode()))
                .thenReturn(List.of(DdcNamespaceEnvAppBindingBO.builder().enabled(true).build()));
        Rbac3UserDetails publisher = Mockito.mock(Rbac3UserDetails.class);
        when(publisher.permissions()).thenReturn(Set.of("TIANSHU_AI_PUBLISH:namespace-public-72"));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken(publisher, "", List.of()));
        SecurityContextHolder.setContext(context);
        DdcAiScopeValidator validator = new DdcAiScopeValidator(
                scopeGate, resourceRepository, namespaceRepository, bindingRepository);

        assertThat(validator.resolveNamespaceIdForPublish(TARGET_SCOPE)).isEqualTo(TARGET_NAMESPACE_ID);
        assertThatThrownBy(() -> validator.resolveNamespaceIdForWrite(TARGET_SCOPE))
                .isInstanceOf(CommonException.class)
                .satisfies(failure -> assertThat(((CommonException) failure).getCode()).isEqualTo(56307));
    }

    private void prepareCommandContext() {
        when(userDetails.identitySub()).thenReturn("operator-subject-1");
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken(userDetails, "", List.of()));
        SecurityContextHolder.setContext(context);
        executeTransactionCallbacksInline(commandTransactionTemplate);
    }

    private DdcAiPromotionCommand promotion(String sourceVersionNo, String expectedTargetRevision,
            String changeNote) {
        return DdcAiPromotionCommand.builder().sourceVersionNo(sourceVersionNo).targetScope(TARGET_SCOPE)
                .targetResourceKey("review-order").expectedTargetRevision(expectedTargetRevision)
                .changeNote(changeNote).build();
    }

    private DdcAiResourceBO resource(Long id, Long namespaceId, DdcAiScopeDTO scope, String resourceKey,
            Long revision, Long lastVersionNo, Long currentVersionId, Long currentVersionNo,
            String displayName, String description) {
        return DdcAiResourceBO.builder().id(id).tenantId(TENANT_ID).version(revision)
                .bizCode(scope.bizCode()).namespaceId(namespaceId).namespaceCode(scope.namespaceCode())
                .envCode(scope.env()).appCode(scope.appCode()).kind(DdcAiKindEnum.SKILL)
                .resourceKey(resourceKey).displayName(displayName).description(description).draftPrompt(null)
                .lastVersionNo(lastVersionNo).currentVersionId(currentVersionId).currentVersionNo(currentVersionNo)
                .createTime(CREATED_AT).updateTime(CREATED_AT).build();
    }

    private DdcAiVersionBO sourceVersion(DdcAiContentStrategy.PreparedContent snapshot) {
        return DdcAiVersionBO.builder().id(SOURCE_VERSION_ID).resourceId(SOURCE_RESOURCE_ID).versionNo(2L)
                .kind(DdcAiKindEnum.SKILL).displayName("Verified Skill").description("source snapshot metadata")
                .promptText(null).contentSha256(snapshot.contentHash()).contentBytes(snapshot.contentBytes())
                .nodeCount(snapshot.nodeCount()).changeNote("validated source").createTime(CREATED_AT).build();
    }

    private DdcAiContentStrategy.PreparedContent preparedSkill(String fileText) {
        List<DdcAiNodeDTO> nodes = List.of(
                DdcAiNodeDTO.builder().path("").nodeType(DdcAiNodeTypeEnum.DIRECTORY).build(),
                DdcAiNodeDTO.builder().path("README.txt").nodeType(DdcAiNodeTypeEnum.FILE).content(fileText).build());
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return new DdcSkillContentStrategy(new ValidationUtils(factory.getValidator()))
                    .prepare(DdcAiContentDTO.builder().promptText(null).nodes(nodes).build());
        }
    }

    private List<DdcAiVersionNodeBO> versionNodes(Long versionId,
            DdcAiContentStrategy.PreparedContent snapshot) {
        return snapshot.nodes().stream().map(node -> {
            boolean file = node.getNodeType() == DdcAiNodeTypeEnum.FILE;
            long contentBytes = file ? node.getContent().getBytes(StandardCharsets.UTF_8).length : 0L;
            return DdcAiVersionNodeBO.builder().versionId(versionId).path(node.getPath())
                    .parentPath(parentPath(node.getPath())).nodeType(node.getNodeType()).content(node.getContent())
                    .contentSha256(file ? snapshot.fileSha256ByPath().get(node.getPath()) : null)
                    .contentBytes(contentBytes).build();
        }).toList();
    }

    private String parentPath(String path) {
        if (path.isEmpty()) return null;
        int separator = path.lastIndexOf('/');
        return separator < 0 ? "" : path.substring(0, separator);
    }

    private DdcAiPublicationResult publicationResult(Long resourceId, Long targetVersionId, Long targetVersionNo,
            boolean targetCreated, Long resourceRevision) {
        return DdcAiPublicationResult.builder().publicationId(PUBLICATION_ID).resourceId(resourceId)
                .operation("PROMOTE").targetVersionId(targetVersionId).targetVersionNo(targetVersionNo)
                .sourceVersionId(SOURCE_VERSION_ID).resourceRevision(resourceRevision)
                .targetCreated(targetCreated).createdAt(CREATED_AT).build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<List<DdcAiVersionNodeBO>> listCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void executeTransactionCallbacksInline(TransactionTemplate template) {
        doAnswer(invocation -> ((TransactionCallback) invocation.getArgument(0))
                .doInTransaction(Mockito.mock(TransactionStatus.class)))
                .when(template).execute(any(TransactionCallback.class));
    }
}
