package top.egon.cola.component.yuheng.llm.proxy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import top.egon.cola.component.yuheng.llm.config.LlmGatewayProperties;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.dto.LlmInvocationCommandDTO;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmCapabilityEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.exception.LlmInvocationException;
import top.egon.cola.component.yuheng.llm.proxy.domain.vo.LlmInvocationResultVO;

/**
 * 中文说明：{@code OpenAiResponsesProtocolStrategy} 是 {@link LlmProtocolStrategy} 四具名适配器中的
 * {@code POST /v1/responses} 协议面（bean 名 {@code openAiResponsesProtocolStrategy}，由
 * {@code llmProtocolStrategyRegistry} 的 {@link LlmProtocolEnum#OPENAI_RESPONSES} 条目限定），只做一件事：
 * <b>同协议保真</b>。请求侧核对已知必需字段（{@code model}+非空 {@code input}（string 或 1–1000 个 items）、
 * 可选 {@code instructions}、本协议自己的输出预算 {@code max_output_tokens}，Chat 的
 * {@code max_tokens}/{@code max_completion_tokens} 出现在这里一律 400，不得跨协议替换），无条件把 {@code store}
 * 钉成 {@code false}（缺失补 false、显式 true 直接 400：网关从不把客户端内容留在上游账号里），拒绝
 * {@code background=true}、{@code conversation}、{@code previous_response_id} 这类托管会话自证，拒绝任意
 * {@code upstream_url}/{@code base_url}/{@code host}/{@code credential}/{@code token} 等出网与凭据自证字段，
 * 未声明能力的请求扩展（tools/tool_choice/parallel_tool_calls→FUNCTION_TOOLS、text→STRUCTURED_OUTPUT、
 * reasoning/include/加密内容→REASONING、图片 input parts→VISION）一律 400；随后只把 {@code model} 改写为
 * {@code route.getUpstreamModel()}，其余结构、数值表示与数组顺序逐字保留，整份文档受
 * {@code yuheng.llm.max-request-bytes} 约束。{@code function_call} 与 {@code function_call_output} items 的
 * {@code call_id} 按不透明字符串原样回放并强制配对（output 必须引用同一文档里出现过的调用），
 * {@code reasoning} items 从不解密、从不打印。响应侧保持类型化 {@code output} items（message/function_call/
 * reasoning）与 {@code status}/{@code usage}/{@code error}/{@code incomplete_details} 及一切未知但安全字段的原生
 * JSON，绝不把 Responses 输出降格成 Chat 的 {@code choices}，只把 {@code model} 回写为客户端 alias；缺少
 * {@code status} 或非空 {@code output} 数组、{@code completed} 却零 item、{@code function_call} 缺 {@code call_id}
 * 或 {@code function_call_output} 配不上对，都是 502 {@code upstream_protocol_error}，绝不返回空成功。
 * 流式侧按 Responses 事件族解析上游 SSE：每帧必须携带文本 {@code type} 与严格递增的 {@code sequence_number}
 * （顺序即上游顺序，{@code response.output_text.delta} 片段永不重排），合法未知事件原样转发并只做低基数计数，
 * 终态标记只有 {@code response.completed}/{@code response.incomplete}/{@code response.failed} 三个——
 * {@code response.failed} 与 {@code error} 绝不被改写成 completed；EOF 之前没见到终态即判 ABORTED（以错误终止本流，
 * 从不伪造终态帧），终态之后到达的帧一律丢弃。单帧受 {@code yuheng.llm.max-frame-bytes} 约束、上游预取不超过 1 帧、
 * 每个上游 {@link DataBuffer} 恰好在字节被取出后释放一次、自己产出的帧交给写出方释放且被取消时就地释放、
 * 下游取消传播为关闭上游流。原生错误对象只有顶层 {@code error} 一个键，内层 {@code message}/{@code type}/
 * {@code param}/{@code code} 完全由 {@link LlmInvocationException} 的四个安全字段驱动，永不套 admin 的
 * {@code code}/{@code data} 信封。{@link #exchange} 是唯一编排入口：用渠道 baseUrl、{@code secretRef}
 * （只在 {@code yuheng.llm.secrets-root} 之下解析，规范化后必须仍在根内）与四段超时预算就地构造 JDK 原生
 * {@link HttpClient}（HTTP/1.1、绝不跟随跳转、绝不把上游 3xx 当作跳转）发起<b>同协议</b>调用，
 * 只有验证过<b>一个完整合法帧</b>（非流为整份可解码成功文档）才进入 COMMITTED 并写回 {@code routed}；
 * 提交前失败一律以原生状态抛出（配置不成立/无同协议渠道 503、执行器饱和 {@code TaskRejectedException} 之类
 * {@link RejectedExecutionException} 429、上游非 2xx 按原生状态交 {@link #encodeError} 出网、结构非法 502、
 * 超时 504），提交后禁止换模型、禁止重试、禁止改写 HTTP 状态。本方法从不在事务或锁内等待上游：配置只读快照
 * 事务早在 {@code MpLlmConfigurationRepository} 返回时结束。日志只出现 alias、协议、channelKey、状态与事件计数。
 *
 * English summary: {@code OpenAiResponsesProtocolStrategy} is the {@code POST /v1/responses} face of the four named
 * {@link LlmProtocolStrategy} adapters (bean name {@code openAiResponsesProtocolStrategy}, filed by the
 * {@link LlmProtocolEnum#OPENAI_RESPONSES} entry of {@code llmProtocolStrategyRegistry}) and its only job is
 * <b>same-protocol fidelity</b>. On the request side it verifies the known required fields ({@code model} plus a
 * non-empty {@code input}, either a string or 1–1000 items, optional {@code instructions}, and this protocol's own
 * output budget {@code max_output_tokens} — Chat's {@code max_tokens}/{@code max_completion_tokens} are a 400 here
 * because a budget may never be replaced across protocols), pins {@code store} to {@code false} unconditionally
 * (absent becomes false, an explicit true is a 400 since the gateway never leaves client content in an upstream
 * account), rejects managed-session self-assertions ({@code background=true}, {@code conversation},
 * {@code previous_response_id}) and every egress or credential self-assertion
 * ({@code upstream_url}/{@code base_url}/{@code host}/{@code credential}/{@code token}), and rejects request
 * extensions whose capability the route did not declare (tools/tool_choice/parallel_tool_calls → FUNCTION_TOOLS,
 * text → STRUCTURED_OUTPUT, reasoning/include/encrypted content → REASONING, image input parts → VISION); it then
 * rewrites only {@code model} into {@code route.getUpstreamModel()}`, keeping every other structure, numeric
 * representation and array order byte-for-byte inside {@code yuheng.llm.max-request-bytes}. {@code function_call} and
 * {@code function_call_output} items replay their opaque {@code call_id} verbatim with enforced pairing (an output
 * must reference a call present in the same document) and {@code reasoning} items are never decrypted nor printed.
 * On the response side it preserves the typed {@code output} items (message/function_call/reasoning) together with
 * {@code status}/{@code usage}/{@code error}/{@code incomplete_details} and every unknown-but-safe field, never
 * downgrading Responses output into Chat {@code choices}, and writes only {@code model} back to the client alias; a
 * missing {@code status} or {@code output} array, a {@code completed} with zero items, a {@code function_call}
 * without a {@code call_id} or an unpaired {@code function_call_output} is a 502
 * {@code upstream_protocol_error} rather than an empty success. The streaming face parses upstream SSE into the
 * Responses event family: each frame must carry a textual {@code type} and a strictly increasing
 * {@code sequence_number} (order is upstream order, so {@code response.output_text.delta} fragments are never
 * re-ordered), legitimate unknown events pass through with only a low-cardinality count, and the only terminal
 * markers are {@code response.completed}/{@code response.incomplete}/{@code response.failed} — a
 * {@code response.failed} or {@code error} is never dressed up as completed. An EOF before a terminal marker is
 * ABORTED (the stream ends in an error, never a fabricated terminal frame) and frames after the terminal are
 * dropped. Each frame stays under {@code yuheng.llm.max-frame-bytes}, upstream prefetch never exceeds one frame,
 * every incoming {@link DataBuffer} is released exactly once after its bytes are taken, frames this face produces are
 * released by the writer or in place when cancelled, and a downstream cancellation becomes an upstream stream close.
 * The native error object has {@code error} as its only top-level key with {@code message}/{@code type}/{@code param}/
 * {@code code} driven solely by the four safe fields of {@link LlmInvocationException}, never an admin
 * {@code code}/{@code data} envelope. {@link #exchange} is the single orchestration entry: it builds a JDK-native
 * {@link HttpClient} locally from the channel's base URL, its {@code secretRef} (resolved only under
 * {@code yuheng.llm.secrets-root}, and only while the normalized path provably stays inside that root) and its four
 * timeout budgets (HTTP/1.1, never following a redirect, never treating an upstream 3xx as one) and issues a
 * <b>same-protocol</b> call, entering COMMITTED and completing {@code routed} only after <b>one complete valid
 * frame</b> (for unary, one fully decodable success document) has been validated. Pre-commit failures leave natively
 * (503 for damaged configuration or a missing same-protocol channel, 429 for a saturated
 * {@link RejectedExecutionException} such as {@code TaskRejectedException}, the native upstream status through
 * {@link #encodeError} for a non-2xx, 502 for a damaged structure, 504 for a timeout); after commit this face may not
 * switch model, retry nor rewrite the HTTP status. It never waits on upstream inside a transaction or a lock, because
 * the read-only configuration snapshot transaction closed when {@code MpLlmConfigurationRepository} returned. Logs
 * carry only the alias, protocol, channelKey, status and event counts.
 *
 * 用法 / Usage: 由 {@code LlmApiController} 经 {@code llmProtocolStrategyRegistry} 按
 * {@link LlmProtocolEnum#OPENAI_RESPONSES} 取 bean 名 {@code openAiResponsesProtocolStrategy} 后限定注入；
 * 顺序固定为 {@code invoke(command)} 路由 → {@code exchange(command, route, routed)} 出网，四个编解码操作同时供
 * {@code LlmProtocolContractTest} 在无真实模型的情况下直接钉住原生 wire shape。构造合同与其余三个 Strategy 完全一致：
 * 单例、两个 {@code private final} 协作者（按名限定的 {@link LlmGatewayProperties} 与 {@code jacksonObjectMapper}）、
 * {@link RequiredArgsConstructor} 注入，不新增任何 bean、协作者类型或工具类。
 * / Resolved by {@code LlmApiController} through {@code llmProtocolStrategyRegistry} under
 * {@link LlmProtocolEnum#OPENAI_RESPONSES} and qualified by the bean name {@code openAiResponsesProtocolStrategy}:
 * {@code invoke(command)} routes first and {@code exchange(command, route, routed)} leaves natively, while the four
 * codec operations are additionally driven straight by {@code LlmProtocolContractTest} to pin the native wire shape
 * without a live model. The construction contract is identical to the other three Strategies: a singleton with two
 * {@code private final} collaborators (the name-qualified {@link LlmGatewayProperties} and
 * {@code jacksonObjectMapper}) injected by {@link RequiredArgsConstructor}, adding no bean, collaborator type or
 * helper class.
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@Component("openAiResponsesProtocolStrategy")
public class OpenAiResponsesProtocolStrategy implements LlmProtocolStrategy {

    /** 中文说明：本协议面的上游端点路径段：Responses 只对应 {@code /responses}（版本段属于渠道 baseUrl）。 English summary: this face's endpoint segment: Responses maps to {@code /responses}, the version segment belongs to the channel base URL. */
    private static final String RESPONSES_PATH = "responses";

    /** 中文说明：baseUrl 没有路径段时补的默认版本前缀，避免把请求打到主机根路径。 English summary: the version prefix added when the base URL carries no path segment, so a request never lands on the host root. */
    private static final String DEFAULT_VERSION_PREFIX = "/v1";

    /** 中文说明：帧累加器在单帧上限之外预留的空行终止符余量。 English summary: the slack the frame accumulator keeps beyond the per-frame ceiling for the blank-line terminator. */
    private static final int FRAME_TERMINATOR_SPARE = 4;

    /** 中文说明：一次上游读取的块大小，决定单条流在途字节数量级（远小于任何帧上限）。 English summary: the upstream read block size, bounding in-flight bytes far below any frame ceiling. */
    private static final int READ_CHUNK_BYTES = 8 * 1024;

    /** 中文说明：{@code input} items 的数量上限，逐字对应 Spec §9.0.1 的 ≤1000 Responses items。 English summary: the {@code input} item ceiling, matching the ≤ 1000 Responses items of Spec §9.0.1 verbatim. */
    private static final int MAX_INPUT_ITEMS = 1_000;

    /** 中文说明：{@code instructions} 字符上限，逐字对应 Spec §9.2.29 的 0–16000。 English summary: the {@code instructions} character ceiling, matching 0–16000 of Spec §9.2.29. */
    private static final int MAX_INSTRUCTIONS_CHARS = 16_000;

    /** 中文说明：{@code tools} 数组上限，逐字对应 Spec §9.0.1 的 0–64。 English summary: the {@code tools} array ceiling, matching 0–64 of Spec §9.0.1. */
    private static final int MAX_TOOLS = 64;

    /** 中文说明：route 允许的输出预算上限（当前托管默认 8192），超限即 400，不擅自夹紧。 English summary: the route-allowed output budget ceiling (currently 8192), exceeded means 400 rather than a silent clamp. */
    private static final long MAX_OUTPUT_TOKENS = 8_192L;

    /** 中文说明：函数名的稳定形态：1–64 个 {@code [A-Za-z0-9_-]}。 English summary: the stable function-name shape, 1–64 {@code [A-Za-z0-9_-]} characters. */
    private static final Pattern FUNCTION_NAME = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    /** 中文说明：可安全回显的 {@code Retry-After} 形态：至多六位秒数，其它一律剥离。 English summary: the only safely echoable {@code Retry-After} shape, at most six seconds digits, anything else is stripped. */
    private static final Pattern RETRY_AFTER_SECONDS = Pattern.compile("^\\d{1,6}$");

    /** 中文说明：{@code secretRef} 指向文件的大小上限，凭据文件极小，越界即视为配置不成立。 English summary: the size ceiling of the file a {@code secretRef} points at; credential files are tiny, so anything larger is damaged configuration. */
    private static final long MAX_SECRET_BYTES = 8 * 1024L;

    /** 中文说明：SSE 写出用的字段前缀，以及解析用的四个字段名与行终止符常量。 English summary: the field prefix this face writes with, plus the field names and the line terminator the parser is built on. */
    private static final String DATA_FIELD = "data:";

    private static final String DATA_NAME = "data";

    private static final String EVENT_NAME = "event";

    private static final String ID_NAME = "id";

    private static final String RETRY_NAME = "retry";

    private static final String LINE_FEED_TEXT = "\n";

    private static final byte LINE_FEED = 10;

    private static final byte CARRIAGE_RETURN = 13;

    /** 中文说明：Responses 的三个合法终态事件；除此之外 EOF 一律 ABORTED，绝不补发伪终态帧。 English summary: the three legitimate Responses terminal events; any other EOF is ABORTED and a fabricated terminal frame never happens. */
    private static final String RESPONSE_COMPLETED = "response.completed";

    private static final String RESPONSE_INCOMPLETE = "response.incomplete";

    private static final String RESPONSE_FAILED = "response.failed";

    private static final Set<String> TERMINAL_EVENTS =
            Set.of(RESPONSE_COMPLETED, RESPONSE_INCOMPLETE, RESPONSE_FAILED);

    /** 中文说明：本版识别的 Responses 事件族，只用于「合法未知事件」计数，不用于过滤转发。 English summary: the event family this version recognizes, used only to count legitimate unknown events and never to filter forwarding. */
    private static final Set<String> KNOWN_EVENTS = Set.of("response.created", "response.in_progress",
            "response.output_item.added", "response.content_part.added", "response.output_text.delta",
            "response.output_text.done", "response.content_part.done", "response.output_item.done",
            "response.function_call_arguments.delta", "response.function_call_arguments.done",
            "response.refusal.delta", "response.refusal.done", "response.error", "error",
            RESPONSE_COMPLETED, RESPONSE_INCOMPLETE, RESPONSE_FAILED);

    /** 中文说明：{@code input} items 允许的类型判别值；{@code message} 简写允许只有 role 没有 type。 English summary: the accepted {@code input} item discriminators, where the {@code message} shorthand may carry only a role. */
    private static final Set<String> INPUT_ITEM_TYPES =
            Set.of("message", "function_call", "function_call_output", "reasoning", "item");

    /** 中文说明：{@code input} 消息 item 允许的角色，逐字对应 Spec §9.2.29。 English summary: the roles an {@code input} message item may carry, verbatim from Spec §9.2.29. */
    private static final Set<String> INPUT_MESSAGE_ROLES = Set.of("user", "assistant", "system", "developer");

    /** 中文说明：{@code output} items 允许的类型判别值；未知类型不拒绝（供应商扩展保留），但必须是对象且自带 type。 English summary: the accepted {@code output} item discriminators; an unknown type is not rejected (vendor extensions stay) but must still be an object carrying its own type. */
    private static final Set<String> OUTPUT_ITEM_TYPES =
            Set.of("message", "function_call", "function_call_output", "reasoning");

    /** 中文说明：{@code tool_choice} 的字符串枚举值。 English summary: the string enumeration of {@code tool_choice}. */
    private static final Set<String> TOOL_CHOICE_MODES = Set.of("none", "auto", "required");

    /** 中文说明：任何情况下都不允许出现的顶层字段：出网/凭据自证与托管会话状态。 English summary: top-level fields that are never allowed, covering egress and credential self-assertions plus managed session state. */
    private static final Set<String> FORBIDDEN_REQUEST_FIELDS = Set.of("upstream_url", "upstreamUrl", "base_url",
            "baseUrl", "url", "endpoint", "host", "api_key", "apiKey", "key", "credential", "credentials", "secret",
            "secret_ref", "secretRef", "token", "authorization", "proxy", "target", "provider", "api_base", "apiBase",
            "previous_response_id", "conversation", "conversation_id");

    /** 中文说明：无需能力声明即可透传的已知 Responses 字段。 English summary: known Responses fields that pass through without any capability declaration. */
    private static final Set<String> KNOWN_REQUEST_FIELDS = Set.of("model", "input", "instructions",
            "max_output_tokens", "stream", "store", "background", "temperature", "top_p", "metadata", "truncation",
            "user", "service_tier", "top_logprobs", "max_tool_calls", "prompt_cache_key");

    /** 中文说明：需要 route 显式声明能力才允许出现的请求扩展；未声明即 400 {@code unsupported_parameter}。 English summary: request extensions that need an explicitly declared route capability; without it the answer is 400 {@code unsupported_parameter}. */
    private static final Map<String, LlmCapabilityEnum> CAPABILITY_GATED_FIELDS = Map.of("tools",
            LlmCapabilityEnum.FUNCTION_TOOLS, "tool_choice", LlmCapabilityEnum.FUNCTION_TOOLS,
            "parallel_tool_calls", LlmCapabilityEnum.FUNCTION_TOOLS, "text", LlmCapabilityEnum.STRUCTURED_OUTPUT,
            "response_format", LlmCapabilityEnum.STRUCTURED_OUTPUT, "reasoning", LlmCapabilityEnum.REASONING,
            "reasoning_effort", LlmCapabilityEnum.REASONING, "include", LlmCapabilityEnum.REASONING);

    /** 中文说明：堆外无关的堆内缓冲区工厂，只为出网帧产出 {@link DataBuffer}，不是 bean。 English summary: a heap buffer factory that only produces outgoing {@link DataBuffer} frames; it is not a bean. */
    private static final DefaultDataBufferFactory DATA_BUFFER_FACTORY = new DefaultDataBufferFactory();

    /** 中文说明：本进程的字节/帧/次数边界与密钥挂载根；与其余三个 Strategy 同一份按名限定的配置 bean。 English summary: this process's byte/frame/attempt boundaries and the secret mount root, the same name-qualified configuration bean the other three Strategies use. */
    @Qualifier(LlmGatewayProperties.BEAN_NAME)
    private final LlmGatewayProperties gatewayProperties;

    /** 中文说明：容器唯一的 Jackson 栈，协议文档载体就是它的 {@link ObjectNode}，禁止另起第二套 JSON 实现。 English summary: the container's single Jackson stack whose {@link ObjectNode} is the protocol document carrier, so no second JSON implementation may appear. */
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：本实现唯一负责的协议：{@link LlmProtocolEnum#OPENAI_RESPONSES}。注册表据此把本 bean 归入 Responses
     * 条目，若与注册键不符即启动失败，绝不回落 Chat。
     * English summary: The single protocol this implementation owns, {@link LlmProtocolEnum#OPENAI_RESPONSES}, which the
     * registry uses to file this bean under the Responses entry, failing startup rather than falling back to Chat when
     * the registry key disagrees.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OpenAiResponsesProtocolStrategy.protocol()}。
     * @return 返回 OPENAI_RESPONSES；returns OPENAI_RESPONSES.
     */
    @Override
    public LlmProtocolEnum protocol() {
        return LlmProtocolEnum.OPENAI_RESPONSES;
    }

    /**
     * 中文说明：职责①请求编码。深拷贝客户端文档后逐段核对：{@code model} 必须与命令 alias 一致（缺失则按命令补齐，
     * 冲突即 400）、{@code input} 必须是非空 string 或 1–1000 个 items、{@code instructions} 必须是不超过 16000
     * 字符的文本、输出预算只能是正整数 {@code max_output_tokens}（≤8192）且 Chat 预算字段出现即 400、
     * {@code stream} 与命令位一致、{@code store} 缺失补 false 而显式 true 拒绝、{@code background=true} 拒绝、
     * tools/tool_choice 形状与函数名引用合法、items 里的 {@code function_call} 必须自带非空 {@code call_id}/
     * {@code name}/{@code arguments} 字符串且 {@code function_call_output} 的 {@code call_id} 必须配对同一文档内的
     * 调用、{@code reasoning} item 原样回放。任何出网/凭据自证字段、托管会话字段与未声明能力的扩展都在改写之前被拒；
     * 最后只把 {@code model} 换成 {@code route.getUpstreamModel()}、把 {@code store} 钉成 false，
     * 且仅在命令为流式而文档缺字段时补 {@code stream:true}（存在时已由 stream 位核对保证同值，绝不把流式请求改写成
     * 非流式），整份序列化后的文档必须在 {@code yuheng.llm.max-request-bytes} 之内。
     * English summary: Duty (1) request encoding. On a deep copy of the client document it verifies section by section:
     * {@code model} must agree with the command alias (absent is completed from the command, a conflict is a 400),
     * {@code input} must be a non-empty string or 1–1000 items, {@code instructions} must be text within 16000
     * characters, the output budget may only be a positive integral {@code max_output_tokens} (≤ 8192) so a Chat budget
     * field is a 400, {@code stream} must equal the command flag, {@code store} defaults to false while an explicit true
     * is refused, {@code background=true} is refused, tools/tool_choice shapes and function references must hold, an
     * {@code function_call} item must carry a non-blank {@code call_id}/{@code name} plus a string {@code arguments}
     * while a {@code function_call_output} must pair with a call from the same document, and {@code reasoning} items
     * replay verbatim. Every egress or credential self-assertion, managed-session field and extension whose capability
     * the route did not declare is rejected before any rewrite; finally only {@code model} becomes
     * {@code route.getUpstreamModel()}, {@code store} is pinned to false and {@code stream: true} is completed solely
     * when the command streams and the document omits the field (a present field already equals the command bit, so a
     * streaming call is never rewritten into a unary one), with the serialized document inside
     * {@code yuheng.llm.max-request-bytes}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code encodeRequest(command, route)}，返回体直接作为上游请求正文；
     * 不就地改写 {@code command.getPayload()}，同一次调用的下一次安全尝试因此仍看到原始文档。
     * / the result is the upstream request body as-is; {@code command.getPayload()} is never mutated in place, so the
     * next safe attempt of the same invocation still sees the original document.
     * @param command 参数 已校验的调用命令，提供 alias、stream 位与原生请求文档；parameter the validated command carrying the alias, the stream bit and the native request document.
     * @param route 参数 已选定的候选 route，提供唯一允许出网的 upstreamModel 与能力声明；parameter the selected candidate route supplying the only upstreamModel allowed to egress plus its capability declarations.
     * @return 返回 协议原生请求文档，除 model/store/stream 外与客户端提交同构；returns the native request document, structurally identical to what the client submitted apart from model, store and stream.
     * @throws LlmInvocationException 400 {@code unsupported_parameter}（字段缺失/非法、未声明扩展、自证出网凭据、托管会话、预算跨协议）、413 {@code request_too_large}；the native status travels with the error.
     */
    @Override
    public ObjectNode encodeRequest(LlmInvocationCommandDTO command, LlmModelSnapshotBO.RouteBO route) {
        ObjectNode request = command.getPayload().deepCopy();
        Set<LlmCapabilityEnum> declared = route.getCapabilities();
        verifyModel(command, request);
        verifyStream(command, request);
        rejectForbiddenFields(request);
        rejectUndeclaredExtensions(request, declared);
        verifyInstructions(request);
        verifyOutputBudget(request);
        verifyStorePolicy(request);
        verifyInput(request, declared);
        verifyTools(request);
        verifyToolChoice(request);
        boolean wantsStream = Boolean.TRUE.equals(command.getStream());
        request.put("model", route.getUpstreamModel());
        request.put("store", false);
        if (wantsStream && !request.has("stream")) {
            request.put("stream", true);
        }
        serialize(request, command);
        log.debug("llm responses request encoded, alias={} protocol={} channelKey={} stream={}", command.getModel(),
                protocol(), route.getChannelKey(), wantsStream);
        return request;
    }

    /**
     * 中文说明：职责②单播解码/透传。只处理成功文档：要求 {@code status} 为文本且 {@code output} 为数组，
     * {@code completed} 但零 item 视为「空成功」而拒绝，逐 item 核对对象形态与 {@code type}，
     * {@code function_call} 必须自带非空 {@code call_id}（并保留 id/name/arguments/status 原样），
     * {@code function_call_output} 的 {@code call_id} 必须能在同一文档的调用里找到，
     * {@code reasoning} item 与一切未知但安全字段（含 {@code usage} 缺失时的 null、{@code error}、
     * {@code incomplete_details}）一律原样保留，绝不缩减成自定义 VO、绝不重排数组、绝不把输出降格成 Chat 的
     * {@code choices}；最后仅把 {@code model} 回写为客户端 alias（不存在则不伪造）。
     * English summary: Duty (2) unary decoding and pass-through. Success documents only: {@code status} must be textual
     * and {@code output} an array, a {@code completed} with zero items is refused as an empty success, every item is
     * checked for object shape and a {@code type}, a {@code function_call} must carry a non-blank {@code call_id} (with
     * id/name/arguments/status kept as-is) and a {@code function_call_output} must reference a call present in the same
     * document, while {@code reasoning} items and every unknown-but-safe field (a null or absent {@code usage} is never
     * fabricated into zero, {@code error} and {@code incomplete_details} included) pass through untouched — nothing is
     * reduced into a bespoke VO, no array is re-ordered and no output is downgraded into Chat {@code choices}; finally
     * only {@code model} is written back to the client alias, which is never invented when absent.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code decodeUnaryResponse(command, upstreamBody)}；非 2xx 上游响应不属于本操作，
     * 由 {@link #exchange} 交给 {@link #encodeError}。/ a non-2xx upstream document is not this operation's business and
     * is handed by {@link #exchange} to {@link #encodeError}.
     * @param command 参数 本次调用命令，提供回写给客户端的 alias；parameter this invocation's command, supplying the alias to write back.
     * @param upstreamBody 参数 上游成功响应的原生 JSON 文档；parameter the native JSON document of the successful upstream response.
     * @return 返回 保真后的原生响应文档，model 已恢复为客户端 alias；returns the fidelity-preserving native document with {@code model} restored to the alias.
     * @throws LlmInvocationException 502 {@code upstream_protocol_error}（必需结构缺失、item 形态非法、call_id 配不上对）；raised when a required structure is absent, an item is malformed or a call_id does not pair.
     */
    @Override
    public ObjectNode decodeUnaryResponse(LlmInvocationCommandDTO command, ObjectNode upstreamBody) {
        ObjectNode response = upstreamBody.deepCopy();
        JsonNode status = response.get("status");
        if (status == null || !status.isTextual() || StringUtils.isBlank(status.asText())) {
            throw protocolError("response", "The upstream response carries no usable status");
        }
        JsonNode output = response.get("output");
        if (output == null || !output.isArray()) {
            throw protocolError("output", "The upstream response carries no typed output array");
        }
        if ("completed".equals(status.textValue()) && output.isEmpty()) {
            throw protocolError("output", "The upstream response completed without a single output item");
        }
        Set<String> called = new HashSet<>();
        for (JsonNode item : output) {
            validateOutputItem(item);
            if ("function_call".equals(item.path("type").textValue())) {
                called.add(item.path("call_id").textValue());
            }
        }
        for (JsonNode item : output) {
            if ("function_call_output".equals(item.path("type").textValue())
                    && !called.contains(item.path("call_id").textValue())) {
                throw protocolError("call_id", "A function call output does not pair with any function call item");
            }
        }
        JsonNode model = response.get("model");
        if (model != null && model.isTextual()) {
            response.put("model", command.getModel());
        }
        log.debug("llm responses unary decoded, alias={} protocol={} items={}", command.getModel(), protocol(),
                output.size());
        return response;
    }

    /**
     * 中文说明：职责③流式帧解码与编码。入参是上游<b>原始字节流</b>，绝不假定「一个缓冲区恰好一帧」：字节先喂进容量
     * 恒为「单帧上限 + 终止符余量」的累加器，再由 {@link #nextFrame} 与 {@link #sseEvent} 按 SSE 文法重建帧（空行收束
     * 一帧，{@code \n} 与 {@code \r\n}（及孤立 {@code \r}）都结束一行，{@code data:} 行去掉前缀与其后的一个可选空格，
     * {@code event:}/{@code id:}/{@code retry:} 与 {@code :} 注释行按 SSE 处理，一帧内多段载荷以换行拼接），因此一帧
     * 跨多个缓冲区、或多个缓冲区并成一段字节都切得出来。每帧载荷必须是带文本 {@code type} 的 JSON 对象，且（若携带）
     * {@code sequence_number} 非负并<b>严格递增</b>，否则判 502 {@code upstream_protocol_error}；解析成功的帧只把顶层
     * {@code model} 与 {@code response.model} 回写为客户端 alias，随后按同协议重新出帧（{@code event:<名>} 取上游
     * {@code event:} 行、缺该行时取已验证的 {@code type}，加 {@code data:<json>} 加空行），因此
     * {@code response.output_text.delta} 片段与工具参数增量顺序逐帧保持。
     * 终态只有 {@code response.completed}/{@code response.incomplete}/{@code response.failed}：见到即转发并收束本流、
     * 其后到达的帧丢弃；EOF 之前没见到终态即以 {@code upstream_protocol_error} 终止（ABORTED），绝不补发伪终态帧。
     * 每帧字节受 {@code yuheng.llm.max-frame-bytes} 约束（累加器容量即上限加终止符余量，超限即失败，不缓冲整条流），
     * 上游经 {@code limitRate(1)} + {@code concatMap(...,1)} 严格一帧一取；上游 {@link DataBuffer} 的所有权只有<b>一处</b>
     * ——{@link #takeBytes} 取空字节后恰好释放一次，其后在途的都是 {@code byte[]}，故 Reactor 对源值的例行丢弃与
     * {@code doOnDiscard} 只会命中「尚未取字节」的缓冲区，complete/error/cancel 三条路径下每块都恰好释放一次；
     * 自己产出的帧交给写出方释放、序列被取消或提前收束时未交付的帧就地释放，取消传播为关闭上游流。
     * English summary: Duty (3) streaming frame decoding and encoding. The input is the upstream's <b>raw byte flux</b>
     * and "one buffer holds exactly one frame" is never assumed: bytes go into an accumulator sized to one frame ceiling
     * plus terminator slack, and {@link #nextFrame} with {@link #sseEvent} rebuild the frames through the SSE grammar (a
     * blank line closes a frame, {@code \n} and {@code \r\n} (and a lone {@code \r}) end a line, a {@code data:} line
     * loses its prefix plus one optional following space, {@code event:}/{@code id:}/{@code retry:} and {@code :} comment
     * lines are handled per SSE, and several payload segments of one frame join with a newline), so a frame split across
     * buffers and several buffers coalesced into one stretch both cut correctly. Every payload must be a JSON object with
     * a textual {@code type} and, when present, a non-negative <b>strictly increasing</b> {@code sequence_number},
     * otherwise it is a 502 {@code upstream_protocol_error}. A parsed frame only gets its top-level {@code model} and
     * {@code response.model} rewritten to the client alias and is then re-framed in the same protocol
     * ({@code event:<name>} taken from the upstream {@code event:} line, falling back to the validated {@code type} when
     * that line is absent, plus {@code data:<json>} plus a blank line), so {@code response.output_text.delta} fragments
     * and tool-argument deltas keep their per-frame order. The only
     * terminal markers are {@code response.completed}/{@code response.incomplete}/{@code response.failed}: seeing one
     * forwards it and closes the stream while any later frame is dropped; an EOF without one ends the sequence with
     * {@code upstream_protocol_error} (ABORTED) and a fabricated terminal frame never happens. Frame bytes stay under
     * {@code yuheng.llm.max-frame-bytes} (the accumulator capacity is exactly that ceiling plus terminator slack, so an
     * oversized frame fails instead of a whole-stream buffer), upstream is drained strictly one frame at a time through
     * {@code limitRate(1)} and {@code concatMap(...,1)}, and an incoming {@link DataBuffer} has exactly <b>one</b> owner:
     * {@link #takeBytes} releases it once right after taking its bytes, so everything in flight afterwards is a
     * {@code byte[]} and neither Reactor's routine discard of a mapped source value nor {@code doOnDiscard} can ever
     * release it twice — every buffer is released exactly once on complete, on error and on cancel alike. Frames this
     * face produces are released by the writer or in place when the sequence is cancelled or
     * closed early, and a cancellation closes the upstream stream.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code encodeStream(command, upstreamFrames)}，交给
     * {@code LlmServletStreamComponent} 逐帧写出。/ hand the result to {@code LlmServletStreamComponent} frame by frame.
     * @param command 参数 本次调用命令，提供逐帧回写 alias 的目标名；parameter this invocation's command, supplying the alias each frame must carry.
     * @param upstreamFrames 参数 上游响应体的原始字节流（帧与缓冲区不对齐）；parameter the raw byte flux of the upstream body, not aligned to frames.
     * @return 返回 同协议客户端字节流，含且仅含一个终态帧；returns the same-protocol client byte flux carrying exactly one terminal frame.
     * @throws LlmInvocationException 提交前：502 {@code upstream_protocol_error}、504 {@code upstream_timeout}；提交后只能终止本流，不得改状态。/ pre-commit only: after commit a failure may only end the stream and never change the status.
     */
    @Override
    public Flux<DataBuffer> encodeStream(LlmInvocationCommandDTO command, Flux<DataBuffer> upstreamFrames) {
        int ceiling = gatewayProperties.getMaxFrameBytes();
        String alias = command.getModel();
        return Flux.defer(() -> {
            ByteBuffer accumulator = ByteBuffer.allocate(ceiling + FRAME_TERMINATOR_SPARE);
            AtomicReference<Long> lastSequence = new AtomicReference<>();
            AtomicBoolean terminalSeen = new AtomicBoolean();
            AtomicInteger events = new AtomicInteger();
            AtomicInteger unknownEvents = new AtomicInteger();
            // 终态收束按「第几帧」而不是「标志位」判定：一个缓冲区里可能同时装着终态之前的若干帧，
            // 用共享标志会把它们连同字节一起悄悄丢掉，只有按产出/下发计数才能在终态帧本身之后才收束。
            AtomicInteger framesProduced = new AtomicInteger();
            AtomicInteger framesDelivered = new AtomicInteger();
            AtomicInteger stopAfterFrame = new AtomicInteger(Integer.MAX_VALUE);
            return upstreamFrames
                    .limitRate(1)
                    // 所有权在此分道：缓冲区只在这一步被取空并恰好释放一次，其后各段算子搬运的都是 byte[]，
                    // 所以 Reactor 在取消或出错时对「源值」的例行丢弃永远不会二次释放同一个上游缓冲区。
                    .map(OpenAiResponsesProtocolStrategy::takeBytes)
                    .concatMap(chunk -> {
                        List<DataBuffer> frames = new ArrayList<>(2);
                        try {
                            feed(accumulator, chunk, ceiling, frame -> {
                                if (terminalSeen.get()) {
                                    return false;
                                }
                                ObjectNode event = parseEvent(frame.data(), alias, lastSequence);
                                String type = event.path("type").textValue();
                                events.incrementAndGet();
                                frames.add(DATA_BUFFER_FACTORY.wrap(
                                        encodeFrame(event, frame.eventOf(type), ceiling)));
                                int produced = framesProduced.incrementAndGet();
                                if (TERMINAL_EVENTS.contains(type)) {
                                    terminalSeen.set(true);
                                    stopAfterFrame.set(produced);
                                } else if (!KNOWN_EVENTS.contains(type)) {
                                    unknownEvents.incrementAndGet();
                                }
                                return terminalSeen.get();
                            });
                        } catch (RuntimeException failure) {
                            frames.forEach(DataBufferUtils::release);
                            throw failure;
                        }
                        return Flux.fromIterable(frames);
                    }, 1)
                    .concatWith(Mono.defer(() -> terminalSeen.get()
                            ? Mono.<DataBuffer>empty()
                            : Mono.<DataBuffer>error(protocolError("response",
                                    "The upstream stream ended before its terminal Responses event"))))
                    .takeUntil(frame -> framesDelivered.incrementAndGet() == stopAfterFrame.get())
                    .doOnComplete(() -> log.debug(
                            "llm responses stream encoded, alias={} protocol={} events={} unknownEvents={}", alias,
                            protocol(), events.get(), unknownEvents.get()))
                    .doOnDiscard(DataBuffer.class, DataBufferUtils::release);
        });
    }

    /**
     * 中文说明：职责④原生错误编码。OpenAI Responses 面的错误对象只有顶层 {@code error} 一个键，内层恰好四个字段：
     * {@code message} 取异常自带的安全说明，{@code type} 由本面按状态自行映射（400/413/422→invalid_request_error、
     * 401→authentication_error、403→permission_error、404→not_found_error、429→rate_limit_error、
     * 5xx→server_error、其余 4xx→invalid_request_error），{@code param} 取异常参数名（无则显式 null），
     * {@code code} 取稳定机器码。本方法不产生 admin 的 {@code code}/{@code data} 信封，也永不把 token、密钥、
     * baseUrl、secretRef、upstreamModel 或上游正文片段放进任何字段。
     * English summary: Duty (4) native error encoding. A Responses error object has {@code error} as its only top-level
     * key and exactly four inner fields: {@code message} is the carrier's own safe description, {@code type} is mapped
     * by this face from the status (400/413/422 → invalid_request_error, 401 → authentication_error,
     * 403 → permission_error, 404 → not_found_error, 429 → rate_limit_error, 5xx → server_error, any other 4xx →
     * invalid_request_error), {@code param} is the carrier's parameter name (an explicit null when absent) and
     * {@code code} is the stable machine code. This operation emits no admin {@code code}/{@code data} envelope and
     * never lets a token, secret, base URL, secretRef, upstream model or an upstream payload fragment enter a field.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code encodeError(error)}，状态取 {@code error.getStatus()}；
     * 未认证与越权的原生错误同样由本方法产出。/ the status is {@code error.getStatus()}, and the native errors for
     * unauthenticated and unauthorized requests come from this same method.
     * @param error 参数 已脱敏的原生错误载体；parameter the masked native error carrier.
     * @return 返回 协议原生错误文档；returns the protocol-native error document.
     */
    @Override
    public ObjectNode encodeError(LlmInvocationException error) {
        ObjectNode envelope = objectMapper.createObjectNode();
        ObjectNode detail = envelope.putObject("error");
        detail.put("message", error.getMessage());
        detail.put("type", nativeErrorType(error.getStatus()));
        detail.put("param", error.getParam());
        detail.put("code", error.getCode());
        return envelope;
    }

    /**
     * 中文说明：唯一的编排入口。先 {@link #requireUsableChannel} 就地失败关闭（引擎未启用、渠道缺失/停用、
     * 渠道协议不是 OPENAI_RESPONSES、route 能力不覆盖命令要求、baseUrl 形态不合法、密钥根未配置 → 503），
     * 再 {@link #encodeRequest}+序列化（400/413），凭据只按 {@code secretRef} 在
     * {@code yuheng.llm.secrets-root} 之下解析（规范化后必须仍在根内，非普通文件/过大/空白即 503），
     * 然后<b>就地</b>用渠道的 connect 预算构造 JDK 原生 {@link HttpClient}（HTTP/1.1、绝不跟随跳转）并按 header/
     * idle/total 三段预算发一次同协议请求。非 2xx 上游按映射后的原生状态经 {@link #encodeError} 写回
     * {@code routed}（只回显形态安全的 {@code Retry-After} 秒数，绝不读上游正文）；2xx 非流在 {@code max-request-bytes}
     * 之内取回整份文档并 {@link #decodeUnaryResponse}；2xx 流式必须先在本方法内读出并验证<b>一个完整合法帧</b>才
     * 写回 publisher —— 这一步就是提交屏障：屏障之前任何失败以原生状态抛出（429/502/503/504 等）由调用方决定
     * 下一次安全尝试，屏障之后禁止换模型、禁止重试、禁止改写 HTTP 状态、禁止伪造终态帧，只能终止本流。
     * 本方法从不在事务或锁内等待上游：只读配置快照事务早已结束，等待只受渠道超时预算与看门狗关闭约束。
     * English summary: The single orchestration entry. {@link #requireUsableChannel} fails closed first (engine disabled,
     * missing or disabled channel, a channel protocol that is not OPENAI_RESPONSES, route capabilities not covering the
     * command's required ones, an unusable base URL, or a missing CLOUD credential/root → 503; a null secretRef is valid
     * only for an unauthenticated LOCAL channel), then
     * {@link #encodeRequest} plus serialization runs (400/413), the credential is resolved from {@code secretRef} under
     * {@code yuheng.llm.secrets-root} only (the normalized path must provably stay inside that root, and a
     * non-regular, oversized or blank file is a 503), and a JDK-native {@link HttpClient} is built <b>locally</b> from
     * the channel's connect budget (HTTP/1.1, never following a redirect) to issue one same-protocol request under the
     * header/idle/total budgets. A non-2xx upstream leaves through {@link #encodeError} at the mapped native status into
     * {@code routed} (only a shape-safe {@code Retry-After} in seconds is echoed and the upstream body is never read);
     * a 2xx unary document is taken within {@code max-request-bytes} and passed to {@link #decodeUnaryResponse}; a 2xx
     * stream must first yield and validate <b>one complete frame</b> inside this method before any publisher is handed
     * back — that step is the commit barrier: before it any failure leaves natively (429/502/503/504 and so on) for the
     * caller to take its next safe attempt, after it no model switch, retry, HTTP status rewrite nor fabricated terminal
     * frame is possible and only ending this stream is. This method never waits on upstream inside a transaction or a
     * lock, since the read-only configuration transaction closed long ago and waiting is bounded only by the channel
     * timeout budgets plus the closing watchdog.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code exchange(command, route, routed)}，由 {@code LlmApiController} 在
     * {@code LlmInvocationService.invoke(command)} 之后调用。/ called by {@code LlmApiController} after
     * {@code LlmInvocationService.invoke(command)}.
     * @param command 参数 已校验的调用命令；parameter the validated invocation command.
     * @param route 参数 已被路由策略放行的首条候选 route；parameter the head candidate route the routing strategy already admitted.
     * @param routed 参数 路由阶段产出的原生结果，携带安全响应头，本方法就地补全 body；parameter the routed native result carrying the safe headers, whose body this method completes in place.
     * @return 返回 已带状态、安全头与有界 body 的原生结果；returns the native result carrying status, safe headers and a bounded body.
     * @throws LlmInvocationException 首帧验证之前的任何上游失败（429/502/503/504 等），状态即协议原生状态；any upstream failure before the first frame validates, whose status is the protocol-native one.
     */
    @Override
    public LlmInvocationResultVO exchange(LlmInvocationCommandDTO command, LlmModelSnapshotBO.RouteBO route,
            LlmInvocationResultVO routed) {
        LlmModelSnapshotBO.ChannelBO channel = requireUsableChannel(command, route);
        byte[] requestBody = serialize(encodeRequest(command, route), command);
        URI target = upstreamUri(channel);
        boolean streaming = Boolean.TRUE.equals(command.getStream());
        String credential = resolveCredential(channel);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(target)
                .timeout(Duration.ofMillis(streaming ? channel.getHeaderTimeoutMs() : channel.getTotalTimeoutMs()))
                .header("Accept", streaming ? "text/event-stream" : "application/json")
                .header("Content-Type", "application/json");
        if (credential != null) {
            requestBuilder.header("Authorization", "Bearer " + credential);
        }
        HttpRequest httpRequest = requestBuilder
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofMillis(channel.getConnectTimeoutMs()))
                .build();
        boolean streamingHandedOff = false;
        try {
            HttpResponse<InputStream> response = send(client, httpRequest, command, channel);
            int upstreamStatus = response.statusCode();
            InputStream body = response.body();
            if (upstreamStatus < HttpURLConnection.HTTP_OK || upstreamStatus >= HttpURLConnection.HTTP_MULT_CHOICE) {
                closeQuietly(body);
                LlmInvocationException failure = upstreamRejected(command, channel, response, upstreamStatus);
                routed.setStatus(failure.getStatus());
                routed.setHeaders(withRetryAfter(routed, response, failure.getStatus()));
                routed.setBody(Flux.just(DATA_BUFFER_FACTORY.wrap(serializeError(failure))));
                return routed;
            }
            long deadline = System.nanoTime() + Duration.ofMillis(channel.getTotalTimeoutMs()).toNanos();
            Duration idle = Duration.ofMillis(channel.getIdleTimeoutMs());
            if (streaming) {
                Flux<DataBuffer> committed = streamBody(command, channel, body, idle, deadline);
                routed.setStatus(upstreamStatus);
                routed.setBody(committed.doFinally(signal -> closeQuietly(client)));
                streamingHandedOff = true;
            } else {
                byte[] document = unaryBody(command, channel, body, deadline);
                routed.setStatus(upstreamStatus);
                routed.setBody(Flux.just(DATA_BUFFER_FACTORY.wrap(document)));
            }
            log.info("llm responses exchange committed, alias={} protocol={} channelKey={} status={} stream={}",
                    command.getModel(), protocol(), channel.getChannelKey(), upstreamStatus, streaming);
            return routed;
        } finally {
            if (!streamingHandedOff) {
                closeQuietly(client);
            }
        }
    }

    /**
     * 中文说明：出网前的就地准入：引擎开关、渠道存在与启用、同协议、能力覆盖、baseUrl 形态，以及 CLOUD 凭据引用/密钥根；
     * 无认证 LOCAL 可以没有 {@code secretRef}。任一必需条件不成立都以 503 {@code model_unavailable} 失败关闭——
     * 绝不静默改用别的协议或别的渠道，
     * 也绝不把「渠道引用悬空」读成「随便挑一个」。
     * English summary: The in-place admission gate before egress: the engine switch, channel presence and enablement,
     * same-protocol, capability coverage, base URL shape, and CLOUD credential/root; an unauthenticated LOCAL channel may
     * omit {@code secretRef}. Missing required facts fail closed with 503 {@code model_unavailable} — never another
     * protocol or channel, and never a dangling channel reference
     * read as "pick any".
     *
     * 用法 / Usage: 仅由 {@link #exchange} 调用。/ called only by {@link #exchange}.
     * @param command 参数 本次调用命令；parameter this invocation's command.
     * @param route 参数 已放行的候选 route；parameter the admitted candidate route.
     * @return 返回 可出网的渠道事实；returns the channel facts allowed to egress.
     * @throws LlmInvocationException 503 {@code model_unavailable}；raised for any unusable route fact.
     */
    private LlmModelSnapshotBO.ChannelBO requireUsableChannel(LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.RouteBO route) {
        LlmModelSnapshotBO.ChannelBO channel = route.getChannel();
        String reason = null;
        if (!gatewayProperties.isEnabled()) {
            reason = "engine egress disabled";
        } else if (channel == null || !Boolean.TRUE.equals(channel.getEnabled())) {
            reason = "channel unresolved or disabled";
        } else if (channel.getProtocol() != protocol()) {
            reason = "channel protocol is not the ingress protocol";
        } else if (channel.getDeployment() == null
                || (channel.getDeployment() != LlmDeploymentEnum.LOCAL
                && channel.getDeployment() != LlmDeploymentEnum.CLOUD)
                || StringUtils.isBlank(channel.getBaseUrl())
                || !route.getCapabilities().containsAll(command.getRequiredCapabilities())) {
            reason = "base URL or capability coverage unusable";
        } else if (channel.getDeployment() == LlmDeploymentEnum.CLOUD
                && (StringUtils.isBlank(channel.getSecretRef())
                || StringUtils.isBlank(gatewayProperties.getSecretsRoot()))) {
            reason = "cloud credential is unavailable";
        }
        if (reason != null) {
            log.warn("llm responses route rejected, alias={} protocol={} channelKey={} reason={}", command.getModel(),
                    protocol(), route.getChannelKey(), reason);
            throw nativeError(HttpURLConnection.HTTP_UNAVAILABLE, "model_unavailable", null,
                    "No usable same-protocol route is available for this model alias", true);
        }
        return channel;
    }

    /**
     * 中文说明：把上游非 2xx 映射成本面的<b>原生</b>错误：408/504→504 {@code upstream_timeout}（不可重试，结果可能
     * 已产生）、429→429 {@code rate_limit_exceeded}（下一次请求可重试）、401/403→503 {@code model_unavailable}
     * （上游拒绝的是<b>本引擎的</b>凭据，绝不能伪装成客户端认证失败）、其它 4xx 与 5xx 原样回显状态并分别以
     * {@code upstream_protocol_error}/{@code model_unavailable} 收束；3xx 与任何 400 以下的非 2xx 一律 502
     * {@code upstream_protocol_error}——重定向在本面不存在，上游跳转状态绝不转交给客户端。
     * 上游响应体一个字节都不读，
     * 因此不可能回显供应商原文；只记录 channelKey 与状态码。
     * English summary: Maps a non-2xx upstream response onto this face's <b>native</b> error: 408/504 → 504
     * {@code upstream_timeout} (not retryable, a result may already exist), 429 → 429 {@code rate_limit_exceeded}
     * (a next request may retry), 401/403 → 503 {@code model_unavailable} (the upstream refused <b>this engine's</b>
     * credential, which must never be dressed up as a client authentication failure) and every other 4xx or 5xx keeps
     * its native status under {@code upstream_protocol_error} or {@code model_unavailable}; a 3xx and any non-2xx below
     * 400 are always a 502 {@code upstream_protocol_error}, because redirection does not exist on this face and an
     * upstream redirect status is never handed to the client.
     * Not one byte of the upstream
     * body is read, so echoing vendor text is impossible, and only the channelKey and status are logged.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 在非 2xx 分支调用。/ called only by {@link #exchange} on its non-2xx branch.
     * @param command 参数 本次调用命令；parameter this invocation's command.
     * @param channel 参数 出错渠道，只用于日志定位；parameter the failing channel, used to locate the log line only.
     * @param response 参数 上游响应，仅取其状态；parameter the upstream response, of which only the status is taken.
     * @param upstreamStatus 参数 上游原生状态；parameter the native upstream status.
     * @return 返回 待出网的原生错误；returns the native error to leave.
     */
    private LlmInvocationException upstreamRejected(LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.ChannelBO channel, HttpResponse<InputStream> response, int upstreamStatus) {
        log.warn("llm responses upstream rejected, alias={} protocol={} channelKey={} status={}", command.getModel(),
                protocol(), channel.getChannelKey(), upstreamStatus);
        if (upstreamStatus == 408 || upstreamStatus == HttpURLConnection.HTTP_GATEWAY_TIMEOUT) {
            return nativeError(HttpURLConnection.HTTP_GATEWAY_TIMEOUT, "upstream_timeout", "response",
                    "The upstream model did not respond within its timeout budget", false);
        }
        if (upstreamStatus == 429) {
            return nativeError(429, "rate_limit_exceeded", null,
                    "The upstream model is rate limited for this channel", true);
        }
        if (upstreamStatus == HttpURLConnection.HTTP_UNAUTHORIZED
                || upstreamStatus == HttpURLConnection.HTTP_FORBIDDEN) {
            return nativeError(HttpURLConnection.HTTP_UNAVAILABLE, "model_unavailable", null,
                    "The channel credential was not accepted upstream for this model alias", true);
        }
        if (upstreamStatus >= HttpURLConnection.HTTP_INTERNAL_ERROR) {
            return nativeError(upstreamStatus, "model_unavailable", null,
                    "The upstream model could not serve this request", true);
        }
        if (upstreamStatus < HttpURLConnection.HTTP_BAD_REQUEST) {
            return nativeError(HttpURLConnection.HTTP_BAD_GATEWAY, "upstream_protocol_error", "response",
                    "The upstream model answered with a status this protocol cannot carry", false);
        }
        return nativeError(upstreamStatus, "upstream_protocol_error", "response",
                "The upstream model rejected this request document", false);
    }

    /**
     * 中文说明：只按渠道 {@code secretRef} 在 {@code yuheng.llm.secrets-root} 之下解析凭据：ref 不得自带绝对路径或
     * {@code ..}，规范化后必须仍在根内且解析到根内的真实文件（软链也不能逃出根），必须是普通文件且不超过 8KiB，
     * 内容 trim 后不得为空。任何一步不成立都是 503 {@code model_unavailable}，日志只给 channelKey 与失败类型，
     * 绝不输出路径、ref 原文或凭据值。
     * English summary: Resolves a credential from the channel's {@code secretRef} only under
     * {@code yuheng.llm.secrets-root}: the reference may not be absolute nor contain {@code ..}, the normalized path must
     * stay inside the root and resolve to a real file inside it (a symlink may not escape either), it must be a regular
     * file no larger than 8KiB and non-blank once trimmed. Every step that fails is a 503 {@code model_unavailable}
     * whose log line names only the channelKey and the failure kind, never a path, the raw reference or the value.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 构造请求头之前调用。/ called only by {@link #exchange} before the headers.
     * @param channel 参数 提供 secretRef 的渠道事实；parameter the channel facts supplying the secretRef.
     * @return 返回 解析出的凭据值，只存在于栈内；returns the resolved credential value which exists only on the stack.
     * @throws LlmInvocationException 503 {@code model_unavailable}；raised when the reference cannot be resolved safely.
     */
    private String resolveCredential(LlmModelSnapshotBO.ChannelBO channel) {
        if (channel.getSecretRef() == null && channel.getDeployment() == LlmDeploymentEnum.LOCAL) {
            return null;
        }
        String reference = StringUtils.trimToNull(channel.getSecretRef());
        String root = StringUtils.trimToNull(gatewayProperties.getSecretsRoot());
        if (root == null || reference == null) {
            throw nativeError(HttpURLConnection.HTTP_UNAVAILABLE, "model_unavailable", null,
                    "The channel credential cannot be resolved for this model alias", true);
        }
        try {
            Path rootPath = Path.of(root).toAbsolutePath().normalize();
            Path resolved = rootPath.resolve(reference).normalize();
            if (reference.indexOf('\0') >= 0 || reference.contains("..") || Path.of(reference).isAbsolute()
                    || resolved.equals(rootPath)
                    || !resolved.startsWith(rootPath) || !Files.isRegularFile(resolved)) {
                throw new IOException("the secret reference leaves the mounted root");
            }
            if (!resolved.toRealPath().startsWith(rootPath.toRealPath())) {
                throw new IOException("the resolved secret target escapes the mounted root");
            }
            if (Files.size(resolved) > MAX_SECRET_BYTES) {
                throw new IOException("the secret material is oversized");
            }
            String value = new String(Files.readAllBytes(resolved), StandardCharsets.UTF_8).trim();
            if (StringUtils.isBlank(value)) {
                throw new IOException("the secret material is empty");
            }
            return value;
        } catch (InvalidPathException | IOException | SecurityException failure) {
            log.error("llm responses credential unresolved, channelKey={} failure={}", channel.getChannelKey(),
                    failure.getClass().getSimpleName());
            throw nativeError(HttpURLConnection.HTTP_UNAVAILABLE, "model_unavailable", null,
                    "The channel credential cannot be resolved for this model alias", true);
        }
    }

    /**
     * 中文说明：把渠道 baseUrl 与本协议端点合成为出网 URI：CLOUD 必须 HTTPS，LOCAL 可用受本地白名单约束的 HTTP，
     * 地址必须绝对、有主机、无 userinfo、无查询、
     * 无片段（携带这些的一律 503，因为它们是出网自证）；baseUrl 已带路径段（通常含版本前缀）时只追加
     * {@code responses}，根地址则补 {@code /v1/responses}。跳转在本面根本不存在：客户端与上游都收不到 3xx 重定向，
     * 因为 {@link HttpClient} 以 {@code Redirect.NEVER} 构造，401/407 等状态由 {@link #upstreamRejected} 承担。
     * English summary: Composes the egress URI from the channel base URL and this face's endpoint: CLOUD must use HTTPS,
     * while LOCAL may use allowlisted HTTP; it must be absolute with a host and without userinfo, query or fragment (any of those is a 503, since they are egress
     * self-assertions); a base URL that already carries a path segment (typically the version prefix) only gets
     * {@code responses} appended while a bare origin receives {@code /v1/responses}. Redirection simply does not exist on
     * this face, because the {@link HttpClient} is built with {@code Redirect.NEVER} and statuses such as 401 or 407 are
     * carried by {@link #upstreamRejected}.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 调用。/ called only by {@link #exchange}.
     * @param channel 参数 渠道事实；parameter the channel facts.
     * @return 返回 出网目标 URI；returns the egress target URI.
     * @throws LlmInvocationException 503 {@code model_unavailable}；raised for an unusable base URL.
     */
    private URI upstreamUri(LlmModelSnapshotBO.ChannelBO channel) {
        try {
            URI base = URI.create(channel.getBaseUrl().trim());
            String path = base.getRawPath() == null ? "" : base.getRawPath();
            while (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            String target = path.isEmpty() ? DEFAULT_VERSION_PREFIX + "/" + RESPONSES_PATH
                    : path + "/" + RESPONSES_PATH;
            boolean secure = "https".equalsIgnoreCase(base.getScheme());
            boolean localHttp = channel.getDeployment() == LlmDeploymentEnum.LOCAL
                    && "http".equalsIgnoreCase(base.getScheme());
            if (!base.isAbsolute() || !(secure || localHttp)
                    || StringUtils.isBlank(base.getHost()) || base.getUserInfo() != null
                    || base.getRawQuery() != null || base.getRawFragment() != null) {
                throw new IOException("the channel base URL is not a clean HTTPS origin");
            }
            return new URI(base.getScheme().toLowerCase(Locale.ROOT), null, base.getHost(), base.getPort(), target,
                    null, null);
        } catch (IllegalArgumentException | URISyntaxException | IOException failure) {
            log.error("llm responses base URL unusable, channelKey={} failure={}", channel.getChannelKey(),
                    failure.getClass().getSimpleName());
            throw nativeError(HttpURLConnection.HTTP_UNAVAILABLE, "model_unavailable", null,
                    "The channel base URL is not usable for this model alias", true);
        }
    }

    /**
     * 中文说明：一次同协议上游发送，并把 JDK 客户端的失败翻译成本面原生状态：饱和类
     * （{@link RejectedExecutionException}，涵盖 Spring 的 {@code TaskRejectedException}）→ <b>提交前</b> 429、
     * header 预算耗尽（{@link HttpTimeoutException}）→ 504、中断 → 503（并复位中断位）、其余 IO/协议失败 → 502。
     * 原始异常只在此处按类型名脱敏记录，绝不成为对外错误的一部分。
     * English summary: Issues one same-protocol upstream send and translates JDK client failures into this face's native
     * statuses: saturation ({@link RejectedExecutionException}, which covers Spring's {@code TaskRejectedException}) →
     * 429 <b>before commit</b>, an exhausted header budget ({@link HttpTimeoutException}) → 504, an interruption → 503
     * with the interrupt bit restored and any other IO or protocol failure → 502. The raw failure is logged here by its
     * type name only and never becomes part of the outward error.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 调用。/ called only by {@link #exchange}.
     * @param client 参数 本方法就地构造的 JDK 原生客户端；parameter the JDK-native client this method built locally.
     * @param request 参数 已带凭据头的出网请求；parameter the egress request whose headers already carry the credential.
     * @param command 参数 本次调用命令；parameter this invocation's command.
     * @param channel 参数 渠道事实，用于超时与日志定位；parameter the channel facts supplying timeouts and log location.
     * @return 返回 已收到响应头的调用结果；returns the call result whose response headers have arrived.
     * @throws LlmInvocationException 429/502/503/504 之一；raised as one of 429/502/503/504.
     */
    private HttpResponse<InputStream> send(HttpClient client, HttpRequest request, LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.ChannelBO channel) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (RejectedExecutionException saturation) {
            log.warn("llm responses upstream saturated, alias={} protocol={} channelKey={}", command.getModel(),
                    protocol(), channel.getChannelKey());
            throw nativeError(429, "rate_limit_exceeded", null,
                    "The model gateway is at its concurrent request ceiling", true);
        } catch (HttpTimeoutException timeout) {
            log.warn("llm responses upstream timed out, alias={} protocol={} channelKey={}", command.getModel(),
                    protocol(), channel.getChannelKey());
            throw nativeError(HttpURLConnection.HTTP_GATEWAY_TIMEOUT, "upstream_timeout", "response",
                    "The upstream model did not respond within its timeout budget", false);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            log.warn("llm responses upstream interrupted, alias={} protocol={} channelKey={}", command.getModel(),
                    protocol(), channel.getChannelKey());
            throw nativeError(HttpURLConnection.HTTP_UNAVAILABLE, "model_unavailable", null,
                    "The upstream call was interrupted before it committed", true);
        } catch (IOException failure) {
            log.error("llm responses upstream failed, alias={} protocol={} channelKey={} failure={}",
                    command.getModel(), protocol(), channel.getChannelKey(), failure.getClass().getSimpleName());
            throw nativeError(HttpURLConnection.HTTP_BAD_GATEWAY, "upstream_protocol_error", "response",
                    "The upstream model call could not be completed", false);
        }
    }

    /**
     * 中文说明：非流成功路径：在 {@code yuheng.llm.max-request-bytes}（同一份文档上限，超出即 502，因为整份文档
     * 必须可校验）之内取回上游字节，交给 Jackson 解析为对象后由 {@link #decodeUnaryResponse} 保真回写 alias，
     * 再序列化回客户端。看门狗在总预算到点时关闭上游流，把阻塞读撞成 IOException，再由 {@link #readFailure}
     * 判定 504/502；无论成败，上游流都恰好关闭一次。
     * English summary: The unary success path: the upstream bytes are taken within
     * {@code yuheng.llm.max-request-bytes} (the same document ceiling, since a document that cannot be validated fully
     * is a 502), parsed into an object by Jackson, passed to {@link #decodeUnaryResponse} for fidelity and the alias
     * write-back and then serialized to the client. A watchdog closes the upstream stream when the total budget expires,
     * turning a blocked read into an IOException that {@link #readFailure} judges as 504 or 502, and the stream is closed
     * exactly once whatever happens.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 在非流分支调用。/ called only by {@link #exchange} on its unary branch.
     * @param command 参数 本次调用命令；parameter this invocation's command.
     * @param channel 参数 渠道事实，用于日志定位；parameter the channel facts used to locate the log line.
     * @param body 参数 上游响应体流；parameter the upstream response body stream.
     * @param deadlineNanos 参数 总预算截止点（nanoTime 基准）；parameter the total budget deadline on the nanoTime scale.
     * @return 返回 回写 alias 后的原生响应字节；returns the native response bytes with the alias written back.
     * @throws LlmInvocationException 502 {@code upstream_protocol_error}、504 {@code upstream_timeout}；raised for a damaged, oversized or stalled document.
     */
    private byte[] unaryBody(LlmInvocationCommandDTO command, LlmModelSnapshotBO.ChannelBO channel, InputStream body,
            long deadlineNanos) {
        long ceiling = gatewayProperties.getMaxRequestBytes();
        ByteArrayOutputStream captured = new ByteArrayOutputStream(READ_CHUNK_BYTES);
        AtomicLong abortAt = new AtomicLong(deadlineNanos);
        armAbortWatchdog(body, abortAt);
        byte[] pool = new byte[READ_CHUNK_BYTES];
        try {
            while (true) {
                int read = body.read(pool);
                if (read < 0) {
                    break;
                }
                if (read == 0) {
                    continue;
                }
                if (captured.size() + read > ceiling) {
                    throw protocolError("response", "The upstream response exceeds the managed document ceiling");
                }
                captured.write(pool, 0, read);
            }
            JsonNode parsed = objectMapper.readTree(captured.toByteArray());
            if (!(parsed instanceof ObjectNode document)) {
                throw protocolError("response", "The upstream response is not a JSON object");
            }
            return objectMapper.writeValueAsBytes(decodeUnaryResponse(command, document));
        } catch (IOException failure) {
            throw readFailure(command, channel, failure, abortAt);
        } finally {
            closeQuietly(body);
        }
    }

    /**
     * 中文说明：流式路径的提交屏障与 publisher 产出：在 exchange 线程上按 idle 预算逐块读上游，喂给一个只用于
     * 探测的帧累加器，直到<b>一个完整帧</b>能被 {@link #parseEvent} 验证通过（或 EOF/超预算/超帧上限而提交前失败，
     * 分别 502/504）；期间读到的原始字节被逐块留作回放前奏（数量受单帧上限约束，绝不缓存整条流）。屏障通过后
     * 才把「前奏字节 + 继续读同一上游流」组成的字节流交给 {@link #encodeStream}，因此出网的第一个帧一定已被验证。
     * 返回的流带 {@code limitRate(1)}、按帧 {@code timeout} 与总预算看门狗，取消即关闭上游流，未交付的帧由
     * {@code doOnDiscard} 释放。
     * English summary: The streaming commit barrier and publisher: on the exchange thread the upstream is read chunk by
     * chunk under the idle budget and fed to a probe-only frame accumulator until <b>one complete frame</b> passes
     * {@link #parseEvent} (EOF, an expired budget or an oversized frame fail before commit as 502/504 respectively);
     * the raw bytes read meanwhile are kept as a replay prelude, bounded by the per-frame ceiling so no whole stream is
     * ever buffered. Only after the barrier does it hand {@link #encodeStream} a byte flux of "prelude bytes plus
     * continued reads of the same upstream stream", so the first frame that leaves has provably been validated. The
     * returned flux carries {@code limitRate(1)}, a per-frame {@code timeout} and the total-budget watchdog, a
     * cancellation closes the upstream stream and undelivered frames are released through {@code doOnDiscard}.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 在流式分支调用。/ called only by {@link #exchange} on its streaming branch.
     * @param command 参数 本次调用命令；parameter this invocation's command.
     * @param channel 参数 渠道事实，提供 idle 预算与日志定位；parameter the channel facts supplying the idle budget and log location.
     * @param body 参数 上游响应体流；parameter the upstream response body stream.
     * @param idle 参数 空闲/帧间预算；parameter the idle or inter-frame budget.
     * @param deadlineNanos 参数 总预算截止点（nanoTime 基准）；parameter the total budget deadline on the nanoTime scale.
     * @return 返回 已通过首帧屏障的同协议字节流；returns the same-protocol byte flux past the first-frame barrier.
     * @throws LlmInvocationException 502 {@code upstream_protocol_error}、504 {@code upstream_timeout}（均为提交前失败）；raised for a pre-commit failure.
     */
    private Flux<DataBuffer> streamBody(LlmInvocationCommandDTO command, LlmModelSnapshotBO.ChannelBO channel,
            InputStream body, Duration idle, long deadlineNanos) {
        int ceiling = gatewayProperties.getMaxFrameBytes();
        List<byte[]> prelude = new ArrayList<>(2);
        ByteBuffer probe = ByteBuffer.allocate(ceiling + FRAME_TERMINATOR_SPARE);
        AtomicBoolean validated = new AtomicBoolean();
        AtomicLong abortAt = new AtomicLong(deadlineNanos);
        byte[] pool = new byte[READ_CHUNK_BYTES];
        boolean handedOff = false;
        try {
            while (!validated.get()) {
                if (System.nanoTime() >= abortAt.get()) {
                    throw upstreamTimeout(command, channel);
                }
                abortAt.set(Math.min(deadlineNanos, System.nanoTime() + idle.toNanos()));
                armAbortWatchdog(body, abortAt);
                int read = body.read(pool);
                if (read < 0) {
                    break;
                }
                if (read == 0) {
                    continue;
                }
                byte[] chunk = Arrays.copyOf(pool, read);
                prelude.add(chunk);
                feed(probe, chunk, ceiling, frame -> {
                    parseEvent(frame.data(), command.getModel(), new AtomicReference<>());
                    validated.set(true);
                    return true;
                });
            }
            if (!validated.get()) {
                throw protocolError("response",
                        "The upstream stream offered no complete valid Responses frame");
            }
            abortAt.set(deadlineNanos);
            armAbortWatchdog(body, abortAt);
            Flux<DataBuffer> encoded = encodeStream(command,
                    Flux.<DataBuffer, InputStream>using(() -> body,
                            live -> Flux.concat(Flux.fromIterable(prelude).map(DATA_BUFFER_FACTORY::wrap),
                                    chunks(live, abortAt, command, channel)),
                            OpenAiResponsesProtocolStrategy::closeQuietly))
                    .limitRate(1)
                    .timeout(idle)
                    .onErrorMap(failure -> failure instanceof LlmInvocationException error ? error
                            : readFailure(command, channel, failure, abortAt))
                    .doOnDiscard(DataBuffer.class, DataBufferUtils::release);
            handedOff = true;
            return encoded;
        } catch (IOException failure) {
            throw readFailure(command, channel, failure, abortAt);
        } finally {
            if (!handedOff) {
                closeQuietly(body);
            }
        }
    }

    /**
     * 中文说明：把上游流包成「每次请求一块」的冷字节流：{@link Flux#generate} 天然一元素一请求，因此上游预取恒为 1；
     * 每次读取前核对总预算（到点即 504 {@code upstream_timeout}），IOException 交 {@link #readFailure} 判为
     * 504/502，EOF 正常收束；本方法绝不缓存多余字节，也不自建线程。
     * 每块都新建堆内 {@link DataBuffer}，由 {@link #encodeStream} 在取出字节后恰好释放一次。
     * English summary: Wraps the upstream stream into a cold byte flux of one chunk per request: {@link Flux#generate}
     * requests exactly one element at a time so upstream prefetch is always one, the total budget is checked before each
     * read (expiry is 504 {@code upstream_timeout}), an IOException is judged by {@link #readFailure} as 504 or 502 and
     * EOF completes normally; nothing is buffered beyond one chunk and no thread is created. Every chunk becomes a fresh
     * heap {@link DataBuffer} that {@link #encodeStream} releases exactly once after taking its bytes.
     *
     * 用法 / Usage: 仅由 {@link #streamBody} 调用。/ called only by {@link #streamBody}.
     * @param stream 参数 上游响应体流；parameter the upstream response body stream.
     * @param abortAt 参数 当前中止截止点（nanoTime 基准）；parameter the current abort deadline on the nanoTime scale.
     * @param command 参数 本次调用命令，仅用于日志定位；parameter this invocation's command, used to locate the log line.
     * @param channel 参数 渠道事实，用于日志定位；parameter the channel facts used to locate the log line.
     * @return 返回 每块一个 DataBuffer 的字节流；returns a byte flux carrying one DataBuffer per chunk.
     */
    private Flux<DataBuffer> chunks(InputStream stream, AtomicLong abortAt, LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.ChannelBO channel) {
        return Flux.<DataBuffer, byte[]>generate(() -> new byte[READ_CHUNK_BYTES], (pool, sink) -> {
            try {
                if (System.nanoTime() >= abortAt.get()) {
                    sink.error(upstreamTimeout(command, channel));
                    return pool;
                }
                int read = stream.read(pool);
                if (read < 0) {
                    sink.complete();
                    return pool;
                }
                sink.next(DATA_BUFFER_FACTORY.wrap(Arrays.copyOf(pool, Math.max(read, 0))));
                return pool;
            } catch (IOException failure) {
                sink.error(readFailure(command, channel, failure, abortAt));
                return pool;
            }
        });
    }

    /**
     * 中文说明：读一个上游 {@link DataBuffer} 的全部可读字节并<b>恰好释放一次</b>该缓冲区——本面只吃字节，
     * 帧边界由 {@link #feed} 在累加器里重建，因此缓冲区绝不会被留到下一个信号之后。它是流式路径上<b>唯一</b>
     * 释放上游缓冲区的地方，且作为 {@code map} 的一步把「缓冲区」与「在途数据」彻底分开：其后各段算子搬运的都是
     * {@code byte[]}，Reactor 在取消或出错时对源值的丢弃就再也不会二次释放同一块缓冲区。
     * English summary: Takes every readable byte of one upstream {@link DataBuffer} and releases that buffer
     * <b>exactly once</b>: this face consumes bytes only and {@link #feed} rebuilds frame boundaries inside the
     * accumulator, so an upstream buffer is never carried past the signal that supplied it. This is the <b>only</b> place
     * the streaming path releases an upstream buffer, and, as the single {@code map} step, it separates "buffer" from
     * "data in flight": everything the later operators carry is a {@code byte[]}, so Reactor's discard of a source value
     * on cancel or error can never release the same buffer twice.
     *
     * 用法 / Usage: 仅由 {@link #encodeStream} 的字节抽取步骤调用。/ called only by the byte-extraction step of {@link #encodeStream}.
     * @param buffer 参数 上游缓冲区；parameter the upstream buffer.
     * @return 返回 该缓冲区里的字节；returns the bytes that buffer held.
     */
    private static byte[] takeBytes(DataBuffer buffer) {
        try {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            return bytes;
        } finally {
            DataBufferUtils.release(buffer);
        }
    }

    /**
     * 中文说明：把一段上游字节喂进帧累加器并逐个交出自成一帧的事件：累加器按 {@code position} 写、{@code flip}/{@code
     * compact} 读，帧边界是空行（\n\n、\r\n\r\n、\r 结尾皆支持），载荷重建见 {@link #sseEvent}。
     * 累加器容量恒等于单帧上限加终止符余量，写满仍切不出帧即 502，因此任何路径都不会把整条流缓存在内存里。
     * {@code frameSink} 返回 true 表示本帧之后停止喂入（首帧屏障与终态收束用），未消费的字节留在累加器里。
     * English summary: Feeds a stretch of upstream bytes into the frame accumulator and yields every self-contained frame
     * event: the accumulator writes at {@code position} and reads through {@code flip}/{@code compact}, a frame ends at a
     * blank line (\n\n, \r\n\r\n and \r\r all count) and {@link #sseEvent} rebuilds name plus payload. The accumulator
     * capacity is exactly the per-frame ceiling plus terminator slack, so a full buffer that still yields no frame is a
     * 502 — no path ever buffers a whole stream. A {@code frameSink} that returns true stops the feeding after that frame
     * (the first-frame barrier and the terminal collapse use it) and the unconsumed bytes stay in the accumulator.
     *
     * 用法 / Usage: 由 {@link #encodeStream} 与 {@link #streamBody} 共用，保证提交屏障与出网解码走同一套帧文法。
     * / shared by {@link #encodeStream} and {@link #streamBody} so the commit barrier and the egress decoder follow one
     * frame grammar.
     * @param accumulator 参数 帧累加器（写模式）；parameter the frame accumulator in write mode.
     * @param chunk 参数 本段上游字节；parameter this stretch of upstream bytes.
     * @param ceiling 参数 单帧字节上限；parameter the per-frame byte ceiling.
     * @param frameSink 参数 帧事件处理者，返回 true 停止喂入；parameter the frame handler, true stops feeding.
     * @throws LlmInvocationException 502 {@code upstream_protocol_error}（单帧越界）；raised for an oversized frame.
     */
    private static void feed(ByteBuffer accumulator, byte[] chunk, int ceiling,
            Predicate<SseEventFrame> frameSink) {
        int offset = 0;
        while (true) {
            SseEventFrame frame = nextFrame(accumulator);
            if (frame != null) {
                if (!frame.data().isEmpty() && frameSink.test(frame)) {
                    return;
                }
                continue;
            }
            if (offset >= chunk.length) {
                return;
            }
            int want = Math.min(chunk.length - offset, accumulator.remaining());
            if (want == 0) {
                throw protocolError("response", "An upstream frame exceeded the managed frame ceiling");
            }
            accumulator.put(chunk, offset, want);
            offset += want;
        }
    }

    /**
     * 中文说明：从累加器里取出第一个完整帧（一帧就是空行之前的全部行，三种行终止符皆认），交回其事件名与拼接后的
     * 载荷；还没有任何完整帧时把缓冲区原样还原为写模式并返回 {@code null}。取出后剩余字节 compact 回头，保证多帧
     * 同块与半帧跨块都只按顺序处理一次。
     * English summary: Takes the first complete frame out of the accumulator — everything before a blank line, with all
     * three line terminators honoured — and returns its event name plus its joined payload, restoring the buffer to write
     * mode unchanged with {@code null} while no frame is complete yet. After a take the remaining bytes are compacted to
     * the head, so multiple frames in one chunk and a frame split across chunks are each handled once and in order.
     *
     * 用法 / Usage: 仅由 {@link #feed} 调用。/ called only by {@link #feed}.
     * @param accumulator 参数 帧累加器（写模式）；parameter the frame accumulator in write mode.
     * @return 返回 一个帧或 null；returns one frame, or null while no frame is closed.
     */
    private static SseEventFrame nextFrame(ByteBuffer accumulator) {
        accumulator.flip();
        int buffered = accumulator.limit();
        int lineStart = 0;
        int index = 0;
        while (index < buffered) {
            byte current = accumulator.get(index);
            int terminatorEnd;
            if (current == LINE_FEED) {
                terminatorEnd = index + 1;
            } else if (current == CARRIAGE_RETURN) {
                terminatorEnd = index + 1 < buffered && accumulator.get(index + 1) == LINE_FEED ? index + 2 : index + 1;
            } else {
                index++;
                continue;
            }
            if (index == lineStart) {
                byte[] block = new byte[lineStart];
                for (int position = 0; position < lineStart; position++) {
                    block[position] = accumulator.get(position);
                }
                accumulator.position(terminatorEnd);
                accumulator.compact();
                return sseEvent(new String(block, StandardCharsets.UTF_8));
            }
            lineStart = terminatorEnd;
            index = terminatorEnd;
        }
        accumulator.position(0);
        accumulator.compact();
        return null;
    }

    /**
     * 中文说明：按 SSE 文法解释一帧的行文本，得出事件名与载荷：{@code :} 开头的注释行与空行不承载载荷，
     * {@code event:} 行给出事件名（后跟的一个可选空格被剥掉），{@code id:} 与 {@code retry:} 行只影响重连状态、
     * 对转发无意义故忽略，{@code data:} 行去掉前缀（含一个可选空格）后计入载荷；<b>既不是已知字段也不是注释的
     * 行是本帧载荷的续行</b>——供应商把整份 JSON 美化成多行时只有第一行带 {@code data:} 前缀，逐字并入才不会把
     * 文档截断，所以未知字名不在此被丢弃（丢弃就会破坏「未知但安全的字段必须保真」的保真合同）。多段载荷以
     * {@code \n} 拼接，与 SSE 的多行 {@code data:} 规则一致；没有任何载荷行时载荷为空串（心跳/纯注释帧）。
     * English summary: Interprets one frame's line text per the SSE grammar, producing the event name and the payload: a
     * {@code :}-prefixed comment and an empty line carry nothing, an {@code event:} line supplies the event name (one
     * optional following space stripped), {@code id:} and {@code retry:} lines only concern reconnection state and are
     * therefore dropped for forwarding purposes, and a {@code data:} line contributes its value minus the prefix (plus one
     * optional space). <b>A line that is neither a known field nor a comment continues this frame's payload</b>: when a
     * vendor pretty-prints one JSON document over several lines only the first carries the {@code data:} prefix, so
     * keeping such a line verbatim is what stops the document from being truncated — an unknown field name is not discarded
     * here, because discarding would break this face's own "unknown but safe fields travel verbatim" contract. Payload
     * segments join with {@code \n}, matching the SSE multi-line {@code data:} rule; with no payload line at all the
     * payload is empty (a heartbeat or comment-only frame).
     *
     * 用法 / Usage: 仅由 {@link #nextFrame} 调用。/ called only by {@link #nextFrame}.
     * @param block 参数 一帧的行文本（不含收束它的空行）；parameter the line text of one frame, without its closing blank line.
     * @return 返回 事件名可为 null 的帧，载荷恒非 null；returns a frame whose name may be null and whose payload never is.
     */
    private static SseEventFrame sseEvent(String block) {
        String name = null;
        List<String> lines = new ArrayList<>(2);
        int from = 0;
        int length = block.length();
        while (from < length) {
            int terminator = indexOfAny(block, from);
            int end = terminator < 0 ? length : terminator;
            String line = block.substring(from, end);
            from = terminator < 0 ? length
                    : block.charAt(terminator) == '\r' && terminator + 1 < length
                            && block.charAt(terminator + 1) == '\n' ? terminator + 2 : terminator + 1;
            if (line.isEmpty() || line.charAt(0) == ':') {
                continue;
            }
            int colon = line.indexOf(':');
            String field = colon < 0 ? line : line.substring(0, colon);
            if (EVENT_NAME.equals(field)) {
                String value = StringUtils.trimToNull(colon < 0 ? "" : line.substring(colon + 1));
                if (value != null) {
                    name = value;
                }
                continue;
            }
            if (ID_NAME.equals(field) || RETRY_NAME.equals(field)) {
                continue;
            }
            lines.add(DATA_NAME.equals(field) ? stripOneLeadingSpace(colon < 0 ? "" : line.substring(colon + 1)) : line);
        }
        return new SseEventFrame(name, String.join(LINE_FEED_TEXT, lines));
    }

    /** 中文说明：只剥掉载荷值前的一个空格，逐字对应 SSE「紧跟冒号的一个可选空格不属于值」。 English summary: Removes exactly one leading space, matching the SSE rule that one optional space after the colon is not part of the value. */
    private static String stripOneLeadingSpace(String value) {
        return value.startsWith(" ") ? value.substring(1) : value;
    }

    /**
     * 中文说明：一个自成一帧的 SSE 事件：可缺省的 {@code event:} 名与按 SSE 规则拼好的载荷（载荷恒非 null，
     * 纯注释/心跳帧为空串）。出帧名优先取 {@code event:} 行，没有该行时才回落到已验证文档的 {@code type}。
     * English summary: One self-contained SSE event: an optional {@code event:} name plus the payload joined per the SSE
     * rules (the payload is never null and is empty for a comment or heartbeat frame). The outgoing name prefers the
     * {@code event:} line and falls back to the validated document's {@code type} only when that line is absent.
     *
     * 用法 / Usage: 由 {@link #nextFrame} 产出、{@link #feed} 交付、{@link #encodeStream} 与首帧屏障消费。/ produced by
     * {@link #nextFrame}, handed over by {@link #feed} and consumed by {@link #encodeStream} and the first-frame barrier.
     * @param name 字段 {@code event:} 行的事件名，可为 null；field the event name from the {@code event:} line, nullable.
     * @param data 字段 拼接后的帧载荷，可为空串；field the joined frame payload, possibly empty.
     */
    private record SseEventFrame(String name, String data) {

        /**
         * 中文说明：本帧出网时写进 {@code event:} 的名字：上游给了就用上游的，否则用已验证的 {@code type}。
         * English summary: The name this frame is written out with: the upstream {@code event:} line when it carried one,
         * otherwise the validated {@code type}.
         *
         * 用法 / Usage: 仅由 {@link #encodeStream} 调用。/ called only by {@link #encodeStream}.
         * @param type 参数 事件文档的 {@code type}；parameter the {@code type} of the event document.
         * @return 返回 出网事件名；returns the name to write out.
         */
        private String eventOf(String type) {
            return name == null ? type : name;
        }
    }

    /**
     * 中文说明：找出下一个行终止符（\n 或 \r）下标，找不到返回 -1。/ English summary: Finds the next line terminator
     * (\n or \r) index, or -1 when there is none.
     * @param block 参数 行文本；parameter the line text.
     * @param from 参数 起始下标；parameter the start index.
     * @return 返回 终止符下标或 -1；returns the terminator index or -1.
     */
    private static int indexOfAny(String block, int from) {
        for (int index = from; index < block.length(); index++) {
            char current = block.charAt(index);
            if (current == '\n' || current == '\r') {
                return index;
            }
        }
        return -1;
    }

    /**
     * 中文说明：解析并验证一个上游事件帧：载荷必须是 JSON 对象、必须带非空白文本 {@code type}（终态判定与出帧
     * {@code event:} 名都由它决定），携带 {@code sequence_number} 时必须是非负整数且<b>严格大于</b>上一帧（顺序即
     * 上游顺序，绝不重排）；随后只把顶层 {@code model} 与 {@code response.model} 回写为客户端 alias，
     * 其它字段（含未知但安全字段、{@code usage}、{@code output_index}、{@code item}）原样保留。
     * English summary: Parses and validates one upstream event frame: the payload must be a JSON object carrying a
     * non-blank textual {@code type} (which decides both the terminal judgement and the outgoing {@code event:} name),
     * a {@code sequence_number} when present must be a non-negative integer <b>strictly greater</b> than the previous
     * frame (order is upstream order and is never re-ordered), and afterwards only the top-level {@code model} and
     * {@code response.model} are written back to the client alias while every other field (unknown-but-safe fields,
     * {@code usage}, {@code output_index}, {@code item} included) is preserved.
     *
     * 用法 / Usage: 由 {@link #encodeStream} 与首帧屏障共用。/ shared by {@link #encodeStream} and the first-frame barrier.
     * @param payload 参数 帧 data 载荷；parameter the frame data payload.
     * @param alias 参数 回写给客户端的模型 alias；parameter the client alias to write back.
     * @param lastSequence 参数 上一帧序号；parameter the previous frame's sequence number.
     * @return 返回 已回写 alias 的事件对象；returns the event object with the alias written back.
     * @throws LlmInvocationException 502 {@code upstream_protocol_error}；raised for a frame this protocol cannot accept.
     */
    private ObjectNode parseEvent(String payload, String alias, AtomicReference<Long> lastSequence) {
        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(payload);
        } catch (JsonProcessingException failure) {
            throw protocolError("response", "An upstream frame is not a JSON document");
        }
        if (!(parsed instanceof ObjectNode event)) {
            throw protocolError("response", "An upstream frame is not a JSON object");
        }
        JsonNode type = event.get("type");
        if (type == null || !type.isTextual() || StringUtils.isBlank(type.asText())) {
            throw protocolError("response", "An upstream frame carries no event type");
        }
        JsonNode sequence = event.get("sequence_number");
        if (sequence != null) {
            if (!sequence.isIntegralNumber() || !sequence.canConvertToLong() || sequence.longValue() < 0L) {
                throw protocolError("sequence_number", "An upstream frame carries an unusable sequence number");
            }
            Long previous = lastSequence.get();
            if (previous != null && sequence.longValue() <= previous.longValue()) {
                throw protocolError("sequence_number", "An upstream frame regressed the event sequence");
            }
            lastSequence.set(Long.valueOf(sequence.longValue()));
        }
        rewriteAlias(event, alias);
        return event;
    }

    /**
     * 中文说明：只回写协议里真正承载模型名的两处（顶层 {@code model} 与 {@code response.model}），且只在原字段是
     * 文本时改写——绝不凭空伪造，也绝不触碰 {@code id}、{@code call_id}、{@code item} 或任何 opaque 字段。
     * English summary: Writes back only the two places the protocol really carries a model name (the top-level
     * {@code model} and {@code response.model}) and only when the original field is textual — nothing is invented and no
     * {@code id}, {@code call_id}, {@code item} or opaque field is ever touched.
     *
     * 用法 / Usage: 仅由 {@link #parseEvent} 调用。/ called only by {@link #parseEvent}.
     * @param event 参数 事件对象；parameter the event object.
     * @param alias 参数 客户端 alias；parameter the client alias.
     */
    private static void rewriteAlias(ObjectNode event, String alias) {
        JsonNode model = event.get("model");
        if (model != null && model.isTextual()) {
            event.put("model", alias);
        }
        JsonNode response = event.get("response");
        if (response instanceof ObjectNode responseNode) {
            JsonNode responseModel = responseNode.get("model");
            if (responseModel != null && responseModel.isTextual()) {
                responseNode.put("model", alias);
            }
        }
    }

    /**
     * 中文说明：按同协议重新出帧：{@code event:<type>\ndata:<json>\n\n}（与 {@code ServerSentEvent} 的写法一致：
     * 冒号后不加空格），type 取自已验证的 {@code type}，sequence_number 原样在载荷里；出帧字节再次受单帧上限约束。
     * 载荷的 JSON 结构与数值表示按 Jackson 树原样序列化，不重新编号、不补字段、不删未知字段。
     * English summary: Re-frames in the same protocol as {@code event:<type>\ndata:<json>\n\n}, matching how
     * {@code ServerSentEvent} writes fields with no space after the colon: the type comes from the validated
     * {@code type}, the sequence number stays inside the payload verbatim, and the emitted bytes are capped by the
     * per-frame ceiling once more. The payload's JSON structure and numeric representations are serialized from the
     * Jackson tree as-is, renumbering nothing, adding no field and dropping no unknown field.
     *
     * 用法 / Usage: 仅由 {@link #encodeStream} 调用。/ called only by {@link #encodeStream}.
     * @param event 参数 已回写 alias 的事件对象；parameter the event object with the alias written back.
     * @param type 参数 事件类型；parameter the event type.
     * @param ceiling 参数 单帧字节上限；parameter the per-frame byte ceiling.
     * @return 返回 客户端 SSE 帧字节；returns the client SSE frame bytes.
     * @throws LlmInvocationException 502 {@code upstream_protocol_error}（重编码后越界或不可序列化）；raised for an oversized or unserializable frame.
     */
    private byte[] encodeFrame(ObjectNode event, String name, int ceiling) {
        try {
            String frame = "event:" + name + "\n" + DATA_FIELD + objectMapper.writeValueAsString(event) + "\n\n";
            byte[] bytes = frame.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > ceiling) {
                throw protocolError("response", "A re-encoded frame exceeded the managed frame ceiling");
            }
            return bytes;
        } catch (JsonProcessingException failure) {
            throw protocolError("response", "An upstream frame cannot be re-encoded");
        }
    }

    /**
     * 中文说明：核对 {@code model}：payload 缺字段时按命令 alias 补齐（协议面是别名的权威来源），存在则必须是与
     * 命令一致的文本——不一致说明入口与文档不同源，400 拒绝而不是猜测。
     * English summary: Checks {@code model}: when the payload lacks it the command alias completes the document (the
     * protocol face is the authoritative source of the alias) and when present it must be textual and equal to the
     * command alias, because a disagreement means ingress and document are not the same source and a 400 beats guessing.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。/ called only by {@link #encodeRequest}.
     * @param command 参数 本次调用命令；parameter this invocation's command.
     * @param request 参数 待改写请求文档；parameter the request document about to be rewritten.
     */
    private static void verifyModel(LlmInvocationCommandDTO command, ObjectNode request) {
        JsonNode model = request.get("model");
        if (model == null || model.isNull()) {
            return;
        }
        if (!model.isTextual() || !command.getModel().equals(model.asText())) {
            throw unsupported("model", "The request model does not match the invoked alias");
        }
    }

    /**
     * 中文说明：核对 {@code stream} 位：允许缺失（由命令位补齐），存在时必须与 {@code command.stream} 完全一致，
     * 否则 400——绝不按 body 猜 stream，也绝不在已定协议之后改形状。
     * English summary: Checks the {@code stream} bit: absence is allowed (the command bit completes it) but a present
     * value must equal {@code command.stream} exactly, otherwise 400 — the stream mode is never guessed from the body
     * and the shape is never changed once the protocol is settled.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。/ called only by {@link #encodeRequest}.
     * @param command 参数 本次调用命令；parameter this invocation's command.
     * @param request 参数 待校验请求文档；parameter the request document to validate.
     */
    private static void verifyStream(LlmInvocationCommandDTO command, ObjectNode request) {
        JsonNode stream = request.get("stream");
        if (stream != null && !stream.isNull()
                && (!stream.isBoolean() || stream.booleanValue() != Boolean.TRUE.equals(command.getStream()))) {
            throw unsupported("stream", "The request stream bit contradicts the ingress mode");
        }
    }

    /**
     * 中文说明：拒绝任何出网/凭据自证与托管会话字段（大小写不敏感），消息不回显字段值。
     * English summary: Rejects every egress, credential and managed-session self-assertion case-insensitively, and the
     * message never echoes the field value.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。/ called only by {@link #encodeRequest}.
     * @param request 参数 待校验请求文档；parameter the request document to validate.
     */
    private static void rejectForbiddenFields(ObjectNode request) {
        for (Iterator<String> names = request.fieldNames(); names.hasNext();) {
            String name = names.next();
            if (FORBIDDEN_REQUEST_FIELDS.contains(name.toLowerCase(Locale.ROOT))) {
                throw unsupported(name, "The request may not self-assert egress, credential or session fields");
            }
        }
    }

    /**
     * 中文说明：拒绝未声明的请求扩展：不在已知字段表里就必须命中能力门且 route 已声明该能力，否则 400
     * {@code unsupported_parameter}；这是「配置声明是准入上限」的落地点，绝不做静默透传。
     * English summary: Rejects undeclared request extensions: a field outside the known table must hit the capability
     * gate with the route declaring exactly that capability, otherwise it is a 400 {@code unsupported_parameter}. This is
     * where "a configuration declaration is an admission ceiling" lands, with no silent pass-through.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。/ called only by {@link #encodeRequest}.
     * @param request 参数 待校验请求文档；parameter the request document to validate.
     * @param declared 参数 route 已声明的能力集合；parameter the capabilities the route declares.
     */
    private static void rejectUndeclaredExtensions(ObjectNode request, Set<LlmCapabilityEnum> declared) {
        for (Iterator<String> names = request.fieldNames(); names.hasNext();) {
            String name = names.next();
            String key = name.toLowerCase(Locale.ROOT);
            if (KNOWN_REQUEST_FIELDS.contains(key) || FORBIDDEN_REQUEST_FIELDS.contains(key)) {
                continue;
            }
            LlmCapabilityEnum required = CAPABILITY_GATED_FIELDS.get(key);
            if (required == null || !declared.contains(required)) {
                throw unsupported(name, "The request extension is not declared by the routed model capabilities");
            }
        }
    }

    /**
     * 中文说明：核对 {@code instructions}（可选，0–16000 字符文本）。
     * English summary: Checks {@code instructions}, an optional text field of 0–16000 characters.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。/ called only by {@link #encodeRequest}.
     * @param request 参数 待校验请求文档；parameter the request document to validate.
     */
    private static void verifyInstructions(ObjectNode request) {
        JsonNode instructions = request.get("instructions");
        if (instructions != null && !instructions.isNull()
                && (!instructions.isTextual() || instructions.asText().length() > MAX_INSTRUCTIONS_CHARS)) {
            throw unsupported("instructions", "instructions must be text within the managed length");
        }
    }

    /**
     * 中文说明：核对本协议自己的输出预算：只接受正整数 {@code max_output_tokens}（≤8192），跨协议预算字段
     * {@code max_tokens}/{@code max_completion_tokens} 出现在 Responses 面即 400——预算不能以另一个协议字段替换；
     * 同时要求 {@code max_tool_calls} 若存在为非负整数。
     * English summary: Checks this protocol's own output budget: only a positive integral {@code max_output_tokens}
     * (≤ 8192) is accepted and the cross-protocol budget fields {@code max_tokens} and {@code max_completion_tokens} are
     * a 400 on the Responses face because a budget may never be replaced by another protocol's field, while
     * {@code max_tool_calls} must be a non-negative integer when present.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。/ called only by {@link #encodeRequest}.
     * @param request 参数 待校验请求文档；parameter the request document to validate.
     */
    private static void verifyOutputBudget(ObjectNode request) {
        for (String foreign : new String[] {"max_tokens", "max_completion_tokens"}) {
            if (request.has(foreign)) {
                throw unsupported(foreign, "This protocol takes its output budget only from max_output_tokens");
            }
        }
        JsonNode budget = request.get("max_output_tokens");
        if (budget != null && !budget.isNull()) {
            if (!budget.isIntegralNumber() || !budget.canConvertToLong() || budget.longValue() < 1L
                    || budget.longValue() > MAX_OUTPUT_TOKENS) {
                throw unsupported("max_output_tokens",
                        "max_output_tokens must be a positive integer within the routed budget");
            }
        }
        JsonNode toolCalls = request.get("max_tool_calls");
        if (toolCalls != null && !toolCalls.isNull()
                && (!toolCalls.isIntegralNumber() || toolCalls.longValue() < 0L)) {
            throw unsupported("max_tool_calls", "max_tool_calls must be a non-negative integer");
        }
    }

    /**
     * 中文说明：网关无托管会话：{@code store} 缺失按 false 处理、显式 true 拒绝（网关从不把客户端内容留在上游），
     * 非布尔值同样拒绝；{@code background} 缺失或 false 允许、true 拒绝。{@code conversation}/
     * {@code previous_response_id} 已在自证字段阶段被拒。
     * English summary: The gateway hosts no session: an absent {@code store} behaves as false, an explicit true is
     * refused (the gateway never leaves client content in an upstream account) and a non-boolean likewise, while
     * {@code background} may be absent or false but never true. {@code conversation} and {@code previous_response_id}
     * already fell in the self-assertion stage.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。/ called only by {@link #encodeRequest}.
     * @param request 参数 待校验请求文档；parameter the request document to validate.
     */
    private static void verifyStorePolicy(ObjectNode request) {
        JsonNode store = request.get("store");
        if (store != null && !store.isNull()) {
            if (!store.isBoolean() || store.booleanValue()) {
                throw unsupported("store", "The gateway never persists request or response content upstream");
            }
        }
        JsonNode background = request.get("background");
        if (background != null && !background.isNull() && (!background.isBoolean() || background.booleanValue())) {
            throw unsupported("background", "Only synchronous Responses calls are served by this gateway");
        }
    }

    /**
     * 中文说明：核对 {@code input}：必须是非空 string 或 1–1000 个 items。逐 item 校验判别字段（缺 type 时按
     * 消息简写并要求 role 合法）：{@code function_call} 必须自带非空 {@code call_id} 与合法 {@code name} 且
     * {@code arguments} 为字符串（不透明回放，绝不解析执行）；{@code function_call_output} 的 {@code call_id}
     * 必须配对同一文档里出现过的调用（工具关联原样保留、不建跨用户缓存）；{@code reasoning} item 只按 REASONING
     * 能力不透明回放；带 {@code input_image} part 的 item 需要 VISION。任何 {@code function_*} item 还需
     * FUNCTION_TOOLS。
     * English summary: Checks {@code input}, which must be a non-empty string or 1–1000 items. Per item the
     * discriminator is verified (a missing type means the message shorthand and requires a legal role): a
     * {@code function_call} must carry a non-blank {@code call_id}, a legal {@code name} and a string
     * {@code arguments} (replayed opaquely, never parsed nor executed); a {@code function_call_output} must pair with a
     * call present in the same document, so tool linkage stays verbatim and no cross-user cache exists; a
     * {@code reasoning} item replays opaquely under REASONING only; and an item with an {@code input_image} part needs
     * VISION. Every {@code function_*} item additionally needs FUNCTION_TOOLS.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。/ called only by {@link #encodeRequest}.
     * @param request 参数 待校验请求文档；parameter the request document to validate.
     * @param declared 参数 route 已声明的能力集合；parameter the capabilities the route declares.
     */
    private static void verifyInput(ObjectNode request, Set<LlmCapabilityEnum> declared) {
        JsonNode input = request.get("input");
        if (input == null || input.isNull()) {
            throw unsupported("input", "input is required by the responses protocol");
        }
        if (input.isTextual()) {
            if (StringUtils.isBlank(input.asText())) {
                throw unsupported("input", "input text must not be blank");
            }
            return;
        }
        if (!input.isArray() || input.isEmpty() || input.size() > MAX_INPUT_ITEMS) {
            throw unsupported("input", "input must be text or 1 to 1000 items");
        }
        Set<String> calls = new HashSet<>();
        for (JsonNode item : input) {
            if (!item.isObject()) {
                throw unsupported("input", "Every input item must be an object");
            }
            JsonNode typeNode = item.get("type");
            String type = typeNode == null || !typeNode.isTextual() ? "message" : typeNode.textValue();
            if (!INPUT_ITEM_TYPES.contains(type)) {
                throw unsupported("input", "The input item type is not part of this protocol");
            }
            if ("message".equals(type) || typeNode == null) {
                JsonNode role = item.get("role");
                if (role == null || !role.isTextual() || !INPUT_MESSAGE_ROLES.contains(role.textValue())) {
                    throw unsupported("input", "An input message item needs a protocol role");
                }
                requireVision(item, declared);
            } else if ("function_call".equals(type)) {
                require(declared, LlmCapabilityEnum.FUNCTION_TOOLS, "input");
                String callId = text(item.get("call_id"));
                if (StringUtils.isBlank(callId)) {
                    throw unsupported("input", "A function call item requires a call_id");
                }
                calls.add(callId);
                if (!FUNCTION_NAME.matcher(StringUtils.defaultString(text(item.get("name")))).matches()) {
                    throw unsupported("input", "A function call item requires a well-formed name");
                }
                JsonNode arguments = item.get("arguments");
                if (arguments != null && !arguments.isNull() && !arguments.isTextual()) {
                    throw unsupported("input", "A function call item carries arguments as an opaque string");
                }
            } else if ("function_call_output".equals(type)) {
                require(declared, LlmCapabilityEnum.FUNCTION_TOOLS, "input");
                String callId = text(item.get("call_id"));
                if (StringUtils.isBlank(callId) || !calls.contains(callId)) {
                    throw unsupported("call_id", "A function call output must pair with a function call of this turn");
                }
                JsonNode output = item.get("output");
                if (output == null || output.isNull() || !(output.isTextual() || output.isArray())) {
                    throw unsupported("input", "A function call output requires text or native content parts");
                }
            } else if ("reasoning".equals(type)) {
                require(declared, LlmCapabilityEnum.REASONING, "input");
            }
        }
    }

    /**
     * 中文说明：识别 input 消息 item 里的图片 part 并要求 VISION 能力声明。
     * English summary: Recognizes image parts inside an input message item and requires the declared VISION capability.
     *
     * 用法 / Usage: 仅由 {@link #verifyInput} 调用。/ called only by {@link #verifyInput}.
     * @param item 参数 消息 item；parameter the message item.
     * @param declared 参数 route 已声明的能力集合；parameter the capabilities the route declares.
     */
    private static void requireVision(JsonNode item, Set<LlmCapabilityEnum> declared) {
        JsonNode content = item.get("content");
        if (!content.isArray()) {
            return;
        }
        for (JsonNode part : content) {
            if ("input_image".equals(text(part.get("type")))) {
                require(declared, LlmCapabilityEnum.VISION, "input");
            }
        }
    }

    /**
     * 中文说明：核对 {@code tools}：必须是至多 64 个函数工具，每项 {@code type=function}、名字符合稳定形态且唯一、
     * {@code parameters} 为对象；网关从不执行工具，只核形状。
     * English summary: Checks {@code tools}: at most 64 function tools, each with {@code type=function}, a unique
     * well-formed name and an object {@code parameters}. The gateway never executes a tool and only checks the shape.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。/ called only by {@link #encodeRequest}.
     * @param request 参数 待校验请求文档；parameter the request document to validate.
     */
    private static void verifyTools(ObjectNode request) {
        JsonNode tools = request.get("tools");
        if (tools == null || tools.isNull()) {
            return;
        }
        if (!tools.isArray() || tools.size() > MAX_TOOLS) {
            throw unsupported("tools", "tools must be an array of at most 64 function tools");
        }
        Set<String> names = new HashSet<>();
        for (JsonNode tool : tools) {
            if (!tool.isObject() || !"function".equals(text(tool.get("type")))) {
                throw unsupported("tools", "Only function tools are served through this gateway");
            }
            String name = StringUtils.defaultString(text(tool.get("name")));
            if (!FUNCTION_NAME.matcher(name).matches() || !names.add(name)) {
                throw unsupported("tools", "Function tool names must be well-formed and unique");
            }
            JsonNode parameters = tool.get("parameters");
            if (parameters != null && !parameters.isNull() && !parameters.isObject()) {
                throw unsupported("tools", "A function tool declares its parameters as an object");
            }
        }
    }

    /**
     * 中文说明：核对 {@code tool_choice}：字符串只能是 none/auto/required，对象只能是引用已声明函数名的
     * {@code {type:"function",name}}；引用未声明的函数即 400，绝不凭空造工具。
     * English summary: Checks {@code tool_choice}: a string may only be none, auto or required and an object may only be
     * {@code {type:"function",name}} referencing a declared function name; a reference to an undeclared function is a 400
     * rather than a manufactured tool.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。/ called only by {@link #encodeRequest}.
     * @param request 参数 待校验请求文档；parameter the request document to validate.
     */
    private static void verifyToolChoice(ObjectNode request) {
        JsonNode choice = request.get("tool_choice");
        if (choice == null || choice.isNull()) {
            return;
        }
        if (choice.isTextual()) {
            if (!TOOL_CHOICE_MODES.contains(choice.textValue())) {
                throw unsupported("tool_choice", "tool_choice is not one of none, auto or required");
            }
            return;
        }
        if (!choice.isObject() || !"function".equals(text(choice.get("type")))
                || !FUNCTION_NAME.matcher(StringUtils.defaultString(text(choice.get("name")))).matches()) {
            throw unsupported("tool_choice", "A function tool choice must name one declared function");
        }
    }

    /**
     * 中文说明：单帧序列校验（首帧屏障专用）：要求载荷是带 {@code type} 的 JSON 对象；序号规则在
     * {@link #parseEvent} 内实现，本方法只借用其结构判定。
     * English summary: Documents the first-frame structural rule: the payload must be a JSON object carrying a
     * {@code type}, with the sequence rules living inside {@link #parseEvent} which this method borrows.
     *
     * 用法 / Usage: 仅由 {@link #streamBody} 的帧回调通过 {@link #parseEvent} 使用同一文法；本方法保留给
     * 输出 item 的形态判定复用。/ the frame callback of {@link #streamBody} uses the same grammar through
     * {@link #parseEvent}; this method stays for the output item shape checks to reuse.
     * @param item 参数 输出或输入 item；parameter an output or input item.
     * @return 返回 item 的判别类型或 null；returns the item discriminator or null.
     */
    private static String itemType(JsonNode item) {
        return item != null && item.isObject() ? text(item.get("type")) : null;
    }

    /**
     * 中文说明：核对一个 {@code output} item 的原生形态：必须是带 {@code type} 的对象，{@code function_call} 必须
     * 自带非空 {@code call_id}、合法 {@code name} 且 {@code arguments} 为字符串（id/call_id/name/arguments/status
     * 全部保真，不重排、不合并），{@code reasoning} 与未知供应商 item 原样保留。
     * English summary: Checks the native shape of one {@code output} item: an object with a {@code type}, where a
     * {@code function_call} must carry a non-blank {@code call_id}, a legal {@code name} and a string
     * {@code arguments} (id, call_id, name, arguments and status all stay faithful, never re-ordered nor merged) while
     * {@code reasoning} and unknown vendor items pass through untouched.
     *
     * 用法 / Usage: 仅由 {@link #decodeUnaryResponse} 调用。/ called only by {@link #decodeUnaryResponse}.
     * @param item 参数 输出 item；parameter one output item.
     */
    private static void validateOutputItem(JsonNode item) {
        if (!item.isObject()) {
            throw protocolError("output", "Every output item must be an object");
        }
        String type = itemType(item);
        if (StringUtils.isBlank(type)) {
            throw protocolError("output", "An output item carries no type discriminator");
        }
        if (OUTPUT_ITEM_TYPES.contains(type) && "function_call".equals(type)) {
            if (StringUtils.isBlank(text(item.get("call_id")))) {
                throw protocolError("call_id", "A function call output item carries no call_id");
            }
            if (StringUtils.isBlank(text(item.get("name")))) {
                throw protocolError("name", "A function call output item carries no function name");
            }
            JsonNode arguments = item.get("arguments");
            if (arguments != null && !arguments.isNull() && !arguments.isTextual()) {
                throw protocolError("arguments", "A function call output item carries arguments as an opaque string");
            }
        }
    }

    /**
     * 中文说明：把整份协议文档序列化成上游请求字节，并强制 {@code yuheng.llm.max-request-bytes}（413
     * {@code request_too_large}）；同时保证 {@code encodeRequest} 的返回体与出网字节同一份形态。
     * English summary: Serializes the whole protocol document into upstream request bytes and enforces
     * {@code yuheng.llm.max-request-bytes} (413 {@code request_too_large}), which also keeps what
     * {@code encodeRequest} returned identical to what actually leaves.
     *
     * 用法 / Usage: 由 {@link #encodeRequest} 末尾与 {@link #exchange} 共用。/ shared by the tail of
     * {@link #encodeRequest} and {@link #exchange}.
     * @param document 参数 协议请求文档；parameter the protocol request document.
     * @param command 参数 本次调用命令，仅用于日志定位；parameter this invocation's command, used to locate the log line.
     * @return 返回 出网请求字节；returns the egress request bytes.
     * @throws LlmInvocationException 413 {@code request_too_large}、400 {@code unsupported_parameter}；raised for an oversized or unserializable document.
     */
    private byte[] serialize(ObjectNode document, LlmInvocationCommandDTO command) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(document);
            if (bytes.length > gatewayProperties.getMaxRequestBytes()) {
                log.warn("llm responses request rejected, alias={} protocol={} bytes={} reason=too-large",
                        command.getModel(), protocol(), bytes.length);
                throw nativeError(HttpURLConnection.HTTP_REQ_TOO_LONG, "request_too_large", "input",
                        "The request document exceeds the managed byte ceiling", false);
            }
            return bytes;
        } catch (JsonProcessingException failure) {
            log.error("llm responses request unserializable, alias={} protocol={}", command.getModel(), protocol());
            throw unsupported("input", "The request document cannot be serialized");
        }
    }

    /**
     * 中文说明：把原生错误对象序列化成错误响应字节。
     * English summary: Serializes the native error object into error response bytes.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 失败分支调用。/ called only by the failure branch of {@link #exchange}.
     * @param failure 参数 已脱敏的原生错误；parameter the masked native error.
     * @return 返回 协议原生错误字节；returns the protocol-native error bytes.
     */
    private byte[] serializeError(LlmInvocationException failure) {
        try {
            return objectMapper.writeValueAsBytes(encodeError(failure));
        } catch (JsonProcessingException illegal) {
            throw new IllegalStateException("YUHENG_LLM_ERROR_ENCODING_FAILED", illegal);
        }
    }

    /**
     * 中文说明：只在 429 时回显形态安全的 {@code Retry-After} 秒数（至多六位数字，其它值一律剥离），其余头部保持
     * 路由阶段产出的安全白名单——{@code secretRef}、baseUrl 与任何供应商身份永远不在头部里。
     * English summary: Echoes {@code Retry-After} only for a 429 and only when it is at most six digits (anything else is
     * stripped), keeping every other header at the safe whitelist routing produced — a secretRef, base URL and any vendor
     * identity never appear in a header.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 失败分支调用。/ called only by the failure branch of {@link #exchange}.
     * @param routed 参数 路由阶段产出的安全头部；parameter the safe headers routing already produced.
     * @param response 参数 上游响应；parameter the upstream response.
     * @param status 参数 本面将要出网的状态；parameter the status this face will leave with.
     * @return 返回 合并后的安全头部；returns the merged safe headers.
     */
    private static Map<String, String> withRetryAfter(LlmInvocationResultVO routed, HttpResponse<?> response,
            int status) {
        Map<String, String> headers = new LinkedHashMap<>(routed.getHeaders());
        if (status == 429) {
            response.headers().firstValue("retry-after")
                    .filter(value -> RETRY_AFTER_SECONDS.matcher(value.trim()).matches())
                    .ifPresent(value -> headers.put("Retry-After", value.trim()));
        }
        return headers;
    }

    /**
     * 中文说明：在总/空闲预算到点时关闭上游流，把阻塞读撞成 IOException 从而让 504 判定可达；使用 JDK 自带的
     * 延时执行器，不自建线程，也不在事务或锁内等待。看门狗只按当前截止点生效一次，续期后的旧任务自动失效。
     * English summary: Closes the upstream stream when the total or idle budget expires so a blocked read turns into an
     * IOException that the 504 judgement can reach, using the JDK's own delayed executor rather than a thread of ours and
     * waiting in no transaction or lock. A watchdog fires once against the deadline it saw, so a stale one expires
     * harmlessly after a renewal.
     *
     * 用法 / Usage: 由 {@link #unaryBody} 与 {@link #streamBody} 调用。/ called by {@link #unaryBody} and {@link #streamBody}.
     * @param stream 参数 上游响应体流；parameter the upstream response body stream.
     * @param abortAt 参数 当前截止点（nanoTime 基准）；parameter the current deadline on the nanoTime scale.
     */
    private static void armAbortWatchdog(InputStream stream, AtomicLong abortAt) {
        long delayNanos = Math.max(1L, abortAt.get() - System.nanoTime());
        CompletableFuture.delayedExecutor(delayNanos, TimeUnit.NANOSECONDS).execute(
                () -> streamAbort(stream, abortAt));
    }

    /**
     * 中文说明：看门狗动作：仅当确实已越过当前截止点才关闭流（关闭幂等，失败就地忽略，因为提交后的流只能终止）。
     * English summary: The watchdog action: closes the stream only when the current deadline really passed, treating the
     * close as idempotent and ignoring its failure, because a committed stream may only end.
     *
     * 用法 / Usage: 仅由 {@link #armAbortWatchdog} 调用。/ called only by {@link #armAbortWatchdog}.
     * @param stream 参数 上游响应体流；parameter the upstream response body stream.
     * @param abortAt 参数 当前截止点；parameter the current deadline.
     */
    private static void streamAbort(InputStream stream, AtomicLong abortAt) {
        if (System.nanoTime() >= abortAt.get()) {
            closeQuietly(stream);
        }
    }

    /**
     * 中文说明：把一次读失败判定成原生错误：越过截止点（看门狗关闭造成的读中断也算）判 504
     * {@code upstream_timeout}，其余判 502 {@code upstream_protocol_error}；原始异常只按类型名脱敏记录。
     * English summary: Judges a read failure natively: past the deadline (including the interruption a watchdog close
     * causes) is 504 {@code upstream_timeout} and anything else is 502 {@code upstream_protocol_error}, with the raw
     * failure logged by its type name only.
     *
     * 用法 / Usage: 由 {@link #unaryBody}、{@link #streamBody} 与 {@link #chunks} 共用。/ shared by {@link #unaryBody},
     * {@link #streamBody} and {@link #chunks}.
     * @param command 参数 本次调用命令；parameter this invocation's command.
     * @param channel 参数 渠道事实，用于日志定位；parameter the channel facts used to locate the log line.
     * @param failure 参数 原始失败；parameter the raw failure.
     * @param abortAt 参数 当前截止点；parameter the current deadline.
     * @return 返回 待抛出的原生错误；returns the native error to throw.
     */
    private LlmInvocationException readFailure(LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.ChannelBO channel, Throwable failure, AtomicLong abortAt) {
        log.error("llm responses upstream read failed, alias={} protocol={} channelKey={} failure={}",
                command.getModel(), protocol(), channel.getChannelKey(), failure.getClass().getSimpleName());
        if (failure instanceof HttpTimeoutException || failure instanceof InterruptedIOException
                || System.nanoTime() >= abortAt.get()) {
            return upstreamTimeout(command, channel);
        }
        return protocolError("response", "The upstream response could not be read");
    }

    /**
     * 中文说明：产出 504 {@code upstream_timeout}（下一次请求可否重试由调用方决定，本面已提交则只终止）。
     * English summary: Produces 504 {@code upstream_timeout}; whether a next request may retry belongs to the caller,
     * and once this face committed it may only end the stream.
     *
     * 用法 / Usage: 由 {@link #readFailure} 与 {@link #streamBody} 调用。/ called by {@link #readFailure} and
     * {@link #streamBody}.
     * @param command 参数 本次调用命令；parameter this invocation's command.
     * @param channel 参数 渠道事实；parameter the channel facts.
     * @return 返回 待抛出的超时错误；returns the timeout error to throw.
     */
    private LlmInvocationException upstreamTimeout(LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.ChannelBO channel) {
        log.warn("llm responses upstream timed out, alias={} protocol={} channelKey={}", command.getModel(), protocol(),
                channel.getChannelKey());
        return nativeError(HttpURLConnection.HTTP_GATEWAY_TIMEOUT, "upstream_timeout", "response",
                "The upstream model did not respond within its timeout budget", false);
    }

    /**
     * 中文说明：产出 502 {@code upstream_protocol_error}：上游没给出本协议要求的结构。
     * English summary: Produces 502 {@code upstream_protocol_error} for a structure this protocol requires that the
     * upstream did not deliver.
     *
     * 用法 / Usage: 由编解码与流式路径共用。/ shared by the codec and streaming paths.
     * @param param 参数 可定位的协议参数名；parameter the locatable protocol parameter name.
     * @param message 参数 安全说明（不含上游原文）；parameter the safe description carrying no upstream text.
     * @return 返回 待抛出的错误；returns the error to throw.
     */
    private static LlmInvocationException protocolError(String param, String message) {
        return nativeError(HttpURLConnection.HTTP_BAD_GATEWAY, "upstream_protocol_error", param, message, false);
    }

    /**
     * 中文说明：产出 400 {@code unsupported_parameter}：字段缺失、形态非法、未声明扩展或自证出网凭据。
     * English summary: Produces 400 {@code unsupported_parameter} for a missing field, an illegal shape, an undeclared
     * extension or an egress or credential self-assertion.
     *
     * 用法 / Usage: 由请求校验路径共用。/ shared by the request validation paths.
     * @param param 参数 可定位的协议参数名；parameter the locatable protocol parameter name.
     * @param message 参数 安全说明；parameter the safe description.
     * @return 返回 待抛出的错误；returns the error to throw.
     */
    private static LlmInvocationException unsupported(String param, String message) {
        return nativeError(HttpURLConnection.HTTP_BAD_REQUEST, "unsupported_parameter", param, message, false);
    }

    /**
     * 中文说明：唯一的错误构造出口，保证四个安全字段一次给全；消息为固定安全文案，永不含上游原文、
     * baseUrl、secretRef 或凭据。
     * English summary: The single error construction point, giving all four safe fields at once with a fixed safe
     * description that never carries upstream text, a base URL, a secretRef or a credential.
     *
     * 用法 / Usage: 由本类全部失败分支调用。/ called by every failure branch of this class.
     * @param status 参数 协议原生状态；parameter the protocol-native status.
     * @param code 参数 稳定机器码；parameter the stable machine code.
     * @param param 参数 协议参数名或 null；parameter the parameter name or null.
     * @param message 参数 安全说明；parameter the safe description.
     * @param retryable 参数 下一次请求可否重试；parameter whether a next request may retry.
     * @return 返回 待抛出的原生错误；returns the native error to throw.
     */
    private static LlmInvocationException nativeError(int status, String code, String param, String message,
            boolean retryable) {
        return new LlmInvocationException(status, code, param, message, retryable);
    }

    /**
     * 中文说明：状态到 Responses 原生 {@code error.type} 的映射，是本面唯一自行决定的字段。
     * English summary: The status to native {@code error.type} mapping, which is the single field this face maps itself.
     *
     * 用法 / Usage: 仅由 {@link #encodeError} 调用。/ called only by {@link #encodeError}.
     * @param status 参数 协议原生状态；parameter the protocol-native status.
     * @return 返回 原生错误类型；returns the native error type.
     */
    private static String nativeErrorType(int status) {
        if (status == 401) {
            return "authentication_error";
        }
        if (status == 403) {
            return "permission_error";
        }
        if (status == 404) {
            return "not_found_error";
        }
        if (status == 429) {
            return "rate_limit_error";
        }
        if (status >= 500) {
            return "server_error";
        }
        return "invalid_request_error";
    }

    /**
     * 中文说明：能力闸门：未声明即 400 {@code unsupported_parameter}，参数名指向承载该能力的字段。
     * English summary: The capability gate: an undeclared capability is a 400 {@code unsupported_parameter} whose
     * parameter name points at the field carrying that capability.
     *
     * 用法 / Usage: 仅由 {@link #verifyInput} 与 {@link #requireVision} 调用。/ called only by {@link #verifyInput} and
     * {@link #requireVision}.
     * @param declared 参数 route 已声明的能力集合；parameter the capabilities the route declares.
     * @param required 参数 本字段要求的能力；parameter the capability this field requires.
     * @param param 参数 可定位的协议参数名；parameter the locatable parameter name.
     */
    private static void require(Set<LlmCapabilityEnum> declared, LlmCapabilityEnum required, String param) {
        if (!declared.contains(required)) {
            throw unsupported(param, "The request uses a capability the routed model does not declare");
        }
    }

    /**
     * 中文说明：读取文本字段值，非文本返回 {@code null}；绝不记录也不回显内容。
     * English summary: Reads a textual field value and yields {@code null} for anything else, never logging nor echoing
     * content.
     *
     * 用法 / Usage: 由请求与响应校验路径共用。/ shared by the request and response validation paths.
     * @param node 参数 字段节点；parameter the field node.
     * @return 返回 字段文本或 null；returns the field text or null.
     */
    private static String text(JsonNode node) {
        return node != null && node.isTextual() ? node.textValue() : null;
    }

    /**
     * 中文说明：幂等关闭上游资源并就地吞掉失败：本面已产出的字节与缓冲区不受影响，也不引入新的失败路径。
     * English summary: Closes an upstream resource idempotently and swallows its failure locally: the bytes and buffers
     * this face already produced are unaffected and no new failure path appears.
     *
     * 用法 / Usage: 由 {@link #exchange}、{@link #unaryBody}、{@link #streamBody} 与 {@link #chunks} 共用。/ shared by
     * {@link #exchange}, {@link #unaryBody}, {@link #streamBody} and {@link #chunks}.
     * @param closeable 参数 待关闭资源，可为 null；parameter the resource to close, null accepted.
     */
    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
            // 提交后的流只能终止，关闭失败不引入新的对外事实。 / a committed stream may only end, so a close failure adds no outward fact.
        }
    }
}
