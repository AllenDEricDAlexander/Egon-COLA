package top.egon.cola.component.tianshu.admin.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.rpc.tianshu.client.ai.DdcAiProtoConverterImpl;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiPromptResult;
import top.egon.cola.component.rpc.tianshu.contract.proto.v1.AiResourceQuery;
import top.egon.cola.component.tianshu.admin.config.DdcAiWebConfiguration;
import top.egon.cola.component.tianshu.admin.controller.ai.DdcAiResourceController;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiCreateCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiDraftCommand;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiPromotionCommand;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiResourceSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiVersionSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiIdQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiPublicationResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiResourceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiVersionResult;
import top.egon.cola.component.tianshu.admin.rpc.provider.DdcAiResourceRpcProvider;
import top.egon.cola.component.tianshu.admin.security.rpc.DdcServicePrincipal;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiOptimizationService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiResourceService;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiResourceQuery;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DdcAiResourceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DdcAiWebConfiguration.class)
class DdcEndToEndContractTest {

    private static final long TENANT_ID = 101L;
    private static final long SOURCE_RESOURCE_ID = 810001L;
    private static final long TARGET_RESOURCE_ID = 810002L;
    private static final long SOURCE_VERSION_ID = 820001L;
    private static final long TARGET_VERSION_ID = 820002L;
    private static final String CONTENT_HASH = "a".repeat(64);
    private static final Instant CREATED_AT = Instant.parse("2026-10-01T12:00:00Z");
    private static final DdcAiScopeDTO SOURCE_SCOPE = new DdcAiScopeDTO(
            "retail", "draft-space", "dev", "assistant-app");
    private static final DdcAiScopeDTO TARGET_SCOPE = new DdcAiScopeDTO(
            "retail", "production-space", "prod", "assistant-app");
    private static final String RESOURCE_KEY = "review-order";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "ddcAiResourceService")
    private DdcAiResourceService service;

    @MockBean(name = "ddcAiOptimizationService")
    private DdcAiOptimizationService optimizationService;

    @MockBean(name = "ddcAiFeedbackService")
    private DdcAiFeedbackService feedbackService;

    @Test
    void restDraftPublishPromotionAndDirectRpcReadKeepOneFixedSnapshotAndSeparateReceipts() throws Exception {
        DdcAiResourceResult created = resourceResult(SOURCE_RESOURCE_ID, SOURCE_SCOPE, RESOURCE_KEY,
                "draft prompt", null, null);
        DdcAiResourceResult saved = resourceResult(SOURCE_RESOURCE_ID, SOURCE_SCOPE, RESOURCE_KEY,
                "approved prompt", null, null);
        DdcAiPublicationResult publishReceipt = publication(SOURCE_RESOURCE_ID, "PUBLISH",
                SOURCE_VERSION_ID, 4L, null, false);
        DdcAiPublicationResult promotionReceipt = publication(TARGET_RESOURCE_ID, "PROMOTE",
                TARGET_VERSION_ID, 1L, SOURCE_VERSION_ID, true);
        DdcAiVersionResult targetSnapshot = versionResult(TARGET_RESOURCE_ID, TARGET_VERSION_ID, 1L,
                "approved prompt");

        when(service.create(any())).thenReturn(created);
        when(service.replaceDraft(any(), any())).thenReturn(saved);
        when(service.publish(any(), eq("publish_order_0001"), any())).thenReturn(publishReceipt);
        when(service.promote(any(), any(), eq("promote_order_0001"))).thenReturn(promotionReceipt);
        when(service.getPublishedVersion(any(), eq(DdcAiKindEnum.PROMPT))).thenAnswer(invocation -> {
            DdcAiResourceQuery query = invocation.getArgument(0);
            long callerTenant = DdcServicePrincipal.current().tenantId();
            if (callerTenant != TENANT_ID) {
                throw new CommonException(56307, "TIANSHU_AI_SCOPE_FORBIDDEN",
                        "AI permission is required for the requested namespace");
            }
            if (query.resourceId() == SOURCE_RESOURCE_ID) {
                throw new CommonException(56302, "TIANSHU_AI_NOT_FOUND",
                        "Resource has no visible published version");
            }
            return targetSnapshot;
        });

        mockMvc.perform(post("/api/v1/tianshu/ai/resources")
                        .header("X-Egon-Tianshu-Tenant", "tenant-101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(SOURCE_SCOPE, "draft prompt")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tianshu/ai/resources/810001"))
                .andExpect(jsonPath("$.data.resource.id").value("810001"));

        mockMvc.perform(put("/api/v1/tianshu/ai/resources/{id}/draft", SOURCE_RESOURCE_ID)
                        .header("X-Egon-Tianshu-Tenant", "tenant-101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftBody("approved prompt")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.draft.promptText").value("approved prompt"));

        String publishJson = mockMvc.perform(post("/api/v1/tianshu/ai/resources/{id}/publications",
                        SOURCE_RESOURCE_ID)
                        .header("X-Egon-Tianshu-Tenant", "tenant-101")
                        .header("Idempotency-Key", "publish_order_0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedRevision\":\"3\",\"changeNote\":\"approved\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/tianshu/ai/resources/810001/versions/4"))
                .andExpect(jsonPath("$.data.operation").value("PUBLISH"))
                .andExpect(jsonPath("$.data.targetVersionNo").value("4"))
                .andReturn().getResponse().getContentAsString();
        assertThat(publishJson).doesNotContain("ackCount", "ackStatus", "targetCount");

        mockMvc.perform(post("/api/v1/tianshu/ai/resources/{id}/promotions", SOURCE_RESOURCE_ID)
                        .header("X-Egon-Tianshu-Tenant", "tenant-101")
                        .header("Idempotency-Key", "promote_order_0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(promotionBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/tianshu/ai/resources/810002/versions/1"))
                .andExpect(jsonPath("$.data.operation").value("PROMOTE"))
                .andExpect(jsonPath("$.data.sourceVersionId").value("820001"))
                .andExpect(jsonPath("$.data.targetVersionNo").value("1"));

        verify(service).create(argThat(command -> command.getResourceKey().equals(RESOURCE_KEY)
                && command.getScope().equals(SOURCE_SCOPE)));
        verify(service).replaceDraft(eq(new DdcAiIdQuery(SOURCE_RESOURCE_ID)),
                argThat(command -> command.getDraft().getPromptText().equals("approved prompt")));
        verify(service).publish(eq(new DdcAiIdQuery(SOURCE_RESOURCE_ID)), eq("publish_order_0001"),
                argThat(command -> command.getExpectedRevision().equals("3")));
        verify(service).promote(eq(new DdcAiIdQuery(SOURCE_RESOURCE_ID)), argThat(command ->
                        command.getSourceVersionNo().equals("4")
                        && command.getTargetScope().equals(TARGET_SCOPE)
                        && command.getExpectedTargetRevision() == null), eq("promote_order_0001"));

        DdcAiResourceRpcProvider provider = new DdcAiResourceRpcProvider(service,
                new DdcAiProtoConverterImpl(),
                org.mockito.Mockito.mock(top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService.class),
                new top.egon.cola.component.tianshu.admin.model.convertor.DdcAiSyncCommandConverterImpl());
        AiPromptResult prompt = withPrincipal(principal(TENANT_ID), () -> provider.getPrompt(targetQuery()));
        assertThat(prompt.getResourceId()).isEqualTo(Long.toString(TARGET_RESOURCE_ID));
        assertThat(prompt.getVersionId()).isEqualTo(Long.toString(TARGET_VERSION_ID));
        assertThat(prompt.getVersionNo()).isEqualTo(1L);
        assertThat(prompt.getTemplate()).isEqualTo("approved prompt");
        verify(service).getPublishedVersion(argThat(query -> query.resourceId() == TARGET_RESOURCE_ID
                && query.versionNo().equals(1L) && query.resourceKey().equals(RESOURCE_KEY)),
                eq(DdcAiKindEnum.PROMPT));

        mockMvc.perform(delete("/api/v1/tianshu/ai/resources/{id}", SOURCE_RESOURCE_ID)
                        .header("X-Egon-Tianshu-Tenant", "tenant-101")
                        .param("expectedRevision", "4"))
                .andExpect(status().isNoContent());

        assertThatThrownBy(() -> withPrincipal(principal(TENANT_ID), () -> provider.getPrompt(
                query(SOURCE_RESOURCE_ID, SOURCE_SCOPE, 4L))))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(failure -> assertThat(((StatusRuntimeException) failure).getStatus().getCode())
                        .isEqualTo(Status.Code.NOT_FOUND));
        assertThatThrownBy(() -> withPrincipal(principal(TENANT_ID + 1), () -> provider.getPrompt(targetQuery())))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(failure -> assertThat(((StatusRuntimeException) failure).getStatus().getCode())
                .isEqualTo(Status.Code.PERMISSION_DENIED));

        verify(service).delete(eq(new DdcAiIdQuery(SOURCE_RESOURCE_ID)), any());
    }

    private static DdcAiResourceResult resourceResult(long resourceId, DdcAiScopeDTO scope, String key,
                                                       String promptText, String currentVersionId,
                                                       String currentVersionNo) {
        DdcAiResourceSummaryDTO resource = DdcAiResourceSummaryDTO.builder()
                .id(Long.toString(resourceId)).scope(scope).kind(DdcAiKindEnum.PROMPT)
                .resourceKey(key).displayName("Review order").description("Prompt")
                .revision("3").lastVersionNo("4").currentVersionId(currentVersionId)
                .currentVersionNo(currentVersionNo).updatedAt(CREATED_AT).build();
        return DdcAiResourceResult.builder().resource(resource)
                .draft(DdcAiContentDTO.builder().promptText(promptText).nodes(List.of()).build()).build();
    }

    private static DdcAiPublicationResult publication(long resourceId, String operation, long versionId,
                                                      long versionNo, Long sourceVersionId,
                                                      boolean targetCreated) {
        return DdcAiPublicationResult.builder().publicationId(830001L).resourceId(resourceId)
                .operation(operation).previousVersionId(null).targetVersionId(versionId).targetVersionNo(versionNo)
                .sourceVersionId(sourceVersionId).resourceRevision(4L).targetCreated(targetCreated)
                .createdAt(CREATED_AT).build();
    }

    private static DdcAiVersionResult versionResult(long resourceId, long versionId, long versionNo,
                                                    String promptText) {
        DdcAiVersionSummaryDTO version = DdcAiVersionSummaryDTO.builder()
                .id(versionId).resourceId(resourceId).versionNo(versionNo).kind(DdcAiKindEnum.PROMPT.name())
                .displayName("Review order").description("Prompt").contentSha256(CONTENT_HASH)
                .contentBytes((long) promptText.length()).nodeCount(0).changeNote("promoted snapshot")
                .createdAt(CREATED_AT).createdBy("user:operator").build();
        return DdcAiVersionResult.builder().version(version)
                .content(DdcAiContentDTO.builder().promptText(promptText).nodes(List.of()).build()).build();
    }

    private String createBody(DdcAiScopeDTO scope, String promptText) throws Exception {
        return objectMapper.writeValueAsString(DdcAiCreateCommand.builder().scope(scope)
                .kind(DdcAiKindEnum.PROMPT).resourceKey(RESOURCE_KEY)
                .displayName("Review order").description("Prompt")
                .draft(DdcAiContentDTO.builder().promptText(promptText).nodes(List.of()).build()).build());
    }

    private String draftBody(String promptText) throws Exception {
        return objectMapper.writeValueAsString(DdcAiDraftCommand.builder().expectedRevision("3")
                .displayName("Review order")
                .description("Prompt").draft(DdcAiContentDTO.builder().promptText(promptText).nodes(List.of()).build())
                .build());
    }

    private String promotionBody() throws Exception {
        return objectMapper.writeValueAsString(DdcAiPromotionCommand.builder().sourceVersionNo("4")
                .targetScope(TARGET_SCOPE)
                .targetResourceKey(RESOURCE_KEY).expectedTargetRevision(null).changeNote("promote")
                .build());
    }

    private static AiResourceQuery targetQuery() {
        return query(TARGET_RESOURCE_ID, TARGET_SCOPE, 1L);
    }

    private static AiResourceQuery query(long resourceId, DdcAiScopeDTO scope, long versionNo) {
        return AiResourceQuery.newBuilder().setBizCode(scope.bizCode()).setNamespaceCode(scope.namespaceCode())
                .setEnv(scope.env()).setAppCode(scope.appCode()).setResourceKey(RESOURCE_KEY)
                .setResourceId(Long.toString(resourceId)).setVersionNo(versionNo).build();
    }

    private static DdcServicePrincipal principal(long tenantId) {
        return new DdcServicePrincipal("runtime-reader", "SDK", Set.of("assistant-app"), Set.of("prod"),
                Set.of("retail"), Set.of("AI_READ"), "assistant-app", "prod", "retail", tenantId,
                "tenant-" + tenantId, Set.of("production-space"));
    }

    private static <T> T withPrincipal(DdcServicePrincipal principal, Callable<T> action) {
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
}
