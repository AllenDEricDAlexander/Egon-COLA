package top.egon.cola.component.yuheng.llm.proxy.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yuheng.llm.config.LlmGatewayProperties;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.dto.LlmInvocationCommandDTO;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmCapabilityEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.exception.LlmInvocationException;
import top.egon.cola.component.yuheng.llm.proxy.domain.vo.LlmInvocationResultVO;
import top.egon.cola.component.yuheng.llm.proxy.repository.LlmConfigurationRepository;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmInvocationService;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmProtocolStrategy;
import top.egon.cola.component.yuheng.llm.proxy.service.LlmServletStreamComponent;

/**
 * 中文说明：{@code LlmApiController} 是原业务 Spec §9.1/§9.2 里 owner=llm-gateway 的<b>模型入口</b>控制器，只交付
 * API-001/002/003/029/030 这五条既有契约的路径、方法、operationId 与<b>原生</b> JSON/SSE shape，因此它是 engine 里
 * 唯一面向客户端的 web 层，也是「四种模型协议不得都被降格成 Chat」这条合同的落地点：
 * {@code POST /v1/chat/completions}={@code createLlmChatCompletion}（OPENAI_CHAT）、
 * {@code POST /v1/embeddings}={@code createLlmEmbeddings}（OPENAI_EMBEDDING）、
 * {@code GET /v1/models}={@code listLlmModels}（OpenAI 目录面）、
 * {@code POST /v1/responses}={@code createLlmResponse}（OPENAI_RESPONSES）、
 * {@code POST /v1/messages}={@code createLlmMessage}（ANTHROPIC_MESSAGES）。协议<b>只</b>由端点常量决定，绝不去
 * 猜 body，也绝不存在 {@code if (protocol == ...)} 式的编解码分支：入口把每条端点固定映射到一个
 * {@link LlmProtocolEnum}，再按 {@code llmProtocolStrategyRegistry} 的具名注册表在 {@link #checkRegistry()} 里建成
 * 只读 EnumMap（缺项、重复或 bean 自报协议与注册键不符即启动失败，<b>永不回落 Chat</b>），随后每个请求都走同一条
 * 编排：① 在路由<b>之前</b>按 {@code yuheng.llm.max-request-bytes} 有界读正文（超限 413，声明长度与实读字节双重校验，
 * 因此永不把整条无界流攒进内存）；② 只验形态——必须是 JSON 对象、必须带 alias 形状的 {@code model}、{@code stream}
 * 必须是布尔（缺省 false、显式 null 拒绝）、任何 {@code upstream_url}/{@code credential}/{@code host}/
 * {@code base_url}/{@code authorization} 之类自证出网或凭据字段一律 400；③ 用 Servlet 已认证主体取可信
 * {@code callerSubject}（缺失即 401，本模块不使用任何 Spring Security 类型）；④ 构造
 * {@link LlmInvocationCommandDTO} 并由端口上的 {@code @Valid} 复核 jakarta 约束，{@link LlmInvocationService#invoke}
 * 完成授权与路由，首条候选 route 交给同协议 Strategy 的 {@code exchange} 出网；⑤ 成功或上游如实回的状态、白名单
 * 安全头与 body 全部由 {@link LlmServletStreamComponent} 出字节（流式 {@code writeStream} 交回该协议的
 * {@code encodeError} 作为提交前编码器，非流式先物化单文档再 {@code writeUnary}），本类不写任何一字节协议内容。
 * 失败语义收在同一处：{@link LlmInvocationException} 与路由 {@link CommonException}（403/404/422/503 等，其
 * {@code getCode()} 即原生状态）都在本类的 {@code @ExceptionHandler} 里换成<b>该协议原生</b>错误对象——按入口路径
 * 选 Strategy 再调 {@code encodeError}，路径无法判定时按 Spec 回落 OpenAI 错误对象——状态逐字保留，
 * 既不使用 admin 的 {@code code}/{@code data} 信封，也绝不把失败伪装成 200 空成功；{@code @Valid} 在端口边界
 * 违约同样按 400 原生出网，避免容器默认错误页破坏协议 shape。日志与错误只带 alias、协议、候选数、channelKey、
 * 状态与延迟分桶这些低基数标识，永不记 prompt、工具参数、向量、token、密钥、baseUrl、upstreamModel 或请求正文。
 * {@code GET /v1/models} 是唯一不路由的端点：它按 Spec §9.2.3 只做目录投影（当前身份有权且 enabled 的 alias、
 * 按 id 升序、最多 1000、固定 {@code owned_by:"yuheng"}），不做上游动态发现，也不返回任何地址或凭据。
 * English summary: {@code LlmApiController} is the model-entry controller of the primary Spec §9.1/§9.2 for
 * owner=llm-gateway and delivers exactly the paths, methods, operationIds and <b>native</b> JSON/SSE shapes of
 * API-001/002/003/029/030: {@code POST /v1/chat/completions}={@code createLlmChatCompletion} (OPENAI_CHAT),
 * {@code POST /v1/embeddings}={@code createLlmEmbeddings} (OPENAI_EMBEDDING), {@code GET /v1/models}=
 * {@code listLlmModels} (the OpenAI catalog face), {@code POST /v1/responses}={@code createLlmResponse}
 * (OPENAI_RESPONSES) and {@code POST /v1/messages}={@code createLlmMessage} (ANTHROPIC_MESSAGES). The protocol comes
 * <b>only</b> from an endpoint constant, never from body sniffing, and there is no {@code if (protocol == ...)} codec
 * branch anywhere: each entry maps to one {@link LlmProtocolEnum}, the named {@code llmProtocolStrategyRegistry} is
 * turned into a read-only EnumMap by {@link #checkRegistry()} at startup (a missing entry, a duplicate bean or a bean
 * whose declared protocol disagrees with its registry key fails startup and it <b>never</b> falls back to Chat), and
 * every request then runs one orchestration: (1) the body is read with a ceiling <b>before</b> routing under
 * {@code yuheng.llm.max-request-bytes} (an oversized request is a 413, checked both against the declared length and
 * the bytes actually read, so an unbounded stream is never buffered); (2) only its shape is verified — a JSON object,
 * an alias-shaped {@code model}, a boolean {@code stream} (absent false, explicit null rejected) and no
 * {@code upstream_url}/{@code credential}/{@code host}/{@code base_url}/{@code authorization} style self-asserted
 * egress or credential field, each a 400; (3) the trusted {@code callerSubject} is taken from the authenticated
 * servlet principal (absent is a 401, and no Spring Security type is used anywhere in this module); (4) a
 * {@link LlmInvocationCommandDTO} is built and its jakarta constraints are re-checked by the {@code @Valid} declared on
 * the port, {@link LlmInvocationService#invoke} authorizes and routes, and the head candidate route is handed to the
 * same-protocol Strategy's {@code exchange}; (5) the status, whitelisted safe headers and body all leave through
 * {@link LlmServletStreamComponent} ({@code writeStream} for SSE with that protocol's {@code encodeError} as the
 * pre-commit encoder, {@code writeUnary} for a materialized single document), so this class writes no protocol byte at
 * all. Failure semantics converge in one place: {@link LlmInvocationException} and the routing
 * {@link CommonException} (403/404/422/503 …, whose {@code getCode()} <i>is</i> the native status) are turned inside
 * this class's {@code @ExceptionHandler} into that protocol's <b>native</b> error object by picking the Strategy from
 * the entry path and calling {@code encodeError}, defaulting to the OpenAI error object when the path cannot decide a
 * protocol, with the status preserved verbatim — never the admin {@code code}/{@code data} envelope and never a
 * fabricated 200 empty success — while a {@code @Valid} violation at the port boundary also leaves as a native 400 so
 * the container error page can never break the protocol shape. Logs and errors carry only the alias, protocol,
 * candidate count, channelKey, status and a latency bucket — never a prompt, tool arguments, vectors, tokens, secrets,
 * base URLs, upstream model names or the request payload. {@code GET /v1/models} is the only unrouted entry: it
 * projects the Spec §9.2.3 catalog (aliases the caller is authorized for and that are enabled, id-ascending, at most
 * 1000, fixed {@code owned_by:"yuheng"}) with no upstream discovery and no address or credential.
 *
 * 用法 / Usage: 由 Spring MVC 按上表五个端点暴露，请求体为该协议原生 JSON、响应为原生 JSON 或原生 SSE；
 * {@code stream:true} 时由 {@link LlmServletStreamComponent} 在有界流式线程池上逐帧写出并在提交前完成首帧屏障。
 * 本类不开事务、不碰持久层写、不解析密钥、不做健康探测；唯一的读是 {@code findSnapshot} 的路由复核与
 * {@code findCatalog} 的目录投影。五个方法用代码优先 OpenAPI 注解声明 operation、原生响应与 bearerAuth；
 * 由部署配置控制的文档发布默认关闭，启用时专属 MVC OpenAPI Starter 会保护文档路径。
 * / Exposed through the five mappings above: the request body is that protocol's native JSON and the response is
 * native JSON or native SSE; with {@code stream:true} the frames are written by {@link LlmServletStreamComponent} on
 * the bounded streaming executor behind a pre-commit first-frame barrier. This class opens no transaction, writes no
 * persistence, resolves no secret, probes nothing and touches no database beyond the routing snapshot re-read and the
 * catalog projection. The five methods declare code-first OpenAPI operations, native responses and {@code bearerAuth}.
 * Document publication is disabled by default and, when enabled by deployment configuration, the dedicated MVC OpenAPI
 * starter protects the documentation path.
 */
@Slf4j
@Validated
@RestController("llmApiController")
@RequiredArgsConstructor
public class LlmApiController {

    /** 中文说明：API-001 精确入口路径（Spec §9.1/§9.2.1，operationId={@code createLlmChatCompletion}）。 English summary: the exact API-001 entry path. */
    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    /** 中文说明：API-002 精确入口路径（Spec §9.1/§9.2.2，operationId={@code createLlmEmbeddings}）。 English summary: the exact API-002 entry path. */
    private static final String EMBEDDINGS_PATH = "/v1/embeddings";

    /** 中文说明：API-003 精确入口路径（Spec §9.1/§9.2.3，operationId={@code listLlmModels}）。 English summary: the exact API-003 entry path. */
    private static final String MODELS_PATH = "/v1/models";

    /** 中文说明：API-029 精确入口路径（Spec §9.1/§9.2.29，operationId={@code createLlmResponse}）。 English summary: the exact API-029 entry path. */
    private static final String RESPONSES_PATH = "/v1/responses";

    /** 中文说明：API-030 精确入口路径（Spec §9.1/§9.2.30，operationId={@code createLlmMessage}）。 English summary: the exact API-030 entry path. */
    private static final String MESSAGES_PATH = "/v1/messages";

    /** 中文说明：端点到协议的固定映射，同时是错误编码器的选择表：协议来自被调用的端点，绝不来自 body 字符串。 English summary: the fixed entry-to-protocol mapping, which doubles as the error encoder selection table: the protocol comes from the invoked entry point and never from a body string. */
    private static final Map<String, LlmProtocolEnum> ENTRY_PROTOCOLS = entryProtocols();

    /** 中文说明：路径无法判定协议时的默认面——OpenAI 错误对象（Spec §9.2.1 的 {@code error} shape）。 English summary: the face used when no path can decide a protocol, namely the OpenAI error object of Spec §9.2.1. */
    private static final LlmProtocolEnum DEFAULT_PROTOCOL = LlmProtocolEnum.OPENAI_CHAT;

    /** 中文说明：alias 形态与业务合同同一正则，先于端口校验，避免把违约交给容器默认错误页。 English summary: the same alias pattern as the business contract, applied before the port so a violation never reaches the container error page. */
    private static final String ALIAS_PATTERN = "^[a-z0-9][a-z0-9-]{0,63}$";

    /** 中文说明：{@code callerSubject} 的字节上界，与 DTO 的 {@code @Size(max = 128)} 同宽。 English summary: the subject ceiling, as wide as the DTO's {@code @Size(max = 128)}. */
    private static final int MAX_SUBJECT_LENGTH = 128;

    /** 中文说明：Spec §9.0.1 明列的自证出网/凭据字段（大小写不敏感），在路由前拒绝，协议 Strategy 再拒一次。 English summary: the self-asserted egress and credential fields named by Spec §9.0.1, refused before routing and refused again by the protocol Strategy. */
    private static final Set<String> FORBIDDEN_ENTRY_FIELDS = forbiddenEntryFields();

    /** 中文说明：Spec §9.2.3 目录的硬上界（最多 1000 条 alias）。 English summary: the Spec §9.2.3 catalog ceiling of at most 1000 aliases. */
    private static final int CATALOG_MAX_ENTRIES = 1000;

    /** 中文说明：目录投影的固定企业网关归属标识，不泄漏供应商账号（Spec §9.2.3 示例）。 English summary: the fixed gateway owner of the catalog projection, which leaks no vendor account (Spec §9.2.3 example). */
    private static final String CATALOG_OWNER = "yuheng";

    /** 中文说明：目录端点没有客户端 alias，用这个固定哨兵只作为日志与写出器的载体。 English summary: the catalog has no client alias, so this fixed sentinel only carries the log and write-out identity. */
    private static final String CATALOG_ALIAS = "models";

    /** 中文说明：路由 {@link CommonException#getStatus()} 名到 Spec 稳定机器码的映射，逐字取自 §9.2.1/§9.2.2/§9.2.3/§9.2.29/§9.2.30 的错误表。 English summary: the mapping from a routing CommonException status name to the Spec's stable machine codes, taken literally from the error tables of §9.2.1/§9.2.2/§9.2.3/§9.2.29/§9.2.30. */
    private static final Map<String, String> ROUTING_ERROR_CODES = routingErrorCodes();

    /** 中文说明：Spec 状态到原生机器码的兜底表（未列状态保留原生状态并用最保守的协议错误码）。 English summary: the fallback status-to-native-code table, where an unlisted status keeps its native status and gets the most conservative protocol error code. */
    private static final Map<Integer, String> NATIVE_CODES_BY_STATUS = nativeCodesByStatus();

    /** 中文说明：Spec 判定的「下一次请求可重试」状态：429 按 Retry-After 退避、503 有界重试。 English summary: the Spec's retryable-next-request statuses: 429 backs off per Retry-After and 503 retries with a bound. */
    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(429, 503);

    /** 中文说明：安全说明的长度上界，避免任何上游文本被整段带回客户端（超出即截断）。 English summary: the ceiling of a safe description, so no raw upstream text is ever carried back verbatim (it is abbreviated past this point). */
    private static final int MAX_SAFE_MESSAGE_LENGTH = 512;

    /** 中文说明：非流式响应文档的物化预算；单文档由协议面在同一次调用内已经产完，这里只是防止任何实现把网络等待带进 Servlet 线程。 English summary: the materialization budget for a non-stream response document; the protocol face has already produced the single document inside the same call, so this only keeps an implementation from carrying a network wait onto the servlet thread. */
    private static final Duration UNARY_BODY_BUDGET = Duration.ofSeconds(30);

    /** 中文说明：延迟分桶边界（毫秒），日志只记桶名，保证低基数。 English summary: the latency bucket boundaries in milliseconds, since only the bucket name is logged, which keeps the cardinality low. */
    private static final long[] LATENCY_BUCKETS_MILLIS = {1_000L, 5_000L, 30_000L, 120_000L};

    /** 中文说明：延迟分桶名，与 {@link #LATENCY_BUCKETS_MILLIS} 一一对应再加一个溢出桶。 English summary: the latency bucket names, one per {@link #LATENCY_BUCKETS_MILLIS} entry plus an overflow bucket. */
    private static final String[] LATENCY_BUCKETS = {"le1s", "le5s", "le30s", "le120s", "gt120s"};

    /**
     * 中文说明：模型调用唯一业务端口，承载授权与路由的全部准入语义；本类看不到任何 PO、DAO 或 MyBatis 类型。
     * English summary: the single model invocation port carrying every authorization and routing admission decision, so
     * this class sees no PO, DAO or MyBatis type.
     *
     * 用法 / Usage: 该字段通过本控制器的五个端点使用。/ Used through the five entry points of this controller.
     */
    @Qualifier("llmInvocationServiceImpl")
    private final LlmInvocationService llmInvocationService;

    /**
     * 中文说明：本进程的出网边界，这里只用 {@code maxRequestBytes} 这一把闸（请求体与单响应文档共用同一上限）。
     * English summary: this process's egress boundary, of which only the {@code maxRequestBytes} gate is used here, the
     * shared ceiling for a request body and for one response document.
     *
     * 用法 / Usage: 该字段在读取正文与物化单文档时收窄上限。/ Applied when reading the body and when materializing a unary document.
     */
    @Qualifier(LlmGatewayProperties.BEAN_NAME)
    private final LlmGatewayProperties gatewayProperties;

    /**
     * 中文说明：容器唯一的 Jackson 栈，用来解析与投影原生协议文档；禁止另起第二套 JSON 实现，也禁止把文档reshape成 VO。
     * English summary: the container's single Jackson stack, used to parse and project native protocol documents; no
     * second JSON implementation is allowed and no document is ever reshaped into a VO.
     *
     * 用法 / Usage: {@code readTree} 解析入口正文与目录投影 {@code ObjectNode}。/ {@code readTree} for the ingress body and {@code ObjectNode} construction for the catalog projection.
     */
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：四种协议的具名 Strategy 注册表（{@code 协议 → bean 名}），由装配方固定为四条不可增删的映射，是
     * 「一协议一适配器」与 Rule 9（无协议分支）的唯一事实来源。
     * English summary: the named Strategy registry of the four protocols ({@code protocol → bean name}), pinned by the
     * wiring side to a non-extensible four-entry mapping, which is the single source of truth for
     * one-protocol-one-adapter and for Rule 9's no-protocol-branch rule.
     *
     * 用法 / Usage: 仅在 {@link #checkRegistry()} 中被解析一次，之后按协议查表。/ Resolved once inside
     * {@link #checkRegistry()} and consulted by protocol afterwards.
     */
    @Qualifier("llmProtocolStrategyRegistry")
    private final Map<LlmProtocolEnum, String> llmProtocolStrategyRegistry;

    /**
     * 中文说明：全部协议 Strategy bean 的<b>按名</b>视图（Spring 对 {@code Map<String, T>} 的多元素注入）。
     * 这里刻意<b>不</b>加 {@code @Qualifier}：限定值会把多元素注入收窄成单个 bean，四个实现就必须靠这一个视图按注册表
     * 的名字取回，不新增任何 bean 类型。
     * English summary: the <b>by-name</b> view of every protocol Strategy bean, which is exactly what Spring's
     * multi-element injection of {@code Map<String, T>} produces. {@code @Qualifier} is deliberately absent here: a
     * qualifier value would narrow multi-element injection down to a single bean, so this one view is what lets the
     * four implementations be taken back by registry name without inventing any new bean type.
     *
     * 用法 / Usage: 仅在 {@link #checkRegistry()} 中消费一次。/ Consumed once, by {@link #checkRegistry()}.
     */
    private final Map<String, LlmProtocolStrategy> llmProtocolStrategies;

    /**
     * 中文说明：Servlet 出字节组件，engine 里唯一把协议字节变成响应字节的地方；本类因此不写任何一字节，也不自己拼
     * SSE 帧或错误正文。
     * English summary: the servlet write-out component, the only place in the engine that turns protocol bytes into
     * response bytes, which is why this class writes no byte at all and assembles neither an SSE frame nor an error body.
     *
     * 用法 / Usage: {@code writeUnary(command, routed, body, response)} 与
     * {@code writeStream(command, routed, frames, strategy::encodeError, response)}。
     */
    @Qualifier("llmServletStreamComponent")
    private final LlmServletStreamComponent llmServletStreamComponent;

    /**
     * 中文说明：受管配置只读端口，只用于一件事：目录端点的 alias 投影。路由所需的候选由 {@code invoke} 一次产出并随
     * {@link LlmInvocationResultVO#getCandidates()} 交付，本类因此<b>不再</b>为拿 route 而二次读快照或二次抽签。它是只读
     * 端口，因此本类不可能写入配置或触碰租户列。
     * English summary: the read-only managed configuration port, used for exactly one thing: projecting the alias catalog.
     * The routes needed for orchestration arrive from {@code invoke} in a single pass through
     * {@link LlmInvocationResultVO#getCandidates()}, so this class no longer re-reads a snapshot or re-draws the weighted
     * pick just to recover a route. Being read-only, it makes a write or a tenant column unreachable from this class.
     *
     * 用法 / Usage: {@code findCatalog()}，在只读事务内由仓储侧完成。/ {@code findCatalog()}, inside the repository's
     * read-only transaction.
     */
    @Qualifier("mpLlmConfigurationRepository")
    private final LlmConfigurationRepository configurationRepository;

    /**
     * 中文说明：启动期自检后的只读 {@code 协议 → Strategy} 视图；在 {@link #checkRegistry()} 之前为空，且永不改变。
     * English summary: the read-only {@code protocol → Strategy} view produced by the startup self-check; null before
     * {@link #checkRegistry()} and never mutated afterwards.
     */
    private volatile Map<LlmProtocolEnum, LlmProtocolStrategy> protocolStrategies;

    /**
     * 中文说明：注册表自检：用 {@link LlmProtocolStrategy#protocol()} 把四个具名 bean 归入只读 EnumMap，缺项、重复
     * bean 名、bean 自报协议与注册键不符、或注册表本身缺项，全部在启动期抛
     * {@link IllegalStateException} 失败关闭，<b>绝不</b>回落到 Chat 适配器，也不允许任何一个协议在无适配器的情况下
     * 被静默跳过。
     * English summary: The registry self-check that files the four named beans into a read-only EnumMap keyed by
     * {@link LlmProtocolStrategy#protocol()}: a missing protocol, a duplicate bean name, a bean whose declared protocol
     * disagrees with its registry key or a gap in the registry itself all fail startup with an
     * {@link IllegalStateException}, and it <b>never</b> falls back to the Chat adapter nor silently serves a protocol
     * that has no adapter.
     *
     * 用法 / Usage: 由容器在依赖注入完成后调用一次 / Invoked once by the container after injection completes.
     * @throws IllegalStateException 注册表缺项、重复或协议不符，容器启动失败；a missing, duplicated or mismatched registry entry, which fails startup.
     */
    @PostConstruct
    public void checkRegistry() {
        if (llmProtocolStrategyRegistry == null || llmProtocolStrategyRegistry.isEmpty()) {
            throw new IllegalStateException("The llmProtocolStrategyRegistry carries no protocol to strategy mapping");
        }
        EnumMap<LlmProtocolEnum, LlmProtocolStrategy> resolved = new EnumMap<>(LlmProtocolEnum.class);
        Set<String> usedBeanNames = new LinkedHashSet<>();
        llmProtocolStrategyRegistry.forEach((protocol, beanName) -> {
            if (protocol == null || StringUtils.isBlank(beanName)) {
                throw new IllegalStateException("Every llm protocol strategy registry entry needs a protocol and a bean name");
            }
            LlmProtocolStrategy strategy = llmProtocolStrategies == null
                    ? null : llmProtocolStrategies.get(beanName);
            if (strategy == null) {
                throw new IllegalStateException("No llm protocol strategy bean named " + beanName
                        + " for protocol " + protocol);
            }
            if (!usedBeanNames.add(beanName)) {
                throw new IllegalStateException("The llm protocol strategy bean " + beanName
                        + " is registered for more than one protocol");
            }
            if (strategy.protocol() != protocol) {
                throw new IllegalStateException("The llm protocol strategy bean " + beanName + " declares "
                        + strategy.protocol() + " but is registered for " + protocol);
            }
            if (resolved.put(protocol, strategy) != null) {
                throw new IllegalStateException("Two llm protocol strategies serve " + protocol);
            }
        });
        for (LlmProtocolEnum protocol : EnumSet.allOf(LlmProtocolEnum.class)) {
            if (!resolved.containsKey(protocol)) {
                throw new IllegalStateException("No llm protocol strategy is registered for " + protocol);
            }
        }
        this.protocolStrategies = Collections.unmodifiableMap(resolved);
        log.info("llm protocol strategy registry checked adapters={} protocols={}", resolved.size(), resolved.keySet());
    }

    /**
     * 中文说明：执行 createLlmChatCompletion 操作（API-001，{@code POST /v1/chat/completions}，Spec §9.2.1）：
     * 以 OPENAI_CHAT 协议处理 Chat Completions 原生 JSON/SSE 文档，能力、工具与流式帧语义全部由该协议 Strategy 决定，
     * 本方法只按端点决定协议并交出编排。
     * English summary: Executes the createLlmChatCompletion operation (API-001, {@code POST /v1/chat/completions},
     * Spec §9.2.1): the native Chat Completions JSON or SSE document is handled under OPENAI_CHAT, where capabilities,
     * tools and frame grammar are that protocol Strategy's business while this method only fixes the protocol by
     * endpoint and hands over the orchestration.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code POST /v1/chat/completions}，原生 Chat body，成功为原生 Chat JSON 或
     * {@code data:} SSE，失败为该协议原生 {@code error} 对象。
     * @param request 参数 当前请求，提供正文字节流与已认证主体；parameter the current request, supplying the body stream and the authenticated principal.
     * @param response 参数 当前响应，由 {@link LlmServletStreamComponent} 写出原生状态、安全头与 body；parameter the current response, written natively by {@link LlmServletStreamComponent}.
     * @throws IOException 客户端断连或容器写出失败；raised when the client disconnects or the container refuses the write.
     */
    @Operation(operationId = "createLlmChatCompletion", summary = "Create an OpenAI Chat Completions response",
            description = "Forwards the native Chat Completions document to a same-protocol route.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JsonNode.class))))
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Native Chat Completions response or SSE stream",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JsonNode.class)),
                            @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                                    schema = @Schema(type = "string"))}),
            @ApiResponse(responseCode = "400", description = "Invalid native request or unsupported parameter",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "401", description = "Enterprise service identity is missing or invalid",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "403", description = "The service identity cannot use this model alias",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "404", description = "The model alias is unavailable",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "413", description = "The request exceeds the configured bound",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "415", description = "The request media type is unsupported",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "422", description = "The configured route cannot satisfy the request policy",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "429", description = "The channel is saturated or rate limited",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "502", description = "The upstream response is invalid for this protocol",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "503", description = "No usable same-protocol route is available",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "504", description = "The upstream request exceeded its timeout budget",
                    content = @Content(schema = @Schema(implementation = JsonNode.class)))
    })
    @PostMapping(CHAT_COMPLETIONS_PATH)
    public void createLlmChatCompletion(HttpServletRequest request, HttpServletResponse response) throws IOException {
        invokeEntry(LlmProtocolEnum.OPENAI_CHAT, request, response);
    }

    /**
     * 中文说明：执行 createLlmEmbeddings 操作（API-002，{@code POST /v1/embeddings}，Spec §9.2.2）：
     * 以 OPENAI_EMBEDDING 协议处理有序批量向量化请求。{@code stream:true} 在这里同样只是被如实交出，由该协议
     * {@code encodeRequest} 在出网前 400 拒绝，因此「拼 JSON 伪造向量流」不可能发生；云端 embedding 候选恒为 0 次调用
     * （422 由路由策略给出，本类不加任何判断）。
     * English summary: Executes the createLlmEmbeddings operation (API-002, {@code POST /v1/embeddings}, Spec §9.2.2):
     * an ordered batch vectorization request under OPENAI_EMBEDDING. {@code stream:true} is passed through unchanged and
     * refused as a 400 by that protocol's {@code encodeRequest} before anything egresses, so a JSON document can never be
     * dressed up as a vector stream; a cloud embedding candidate stays at zero calls (the 422 comes from the routing
     * policy, which this class never duplicates).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code POST /v1/embeddings}，原生 Embeddings body，成功为原生
     * {@code data[].embedding} 文档。
     * @param request 参数 当前请求；parameter the current request.
     * @param response 参数 当前响应；parameter the current response.
     * @throws IOException 写出失败；raised when the write-out fails.
     */
    @Operation(operationId = "createLlmEmbeddings", summary = "Create local embeddings",
            description = "Forwards the native OpenAI Embeddings document to a local same-protocol route.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JsonNode.class))))
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Native Embeddings response",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "400", description = "Invalid native request or unsupported parameter",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "401", description = "Enterprise service identity is missing or invalid",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "403", description = "The service identity cannot use this model alias",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "404", description = "The model alias is unavailable",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "413", description = "The request exceeds the configured bound",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "415", description = "The request media type is unsupported",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "422", description = "Embedding egress is restricted to a local route",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "429", description = "The local embedding channel is saturated",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "503", description = "No usable local embedding route is available",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "504", description = "The local embedding request exceeded its timeout budget",
                    content = @Content(schema = @Schema(implementation = JsonNode.class)))
    })
    @PostMapping(EMBEDDINGS_PATH)
    public void createLlmEmbeddings(HttpServletRequest request, HttpServletResponse response) throws IOException {
        invokeEntry(LlmProtocolEnum.OPENAI_EMBEDDING, request, response);
    }

    /**
     * 中文说明：执行 createLlmResponse 操作（API-029，{@code POST /v1/responses}，Spec §9.2.29）：以
     * OPENAI_RESPONSES 协议处理类型化 input items、instructions、{@code function_call}/
     * {@code function_call_output} 与 opaque {@code reasoning} 回放；不转换成 Chat，也不在此判断任何 Responses 字段。
     * English summary: Executes the createLlmResponse operation (API-029, {@code POST /v1/responses}, Spec §9.2.29) under
     * OPENAI_RESPONSES for typed input items, instructions, {@code function_call}/{@code function_call_output} and opaque
     * {@code reasoning} replay: nothing is converted into Chat and no Responses field is inspected here.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code POST /v1/responses}，原生 Responses body，成功为原生 Responses JSON 或类型化事件 SSE。
     * @param request 参数 当前请求；parameter the current request.
     * @param response 参数 当前响应；parameter the current response.
     * @throws IOException 写出失败；raised when the write-out fails.
     */
    @Operation(operationId = "createLlmResponse", summary = "Create an OpenAI Responses response",
            description = "Forwards the native Responses document and typed stream to a same-protocol route.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JsonNode.class))))
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Native Responses result or typed SSE stream",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JsonNode.class)),
                            @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                                    schema = @Schema(type = "string"))}),
            @ApiResponse(responseCode = "400", description = "Invalid native request or unsupported parameter",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "401", description = "Enterprise service identity is missing or invalid",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "403", description = "The service identity cannot use this model alias",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "404", description = "The model alias is unavailable",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "413", description = "The request exceeds the configured bound",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "415", description = "The request media type is unsupported",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "422", description = "The configured route cannot satisfy request policy",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "429", description = "The channel is saturated or rate limited",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "502", description = "The upstream response is invalid for this protocol",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "503", description = "No usable same-protocol route is available",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "504", description = "The upstream request exceeded its timeout budget",
                    content = @Content(schema = @Schema(implementation = JsonNode.class)))
    })
    @PostMapping(RESPONSES_PATH)
    public void createLlmResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        invokeEntry(LlmProtocolEnum.OPENAI_RESPONSES, request, response);
    }

    /**
     * 中文说明：执行 createLlmMessage 操作（API-030，{@code POST /v1/messages}，Spec §9.2.30）：以
     * ANTHROPIC_MESSAGES 协议处理顶层 {@code system}、content blocks、{@code tool_use}/{@code tool_result} 与
     * {@code thinking} 签名保真。该端的凭据头适配已由 {@code LlmClientCredentialFilter} 在身份过滤器之前完成，本方法
     * 不读任何凭据；其错误对象为该协议原生 {@code type=error} 形状，永不套上 OpenAI 的 error/choices 结构。
     * English summary: Executes the createLlmMessage operation (API-030, {@code POST /v1/messages}, Spec §9.2.30) under
     * ANTHROPIC_MESSAGES for the top-level {@code system}, content blocks, {@code tool_use}/{@code tool_result} and
     * fidelity-preserving {@code thinking} signatures. Credential header adaptation for this entry already happened in
     * {@code LlmClientCredentialFilter} ahead of the identity filters, so no credential is read here, and its error object
     * is that protocol's native {@code type=error} shape, which never borrows the OpenAI error or choices structure.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code POST /v1/messages}，原生 Messages body，成功为原生 Message JSON 或
     * {@code message_start}…{@code message_stop} SSE。
     * @param request 参数 当前请求；parameter the current request.
     * @param response 参数 当前响应；parameter the current response.
     * @throws IOException 写出失败；raised when the write-out fails.
     */
    @Operation(operationId = "createLlmMessage", summary = "Create an Anthropic Messages response",
            description = "Forwards the native Anthropic Messages document and typed stream to a same-protocol route.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JsonNode.class))))
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Native Anthropic Messages result or typed SSE stream",
                    content = {@Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JsonNode.class)),
                            @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                                    schema = @Schema(type = "string"))}),
            @ApiResponse(responseCode = "400", description = "Invalid native request or unsupported parameter",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "401", description = "Enterprise service identity is missing or invalid",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "403", description = "The service identity cannot use this model alias",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "404", description = "The model alias is unavailable",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "413", description = "The request exceeds the configured bound",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "415", description = "The request media type is unsupported",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "422", description = "The configured route cannot satisfy request policy",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "429", description = "The channel is saturated or rate limited",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "502", description = "The upstream response is invalid for this protocol",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "503", description = "No usable same-protocol route is available",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "504", description = "The upstream request exceeded its timeout budget",
                    content = @Content(schema = @Schema(implementation = JsonNode.class)))
    })
    @PostMapping(MESSAGES_PATH)
    public void createLlmMessage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        invokeEntry(LlmProtocolEnum.ANTHROPIC_MESSAGES, request, response);
    }

    /**
     * 中文说明：执行 listLlmModels 操作（API-003，{@code GET /v1/models}，Spec §9.2.3）：唯一不路由的端点，只把
     * {@code findCatalog()} 的只读目录投影成原生 {@code {"object":"list","data":[…]}}：只保留当前身份被授权且 enabled
     * 的 alias、按 id 升序、最多 {@value #CATALOG_MAX_ENTRIES} 条，每项固定 {@code object:"model"} 与
     * {@code owned_by:"yuheng"}，不做上游动态发现、不返回地址或凭据、也不泄漏供应商真实模型名。
     * {@code created} 为模型持久化 {@code create_time} 对应的 {@link Instant} 秒值。失败（含无身份 401）按 OpenAI
     * 目录面的原生错误对象出网。
     * English summary: Executes the listLlmModels operation (API-003, {@code GET /v1/models}, Spec §9.2.3), the only
     * entry that routes nothing: {@code findCatalog()} is projected into the native
     * {@code {"object":"list","data":[…]}} document, keeping only the enabled aliases this identity is authorized for in
     * ascending id order and at most {@value #CATALOG_MAX_ENTRIES} entries, each with a fixed {@code object:"model"} and
     * {@code owned_by:"yuheng"}, with no upstream discovery, no address or credential and no vendor model name. The
     * {@code created} value is the persisted model {@code create_time} as epoch seconds. Failures, including a missing
     * identity as 401, leave as that OpenAI catalog face's native error object.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GET /v1/models}，无 body；成功 200 为原生 list 文档。
     * @param request 参数 当前请求，只提供已认证主体；parameter the current request, supplying only the authenticated principal.
     * @param response 参数 当前响应，由 {@link LlmServletStreamComponent#writeUnary} 写出；parameter the current response, written by {@link LlmServletStreamComponent#writeUnary}.
     * @throws IOException 写出失败；raised when the write-out fails.
     */
    @Operation(operationId = "listLlmModels", summary = "List model aliases available to this service identity",
            description = "Returns only enabled aliases authorized for the authenticated service subject.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Native model catalog",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "401", description = "Enterprise service identity is missing or invalid",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "403", description = "The service identity cannot list model aliases",
                    content = @Content(schema = @Schema(implementation = JsonNode.class))),
            @ApiResponse(responseCode = "503", description = "The model configuration catalog is unavailable",
                    content = @Content(schema = @Schema(implementation = JsonNode.class)))
    })
    @GetMapping(MODELS_PATH)
    public void listLlmModels(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Instant started = Instant.now();
        LlmInvocationCommandDTO command = catalogCommand(authenticatedSubject(request));
        ArrayNode data = objectMapper.createArrayNode();
        for (LlmModelSnapshotBO alias : authorizedCatalog(command)) {
            ObjectNode entry = data.addObject();
            entry.put("id", alias.getModelKey());
            entry.put("object", "model");
            entry.put("created", alias.getCreatedAt().getEpochSecond());
            entry.put("owned_by", CATALOG_OWNER);
        }
        ObjectNode document = objectMapper.createObjectNode();
        document.put("object", "list");
        document.set("data", data);
        LlmInvocationResultVO result = new LlmInvocationResultVO()
                .setStatus(200)
                .setHeaders(Map.of("Content-Type", "application/json", "Cache-Control", "no-store"));
        llmServletStreamComponent.writeUnary(command, result, document, response);
        log.info("llm catalog alias={} protocol={} entries={} status={} latency={}", CATALOG_ALIAS,
                command.getProtocol(), data.size(), result.getStatus(), latencyBucket(started));
    }

    /**
     * 中文说明：四个推理端点唯一的编排入口：有界读正文 → 只验形态 → 取可信主体 → 构造命令 →
     * {@code invoke} 授权与路由 → 复核首条候选 route → 同协议 {@code exchange} → 按 {@code stream} 位交
     * {@link LlmServletStreamComponent} 出字节。全程没有一处按协议分支：协议只作为参数传下去，编解码、上游出网、
     * 帧文法与错误 shape 全在按名解析出来的 Strategy 里。
     * English summary: The single orchestration entry of the four inference endpoints: a bounded body read → shape-only
     * validation → the trusted subject → the command → {@code invoke} for authorization and routing → the head candidate
     * route re-checked through the same policy → the same-protocol {@code exchange} → and, by the {@code stream} flag, the
     * bytes out through {@link LlmServletStreamComponent}. No branch on protocol appears anywhere: the protocol is only
     * passed down as a parameter, while encoding, egress, frame grammar and error shape all live in the by-name resolved
     * Strategy.
     *
     * 用法 / Usage: 仅由五个端点方法调用 / Invoked only by the five entry point methods.
     * @param protocol 参数 由端点常量决定的协议；parameter the endpoint-pinned protocol.
     * @param request 参数 当前请求；parameter the current request.
     * @param response 参数 当前响应；parameter the current response.
     * @throws IOException 写出失败；raised when the write-out fails.
     */
    private void invokeEntry(LlmProtocolEnum protocol, HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        Instant started = Instant.now();
        ObjectNode payload = nativePayload(request);
        rejectForbiddenEntryFields(payload);
        LlmInvocationCommandDTO command = LlmInvocationCommandDTO.builder()
                .protocol(protocol)
                .model(requiredAlias(payload))
                .stream(requiredStreamFlag(payload))
                .payload(payload)
                .callerSubject(authenticatedSubject(request))
                .allowedDeployments(EnumSet.allOf(LlmDeploymentEnum.class))
                .requiredCapabilities(requiredCapabilities(payload))
                .build();
        LlmInvocationResultVO routed = llmInvocationService.invoke(command);
        List<LlmModelSnapshotBO.RouteBO> candidates = routed.getCandidates();
        int safeAttempts = Math.min(gatewayProperties.getMaximumAttempts(), candidates.size());
        LlmProtocolStrategy strategy = strategyFor(protocol);
        int attempted = 0;
        LlmInvocationResultVO completed;
        while (true) {
            attempted++;
            try {
                completed = strategy.exchange(command, candidates.get(attempted - 1), routed);
                break;
            } catch (LlmInvocationException preCommit) {
                if (attempted >= safeAttempts || !preCommit.isRetryable()) {
                    throw preCommit;
                }
                log.warn("llm entry attempt closed alias={} protocol={} channel={} status={} attempt={} of {}",
                        command.getModel(), protocol, candidates.get(attempted - 1).getChannelKey(),
                        preCommit.getStatus(), attempted, safeAttempts);
            }
        }
        LlmModelSnapshotBO.RouteBO used = candidates.get(attempted - 1);
        int status = writeOut(command, strategy, completed, response);
        log.info("llm entry alias={} protocol={} stream={} candidates={} attempts={} channel={} status={} latency={}",
                command.getModel(), protocol, command.getStream(), candidates.size(), attempted, used.getChannelKey(),
                status, latencyBucket(started));
    }

    /**
     * 中文说明：按 {@code stream} 位交出唯一一次写出：流式把该协议的 {@code encodeError} 作为提交前编码器交给
     * {@code writeStream}；非流式先把单文档物化成原生 {@link ObjectNode} 再交给 {@code writeUnary}。body 缺失一律按
     * 502 原生收束，<b>绝不</b>退化成空成功。
     * English summary: Hands the single write-out over according to the {@code stream} flag: a stream takes this
     * protocol's {@code encodeError} as its pre-commit encoder through {@code writeStream}, while a non-stream call first
     * materializes the one document into its native {@link ObjectNode} for {@code writeUnary}. A missing body always
     * closes as a native 502 and never degrades into an empty success.
     *
     * 用法 / Usage: 由 {@link #invokeEntry} 调用 / Called by {@link #invokeEntry()}.
     * @param command 参数 本次命令；parameter this invocation's command.
     * @param strategy 参数 同协议 Strategy；parameter the same-protocol Strategy.
     * @param completed 参数 {@code exchange} 已补全的原生结果；parameter the native result {@code exchange} completed.
     * @param response 参数 当前响应；parameter the current response.
     * @return 返回 实际写出的原生状态；returns the native status actually written.
     * @throws IOException 写出失败；raised when the write-out fails.
     */
    private int writeOut(LlmInvocationCommandDTO command, LlmProtocolStrategy strategy,
            LlmInvocationResultVO completed, HttpServletResponse response) throws IOException {
        Flux<DataBuffer> body = completed.getBody();
        if (body == null) {
            throw new LlmInvocationException(502, "upstream_protocol_error", null,
                    "The routed llm result carries no response body", false);
        }
        if (Boolean.TRUE.equals(command.getStream())) {
            llmServletStreamComponent.writeStream(command, completed, body, strategy::encodeError, response);
        } else {
            llmServletStreamComponent.writeUnary(command, completed, unaryDocument(command, body), response);
        }
        return completed.getStatus();
    }

    /**
     * 中文说明：在路由<b>之前</b>有界读取原生请求文档：先按声明长度快速拒绝，再用
     * {@code readNBytes(ceiling + 1)} 做绝对上界（永不无界缓冲），然后只接受 JSON 对象。任何失败都是该端点协议的
     * 原生错误：413 {@code request_too_large}（Spec §9.0.1 的整请求上限）、400 {@code unsupported_parameter}
     * （不可解析或不是对象）。这里不做任何字段语义判断，那属于协议面职责。
     * English summary: Reads the native request document with a bound <b>before</b> routing: the declared content length
     * rejects cheaply first, an absolute ceiling of {@code ceiling + 1} bytes makes the buffer bounded, and only a JSON
     * object survives. Every failure here is that entry protocol's native error: 413 {@code request_too_large} for
     * Spec §9.0.1's whole-request ceiling and 400 {@code unsupported_parameter} for an unparsable or non-object document.
     * No field semantics are judged at all, since that is the protocol face's duty.
     *
     * 用法 / Usage: 由 {@link #invokeEntry} 调用 / Called by {@link #invokeEntry()}.
     * @param request 参数 当前请求；parameter the current request.
     * @return 返回 原生请求文档；returns the native request document.
     * @throws LlmInvocationException 413 超限、400 不可解析或不是 JSON 对象、或正文不可读；a 413 above the ceiling, a 400 for an unparsable or non-object document, or an unreadable body.
     */
    private ObjectNode nativePayload(HttpServletRequest request) {
        long ceiling = gatewayProperties.getMaxRequestBytes();
        long declared = request.getContentLengthLong();
        if (declared > ceiling) {
            throw requestTooLarge();
        }
        byte[] raw;
        try {
            raw = request.getInputStream().readNBytes(toReadLimit(ceiling));
        } catch (IOException unreadable) {
            log.warn("llm request body could not be read type={}", unreadable.getClass().getSimpleName());
            throw new LlmInvocationException(400, "unsupported_parameter", null,
                    "The llm request body could not be read", false);
        }
        if (raw.length > ceiling) {
            throw requestTooLarge();
        }
        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(raw);
        } catch (IOException malformed) {
            throw new LlmInvocationException(400, "unsupported_parameter", null,
                    "The llm request body is not a parsable JSON document", false);
        }
        if (!(parsed instanceof ObjectNode document)) {
            throw new LlmInvocationException(400, "unsupported_parameter", null,
                    "The llm request body must be a JSON object", false);
        }
        return document;
    }

    /**
     * 中文说明：在路由前拒绝任何自证出网或凭据字段（大小写不敏感的顶层字段名）：这类字段的存在意味着客户端试图
     * 自己指定上游地址或密钥，Spec §9.0.1 一律禁止，协议面 {@code encodeRequest} 会再拒一次，因此违约不可能出网。
     * 错误只回字段名，绝不回字段值。
     * English summary: Refuses any self-asserted egress or credential field before routing, by case-insensitive top-level
     * field name: such a field means the client is trying to choose its own upstream address or key, which Spec §9.0.1
     * forbids outright, and the protocol face's {@code encodeRequest} refuses it a second time so no violation can ever
     * egress. The error names the field only and never its value.
     *
     * 用法 / Usage: 由 {@link #invokeEntry} 在构造命令之前调用 / Called by {@link #invokeEntry()} before the command is built.
     * @param payload 参数 原生请求文档；parameter the native request document.
     * @throws LlmInvocationException 400 {@code unsupported_parameter}（param 为违规字段名）；a 400 {@code unsupported_parameter} whose param is the offending field.
     */
    private void rejectForbiddenEntryFields(ObjectNode payload) {
        for (Iterator<String> fields = payload.fieldNames(); fields.hasNext();) {
            String field = fields.next();
            if (FORBIDDEN_ENTRY_FIELDS.contains(StringUtils.lowerCase(field, Locale.ROOT))) {
                throw new LlmInvocationException(400, "unsupported_parameter", field,
                        "Self-asserted egress and credential fields are not accepted by any llm entry point", false);
            }
        }
    }

    /**
     * 中文说明：读取 {@code model}：必填、必须是字符串、trim 后必须仍是受管 alias 形态。违约即 400 且只回
     * {@code model} 这个参数名，绝不回显客户端提交的值。
     * English summary: Reads {@code model}: required, textual, and still in the managed alias shape after trimming. A
     * violation is a 400 naming {@code model} only and never echoes what the client submitted.
     *
     * 用法 / Usage: 由 {@link #invokeEntry} 调用 / Called by {@link #invokeEntry()}.
     * @param payload 参数 原生请求文档；parameter the native request document.
     * @return 返回 客户端 alias；returns the client alias.
     * @throws LlmInvocationException 400 {@code unsupported_parameter}（param 为 {@code model}）；a 400 {@code unsupported_parameter} on param {@code model}.
     */
    private String requiredAlias(ObjectNode payload) {
        JsonNode model = payload.get("model");
        String alias = model == null || !model.isTextual() ? null : StringUtils.trimToNull(model.asText());
        if (alias == null || !alias.matches(ALIAS_PATTERN)) {
            throw new LlmInvocationException(400, "unsupported_parameter", "model",
                    "A managed model alias is required", false);
        }
        return alias;
    }

    /**
     * 中文说明：读取 {@code stream}：缺省显式写 false，显式 {@code null} 与非布尔一律 400 拒绝（Spec §9.0.1
     * 「缺省 false，null 拒绝」），绝不猜默认值。
     * English summary: Reads {@code stream}: absent writes an explicit false, while an explicit {@code null} or any
     * non-boolean is refused with a 400 (Spec §9.0.1's "default false, reject null"), so no default is ever guessed.
     *
     * 用法 / Usage: 由 {@link #invokeEntry} 调用 / Called by {@link #invokeEntry()}.
     * @param payload 参数 原生请求文档；parameter the native request document.
     * @return 返回 非 null 的 stream 位；returns the non-null stream flag.
     * @throws LlmInvocationException 400 {@code unsupported_parameter}（param 为 {@code stream}）；a 400 {@code unsupported_parameter} on param {@code stream}.
     */
    private Boolean requiredStreamFlag(ObjectNode payload) {
        JsonNode stream = payload.get("stream");
        if (stream == null) {
            return Boolean.FALSE;
        }
        if (!stream.isBoolean()) {
            throw new LlmInvocationException(400, "unsupported_parameter", "stream",
                    "The llm stream flag must be a boolean when present", false);
        }
        return stream.booleanValue();
    }

    /**
     * 中文说明：由已校验字段派生本端点必须具备的能力：TEXT 恒需，声明了非空 {@code tools} 数组再加
     * FUNCTION_TOOLS。该规则只看结构、不看协议，因此四种协议共用一条派生路径，而 VISION/REASONING/
     * STRUCTURED_OUTPUT 的取舍仍属协议面 {@code encodeRequest}。
     * English summary: Derives the capabilities this entry requires from the validated fields: TEXT always, plus
     * FUNCTION_TOOLS when a non-empty {@code tools} array is declared. The rule reads structure rather than protocol, so
     * all four faces share one derivation path while VISION, REASONING and STRUCTURED_OUTPUT stay with the protocol
     * face's {@code encodeRequest}.
     *
     * 用法 / Usage: 由 {@link #invokeEntry} 调用 / Called by {@link #invokeEntry()}.
     * @param payload 参数 原生请求文档；parameter the native request document.
     * @return 返回 1–5 项能力集合；returns the one-to-five capability set.
     */
    private Set<LlmCapabilityEnum> requiredCapabilities(ObjectNode payload) {
        Set<LlmCapabilityEnum> capabilities = EnumSet.of(LlmCapabilityEnum.TEXT);
        JsonNode tools = payload.get("tools");
        if (tools != null && tools.isArray() && !tools.isEmpty()) {
            capabilities.add(LlmCapabilityEnum.FUNCTION_TOOLS);
        }
        return capabilities;
    }

    /**
     * 中文说明：取本次命令的可信 SERVICE 主体：只用 Servlet 容器的已认证主体（本模块禁止使用任何 Spring Security
     * 类型），空白、过长或不可用一律 401 {@code authentication_error} 失败关闭——主体缺失时连快照都不读，因此
     * 未认证请求不可能泄漏任何 alias 事实。
     * English summary: Takes the trusted SERVICE subject of this command from the servlet container's authenticated
     * principal only (no Spring Security type is permitted in this module), so a blank, over-long or unusable identity
     * fails closed as a 401 {@code authentication_error}. Because nothing is read before the subject exists, an
     * unauthenticated request can never leak a single alias fact.
     *
     * 用法 / Usage: 由 {@link #invokeEntry} 与 {@link #listLlmModels} 调用 / Called by {@link #invokeEntry()} and {@link #listLlmModels}.
     * @param request 参数 当前请求；parameter the current request.
     * @return 返回 至多 {@value #MAX_SUBJECT_LENGTH} 字符的主体标识；returns the subject identifier, at most {@value #MAX_SUBJECT_LENGTH} characters.
     * @throws LlmInvocationException 401 {@code authentication_error}；raised when no usable authenticated identity reached this entry point.
     */
    private String authenticatedSubject(HttpServletRequest request) {
        String subject;
        try {
            subject = request.getUserPrincipal() == null
                    ? null : StringUtils.trimToNull(request.getUserPrincipal().getName());
        } catch (RuntimeException unusable) {
            log.warn("llm caller identity unusable type={}", unusable.getClass().getSimpleName());
            subject = null;
        }
        if (subject == null || subject.length() > MAX_SUBJECT_LENGTH) {
            throw new LlmInvocationException(401, "authentication_error", null,
                    "A verified enterprise service identity is required for this llm entry point", false);
        }
        return subject;
    }

    /**
     * 中文说明：按协议取同协议适配器；自检已经保证四个协议各有一个适配器，因此这里为空只可能是编排被绕过，
     * 按 503 {@code model_unavailable} 原生失败关闭，<b>绝不</b>回落 Chat。
     * English summary: Resolves the same-protocol adapter by protocol. The self-check already guarantees one adapter per
     * protocol, so a null here can only mean the orchestration was bypassed, which fails closed as a native 503
     * {@code model_unavailable} and <b>never</b> falls back to Chat.
     *
     * 用法 / Usage: 由 {@link #invokeEntry} 与错误编码入口调用 / Called by {@link #invokeEntry()} and by the error encoding seam.
     * @param protocol 参数 目标协议；parameter the target protocol.
     * @return 返回 该协议的 Strategy；returns that protocol's Strategy.
     * @throws LlmInvocationException 503 {@code model_unavailable}（该协议没有适配器）；raised when no adapter is registered for the protocol.
     */
    private LlmProtocolStrategy strategyFor(LlmProtocolEnum protocol) {
        Map<LlmProtocolEnum, LlmProtocolStrategy> resolved = this.protocolStrategies;
        LlmProtocolStrategy strategy = resolved == null ? null : resolved.get(protocol);
        if (strategy == null) {
            throw new LlmInvocationException(503, "model_unavailable", null,
                    "No protocol adapter is available for this llm entry point", false);
        }
        return strategy;
    }

    /**
     * 中文说明：把非流式的单文档 body 物化成原生 {@link ObjectNode}：用 {@code DataBufferUtils.join} 在同一字节上限下
     * 收束并恰好释放一次，空文档 502、超上限 413、预算内未就绪 504、不可解析 502——每条路径都如实报错，
     * 因此「上游失败」不可能被伪装成空成功。协议面的 {@code exchange} 已把响应读成内存单缓冲，这里的预算只是防御。
     * English summary: Materializes a non-stream single-document body into its native {@link ObjectNode}:
     * {@code DataBufferUtils.join} closes it under the same byte ceiling and it is released exactly once, with an empty
     * document a 502, an overflow a 413, an expired budget a 504 and unparsable bytes a 502, so every path reports itself
     * honestly and an upstream failure can never be dressed up as an empty success. The protocol face's {@code exchange}
     * has already read the response into one in-memory buffer, so the budget here is only defensive.
     *
     * 用法 / Usage: 由 {@link #writeOut} 的非流式分支调用 / Called from the non-stream branch of {@link #writeOut}.
     * @param command 参数 本次命令，只用于日志标识；parameter this command, used only for the log identities.
     * @param body 参数 协议面产出的有界单文档；parameter the bounded single document the protocol face produced.
     * @return 返回 原生响应文档；returns the native response document.
     * @throws LlmInvocationException 413/502/504，状态即原生状态；a 413, 502 or 504, each already the native status.
     */
    private ObjectNode unaryDocument(LlmInvocationCommandDTO command, Flux<DataBuffer> body) {
        DataBuffer document = null;
        try {
            document = DataBufferUtils.join(body, toReadLimit(gatewayProperties.getMaxRequestBytes()))
                    .blockOptional(UNARY_BODY_BUDGET)
                    .orElse(null);
        } catch (LlmInvocationException nativeFailure) {
            throw nativeFailure;
        } catch (DataBufferLimitException oversized) {
            throw requestTooLarge();
        } catch (IllegalStateException stalled) {
            throw new LlmInvocationException(504, "upstream_timeout", null,
                    "The llm response document did not arrive within the unary budget", false);
        } catch (RuntimeException failed) {
            log.warn("llm unary response document failed alias={} protocol={} type={}",
                    command.getModel(), command.getProtocol(), failed.getClass().getSimpleName());
            throw new LlmInvocationException(502, "upstream_protocol_error", null,
                    "The llm response document could not be read", false);
        }
        if (document == null) {
            throw new LlmInvocationException(502, "upstream_protocol_error", null,
                    "The llm upstream response produced no document", false);
        }
        try {
            byte[] raw = new byte[document.readableByteCount()];
            document.read(raw);
            JsonNode parsed = objectMapper.readTree(raw);
            if (!(parsed instanceof ObjectNode nativeDocument)) {
                throw new LlmInvocationException(502, "upstream_protocol_error", null,
                        "The llm upstream response is not a JSON object", false);
            }
            return nativeDocument;
        } catch (IOException malformed) {
            throw new LlmInvocationException(502, "upstream_protocol_error", null,
                    "The llm upstream response is not parsable JSON", false);
        } finally {
            DataBufferUtils.release(document);
        }
    }

    /**
     * 中文说明：目录投影的授权过滤：只保留 enabled 且 {@code allowedSubjects} 显式包含本次主体的 alias，按 id
     * （即 alias）升序并截断到 {@value #CATALOG_MAX_ENTRIES} 条；授权列表缺失或为空即禁止全部，绝不视为放行。
     * English summary: The catalog's authorization filter, keeping only enabled aliases whose {@code allowedSubjects}
     * explicitly contain this request's subject, ordered by id (the alias itself) ascending and truncated to
     * {@value #CATALOG_MAX_ENTRIES} entries; an absent or empty authorization list forbids all callers and is never read
     * as permission.
     *
     * 用法 / Usage: 由 {@link #listLlmModels} 调用 / Called by {@link #listLlmModels}.
     * @param command 参数 目录命令，只提供可信主体；parameter the catalog command, supplying only the trusted subject.
     * @return 返回 有界、升序、已授权的目录视图；returns the bounded, id-ascending, authorized catalog view.
     */
    private List<LlmModelSnapshotBO> authorizedCatalog(LlmInvocationCommandDTO command) {
        List<LlmModelSnapshotBO> catalog = configurationRepository.findCatalog();
        if (catalog == null || catalog.isEmpty()) {
            return List.of();
        }
        return catalog.stream()
                .filter(alias -> alias != null && Boolean.TRUE.equals(alias.getEnabled()))
                .filter(alias -> StringUtils.isNotBlank(alias.getModelKey()))
                .filter(alias -> alias.getAllowedSubjects() != null
                        && alias.getAllowedSubjects().contains(command.getCallerSubject()))
                .sorted(Comparator.comparing(LlmModelSnapshotBO::getModelKey))
                .limit(CATALOG_MAX_ENTRIES)
                .toList();
    }

    /**
     * 中文说明：目录端点没有客户端 alias，因此构造一个只用于日志与安全头写出的命令载体：协议为 OpenAI 目录面、
     * {@code stream} 恒 false、正文为空对象、能力只要求 TEXT；该命令不进入路由，也不携带任何客户端数据。
     * English summary: The catalog has no client alias, so this builds the command carrier used only for the log and the
     * safe headers: the OpenAI catalog face as protocol, {@code stream} always false, an empty document and TEXT as the
     * only capability. It never reaches routing and carries no client data.
     *
     * 用法 / Usage: 由 {@link #listLlmModels} 调用 / Called by {@link #listLlmModels}.
     * @param callerSubject 参数 已认证主体；parameter the authenticated subject.
     * @return 返回 目录命令；returns the catalog command.
     */
    private LlmInvocationCommandDTO catalogCommand(String callerSubject) {
        return LlmInvocationCommandDTO.builder()
                .protocol(DEFAULT_PROTOCOL)
                .model(CATALOG_ALIAS)
                .stream(Boolean.FALSE)
                .payload(objectMapper.createObjectNode())
                .callerSubject(callerSubject)
                .allowedDeployments(EnumSet.allOf(LlmDeploymentEnum.class))
                .requiredCapabilities(EnumSet.of(LlmCapabilityEnum.TEXT))
                .build();
    }

    /**
     * 中文说明：处理 {@link LlmInvocationException}：按入口路径选出<b>该协议</b>的 Strategy，用其
     * {@code encodeError} 产出原生错误文档，状态逐字保留并显式给出 JSON 与 {@code no-store}；路径判不出协议时按
     * Spec 回落 OpenAI 错误对象。已提交的响应不再补写任何字节（提交屏障之后只允许结束）。
     * English summary: Handles {@link LlmInvocationException} by resolving the Strategy of <b>that</b> protocol from the
     * entry path, producing its native error document through {@code encodeError} with the status preserved verbatim and
     * an explicit JSON plus {@code no-store}, and falling back to the OpenAI error object when no path decides a protocol,
     * exactly as the Spec requires. A committed response gets no further bytes, since only ending is allowed past the
     * commit barrier.
     *
     * 用法 / Usage: 由 Spring MVC 异常解析调用 / Invoked by Spring MVC's exception resolution.
     * @param failure 参数 安全原生错误载体；parameter the safe native error carrier.
     * @param request 参数 当前请求，提供入口路径；parameter the current request, supplying the entry path.
     * @param response 参数 当前响应；parameter the current response.
     * @return 返回 原生状态与安全错误文档；returns the native status with the safe error document.
     */
    @ExceptionHandler(LlmInvocationException.class)
    public ResponseEntity<ObjectNode> handleLlmInvocationException(LlmInvocationException failure,
            HttpServletRequest request, HttpServletResponse response) {
        return nativeError(entryProtocol(request), failure, request, response);
    }

    /**
     * 中文说明：处理路由 {@link CommonException}（403/404/422/503 等）：其 {@code getCode()} 就是原生 HTTP 状态，
     * 机器码按 Spec 状态表映射，说明文字截断到安全长度，然后同样交给该端点协议的原生 {@code encodeError}——因此
     * 授权与路由失败绝不会以 admin 的 {@code code}/{@code data} 信封或容器错误页出网。
     * English summary: Handles the routing {@link CommonException} (403/404/422/503 …), whose {@code getCode()}
     * <i>is</i> the native HTTP status: the machine code comes from the Spec status table, the description is
     * abbreviated to a safe length and the result is encoded through this entry protocol's native {@code encodeError}, so
     * an authorization or routing failure can never leave as the admin {@code code}/{@code data} envelope or as a
     * container error page.
     *
     * 用法 / Usage: 由 Spring MVC 异常解析调用 / Invoked by Spring MVC's exception resolution.
     * @param failure 参数 路由或授权失败；parameter the routing or authorization failure.
     * @param request 参数 当前请求；parameter the current request.
     * @param response 参数 当前响应；parameter the current response.
     * @return 返回 原生状态与原生错误文档；returns the native status with the native error document.
     */
    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ObjectNode> handleCommonException(CommonException failure,
            HttpServletRequest request, HttpServletResponse response) {
        return nativeError(entryProtocol(request), asNative(failure), request, response);
    }

    /**
     * 中文说明：处理端口边界 {@code @Valid} 抛出的 {@link ConstraintViolationException}：违约意味着控制器构造的命令
     * 不合业务合同，按 400 {@code unsupported_parameter} 原生收束，且只用固定安全说明——属性消息、被拒值与正文一律
     * 不回显，容器默认错误页也不可能出现。
     * English summary: Handles the {@link ConstraintViolationException} a {@code @Valid} port boundary raises: a violation
     * means the command this controller built breaks the business contract, so it closes as a native 400
     * {@code unsupported_parameter} with a fixed safe description only — no property message, no rejected value and no
     * payload is ever echoed, and the container's default error page cannot appear either.
     *
     * 用法 / Usage: 由 Spring MVC 异常解析调用 / Invoked by Spring MVC's exception resolution.
     * @param failure 参数 方法校验违约；parameter the violated method constraints.
     * @param request 参数 当前请求；parameter the current request.
     * @param response 参数 当前响应；parameter the current response.
     * @return 返回 原生 400 与协议原生错误文档；returns the native 400 with that protocol's native error document.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ObjectNode> handleConstraintViolation(ConstraintViolationException failure,
            HttpServletRequest request, HttpServletResponse response) {
        log.warn("llm command contract violated path={} violations={}", entryPath(request),
                failure.getConstraintViolations().size());
        return nativeError(entryProtocol(request), new LlmInvocationException(400, "unsupported_parameter", null,
                "The model invocation command violates its business contract", false), request, response);
    }

    /**
     * 中文说明：原生错误出网的唯一收口：把安全载体交给该端点协议的 {@code encodeError}，并显式给出
     * {@code application/json} 与 {@code Cache-Control: no-store}；日志只记路径、alias、协议、状态与机器码。
     * 已提交时只结束本次响应，绝不写第二个状态行。
     * English summary: The single closure of a native error: the safe carrier goes to this entry protocol's
     * {@code encodeError} and the response states {@code application/json} plus {@code Cache-Control: no-store}
     * explicitly, while the log records only the path, alias, protocol, status and machine code. A committed response is
     * merely ended, never given a second status line.
     *
     * 用法 / Usage: 由三个 {@code @ExceptionHandler} 调用 / Called by the three exception handlers.
     * @param protocol 参数 决定错误 shape 的协议面；parameter the protocol face deciding the error shape.
     * @param failure 参数 已脱敏的原生错误；parameter the masked native error.
     * @param request 参数 当前请求；parameter the current request.
     * @param response 参数 当前响应；parameter the current response.
     * @return 返回 原生状态与安全错误文档，已提交时为同状态空文档；returns the native status with the safe error document, or the same status with no body once committed.
     */
    private ResponseEntity<ObjectNode> nativeError(LlmProtocolEnum protocol, LlmInvocationException failure,
            HttpServletRequest request, HttpServletResponse response) {
        log.warn("llm entry rejected path={} protocol={} status={} code={} retryable={}", entryPath(request),
                protocol, failure.getStatus(), failure.getCode(), failure.isRetryable());
        if (response.isCommitted()) {
            return ResponseEntity.status(failure.getStatus()).build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setCacheControl(CacheControl.noStore());
        return ResponseEntity.status(failure.getStatus()).headers(headers).body(strategyFor(protocol).encodeError(failure));
    }

    /**
     * 中文说明：把路由 {@link CommonException} 换成安全原生载体：状态取 {@code getCode()}（越界则退回 502），
     * 机器码按 Spec 状态表，说明文字 trim 后截断（协议面无不回显上游原文的固定说明），可重试位取 Spec 的
     * 429/503 判定与异常自身结论的并集。
     * English summary: Turns the routing {@link CommonException} into the safe native carrier: the status is
     * {@code getCode()} (502 when it is not a real HTTP status), the machine code comes from the Spec status table, the
     * description is trimmed and abbreviated (the protocol face carries no raw upstream text to begin with), and the
     * retryable flag is the union of the Spec's 429/503 verdict and the exception's own.
     *
     * 用法 / Usage: 由 {@link #handleCommonException} 调用 / Called by {@link #handleCommonException}.
     * @param failure 参数 路由或授权失败；parameter the routing or authorization failure.
     * @return 返回 安全原生错误；returns the safe native error.
     */
    private LlmInvocationException asNative(CommonException failure) {
        int status = failure.getCode();
        if (status < 100 || status > 599) {
            status = 502;
        }
        String code = ROUTING_ERROR_CODES.get(failure.getStatus());
        if (code == null) {
            code = NATIVE_CODES_BY_STATUS.get(status);
        }
        String message = StringUtils.abbreviate(StringUtils.trimToNull(failure.getMessage()), MAX_SAFE_MESSAGE_LENGTH);
        return new LlmInvocationException(status, code, null,
                message == null ? "The model alias cannot be served by any authorized route" : message,
                failure.isRetryable() || RETRYABLE_STATUSES.contains(status));
    }

    /**
     * 中文说明：从入口路径判定协议面：先剥掉 contextPath 与结尾斜杠（与既有 Messages 凭据过滤器同一写法），
     * 再查端点表；判不出即返回默认 OpenAI 面。
     * English summary: Decides the protocol face from the entry path by stripping the context path and a trailing slash
     * (the same convention the existing Messages credential filter uses), then consulting the endpoint table and taking
     * the default OpenAI face when no entry matches.
     *
     * 用法 / Usage: 由三个 {@code @ExceptionHandler} 调用 / Called by the three exception handlers.
     * @param request 参数 当前请求；parameter the current request.
     * @return 返回 该请求的协议面；returns the protocol face of this request.
     */
    private LlmProtocolEnum entryProtocol(HttpServletRequest request) {
        LlmProtocolEnum protocol = ENTRY_PROTOCOLS.get(entryPath(request));
        return protocol == null ? DEFAULT_PROTOCOL : protocol;
    }

    /** 中文说明：入口路径（去掉 contextPath 与结尾斜杠），只用于协议选择与日志。 English summary: The entry path without the context path or a trailing slash, used only for protocol selection and logging. */
    private static String entryPath(HttpServletRequest request) {
        return StringUtils.removeEnd(StringUtils.removeStart(request.getRequestURI(),
                StringUtils.defaultString(request.getContextPath())), "/");
    }

    /** 中文说明：整请求字节的读取上界（多留 1 字节即可判定越界，同时永不无界分配）。 English summary: The read ceiling in bytes, one past the request limit so an overflow is detectable while the allocation never grows without bound. */
    private static int toReadLimit(long ceiling) {
        return (int) Math.min(ceiling + 1L, Integer.MAX_VALUE - 1L);
    }

    /** 中文说明：413 {@code request_too_large} 的固定安全错误，请求与响应文档共用。 English summary: The fixed safe 413 {@code request_too_large}, shared by the request and the response document. */
    private static LlmInvocationException requestTooLarge() {
        return new LlmInvocationException(413, "request_too_large", null,
                "The llm request exceeds yuheng.llm.max-request-bytes", false);
    }

    /** 中文说明：延迟分桶，日志只记桶名以保持低基数。 English summary: The latency bucket, since logging only the bucket name keeps the cardinality low. */
    private static String latencyBucket(Instant started) {
        long millis = Duration.between(started, Instant.now()).toMillis();
        for (int index = 0; index < LATENCY_BUCKETS_MILLIS.length; index++) {
            if (millis <= LATENCY_BUCKETS_MILLIS[index]) {
                return LATENCY_BUCKETS[index];
            }
        }
        return LATENCY_BUCKETS[LATENCY_BUCKETS.length - 1];
    }

    /** 中文说明：端点与协议的固定映射表（含目录端点走 OpenAI 面）。 English summary: The fixed endpoint-to-protocol table, including the catalog entry on the OpenAI face. */
    private static Map<String, LlmProtocolEnum> entryProtocols() {
        Map<String, LlmProtocolEnum> protocols = new TreeMap<>();
        protocols.put(CHAT_COMPLETIONS_PATH, LlmProtocolEnum.OPENAI_CHAT);
        protocols.put(EMBEDDINGS_PATH, LlmProtocolEnum.OPENAI_EMBEDDING);
        protocols.put(MODELS_PATH, LlmProtocolEnum.OPENAI_CHAT);
        protocols.put(RESPONSES_PATH, LlmProtocolEnum.OPENAI_RESPONSES);
        protocols.put(MESSAGES_PATH, LlmProtocolEnum.ANTHROPIC_MESSAGES);
        return Collections.unmodifiableMap(protocols);
    }

    /** 中文说明：Spec §9.0.1 明列的危险字段名（小写比较），在路由前拒绝。 English summary: The dangerous field names Spec §9.0.1 lists explicitly, compared lower-cased and refused before routing. */
    private static Set<String> forbiddenEntryFields() {
        Set<String> fields = new LinkedHashSet<>();
        Collections.addAll(fields, "upstream_url", "upstreammodel", "base_url", "baseurl", "endpoint",
                "credential", "credentials", "secret", "secret_ref", "secretref", "host", "authorization",
                "api_key", "apikey", "x_api_key", "xapikey");
        return Collections.unmodifiableSet(fields);
    }

    /** 中文说明：路由策略的稳定状态名到 Spec 原生机器码的映射。 English summary: The mapping from the routing policy's stable status names to the Spec's native machine codes. */
    private static Map<String, String> routingErrorCodes() {
        Map<String, String> codes = new TreeMap<>();
        codes.put("LLM_ENGINE_DISABLED", "model_unavailable");
        codes.put("LLM_MODEL_NOT_FOUND", "model_unavailable");
        codes.put("LLM_MODEL_NOT_AVAILABLE", "model_unavailable");
        codes.put("LLM_MODEL_NOT_AUTHORIZED", "model_forbidden");
        codes.put("LLM_NO_ELIGIBLE_ROUTE", "model_unavailable");
        codes.put("LLM_EMBEDDING_CLOUD_CONFIGURED", "unsupported_parameter");
        codes.put("LLM_EMBEDDING_SPACE_UNDECLARED", "unsupported_parameter");
        return Collections.unmodifiableMap(codes);
    }

    /** 中文说明：Spec 模型入口错误表的状态到机器码兜底映射（未列状态保留原生状态并给出最保守的协议错误码）。 English summary: The Spec status-to-machine-code fallback for the model entry (an unlisted status keeps its native status and gets the most conservative protocol error code). */
    private static Map<Integer, String> nativeCodesByStatus() {
        Map<Integer, String> codes = new TreeMap<>();
        codes.put(400, "unsupported_parameter");
        codes.put(401, "authentication_error");
        codes.put(403, "model_forbidden");
        codes.put(404, "model_unavailable");
        codes.put(413, "request_too_large");
        codes.put(422, "unsupported_parameter");
        codes.put(429, "rate_limit_exceeded");
        codes.put(502, "upstream_protocol_error");
        codes.put(503, "model_unavailable");
        codes.put(504, "upstream_timeout");
        return Collections.unmodifiableMap(codes);
    }
}
