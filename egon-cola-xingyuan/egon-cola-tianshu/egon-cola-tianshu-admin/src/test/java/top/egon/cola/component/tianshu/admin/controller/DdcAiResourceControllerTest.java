package top.egon.cola.component.tianshu.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import top.egon.cola.component.tianshu.admin.controller.ai.DdcAiResourceController;
import top.egon.cola.component.tianshu.admin.config.DdcAiWebConfiguration;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.tianshu.admin.model.dto.DdcConfigCreateRequest;
import top.egon.cola.component.tianshu.admin.model.command.DdcAiCreateCommand;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiResourceSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.dto.DdcAiVersionSummaryDTO;
import top.egon.cola.component.tianshu.admin.model.bo.DdcAiVersionBO;
import top.egon.cola.component.tianshu.admin.model.convertor.DdcAiVersionConverterImpl;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiIdQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiPublicationPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiVersionPageQuery;
import top.egon.cola.component.tianshu.admin.model.query.DdcAiVersionQuery;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiPublicationResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiResourceResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcAiVersionResult;
import top.egon.cola.component.tianshu.admin.model.vo.DdcPageResult;
import top.egon.cola.component.tianshu.admin.security.management.DdcAdminAuthenticationEntryPoint;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiFeedbackService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiOptimizationService;
import top.egon.cola.component.tianshu.admin.service.ai.DdcAiResourceService;
import top.egon.cola.component.tianshu.model.ai.DdcAiContentDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiKindEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeDTO;
import top.egon.cola.component.tianshu.model.ai.DdcAiNodeTypeEnum;
import top.egon.cola.component.tianshu.model.ai.DdcAiScopeDTO;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DdcAiResourceController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(DdcAiWebConfiguration.class)
class DdcAiResourceControllerTest {

    private static final long RESOURCE_ID = 810001L;
    private static final long VERSION_ID = 820001L;
    private static final long PUBLICATION_ID = 830001L;
    private static final Instant NOW = Instant.parse("2026-09-29T10:00:00Z");
    private static final DdcAiScopeDTO SCOPE =
            new DdcAiScopeDTO("retail", "dev-space", "dev", "assistant-app");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RequestMappingHandlerAdapter handlerAdapter;
    @MockBean(name = "ddcAiResourceService") private DdcAiResourceService service;
    @MockBean(name = "ddcAiOptimizationService") private DdcAiOptimizationService optimizationService;
    @MockBean(name = "ddcAiFeedbackService") private DdcAiFeedbackService feedbackService;

    @Test
    void pageAiResourcesBindsOnlyDeclaredScopeKindAndPagingParameters() throws Exception {
        when(service.page(any(DdcAiPageQuery.class))).thenReturn(page(resourceSummary(), 1, 20));

        mockMvc.perform(get("/api/v1/tianshu/ai/resources")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a")
                        .param("bizCode", "retail").param("namespaceCode", "dev-space")
                        .param("env", "dev").param("appCode", "assistant-app")
                        .param("kind", "SKILL").param("resourceKeyPrefix", "review")
                        .param("pageNo", "1").param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(10000))
                .andExpect(jsonPath("$.records[0].id").value("810001"))
                .andExpect(jsonPath("$.records[0].currentVersionId").value("820001"))
                .andExpect(jsonPath("$.page.pageNo").value(1))
                .andExpect(jsonPath("$.page.pageSize").value(20));

        verify(service).page(any(DdcAiPageQuery.class));
    }

    @Test
    void createGetAndReplaceDraftKeepStringIdsAndOriginalText() throws Exception {
        DdcAiResourceResult result = resourceResult();
        DdcAiResourceResult updatedResult = DdcAiResourceResult.builder().resource(resourceSummary())
                .draft(DdcAiContentDTO.builder().promptText("Updated prompt").nodes(List.of()).build()).build();
        when(service.create(any())).thenReturn(result);
        when(service.get(any())).thenReturn(result);
        when(service.replaceDraft(any(), any())).thenReturn(updatedResult);

        mockMvc.perform(post("/api/v1/tianshu/ai/resources")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Location", "/api/v1/tianshu/ai/resources/810001"))
                .andExpect(jsonPath("$.data.resource.id").value("810001"))
                .andExpect(jsonPath("$.data.draft.promptText").value("Check order fields.\n"));

        mockMvc.perform(get("/api/v1/tianshu/ai/resources/{id}", "810001")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resource.revision").value("3"));

        mockMvc.perform(put("/api/v1/tianshu/ai/resources/{id}/draft", "810001")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON).content(draftBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.draft.promptText").value("Updated prompt"));

        verify(service).create(any());
        verify(service).get(new DdcAiIdQuery(RESOURCE_ID));
        verify(service).replaceDraft(any(), any());
    }

    @Test
    void deleteUsesExpectedRevisionQueryAndReturnsBodyless204() throws Exception {
        mockMvc.perform(delete("/api/v1/tianshu/ai/resources/{id}", "810001")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a")
                        .param("expectedRevision", "3"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string("Cache-Control", "no-store"));

        verify(service).delete(eq(new DdcAiIdQuery(RESOURCE_ID)),
                org.mockito.ArgumentMatchers.argThat(command -> "3".equals(command.getExpectedRevision())));
    }

    @Test
    void versionAndPublicationQueriesUseResourceScopedStringPathsAndPages() throws Exception {
        when(service.pageVersions(any())).thenReturn(page(versionSummary(), 1, 20));
        when(service.getVersion(any())).thenReturn(DdcAiVersionResult.builder()
                .version(versionSummary()).content(skillContent()).build());
        when(service.pagePublications(any())).thenReturn(page(publicationResult("PUBLISH"), 1, 20));

        mockMvc.perform(get("/api/v1/tianshu/ai/resources/{id}/versions", "810001")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a").param("pageNo", "1").param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("820001"))
                .andExpect(jsonPath("$.records[0].versionNo").value("1"));

        mockMvc.perform(get("/api/v1/tianshu/ai/resources/{id}/versions/{versionNo}", "810001", "1")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version.resourceId").value("810001"))
                .andExpect(jsonPath("$.data.version.createdBy").value("user:operator-1"))
                .andExpect(jsonPath("$.data.content.nodes[1].content").value("Check order fields.\n"));

        mockMvc.perform(get("/api/v1/tianshu/ai/resources/{id}/publications", "810001")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a")
                        .param("pageNo", "1").param("pageSize", "20").param("requestId", "request_1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].operation").value("PUBLISH"));

        verify(service).pageVersions(any(DdcAiVersionPageQuery.class));
        verify(service).getVersion(new DdcAiVersionQuery(RESOURCE_ID, 1L));
        verify(service).pagePublications(any(DdcAiPublicationPageQuery.class));
    }

    @Test
    void publishRollbackAndPromotionCarryIdempotencyAndStableLocation() throws Exception {
        when(service.publish(any(), any(), any())).thenReturn(publicationResult("PUBLISH"));
        when(service.rollback(any(), any(), any())).thenReturn(publicationResult("ROLLBACK"));
        when(service.promote(any(), any(), any())).thenReturn(publicationResult("PROMOTE"));

        mockMvc.perform(post("/api/v1/tianshu/ai/resources/{id}/publications", "810001")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a").header("Idempotency-Key", "publish_1234567890")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"expectedRevision":"3","changeNote":"publish"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tianshu/ai/resources/810001/versions/1"))
                .andExpect(jsonPath("$.data.id").value("830001"));

        mockMvc.perform(post("/api/v1/tianshu/ai/resources/{id}/rollbacks", "810001")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a").header("Idempotency-Key", "rollback_123456789")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"expectedRevision":"4","versionNo":"1","changeNote":"rollback"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operation").value("ROLLBACK"));

        mockMvc.perform(post("/api/v1/tianshu/ai/resources/{id}/promotions", "810001")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a").header("Idempotency-Key", "promote_1234567890")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"sourceVersionNo":"2","targetScope":{"bizCode":"retail",
                                "namespaceCode":"prod-space","env":"prod","appCode":"assistant-app"},
                                "targetResourceKey":"review-order","expectedTargetRevision":null,
                                "changeNote":"promote"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tianshu/ai/resources/810002/versions/1"))
                .andExpect(jsonPath("$.data.operation").value("PROMOTE"));

        verify(service).publish(eq(new DdcAiIdQuery(RESOURCE_ID)), eq("publish_1234567890"), any());
        verify(service).rollback(eq(new DdcAiIdQuery(RESOURCE_ID)), eq("rollback_123456789"), any());
        verify(service).promote(eq(new DdcAiIdQuery(RESOURCE_ID)), any(), eq("promote_1234567890"));
    }

    @Test
    void businessFailuresKeepSevenFieldEnvelopeAndUnknownCommandFieldsAreRejected() throws Exception {
        MappingJackson2HttpMessageConverter strictConverter = handlerAdapter.getMessageConverters().stream()
                .filter(converter -> converter.getClass().getSimpleName().equals("DdcAiStrictJsonMessageConverter"))
                .map(MappingJackson2HttpMessageConverter.class::cast)
                .findFirst().orElseThrow();
        assertThat(strictConverter.canRead(DdcAiCreateCommand.class, MediaType.APPLICATION_JSON)).isTrue();
        assertThat(strictConverter.canRead(DdcConfigCreateRequest.class, MediaType.APPLICATION_JSON)).isFalse();

        when(service.get(any())).thenThrow(new CommonException(
                56303, "TIANSHU_AI_REVISION_CONFLICT", "Revision conflict"));
        when(service.create(any())).thenReturn(resourceResult());

        mockMvc.perform(get("/api/v1/tianshu/ai/resources/{id}", "810001")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a"))
                .andExpect(status().isConflict())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.code").value(56303))
                .andExpect(jsonPath("$.status").value("TIANSHU_AI_REVISION_CONFLICT"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.success").isBoolean())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.traceId").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(post("/api/v1/tianshu/ai/resources")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody().replace("\"kind\":\"PROMPT\"", "\"kind\":\"PROMPT\",\"tenantId\":101")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(56301))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(post("/api/v1/tianshu/ai/resources")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody().replace("\"displayName\":\"Review order\"",
                                "\"displayName\":\"Review order\",\"displayName\":\"Changed\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(56301));

        mockMvc.perform(post("/api/v1/tianshu/ai/resources")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody().replace("\"resourceKey\":\"review-order\"",
                                "\"resourceKey\":123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(56301));

        mockMvc.perform(post("/api/v1/tianshu/ai/resources")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scope":{"bizCode":"retail","namespaceCode":"dev-space","env":"dev","appCode":"assistant-app"},
                                "kind":"PROMPT","resourceKey":"review-order","displayName":"Review order",
                                "description":"Prompt","draft":{"nodes":[]}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(56301));

        mockMvc.perform(post("/api/v1/tianshu/ai/resources")
                        .header("X-Egon-Tianshu-Tenant", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(" ".repeat(DdcAiWebConfiguration.MAX_REQUEST_BODY_BYTES + 1)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value(56312));

        verify(service, never()).create(any());
    }

    @Test
    void tenantIdentityFailureUsesTheTwoFieldSecurityEnvelope() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new DdcAdminAuthenticationEntryPoint(objectMapper).handleTenantMismatch(
                new MockHttpServletRequest(), response);

        var body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(body.size()).isEqualTo(2);
        assertThat(body.has("code")).isTrue();
        assertThat(body.has("message")).isTrue();
        assertThat(body.get("code").asText()).isEqualTo("TIANSHU_ADMIN_TENANT_MISMATCH");
        assertThat(body.get("message").asText()).isEqualTo("TIANSHU_ADMIN_TENANT_MISMATCH");
    }

    @Test
    void versionHistoryProjectionCarriesTrustedCreatorFromAuditMetadata() {
        DdcAiVersionBO version = DdcAiVersionBO.builder().id(VERSION_ID).resourceId(RESOURCE_ID)
                .versionNo(1L).kind(DdcAiKindEnum.SKILL).displayName("Review order")
                .description("Skill").contentSha256("b".repeat(64)).contentBytes(21L).nodeCount(2)
                .changeNote("Initial").createTime(NOW).createUserId("user:operator-1").build();

        DdcAiVersionSummaryDTO summary = new DdcAiVersionConverterImpl().toVersionSummary(version);

        assertThat(summary.getCreatedBy()).isEqualTo("user:operator-1");
    }

    private DdcAiResourceSummaryDTO resourceSummary() {
        return DdcAiResourceSummaryDTO.builder().id("810001").scope(SCOPE).kind(DdcAiKindEnum.SKILL)
                .resourceKey("review-order").displayName("Review order").description("Skill")
                .revision("3").lastVersionNo("1").currentVersionId("820001").currentVersionNo("1")
                .updatedAt(NOW).build();
    }

    private DdcAiResourceResult resourceResult() {
        return DdcAiResourceResult.builder().resource(resourceSummary())
                .draft(DdcAiContentDTO.builder().promptText("Check order fields.\n")
                        .nodes(List.of()).build()).build();
    }

    private DdcAiVersionSummaryDTO versionSummary() {
        return DdcAiVersionSummaryDTO.builder().id(VERSION_ID).resourceId(RESOURCE_ID).versionNo(1L)
                .kind("SKILL").displayName("Review order").description("Skill")
                .contentSha256("b".repeat(64)).contentBytes(21L).nodeCount(2).sourceVersionId(null)
                .changeNote("Initial").createdAt(NOW).createdBy("user:operator-1").build();
    }

    private DdcAiPublicationResult publicationResult(String operation) {
        long targetResourceId = operation.equals("PROMOTE") ? 810002L : RESOURCE_ID;
        return DdcAiPublicationResult.builder().publicationId(PUBLICATION_ID).resourceId(targetResourceId)
                .operation(operation).previousVersionId(null).targetVersionId(VERSION_ID).targetVersionNo(1L)
                .sourceVersionId(operation.equals("PROMOTE") ? 820002L : null).resourceRevision(4L)
                .targetCreated(operation.equals("PROMOTE")).createdAt(NOW).build();
    }

    private DdcAiContentDTO skillContent() {
        return DdcAiContentDTO.builder().promptText(null).nodes(List.of(
                DdcAiNodeDTO.builder().path("").nodeType(DdcAiNodeTypeEnum.DIRECTORY).content(null).build(),
                DdcAiNodeDTO.builder().path("README.txt").nodeType(DdcAiNodeTypeEnum.FILE)
                        .content("Check order fields.\n").build())).build();
    }

    private DdcPageResult<DdcAiResourceSummaryDTO> page(DdcAiResourceSummaryDTO record, int pageNo, int pageSize) {
        return DdcPageResult.<DdcAiResourceSummaryDTO>builder().record(record).total(1)
                .pageNo(pageNo).pageSize(pageSize).build();
    }

    private DdcPageResult<DdcAiVersionSummaryDTO> page(DdcAiVersionSummaryDTO record, int pageNo, int pageSize) {
        return DdcPageResult.<DdcAiVersionSummaryDTO>builder().record(record).total(1)
                .pageNo(pageNo).pageSize(pageSize).build();
    }

    private DdcPageResult<DdcAiPublicationResult> page(DdcAiPublicationResult record, int pageNo, int pageSize) {
        return DdcPageResult.<DdcAiPublicationResult>builder().record(record).total(1)
                .pageNo(pageNo).pageSize(pageSize).build();
    }

    private String createBody() {
        return """
                {"scope":{"bizCode":"retail","namespaceCode":"dev-space","env":"dev","appCode":"assistant-app"},
                "kind":"PROMPT","resourceKey":"review-order","displayName":"Review order",
                "description":"Prompt","draft":{"promptText":"Check order fields.\\n","nodes":[]}}
                """;
    }

    private String draftBody() {
        return """
                {"expectedRevision":"3","displayName":"Review order","description":"Prompt",
                "draft":{"promptText":"Updated prompt","nodes":[]}}
                """;
    }
}
