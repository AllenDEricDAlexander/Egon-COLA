package top.egon.cola.component.tianshu.admin.rpc.provider;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Component;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiNode;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiNodeType;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPublicationReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiRevisionResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillDraftUpload;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillPublishRequest;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiVersionHead;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiPublicationReceiptResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiRevisionResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiVersionHeadResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPromptResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiResourceQuery;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillResult;
import top.egon.cola.component.rpc.tianshu.client.ai.DdcAiProtoConverterImpl;
import top.egon.cola.component.rpc.tianshu.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiResourceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceBO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcNamespaceEnvAppBindingBO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiVersionSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiVersionResult;
import top.egon.cola.component.tianshu.admin.repository.DdcAiResourceRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceEnvAppBindingRepository;
import top.egon.cola.component.tianshu.admin.repository.DdcNamespaceRepository;
import top.egon.cola.component.tianshu.admin.security.rpc.DdcServicePrincipal;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiOutcome;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReport;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportBatch;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportItemReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportStatus;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiUsageReportBatchCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiUsageReportCommand;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiSyncCommandConverterImpl;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiUsageReportReceiptResult;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService;
import top.egon.cola.component.tianshu.model.ai.DdcAiUsageReportStatusEnum;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiResourceService;
import top.egon.cola.component.tianshu.admin.service.ai.validation.DdcAiScopeValidator;
import top.egon.cola.component.tianshu.admin.service.metadata.DdcScopeGate;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcAiResourceRpcProviderTest {

    private static final long TENANT_ID = 1001L;
    private static final long RESOURCE_ID = 810001L;
    private static final long VERSION_ID = 820004L;
    private static final String HASH = "a".repeat(64);
    private static final Instant CREATED_AT = Instant.parse("2026-09-29T10:00:00Z");
    private static final DdcAiScopeDTO SCOPE = new DdcAiScopeDTO(
            "retail", "prod-space", "prod", "assistant-app");

    @AfterEach
    void clearThreadContext() {
        MDC.clear();
    }

    @Test
    void springProviderUsesItsExplicitBeanNameAndQualifiedConstructorDependencies() {
        new ApplicationContextRunner()
                .withUserConfiguration(ProviderConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("ddcAiResourceRpcProvider");
                    assertThat(DdcAiResourceRpcProvider.class.getAnnotation(Component.class).value())
                            .isEqualTo("ddcAiResourceRpcProvider");
                    var constructor = DdcAiResourceRpcProvider.class.getDeclaredConstructors()[0];
                    assertThat(constructor.getParameters()[0].getAnnotation(
                            org.springframework.beans.factory.annotation.Qualifier.class).value())
                            .isEqualTo("ddcAiResourceService");
                    assertThat(constructor.getParameters()[1].getAnnotation(
                            org.springframework.beans.factory.annotation.Qualifier.class).value())
                            .isEqualTo("ddcAiProtoConverterImpl");
                    assertThat(constructor.getParameters()[2].getAnnotation(
                            org.springframework.beans.factory.annotation.Qualifier.class).value())
                            .isEqualTo("ddcAiFeedbackService");
                    assertThat(constructor.getParameters()[3].getAnnotation(
                            org.springframework.beans.factory.annotation.Qualifier.class).value())
                            .isEqualTo("ddcAiSyncCommandConverterImpl");
                });
    }

    @Test
    void promptReadDelegatesToTheSinglePublishedVersionServiceQuery() {
        DdcAiResourceService service = mock(DdcAiResourceService.class);
        DdcAiResourceRpcProvider provider = provider(service);
        DdcAiVersionResult published = versionResult(DdcAiKindEnum.PROMPT,
                DdcAiContentDTO.builder().promptText("approved prompt").nodes(List.of()).build());
        when(service.getPublishedVersion(any(), eq(DdcAiKindEnum.PROMPT))).thenReturn(published);
        AiResourceQuery query = query("review-order", 4L);

        AiPromptResult response = withPrincipal(principal(TENANT_ID, Set.of("prod-*")),
                () -> provider.getPrompt(query));

        assertThat(response.getResourceId()).isEqualTo("810001");
        assertThat(response.getResourceKey()).isEqualTo("review-order");
        assertThat(response.getVersionId()).isEqualTo("820004");
        assertThat(response.getVersionNo()).isEqualTo(4L);
        assertThat(response.getContentSha256()).isEqualTo(HASH);
        assertThat(response.getTemplate()).isEqualTo("approved prompt");
        verify(service).getPublishedVersion(argThat(value -> value.resourceId().equals(RESOURCE_ID)
                && value.namespaceCode().equals(SCOPE.namespaceCode())
                && value.resourceKey().equals("review-order")
                && value.versionNo().equals(4L)), eq(DdcAiKindEnum.PROMPT));
    }

    @Test
    void skillReadReturnsTheCompleteTreeAndPreservesDirectoryAndEmptyFilePresence() {
        DdcAiResourceService service = mock(DdcAiResourceService.class);
        DdcAiResourceRpcProvider provider = provider(service);
        DdcAiVersionResult published = versionResult(DdcAiKindEnum.SKILL,
                DdcAiContentDTO.builder().nodes(List.of(
                        DdcAiNodeDTO.builder().path("").nodeType(DdcAiNodeTypeEnum.DIRECTORY).build(),
                        DdcAiNodeDTO.builder().path("README.txt").nodeType(DdcAiNodeTypeEnum.FILE)
                                .content("").build())).build());
        when(service.getPublishedVersion(any(), eq(DdcAiKindEnum.SKILL))).thenReturn(published);

        AiSkillResult response = withPrincipal(principal(TENANT_ID, Set.of("prod-space")),
                () -> provider.getSkill(query("review-order", null)));

        assertThat(response.getFilesList()).hasSize(2);
        assertThat(response.getFiles(0).getNodeType()).isEqualTo(AiNodeType.DIRECTORY);
        assertThat(response.getFiles(0).hasContent()).isFalse();
        assertThat(response.getFiles(1).getNodeType()).isEqualTo(AiNodeType.FILE);
        assertThat(response.getFiles(1).hasContent()).isTrue();
        assertThat(response.getFiles(1).getContent()).isEmpty();
        verify(service).getPublishedVersion(any(), eq(DdcAiKindEnum.SKILL));
    }

    @Test
    void knownAiScopeFailureKeepsPermissionStatusAndTypedTrailer() {
        DdcAiResourceService service = mock(DdcAiResourceService.class);
        DdcAiResourceRpcProvider provider = provider(service);
        when(service.getPublishedVersion(any(), eq(DdcAiKindEnum.PROMPT)))
                .thenThrow(new CommonException(56307, "TIANSHU_AI_SCOPE_FORBIDDEN",
                        "AI permission is required for the requested namespace"));

        assertThatThrownBy(() -> withPrincipal(principal(TENANT_ID, Set.of("*")),
                () -> provider.getPrompt(query("review-order", null))))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(failure -> {
                    StatusRuntimeException status = (StatusRuntimeException) failure;
                    assertThat(status.getStatus().getCode()).isEqualTo(Status.Code.PERMISSION_DENIED);
                    assertThat(status.getTrailers()
                            .get(DdcRpcStatusExceptionMapper.ERROR_DETAIL).getCode())
                            .isEqualTo("TIANSHU_AI_SCOPE_FORBIDDEN");
                });
    }

    @Test
    void unpublishedResourceRemainsANotFoundStatusForAiConsumers() {
        DdcAiResourceService service = mock(DdcAiResourceService.class);
        DdcAiResourceRpcProvider provider = provider(service);
        when(service.getPublishedVersion(any(), eq(DdcAiKindEnum.PROMPT)))
                .thenThrow(new CommonException(56306, "TIANSHU_AI_NOT_PUBLISHED",
                        "Resource has no published version"));

        assertThatThrownBy(() -> withPrincipal(principal(TENANT_ID, Set.of("*")),
                () -> provider.getPrompt(query("review-order", null))))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(failure -> {
                    StatusRuntimeException status = (StatusRuntimeException) failure;
                    assertThat(status.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
                    assertThat(status.getTrailers()
                            .get(DdcRpcStatusExceptionMapper.ERROR_DETAIL).getCode())
                            .isEqualTo("TIANSHU_AI_NOT_PUBLISHED");
                });
    }

    @Test
    void trustedServicePrincipalIsLimitedToTenantAndNamespaceAiRead() {
        DdcScopeGate scopeGate = mock(DdcScopeGate.class);
        DdcAiResourceRepository resourceRepository = mock(DdcAiResourceRepository.class);
        DdcNamespaceRepository namespaceRepository = mock(DdcNamespaceRepository.class);
        DdcNamespaceEnvAppBindingRepository bindingRepository = mock(DdcNamespaceEnvAppBindingRepository.class);
        DdcNamespaceBO namespace = DdcNamespaceBO.builder().id(71L).publicId("namespace-prod-71")
                .bizCode(SCOPE.bizCode()).namespaceCode(SCOPE.namespaceCode()).enabled(true).build();
        when(namespaceRepository.findByBizCodeAndNamespaceCode(SCOPE.bizCode(), SCOPE.namespaceCode()))
                .thenReturn(Optional.of(namespace));
        when(bindingRepository.findAll(SCOPE.bizCode(), SCOPE.namespaceCode(), SCOPE.env(), SCOPE.appCode()))
                .thenReturn(List.of(DdcNamespaceEnvAppBindingBO.builder().enabled(true).build()));
        DdcAiScopeValidator validator = new DdcAiScopeValidator(
                scopeGate, resourceRepository, namespaceRepository, bindingRepository);
        DdcAiResourceBO resource = DdcAiResourceBO.builder().id(RESOURCE_ID).tenantId(TENANT_ID)
                .bizCode(SCOPE.bizCode()).namespaceId(71L).namespaceCode(SCOPE.namespaceCode())
                .envCode(SCOPE.env()).appCode(SCOPE.appCode()).build();

        MDC.put("tenantId", Long.toString(TENANT_ID));
        DdcServicePrincipal permitted = principal(TENANT_ID, Set.of("prod-*"));
        withPrincipal(permitted, () -> {
            validator.assertCanRead(resource);
            assertThatThrownBy(() -> validator.assertCanWrite(resource))
                    .isInstanceOfSatisfying(CommonException.class,
                            failure -> assertThat(failure.getCode()).isEqualTo(56307));
            return null;
        });

        DdcServicePrincipal wrongTenant = principal(TENANT_ID + 1, Set.of("prod-*"));
        assertThatThrownBy(() -> withPrincipal(wrongTenant, () -> {
            validator.assertCanRead(resource);
            return null;
        }))
                .isInstanceOfSatisfying(CommonException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(56307));

        DdcServicePrincipal wrongNamespace = principal(TENANT_ID, Set.of("dev-*"));
        assertThatThrownBy(() -> withPrincipal(wrongNamespace, () -> {
            validator.assertCanRead(resource);
            return null;
        }))
                .isInstanceOfSatisfying(CommonException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(56307));

        DdcServicePrincipal missingAiRead = principal(TENANT_ID, Set.of("prod-*"),
                "assistant-app", "prod", "retail", Set.of("CONFIG_PULL"));
        assertThatThrownBy(() -> withPrincipal(missingAiRead, () -> {
            validator.assertCanRead(resource);
            return null;
        }))
                .isInstanceOfSatisfying(CommonException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(56307));

        DdcServicePrincipal wrongBusinessScope = principal(TENANT_ID, Set.of("prod-*"),
                "assistant-app", "prod", "other-biz", Set.of("AI_READ"));
        assertThatThrownBy(() -> withPrincipal(wrongBusinessScope, () -> {
            validator.assertCanRead(resource);
            return null;
        }))
                .isInstanceOfSatisfying(CommonException.class,
                        failure -> assertThat(failure.getCode()).isEqualTo(56307));
        verify(resourceRepository).lockScopeParentsForShare(SCOPE);
    }

    private DdcAiVersionResult versionResult(DdcAiKindEnum kind, DdcAiContentDTO content) {
        DdcAiVersionSummaryDTO summary = DdcAiVersionSummaryDTO.builder()
                .id(VERSION_ID).resourceId(RESOURCE_ID).versionNo(4L).kind(kind.name())
                .displayName("Review Order").description("").contentSha256(HASH)
                .contentBytes(16L).nodeCount(content.getNodes().size()).changeNote("approved")
                .createdAt(CREATED_AT).createdBy("operator").build();
        return DdcAiVersionResult.builder().version(summary).content(content).build();
    }

    private AiResourceQuery query(String key, Long versionNo) {
        AiResourceQuery.Builder query = AiResourceQuery.newBuilder().setBizCode(SCOPE.bizCode())
                .setNamespaceCode(SCOPE.namespaceCode()).setEnv(SCOPE.env()).setAppCode(SCOPE.appCode())
                .setResourceKey(key).setResourceId(Long.toString(RESOURCE_ID));
        if (versionNo != null) query.setVersionNo(versionNo);
        return query.build();
    }

    @Test
    void reportUsageDelegatesToFeedbackServiceAndPreservesOrder() {
        DdcAiFeedbackService feedbackService = mock(DdcAiFeedbackService.class);
        DdcAiResourceRpcProvider provider = new DdcAiResourceRpcProvider(mock(DdcAiResourceService.class),
                new DdcAiProtoConverterImpl(), feedbackService, new DdcAiSyncCommandConverterImpl());
        when(feedbackService.accept(org.mockito.ArgumentMatchers.any())).thenReturn(DdcAiUsageReportReceiptResult.builder()
                .items(java.util.List.of(
                        receiptItem("01JAX0M7Q9K2R5T8V1W4X7Y0Z1", DdcAiUsageReportStatusEnum.ACCEPTED, null),
                        receiptItem("01JAX0M7Q9K2R5T8V1W4X7Y0Z2", DdcAiUsageReportStatusEnum.ACCEPTED_DUPLICATE, null),
                        receiptItem("bad", DdcAiUsageReportStatusEnum.REJECTED, "INVALID_FIELD")))
                .build());
        AiUsageReportBatch batch = AiUsageReportBatch.newBuilder()
                .addReports(usage("01JAX0M7Q9K2R5T8V1W4X7Y0Z1", "810001", "2026-10-17T04:58:12.345Z"))
                .addReports(usage("01JAX0M7Q9K2R5T8V1W4X7Y0Z2", "810001", "2026-10-17T04:58:13Z"))
                .addReports(usage("bad", "not-a-number", "yesterday"))
                .build();

        AiUsageReportReceipt receipt = withPrincipal(principal(TENANT_ID, Set.of("*")), () -> provider.reportUsage(batch));

        assertThat(receipt.getItemsList()).extracting(AiUsageReportItemReceipt::getStatus).containsExactly(
                AiUsageReportStatus.AI_USAGE_REPORT_ACCEPTED, AiUsageReportStatus.AI_USAGE_REPORT_ACCEPTED_DUPLICATE,
                AiUsageReportStatus.AI_USAGE_REPORT_REJECTED);
        assertThat(receipt.getItems(2).getReason()).isEqualTo("INVALID_FIELD");
        org.mockito.ArgumentCaptor<DdcAiUsageReportBatchCommand> sent =
                org.mockito.ArgumentCaptor.forClass(DdcAiUsageReportBatchCommand.class);
        verify(feedbackService).accept(sent.capture());
        assertThat(sent.getValue().getReports()).extracting(DdcAiUsageReportCommand::getUsageId)
                .containsExactly("01JAX0M7Q9K2R5T8V1W4X7Y0Z1", "01JAX0M7Q9K2R5T8V1W4X7Y0Z2", "bad");
        assertThat(sent.getValue().getReports().get(0).getResourceId()).isEqualTo(810001L);
        assertThat(sent.getValue().getReports().get(0).getOccurredAt())
                .isEqualTo(java.time.Instant.parse("2026-10-17T04:58:12.345Z"));
        assertThat(sent.getValue().getReports().get(2).getResourceId()).isNull();
        assertThat(sent.getValue().getReports().get(2).getOccurredAt()).isNull();
    }

    @Test
    void reportUsageRejectsEmptyOrOversizedBatchWithFeedbackInvalidTrailer() {
        DdcAiFeedbackService feedbackService = mock(DdcAiFeedbackService.class);
        DdcAiResourceRpcProvider provider = new DdcAiResourceRpcProvider(mock(DdcAiResourceService.class),
                new DdcAiProtoConverterImpl(), feedbackService, new DdcAiSyncCommandConverterImpl());
        AiUsageReportBatch.Builder oversized = AiUsageReportBatch.newBuilder();
        for (int index = 0; index < 101; index++) {
            oversized.addReports(usage("01JAX0M7Q9K2R5T8V1W4X7Y0Z" + index, "810001", "2026-10-17T04:58:12Z"));
        }

        for (AiUsageReportBatch batch : java.util.List.of(AiUsageReportBatch.getDefaultInstance(), oversized.build())) {
            assertThatThrownBy(() -> withPrincipal(principal(TENANT_ID, Set.of("*")), () -> provider.reportUsage(batch)))
                    .isInstanceOfSatisfying(io.grpc.StatusRuntimeException.class, failure -> {
                        assertThat(failure.getStatus().getCode()).isEqualTo(io.grpc.Status.Code.INVALID_ARGUMENT);
                        assertThat(failure.getTrailers().get(
                                top.egon.cola.component.rpc.tianshu.mapping.DdcRpcStatusExceptionMapper.ERROR_DETAIL)
                                .getCode()).isEqualTo("TIANSHU_AI_FEEDBACK_INVALID");
                    });
        }
        org.mockito.Mockito.verifyNoInteractions(feedbackService);
    }

    @Test
    void reportUsageRequiresAnAuthenticatedPrincipal() {
        DdcAiResourceRpcProvider provider = new DdcAiResourceRpcProvider(mock(DdcAiResourceService.class),
                new DdcAiProtoConverterImpl(), mock(DdcAiFeedbackService.class), new DdcAiSyncCommandConverterImpl());

        assertThatThrownBy(() -> provider.reportUsage(AiUsageReportBatch.newBuilder()
                .addReports(usage("01JAX0M7Q9K2R5T8V1W4X7Y0Z1", "810001", "2026-10-17T04:58:12Z")).build()))
                .isInstanceOfSatisfying(io.grpc.StatusRuntimeException.class, failure ->
                        assertThat(failure.getStatus().getCode()).isEqualTo(io.grpc.Status.Code.PERMISSION_DENIED));
    }

    @Test
    void getVersionHeadReturnsUnpublishedHeadWithoutVersion() {
        DdcAiResourceService service = mock(DdcAiResourceService.class);
        DdcAiResourceRpcProvider provider = provider(service);
        when(service.getVersionHead(any())).thenReturn(DdcAiVersionHeadResult.builder().resourceId(RESOURCE_ID)
                .resourceKey("refund-skill").resourceRevision(3L).published(false).build());

        AiVersionHead head = withPrincipal(principal(TENANT_ID, Set.of("prod-*")),
                () -> provider.getVersionHead(query("refund-skill", null)));

        assertThat(head.getPublished()).isFalse();
        assertThat(head.hasVersionId()).isFalse();
        assertThat(head.hasVersionNo()).isFalse();
        assertThat(head.hasContentSha256()).isFalse();
        assertThat(head.getResourceRevision()).isEqualTo("3");
        verify(service).getVersionHead(argThat(value -> value.resourceId().equals(RESOURCE_ID)
                && value.versionNo() == null && value.resourceKey().equals("refund-skill")));
    }

    @Test
    void getVersionHeadReturnsCurrentVersionHeadAndRejectsAPinnedVersion() {
        DdcAiResourceService service = mock(DdcAiResourceService.class);
        DdcAiResourceRpcProvider provider = provider(service);
        when(service.getVersionHead(any())).thenReturn(DdcAiVersionHeadResult.builder().resourceId(RESOURCE_ID)
                .resourceKey("refund-skill").versionId(VERSION_ID).versionNo(4L).contentSha256(HASH)
                .resourceRevision(9L).published(true).build());

        AiVersionHead head = withPrincipal(principal(TENANT_ID, Set.of("prod-*")),
                () -> provider.getVersionHead(query("refund-skill", null)));

        assertThat(head.getPublished()).isTrue();
        assertThat(head.getVersionId()).isEqualTo("820004");
        assertThat(head.getVersionNo()).isEqualTo(4L);
        assertThat(head.getContentSha256()).isEqualTo(HASH);
        assertThatThrownBy(() -> withPrincipal(principal(TENANT_ID, Set.of("prod-*")),
                () -> provider.getVersionHead(query("refund-skill", 4L))))
                .isInstanceOfSatisfying(StatusRuntimeException.class, failure -> {
                    assertThat(failure.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
                    assertThat(failure.getTrailers().get(DdcRpcStatusExceptionMapper.ERROR_DETAIL).getCode())
                            .isEqualTo("TIANSHU_AI_VALIDATION_ERROR");
                });
    }

    @Test
    void replaceSkillDraftConvertsTheWholeTreeAndReturnsTheRevisionReceipt() {
        DdcAiResourceService service = mock(DdcAiResourceService.class);
        DdcAiResourceRpcProvider provider = provider(service);
        when(service.replaceSkillDraft(any())).thenReturn(DdcAiRevisionResult.builder().resourceId(RESOURCE_ID)
                .resourceRevision(4L).draftSha256(HASH).draftMatchesCurrent(false).build());

        AiRevisionResult receipt = withPrincipal(writer(),
                () -> provider.replaceSkillDraft(upload("3")));

        assertThat(receipt.getResourceId()).isEqualTo("810001");
        assertThat(receipt.getResourceRevision()).isEqualTo("4");
        assertThat(receipt.getDraftSha256()).isEqualTo(HASH);
        assertThat(receipt.getDraftMatchesCurrent()).isFalse();
        verify(service).replaceSkillDraft(argThat(command -> command.getResourceId().equals(RESOURCE_ID)
                && "3".equals(command.getExpectedRevision())
                && command.getNamespaceCode().equals(SCOPE.namespaceCode())
                && command.getContent().getPromptText() == null
                && command.getContent().getNodes().size() == 2
                && command.getContent().getNodes().get(0).getNodeType() == DdcAiNodeTypeEnum.DIRECTORY
                && "# refund".equals(command.getContent().getNodes().get(1).getContent())));
    }

    @Test
    void replaceSkillDraftMapsRevisionConflictAndKindMismatchToTypedStatuses() {
        DdcAiResourceService service = mock(DdcAiResourceService.class);
        DdcAiResourceRpcProvider provider = provider(service);
        when(service.replaceSkillDraft(any()))
                .thenThrow(new CommonException(56303, "TIANSHU_AI_REVISION_CONFLICT", "stale"))
                .thenThrow(new CommonException(56308, "TIANSHU_AI_KIND_MISMATCH", "kind"));

        assertThatThrownBy(() -> withPrincipal(writer(), () -> provider.replaceSkillDraft(upload("3"))))
                .isInstanceOfSatisfying(StatusRuntimeException.class, failure -> {
                    assertThat(failure.getStatus().getCode()).isEqualTo(Status.Code.ABORTED);
                    assertThat(failure.getTrailers().get(DdcRpcStatusExceptionMapper.ERROR_DETAIL).getCode())
                            .isEqualTo("TIANSHU_AI_REVISION_CONFLICT");
                });
        assertThatThrownBy(() -> withPrincipal(writer(), () -> provider.replaceSkillDraft(upload("3"))))
                .isInstanceOfSatisfying(StatusRuntimeException.class, failure ->
                        assertThat(failure.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    @Test
    void replaceSkillDraftRejectsInvalidUploadsBeforeTheService() {
        DdcAiResourceService service = mock(DdcAiResourceService.class);
        DdcAiResourceRpcProvider provider = provider(service);
        when(service.replaceSkillDraft(any())).thenThrow(new jakarta.validation.ConstraintViolationException(
                "invalid", Set.of()));

        assertThatThrownBy(() -> withPrincipal(writer(), () -> provider.replaceSkillDraft(
                upload("3").toBuilder().setResourceId("not-a-number").build())))
                .isInstanceOfSatisfying(StatusRuntimeException.class, failure ->
                        assertThat(failure.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
        assertThatThrownBy(() -> provider.replaceSkillDraft(upload("3")))
                .isInstanceOfSatisfying(StatusRuntimeException.class, failure ->
                        assertThat(failure.getStatus().getCode()).isEqualTo(Status.Code.PERMISSION_DENIED));
    }

    @Test
    void publishSkillDraftUsesRequestIdAndReturnsReceipt() {
        DdcAiResourceService service = mock(DdcAiResourceService.class);
        DdcAiResourceRpcProvider provider = provider(service);
        when(service.publishSkillDraft(any())).thenReturn(DdcAiPublicationReceiptResult.builder()
                .publicationId(555L).resourceId(RESOURCE_ID).targetVersionId(VERSION_ID).targetVersionNo(4L)
                .resourceRevision(8L).contentSha256(HASH).build());

        AiPublicationReceipt receipt = withPrincipal(writer(), () -> provider.publishSkillDraft(
                AiSkillPublishRequest.newBuilder().setNamespaceCode(SCOPE.namespaceCode())
                        .setResourceKey("refund-skill").setResourceId(Long.toString(RESOURCE_ID))
                        .setExpectedRevision("7").setRequestId("auth-0123456789abcdef")
                        .setChangeNote("from authoring").build()));

        assertThat(receipt.getPublicationId()).isEqualTo("555");
        assertThat(receipt.getTargetVersionId()).isEqualTo("820004");
        assertThat(receipt.getTargetVersionNo()).isEqualTo(4L);
        assertThat(receipt.getResourceRevision()).isEqualTo("8");
        assertThat(receipt.getContentSha256()).isEqualTo(HASH);
        verify(service).publishSkillDraft(argThat(command -> command.getResourceId().equals(RESOURCE_ID)
                && "auth-0123456789abcdef".equals(command.getRequestId())
                && "7".equals(command.getExpectedRevision())
                && "from authoring".equals(command.getChangeNote())));
    }

    private DdcServicePrincipal writer() {
        return principal(TENANT_ID, Set.of("prod-*"), "assistant-app", "prod", "retail",
                Set.of("AI_READ", "AI_WRITE", "AI_PUBLISH"));
    }

    private AiSkillDraftUpload upload(String expectedRevision) {
        return AiSkillDraftUpload.newBuilder().setNamespaceCode(SCOPE.namespaceCode()).setResourceKey("refund-skill")
                .setResourceId(Long.toString(RESOURCE_ID)).setExpectedRevision(expectedRevision)
                .setDisplayName("Refund").setDescription("")
                .addFiles(AiNode.newBuilder().setPath("").setNodeType(AiNodeType.DIRECTORY))
                .addFiles(AiNode.newBuilder().setPath("SKILL.md").setNodeType(AiNodeType.FILE).setContent("# refund"))
                .build();
    }

    private static AiUsageReport usage(String usageId, String resourceId, String occurredAt) {
        return AiUsageReport.newBuilder().setUsageId(usageId).setNamespaceCode("prod-space")
                .setResourceKey("review-order").setResourceId(resourceId).setVersionId("820004").setVersionNo(4L)
                .setOutcome(AiOutcome.AI_OUTCOME_FAILURE).setOccurredAt(occurredAt).build();
    }

    private static DdcAiUsageReportReceiptResult.ItemResult receiptItem(String usageId,
            DdcAiUsageReportStatusEnum status, String reason) {
        return DdcAiUsageReportReceiptResult.ItemResult.builder().usageId(usageId).status(status).reason(reason).build();
    }

    private DdcAiResourceRpcProvider provider(DdcAiResourceService service) {
        return new DdcAiResourceRpcProvider(service, new DdcAiProtoConverterImpl(),
                mock(DdcAiFeedbackService.class), new DdcAiSyncCommandConverterImpl());
    }

    private DdcServicePrincipal principal(long tenantId, Set<String> namespacePatterns) {
        return principal(tenantId, namespacePatterns, "assistant-app", "prod", "retail", Set.of("AI_READ"));
    }

    private DdcServicePrincipal principal(
            long tenantId,
            Set<String> namespacePatterns,
            String appCode,
            String env,
            String bizCode,
            Set<String> operations) {
        return new DdcServicePrincipal("ai-reader", "SDK", Set.of(appCode), Set.of(env),
                Set.of(bizCode), operations, appCode, env, bizCode, tenantId,
                "tenant-" + tenantId, namespacePatterns);
    }

    private <T> T withPrincipal(DdcServicePrincipal principal, java.util.concurrent.Callable<T> action) {
        Context context = principal.bind(Context.current());
        Context previous = context.attach();
        try {
            return action.call();
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new AssertionError(failure);
        } finally {
            context.detach(previous);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
            basePackageClasses = DdcAiResourceRpcProvider.class,
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = DdcAiResourceRpcProvider.class))
    static class ProviderConfiguration {
        @Bean("ddcAiResourceService")
        DdcAiResourceService ddcAiResourceService() {
            return mock(DdcAiResourceService.class);
        }

        @Bean("ddcAiProtoConverterImpl")
        DdcAiProtoConverterImpl ddcAiProtoConverter() {
            return new DdcAiProtoConverterImpl();
        }

        @Bean("ddcAiFeedbackService")
        DdcAiFeedbackService ddcAiFeedbackService() {
            return mock(DdcAiFeedbackService.class);
        }

        @Bean("ddcAiSyncCommandConverterImpl")
        DdcAiSyncCommandConverterImpl ddcAiSyncCommandConverter() {
            return new DdcAiSyncCommandConverterImpl();
        }
    }
}
