package top.egon.cola.component.rpc.tianshu.client.ai;

import io.grpc.Metadata;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.egon.cola.component.rpc.tianshu.contract.DdcAiResourceRpc;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiNodeType;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPublicationReceipt;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiResourceQuery;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiRevisionResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillDraftUpload;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiSkillPublishRequest;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiVersionHead;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.DdcRpcErrorDetail;
import top.egon.cola.component.rpc.tianshu.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.error.DdcException;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiPublicationReceiptDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;
import top.egon.cola.component.tianshu.model.ai.DdcAiRevisionDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiSkillDraftUploadDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiSkillPublishDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiVersionHeadDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RpcDdcAiSyncClientTest {

    private static final String HASH = "b".repeat(64);

    private final DdcAiResourceRpc rpc = mock(DdcAiResourceRpc.class);
    private final RpcDdcAiSyncClient client = new RpcDdcAiSyncClient(
            rpc, new DdcAiProtoConverterImpl(), new DdcRpcStatusExceptionMapper(), properties());

    @Test
    void getVersionHeadSendsScopedQueryWithoutVersionAndMapsUnpublishedHead() {
        when(rpc.getVersionHead(any())).thenReturn(AiVersionHead.newBuilder()
                .setResourceId("810001").setResourceKey("refund-skill").setResourceRevision("7")
                .setPublished(false).build());

        DdcAiVersionHeadDTO head = client.getVersionHead(
                new DdcAiResourceQuery("prod-space", "refund-skill", null, 810001L));

        assertThat(head.getPublished()).isFalse();
        assertThat(head.getVersionNo()).isNull();
        assertThat(head.getContentSha256()).isNull();
        assertThat(head.getResourceRevision()).isEqualTo(7L);
        ArgumentCaptor<AiResourceQuery> sent = ArgumentCaptor.forClass(AiResourceQuery.class);
        verify(rpc).getVersionHead(sent.capture());
        assertThat(sent.getValue().getBizCode()).isEqualTo("retail");
        assertThat(sent.getValue().getEnv()).isEqualTo("prod");
        assertThat(sent.getValue().getAppCode()).isEqualTo("assistant-app");
        assertThat(sent.getValue().hasVersionNo()).isFalse();
        assertThat(sent.getValue().getResourceId()).isEqualTo("810001");
    }

    @Test
    void getVersionHeadMapsPublishedHead() {
        when(rpc.getVersionHead(any())).thenReturn(AiVersionHead.newBuilder()
                .setResourceId("810001").setResourceKey("refund-skill").setVersionId("820004")
                .setVersionNo(4L).setContentSha256(HASH).setResourceRevision("9").setPublished(true).build());

        DdcAiVersionHeadDTO head = client.getVersionHead(
                new DdcAiResourceQuery("prod-space", "refund-skill", null, 810001L));

        assertThat(head.getVersionId()).isEqualTo(820004L);
        assertThat(head.getVersionNo()).isEqualTo(4L);
        assertThat(head.getContentSha256()).isEqualTo(HASH);
    }

    @Test
    void getVersionHeadRejectsVersionNo() {
        assertThatThrownBy(() -> client.getVersionHead(
                new DdcAiResourceQuery("prod-space", "refund-skill", 3L, 810001L)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(rpc);
    }

    @Test
    void replaceSkillDraftSendsWholeTreeAndMapsRevision() {
        when(rpc.replaceSkillDraft(any())).thenReturn(AiRevisionResult.newBuilder()
                .setResourceId("810001").setResourceRevision("12").setDraftSha256(HASH)
                .setDraftMatchesCurrent(false).build());

        DdcAiRevisionDTO revision = client.replaceSkillDraft(upload());

        assertThat(revision.getResourceRevision()).isEqualTo(12L);
        assertThat(revision.getDraftMatchesCurrent()).isFalse();
        ArgumentCaptor<AiSkillDraftUpload> sent = ArgumentCaptor.forClass(AiSkillDraftUpload.class);
        verify(rpc).replaceSkillDraft(sent.capture());
        assertThat(sent.getValue().getExpectedRevision()).isEqualTo("11");
        assertThat(sent.getValue().getResourceId()).isEqualTo("810001");
        assertThat(sent.getValue().getFilesList()).extracting(node -> node.getPath()).containsExactly("", "SKILL.md");
        assertThat(sent.getValue().getFiles(0).getNodeType()).isEqualTo(AiNodeType.DIRECTORY);
        assertThat(sent.getValue().getFiles(0).hasContent()).isFalse();
    }

    @Test
    void replaceSkillDraftRestoresRevisionConflictAsTypedException() {
        Metadata trailers = new Metadata();
        trailers.put(DdcRpcStatusExceptionMapper.ERROR_DETAIL, DdcRpcErrorDetail.newBuilder()
                .setCode("TIANSHU_AI_REVISION_CONFLICT").setMessage("Resource revision changed").build());
        when(rpc.replaceSkillDraft(any())).thenThrow(Status.ABORTED.asRuntimeException(trailers));

        assertThatThrownBy(() -> client.replaceSkillDraft(upload()))
                .isInstanceOfSatisfying(DdcException.class, failure -> {
                    assertThat(failure.getCode()).isEqualTo(56303);
                    assertThat(failure.getStatus()).isEqualTo("TIANSHU_AI_REVISION_CONFLICT");
                });
    }

    @Test
    void publishSkillDraftSendsRequestIdAndMapsReceipt() {
        when(rpc.publishSkillDraft(any())).thenReturn(AiPublicationReceipt.newBuilder()
                .setPublicationId("555").setResourceId("810001").setTargetVersionId("820005")
                .setTargetVersionNo(5L).setResourceRevision("13").setContentSha256(HASH).build());

        DdcAiPublicationReceiptDTO receipt = client.publishSkillDraft(DdcAiSkillPublishDTO.builder()
                .namespaceCode("prod-space").resourceKey("refund-skill").resourceId(810001L)
                .expectedRevision(12L).requestId("auth-0123456789abcdef").changeNote("authoring").build());

        assertThat(receipt.getPublicationId()).isEqualTo(555L);
        assertThat(receipt.getTargetVersionNo()).isEqualTo(5L);
        assertThat(receipt.getResourceRevision()).isEqualTo(13L);
        ArgumentCaptor<AiSkillPublishRequest> sent = ArgumentCaptor.forClass(AiSkillPublishRequest.class);
        verify(rpc).publishSkillDraft(sent.capture());
        assertThat(sent.getValue().getRequestId()).isEqualTo("auth-0123456789abcdef");
        assertThat(sent.getValue().getExpectedRevision()).isEqualTo("12");
    }

    private static DdcAiSkillDraftUploadDTO upload() {
        return DdcAiSkillDraftUploadDTO.builder()
                .namespaceCode("prod-space").resourceKey("refund-skill").resourceId(810001L).expectedRevision(11L)
                .displayName("Refund").description("refund skill")
                .files(List.of(
                        DdcAiNodeDTO.builder().path("").nodeType(DdcAiNodeTypeEnum.DIRECTORY).build(),
                        DdcAiNodeDTO.builder().path("SKILL.md").nodeType(DdcAiNodeTypeEnum.FILE)
                                .content("# refund").build()))
                .build();
    }

    private static DdcProperties properties() {
        DdcProperties properties = new DdcProperties();
        properties.setBizCode("retail");
        properties.setEnv("prod");
        properties.setAppCode("assistant-app");
        return properties;
    }
}
