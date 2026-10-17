package top.egon.cola.component.tianshu.admin.service.ai;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.MDC;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.common.core.pojo.PageQuery;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiDraftNodeBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceEnvAppBindingBO;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiCreateCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDeleteCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDraftCommand;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiDraftNodeConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiPublicationConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiResourceConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiResourceResultConverter;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiVersionConverter;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiResourceSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiIdQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiPageQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiResourceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.config.DdcAiTransactionConfiguration;
import top.egon.cola.component.tianshu.admin.repository.DdcAiDraftNodeRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiPublicationRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionNodeRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcAiVersionRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiContentStrategy;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiContentStrategy.PreparedContent;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcAiResourceServiceImpl;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcPromptContentStrategy;
import top.egon.cola.component.tianshu.admin.service.ai.impl.DdcSkillContentStrategy;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiScopeValidator;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;
import top.egon.cola.platform.tianquan.jianshen.starter.security.Rbac3UserDetails;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
class DdcAiDraftServiceTest {

    private final top.egon.cola.component.tianshu.admin.service.ai.notification.DdcAiPublicationNotifier publicationNotifier =
            org.mockito.Mockito.mock(top.egon.cola.component.tianshu.admin.service.ai.notification.DdcAiPublicationNotifier.class);
    @SuppressWarnings("unchecked")
    private final org.springframework.beans.factory.ObjectProvider<DdcAiOptimizationService> optimizationService =
            org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);

    private static final DdcAiScopeDTO SCOPE = new DdcAiScopeDTO("retail", "dev-space", "dev", "assistant-app");
    private static final long TENANT_ID = 1001L;
    private static final long NAMESPACE_ID = 71L;
    private static final long RESOURCE_ID = 810001L;

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
    @Mock private DdcAiResourceResultConverter resultConverter;
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    @Mock private Map<DdcAiKindEnum, DdcAiContentStrategy> strategyRegistry;
    @Mock private DdcAiContentStrategy promptStrategy;
    @Mock private DdcAiContentStrategy skillStrategy;
    @Mock private TransactionTemplate readTransactionTemplate;
    @Mock private TransactionTemplate commandTransactionTemplate;

    private DdcAiResourceServiceImpl service;

    @BeforeEach
    void setUp() {
        MDC.put("tenantId", Long.toString(TENANT_ID));
        service = new DdcAiResourceServiceImpl(resourceRepository, draftNodeRepository, scopeValidator,
                resourceConverter, draftNodeConverter, versionRepository, versionNodeRepository,
                publicationRepository, versionConverter, publicationConverter, resultConverter, meterRegistry,
                strategyRegistry, readTransactionTemplate, commandTransactionTemplate,
                publicationNotifier, optimizationService);
    }

    @AfterEach
    void clearTenantContext() {
        MDC.clear();
        SecurityContextHolder.clearContext();
        meterRegistry.close();
    }

    @Test
    void promptStrategyPreservesOriginalTextAndUsesTheApprovedCanonicalHash() {
        String promptText = "  keep me\r\n";
        DdcAiContentDTO content = DdcAiContentDTO.builder().promptText(promptText).nodes(List.of()).build();

        PreparedContent prepared;
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            prepared = new DdcPromptContentStrategy(new ValidationUtils(factory.getValidator())).prepare(content);
        }

        assertThat(prepared.promptText()).isEqualTo(promptText);
        assertThat(prepared.nodes()).isEmpty();
        assertThat(prepared.contentBytes()).isEqualTo(11L);
        assertThat(prepared.nodeCount()).isZero();
        assertThat(prepared.contentHash()).isEqualTo("4f91ea9373d3327c3f5506709fcff75574fa42ef856c40df59673b3740f9a582");
    }

    @Test
    void skillStrategyCanonicalizesOneCompleteTreeIndependentOfInputOrder() {
        DdcAiContentDTO forward = skillContent(List.of(
                node("", DdcAiNodeTypeEnum.DIRECTORY, null),
                node("docs", DdcAiNodeTypeEnum.DIRECTORY, null),
                node("docs/readme.md", DdcAiNodeTypeEnum.FILE, "check\n")));
        DdcAiContentDTO reverse = skillContent(List.of(
                node("docs/readme.md", DdcAiNodeTypeEnum.FILE, "check\n"),
                node("docs", DdcAiNodeTypeEnum.DIRECTORY, null),
                node("", DdcAiNodeTypeEnum.DIRECTORY, null)));

        PreparedContent prepared;
        PreparedContent reordered;
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            DdcSkillContentStrategy strategy = new DdcSkillContentStrategy(new ValidationUtils(factory.getValidator()));
            prepared = strategy.prepare(forward);
            reordered = strategy.prepare(reverse);
        }

        assertThat(prepared.promptText()).isNull();
        assertThat(prepared.nodes()).extracting(DdcAiNodeDTO::getPath)
                .containsExactly("", "docs", "docs/readme.md");
        assertThat(prepared.nodes().getLast().getContent()).isEqualTo("check\n");
        assertThat(prepared.contentBytes()).isEqualTo(6L);
        assertThat(prepared.nodeCount()).isEqualTo(3);
        assertThat(prepared.contentHash()).isEqualTo("738b5da054b53d5074266e39cfd5f64df197b76a0e21bb214a552e442f24e25d");
        assertThat(reordered.contentHash()).isEqualTo(prepared.contentHash());
    }

    @Test
    void createCommandCascadesSaveGroupTreeAndUtf8Validation() {
        DdcAiContentDTO duplicatePathTree = skillContent(List.of(
                node("", DdcAiNodeTypeEnum.DIRECTORY, null),
                node("SKILL.md", DdcAiNodeTypeEnum.FILE, "first"),
                node("SKILL.md", DdcAiNodeTypeEnum.FILE, "second")));
        DdcAiCreateCommand duplicatePathCommand = DdcAiCreateCommand.builder().scope(SCOPE)
                .kind(DdcAiKindEnum.SKILL).resourceKey("review-order").displayName("Review Order")
                .description("").draft(duplicatePathTree).build();
        DdcAiCreateCommand invalidUtf8Command = createCommand("bad" + Character.toString((char) 0) + "content");

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(duplicatePathCommand)).isNotEmpty();
            assertThat(validator.validate(invalidUtf8Command)).isNotEmpty();
        }
    }

    @Test
    void createStoresOriginalPromptAndDoesNotCreatePublishedState() {
        executeTransactionCallbacksInline(commandTransactionTemplate);
        String promptText = "  exact prompt\r\n";
        DdcAiCreateCommand command = createCommand(promptText);
        DdcAiResourceBO mapped = DdcAiResourceBO.builder().bizCode(SCOPE.bizCode()).envCode(SCOPE.env())
                .appCode(SCOPE.appCode()).kind(DdcAiKindEnum.PROMPT).resourceKey("review-order")
                .displayName("Review Order").description("").draftPrompt(promptText).lastVersionNo(0L).build();
        when(scopeValidator.resolveNamespaceIdForWrite(SCOPE)).thenReturn(NAMESPACE_ID);
        when(resourceRepository.findByBusinessKey(SCOPE.bizCode(), NAMESPACE_ID, SCOPE.env(), SCOPE.appCode(),
                DdcAiKindEnum.PROMPT, "review-order")).thenReturn(Optional.empty());
        when(strategyRegistry.get(DdcAiKindEnum.PROMPT)).thenReturn(promptStrategy);
        when(promptStrategy.kind()).thenReturn(DdcAiKindEnum.PROMPT);
        when(promptStrategy.prepare(command.getDraft())).thenReturn(preparedPrompt(promptText));
        when(resourceConverter.toBusiness(command)).thenReturn(mapped);
        when(resourceRepository.insert(any(DdcAiResourceBO.class))).thenAnswer(invocation -> {
            DdcAiResourceBO resource = invocation.getArgument(0);
            resource.setId(RESOURCE_ID).setTenantId(TENANT_ID).setVersion(0L);
            return resource;
        });
        when(resultConverter.toTarget(any(DdcAiResourceBO.class)))
                .thenAnswer(invocation -> summary(invocation.getArgument(0)));

        DdcAiResourceResult result = service.create(command);

        assertThat(result.getResource().getId()).isEqualTo(Long.toString(RESOURCE_ID));
        assertThat(result.getDraft().getPromptText()).isEqualTo(promptText);
        var inserted = org.mockito.ArgumentCaptor.forClass(DdcAiResourceBO.class);
        verify(resourceRepository).insert(inserted.capture());
        assertThat(inserted.getValue().getNamespaceId()).isEqualTo(NAMESPACE_ID);
        assertThat(inserted.getValue().getDraftPrompt()).isEqualTo(promptText);
        assertThat(inserted.getValue().getLastVersionNo()).isZero();
        assertThat(inserted.getValue().getCurrentVersionId()).isNull();
        verifyNoInteractions(draftNodeRepository);
    }

    @Test
    void getLocksTheTenantScopedRootBeforeReadingTheCompleteLiveTree() {
        executeTransactionCallbacksInline(readTransactionTemplate);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 3L, DdcAiKindEnum.SKILL, null, 902L);
        DdcAiDraftNodeBO root = draftNode(91L, "", null, DdcAiNodeTypeEnum.DIRECTORY, null);
        DdcAiDraftNodeBO file = draftNode(92L, "SKILL.md", "", DdcAiNodeTypeEnum.FILE, "check\n");
        when(resourceRepository.findActiveForShareById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(draftNodeRepository.findActiveByResourceId(RESOURCE_ID)).thenReturn(List.of(root, file));
        when(draftNodeConverter.toDTO(root)).thenReturn(node("", DdcAiNodeTypeEnum.DIRECTORY, null));
        when(draftNodeConverter.toDTO(file)).thenReturn(node("SKILL.md", DdcAiNodeTypeEnum.FILE, "check\n"));
        when(strategyRegistry.get(DdcAiKindEnum.SKILL)).thenReturn(skillStrategy);
        when(skillStrategy.kind()).thenReturn(DdcAiKindEnum.SKILL);
        when(skillStrategy.prepare(any(DdcAiContentDTO.class)))
                .thenAnswer(invocation -> prepareSkill(invocation.getArgument(0)));
        when(resultConverter.toTarget(resource)).thenReturn(summary(resource));

        DdcAiResourceResult result = service.get(new DdcAiIdQuery(RESOURCE_ID));

        assertThat(result.getResource().getId()).isEqualTo(Long.toString(RESOURCE_ID));
        assertThat(result.getDraft().getNodes()).extracting(DdcAiNodeDTO::getPath)
                .containsExactly("", "SKILL.md");
        assertThat(result.getDraft().getNodes().getLast().getContent()).isEqualTo("check\n");
        InOrder order = inOrder(resourceRepository, scopeValidator, draftNodeRepository);
        order.verify(resourceRepository).findActiveForShareById(RESOURCE_ID);
        order.verify(scopeValidator).assertCanRead(resource);
        order.verify(draftNodeRepository).findActiveByResourceId(RESOURCE_ID);
    }

    @Test
    void pageValidatesTheFullScopeAndReturnsJoinedSummaryRowsWithoutDraftBodies() {
        executeTransactionCallbacksInline(readTransactionTemplate);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 3L, DdcAiKindEnum.SKILL, null, 902L);
        DdcAiPageQuery query = DdcAiPageQuery.builder().bizCode(SCOPE.bizCode())
                .namespaceCode(SCOPE.namespaceCode()).env(SCOPE.env()).appCode(SCOPE.appCode())
                .kind(DdcAiKindEnum.SKILL).resourceKeyPrefix("skill_").pageNo(2).pageSize(20).build();
        DdcPageResult<DdcAiResourceBO> repositoryPage = DdcPageResult.<DdcAiResourceBO>builder()
                .record(resource).total(31).pageNo(2).pageSize(20).build();
        when(scopeValidator.resolveNamespaceIdForRead(query.toScope())).thenReturn(NAMESPACE_ID);
        when(resourceRepository.search(SCOPE.bizCode(), NAMESPACE_ID, SCOPE.env(), SCOPE.appCode(),
                DdcAiKindEnum.SKILL, "skill_", new PageQuery(2, 20))).thenReturn(repositoryPage);
        when(resultConverter.toTargetList(List.of(resource))).thenReturn(List.of(summary(resource)));

        DdcPageResult<DdcAiResourceSummaryDTO> page = service.page(query);

        assertThat(page.getTotal()).isEqualTo(31);
        assertThat(page.getPageNo()).isEqualTo(2);
        assertThat(page.getPageSize()).isEqualTo(20);
        assertThat(page.getRecords()).singleElement().satisfies(row -> {
            assertThat(row.getId()).isEqualTo(Long.toString(RESOURCE_ID));
            assertThat(row.getScope().namespaceCode()).isEqualTo(SCOPE.namespaceCode());
        });
        verifyNoInteractions(draftNodeRepository);
    }

    @Test
    void scopeValidatorRequiresTheActionPermissionForTheExactNamespace() {
        DdcScopeGate scopeGate = mock(DdcScopeGate.class);
        DdcAiResourceRepository resourceRepository = mock(DdcAiResourceRepository.class);
        DdcNamespaceRepository namespaceRepository = mock(DdcNamespaceRepository.class);
        DdcNamespaceEnvAppBindingRepository bindingRepository = mock(DdcNamespaceEnvAppBindingRepository.class);
        DdcNamespaceBO namespace = DdcNamespaceBO.builder().id(NAMESPACE_ID).publicId("namespace-public-71")
                .bizCode(SCOPE.bizCode()).namespaceCode(SCOPE.namespaceCode()).enabled(true).build();
        when(namespaceRepository.findByBizCodeAndNamespaceCode(SCOPE.bizCode(), SCOPE.namespaceCode()))
                .thenReturn(Optional.of(namespace));
        when(bindingRepository.findAll(SCOPE.bizCode(), SCOPE.namespaceCode(), SCOPE.env(), SCOPE.appCode()))
                .thenReturn(List.of(DdcNamespaceEnvAppBindingBO.builder().enabled(true).build()));
        Rbac3UserDetails principal = mock(Rbac3UserDetails.class);
        when(principal.permissions()).thenReturn(Set.of("TIANSHU_AI_READ:namespace-public-71"));
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new TestingAuthenticationToken(principal, "", List.of()));
        SecurityContextHolder.setContext(securityContext);

        DdcAiScopeValidator validator = new DdcAiScopeValidator(
                scopeGate, resourceRepository, namespaceRepository, bindingRepository);

        assertThat(validator.resolveNamespaceIdForRead(SCOPE)).isEqualTo(NAMESPACE_ID);
        verifyNoInteractions(resourceRepository);
        org.mockito.Mockito.clearInvocations(scopeGate);
        assertThatThrownBy(() -> validator.resolveNamespaceIdForWrite(SCOPE))
                .isInstanceOfSatisfying(CommonException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(56307));
        InOrder order = inOrder(resourceRepository, scopeGate);
        order.verify(resourceRepository).lockScopeParentsForShare(SCOPE);
        order.verify(scopeGate).invalidate("biz:" + SCOPE.bizCode());
        order.verify(scopeGate).assertEnabled(SCOPE.bizCode(), SCOPE.appCode(), SCOPE.env(), SCOPE.namespaceCode());

        org.mockito.Mockito.doReturn(Set.of("TIANSHU_AI_PUBLISH:namespace-public-71"))
                .when(principal).permissions();
        DdcAiResourceBO publishResource = DdcAiResourceBO.builder().bizCode(SCOPE.bizCode())
                .namespaceId(NAMESPACE_ID).namespaceCode(SCOPE.namespaceCode()).envCode(SCOPE.env())
                .appCode(SCOPE.appCode()).build();
        validator.assertCanPublish(publishResource);
        verify(resourceRepository, times(2)).lockScopeParentsForShare(SCOPE);
    }

    @Test
    void aiTransactionTemplatesUseReadCommittedAndStrategyRegistryIsClosedOverBothKinds() {
        DdcAiTransactionConfiguration configuration = new DdcAiTransactionConfiguration();
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionTemplate readTemplate = configuration.ddcAiReadTransactionTemplate(transactionManager);
        TransactionTemplate commandTemplate = configuration.ddcAiCommandTransactionTemplate(transactionManager);
        when(promptStrategy.kind()).thenReturn(DdcAiKindEnum.PROMPT);
        when(skillStrategy.kind()).thenReturn(DdcAiKindEnum.SKILL);
        Map<DdcAiKindEnum, DdcAiContentStrategy> registry =
                configuration.ddcAiContentStrategyRegistry(promptStrategy, skillStrategy);

        assertThat(readTemplate.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertThat(readTemplate.getTimeout()).isEqualTo(8);
        assertThat(readTemplate.isReadOnly()).isFalse();
        assertThat(commandTemplate.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
        assertThat(commandTemplate.getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(commandTemplate.getTimeout()).isEqualTo(8);
        assertThat(commandTemplate.isReadOnly()).isFalse();
        assertThat(registry).containsOnlyKeys(DdcAiKindEnum.PROMPT, DdcAiKindEnum.SKILL);
        assertThat(registry.get(DdcAiKindEnum.PROMPT)).isSameAs(promptStrategy);
        assertThat(registry.get(DdcAiKindEnum.SKILL)).isSameAs(skillStrategy);
    }

    @Test
    void replaceDraftPreservesCurrentVersionAndLastVersionNumber() {
        executeTransactionCallbacksInline(commandTransactionTemplate);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 3L, DdcAiKindEnum.PROMPT, "old", 902L);
        DdcAiDraftCommand command = DdcAiDraftCommand.builder().expectedRevision("3").displayName("Review Order")
                .description("").draft(promptContent("new prompt\n")).build();
        DdcAiResourceBO commandBO = DdcAiResourceBO.builder().displayName("Review Order").description("")
                .draftPrompt("new prompt\n").build();
        when(resourceRepository.findActiveById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(resourceRepository.findActiveForUpdateById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(resourceRepository.update(resource)).thenAnswer(invocation -> {
            resource.setVersion(4L);
            return Optional.of(resource);
        });
        when(resourceConverter.toBusiness(command)).thenReturn(commandBO);
        when(strategyRegistry.get(DdcAiKindEnum.PROMPT)).thenReturn(promptStrategy);
        when(promptStrategy.kind()).thenReturn(DdcAiKindEnum.PROMPT);
        when(promptStrategy.prepare(command.getDraft())).thenReturn(preparedPrompt("new prompt\n"));
        when(resultConverter.toTarget(resource)).thenAnswer(invocation -> summary(resource));

        DdcAiResourceResult result = service.replaceDraft(new DdcAiIdQuery(RESOURCE_ID), command);

        assertThat(result.getDraft().getPromptText()).isEqualTo("new prompt\n");
        assertThat(resource.getCurrentVersionId()).isEqualTo(902L);
        assertThat(resource.getLastVersionNo()).isEqualTo(2L);
        assertThat(resource.getDraftPrompt()).isEqualTo("new prompt\n");
        assertThat(meterRegistry.get("tianshu.ai.commands").tag("operation", "DRAFT_SAVE")
                .tag("result", "success").counter().count()).isEqualTo(1.0);
        InOrder lockOrder = inOrder(resourceRepository, scopeValidator);
        lockOrder.verify(resourceRepository).findActiveById(RESOURCE_ID);
        lockOrder.verify(scopeValidator).assertCanWrite(resource);
        lockOrder.verify(resourceRepository).findActiveForUpdateById(RESOURCE_ID);
        verify(draftNodeRepository, never()).delete(any(DdcAiDraftNodeBO.class));
    }

    @Test
    void staleRevisionFailsBeforeAnyDraftMutation() {
        executeTransactionCallbacksInline(commandTransactionTemplate);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 4L, DdcAiKindEnum.PROMPT, "old", 902L);
        DdcAiDraftCommand command = DdcAiDraftCommand.builder().expectedRevision("3").displayName("Review Order")
                .description("").draft(promptContent("new prompt")).build();
        when(resourceRepository.findActiveById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(resourceRepository.findActiveForUpdateById(RESOURCE_ID)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> service.replaceDraft(new DdcAiIdQuery(RESOURCE_ID), command))
                .isInstanceOfSatisfying(CommonException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(56303));

        verify(scopeValidator).assertCanWrite(resource);
        verify(resourceRepository, never()).update(any(DdcAiResourceBO.class));
        verifyNoInteractions(draftNodeRepository);
    }

    @Test
    void deleteSoftDeletesNodesAndResourceWithinTheCommandTransaction() {
        executeTransactionCallbacksInline(commandTransactionTemplate);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 2L, DdcAiKindEnum.SKILL, null, 902L);
        DdcAiDraftNodeBO root = draftNode(91L, "", null, DdcAiNodeTypeEnum.DIRECTORY, null);
        DdcAiDraftNodeBO file = draftNode(92L, "SKILL.md", "", DdcAiNodeTypeEnum.FILE, "check\n");
        when(resourceRepository.findActiveById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(resourceRepository.findActiveForUpdateById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(draftNodeRepository.findActiveByResourceId(RESOURCE_ID)).thenReturn(List.of(root, file));
        when(draftNodeRepository.delete(root)).thenReturn(true);
        when(draftNodeRepository.delete(file)).thenReturn(true);
        when(resourceRepository.delete(resource)).thenReturn(true);

        service.delete(new DdcAiIdQuery(RESOURCE_ID), DdcAiDeleteCommand.builder().expectedRevision("2").build());

        InOrder order = inOrder(resourceRepository, scopeValidator, draftNodeRepository);
        order.verify(resourceRepository).findActiveById(RESOURCE_ID);
        order.verify(scopeValidator).assertCanWrite(resource);
        order.verify(resourceRepository).findActiveForUpdateById(RESOURCE_ID);
        order.verify(draftNodeRepository).delete(root);
        order.verify(draftNodeRepository).delete(file);
        order.verify(resourceRepository).delete(resource);
    }

    @Test
    void deleteOfAlreadyDeletedSameTenantResourceIsIdempotent() {
        executeTransactionCallbacksInline(commandTransactionTemplate);
        DdcAiResourceBO deleted = resource(RESOURCE_ID, 3L, DdcAiKindEnum.PROMPT, "old", 902L);
        deleted.setDeletedAt(java.time.LocalDateTime.parse("2026-09-29T09:00:00"));
        when(resourceRepository.findActiveById(RESOURCE_ID)).thenReturn(Optional.empty());
        when(resourceRepository.findIncludingDeletedById(RESOURCE_ID)).thenReturn(Optional.of(deleted));

        service.delete(new DdcAiIdQuery(RESOURCE_ID), DdcAiDeleteCommand.builder().expectedRevision("2").build());

        verify(scopeValidator).assertCanWrite(deleted);
        verify(resourceRepository, never()).delete(any(DdcAiResourceBO.class));
        verifyNoInteractions(draftNodeRepository);
    }

    @Test
    void deletedAtLifecycleCollisionRollsBackAndRetriesThreeFreshTransactions() {
        executeTransactionCallbacksInline(commandTransactionTemplate);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 2L, DdcAiKindEnum.PROMPT, "old", 902L);
        when(resourceRepository.findActiveById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(resourceRepository.findActiveForUpdateById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(draftNodeRepository.findActiveByResourceId(RESOURCE_ID)).thenReturn(List.of());
        DataIntegrityViolationException collision = new DataIntegrityViolationException(
                "duplicate key violates constraint uk_ai_resource_lifecycle");
        when(resourceRepository.delete(resource)).thenThrow(collision, collision, collision, collision);

        assertThatThrownBy(() -> service.delete(new DdcAiIdQuery(RESOURCE_ID),
                DdcAiDeleteCommand.builder().expectedRevision("2").build()))
                .isInstanceOfSatisfying(CommonException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(56316));

        verify(commandTransactionTemplate, org.mockito.Mockito.times(4))
                .execute(any(TransactionCallback.class));
        verify(resourceRepository, org.mockito.Mockito.times(4)).findActiveForUpdateById(RESOURCE_ID);
        verify(resourceRepository, org.mockito.Mockito.times(4)).delete(resource);
        assertThat(resource.getDeletedAt()).isNull();
        assertThat(resource.getVersion()).isEqualTo(2L);
    }

    @Test
    void draftNodeLifecycleCollisionAlsoRetriesBeforeReturning409() {
        executeTransactionCallbacksInline(commandTransactionTemplate);
        DdcAiResourceBO resource = resource(RESOURCE_ID, 2L, DdcAiKindEnum.SKILL, null, 902L);
        DdcAiDraftNodeBO root = draftNode(91L, "", null, DdcAiNodeTypeEnum.DIRECTORY, null);
        when(resourceRepository.findActiveById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(resourceRepository.findActiveForUpdateById(RESOURCE_ID)).thenReturn(Optional.of(resource));
        when(draftNodeRepository.findActiveByResourceId(RESOURCE_ID)).thenReturn(List.of(root));
        DataIntegrityViolationException collision = new DataIntegrityViolationException(
                "duplicate key violates constraint uk_ai_draft_node_lifecycle");
        when(draftNodeRepository.delete(root)).thenThrow(collision, collision, collision, collision);

        assertThatThrownBy(() -> service.delete(new DdcAiIdQuery(RESOURCE_ID),
                DdcAiDeleteCommand.builder().expectedRevision("2").build()))
                .isInstanceOfSatisfying(CommonException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(56316));

        verify(commandTransactionTemplate, org.mockito.Mockito.times(4))
                .execute(any(TransactionCallback.class));
        verify(resourceRepository, org.mockito.Mockito.times(4)).findActiveForUpdateById(RESOURCE_ID);
        verify(draftNodeRepository, org.mockito.Mockito.times(4)).delete(root);
        verify(resourceRepository, never()).delete(any(DdcAiResourceBO.class));
        assertThat(resource.getDeletedAt()).isNull();
        assertThat(resource.getVersion()).isEqualTo(2L);
    }

    private DdcAiCreateCommand createCommand(String promptText) {
        return DdcAiCreateCommand.builder().scope(SCOPE).kind(DdcAiKindEnum.PROMPT).resourceKey("review-order")
                .displayName("Review Order").description("").draft(promptContent(promptText)).build();
    }

    private DdcAiContentDTO promptContent(String promptText) {
        return DdcAiContentDTO.builder().promptText(promptText).nodes(List.of()).build();
    }

    private DdcAiContentDTO skillContent(List<DdcAiNodeDTO> nodes) {
        return DdcAiContentDTO.builder().promptText(null).nodes(nodes).build();
    }

    private DdcAiNodeDTO node(String path, DdcAiNodeTypeEnum type, String content) {
        return DdcAiNodeDTO.builder().path(path).nodeType(type).content(content).build();
    }

    private DdcAiDraftNodeBO draftNode(Long id, String path, String parentPath, DdcAiNodeTypeEnum type,
            String content) {
        String hash = type == DdcAiNodeTypeEnum.FILE
                ? DdcAiContentStrategy.sha256Hex(content.getBytes(StandardCharsets.UTF_8)) : null;
        long contentBytes = content == null ? 0 : content.getBytes(StandardCharsets.UTF_8).length;
        return DdcAiDraftNodeBO.builder().id(id).tenantId(TENANT_ID).version(0L).resourceId(RESOURCE_ID)
                .parentPath(parentPath).path(path).nodeType(type).content(content).contentSha256(hash)
                .contentBytes(contentBytes).build();
    }

    private DdcAiResourceBO resource(Long id, Long revision, DdcAiKindEnum kind, String draftPrompt,
            Long currentVersionId) {
        return DdcAiResourceBO.builder().id(id).tenantId(TENANT_ID).version(revision).bizCode(SCOPE.bizCode())
                .namespaceId(NAMESPACE_ID).namespaceCode(SCOPE.namespaceCode()).envCode(SCOPE.env())
                .appCode(SCOPE.appCode()).kind(kind).resourceKey("review-order").displayName("Review Order")
                .description("").draftPrompt(draftPrompt).lastVersionNo(2L).currentVersionId(currentVersionId)
                .updateTime(Instant.parse("2026-09-29T09:00:00Z")).build();
    }

    private DdcAiResourceSummaryDTO summary(DdcAiResourceBO resource) {
        return DdcAiResourceSummaryDTO.builder().id(Long.toString(resource.getId())).scope(SCOPE)
                .kind(resource.getKind()).resourceKey(resource.getResourceKey()).displayName(resource.getDisplayName())
                .description(resource.getDescription()).revision(Long.toString(resource.getVersion()))
                .lastVersionNo(Long.toString(resource.getLastVersionNo()))
                .currentVersionId(resource.getCurrentVersionId() == null ? null : resource.getCurrentVersionId().toString())
                .currentVersionNo(resource.getCurrentVersionNo() == null ? null : resource.getCurrentVersionNo().toString())
                .updatedAt(resource.getUpdateTime()).build();
    }

    private PreparedContent preparedPrompt(String promptText) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return new DdcPromptContentStrategy(new ValidationUtils(factory.getValidator()))
                    .prepare(promptContent(promptText));
        }
    }

    private PreparedContent prepareSkill(DdcAiContentDTO content) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return new DdcSkillContentStrategy(new ValidationUtils(factory.getValidator())).prepare(content);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void executeTransactionCallbacksInline(TransactionTemplate template) {
        doAnswer(invocation -> ((TransactionCallback) invocation.getArgument(0))
                .doInTransaction(mock(TransactionStatus.class)))
                .when(template).execute(any(TransactionCallback.class));
    }
}
