package top.egon.cola.component.yuheng.admin.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.hamcrest.Matchers;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.yuheng.admin.knowledge.controller.KnowledgeController;
import top.egon.cola.component.yuheng.admin.knowledge.controller.KnowledgeDocumentController;
import top.egon.cola.component.yuheng.admin.knowledge.controller.KnowledgeJobController;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeEgressPolicyEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobStatusEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeJobTypeEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.enums.KnowledgeMemberRoleEnum;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeBaseVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeDocumentVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgeJobVO;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgePageVO;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeJobService;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeService;
import top.egon.cola.component.yuheng.admin.llm.controller.LlmConfigurationController;
import top.egon.cola.component.yuheng.admin.llm.domain.vo.LlmChannelVO;
import top.egon.cola.component.yuheng.admin.llm.domain.vo.LlmModelVO;
import top.egon.cola.component.yuheng.admin.llm.service.LlmConfigurationService;
import top.egon.cola.component.yuheng.admin.shared.controller.GatewayAdminActorArgumentResolver;
import top.egon.cola.component.yuheng.admin.shared.controller.GatewayAdminExceptionHandler;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;
import top.egon.cola.component.yuheng.admin.wiki.controller.WikiController;
import top.egon.cola.component.yuheng.admin.wiki.domain.vo.WikiPageVO;
import top.egon.cola.component.yuheng.admin.wiki.service.WikiService;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：{@code KnowledgeOpenApiContractTest} 固定 AI 面对外 HTTP 合同：管理面 26 个知识与 Wiki 操作加上 LLM 网关
 * 5 个原生入口正好是批准过的 31 个操作，逐个锁定方法、路径、operationId、能力要求、成功状态、分页与空值形态，
 * 并证明线上载体不泄漏任何持久化内部字段。
 * English summary: {@code KnowledgeOpenApiContractTest} pins the published AI-plane HTTP contract: the 26 management
 * knowledge/Wiki operations plus the 5 native LLM-gateway entry points are exactly the 31 approved operations, each fixed
 * on method, path, operationId, required capability, success status, paging and null shape, while no wire carrier may
 * project a persistence internal field.
 *
 * 用法 / Usage: 注解合同由反射读取，成功状态与响应体由 standalone MockMvc 真实调用被命名的公开入口取得（业务合同以
 * mock 注入，不启动 Spring 上下文、数据库或网络）；/ The annotation contract is read reflectively, while the real success
 * statuses and bodies come from invoking the named public entries through standalone MockMvc with the business contracts
 * mocked, so no application context, database or network is started.
 */
class KnowledgeOpenApiContractTest {

    /** 与 Spring Boot 同构的 JSON 载体（含 java.time 模块），避免用裸 ObjectMapper 得出与线上不同的形状。
     * {@code JSON} mirrors the Boot-configured mapper, including the java.time module, so the assertions never see a
     * shape the deployed service could not produce. */
    private static final ObjectMapper JSON = Jackson2ObjectMapperBuilder.json().build();

    private static final String ADMIN_BASE = "/api/v1/yuheng/admin";

    private static final Path SECURITY_CONFIGURATION = Path.of(
            "src/main/java/top/egon/cola/component/yuheng/admin/config/GatewayAdminSecurityConfiguration.java");

    private static final List<Class<?>> KNOWLEDGE_PLANE_CONTROLLERS = List.of(
            KnowledgeController.class,
            KnowledgeDocumentController.class,
            KnowledgeJobController.class,
            WikiController.class,
            LlmConfigurationController.class);

    private static final List<Class<?>> WIRE_CARRIERS = List.of(
            KnowledgeBaseVO.class, KnowledgeDocumentVO.class, KnowledgeJobVO.class,
            KnowledgePageVO.class, WikiPageVO.class, LlmChannelVO.class, LlmModelVO.class);

    /** LLM 网关原生入口的合同来源 / where the native LLM entry-point contract is read from. */
    private static final Path LLM_ENTRY_CONTROLLER = Path.of("..").resolve("yuheng-llm-gateway")
            .resolve("src/main/java/top/egon/cola/component/yuheng/llm/proxy/controller/LlmApiController.java");

    private final KnowledgeService knowledgeService = mock(KnowledgeService.class);
    private final KnowledgeJobService knowledgeJobService = mock(KnowledgeJobService.class);
    private final WikiService wikiService = mock(WikiService.class);
    private final LlmConfigurationService llmConfigurationService = mock(LlmConfigurationService.class);

    @BeforeEach
    void installVerifiedManagementActor() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                "svc:yuheng-admin-operator",
                "irrelevant",
                "CAP_yuheng:knowledge:write",
                "CAP_yuheng:llm:write",
                "CAP_*",
                "ROLE_ADMIN"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("管理知识面正好暴露 26 个批准操作，方法/路径/operationId 逐项相同")
    void theAdminKnowledgePlaneExposesExactlyTheTwentySixApprovedOperations() {
        assertThat(operations()).containsExactlyInAnyOrderElementsOf(List.of(
                "GET " + ADMIN_BASE + "/knowledge-bases -> listKnowledgeBases",
                "POST " + ADMIN_BASE + "/knowledge-bases -> createKnowledgeBase",
                "GET " + ADMIN_BASE + "/knowledge-bases/{kbId} -> getKnowledgeBase",
                "PUT " + ADMIN_BASE + "/knowledge-bases/{kbId} -> replaceKnowledgeBase",
                "GET " + ADMIN_BASE + "/knowledge-bases/{kbId}/members -> listKnowledgeMembers",
                "PUT " + ADMIN_BASE + "/knowledge-bases/{kbId}/members -> replaceKnowledgeMembers",
                "POST " + ADMIN_BASE + "/knowledge-bases/{kbId}/answers -> createKnowledgeAnswer",
                "GET " + ADMIN_BASE + "/knowledge-bases/{kbId}/documents -> listKnowledgeDocuments",
                "POST " + ADMIN_BASE + "/knowledge-bases/{kbId}/documents -> uploadKnowledgeDocument",
                "GET " + ADMIN_BASE + "/knowledge-bases/{kbId}/documents/{documentId}"
                        + "/revisions/{revisionId} -> getKnowledgeDocumentRevision",
                "DELETE " + ADMIN_BASE + "/knowledge-bases/{kbId}/documents/{documentId}"
                        + " -> deleteKnowledgeDocument",
                "POST " + ADMIN_BASE + "/knowledge-bases/{kbId}/documents/{documentId}"
                        + "/reindex-jobs -> createKnowledgeReindexJob",
                "GET " + ADMIN_BASE + "/knowledge-jobs/{jobId} -> getKnowledgeJob",
                "GET " + ADMIN_BASE + "/knowledge-bases/{kbId}/jobs -> listKnowledgeJobs",
                "POST " + ADMIN_BASE + "/knowledge-jobs/{jobId}/retries -> retryKnowledgeJob",
                "GET " + ADMIN_BASE + "/knowledge-bases/{kbId}/wiki/pages -> listWikiPages",
                "POST " + ADMIN_BASE + "/knowledge-bases/{kbId}/wiki/generation-jobs"
                        + " -> createWikiGenerationJob",
                "GET " + ADMIN_BASE + "/knowledge-bases/{kbId}/wiki/pages/{pageId} -> getWikiPage",
                "PUT " + ADMIN_BASE + "/knowledge-bases/{kbId}/wiki/pages/{pageId}/draft"
                        + " -> replaceWikiDraft",
                "POST " + ADMIN_BASE + "/knowledge-bases/{kbId}/wiki/pages/{pageId}/publications"
                        + " -> publishWikiRevision",
                "GET " + ADMIN_BASE + "/knowledge-bases/{kbId}/wiki/graph -> getWikiGraph",
                "DELETE " + ADMIN_BASE + "/knowledge-bases/{kbId}/wiki/pages/{pageId}"
                        + "/publications/current -> unpublishWikiPage",
                "GET " + ADMIN_BASE + "/llm/channels -> listLlmChannels",
                "PUT " + ADMIN_BASE + "/llm/channels/{channelKey} -> replaceLlmChannel",
                "GET " + ADMIN_BASE + "/llm/models -> listLlmModelConfigurations",
                "PUT " + ADMIN_BASE + "/llm/models/{modelKey} -> replaceLlmModel"));
    }

    @Test
    @DisplayName("AI 面合同总量是 31 个操作：管理 26 个加 LLM 网关原生 5 个，互不重叠")
    void theAiPlaneSurfaceIsTheThirtyOneApprovedOperationsWithoutOverlap() throws IOException {
        Set<String> admin = new TreeSet<>(operations());
        Set<String> nativeEntries = nativeGatewayEntries();

        assertThat(nativeEntries).containsExactlyInAnyOrder(
                "POST /v1/chat/completions -> createLlmChatCompletion",
                "POST /v1/embeddings -> createLlmEmbeddings",
                "POST /v1/responses -> createLlmResponse",
                "POST /v1/messages -> createLlmMessage",
                "GET /v1/models -> listLlmModels");
        assertThat(admin).hasSize(26);
        assertThat(Stream.concat(admin.stream(), nativeEntries.stream()).distinct()).hasSize(31);
        assertThat(nativeEntries)
                .as("the native protocol faces never move under the management path")
                .allSatisfy(entry -> assertThat(entry).doesNotContain(ADMIN_BASE));
    }

    @Test
    @DisplayName("每个知识面操作都带目录、分组、operationId 与对外暴露策略")
    void everyKnowledgeOperationIsCataloguedGroupedAndPolicyAnnotated() {
        for (Class<?> controller : KNOWLEDGE_PLANE_CONTROLLERS) {
            assertThat(controller.getAnnotation(EgonApiCatalog.class))
                    .as("catalog for %s", controller.getSimpleName())
                    .isNotNull()
                    .satisfies(catalog -> assertThat(catalog.interfaceGroupCode()).isEqualTo("yuheng-admin"));
            assertThat(controller.getAnnotation(Tag.class))
                    .as("tag for %s", controller.getSimpleName())
                    .isNotNull()
                    .satisfies(tag -> assertThat(tag.name()).isEqualTo("yuheng-admin"));
            assertThat(controller.getAnnotation(RestController.class))
                    .as("rest controller for %s", controller.getSimpleName())
                    .isNotNull();
        }
        for (Method handler : handlerMethods()) {
            assertThat(handler.getAnnotation(Operation.class).operationId())
                    .as("operationId for %s", handler.getName())
                    .isNotBlank()
                    .doesNotStartWith("admin.");
            assertThat(handler.getAnnotation(EgonGatewayPolicy.class))
                    .as("gateway policy for %s", handler.getName())
                    .isNotNull()
                    .satisfies(policy -> assertThat(policy.exposure())
                            .isEqualTo(EgonGatewayPolicy.Exposure.EXTERNAL));
        }
    }

    @Test
    @DisplayName("幂等策略绑定在四个意图写操作上，Wiki 生成作业的缺口作为已登记偏差固定下来")
    void idempotencyCoversTheFourIntentBoundWritesAndTheWikiGapStaysRegistered() {
        Set<String> declared = new TreeSet<>();
        for (Method handler : handlerMethods()) {
            EgonGatewayPolicy policy = handler.getAnnotation(EgonGatewayPolicy.class);
            if (policy.idempotency() == EgonGatewayPolicy.Idempotency.TRUE) {
                declared.add(policyOf(handler));
            }
        }

        assertThat(declared).containsExactlyInAnyOrder(
                "createKnowledgeBase",
                "uploadKnowledgeDocument",
                "createKnowledgeReindexJob",
                "retryKnowledgeJob");
        assertThat(declaredIdempotencyKeyBindings())
                .as("附录 A9 registers createWikiGenerationJob accepting Idempotency-Key without the policy attribute")
                .contains("createWikiGenerationJob")
                .containsAll(declared);
    }

    @Test
    @DisplayName("读操作继承类级能力，写操作逐个收紧，替换类管理操作单独要求 knowledge:admin")
    void requiredCapabilitiesNarrowFromTheClassLevelReadToPerOperationWriteAndAdmin() {
        assertThat(effectiveCapabilities("listKnowledgeBases"))
                .containsExactlyInAnyOrder("yuheng:knowledge:read", "*");
        assertThat(effectiveCapabilities("listKnowledgeDocuments"))
                .containsExactlyInAnyOrder("yuheng:knowledge:read", "*");
        assertThat(effectiveCapabilities("getWikiGraph"))
                .containsExactlyInAnyOrder("yuheng:knowledge:read", "*");
        assertThat(effectiveCapabilities("listLlmChannels"))
                .containsExactlyInAnyOrder("yuheng:llm:read", "*");
        assertThat(effectiveCapabilities("listLlmModelConfigurations"))
                .containsExactlyInAnyOrder("yuheng:llm:read", "*");
        assertThat(effectiveCapabilities("createKnowledgeAnswer"))
                .as("grounded answering stays read-scoped")
                .containsExactlyInAnyOrder("yuheng:knowledge:read", "*");
        assertThat(effectiveCapabilities("createKnowledgeBase"))
                .containsExactlyInAnyOrder("yuheng:knowledge:write", "*");
        assertThat(effectiveCapabilities("uploadKnowledgeDocument"))
                .containsExactlyInAnyOrder("yuheng:knowledge:write", "*");
        assertThat(effectiveCapabilities("deleteKnowledgeDocument"))
                .containsExactlyInAnyOrder("yuheng:knowledge:write", "*");
        assertThat(effectiveCapabilities("publishWikiRevision"))
                .containsExactlyInAnyOrder("yuheng:knowledge:write", "*");
        assertThat(effectiveCapabilities("replaceLlmChannel"))
                .containsExactlyInAnyOrder("yuheng:llm:write", "*");
        assertThat(effectiveCapabilities("replaceLlmModel"))
                .containsExactlyInAnyOrder("yuheng:llm:write", "*");
        assertThat(effectiveCapabilities("replaceKnowledgeBase"))
                .containsExactlyInAnyOrder("yuheng:knowledge:admin", "*");
        assertThat(effectiveCapabilities("replaceKnowledgeMembers"))
                .containsExactlyInAnyOrder("yuheng:knowledge:admin", "*");
    }

    @Test
    @DisplayName("知识面没有任何匿名放行路径，全部落在管理面鉴权与文档分组过滤之内")
    void noKnowledgeOperationIsPubliclyReachable() throws IOException {
        String security = Files.readString(SECURITY_CONFIGURATION, StandardCharsets.UTF_8);
        String matchers = security.substring(
                security.indexOf("authorizeHttpRequests"), security.indexOf("permitAll"));

        assertThat(matchers)
                .as("only the definition-report ingest path and the actuator reads are public")
                .doesNotContain("/api/v1/yuheng/admin")
                .contains("/api/v1/yuheng/openapi/interface-definitions/**")
                .contains("/actuator/health/**");
        assertThat(security)
                .contains(".anyRequest().authenticated()")
                .contains("List.of(\"/api/v1/yuheng/admin/**\")")
                .contains("YUHENG_ADMIN_AUTHENTICATION_REQUIRED")
                .contains("YUHENG_ADMIN_CAPABILITY_REQUIRED");

        assertThat(yamlValues("springdoc.group-configs[0].paths-to-match"))
                .contains(ADMIN_BASE + "/**");
        assertThat(yamlValues("springdoc.api-docs.enabled"))
                .as("the document stays operator-enabled, never on by default")
                .containsExactly("${YUHENG_ADMIN_HTTP_OPENAPI_ENABLED:false}");
    }

    @Test
    @DisplayName("成功状态只来自运行时 ResponseEntity 分支，注解不谎报")
    void successStatusesAreChosenAtRuntimeAndNeverPinnedByAnnotation() {
        List<Method> handlers = handlerMethods();
        long runtimeChosen = handlers.stream()
                .filter(handler -> ResponseEntity.class.isAssignableFrom(handler.getReturnType()))
                .count();

        for (Method handler : handlers) {
            assertThat(AnnotatedElementUtils.findMergedAnnotation(handler, ResponseStatus.class))
                    .as("@ResponseStatus on %s", handler.getName())
                    .isNull();
            assertThat(AnnotatedElementUtils.findMergedAnnotation(
                    handler.getDeclaringClass(), ResponseStatus.class))
                    .as("class-level @ResponseStatus on %s", handler.getDeclaringClass().getSimpleName())
                    .isNull();
        }
        assertThat(runtimeChosen)
                .as("the nine accepted/created outcomes are chosen per branch, the seventeen reads answer 200")
                .isEqualTo(9L);
    }

    @Test
    @DisplayName("创建知识库返回 201、Location 与不含持久化字段的知识库投影")
    void createKnowledgeBaseAnswersTwentyOneWithTheNewLocation() throws Exception {
        when(knowledgeService.createBase(any(), any(), any())).thenReturn(new KnowledgeBaseVO()
                .setId("7001")
                .setName("合同知识库")
                .setDescription("企业合同与交付知识库")
                .setOwnerActorId("svc:yuheng-admin-operator")
                .setMyRole(KnowledgeMemberRoleEnum.OWNER)
                .setEgressPolicy(KnowledgeEgressPolicyEnum.LOCAL_ONLY)
                .setChatModel("company-chat")
                .setEmbeddingModel("company-embed-v1")
                .setEmbeddingSpaceId(null)
                .setDimensions(null)
                .setRevision(1L));

        MvcResult result = mockMvc(new KnowledgeController(knowledgeService))
                .perform(post(ADMIN_BASE + "/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "kb-create-1")
                        .content(body("name", "合同知识库",
                                "description", "企业合同与交付知识库",
                                "egressPolicy", "LOCAL_ONLY",
                                "chatModel", "company-chat",
                                "embeddingModel", "company-embed-v1")))
                .andExpect(status().isCreated())
                .andExpect(locationAt(ADMIN_BASE + "/knowledge-bases/7001"))
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.egressPolicy").value("LOCAL_ONLY"))
                .andReturn();

        assertThat(fieldNames(result)).containsExactlyInAnyOrder(
                "id", "name", "description", "ownerActorId", "myRole", "egressPolicy",
                "chatModel", "embeddingModel", "embeddingSpaceId", "dimensions", "revision");
        assertThat(JSON.readTree(result.getResponse().getContentAsString()).get("dimensions").isNull())
                .as("a nullable projection stays present as null instead of being silently dropped")
                .isTrue();
    }

    @Test
    @DisplayName("四个异步受理返回 202、作业 Location 与 Retry-After: 2")
    void theFourAsyncAcceptancesAnswerTwentyTwoWithTheJobPollingAddress() throws Exception {
        when(knowledgeService.uploadDocument(any(), any(), any(), any())).thenReturn(document("9100"));
        when(knowledgeService.createReindexJob(any(), any(), any(), any(), any())).thenReturn(job("9101"));
        when(knowledgeJobService.retryJob(any(), any(), any(), any())).thenReturn(job("9102"));
        when(wikiService.createGenerationJob(any(), any(), any(), any())).thenReturn(job("9103"));

        mockMvc(new KnowledgeDocumentController(knowledgeService))
                .perform(post(ADMIN_BASE + "/knowledge-bases/7001/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "doc-upload-1")
                        .content(body("fileName", "handbook.pdf",
                                "mediaType", "application/pdf",
                                "content", "aGFuZGJvb2sucGRm")))
                .andExpect(status().isAccepted())
                .andExpect(locationAt(ADMIN_BASE + "/knowledge-jobs/9100"))
                .andExpect(header().string("Retry-After", "2"));

        mockMvc(new KnowledgeDocumentController(knowledgeService))
                .perform(post(ADMIN_BASE + "/knowledge-bases/7001/documents/8001/reindex-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "reindex-1")
                        .content(body("sourceRevisionId", "8101", "expectedRevision", 3)))
                .andExpect(status().isAccepted())
                .andExpect(locationAt(ADMIN_BASE + "/knowledge-jobs/9101"));

        mockMvc(new KnowledgeJobController(knowledgeJobService))
                .perform(post(ADMIN_BASE + "/knowledge-jobs/9101/retries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "retry-1")
                        .content(body("expectedRevision", 3)))
                .andExpect(status().isAccepted())
                .andExpect(locationAt(ADMIN_BASE + "/knowledge-jobs/9102"));

        mockMvc(new WikiController(wikiService))
                .perform(post(ADMIN_BASE + "/knowledge-bases/7001/wiki/generation-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "wiki-1")
                        .content(body("sourceRevisionIds", List.of("8101"), "expectedRevision", 0L)))
                .andExpect(status().isAccepted())
                .andExpect(locationAt(ADMIN_BASE + "/knowledge-jobs/9103"))
                .andExpect(header().string("Retry-After", "2"));
    }

    @Test
    @DisplayName("软删文档与撤回发布返回 204 且没有响应体")
    void deleteAndUnpublishAnswerTwentyFourWithNoBody() throws Exception {
        when(knowledgeService.deleteDocument(any(), any(), any(), any())).thenReturn(document(null));

        mockMvc(new KnowledgeDocumentController(knowledgeService))
                .perform(delete(ADMIN_BASE + "/knowledge-bases/7001/documents/8001")
                        .param("expectedRevision", "3"))
                .andExpect(status().isNoContent())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEmpty());

        mockMvc(new WikiController(wikiService))
                .perform(delete(ADMIN_BASE + "/knowledge-bases/7001/wiki/pages/9001/publications/current")
                        .param("expectedRevision", "4"))
                .andExpect(status().isNoContent())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEmpty());
    }

    @Test
    @DisplayName("渠道与模型完整保存按 expectedRevision 区分 201 与 200，绝不谎报创建")
    void llmFullSaveAnswersTwentyOneForTheCreateIntentAndTwentyForAReplacement() throws Exception {
        when(llmConfigurationService.replaceChannel(any(), any(), any(), any()))
                .thenReturn(new LlmChannelVO().setKey("local-a").setRevision(1L));
        when(llmConfigurationService.replaceModel(any(), any(), any(), any()))
                .thenReturn(new LlmModelVO().setKey("company-chat").setRevision(4L));
        MockMvc mvc = mockMvc(llmConfigurationController());

        mvc.perform(put(ADMIN_BASE + "/llm/channels/local-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(channelCommand("local-a", 0)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));

        mvc.perform(put(ADMIN_BASE + "/llm/channels/local-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(channelCommand("local-a", 1)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"));

        mvc.perform(put(ADMIN_BASE + "/llm/models/company-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(modelCommand("company-chat", 0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("company-chat"));
    }

    @Test
    @DisplayName("分页读返回 200 的 items/page/size/total 信封，空页是 [] 而非缺字段")
    void pagedReadsAnswerTwentyWithThePageEnvelopeOnly() throws Exception {
        when(knowledgeService.listBases(any(), any())).thenReturn(new KnowledgePageVO<KnowledgeBaseVO>()
                .setItems(List.of()).setPage(1).setSize(20).setTotal(0L));
        when(llmConfigurationService.listChannels(anyInt(), anyInt()))
                .thenReturn(new KnowledgePageVO<LlmChannelVO>()
                        .setItems(List.of(new LlmChannelVO().setKey("local-a").setSecretRef("llm/local-a")))
                        .setPage(1).setSize(20).setTotal(1L));

        MvcResult emptyPage = mockMvc(new KnowledgeController(knowledgeService))
                .perform(get(ADMIN_BASE + "/knowledge-bases"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(fieldNames(emptyPage)).containsExactlyInAnyOrder("items", "page", "size", "total");
        JsonNode items = JSON.readTree(emptyPage.getResponse().getContentAsString()).get("items");
        assertThat(items.isArray() && items.isEmpty())
                .as("an empty page is an empty array, never a missing field")
                .isTrue();

        MvcResult channelPage = mockMvc(llmConfigurationController())
                .perform(get(ADMIN_BASE + "/llm/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].secretRef").value("llm/local-a"))
                .andReturn();
        assertThat(fieldNamesOf(channelPage, "/items/0")).containsExactlyInAnyOrder(
                "key", "name", "deployment", "protocol", "baseUrl", "secretRef", "enabled",
                "connectTimeoutMs", "headerTimeoutMs", "idleTimeoutMs", "totalTimeoutMs",
                "maxConcurrent", "revision");
    }

    @Test
    @DisplayName("业务失败映射为管理面错误信封状态，绝不返回伪造成功")
    void businessFailuresBecomeTheAdminErrorEnvelope() throws Exception {
        // doThrow 而不是 when().thenThrow()：同一 mock 上重新打桩会重放上一个抛错答案，异常会在装配阶段逃出 MockMvc。
        // doThrow instead of when().thenThrow(): re-stubbing the same mock replays the previous throwing answer and the
        // exception escapes while the test is still being wired rather than travelling through the controller advice.
        doThrow(new GatewayAdminNotFoundException("knowledge base not found"))
                .when(knowledgeService).listBases(any(), any());
        mockMvc(new KnowledgeController(knowledgeService))
                .perform(get(ADMIN_BASE + "/knowledge-bases"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").exists());

        doThrow(new GatewayAdminRevisionConflictException(7L))
                .when(knowledgeService).listBases(any(), any());
        mockMvc(new KnowledgeController(knowledgeService))
                .perform(get(ADMIN_BASE + "/knowledge-bases"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentRevision").value(7));

        doThrow(new IllegalStateException("Tianshu registration is unavailable"))
                .when(knowledgeService).listBases(any(), any());
        mockMvc(new KnowledgeController(knowledgeService))
                .perform(get(ADMIN_BASE + "/knowledge-bases"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("YUHENG_ADMIN_TIANSHU_UNAVAILABLE"));
    }

    @Test
    @DisplayName("被校验拒绝的请求不触达业务合同，也没有成功可返回")
    void aRejectedRequestNeverReachesTheBusinessContract() throws Exception {
        mockMvc(new KnowledgeController(knowledgeService))
                .perform(post(ADMIN_BASE + "/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("YUHENG_ADMIN_VALIDATION_FAILED"));

        verifyNoInteractions(knowledgeService);
    }

    @Test
    @DisplayName("知识面线上载体只投影业务字段，持久化内部列与 PO 类型都不出现")
    void wireCarriersProjectNoPersistenceInternals() {
        for (Class<?> carrier : WIRE_CARRIERS) {
            for (Field field : instanceFields(carrier)) {
                assertThat(field.getType().getSimpleName())
                        .as("%s#%s must not carry a row model", carrier.getSimpleName(), field.getName())
                        .doesNotEndWith("PO");
                assertThat(field.getType().getPackageName())
                        .as("%s#%s must not reach into the persistence package",
                                carrier.getSimpleName(), field.getName())
                        .doesNotContain(".domain.po");
                assertThat(field.getName())
                        .as("%s#%s is a persistence-internal column", carrier.getSimpleName(), field.getName())
                        .isNotIn(List.of("tenantId", "deletedAt", "createTime", "updateTime",
                                "createUserId", "updateUserId", "rawBytes", "objectKey", "serverPath"));
            }
        }
    }

    @Test
    @DisplayName("可空线上字段是引用类型，枚举按 @JsonValue 上线而不是序号，也没有载体隐藏 null")
    void nullableWireFieldsAreReferenceTypesAndEnumsCarryTheirWireValue() throws Exception {
        for (Class<?> carrier : List.of(KnowledgeBaseVO.class, KnowledgeDocumentVO.class, KnowledgeJobVO.class)) {
            for (Field field : instanceFields(carrier)) {
                if (field.isAnnotationPresent(NotNull.class)) {
                    continue;
                }
                assertThat(field.getType().isPrimitive())
                        .as("%s#%s must stay a reference type so absence is null, never a zero default",
                                carrier.getSimpleName(), field.getName())
                        .isFalse();
            }
        }

        JsonNode job = JSON.valueToTree(job("9101"));
        assertThat(names(job)).containsExactlyInAnyOrderElementsOf(List.of(
                "id", "kbId", "type", "resourceId", "status", "stage", "revision",
                "attempt", "errorCode", "createdAt", "updatedAt"));
        assertThat(job.get("type").isTextual() && job.get("status").isTextual())
                .as("an enum never reaches the wire as its ordinal")
                .isTrue();
        assertThat(job.get("type").asText()).isEqualTo(KnowledgeJobTypeEnum.DOCUMENT_INGEST.wireValue());
        assertThat(job.get("status").asText()).isEqualTo(KnowledgeJobStatusEnum.QUEUED.wireValue());
        assertThat(job.get("stage").isNull() && job.get("attempt").isNull())
                .as("declared-null members stay observable instead of vanishing")
                .isTrue();
        assertThat(annotatedJsonIncludeCarriers()).isEmpty();
    }

    private MockMvc mockMvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GatewayAdminExceptionHandler())
                .setCustomArgumentResolvers(new GatewayAdminActorArgumentResolver())
                .build();
    }

    /** 中文说明：控制器刻意按当前部署上下文拼装绝对 {@code Location}，本测试固定的是合同路径本身加上部署主机；
     * standalone MockMvc 的请求主机固定为 {@code http://localhost}，因此这里断言绝对地址等于同一部署下的该路径。
     * English summary: the controllers build an absolute {@code Location} from the deployment context, so the pinned
     * value is that path under the deployment host; standalone MockMvc always resolves the mock request to
     * {@code http://localhost}, which is why the absolute form rather than a bare path is expected here.
     *
     * 用法 / Usage: {@code .andExpect(locationAt(ADMIN_BASE + "/knowledge-bases/" + id))}。
     * @param path 合同路径；parameter the contractual request path
     * @return 返回 Location 断言；returns the Location header matcher
     */
    private static ResultMatcher locationAt(String path) {
        return header().string("Location", Matchers.equalTo("http://localhost" + path));
    }

    private Object llmConfigurationController() throws Exception {
        return LlmConfigurationController.class
                .getConstructor(LlmConfigurationService.class)
                .newInstance(llmConfigurationService);
    }

    /** 中文说明：PUT 是完整保存而不是局部更新，因此请求体必须满足 {@code LlmChannelCommandDTO} 的全部必填约束；
     * 只有 {@code expectedRevision} 随创建/替换意图变化，其余字段两种意图下必须同构。
     * English summary: PUT is a full save rather than a patch, so the body satisfies every mandatory constraint of
     * {@code LlmChannelCommandDTO}; only {@code expectedRevision} carries the create-or-replace intent.
     *
     * 用法 / Usage: {@code channelCommand("local-a", 0L)} for a create intent.
     * @param key 渠道稳定 key；parameter the channel stable key
     * @param expectedRevision 乐观版本哨兵；parameter the optimistic revision sentinel
     * @return 返回 请求体 JSON；returns the command body
     */
    private static String channelCommand(String key, long expectedRevision) throws Exception {
        return body("key", key,
                "name", "本地渠道",
                "deployment", "LOCAL",
                "protocol", "OPENAI_CHAT",
                "baseUrl", "http://127.0.0.1:18080/v1",
                "secretRef", "llm/local-a",
                "enabled", true,
                "connectTimeoutMs", 3_000,
                "headerTimeoutMs", 30_000,
                "idleTimeoutMs", 60_000,
                "totalTimeoutMs", 120_000,
                "maxConcurrent", 8,
                "expectedRevision", expectedRevision);
    }

    /** 中文说明：CHAT 别名的完整命令，向量专属字段保持 null，由 {@code @ValidLlmModel} 校验 kind 与 dimensions /
     * embeddingSpaceId 的一致性；routes 复用同一个顶层绑定载体。
     * English summary: a full CHAT alias command keeping the vector-only members null so {@code @ValidLlmModel} can check
     * kind against dimensions/embeddingSpaceId, with routes reusing the same top-level binding carrier.
     *
     * 用法 / Usage: {@code modelCommand("company-chat", 0L)}。
     * @param key 模型别名稳定 key；parameter the model alias stable key
     * @param expectedRevision 乐观版本哨兵；parameter the optimistic revision sentinel
     * @return 返回 请求体 JSON；returns the command body
     */
    private static String modelCommand(String key, long expectedRevision) throws Exception {
        return body("key", key,
                "name", "企业对话模型",
                "kind", "CHAT",
                "protocols", List.of("OPENAI_CHAT"),
                "enabled", true,
                "dimensions", null,
                "embeddingSpaceId", null,
                "allowedSubjects", List.of("dept:knowledge"),
                "routes", List.of(Map.of("channelKey", "local-a",
                        "upstreamModel", key,
                        "capabilities", List.of("TEXT"),
                        "priority", 0,
                        "weight", 100)),
                "expectedRevision", expectedRevision);
    }

    private KnowledgeDocumentVO document(String latestJobId) {
        return new KnowledgeDocumentVO()
                .setId("8001")
                .setKbId("7001")
                .setFileName("handbook.pdf")
                .setActiveRevisionId(null)
                .setLatestJobId(latestJobId)
                .setRevision(1L)
                .setStatus("STAGING")
                .setCreatedAt(Instant.parse("2026-09-22T08:30:00Z"));
    }

    private KnowledgeJobVO job(String id) {
        return new KnowledgeJobVO()
                .setId(id)
                .setKbId("7001")
                .setType(KnowledgeJobTypeEnum.DOCUMENT_INGEST)
                .setResourceId("8001")
                .setStatus(KnowledgeJobStatusEnum.QUEUED)
                .setStage(null)
                .setRevision(1L)
                .setAttempt(null)
                .setErrorCode(null)
                .setCreatedAt(Instant.parse("2026-09-22T08:30:00Z"))
                .setUpdatedAt(Instant.parse("2026-09-22T08:30:00Z"));
    }

    private static List<String> operations() {
        List<String> operations = new ArrayList<>();
        for (Method handler : handlerMethods()) {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(handler, RequestMapping.class);
            for (RequestMethod verb : mapping.method()) {
                operations.add(verb.name() + " " + mergedPath(handler.getDeclaringClass())
                        + String.join("", mapping.path()) + " -> " + handler.getAnnotation(Operation.class).operationId());
            }
        }
        return operations;
    }

    private static List<Method> handlerMethods() {
        List<Method> handlers = new ArrayList<>();
        for (Class<?> controller : KNOWLEDGE_PLANE_CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) != null
                        && method.isAnnotationPresent(Operation.class)) {
                    handlers.add(method);
                }
            }
        }
        return handlers;
    }

    private static String mergedPath(Class<?> owner) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(owner, RequestMapping.class);
        return mapping == null ? "" : String.join("", mapping.path());
    }

    private static String policyOf(Method handler) {
        return handler.getAnnotation(Operation.class).operationId();
    }

    private static Set<String> effectiveCapabilities(String operationId) {
        for (Method handler : handlerMethods()) {
            if (!operationId.equals(policyOf(handler))) {
                continue;
            }
            PreAuthorize guard = AnnotatedElementUtils.findMergedAnnotation(handler, PreAuthorize.class);
            if (guard == null) {
                guard = AnnotatedElementUtils.findMergedAnnotation(handler.getDeclaringClass(), PreAuthorize.class);
            }
            assertThat(guard).as("capability guard for %s", operationId).isNotNull();
            Set<String> capabilities = new LinkedHashSet<>();
            for (String authority : guard.value()
                    .replace("hasAnyAuthority(", "")
                    .replace(")", "")
                    .split(",")) {
                capabilities.add(authority.trim().replace("'", "").substring("CAP_".length()));
            }
            return capabilities;
        }
        throw new AssertionError("no handler declares operationId " + operationId);
    }

    /**
     * 中文说明：{@code Idempotency-Key} 的绑定位置从形参注解读取，用于把策略声明与真实接受该 Header 的操作做差。
     * English summary: Reads the {@code Idempotency-Key} binding sites from the parameter annotations so the declared
     * policy can be diffed against the operations that really accept the header.
     * @return 返回接受该 Header 的 operationId 集合；returns the operation ids that accept the header.
     */
    private static Set<String> declaredIdempotencyKeyBindings() {
        Set<String> bound = new LinkedHashSet<>();
        for (Method handler : handlerMethods()) {
            boolean acceptsKey = Arrays.stream(handler.getParameterAnnotations())
                    .flatMap(Arrays::stream)
                    .anyMatch(annotation -> annotation instanceof RequestHeader header
                            && "Idempotency-Key".equals(header.value().isEmpty() ? header.name() : header.value()));
            if (acceptsKey) {
                bound.add(policyOf(handler));
            }
        }
        return bound;
    }

    private static Set<String> nativeGatewayEntries() throws IOException {
        String source = Files.readString(LLM_ENTRY_CONTROLLER, StandardCharsets.UTF_8);
        Map<String, String> handlers = Map.of(
                "CHAT_COMPLETIONS_PATH", "createLlmChatCompletion",
                "EMBEDDINGS_PATH", "createLlmEmbeddings",
                "RESPONSES_PATH", "createLlmResponse",
                "MESSAGES_PATH", "createLlmMessage",
                "MODELS_PATH", "listLlmModels");
        Set<String> entries = new LinkedHashSet<>();
        for (Map.Entry<String, String> declared : handlers.entrySet()) {
            String path = quotedConstant(source, declared.getKey());
            String verb = "MODELS_PATH".equals(declared.getKey()) ? "Get" : "Post";
            assertThat(source)
                    .as("%s is mapped on the %s verb", path, verb.toUpperCase())
                    .contains("@" + verb + "Mapping(" + declared.getKey() + ")");
            entries.add(verb.toUpperCase() + " " + path + " -> " + declared.getValue());
        }
        return entries;
    }

    private static String quotedConstant(String source, String constant) {
        int start = source.indexOf(constant + " = \"");
        assertThat(start).as("the %s path constant is declared", constant).isNotNegative();
        String tail = source.substring(start + constant.length() + " = \"".length());
        return tail.substring(0, tail.indexOf('"'));
    }

    private static List<Field> instanceFields(Class<?> carrier) {
        return Arrays.stream(carrier.getDeclaredFields())
                .filter(field -> !field.isSynthetic() && !Modifier.isStatic(field.getModifiers()))
                .toList();
    }

    private static Set<String> annotatedJsonIncludeCarriers() {
        Set<String> annotated = new LinkedHashSet<>();
        for (Class<?> carrier : WIRE_CARRIERS) {
            if (carrier.isAnnotationPresent(JsonInclude.class)) {
                annotated.add(carrier.getSimpleName());
            }
        }
        return annotated;
    }

    private static Set<String> yamlValues(String key) {
        Set<String> values = new LinkedHashSet<>();
        try {
            for (PropertySource<?> source : new YamlPropertySourceLoader().load("base",
                    new FileSystemResource(Path.of("src/main/resources/application.yml")))) {
                if (source instanceof EnumerablePropertySource<?> enumerable) {
                    Arrays.stream(enumerable.getPropertyNames())
                            .filter(name -> name.startsWith(key))
                            .forEach(name -> {
                                Object value = enumerable.getProperty(name);
                                if (value != null) {
                                    values.add(String.valueOf(value));
                                }
                            });
                }
            }
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
        return values;
    }

    private static List<String> fieldNames(MvcResult result) throws Exception {
        return names(JSON.readTree(result.getResponse().getContentAsString()));
    }

    private static List<String> fieldNamesOf(MvcResult result, String pointer) throws Exception {
        return names(JSON.readTree(result.getResponse().getContentAsString()).at(pointer));
    }

    private static List<String> names(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static String body(Object... pairs) throws Exception {
        var document = JSON.createObjectNode();
        for (int index = 0; index < pairs.length; index += 2) {
            document.putPOJO(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return JSON.writeValueAsString(document);
    }
}
