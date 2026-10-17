package top.egon.cola.component.rpc.tianshu.client.ai;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;
import top.egon.cola.component.rpc.tianshu.contract.DdcAiResourceRpc;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiNode;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiNodeType;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPromptResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiResourceQuery;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.DdcAiResourceServiceGrpc;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiOutcome;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillDraftUpload;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillPublishRequest;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReport;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportBatch;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiUsageReportStatus;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiVersionHead;
import top.egon.cola.component.rpc.tianshu.security.DdcRpcOperation;
import top.egon.cola.component.rpc.tianshu.security.DdcRpcOperationResolver;
import top.egon.cola.component.rpc.tianshu.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.tianshu.api.client.DdcPromptClient;
import top.egon.cola.component.tianshu.api.client.DdcSkillClient;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;
import top.egon.cola.component.tianshu.model.ai.DdcPromptResult;
import top.egon.cola.component.tianshu.model.ai.DdcSkillResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcAiRpcContractTest {

    private static final String HASH = "a".repeat(64);

    @Test
    void typedFacadeExposesReadFeedbackAndSyncMethodsWithIdempotencyMarkers() throws NoSuchMethodException {
        EgonRpcService service = DdcAiResourceRpc.class.getAnnotation(EgonRpcService.class);

        assertThat(service).isNotNull();
        assertThat(service.grpcClass()).isEqualTo(DdcAiResourceServiceGrpc.class);
        assertThat(DdcAiResourceRpc.class.getDeclaredMethods()).hasSize(6);
        assertMethod("getPrompt", "GetPrompt", AiResourceQuery.class, true);
        assertMethod("getSkill", "GetSkill", AiResourceQuery.class, true);
        assertMethod("getVersionHead", "GetVersionHead", AiResourceQuery.class, true);
        assertMethod("reportUsage", "ReportUsage", AiUsageReportBatch.class, false);
        assertMethod("replaceSkillDraft", "ReplaceSkillDraft", AiSkillDraftUpload.class, false);
        assertMethod("publishSkillDraft", "PublishSkillDraft", AiSkillPublishRequest.class, false);
        assertThat(DdcAiResourceServiceGrpc.getServiceDescriptor().getMethods())
                .extracting(method -> method.getBareMethodName())
                .containsExactlyInAnyOrder("GetPrompt", "GetSkill", "ReportUsage", "GetVersionHead",
                        "ReplaceSkillDraft", "PublishSkillDraft");
    }

    @Test
    void newAiOperationsResolveToDedicatedPermissions() {
        DdcRpcOperationResolver resolver = new DdcRpcOperationResolver();
        String prefix = "egon.tianshu.v1.DdcAiResourceService/";

        assertThat(resolver.resolve(prefix + "ReportUsage")).isEqualTo(DdcRpcOperation.AI_REPORT);
        assertThat(resolver.resolve(prefix + "GetVersionHead")).isEqualTo(DdcRpcOperation.AI_READ);
        assertThat(resolver.resolve(prefix + "ReplaceSkillDraft")).isEqualTo(DdcRpcOperation.AI_WRITE);
        assertThat(resolver.resolve(prefix + "PublishSkillDraft")).isEqualTo(DdcRpcOperation.AI_PUBLISH);
        assertThat(DdcRpcOperation.AI_REPORT.getCode()).isEqualTo(20);
        assertThat(DdcRpcOperation.AI_WRITE.getCode()).isEqualTo(21);
        assertThat(DdcRpcOperation.AI_PUBLISH.getCode()).isEqualTo(22);
        assertThat(DdcRpcOperation.AI_PUBLISH.management()).isFalse();
    }

    @Test
    void protoMessagesCarryOptionalFieldsAndPrefixedEnums() {
        AiUsageReport report = AiUsageReport.newBuilder().setUsageId("u").setOccurredAt("2026-10-17T00:00:00Z")
                .setOutcome(AiOutcome.AI_OUTCOME_SUCCESS).build();
        AiVersionHead head = AiVersionHead.newBuilder().setResourceId("1").setResourceKey("k")
                .setResourceRevision("0").setPublished(false).build();

        assertThat(report.hasExplicitScore()).isFalse();
        assertThat(report.hasInputExcerpt()).isFalse();
        assertThat(report.hasTraceId()).isFalse();
        assertThat(head.hasVersionNo()).isFalse();
        assertThat(head.hasContentSha256()).isFalse();
        assertThat(AiUsageReportStatus.AI_USAGE_REPORT_ACCEPTED_DUPLICATE.getNumber()).isEqualTo(2);
        assertThat(AiOutcome.AI_OUTCOME_UNKNOWN.getNumber()).isEqualTo(3);
    }

    @Test
    void queryAndResultsPreserveRootVersionAndSkillTextPresence() {
        DdcAiProtoConverter converter = new DdcAiProtoConverterImpl();
        DdcAiQueryDTO query = new DdcAiQueryDTO(
                "retail", "prod-space", "prod", "assistant-app", "review-order", 4L, 810001L);
        AiResourceQuery protoQuery = converter.toQuery(query);

        assertThat(protoQuery.getBizCode()).isEqualTo("retail");
        assertThat(protoQuery.getNamespaceCode()).isEqualTo("prod-space");
        assertThat(protoQuery.getEnv()).isEqualTo("prod");
        assertThat(protoQuery.getAppCode()).isEqualTo("assistant-app");
        assertThat(protoQuery.getResourceKey()).isEqualTo("review-order");
        assertThat(protoQuery.hasVersionNo()).isTrue();
        assertThat(protoQuery.getVersionNo()).isEqualTo(4L);
        assertThat(protoQuery.hasResourceId()).isTrue();
        assertThat(protoQuery.getResourceId()).isEqualTo("810001");

        AiResourceQuery currentQuery = converter.toQuery(new DdcAiQueryDTO(
                "retail", "prod-space", "prod", "assistant-app", "review-order", null, 810001L));
        assertThat(currentQuery.hasVersionNo()).isFalse();
        assertThat(currentQuery.getResourceId()).isEqualTo("810001");

        DdcPromptResult prompt = converter.fromPromptResult(AiPromptResult.newBuilder()
                .setResourceId("810001").setResourceKey("review-order").setVersionId("820004")
                .setVersionNo(4L).setContentSha256(HASH).setTemplate("approved prompt")
                .build());
        assertThat(prompt.getResourceId()).isEqualTo(810001L);
        assertThat(prompt.getVersionId()).isEqualTo(820004L);
        assertThat(prompt.getVersionNo()).isEqualTo(4L);
        assertThat(prompt.getTemplate()).isEqualTo("approved prompt");

        AiSkillResult skillProto = AiSkillResult.newBuilder()
                .setResourceId("810001").setResourceKey("review-order").setVersionId("820005")
                .setVersionNo(5L).setContentSha256(HASH)
                .addFiles(AiNode.newBuilder().setPath("").setNodeType(AiNodeType.DIRECTORY))
                .addFiles(AiNode.newBuilder().setPath("README.txt").setNodeType(AiNodeType.FILE)
                        .setContent(""))
                .build();
        DdcSkillResult skill = converter.fromSkillResult(skillProto);
        assertThat(skill.getFiles()).extracting(node -> node.getPath())
                .containsExactly("", "README.txt");
        assertThat(skill.getFiles().get(0).getNodeType()).isEqualTo(DdcAiNodeTypeEnum.DIRECTORY);
        assertThat(skill.getFiles().get(0).getContent()).isNull();
        assertThat(skill.getFiles().get(1).getNodeType()).isEqualTo(DdcAiNodeTypeEnum.FILE);
        assertThat(skill.getFiles().get(1).getContent()).isEmpty();

        assertThatThrownBy(() -> converter.fromPromptResult(AiPromptResult.newBuilder()
                .setResourceId("0810001").setResourceKey("review-order").setVersionId("820004")
                .setVersionNo(4L).setContentSha256(HASH).setTemplate("x").build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void publicQueryRequiresAPositiveStableRootId() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var missingRoot = new DdcAiResourceQuery("prod-space", "review-order", null, null);
            var zeroRoot = new DdcAiResourceQuery("prod-space", "review-order", null, 0L);

            assertThat(validator.validate(missingRoot))
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("resourceId"));
            assertThat(validator.validate(zeroRoot))
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("resourceId"));
        }
    }

    @Test
    void promptAndSkillPortsSendTheCallerRootAndReturnFullPublishedContent() {
        DdcAiResourceRpc rpc = mock(DdcAiResourceRpc.class);
        DdcAiProtoConverter converter = new DdcAiProtoConverterImpl();
        DdcRpcStatusExceptionMapper errors = new DdcRpcStatusExceptionMapper();
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail");
        properties.setEnv("prod");
        properties.setAppCode("assistant-app");
        DdcPromptClient promptClient = new RpcDdcPromptClient(rpc, converter, errors, properties);
        DdcSkillClient skillClient = new RpcDdcSkillClient(rpc, converter, errors, properties);
        DdcAiResourceQuery query = new DdcAiResourceQuery("prod-space", "review-order", 4L, 810001L);
        when(rpc.getPrompt(org.mockito.ArgumentMatchers.any())).thenReturn(AiPromptResult.newBuilder()
                .setResourceId("810001").setResourceKey("review-order").setVersionId("820004")
                .setVersionNo(4L).setContentSha256(HASH).setTemplate("approved prompt")
                .build());
        when(rpc.getSkill(org.mockito.ArgumentMatchers.any())).thenReturn(AiSkillResult.newBuilder()
                .setResourceId("810001").setResourceKey("review-order").setVersionId("820004")
                .setVersionNo(4L).setContentSha256(HASH)
                .addFiles(AiNode.newBuilder().setPath("").setNodeType(AiNodeType.DIRECTORY))
                .addFiles(AiNode.newBuilder().setPath("README.txt").setNodeType(AiNodeType.FILE)
                        .setContent("published skill"))
                .build());

        DdcPromptResult prompt = promptClient.get(query);
        DdcSkillResult skill = skillClient.get(query);

        assertThat(prompt.getTemplate()).isEqualTo("approved prompt");
        assertThat(skill.getFiles()).hasSize(2);
        assertThat(skill.getFiles().get(1).getContent()).isEqualTo("published skill");
        verify(rpc).getPrompt(argThat(request -> hasExpectedRoot(request, 4L)));
        verify(rpc).getSkill(argThat(request -> hasExpectedRoot(request, 4L)));
    }

    private void assertMethod(String javaName, String rpcName, Class<?> requestType, boolean idempotent)
            throws NoSuchMethodException {
        EgonRpcMethod method = DdcAiResourceRpc.class.getMethod(javaName, requestType)
                .getAnnotation(EgonRpcMethod.class);
        assertThat(method).isNotNull();
        assertThat(method.name()).isEqualTo(rpcName);
        assertThat(method.idempotent()).isEqualTo(idempotent);
    }

    private boolean hasExpectedRoot(AiResourceQuery request, long versionNo) {
        return request.getBizCode().equals("retail")
                && request.getNamespaceCode().equals("prod-space")
                && request.getEnv().equals("prod")
                && request.getAppCode().equals("assistant-app")
                && request.getResourceKey().equals("review-order")
                && request.hasVersionNo() && request.getVersionNo() == versionNo
                && request.hasResourceId() && request.getResourceId().equals("810001");
    }
}
