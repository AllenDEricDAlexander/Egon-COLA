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
    void typedFacadeExposesOnlyTheTwoIdempotentAiReadMethods() throws NoSuchMethodException {
        EgonRpcService service = DdcAiResourceRpc.class.getAnnotation(EgonRpcService.class);

        assertThat(service).isNotNull();
        assertThat(service.grpcClass()).isEqualTo(DdcAiResourceServiceGrpc.class);
        assertThat(DdcAiResourceRpc.class.getDeclaredMethods()).hasSize(2);
        assertMethod("getPrompt", "GetPrompt");
        assertMethod("getSkill", "GetSkill");
        assertThat(DdcAiResourceServiceGrpc.getServiceDescriptor().getMethods())
                .extracting(method -> method.getBareMethodName())
                .containsExactlyInAnyOrder("GetPrompt", "GetSkill");
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

    private void assertMethod(String javaName, String rpcName) throws NoSuchMethodException {
        EgonRpcMethod method = DdcAiResourceRpc.class.getMethod(javaName,
                AiResourceQuery.class).getAnnotation(EgonRpcMethod.class);
        assertThat(method).isNotNull();
        assertThat(method.name()).isEqualTo(rpcName);
        assertThat(method.idempotent()).isTrue();
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
