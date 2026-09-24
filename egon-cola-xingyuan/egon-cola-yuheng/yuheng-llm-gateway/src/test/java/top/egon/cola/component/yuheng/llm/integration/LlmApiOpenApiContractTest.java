package top.egon.cola.component.yuheng.llm.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yuheng.llm.config.LlmGatewayConfiguration;
import top.egon.cola.component.yuheng.llm.config.LlmGatewayProperties;
import top.egon.cola.component.yuheng.llm.proxy.controller.LlmApiController;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmModelKindEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.exception.LlmInvocationException;
import top.egon.cola.component.yuheng.llm.proxy.domain.vo.LlmInvocationResultVO;
import top.egon.cola.component.yuheng.llm.proxy.repository.LlmConfigurationRepository;
import top.egon.cola.component.yuheng.llm.proxy.service.AnthropicMessagesProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmInvocationService;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmServletStreamComponent;
import top.egon.cola.component.yuheng.llm.proxy.service.OpenAiChatProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.OpenAiEmbeddingProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.OpenAiResponsesProtocolStrategy;

/**
 * 中文说明：{@code LlmApiOpenApiContractTest} 是 Step 16 File 3 的模型入口合同闸门，固定 {@link LlmApiController}
 * 的四件事：① <b>原生面</b>恰好是 API-001/002/003/029/030 这五个「方法 + 路径 + operationId」三元组——注解视图与
 * 源码视图必须逐项相等，既没有第六个入口，也没有任何一个入口落在 admin 的 {@code /api/v1/yuheng/admin} 前缀下；
 * ② {@code entryProtocols()} 覆盖这五个入口，且协议只取 {@link LlmProtocolEnum#wireValue()} 的四个批准字面量
 * （目录面按 Spec 走 OpenAI 面），数字形态的 {@code code}/{@code ordinal} 永不上 wire；
 * ③ <b>运行时错误 fixtures</b>：{@link LlmInvocationException} 的四个安全字段在 OpenAI 三面编码成
 * {@code error{message,type,param,code}}（{@code param} 恒在位、可为 JSON null，绝不省略），Messages 面编码成
 * {@code {type:"error",error:{type,message}}}（永无 {@code code}/{@code param}），路由 {@link CommonException}
 * 也按入口路径换成同协议原生错误、状态逐字保留、永不套 admin 的 {@code code}/{@code data} 信封；
 * {@code retryable} 与 {@code NATIVE_CODES_BY_STATUS}/{@code RETRYABLE_STATUSES} 两份库存一致，且只有可重试状态
 * 才会消耗第二条候选 route；④ 凭据与出网自证字段在路由之前就被 {@code FORBIDDEN_ENTRY_FIELDS} 全量拒绝
 * （大小写不敏感、不回显字段值、被拒请求对 {@code invoke} 零副作用），而 {@code GET /v1/models} 只投影被授权且
 * enabled 的 alias，任何 {@code baseUrl}/{@code secretRef}/凭据材料都不可能出现在响应字节里。
 * 本类<b>不启动 Spring 容器</b>：本仓库刻意缺少启动所需的运维配置（数据源、ShardingSphere、Redis、Kafka、
 * tianquan JWT issuer/JWK、tianshu），三道过滤器的 {@code IllegalStateException} 守卫会让启动直接失败。证据只来自
 * 既有测试已采用的三条途径：注解反射、{@code MockMvcBuilders.standaloneSetup(controller)} 加控制器自带的
 * {@code @ExceptionHandler}（本模块没有独立的 advice 类），以及用本模块 {@link LlmGatewayProperties} 与
 * {@link LlmGatewayConfiguration#llmProtocolStrategyRegistry()} 真实装配出来的 Strategy 与
 * {@link LlmServletStreamComponent}（目录面的断言读的是它真实写出的字节）。
 * 注意（runtime-unverified）：{@code /v3/api-docs} 与已发布的 {@code openapi: 3.1.0} 文档本身在 standalone MockMvc
 * 下不存在，本模块也不依赖 openapi starter（{@code swagger-annotations-jakarta} 在 common-core 里是
 * {@code provided}/{@code optional}，控制器因此不声明 {@code @Operation}），operationId 的实际载体是「处理器方法名 +
 * 方法 javadoc 首行」，本类从反射与源码两侧同时钉住它；文档是否真的发布出来只能由启动服务证明，此处不伪造。
 * English summary: {@code LlmApiOpenApiContractTest} is the Step 16 File 3 gate over the model entry. It pins (1) that
 * the native surface is exactly the five {@code method + path + operationId} triples of API-001/002/003/029/030, with the
 * annotation view and the source view equal item by item, no sixth entry point and nothing mounted under admin's
 * {@code /api/v1/yuheng/admin} prefix; (2) that {@code entryProtocols()} covers all five entries and speaks only the four
 * approved {@link LlmProtocolEnum#wireValue()} literals (the catalogue rides the OpenAI face per Spec), never a numeric
 * code or ordinal on the wire; (3) the runtime error fixtures — the four safe fields of {@link LlmInvocationException}
 * become {@code error{message,type,param,code}} on the three OpenAI faces, where {@code param} always stays in place and
 * may be JSON null but is never dropped, and {@code {type:"error",error:{type,message}}} on Messages with no
 * {@code code} and no {@code param} at all, while a routing {@link CommonException} is re-encoded into the invoked
 * entry's own protocol with its status preserved verbatim and never inside admin's {@code code}/{@code data} envelope,
 * and {@code retryable} agrees with the {@code NATIVE_CODES_BY_STATUS}/{@code RETRYABLE_STATUSES} inventories and is the
 * only thing that lets a second candidate route be spent; (4) that every self-asserted egress or credential field dies
 * before routing via {@code FORBIDDEN_ENTRY_FIELDS} (case-insensitively, without echoing the submitted value, with zero
 * effect on {@code invoke}), while {@code GET /v1/models} projects only the authorized enabled aliases so no
 * {@code baseUrl}/{@code secretRef}/credential material can reach the response bytes. No Spring context starts here: this
 * repository deliberately omits the operator configuration a boot needs (datasource, ShardingSphere, Redis, Kafka, the
 * tianquan JWT issuer/JWK, tianshu) and three filter guards abort startup with {@code IllegalStateException}, so the
 * evidence comes only from the three routes the sibling tests already use — annotation reflection,
 * {@code MockMvcBuilders.standaloneSetup(controller)} on top of the controller's own {@code @ExceptionHandler} methods
 * (this module has no separate advice class), and Strategies plus the {@link LlmServletStreamComponent} wired for real
 * through this module's {@link LlmGatewayProperties} and
 * {@link LlmGatewayConfiguration#llmProtocolStrategyRegistry()}, whose bytes the catalogue assertions read back.
 * Runtime-unverified on purpose: {@code /v3/api-docs} and the published {@code openapi: 3.1.0} document do not exist
 * under standalone MockMvc, and this module pulls in no openapi starter ({@code swagger-annotations-jakarta} is
 * {@code provided}/{@code optional} in common-core, so the controller declares no {@code @Operation}); the real carrier
 * of an operationId is the handler name plus the first line of its javadoc, which this class pins from reflection and
 * from source text at once, while whether the document is ever published stays a booted-service fact and is not faked.
 *
 * 用法 / Usage: 注解事实走反射（{@link AnnotatedElementUtils} 与私有静态成员的反射读取），面清单与 operationId 另读
 * 本模块 {@code src/main/java} 下的控制器源码文本，错误 shape 与目录 shape 全部由 standalone MockMvc 真实发起请求、
 * 由 {@link LlmServletStreamComponent} 真实写出字节后断言；绝不抓 {@code /v3/api-docs}，绝不连数据库。
 * / Annotation facts come from reflection, the surface and operationIds are additionally read from this module's
 * {@code src/main/java} controller text, and every error and catalogue shape is asserted from bytes a real standalone
 * MockMvc call actually wrote through {@link LlmServletStreamComponent}; {@code /v3/api-docs} is never fetched and no
 * database is ever reached. 聚焦执行 / Focused run:
 * {@code ./mvnw -o -f …/yuheng-llm-gateway/pom.xml -Dtest=LlmApiOpenApiContractTest test}。
 */
class LlmApiOpenApiContractTest {

    private static final String SUBJECT = "svc:wiki-indexer";
    private static final String ADMIN_PREFIX = "/api/v1/yuheng/admin";
    private static final String CHAT_ALIAS = "company-chat";
    private static final String BASE_URL = "https://local-llm.internal:8000/v1";
    private static final String SECRET_REF = "env:LLM_LOCAL_A_SECRET";
    private static final String CREDENTIAL_TEXT = "sk-enterprise-upstream-credential-4f4a";
    private static final String UPSTREAM_MODEL = "vendor-real-model-x";
    private static final String UNSUPPORTED_PARAMETER = "unsupported_parameter";
    private static final String CONTROLLER_SOURCE =
            "src/main/java/top/egon/cola/component/yuheng/llm/proxy/controller/LlmApiController.java";

    /** 中文说明：Spec §9.2.1/§9.2.29 允许的 OpenAI 错误分类，三面共用一份，未列出的分类即即兴发明。 English summary: the OpenAI error categories Spec §9.2.1/§9.2.29 allows, shared by the three faces, where anything else counts as invented. */
    private static final Set<String> OPENAI_ERROR_TYPES = Set.of("invalid_request_error", "authentication_error",
            "permission_error", "not_found_error", "rate_limit_error", "api_error", "server_error");

    /** 中文说明：Spec §9.2.30 允许的 Messages 错误分类，与上面那份并列，用来证明两面不是同一形状的别名。 English summary: the Messages categories Spec §9.2.30 allows, listed beside the OpenAI set to prove the two faces are not aliases of one shape. */
    private static final Set<String> ANTHROPIC_ERROR_TYPES = Set.of("invalid_request_error", "authentication_error",
            "permission_error", "not_found_error", "request_too_large", "rate_limit_error", "api_error",
            "overloaded_error");

    /** 中文说明：五个入口的合同三元组，逐字取自 Spec §9.1/§9.2 与本模块的端点常量。 English summary: the five contract triples, taken verbatim from Spec §9.1/§9.2 and this module's endpoint constants. */
    private static final List<EntryContract> ENTRIES = List.of(
            new EntryContract("API-001", "POST", "/v1/chat/completions", "createLlmChatCompletion",
                    "CHAT_COMPLETIONS_PATH"),
            new EntryContract("API-002", "POST", "/v1/embeddings", "createLlmEmbeddings", "EMBEDDINGS_PATH"),
            new EntryContract("API-003", "GET", "/v1/models", "listLlmModels", "MODELS_PATH"),
            new EntryContract("API-029", "POST", "/v1/responses", "createLlmResponse", "RESPONSES_PATH"),
            new EntryContract("API-030", "POST", "/v1/messages", "createLlmMessage", "MESSAGES_PATH"));

    /** 中文说明：容器唯一 Jackson 栈的测试等价物（Boot 等价构造，注册 jsr310，因此永不因 {@code Instant} 失败）。 English summary: the test's equivalent of the container's single Jackson stack, built the Boot way so an {@code Instant} can never break it. */
    private static final ObjectMapper JSON = Jackson2ObjectMapperBuilder.json().build();

    @Test
    @DisplayName("原生面恰是五个「方法+路径+operationId」三元组：无第六入口，也无一落在 admin 前缀下")
    void theNativeSurfaceIsExactlyTheFiveContractEntriesAndNoneSitsUnderTheAdminPrefix() {
        Map<String, String> annotated = annotatedEntries();
        List<String> contract = ENTRIES.stream().map(EntryContract::route).sorted().toList();

        assertThat(annotated.keySet()).as("the five mapped routes of the model entry")
                .containsExactlyElementsOf(contract);
        assertThat(annotated.values()).as("operationIds carried by the handler names")
                .containsExactlyInAnyOrderElementsOf(ENTRIES.stream().map(EntryContract::operationId).toList());
        assertThat(annotated.keySet()).noneMatch(route -> route.contains(ADMIN_PREFIX));
        assertThat(AnnotatedElementUtils.findMergedAnnotation(LlmApiController.class, RequestMapping.class))
                .as("no class-level prefix may widen a contract path").isNull();

        assertThat(sourceMappedRoutes()).as("the source view must agree with the annotation view item by item")
                .isEqualTo(contract);
        assertThat(adminPrefixedMappingLines()).as("the gateway mounts nothing under the admin prefix").isEmpty();
    }

    @Test
    @DisplayName("每个入口都声明非空 operationId：处理器名与方法 javadoc 首行必须给出同一个值")
    void everyEntryHandlerDeclaresANonBlankOperationIdInItsOwnNameAndJavadoc() {
        String source = readControllerSource();
        Map<String, String> annotated = annotatedEntries();

        for (EntryContract entry : ENTRIES) {
            Method handler = handlerFor(entry);
            assertThat(annotated.get(entry.route())).as("operationId of %s", entry.path())
                    .isEqualTo(entry.operationId());
            assertThat(entry.operationId()).isNotBlank();
            assertThat(Modifier.isPublic(handler.getModifiers())).isTrue();
            assertThat(handler.getReturnType()).as("the native entry writes bytes, it returns no VO")
                    .isEqualTo(void.class);
            assertThat(handler.getParameterCount()).as("handler arity of %s", entry.path()).isEqualTo(2);

            String javadoc = javadocAbove(source, entry.annotation());
            assertThat(javadoc).as("the javadoc of %s records the operationId", entry.path())
                    .contains(entry.operationId());
            assertThat(javadoc).as("the javadoc of %s records the API id", entry.path()).contains(entry.apiId());
            assertThat(javadoc).as("the javadoc of %s records the exact route", entry.path())
                    .contains(entry.route());
        }
        // swagger-annotations-jakarta 在本模块是 provided/optional，控制器不声明 @Operation：这里证明「没有第二套
        // operationId 载体」，其上唯一的载体就是被反射与 javadoc 同时钉住的那个处理器名（见类注释的 runtime-unverified）。
        assertThat(swaggerAnnotatedHandlers(LlmApiController.class)).as("no @Operation carrier exists in this module")
                .isEmpty();
    }

    @Test
    @DisplayName("entryProtocols() 覆盖五入口且只用四个批准 wire 值，目录面走 OpenAI 面")
    void theEntryProtocolTableCoversEveryEntryAndOnlyEverUsesTheApprovedWireValues() {
        Map<String, LlmProtocolEnum> table = LlmApiOpenApiContractTest.<Map<String, LlmProtocolEnum>>controllerStatic(
                "entryProtocols");

        assertThat(table.keySet()).as("every mapped path decides a protocol")
                .containsExactlyInAnyOrderElementsOf(ENTRIES.stream().map(EntryContract::path).toList());
        assertThat(table.get("/v1/chat/completions")).isEqualTo(LlmProtocolEnum.OPENAI_CHAT);
        assertThat(table.get("/v1/embeddings")).isEqualTo(LlmProtocolEnum.OPENAI_EMBEDDING);
        assertThat(table.get("/v1/responses")).isEqualTo(LlmProtocolEnum.OPENAI_RESPONSES);
        assertThat(table.get("/v1/messages")).isEqualTo(LlmProtocolEnum.ANTHROPIC_MESSAGES);
        assertThat(table.get("/v1/models")).as("the catalogue is an OpenAI face, not a fifth protocol")
                .isEqualTo(LlmProtocolEnum.OPENAI_CHAT);
        assertThat(new LinkedHashSet<>(table.values())).as("the table speaks all four protocols and no fifth")
                .containsExactlyInAnyOrderElementsOf(Arrays.asList(LlmProtocolEnum.values()));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> table.put("/v1/sixth", LlmProtocolEnum.OPENAI_CHAT));

        assertThat(Arrays.stream(LlmProtocolEnum.values()).map(LlmProtocolEnum::wireValue).toList())
                .containsExactlyInAnyOrder("OPENAI_CHAT", "OPENAI_EMBEDDING", "OPENAI_RESPONSES",
                        "ANTHROPIC_MESSAGES");
        for (LlmProtocolEnum protocol : LlmProtocolEnum.values()) {
            assertThat(protocol.wireValue()).matches("^[A-Z][A-Z0-9_]*$");
            assertThat(JSON.convertValue(protocol, String.class)).as("%s leaves as its wire value", protocol)
                    .isEqualTo(protocol.wireValue());
            assertThat(LlmProtocolEnum.fromWire(protocol.wireValue())).isEqualTo(protocol);
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .as("a numeric code is never a wire value").isThrownBy(
                            () -> LlmProtocolEnum.fromWire(String.valueOf(protocol.getCode())));
            assertThat(protocol.getCode()).as("%s code must never be an ordinal", protocol)
                    .isNotEqualTo(protocol.ordinal());
            assertThat(protocol.getCode()).as("%s code stays on the declared tens interval", protocol)
                    .isEqualTo((protocol.ordinal() + 1) * 10);
        }
        assertThat(hasAnnotation(LlmProtocolEnum.class, "wireValue", JsonValue.class)).isTrue();
        assertThat(hasAnnotation(LlmProtocolEnum.class, "fromWire", JsonCreator.class)).isTrue();
    }

    @Test
    @DisplayName("路径判不出协议时按 Spec 回落 OpenAI 错误对象，状态与 no-store 逐字保留")
    void aPathThatCannotDecideAProtocolFallsBackToTheOpenAiErrorObject() {
        LlmGatewayProperties properties = gatewayProperties();
        Fixture fixture = fixture(properties, realFaces(properties));
        LlmInvocationException failure = new LlmInvocationException(413, "request_too_large", null,
                "The llm request exceeds yuheng.llm.max-request-bytes", false);

        ResponseEntity<ObjectNode> undecidable = fixture.controller.handleLlmInvocationException(failure,
                new MockHttpServletRequest("GET", "/v1/unrouted"), new MockHttpServletResponse());
        ResponseEntity<ObjectNode> onMessages = fixture.controller.handleLlmInvocationException(failure,
                new MockHttpServletRequest("POST", "/v1/messages"), new MockHttpServletResponse());

        assertThat(undecidable.getStatusCode().value()).isEqualTo(413);
        assertThat(fieldNames(undecidable.getBody())).containsExactly("error");
        assertThat(fieldNames(undecidable.getBody().path("error")))
                .containsExactlyInAnyOrder("message", "type", "param", "code");
        assertThat(undecidable.getBody().path("error").path("param").isNull()).isTrue();
        assertThat(undecidable.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(onMessages.getStatusCode().value()).isEqualTo(413);
        assertThat(fieldNames(onMessages.getBody())).containsExactly("type", "error");
    }

    @Test
    @DisplayName("OpenAI 三面错误 shape：唯一顶层 error、内层四键，param 恒在位可为 null 但绝不省略")
    void theOpenAiErrorShapeKeepsParamPresentEvenWhenTheFailureCarriesNoParameter() throws Exception {
        LlmGatewayProperties properties = gatewayProperties();
        Fixture fixture = fixture(properties, realFaces(properties));

        MvcResult refusedField = performEntry(fixture, "/v1/chat/completions",
                documentWith("api_key", CREDENTIAL_TEXT), SUBJECT);
        JsonNode refused = json(refusedField);

        assertThat(refusedField.getResponse().getStatus()).isEqualTo(400);
        assertThat(refusedField.getResponse().getContentType()).startsWith("application/json");
        assertThat(refusedField.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(fieldNames(refused)).containsExactly("error");
        assertThat(fieldNames(refused.path("error"))).containsExactlyInAnyOrder("message", "type", "param", "code");
        assertThat(refused.path("error").path("param").asText()).isEqualTo("api_key");
        assertThat(refused.path("error").path("code").asText()).isEqualTo(UNSUPPORTED_PARAMETER);
        assertThat(OPENAI_ERROR_TYPES).contains(refused.path("error").path("type").asText());
        assertThat(refusedField.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .as("the guard names the field but never its value").doesNotContain(CREDENTIAL_TEXT);

        MvcResult unauthenticated = performEntry(fixture, "/v1/responses", minimalEntryDocument(), null);
        JsonNode missingParam = json(unauthenticated);
        assertThat(unauthenticated.getResponse().getStatus()).as("no identity, no routing at all").isEqualTo(401);
        assertThat(missingParam.path("error").has("param")).as("param stays in place as JSON null").isTrue();
        assertThat(missingParam.path("error").path("param").isNull()).isTrue();
        assertThat(missingParam.path("error").path("code").asText()).isEqualTo("authentication_error");
        verifyNoInteractions(fixture.service);
    }

    @Test
    @DisplayName("Messages 面错误 shape 只有 type=error 与 error{type,message}，永无 code/param，也永不套 OpenAI 形状")
    void theAnthropicErrorShapeCarriesNoCodeAndNoParam() throws Exception {
        LlmGatewayProperties properties = gatewayProperties();
        Fixture fixture = fixture(properties, realFaces(properties));

        MvcResult result = performEntry(fixture, "/v1/messages", documentWith("credential", CREDENTIAL_TEXT),
                SUBJECT);
        String raw = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode body = json(result);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(fieldNames(body)).containsExactly("type", "error");
        assertThat(body.path("type").asText()).isEqualTo("error");
        assertThat(fieldNames(body.path("error"))).containsExactlyInAnyOrder("type", "message");
        assertThat(ANTHROPIC_ERROR_TYPES).contains(body.path("error").path("type").asText());
        assertThat(body.path("error").path("message").asText()).isNotBlank();
        assertThat(raw).as("the Messages face never borrows the OpenAI error object")
                .doesNotContain("\"param\"", "\"code\"", "choices", "usage", CREDENTIAL_TEXT);
    }

    @Test
    @DisplayName("运行时错误 fixtures：每个 Spec 状态在三个 OpenAI 面上都保住自己的机器码，且无 admin 信封")
    void everyRepresentativeStatusKeepsItsSpecMachineCodeOnAllThreeOpenAiFaces() {
        Map<Integer, String> nativeCodes = LlmApiOpenApiContractTest.<Map<Integer, String>>controllerStatic(
                "nativeCodesByStatus");
        Map<String, String> routingCodes = LlmApiOpenApiContractTest.<Map<String, String>>controllerStatic(
                "routingErrorCodes");
        Set<Integer> retryableStatuses = LlmApiOpenApiContractTest.<Set<Integer>>controllerStatic(
                "RETRYABLE_STATUSES");
        List<LlmProtocolStrategy> faces = List.of(chatFace(), embeddingsFace(), responsesFace());

        assertThat(nativeCodes.keySet()).containsExactlyInAnyOrder(400, 401, 403, 404, 413, 422, 429, 502, 503, 504);
        assertThat(routingCodes.values()).isSubsetOf(new LinkedHashSet<>(nativeCodes.values()));
        assertThat(retryableStatuses).containsExactlyInAnyOrder(429, 503);
        assertThat(retryableStatuses).isSubsetOf(nativeCodes.keySet());
        assertThat(nativeCodes.entrySet().stream().filter(entry -> retryableStatuses.contains(entry.getKey()))
                .map(Map.Entry::getValue).toList())
                .as("the retryable statuses carry exactly the two Spec backoff codes")
                .containsExactlyInAnyOrder("rate_limit_exceeded", "model_unavailable");
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> nativeCodes.put(418, "invented_code"));

        for (LlmProtocolStrategy face : faces) {
            for (Map.Entry<Integer, String> status : nativeCodes.entrySet()) {
                LlmInvocationException carrier = new LlmInvocationException(status.getKey(), status.getValue(),
                        "model", "A stable safe description for the contract",
                        retryableStatuses.contains(status.getKey()));

                JsonNode error = face.encodeError(carrier);

                assertThat(fieldNames(error)).as("top level of %s / %s", face.protocol(), status.getKey())
                        .containsExactly("error");
                assertThat(fieldNames(error.path("error")))
                        .as("inner keys of %s / %s", face.protocol(), status.getKey())
                        .containsExactlyInAnyOrder("message", "type", "param", "code");
                assertThat(error.path("error").path("code").asText()).isEqualTo(status.getValue());
                assertThat(error.path("error").path("param").asText()).isEqualTo("model");
                assertThat(OPENAI_ERROR_TYPES).contains(error.path("error").path("type").asText());
                assertThat(error.path("error").path("message").asText()).isNotBlank();
                assertThat(write(error)).as("no admin code/data envelope on %s", face.protocol())
                        .doesNotContain("\"traceId\"", "\"timestamp\"", "\"success\"", "\"data\"", BASE_URL,
                                SECRET_REF, CREDENTIAL_TEXT, UPSTREAM_MODEL);
            }
        }
    }

    @Test
    @DisplayName("路由失败按入口协议原生出网：403/422 状态与机器码逐字保留，两面形状仍各自独立")
    void routingFailuresLeaveAsNativeProtocolErrorsAndNeverAsTheAdminEnvelope() throws Exception {
        LlmGatewayProperties properties = gatewayProperties();
        Fixture openAi = fixture(properties, realFaces(properties));
        when(openAi.service.invoke(any())).thenThrow(new CommonException(403, "LLM_MODEL_NOT_AUTHORIZED",
                "The subject is not authorized for this model alias"));

        MvcResult responses = performEntry(openAi, "/v1/responses", minimalEntryDocument(), SUBJECT);
        JsonNode responsesBody = json(responses);
        assertThat(responses.getResponse().getStatus()).isEqualTo(403);
        assertThat(fieldNames(responsesBody)).containsExactly("error");
        assertThat(responsesBody.path("error").path("code").asText()).isEqualTo("model_forbidden");
        assertThat(responsesBody.path("error").path("type").asText()).isEqualTo("permission_error");
        assertThat(responsesBody.path("error").path("message").asText())
                .isEqualTo("The subject is not authorized for this model alias");

        Fixture messages = fixture(properties, realFaces(properties));
        when(messages.service.invoke(any())).thenThrow(new CommonException(403, "LLM_MODEL_NOT_AUTHORIZED",
                "The subject is not authorized for this model alias"));
        MvcResult onMessages = performEntry(messages, "/v1/messages", minimalEntryDocument(), SUBJECT);
        String raw = onMessages.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(onMessages.getResponse().getStatus()).isEqualTo(403);
        assertThat(raw).contains("\"type\":\"error\"").doesNotContain("\"code\"", "\"param\"");

        Fixture embeddings = fixture(properties, realFaces(properties));
        when(embeddings.service.invoke(any())).thenThrow(new CommonException(422,
                "LLM_EMBEDDING_CLOUD_CONFIGURED", "An embedding alias may only bind a local channel"));
        MvcResult cloudEmbedding = performEntry(embeddings, "/v1/embeddings", minimalEntryDocument(), SUBJECT);
        assertThat(cloudEmbedding.getResponse().getStatus()).as("the 422 status survives verbatim").isEqualTo(422);
        assertThat(json(cloudEmbedding).path("error").path("code").asText()).isEqualTo(UNSUPPORTED_PARAMETER);
    }

    @Test
    @DisplayName("只有可重试状态才会消耗第二条候选 route，429/503 的机器码仍按库存原生出网")
    void onlyARetryableFailureSpendsASecondCandidateRoute() throws Exception {
        LlmGatewayProperties properties = gatewayProperties().setMaximumAttempts(2);
        Map<LlmProtocolEnum, LlmProtocolStrategy> faces = realFaces(properties);
        LlmProtocolStrategy realChat = faces.get(LlmProtocolEnum.OPENAI_CHAT);
        LlmProtocolStrategy chat = mock(LlmProtocolStrategy.class);
        stubChatFace(chat, realChat);
        faces.put(LlmProtocolEnum.OPENAI_CHAT, chat);
        Fixture fixture = fixture(properties, faces);

        when(fixture.service.invoke(any())).thenReturn(routedWith(2));
        when(chat.exchange(any(), any(), any())).thenThrow(new LlmInvocationException(429, "rate_limit_exceeded",
                null, "The engine is saturated; retry this request later", true));
        MvcResult saturated = performEntry(fixture, "/v1/chat/completions", minimalEntryDocument(), SUBJECT);
        assertThat(saturated.getResponse().getStatus()).isEqualTo(429);
        assertThat(json(saturated).path("error").path("code").asText()).isEqualTo("rate_limit_exceeded");
        verify(chat, times(2)).exchange(any(), any(), any());

        reset(chat);
        stubChatFace(chat, realChat);
        when(chat.exchange(any(), any(), any())).thenThrow(new LlmInvocationException(429, "rate_limit_exceeded",
                null, "The engine is saturated; retry this request later", false));
        MvcResult refused = performEntry(fixture, "/v1/chat/completions", minimalEntryDocument(), SUBJECT);
        assertThat(refused.getResponse().getStatus()).isEqualTo(429);
        verify(chat, times(1)).exchange(any(), any(), any());

        reset(chat);
        stubChatFace(chat, realChat);
        when(fixture.service.invoke(any())).thenReturn(routedWith(1));
        when(chat.exchange(any(), any(), any())).thenThrow(new LlmInvocationException(503, "model_unavailable",
                null, "The routed llm channel is not serving this alias", true));
        MvcResult singleCandidate = performEntry(fixture, "/v1/chat/completions", minimalEntryDocument(), SUBJECT);
        assertThat(singleCandidate.getResponse().getStatus()).isEqualTo(503);
        assertThat(json(singleCandidate).path("error").path("code").asText()).isEqualTo("model_unavailable");
        verify(chat, times(1)).exchange(any(), any(), any());
    }

    @Test
    @DisplayName("FORBIDDEN_ENTRY_FIELDS 在路由前全量拒绝自证出网/凭据字段，且绝不回显字段值")
    void theForbiddenEntryFieldGuardRefusesEveryCredentialBearingFieldBeforeRouting() throws Exception {
        Set<String> guard = LlmApiOpenApiContractTest.<Set<String>>controllerStatic("FORBIDDEN_ENTRY_FIELDS");
        LlmGatewayProperties properties = gatewayProperties();
        Fixture fixture = fixture(properties, realFaces(properties));

        assertThat(guard).contains("upstream_url", "upstreammodel", "base_url", "baseurl", "endpoint", "credential",
                "credentials", "secret", "secret_ref", "secretref", "host", "authorization", "api_key", "apikey",
                "x_api_key", "xapikey");
        for (String field : guard) {
            for (String spelling : List.of(field, field.toUpperCase(Locale.ROOT))) {
                MvcResult result = performEntry(fixture, "/v1/chat/completions",
                        documentWith(spelling, CREDENTIAL_TEXT), SUBJECT);
                JsonNode body = json(result);

                assertThat(result.getResponse().getStatus()).as("status for %s", spelling).isEqualTo(400);
                assertThat(body.path("error").path("code").asText()).as("code for %s", spelling)
                        .isEqualTo(UNSUPPORTED_PARAMETER);
                assertThat(body.path("error").path("param").asText()).as("param for %s", spelling)
                        .isEqualToIgnoringCase(spelling);
                assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                        .as("no echo of the submitted value of %s", spelling).doesNotContain(CREDENTIAL_TEXT);
            }
        }
        verifyNoInteractions(fixture.service);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> guard.add("model"));
    }

    @Test
    @DisplayName("超过 yuheng.llm.max-request-bytes 的请求在任何路由之前就是原生 413，两面形状各归各")
    void anOversizedRequestFailsClosedAsANativeFourHundredThirteenBeforeRouting() throws Exception {
        LlmGatewayProperties tiny = gatewayProperties().setMaxRequestBytes(16L);
        Fixture fixture = fixture(tiny, realFaces(tiny));

        MvcResult openAi = performEntry(fixture, "/v1/chat/completions", minimalEntryDocument(), SUBJECT);
        MvcResult anthropic = performEntry(fixture, "/v1/messages", minimalEntryDocument(), SUBJECT);

        assertThat(openAi.getResponse().getStatus()).isEqualTo(413);
        assertThat(json(openAi).path("error").path("code").asText()).isEqualTo("request_too_large");
        assertThat(json(openAi).path("error").path("param").isNull()).isTrue();
        assertThat(anthropic.getResponse().getStatus()).isEqualTo(413);
        assertThat(fieldNames(json(anthropic))).containsExactly("type", "error");
        assertThat(json(anthropic).path("error").path("type").asText()).isEqualTo("request_too_large");
        verifyNoInteractions(fixture.service);
    }

    @Test
    @DisplayName("目录面只投影被授权且启用的 alias，任何 baseUrl/secretRef/凭据材料都不进响应字节")
    void theCatalogueFaceProjectsOnlyAuthorizedAliasesAndNoCredentialMaterial() throws Exception {
        LlmGatewayProperties properties = gatewayProperties();
        Fixture fixture = fixture(properties, realFaces(properties));
        when(fixture.repository.findCatalog()).thenReturn(Arrays.asList(
                catalogAlias("b-chat", Boolean.TRUE, List.of(SUBJECT)),
                catalogAlias("a-chat", Boolean.TRUE, List.of(SUBJECT)),
                null,
                catalogAlias("c-chat", Boolean.FALSE, List.of(SUBJECT)),
                catalogAlias("d-chat", Boolean.TRUE, List.of("svc:someone-else"))));

        MvcResult result = fixture.mockMvc.perform(get("/v1/models").principal(namedSubject(SUBJECT))).andReturn();
        String raw = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode document = JSON.readTree(raw);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(fieldNames(document)).containsExactly("object", "data");
        assertThat(document.path("object").asText()).isEqualTo("list");
        assertThat(document.path("data")).as("only the enabled aliases this subject is authorized for").hasSize(2);
        for (JsonNode entry : document.path("data")) {
            assertThat(fieldNames(entry)).containsExactlyInAnyOrder("id", "object", "created", "owned_by");
            assertThat(entry.path("object").asText()).isEqualTo("model");
            assertThat(entry.path("owned_by").asText()).isEqualTo("yuheng");
            assertThat(entry.path("created").asLong()).isGreaterThan(1_700_000_000L);
            assertThat(entry.path("id").asText()).isIn("a-chat", "b-chat");
        }
        assertThat(document.path("data").get(0).path("id").asText()).as("the projection is alias-ascending")
                .isEqualTo("a-chat");
        assertThat(raw).as("no resolved credential, address or reference ever leaves the catalogue")
                .doesNotContain(BASE_URL, "local-llm.internal", SECRET_REF, "LLM_LOCAL_A_SECRET", CREDENTIAL_TEXT,
                        UPSTREAM_MODEL, "sk-", "secretRef", "baseUrl", "apiKey", "authorization");
        verify(fixture.repository).findCatalog();
        verifyNoInteractions(fixture.service);
    }

    // ------------------------------------------------------------------ standalone harness

    /**
     * 中文说明：用生产装配代码把四个同协议 Strategy 与写出组件真实接起来，再交给 standalone MockMvc；
     * 唯一被 mock 的是路由端口与受管配置只读端口，因为本仓库不启动容器也不连库。
     * English summary: Wires the four same-protocol Strategies and the write-out component through the production configuration class and hands them to standalone MockMvc; only the routing port and the read-only configuration port are mocked, because this repository boots no container and reaches no database.
     * @param properties 参数 本模块属性对象；parameter this module's properties object.
     * @param faces 参数 协议到 Strategy 的映射；parameter the protocol to Strategy map.
     * @return 返回 已自检完成的控制器与可观察协作者；returns the self-checked controller with its observable collaborators.
     */
    private static Fixture fixture(LlmGatewayProperties properties,
            Map<LlmProtocolEnum, LlmProtocolStrategy> faces) {
        LlmGatewayConfiguration wiring = new LlmGatewayConfiguration(properties, JSON);
        Map<LlmProtocolEnum, String> registry = wiring.llmProtocolStrategyRegistry();
        Map<String, LlmProtocolStrategy> byName = new LinkedHashMap<>();
        registry.forEach((protocol, beanName) -> byName.put(beanName, faces.get(protocol)));
        assertThat(byName.keySet()).as("the production registry keys one distinct bean per protocol").hasSize(4);
        LlmInvocationService service = mock(LlmInvocationService.class);
        LlmConfigurationRepository repository = mock(LlmConfigurationRepository.class);
        LlmServletStreamComponent writer =
                new LlmServletStreamComponent(properties, wiring.llmStreamingExecutor(), JSON);
        LlmApiController controller = new LlmApiController(service, properties, JSON, registry, byName, writer,
                repository);
        controller.checkRegistry();
        return new Fixture(controller, service, repository, MockMvcBuilders.standaloneSetup(controller).build());
    }

    private static Map<LlmProtocolEnum, LlmProtocolStrategy> realFaces(LlmGatewayProperties properties) {
        Map<LlmProtocolEnum, LlmProtocolStrategy> faces = new EnumMap<>(LlmProtocolEnum.class);
        faces.put(LlmProtocolEnum.OPENAI_CHAT, new OpenAiChatProtocolStrategy(properties, JSON));
        faces.put(LlmProtocolEnum.OPENAI_EMBEDDING, new OpenAiEmbeddingProtocolStrategy(properties, JSON));
        faces.put(LlmProtocolEnum.OPENAI_RESPONSES, new OpenAiResponsesProtocolStrategy(properties, JSON));
        faces.put(LlmProtocolEnum.ANTHROPIC_MESSAGES, new AnthropicMessagesProtocolStrategy(properties, JSON));
        return faces;
    }

    private static LlmProtocolStrategy chatFace() {
        return new OpenAiChatProtocolStrategy(gatewayProperties(), JSON);
    }

    private static LlmProtocolStrategy embeddingsFace() {
        return new OpenAiEmbeddingProtocolStrategy(gatewayProperties(), JSON);
    }

    private static LlmProtocolStrategy responsesFace() {
        return new OpenAiResponsesProtocolStrategy(gatewayProperties(), JSON);
    }

    /**
     * 中文说明：把协议闸门与原生错误编码交回真实 Chat 面，只让 {@code exchange} 留在 mock 上，
     * 这样重试次数可数、错误 shape 仍是生产代码写的。
     * English summary: Hands the protocol gate and the native error encoding back to the real Chat face so only {@code exchange} stays mocked, which keeps attempts countable while the error shape is still production code.
     * @param face 参数 mock 的 Chat 面；parameter the mocked Chat face.
     * @param realChat 参数 真实 Chat 面；parameter the real Chat face.
     */
    private static void stubChatFace(LlmProtocolStrategy face, LlmProtocolStrategy realChat) {
        when(face.protocol()).thenReturn(LlmProtocolEnum.OPENAI_CHAT);
        when(face.encodeError(any())).thenAnswer(invocation -> realChat.encodeError(invocation.getArgument(0)));
    }

    private static LlmGatewayProperties gatewayProperties() {
        return new LlmGatewayProperties()
                .setEnabled(true)
                .setAllowedLocalCidrs(List.of("local-llm.internal"))
                .setAllowedCloudHosts(List.of("api.cloud-llm.example"))
                .setSecretsRoot("/run/secrets/yuheng-llm")
                .setMaxRequestBytes(1_048_576L)
                .setMaxFrameBytes(65_536)
                .setMaximumAttempts(2)
                .setStreamingThreads(8)
                .setIdentity(new LlmGatewayProperties.Identity()
                        .setResourceUri("https://llm.yuheng.internal")
                        .setServiceTokenAudience("yuheng-llm"));
    }

    /**
     * 中文说明：路由已完成、只待协议面出字节的载体；候选 route 只为数重试次数而存在。
     * English summary: A routed carrier waiting only on the protocol face, whose candidate routes exist only so attempts can be counted.
     * @param candidates 参数 候选 route 条数；parameter how many candidate routes routing produced.
     * @return 返回 已路由结果；returns the routed result.
     */
    private static LlmInvocationResultVO routedWith(int candidates) {
        List<LlmModelSnapshotBO.RouteBO> routes = new ArrayList<>();
        for (int index = 0; index < candidates; index++) {
            routes.add(new LlmModelSnapshotBO.RouteBO()
                    .setChannelKey("local-" + index)
                    .setUpstreamModel(UPSTREAM_MODEL)
                    .setPriority(1)
                    .setWeight(100)
                    .setChannel(new LlmModelSnapshotBO.ChannelBO()
                            .setChannelKey("local-" + index)
                            .setDeployment(LlmDeploymentEnum.LOCAL)
                            .setProtocol(LlmProtocolEnum.OPENAI_CHAT)
                            .setBaseUrl(BASE_URL)
                            .setSecretRef(SECRET_REF)
                            .setEnabled(Boolean.TRUE)
                            .setConnectTimeoutMs(2_000)
                            .setHeaderTimeoutMs(30_000)
                            .setIdleTimeoutMs(60_000)
                            .setTotalTimeoutMs(120_000)
                            .setMaxConcurrent(4)));
        }
        return new LlmInvocationResultVO().setStatus(200).setCandidates(routes);
    }

    /**
     * 中文说明：一条受管目录 alias，其 route 里带着真实的 baseUrl 与 secretRef，用来证明目录投影不会把它们带出网。
     * English summary: A managed catalogue alias whose route carries a real base URL and secret reference, used to prove the projection never carries them out.
     * @param modelKey 参数 alias；parameter the alias.
     * @param enabled 参数 启停位；parameter the enablement flag.
     * @param allowedSubjects 参数 授权主体清单；parameter the authorized subject list.
     * @return 返回 目录条目；returns the catalogue entry.
     */
    private static LlmModelSnapshotBO catalogAlias(String modelKey, boolean enabled, List<String> allowedSubjects) {
        return new LlmModelSnapshotBO()
                .setModelKey(modelKey)
                .setName("上游真机 alias")
                .setKind(LlmModelKindEnum.CHAT)
                .setEnabled(enabled)
                .setProtocols(List.of(LlmProtocolEnum.OPENAI_CHAT))
                .setAllowedSubjects(allowedSubjects)
                .setRevision(7L)
                .setRoutes(List.of(routedWith(1).getCandidates().get(0)));
    }

    private static MvcResult performEntry(Fixture fixture, String path, String document, String subject)
            throws Exception {
        MockHttpServletRequestBuilder request = post(path).contentType(MediaType.APPLICATION_JSON).content(document);
        if (subject != null) {
            request.principal(namedSubject(subject));
        }
        return fixture.mockMvc.perform(request).andReturn();
    }

    private static Principal namedSubject(String name) {
        return () -> name;
    }

    private static String minimalEntryDocument() {
        return write(JSON.createObjectNode().put("model", CHAT_ALIAS));
    }

    private static String documentWith(String field, String value) {
        return write(JSON.createObjectNode().put("model", CHAT_ALIAS).put(field, value));
    }

    /**
     * 中文说明：把一次响应的字节按容器等价 Jackson 读回；读不回 JSON 本身就是合同被破坏。
     * English summary: Reads one response's bytes back through the container-equivalent Jackson; failing to read JSON back is itself a broken contract.
     * @param result 参数 一次 MockMvc 结果；parameter one MockMvc outcome.
     * @return 返回 响应文档；returns the response document.
     */
    private static JsonNode json(MvcResult result) throws IOException {
        try {
            return JSON.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("The model entry answered with something that is not JSON", failure);
        }
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        if (node != null) {
            node.fieldNames().forEachRemaining(names::add);
        }
        return names;
    }

    private static Map<String, String> annotatedEntries() {
        Map<String, String> entries = new TreeMap<>();
        for (Method method : LlmApiController.class.getDeclaredMethods()) {
            GetMapping get = AnnotatedElementUtils.findMergedAnnotation(method, GetMapping.class);
            PostMapping post = AnnotatedElementUtils.findMergedAnnotation(method, PostMapping.class);
            if (get != null && post != null) {
                throw new IllegalStateException("One handler cannot be both a GET and a POST entry: " + method);
            }
            if (get != null) {
                entries.put("GET " + solePath(get.value()), method.getName());
            }
            if (post != null) {
                entries.put("POST " + solePath(post.value()), method.getName());
            }
        }
        return entries;
    }

    /**
     * 中文说明：按 operationId 取回处理器方法，取不到就说明合同入口被改名或被删。
     * English summary: Takes the handler back by operationId; a miss means a contract entry was renamed or removed.
     * @param entry 参数 入口合同；parameter the entry contract.
     * @return 返回 处理器方法；returns the handler method.
     */
    private static Method handlerFor(EntryContract entry) {
        for (Method method : LlmApiController.class.getDeclaredMethods()) {
            if (method.getName().equals(entry.operationId())) {
                return method;
            }
        }
        throw new IllegalStateException("The model entry handler " + entry.operationId() + " is gone");
    }

    private static String solePath(String[] paths) {
        if (paths.length != 1) {
            throw new IllegalStateException("A contract entry maps exactly one path: " + Arrays.toString(paths));
        }
        return paths[0];
    }

    /**
     * 中文说明：反射读取控制器私有静态成员（字段优先，其次无参静态方法）的唯一入口，避免为测试放开生产可见性。
     * English summary: The single reflective seam for the controller's private static members (field first, then a no-argument static method), so no production visibility is widened for this test.
     * @param name 参数 字段名或无参静态方法名；parameter the field or no-argument static method name.
     * @param <T> 参数 期望的成员类型；parameter the expected member type.
     * @return 返回 该成员的当前值；returns the member's current value.
     */
    @SuppressWarnings("unchecked")
    private static <T> T controllerStatic(String name) {
        try {
            Field field = LlmApiController.class.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(null);
        } catch (NoSuchFieldException absent) {
            try {
                Method method = LlmApiController.class.getDeclaredMethod(name);
                method.setAccessible(true);
                return (T) method.invoke(null);
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException("The controller member " + name + " is unreachable", failure);
            }
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("The controller member " + name + " is unreadable", failure);
        }
    }

    /**
     * 中文说明：证明「五个入口」在源码侧也成立：每条端点常量的字面量必须逐字不变，且 {@code src/main/java} 下
     * 不再出现任何其它请求映射注解。
     * English summary: Proves the five entries hold on the source side too: every endpoint constant must keep its literal verbatim and no other request mapping may appear anywhere under {@code src/main/java}.
     * @return 返回 按合同排序的「VERB path」清单；returns the contract-ordered {@code VERB path} list.
     */
    private static List<String> sourceMappedRoutes() {
        String source = readControllerSource();
        List<String> routes = new ArrayList<>();
        for (EntryContract entry : ENTRIES) {
            assertThat(source).as("the mapping line of %s", entry.path()).contains(entry.annotation());
            Matcher constant = Pattern.compile("String\\s+" + entry.pathConstant()
                    + "\\s*=\\s*\"([^\"]+)\"").matcher(source);
            assertThat(constant.find()).as("the endpoint constant %s", entry.pathConstant()).isTrue();
            assertThat(constant.group(1)).as("the literal behind %s", entry.pathConstant()).isEqualTo(entry.path());
            routes.add(entry.route());
        }
        Pattern mapping = Pattern.compile("^@(Get|Post|Put|Delete|Patch)Mapping\\(");
        List<String> unexpected = new ArrayList<>();
        for (Map.Entry<Path, List<String>> file : mainSources()) {
            for (String line : file.getValue()) {
                String stripped = line.strip();
                if (mapping.matcher(stripped).find()
                        && ENTRIES.stream().noneMatch(entry -> entry.annotation().equals(stripped))) {
                    unexpected.add(file.getKey() + " " + stripped);
                }
            }
        }
        assertThat(unexpected).as("no request mapping exists beyond the five contract entries").isEmpty();
        return routes.stream().sorted().toList();
    }

    /**
     * 中文说明：本模块源码里任何提到 admin 前缀的行，必须恒为空：模型入口永不挂到管理面之下。
     * English summary: Every line in this module mentioning the admin prefix, which must stay empty: the model entry is never mounted below the management face.
     * @return 返回 违规行清单；returns the offending lines.
     */
    private static List<String> adminPrefixedMappingLines() {
        List<String> offenders = new ArrayList<>();
        for (Map.Entry<Path, List<String>> file : mainSources()) {
            for (String line : file.getValue()) {
                if (line.contains(ADMIN_PREFIX)) {
                    offenders.add(file.getKey() + " " + line.strip());
                }
            }
        }
        return offenders;
    }

    private static List<Map.Entry<Path, List<String>>> mainSources() {
        List<Map.Entry<Path, List<String>>> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(moduleRoot().resolve("src/main/java"))) {
            for (Path path : paths.filter(candidate -> candidate.getFileName().toString().endsWith(".java"))
                    .toList()) {
                files.add(Map.entry(path, Files.readAllLines(path, StandardCharsets.UTF_8)));
            }
        } catch (IOException failure) {
            throw new IllegalStateException("The gateway sources could not be inventoried", failure);
        }
        return files;
    }

    private static List<String> swaggerAnnotatedHandlers(Class<?> type) {
        List<String> handlers = new ArrayList<>();
        for (Method method : type.getDeclaredMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                if (annotation.annotationType().getName().startsWith("io.swagger")) {
                    handlers.add(method.getName() + " " + annotation);
                }
            }
        }
        return handlers;
    }

    private static boolean hasAnnotation(Class<?> type, String methodName,
            Class<? extends Annotation> annotation) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 中文说明：取映射注解上方那段 javadoc 的文本，operationId 与 API 编号就登记在那里。
     * English summary: Takes the javadoc block above a mapping annotation, which is where the operationId and the API id are recorded.
     * @param source 参数 控制器源码全文；parameter the whole controller source.
     * @param annotation 参数 映射注解原文；parameter the mapping annotation text.
     * @return 返回 该注解紧邻的上一段 javadoc；returns the javadoc block immediately above that annotation.
     */
    private static String javadocAbove(String source, String annotation) {
        int annotationStart = source.indexOf(annotation);
        if (annotationStart < 0) {
            throw new IllegalStateException("The mapping " + annotation + " is not in the source");
        }
        int docEnd = source.lastIndexOf("*/", annotationStart);
        int docStart = source.lastIndexOf("/**", docEnd);
        if (docStart < 0 || docEnd < 0) {
            throw new IllegalStateException("No javadoc precedes " + annotation);
        }
        return source.substring(docStart, docEnd);
    }

    private static String readControllerSource() {
        try {
            return Files.readString(moduleRoot().resolve(CONTROLLER_SOURCE), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException("The controller source could not be read", failure);
        }
    }

    /** 中文说明：从 surefire 工作目录向上找到本模块根，绝不依赖任何绝对路径常量。 English summary: Walks up from surefire's working directory to this module's root, so no absolute path constant is ever needed. */
    private static Path moduleRoot() {
        Path directory = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 6 && directory != null; depth++) {
            if (Files.isRegularFile(directory.resolve(CONTROLLER_SOURCE))) {
                return directory;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("The yuheng-llm-gateway sources are not reachable from the test directory");
    }

    private static String write(JsonNode node) {
        try {
            return JSON.writeValueAsString(node);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Unserialisable fixture", failure);
        }
    }

    /** 中文说明：一个入口合同三元组（API 编号、方法、路径、operationId 与承载它的路径常量名）。 English summary: One entry contract triple, plus the endpoint constant it is written from. */
    private record EntryContract(String apiId, String verb, String path, String operationId, String pathConstant) {

        private String route() {
            return verb + " " + path;
        }

        private String annotation() {
            return ("GET".equals(verb) ? "@GetMapping(" : "@PostMapping(") + pathConstant + ")";
        }
    }

    /** 中文说明：standalone 装配出来的控制器与其两个可 mock 端口，绝不含任何容器 Bean。 English summary: The standalone controller plus the only two ports this test may mock, with no container bean in sight. */
    private static final class Fixture {

        private final LlmApiController controller;

        private final LlmInvocationService service;

        private final LlmConfigurationRepository repository;

        private final MockMvc mockMvc;

        private Fixture(LlmApiController controller, LlmInvocationService service,
                LlmConfigurationRepository repository, MockMvc mockMvc) {
            this.controller = controller;
            this.service = service;
            this.repository = repository;
            this.mockMvc = mockMvc;
        }
    }
}
