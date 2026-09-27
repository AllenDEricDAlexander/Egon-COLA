package top.egon.cola.component.yuheng.llm.proxy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
import top.egon.cola.component.yuheng.llm.config.LlmGatewayProperties;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.dto.LlmInvocationCommandDTO;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmCapabilityEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.exception.LlmInvocationException;
import top.egon.cola.component.yuheng.llm.proxy.domain.vo.LlmInvocationResultVO;

/**
 * 中文说明：{@code AnthropicMessagesProtocolStrategy} 是四种同协议 Strategy 里的 Messages（Anthropic 原生）面，
 * {@code llmProtocolStrategyRegistry} 用 bean 名 {@code anthropicMessagesProtocolStrategy} 把它归到
 * {@link LlmProtocolEnum#ANTHROPIC_MESSAGES}，因此它只服务 {@code POST /v1/messages}，永不回落 Chat、也永不把
 * Messages 结构翻译成 Chat 的 {@code choices}/{@code tool_calls}。四项职责按 {@link LlmProtocolStrategy} 的字面合同落地：
 * ① {@link #encodeRequest} 把 {@code system} 永久留在<b>顶层</b>（文本或 text blocks，绝不折进 {@code messages}），
 * {@code messages} 只接受 user/assistant 并按 content blocks 原序透传（{@code text}/{@code image}/
 * {@code tool_use}(assistant)/{@code tool_result}(user 且带 {@code tool_use_id})/{@code thinking}(带 signature)/
 * {@code redacted_thinking}），{@code max_tokens} 是本协议<b>必填</b>的输出预算字段（借别的 {@code max_completion_tokens}
 * /{@code max_output_tokens} 一律 400），{@code tools}（{@code input_schema} 必须是对象、name 唯一）与
 * {@code tool_choice}(auto/none/any/tool) 原样送出；只把 {@code model} 改写为 {@code route.upstreamModel}，
 * 拒绝面是<b>黑名单</b>：能力未声明的请求扩展与任何 {@code upstream_url}/{@code base_url}/{@code api_key}/
 * {@code authorization}/{@code credential}/{@code host} 式自证出网、凭据或别名仿冒字段直接 400，未知但安全的字段
 * 连同其结构与数值表示原样透传，绝不因不在已知清单里被丢弃，整份文档受 {@code yuheng.llm.max-request-bytes} 约束；
 * ② {@link #decodeUnaryResponse} 单播保真：{@code content} 块序不变、{@code tool_use} 保留 {@code id} 与其
 * （可能只填到一半的）{@code input} 对象、{@code thinking} 保留 {@code signature}、{@code redacted_thinking} 保留
 * {@code data}，并保留 {@code stop_reason}/{@code stop_sequence}/{@code usage} 与一切未知但安全的字段，只把
 * {@code model} 回写为客户端 alias；缺必需结构是 502 {@code upstream_protocol_error}，绝不退化成空成功；
 * ③ {@link #encodeStream} 收的是上游<b>原始字节流</b>：本方法自己攒字节、按真正的 SSE 帧文法切帧（空行收帧、
 * {@code \r\n} 与 {@code \n} 都是行尾、{@code data:} 去前缀与其后一个可选空格、多行 {@code data} 以 {@code \n} 拼接、
 * {@code event:}/{@code id:}/{@code retry:} 与 {@code :} 注释各按 SSE 处置、载荷折行原样续接），一帧跨多个缓冲区、
 * 多帧挤进一个缓冲区都得成立，绝不要求调用方「一个缓冲区一个裸 JSON 对象」；随后按 Messages 事件文法解析再同文法
 * 出帧：{@code message_start}→{@code content_block_start}/
 * {@code content_block_delta}/{@code content_block_stop}→{@code message_delta}→{@code message_stop}，允许并转发
 * {@code ping}，合法未知事件原样转发，{@code input_json_delta.partial_json} 只按块索引累积、<b>到 {@code content_block_stop} 才整体解析</b>
 * （绝不半段强解析、绝不丢弃），{@code signature_delta} 原样保留；EOF 没有 {@code message_stop} 即 ABORTED，
 * 绝不伪造终态帧，流内失败只能以本协议 {@code error} 事件收束或以断开结束；预取恒为 1、单帧受
 * {@code yuheng.llm.max-frame-bytes} 限制、下游取消传播为上游关闭、每个 {@link DataBuffer} 恰好释放一次；
 * ④ {@link #encodeError} 只产出 {@code {"type":"error","error":{"type","message"}}}，永不套 OpenAI 的
 * error/choices 形状、永不出 admin 的 {@code code}/{@code data} 信封，内容只来自 {@link LlmInvocationException}
 * 的四个安全字段。编排入口 {@link #exchange} 用 JDK 原生 {@link HttpClient} 就地按渠道四段超时预算发起同协议调用，
 * 凭据只在 {@code yuheng.llm.secrets-root} 之下解析 {@code secretRef}，提交屏障是「一个完整合法帧」：
 * 首帧验证之前任何失败都按原生状态抛出（饱和即 429、超时即 504），一旦提交则不换模型、不重试、不改 HTTP 状态，
 * 非 2xx 上游按原生状态交 {@link #encodeError}，且全程不在事务或锁内等待上游。日志只有 alias/协议/渠道 key/状态与
 * 块计数，永不出正文、thinking 文本、签名与密钥。
 * English summary: {@code AnthropicMessagesProtocolStrategy} is the Messages (Anthropic-native) face of the four
 * same-protocol Strategies; {@code llmProtocolStrategyRegistry} files the bean named
 * {@code anthropicMessagesProtocolStrategy} under {@link LlmProtocolEnum#ANTHROPIC_MESSAGES}, so it serves only
 * {@code POST /v1/messages} and never falls back to Chat nor translates a Messages structure into Chat's
 * {@code choices}/{@code tool_calls}. The four duties follow the literal {@link LlmProtocolStrategy} contract:
 * (1) {@link #encodeRequest} keeps {@code system} as a <b>top-level</b> field (text or text blocks, never folded into
 * {@code messages}), accepts only user/assistant roles and forwards content blocks in their original order
 * ({@code text}/{@code image}/{@code tool_use} on assistant/{@code tool_result} on user carrying {@code tool_use_id}/
 * {@code thinking} carrying {@code signature}/{@code redacted_thinking}), treats {@code max_tokens} as this protocol's
 * <b>required</b> output budget (a borrowed {@code max_completion_tokens}/{@code max_output_tokens} is a 400), keeps
 * {@code tools} ({@code input_schema} must be an object, names unique) and {@code tool_choice}
 * (auto/none/any/tool) exactly as sent, rewrites only {@code model} into {@code route.upstreamModel}, refuses through a
 * <b>blacklist</b> — a request extension whose capability was never declared and any self-asserted
 * {@code upstream_url}/{@code base_url}/{@code api_key}/{@code authorization}/{@code credential}/{@code host} egress,
 * credential or alias-spoofing field are a 400 — while an unknown-but-safe field travels with its own structure and
 * numeric representation instead of being dropped for missing a known-name list, and caps the whole document at
 * {@code yuheng.llm.max-request-bytes}; (2)
 * {@link #decodeUnaryResponse} preserves block order, a {@code tool_use} {@code id} with its (possibly partially
 * filled) {@code input} object, the {@code thinking} {@code signature} and {@code redacted_thinking} {@code data},
 * together with {@code stop_reason}/{@code stop_sequence}/{@code usage} and every unknown-but-safe field, rewriting
 * only {@code model} back to the client alias — missing required structure is a 502
 * {@code upstream_protocol_error}, never an empty success; (3) {@link #encodeStream} is handed the upstream's
 * <b>raw byte flux</b> and therefore accumulates bytes and parses the real SSE frame grammar itself: a frame closes on a
 * blank line, {@code \r\n} and {@code \n} both end a line, a {@code data:} line loses its prefix plus one optional
 * following space, several {@code data:} lines of one frame join with {@code \n}, {@code event:}/{@code id:}/
 * {@code retry:} and a {@code :} comment are handled as SSE says and a wrapped payload line is continued verbatim, so a
 * frame split across buffers or several frames coalesced into one buffer both work and no caller is ever required to
 * hand over one bare JSON object per buffer; it then parses and re-emits the Messages
 * event grammar {@code message_start}→{@code content_block_start}/{@code content_block_delta}/{@code content_block_stop}
 * →{@code message_delta}→{@code message_stop}, forwards {@code ping} and a legitimate unknown event verbatim, accumulates
 * {@code input_json_delta.partial_json} per block index and parses it <b>only once the block is complete</b> (never
 * half-parsed, never dropped), keeps {@code signature_delta} intact, judges an EOF without {@code message_stop}
 * ABORTED instead of fabricating a terminal frame, ends an in-stream failure with this protocol's {@code error} event
 * or a disconnect, prefetches exactly one frame, caps frames at {@code yuheng.llm.max-frame-bytes}, propagates a
 * downstream cancellation upstream and releases every {@link DataBuffer} once; (4) {@link #encodeError} emits only
 * {@code {"type":"error","error":{"type","message"}}}, never borrowing the OpenAI error/choices envelope nor the admin
 * {@code code}/{@code data} wrapper and drawing only on the four safe fields of {@link LlmInvocationException}. The
 * orchestration entry {@link #exchange} issues the same-protocol call with a locally built JDK {@link HttpClient}
 * sized by the route channel's four timeout budgets, resolves the credential for {@code secretRef} only under
 * {@code yuheng.llm.secrets-root}, and treats one complete valid frame as the commit barrier: everything before that
 * barrier raises natively (saturation is a 429, a timeout a 504) while after it no model switch, retry or HTTP-status
 * rewrite is possible, a non-2xx upstream answers with its native status through {@link #encodeError}, and no wait on
 * upstream ever happens inside a transaction or a lock. Logs carry only alias, protocol, channel key, status and block
 * counts — never content, thinking text, signatures or keys.
 *
 * 用法 / Usage: 由 {@code LlmApiController} 经 {@code llmProtocolStrategyRegistry} 按协议取到本 bean：先
 * {@code exchange(command, route, routed)}；{@code LlmProtocolContractTest} 亦可直接驱动四个编解码操作在无真实模型
 * 的情况下固定原生 wire shape。构造契约与其它三面完全相同：两个 {@code private final} 协作者按名注入。
 * / Resolve this bean by protocol through {@code llmProtocolStrategyRegistry} and call
 * {@code exchange(command, route, routed)}; {@code LlmProtocolContractTest} may also drive the four codec operations
 * directly to pin the native wire shape without a live model. The construction contract is identical to the other
 * three faces: exactly two {@code private final} collaborators, both qualified by name.
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@Component("anthropicMessagesProtocolStrategy")
public class AnthropicMessagesProtocolStrategy implements LlmProtocolStrategy {

    /** 中文说明：Messages 唯一被允许的上游路径，协议名不推断、不改写。 English summary: the only upstream path Messages may call; the protocol is never guessed nor rewritten. */
    private static final String MESSAGES_PATH = "/v1/messages";

    /** 中文说明：首版唯一受支持的协议版本，出网恒定带上该头。 English summary: the only protocol version this face supports, always sent upstream. */
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    /** 中文说明：{@code anthropic-version} 头名。 English summary: the {@code anthropic-version} header name. */
    private static final String VERSION_HEADER = "anthropic-version";

    /** 中文说明：Anthropic 原生凭据头，由 engine 用 secretRef 重建，客户端凭据永不转发。 English summary: the native Anthropic credential header, rebuilt by the engine from secretRef; client credentials are never relayed. */
    private static final String API_KEY_HEADER = "x-api-key";

    /** 中文说明：429 唯一被允许透传的安全响应头，且只接受纯数字。 English summary: the only safe response header allowed through on a 429, and only when it is purely numeric. */
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    /** 中文说明：SSE 帧的四个字段前缀：出帧严格按 {@code event:}+{@code data:}+空行；入帧另认 {@code id:} 与
     * {@code retry:} 两个流控字段——它们属于分帧文法但不进载荷。 English summary: the four SSE field prefixes: frames
     * leave as {@code event:} plus {@code data:} plus a blank line, while parsing additionally knows the {@code id:} and
     * {@code retry:} flow-control fields, which belong to the frame grammar but never to the payload. */
    private static final String EVENT_FIELD = "event:";
    private static final String DATA_FIELD = "data:";
    private static final String ID_FIELD = "id:";
    private static final String RETRY_FIELD = "retry:";

    /** 中文说明：一次请求最多 100 条 messages、一条响应最多 100 个 content block。 English summary: at most 100 messages per request and 100 content blocks per response stream. */
    private static final int MAX_MESSAGES = 100;
    private static final int MAX_CONTENT_BLOCKS = 100;

    /** 中文说明：工具定义、名称、工具 ID、停止序列与 metadata 的长度与数量上界，逐字对应 Spec §9.0.1/§9.2.30。 English summary: the tool, name, tool-id, stop-sequence and metadata ceilings taken verbatim from Spec §9.0.1/§9.2.30. */
    private static final int MAX_TOOLS = 64;
    private static final int MAX_TOOL_NAME_CHARS = 64;
    private static final int MAX_TOOL_ID_CHARS = 128;
    private static final int MAX_STOP_SEQUENCES = 4;
    private static final int MAX_STOP_SEQUENCE_CHARS = 200;
    private static final int MAX_METADATA_USER_ID_CHARS = 256;

    /** 中文说明：跨块拼装后的工具入参上限，防止超长 {@code partial_json} 把堆吃空。 English summary: the ceiling of a tool input assembled across fragments, which keeps an oversized {@code partial_json} stream from exhausting the heap. */
    private static final int MAX_ASSEMBLED_INPUT_CHARS = 262_144;

    /** 中文说明：工具/消息函数名字符集，与 Chat/Responses 面同一约束。 English summary: the tool name character class, the same constraint the Chat and Responses faces apply. */
    private static final Pattern TOOL_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    /** 中文说明：任何自证出网/凭据/别名仿冒字段名（大小写不敏感），出现在顶层、message 对象、content block 对象、
     * tool、thinking 或 metadata 内即拒——这是 Spec §9.0.1「扩展字段」行给出的黑名单：网关只拒绝被自证的出网目标与
     * 凭据，以及 caller 假装的 route/upstream 状态值，从不因「名字不在清单里」丢弃一个安全字段。
     * English summary: every self-asserted egress, credential or alias-spoofing field name, matched case-insensitively and
     * refused in the document root, a message object, a content block, a tool, {@code thinking} or {@code metadata} — the
     * blacklist Spec §9.0.1's extension row gives: the gateway refuses a self-asserted egress target or credential, and a
     * caller-faked route/upstream value, and never drops a safe field merely because its name is not on a list. */
    private static final Set<String> SELF_ASSERTED_EGRESS_FIELDS = Set.of(
            "url", "uri", "endpoint", "host", "authority", "baseurl", "base_url", "upstreamurl", "upstream_url",
            "upstreammodel", "upstream_model", "upstream_status", "model_override", "override_model", "modelname",
            "model_name", "target", "proxy", "forward_to", "credential", "credentials",
            "api_key", "apikey", "x-api-key", "anthropic_api_key", "authorization", "authentication", "bearer",
            "token", "access_token", "secret", "secretref", "secret_ref", "secret_ref_value");

    /** 中文说明：messages 只接受的两个角色，{@code system} 永远不是消息角色。 English summary: the two roles messages accept; {@code system} is never a message role. */
    private static final Set<String> MESSAGE_ROLES = Set.of("user", "assistant");

    /** 中文说明：tool_choice 的四种原生类型。 English summary: the four native {@code tool_choice} types. */
    private static final Set<String> TOOL_CHOICE_TYPES = Set.of("auto", "none", "any", "tool");

    /** 中文说明：{@code thinking} 的两种原生开关。 English summary: the two native {@code thinking} switches. */
    private static final Set<String> THINKING_TYPES = Set.of("enabled", "disabled");

    /** 中文说明：出网请求头的名字，值只有媒体类型与本进程凭据，绝不含客户端原头的透传。 English summary: the egress request header names, whose values are only media types and this process's own credential, never a relayed client header. */
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String JSON_MEDIA_TYPE = "application/json";

    /** 中文说明：Messages 原生错误类别集合，命中即按原值出网，否则由状态映射。 English summary: the native Messages error categories; a match travels as-is, anything else is mapped from the status. */
    private static final Set<String> NATIVE_ERROR_TYPES = Set.of(
            "invalid_request_error", "authentication_error", "permission_error", "not_found_error",
            "request_too_large", "rate_limit_error", "api_error", "overloaded_error");

    /** 中文说明：SSE 分帧用到的换行字节；UTF-8 续字节恒 ≥0x80，因此分帧永不误切多字节字符。 English summary: the newline bytes the SSE framer uses; UTF-8 continuation bytes are always ≥ 0x80, so framing never splits a multi-byte character. */
    private static final byte LINE_FEED = (byte) '\n';
    private static final byte CARRIAGE_RETURN = (byte) '\r';

    /** 中文说明：读上游的块大小与拼装缓冲初值，与字节上限无关。 English summary: the upstream read chunk and the initial assembly buffer, both independent of the byte ceilings. */
    private static final int UPSTREAM_CHUNK_BYTES = 8_192;
    private static final int STREAM_BUFFER_BYTES = 8_192;

    /** 中文说明：本进程的出网边界（{@code secrets-root} 与请求/帧上限），按名注入且不提供宽松默认。 English summary: this process's egress boundary ({@code secrets-root} plus the request/frame ceilings), injected by name with no permissive default. */
    @Qualifier(LlmGatewayProperties.BEAN_NAME)
    private final LlmGatewayProperties gatewayProperties;

    /** 中文说明：容器唯一的 Jackson 栈，用作协议文档载体，禁止另起第二套 JSON 实现。 English summary: the container's single Jackson stack used as the protocol document carrier, which forbids a second JSON implementation. */
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：本实现的身份，恒为 {@link LlmProtocolEnum#ANTHROPIC_MESSAGES}：注册表用它建只读 EnumMap，与本 bean 名
     * 不符即启动失败，绝不回落 Chat。
     * English summary: This adapter's identity, always {@link LlmProtocolEnum#ANTHROPIC_MESSAGES}: the registry builds its
     * read-only EnumMap from it, and a mismatch with this bean name fails startup instead of falling back to Chat.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code AnthropicMessagesProtocolStrategy.protocol()}。
     * @return 返回 Messages 协议常量；returns the Messages protocol constant.
     */
    @Override
    public LlmProtocolEnum protocol() {
        return LlmProtocolEnum.ANTHROPIC_MESSAGES;
    }

    /**
     * 中文说明：职责①请求编码。拒绝面是<b>黑名单</b>而不是白名单：任何自证出网/凭据/别名仿冒字段（{@code upstream_url}、
     * {@code base_url}、{@code api_key}、{@code x-api-key}、{@code authorization}、{@code credential}、{@code host} 等，
     * 见 {@code SELF_ASSERTED_EGRESS_FIELDS}）与借自其它协议的输出预算（{@code max_completion_tokens}/
     * {@code max_output_tokens}）一律 400，未落在 route 已声明能力上的扩展同样 400（{@code image}→VISION、
     * 工具族→FUNCTION_TOOLS、thinking 族→REASONING）；除此之外，<b>未知但安全的字段按原结构与原数值表示透传</b>，
     * 绝不因「名字不在已知清单里」被静默丢弃（Spec §9.0.1「扩展字段」行与「请求可扩展结构仅在 route capability
     * 校验后透传，不开放任意URL/凭据字段」）。已知必需字段的合同照核：{@code model} 必须是客户端 alias 且与命令一致、
     * {@code max_tokens} 必须是正整数（本协议唯一的输出预算字段）、{@code messages} 为 1–100 条且角色只有
     * user/assistant、{@code system} 留在顶层（文本或 text blocks）、content blocks 按原序逐块校验（{@code tool_use}
     * 只能在 assistant 且带 1–128 的 id 与对象 {@code input}，{@code tool_result} 只能在 user 且带
     * {@code tool_use_id}，{@code thinking} 必带 {@code signature}），{@code tools}/{@code tool_choice} 原样保留，
     * {@code thinking.budget_tokens} 只核是正整数且小于本请求 {@code max_tokens}（绝对下限属模型事实，由上游核定）；
     * 随后只把 {@code model} 改写为 {@code route.upstreamModel}、把 {@code stream} 写成命令的 stream 位，并在
     * {@code yuheng.llm.max-request-bytes} 之内返回。
     * English summary: Duty (1) request encoding. The refusal surface is a <b>blacklist</b>, never a whitelist: any
     * self-asserted egress, credential or alias-spoofing field ({@code upstream_url}, {@code base_url}, {@code api_key},
     * {@code x-api-key}, {@code authorization}, {@code credential}, {@code host} and friends, see
     * {@code SELF_ASSERTED_EGRESS_FIELDS}), a budget borrowed from another protocol ({@code max_completion_tokens}/
     * {@code max_output_tokens}) and an extension whose capability the route never declared ({@code image}→VISION, the tool
     * family→FUNCTION_TOOLS, the thinking family→REASONING) are a 400, while everything else — unknown but safe —
     * <b>travels with its original structure and numeric representation</b> and is never silently dropped because its name
     * is not on a known list (Spec §9.0.1's extension row: extendable request structures pass through once the route
     * capability check has run, and no arbitrary URL or credential field is opened). The known required fields keep being
     * verified: {@code model} must be the client alias agreeing with the command, {@code max_tokens} a positive integer
     * (this protocol's only output budget), {@code messages} 1–100 entries whose roles are only user/assistant,
     * {@code system} top-level (text or text blocks), content blocks validated in their original order ({@code tool_use}
     * only on assistant with a 1–128 id and an object {@code input}, {@code tool_result} only on user carrying
     * {@code tool_use_id}, {@code thinking} always carrying its {@code signature}), {@code tools}/{@code tool_choice}
     * exactly as sent, and {@code thinking.budget_tokens} checked only as a positive integer below this request's
     * {@code max_tokens} — an absolute floor is a per-model upstream fact, which upstream adjudicates. Afterwards only
     * {@code model} becomes {@code route.upstreamModel}, {@code stream} becomes the command's flag, and the document is
     * returned inside {@code yuheng.llm.max-request-bytes}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code encodeRequest(command, route)}，返回体直接作为上游请求正文。
     * @param command 参数 已校验的调用命令，提供 alias、stream 位与原生请求文档；parameter the validated command carrying the alias, the stream flag and the native request document.
     * @param route 参数 已选定的候选 route，提供唯一可出网的 upstreamModel 与能力声明；parameter the selected candidate route supplying the only egress-able upstreamModel and its capability declarations.
     * @return 返回 协议原生请求文档，除 {@code model} 与 {@code stream} 外与客户端提交同构（字段、结构与数值表示都不丢）；returns the native request document, structurally identical to what the client submitted apart from {@code model} and {@code stream}, keeping every field, structure and numeric representation.
     * @throws LlmInvocationException 400 {@code unsupported_parameter}、413 {@code request_too_large}；the native status travels with the error.
     */
    @Override
    public ObjectNode encodeRequest(LlmInvocationCommandDTO command, LlmModelSnapshotBO.RouteBO route) {
        ObjectNode payload = command.getPayload();
        if (payload == null) {
            throw unsupported("payload");
        }
        Set<LlmCapabilityEnum> offered = route.getCapabilities() == null
                ? Set.of() : route.getCapabilities();
        rejectSelfAssertedEgress(payload, "payload");
        requireAlias(payload, command.getModel());
        requireOutputBudget(payload);
        requireSystem(payload);
        requireMessages(payload, offered);
        requireTools(payload, offered);
        requireToolChoice(payload, offered);
        requireSampling(payload);
        requireStopSequences(payload);
        requireThinking(payload, offered);
        requireMetadata(payload);
        requireStreamFlag(payload);
        ObjectNode encoded = payload.deepCopy();
        encoded.put("model", route.getUpstreamModel());
        encoded.put("stream", Boolean.TRUE.equals(command.getStream()));
        enforceRequestCeiling(encoded, command.getModel());
        log.debug("llm messages request encoded alias={} protocol={} channel={} messages={}", command.getModel(),
                protocol(), route.getChannelKey(), encoded.path("messages").size());
        return encoded;
    }

    /**
     * 中文说明：职责②单播解码/透传。只处理成功文档：要求顶层 {@code type=message}、文本 {@code id}、
     * {@code role=assistant}、数组 {@code content}、存在的 {@code stop_reason}、对象 {@code usage} 与文本
     * {@code model}；逐块保真——{@code text} 带文本、{@code tool_use} 带 1–128 的 {@code id}、name 与（可能只填了一半的）
     * 对象 {@code input}、{@code thinking} 必带 {@code signature}、{@code redacted_thinking} 必带 {@code data}，
     * 未知块类型原样保留；最后仅把 {@code model} 回写为客户端 alias。绝不产出 {@code choices}/{@code tool_calls}，
     * 绝不把缺失的 {@code usage} 伪造成 0，也绝不把结构缺失变成空成功。
     * English summary: Duty (2) unary decoding and pass-through. Success documents only: the root must be
     * {@code type=message} with a textual {@code id}, {@code role=assistant}, an array {@code content}, a present
     * {@code stop_reason}, an object {@code usage} and a textual {@code model}; every block stays faithful — {@code text}
     * carries text, {@code tool_use} carries its 1–128 {@code id}, name and (possibly only half-filled) object
     * {@code input}, {@code thinking} must carry its {@code signature} and {@code redacted_thinking} its {@code data} —
     * while an unknown block type is kept as-is; finally only {@code model} is written back to the client alias. No
     * {@code choices}/{@code tool_calls} are ever produced, a missing {@code usage} is never fabricated as zero and missing
     * structure never becomes an empty success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code decodeUnaryResponse(command, upstreamBody)}。
     * @param command 参数 本次调用命令，提供回写给客户端的 alias；parameter this invocation's command, supplying the alias to write back to the client.
     * @param upstreamBody 参数 上游成功响应的原生 JSON 文档；parameter the native JSON document of the successful upstream response.
     * @return 返回 保真后的原生响应文档，{@code model} 已恢复为客户端 alias；returns the fidelity-preserving native response document with {@code model} restored to the client alias.
     * @throws LlmInvocationException 502 {@code upstream_protocol_error}（本协议必需结构缺失）；raised when a structure this protocol requires is absent.
     */
    @Override
    public ObjectNode decodeUnaryResponse(LlmInvocationCommandDTO command, ObjectNode upstreamBody) {
        if (!"message".equals(textField(upstreamBody, "type"))) {
            throw protocolError("the upstream Messages document is not a message object", "type");
        }
        if (StringUtils.isBlank(textField(upstreamBody, "id"))) {
            throw protocolError("the upstream Messages document has no message id", "id");
        }
        if (!"assistant".equals(textField(upstreamBody, "role"))) {
            throw protocolError("the upstream Messages document is not an assistant turn", "role");
        }
        if (StringUtils.isBlank(textField(upstreamBody, "model"))) {
            throw protocolError("the upstream Messages document declares no model", "model");
        }
        if (upstreamBody.get("stop_reason") == null) {
            throw protocolError("the upstream Messages document has no stop_reason", "stop_reason");
        }
        JsonNode stopSequence = upstreamBody.get("stop_sequence");
        if (stopSequence != null && !stopSequence.isNull() && !stopSequence.isTextual()) {
            throw protocolError("the upstream Messages stop_sequence is not text", "stop_sequence");
        }
        if (!upstreamBody.path("usage").isObject()) {
            throw protocolError("the upstream Messages document has no usage object", "usage");
        }
        JsonNode content = upstreamBody.get("content");
        if (content == null || !content.isArray() || content.size() > MAX_CONTENT_BLOCKS) {
            throw protocolError("the upstream Messages document has no content array", "content");
        }
        for (JsonNode block : content) {
            requireResponseBlock(block);
        }
        ObjectNode decoded = upstreamBody.deepCopy();
        decoded.put("model", command.getModel());
        log.debug("llm messages unary decoded alias={} protocol={} blocks={} stopReason={}", command.getModel(),
                protocol(), content.size(), StringUtils.defaultString(textField(upstreamBody, "stop_reason")));
        return decoded;
    }

    /**
     * 中文说明：职责③流式帧解码与编码。把上游字节按 SSE 帧边界（LF/CRLF/CR 双换行）切成帧、逐帧解析校验后按同协议
     * 重新出帧；顺序与终态由 Messages 文法决定：{@code ping} 允许并转发，{@code message_start} 必须且只能出现一次并
     * 回写 alias，块事件必须落在已开启的 index 上，{@code input_json_delta.partial_json} 只累积到
     * {@code content_block_stop} 才整体解析（半段绝不解析、一块绝不丢），{@code signature_delta} 必须落在 thinking 块上
     * 且原样保留，未知但合法的事件原样转发，{@code message_delta} 的 usage 是累计值因此永不重复累加。
     * 返回流严格有界：{@code limitRate(1)} 保证预取不超过一帧、单帧字节超 {@code yuheng.llm.max-frame-bytes} 即失败、
     * 终帧之后立即取消上游（取消传播）、每个入站 {@link DataBuffer} 在 finally 里恰好释放一次；EOF 缺少
     * {@code message_stop} 判 ABORTED——先按本协议 {@code error} 事件收束再抛出异常断开，绝不伪造 {@code message_stop}。
     * English summary: Duty (3) streaming frame decoding and encoding. The upstream bytes are cut into SSE frames on the
     * LF/CRLF/CR blank-line boundary, each frame parsed and validated, then re-emitted in the same protocol; ordering and
     * the terminal marker follow the Messages grammar: {@code ping} is allowed and forwarded, {@code message_start} must
     * appear exactly once and has its alias written back, block events must land on an opened index,
     * {@code input_json_delta.partial_json} is only accumulated and is parsed once at {@code content_block_stop} (never
     * half-parsed, never dropped), {@code signature_delta} must belong to a thinking block and stays intact, legitimate
     * unknown events pass through, and {@code message_delta}'s usage is cumulative so it is never re-added. The returned
     * flux is strictly bounded: {@code limitRate(1)} keeps prefetch at one frame, a frame larger than
     * {@code yuheng.llm.max-frame-bytes} fails, the upstream is cancelled immediately after the terminal frame (cancel
     * propagates), and every inbound {@link DataBuffer} is released exactly once in a finally; an EOF without
     * {@code message_stop} is ABORTED — the protocol's own {@code error} event closes the stream before the failure
     * signal, and a {@code message_stop} is never fabricated.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code encodeStream(command, upstreamFrames)}，交给
     * {@code LlmServletStreamComponent} 逐帧写出。
     * @param command 参数 本次调用命令，提供逐帧回写 alias 的目标名；parameter this invocation's command, supplying the alias each frame must carry.
     * @param upstreamFrames 参数 上游响应体的有界字节流；parameter the bounded byte flux of the upstream response body.
     * @return 返回 同协议客户端字节流，含终态帧；returns the same-protocol client byte flux including its terminal frame.
     * @throws LlmInvocationException 提交前 502 {@code upstream_protocol_error}；提交后以本协议错误事件与流终止收束，不改状态。
     *                                Pre-commit 502 {@code upstream_protocol_error}; after commit the stream closes with the native error event and a terminal signal, never a new status.
     */
    @Override
    public Flux<DataBuffer> encodeStream(LlmInvocationCommandDTO command, Flux<DataBuffer> upstreamFrames) {
        String alias = command.getModel();
        return Flux.defer(() -> {
            byte[][] holder = new byte[][] {new byte[STREAM_BUFFER_BYTES]};
            int[] accumulated = new int[1];
            AtomicBoolean started = new AtomicBoolean();
            AtomicBoolean terminated = new AtomicBoolean();
            AtomicInteger pendingFrames = new AtomicInteger();
            Map<Integer, String> openBlocks = new HashMap<>();
            Map<Integer, StringBuilder> partialJson = new HashMap<>();
            AtomicInteger blockCount = new AtomicInteger();
            return upstreamFrames
                    .limitRate(1)
                    .concatMap(chunk -> {
                        List<DataBuffer> emitted = new ArrayList<>(2);
                        try {
                            drainFrames(readableBytes(chunk), holder, accumulated, alias, started, terminated,
                                    openBlocks, partialJson, blockCount, emitted);
                            pendingFrames.set(emitted.size());
                            return Flux.fromIterable(emitted);
                        } catch (RuntimeException failure) {
                            emitted.forEach(DataBufferUtils::release);
                            throw failure;
                        } finally {
                            DataBufferUtils.release(chunk);
                        }
                    })
                    .takeUntil(delivered -> pendingFrames.decrementAndGet() <= 0 && terminated.get())
                    .onErrorResume(failure -> closeWith(failure, terminated, alias))
                    .concatWith(Flux.defer(() -> terminated.get()
                            ? Flux.<DataBuffer>empty()
                            : closeWith(abort(), terminated, alias)));
        });
    }

    /**
     * 中文说明：流内的失败收束，也是「一个完整合法帧之后」唯一的结束方式：已经把终态帧交给客户端就什么都不再做
     * （绝不补发第二份错误、绝不伪造 {@code message_stop}）；否则先按本协议出一个 {@code error} 事件帧，再以异常终止
     * 信号断开。异常本身一律换成 {@link LlmInvocationException} 的四个安全字段，因此上游原文、签名与密钥都不可能
     * 随错误离开本进程。
     * English summary: How an in-stream failure ends, which is the only way anything may finish after one complete valid
     * frame has left: when a terminal frame was already handed over nothing follows at all (a second error is never added
     * nor a {@code message_stop} fabricated), otherwise one {@code error} event frame goes out in this protocol's shape
     * before the failure signal disconnects. The failure itself is always replaced by the four safe fields of
     * {@link LlmInvocationException}, so upstream text, a signature or a key can never leave this process with it.
     *
     * 用法 / Usage: 仅由 {@link #encodeStream} 的错误分支与 EOF 分支调用。
     * @param failure 参数 触发收束的失败；parameter the failure that asks for this closure.
     * @param terminated 参数 是否已见终态事件；parameter whether the terminal event was already seen.
     * @param alias 参数 客户端 alias，仅用于日志；parameter the client alias, used only for logging.
     * @return 返回 至多一帧后终止的字节流；returns a byte flux of at most one frame followed by termination.
     */
    private Flux<DataBuffer> closeWith(Throwable failure, AtomicBoolean terminated, String alias) {
        if (terminated.get()) {
            return Flux.empty();
        }
        LlmInvocationException carried = failure instanceof LlmInvocationException safe
                ? safe : protocolError("the upstream Messages stream failed", "stream");
        terminated.set(true);
        log.warn("llm messages stream closed by a native error alias={} protocol={} status={} code={}",
                alias, protocol(), carried.getStatus(), carried.getCode());
        return Flux.just(buffer(sseFrame("error", encodeError(carried))))
                .concatWith(Flux.error(carried));
    }

    /**
     * 中文说明：职责④原生错误编码，产出 Messages 自己的形状：顶层 {@code type=error} 加只带 {@code type} 与
     * {@code message} 的 {@code error} 子对象；{@code error.type} 在异常机器码本身就是原生类别时按原值出网，否则由
     * 状态映射（400/409/422→invalid_request_error、401→authentication_error、403→permission_error、404→
     * not_found_error、413→request_too_large、429→rate_limit_error、529→overloaded_error、其余→api_error）。
     * 永不套 OpenAI 的 error/choices 信封、永不出 admin 的 {@code code}/{@code data} 信封；Messages 原生错误对象没有
     * {@code code}/{@code param} 槽位，因此这两个安全字段只进低基数日志，绝不进任何出网字段，也不回显 token、密钥、
     * baseUrl、upstreamModel 或上游正文。
     * English summary: Duty (4) native error encoding in Messages' own shape: a top-level {@code type=error} plus an
     * {@code error} object carrying only {@code type} and {@code message}; {@code error.type} travels verbatim when the
     * exception's machine code already is a native category and is otherwise mapped from the status
     * (400/409/422→invalid_request_error, 401→authentication_error, 403→permission_error, 404→not_found_error,
     * 413→request_too_large, 429→rate_limit_error, 529→overloaded_error, everything else→api_error). The OpenAI
     * error/choices envelope is never borrowed and the admin {@code code}/{@code data} wrapper never appears; because the
     * native Messages error object has no {@code code}/{@code param} slots those two safe fields only reach a
     * low-cardinality log, never a wire field, and no token, secret, base URL, upstream model or upstream payload is echoed.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code encodeError(error)}，状态取 {@code error.getStatus()}；未认证与越权的
     * 原生错误同样由本方法产出。
     * @param error 参数 已脱敏的原生错误载体；parameter the masked native error carrier.
     * @return 返回 Messages 原生错误文档；returns the Messages-native error document.
     */
    @Override
    public ObjectNode encodeError(LlmInvocationException error) {
        log.warn("llm messages native error status={} code={} param={} retryable={}",
                error.getStatus(), error.getCode(), error.getParam(), error.isRetryable());
        ObjectNode document = objectMapper.createObjectNode();
        document.put("type", "error");
        ObjectNode detail = document.putObject("error");
        detail.put("type", errorType(error));
        detail.put("message", error.getMessage());
        return document;
    }

    /**
     * 中文说明：唯一的编排入口。先用 {@code route.channel} 的事实把不成立的尝试挡在出网之前（渠道悬空/停用、
     * 协议不同、地址或超时预算缺失都按 503 {@code model_unavailable} 关闭，绝不猜渠道），凭据只在
     * {@code yuheng.llm.secrets-root} 之下解析 {@code secretRef}（越界、非普通文件、空值都是 503，值永不入日志与错误）；
     * 再用就地按渠道连接/头部预算构建的 JDK {@link HttpClient} 发一次同协议 POST：单播出 {@code decodeUnaryResponse}
     * 后一次性写回有界 body，流式则先阻塞读完并校验<b>一个完整合法帧</b>（{@code message_start}，允许其前有
     * {@code ping}）才把 publisher 写进 {@code routed} 进入 COMMITTED。首帧之前的任何失败都按原生状态抛出
     * （{@link HttpTimeoutException}→504 {@code upstream_timeout}、{@link RejectedExecutionException} 饱和→429
     * {@code rate_limit_exceeded}、连接失败→503、结构不合→502），提交后既不换模型也不重试也不改 HTTP 状态，只能以本
     * 协议 {@code error} 事件或断开收束；非 2xx 上游按其原生状态交 {@link #encodeError}（429 才透传数字
     * {@code Retry-After}），绝不伪装成空成功。整个过程在只读快照事务之外，本方法不声明事务、不加锁、不持有连接跨等待。
     * English summary: The single orchestration entry. Channel facts first close an unsound attempt before any egress (a
     * dangling or disabled channel, a foreign protocol, a missing address or timeout budget are all 503
     * {@code model_unavailable}, never a guess), and the credential is resolved only under
     * {@code yuheng.llm.secrets-root} from {@code secretRef}; no-auth LOCAL may omit the reference, while CLOUD requires a
     * valid file (traversal, a non-regular file or a blank value are 503 and the value never reaches a log or an error).
     * A single same-protocol POST then leaves through a JDK {@link HttpClient} built
     * on the spot from the channel's connect and header budgets: the unary face answers through {@link #decodeUnaryResponse}
     * as one bounded body, while the streaming face blocks until <b>one complete valid frame</b> (a
     * {@code message_start}, {@code ping} allowed ahead of it) has been validated before the publisher is attached to
     * {@code routed} and COMMITTED is entered. Every failure before that first frame raises natively
     * ({@link HttpTimeoutException}→504 {@code upstream_timeout}, {@link RejectedExecutionException} saturation→429
     * {@code rate_limit_exceeded}, a connect failure→503, malformed structure→502); after commit there is no model switch,
     * no retry and no HTTP-status rewrite, only this protocol's {@code error} event or a disconnect. A non-2xx upstream
     * answers with its own native status through {@link #encodeError} (a numeric {@code Retry-After} travels only on 429)
     * and is never turned into an empty success. None of this happens inside the read-only snapshot transaction: this method
     * declares no transaction, takes no lock and holds no connection across a wait.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code exchange(command, route, routed)}，由 {@code LlmApiController} 在
     * {@code LlmInvocationService.invoke(command)} 之后调用。
     * @param command 参数 已校验的调用命令；parameter the validated invocation command.
     * @param route 参数 已被路由策略放行的首条候选 route；parameter the head candidate route the routing strategy already admitted.
     * @param routed 参数 路由阶段产出的原生结果，携带安全响应头，本方法就地补全 body；parameter the routed native result carrying the safe headers, whose body this method completes in place.
     * @return 返回 已带状态、安全头与有界 body 的原生结果；returns the native result carrying status, safe headers and a bounded body.
     * @throws LlmInvocationException 首帧验证之前的任何上游失败（429/502/503/504 等），状态即协议原生状态；any upstream failure before the first frame validates, whose status is the protocol-native one.
     */
    @Override
    public LlmInvocationResultVO exchange(LlmInvocationCommandDTO command,
                                          LlmModelSnapshotBO.RouteBO route,
                                          LlmInvocationResultVO routed) {
        LlmModelSnapshotBO.ChannelBO channel = requireUsableChannel(route, command);
        String secret = credential(channel, command);
        ObjectNode request = encodeRequest(command, route);
        byte[] body = serialize(request);
        boolean streaming = Boolean.TRUE.equals(command.getStream());
        Duration budget = Duration.ofMillis(streaming
                ? channel.getHeaderTimeoutMs() : channel.getTotalTimeoutMs());
        Instant deadline = Instant.now().plus(Duration.ofMillis(channel.getTotalTimeoutMs()));
        HttpRequest.Builder egressBuilder = HttpRequest.newBuilder(endpoint(channel, command))
                .timeout(budget)
                .header(CONTENT_TYPE_HEADER, JSON_MEDIA_TYPE)
                .header(ACCEPT_HEADER, streaming ? "text/event-stream" : JSON_MEDIA_TYPE)
                .header(VERSION_HEADER, ANTHROPIC_VERSION);
        if (secret != null) {
            egressBuilder.header(API_KEY_HEADER, secret)
                    .header(AUTHORIZATION_HEADER, "Bearer " + secret);
        }
        HttpRequest egress = egressBuilder
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        HttpResponse<InputStream> response = send(egress, channel, command);
        int status = response.statusCode();
        if (status < 200 || status > 299) {
            return nativeFailure(routed, response, upstreamFailure(status), channel, command);
        }
        return streaming
                ? commitStream(command, routed, response, channel, deadline)
                : completeUnary(command, routed, response, channel, deadline);
    }

    /**
     * 中文说明：把上游<b>原始字节</b>并入拼装缓冲，并抽出所有已闭合帧（帧边界是 SSE 的空行，行尾 {@code \n} 与
     * {@code \r\n} 皆认），逐帧交给 {@link #renderFrame}，产出的出网帧写入 {@code emitted}：一个缓冲区里挤着好几帧、
     * 一帧被拆到好几个缓冲区，都在此处按字节顺序成立，绝不要求调用方「一个缓冲区一个裸 JSON 对象」；拼装后的未闭合
     * 字节恒受 {@code yuheng.llm.max-frame-bytes} 约束（超限即 502，且不回显内容）。
     * 预提交阶段与流式阶段共用本方法，因此「一个完整合法帧」的判定与后续出帧文法完全一致。
     * English summary: Merges the upstream's <b>raw bytes</b> into the assembly buffer and extracts every closed frame —
     * the frame boundary is the SSE blank line, with both {@code \n} and {@code \r\n} ending a line — handing each to
     * {@link #renderFrame} and appending the outbound frames to {@code emitted}: several frames packed into one buffer and
     * one frame torn across several buffers both work here in byte order, so a caller is never asked for one bare JSON
     * object per buffer; the unclosed remainder is always capped by {@code yuheng.llm.max-frame-bytes} (oversize is a 502
     * that echoes no content). Both the pre-commit phase and the streaming phase share this method, which is why "one
     * complete valid frame" and the later frame grammar agree exactly.
     *
     * 用法 / Usage: 仅由 {@link #encodeStream} 与 {@link #commitStream} 调用。
     * @param chunk 参数 本次读到的原始字节；parameter the raw bytes read in this step.
     * @param holder 参数 单元素数组承载可增长的拼装缓冲；parameter a one-element array carrying the growable assembly buffer.
     * @param accumulated 参数 当前未闭合字节数，就地更新；parameter the unclosed byte count, updated in place.
     * @param alias 参数 逐帧回写的客户端 alias；parameter the client alias written back frame by frame.
     * @param started 参数 是否已见 message_start；parameter whether message_start was already seen.
     * @param terminated 参数 是否已见终态事件；parameter whether the terminal event was already seen.
     * @param openBlocks 参数 已开启块的 index→类型表；parameter the index-to-type table of opened blocks.
     * @param partialJson 参数 块 index→已累积 partial_json；parameter the per-index accumulation of partial_json.
     * @param blockCount 参数 已开启块计数，仅用于日志；parameter the opened-block counter, used only for logging.
     * @param emitted 参数 出网帧的收集列表，失败时由调用方释放；parameter the collector of outbound frames, released by the caller on failure.
     */
    private void drainFrames(byte[] chunk, byte[][] holder, int[] accumulated, String alias,
                             AtomicBoolean started, AtomicBoolean terminated,
                             Map<Integer, String> openBlocks, Map<Integer, StringBuilder> partialJson,
                             AtomicInteger blockCount, List<DataBuffer> emitted) {
        accumulated[0] = append(holder, accumulated[0], chunk);
        for (;;) {
            int end = frameEnd(holder[0], accumulated[0]);
            if (end < 0) {
                requireFrameBudget(accumulated[0], alias);
                return;
            }
            requireFrameBudget(end, alias);
            int consumed = end + delimiterLength(holder[0], accumulated[0], end);
            String frame = new String(holder[0], 0, end, StandardCharsets.UTF_8);
            accumulated[0] = consume(holder[0], consumed, accumulated[0]);
            if (terminated.get()) {
                continue;
            }
            String rendered = renderFrame(frame, alias, started, terminated, openBlocks, partialJson, blockCount);
            if (rendered != null) {
                emitted.add(buffer(rendered));
            }
        }
    }

    /**
     * 中文说明：单帧字节上限：无论已闭合帧还是尚未闭合的残留，一旦超过 {@code yuheng.llm.max-frame-bytes} 就以 502
     * {@code upstream_protocol_error} 收束，且原因短语是实现内置常量，绝不回显任何字节。
     * English summary: The per-frame ceiling: whether a closed frame or an unclosed remainder, crossing
     * {@code yuheng.llm.max-frame-bytes} collapses into a 502 {@code upstream_protocol_error} whose reason is an
     * implementer-authored constant echoing no byte.
     *
     * 用法 / Usage: 仅由 {@link #drainFrames} 调用。
     * @param bytes 参数 待判定的字节数；parameter the byte count under judgement.
     * @param alias 参数 客户端 alias，仅用于日志；parameter the client alias, used only for logging.
     */
    private void requireFrameBudget(int bytes, String alias) {
        if (bytes <= gatewayProperties.getMaxFrameBytes()) {
            return;
        }
        log.warn("llm messages frame exceeds the ceiling alias={} protocol={} bytes={} ceiling={}",
                alias, protocol(), bytes, gatewayProperties.getMaxFrameBytes());
        throw protocolError("a Messages stream frame exceeded yuheng.llm.max-frame-bytes", "stream");
    }

    /**
     * 中文说明：解析一帧 Messages SSE 文本、验证其结构与顺序、仅对 {@code message_start} 回写 alias，并产出可直接
     * 出网的帧文本；无 data 字段的帧（注释、心跳）返回 {@code null} 表示本帧不出。上游自带的 {@code error} 事件按事实
     * 收束本流：类别取自八个原生类别白名单、说明换成实现内置文本，原文不外传。所有结构违例都收敛成 502
     * {@code upstream_protocol_error}，原因只是实现内置常量短语，绝不含上游正文、签名或密钥。
     * English summary: Parses one Messages SSE frame, validates its structure and position in the grammar, writes the alias
     * back on {@code message_start} only, and returns the frame text ready to leave; a frame without a data field (a comment
     * or keep-alive) yields {@code null} meaning nothing leaves for it. An upstream-authored {@code error} event closes the
     * stream as fact: the category comes from the eight-member native whitelist and the description is implementer-authored,
     * so upstream text never travels. Every structural violation collapses into a 502
     * {@code upstream_protocol_error} whose reason is an implementer-authored constant, never upstream content, a signature
     * or a key.
     *
     * 用法 / Usage: 仅由 {@link #drainFrames} 调用。
     * @param frameText 参数 一帧的文本（不含终止空行）；parameter the text of one frame without its terminating blank line.
     * @param alias 参数 客户端 alias；parameter the client alias.
     * @param started 参数 是否已见 message_start；parameter whether message_start was already seen.
     * @param terminated 参数 是否已见终态事件，本方法可能就地置位；parameter whether the terminal event was seen, set here.
     * @param openBlocks 参数 已开启块的 index→类型表，本方法就地维护；parameter the opened-block table, maintained here.
     * @param partialJson 参数 块 index→已累积 partial_json，本方法就地维护；parameter the partial_json accumulation, maintained here.
     * @param blockCount 参数 已开启块计数；parameter the opened-block counter.
     * @return 返回 出网帧文本，或 null 表示本帧不出；returns the outbound frame text, or null when nothing leaves for this frame.
     * @throws LlmInvocationException 502 {@code upstream_protocol_error}；raised on any structural or ordering violation.
     */
    private String renderFrame(String frameText, String alias, AtomicBoolean started, AtomicBoolean terminated,
                              Map<Integer, String> openBlocks, Map<Integer, StringBuilder> partialJson,
                              AtomicInteger blockCount) {
        ObjectNode data = parseFrameData(frameText);
        if (data == null) {
            return null;
        }
        String type = data.path("type").textValue();
        switch (type) {
            case "ping" -> {
                return sseFrame(type, data);
            }
            case "error" -> {
                JsonNode detail = data.path("error");
                String category = textField(detail, "type");
                if (!detail.isObject() || category == null || !NATIVE_ERROR_TYPES.contains(category)
                        || StringUtils.isBlank(textField(detail, "message"))) {
                    throw protocolError("the in-stream error event carried no native error category", "error");
                }
                terminated.set(true);
                log.warn("llm messages stream ended with an upstream error alias={} category={}", alias, category);
                return sseFrame("error", closedErrorEvent(category));
            }
            case "message_start" -> {
                if (!started.compareAndSet(false, true)) {
                    throw protocolError("message_start appeared twice", "message_start");
                }
                acceptMessageStart(data, alias);
                return sseFrame(type, data);
            }
            case "content_block_start" -> {
                requireStarted(started);
                openBlock(data, openBlocks, blockCount);
                return sseFrame(type, data);
            }
            case "content_block_delta" -> {
                requireStarted(started);
                applyDelta(data, openBlocks, partialJson);
                return sseFrame(type, data);
            }
            case "content_block_stop" -> {
                requireStarted(started);
                closeBlock(data, openBlocks, partialJson);
                return sseFrame(type, data);
            }
            case "message_delta" -> {
                requireStarted(started);
                requireOptionalObject(data, "delta", "message_delta");
                requireOptionalObject(data, "usage", "message_delta");
                return sseFrame(type, data);
            }
            case "message_stop" -> {
                requireStarted(started);
                terminated.set(true);
                return sseFrame(type, data);
            }
            default -> {
                requireStarted(started);
                log.debug("llm messages unknown stream event forwarded alias={} type={}", alias, type);
                return sseFrame(type, data);
            }
        }
    }

    /**
     * 中文说明：按 SSE 字段文法解析一帧文本，取出该帧唯一的 data 载荷并把它读成一个 JSON 对象。一帧是「若干行 + 一个
     * 空行」，因此这里必须走真正的行文法：行终止符 {@code \r\n}、{@code \n}、{@code \r} 三种都认；{@code :} 开头的行是
     * 注释/心跳，直接丢掉；{@code event:} 给出事件名（存在即以它为准，缺失时由载荷的 {@code type} 承担）；
     * {@code id:}/{@code retry:} 是流控字段，参与文法但不进载荷；{@code data:} 去掉前缀与其后<b>最多一个</b>空格后计入
     * 载荷，同帧多条 {@code data:} 行按 SSE 以 {@code \n} 拼接；一条既非已知字段也非注释的行是载荷自己的折行——
     * 上游或中转层常把一个 JSON 文档折成多行而不给后续行加前缀，所以只要载荷已经开始就必须原样续接，
     * <b>绝不做「每个缓冲区一个裸 JSON 对象」这种要求，也绝不把半段文档丢给解析器再判错</b>。没有 data 行返回
     * {@code null}（注释/心跳不出帧），载荷解析不出唯一对象、缺 {@code type} 或 {@code event:} 与 {@code type} 不符
     * （Spec §9.0.1/§9.2.30「SSE event/type一致」）都是 502，且日志只记异常类型名。
     * English summary: Parses one frame's text through the SSE field grammar and reads its single data payload as one JSON
     * object. A frame is "some lines plus a blank line", so the real line grammar has to be walked here: all three line
     * terminators {@code \r\n}, {@code \n} and {@code \r} count; a line starting with {@code :} is a comment or heartbeat
     * and is dropped; {@code event:} carries the event name, which rules when present and otherwise falls back to the
     * payload's own {@code type}; {@code id:} and {@code retry:} are flow-control fields that take part in the grammar but
     * never in the payload; a {@code data:} line contributes its value with the prefix and <b>at most one</b> following
     * space removed, and several {@code data:} lines of one frame join with {@code \n} as SSE prescribes; a line that is
     * neither a known field nor a comment is a wrapped continuation of the payload — an upstream or an intermediary commonly
     * folds one JSON document over several lines without re-prefixing them — so as soon as the payload has opened such a
     * line is appended verbatim, because this face must <b>never demand one bare JSON object per buffer and never hand half
     * a document to the parser and then call it a violation</b>. No data line yields {@code null} (a comment or keep-alive
     * emits nothing), while a payload that is not exactly one JSON object, one that declares no {@code type} and one whose
     * {@code event:} disagrees with its {@code type} (Spec §9.0.1/§9.2.30 "SSE event/type一致") are a 502 whose log names
     * only the exception type.
     *
     * 用法 / Usage: 仅由 {@link #renderFrame} 调用。
     * @param frameText 参数 帧文本（不含终止空行）；parameter the frame text without its terminating blank line.
     * @return 返回 帧内 data 对象，或 null；returns the frame's data object, or null.
     */
    private ObjectNode parseFrameData(String frameText) {
        StringBuilder data = new StringBuilder();
        String eventName = null;
        boolean hasData = false;
        for (String line : sseLines(frameText)) {
            if (line.isEmpty() || line.startsWith(":")) {
                continue;
            }
            if (line.startsWith(EVENT_FIELD)) {
                eventName = StringUtils.trimToNull(sseFieldValue(line, EVENT_FIELD.length()));
            } else if (line.startsWith(ID_FIELD) || line.startsWith(RETRY_FIELD)) {
                continue;
            } else if (line.startsWith(DATA_FIELD)) {
                if (hasData) {
                    data.append('\n');
                }
                data.append(sseFieldValue(line, DATA_FIELD.length()));
                hasData = true;
            } else if (hasData) {
                data.append('\n').append(line);
            }
        }
        if (!hasData) {
            return null;
        }
        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(data.toString());
        } catch (JsonProcessingException | IllegalArgumentException failure) {
            log.warn("llm messages stream frame is unparsable, exception={}", failure.getClass().getSimpleName());
            throw protocolError("a Messages frame did not carry one JSON object", "stream");
        }
        if (!(parsed instanceof ObjectNode object)) {
            throw protocolError("a Messages frame did not carry one JSON object", "stream");
        }
        String type = object.path("type").textValue();
        if (type == null) {
            throw protocolError("a Messages frame declared no event type", "stream");
        }
        if (eventName != null && !eventName.equals(type)) {
            throw protocolError("a Messages frame's event name disagreed with its type", "stream");
        }
        return object;
    }

    /**
     * 中文说明：把一帧文本按 SSE 行文法切成行（{@code \r\n}、{@code \n}、{@code \r} 三种终止符都被识别且都不留在行里），
     * 帧尾未闭合的半行按已给出的内容返回；本方法只做切分，不判字段。
     * English summary: Cuts one frame's text into SSE lines, recognising all three terminators {@code \r\n}, {@code \n} and
     * {@code \r} and keeping none of them inside a line, where an unterminated tail line is returned as the bytes it holds;
     * this method only splits and never interprets a field.
     *
     * 用法 / Usage: 仅由 {@link #parseFrameData} 调用。/ called only by {@link #parseFrameData}.
     * @param frameText 参数 帧文本；parameter the frame text.
     * @return 返回 自成一行的文本序列，可能为空；returns the sequence of line texts, possibly empty.
     */
    private static List<String> sseLines(String frameText) {
        List<String> lines = new ArrayList<>();
        int length = frameText.length();
        int start = 0;
        int cursor = 0;
        while (cursor < length) {
            char current = frameText.charAt(cursor);
            if (current != '\n' && current != '\r') {
                cursor++;
                continue;
            }
            lines.add(frameText.substring(start, cursor));
            int next = cursor + 1;
            if (current == '\r' && next < length && frameText.charAt(next) == '\n') {
                next++;
            }
            cursor = next;
            start = cursor;
        }
        if (start < length) {
            lines.add(frameText.substring(start));
        }
        return lines;
    }

    /**
     * 中文说明：取一行的字段值：只按 SSE 去掉前缀后紧跟的<b>一个</b>空格，其余字符（含载荷自己的缩进）原样保留，
     * 因为 {@code input_json_delta.partial_json} 这类片段的内容必须逐字出网。
     * English summary: Reads a line's field value by dropping exactly <b>one</b> space after the prefix as SSE says and
     * keeping every other character, the payload's own indentation included, because the content of a fragment such as
     * {@code input_json_delta.partial_json} has to leave byte for byte.
     *
     * 用法 / Usage: 仅由 {@link #parseFrameData} 调用。/ called only by {@link #parseFrameData}.
     * @param line 参数 不含终止符的一行；parameter one terminator-free line.
     * @param prefixLength 参数 字段前缀长度；parameter the length of the field prefix.
     * @return 返回 字段值，可为空串；returns the field value, possibly empty.
     */
    private static String sseFieldValue(String line, int prefixLength) {
        String value = line.substring(prefixLength);
        return value.startsWith(" ") ? value.substring(1) : value;
    }

    /**
     * 中文说明：校验 {@code message_start} 的必需结构（id/type/role/content/usage）并仅把 {@code message.model} 回写为
     * 客户端 alias；其它字段（含未知安全字段）保持原样。
     * English summary: Validates the required structure of {@code message_start} (id/type/role/content/usage) and writes back
     * only {@code message.model} as the client alias, leaving every other field, safe unknowns included, untouched.
     *
     * 用法 / Usage: 仅由 {@link #renderFrame} 调用。
     * @param data 参数 message_start 的 data 对象；parameter the message_start data object.
     * @param alias 参数 回写目标 alias；parameter the alias to write back.
     */
    private static void acceptMessageStart(ObjectNode data, String alias) {
        JsonNode message = data.get("message");
        if (!(message instanceof ObjectNode opened)) {
            throw protocolError("message_start carried no message object", "message_start");
        }
        if (StringUtils.isBlank(textField(opened, "id")) || !"message".equals(textField(opened, "type"))
                || !"assistant".equals(textField(opened, "role"))) {
            throw protocolError("message_start carried an incomplete message identity", "message_start");
        }
        if (!opened.path("content").isArray() || !opened.path("usage").isObject()) {
            throw protocolError("message_start carried no content array or usage object", "message_start");
        }
        opened.put("model", alias);
    }

    /**
     * 中文说明：开启一个 content block：index 必须唯一且不越界，块必须声明类型，{@code tool_use} 必须带 1–128 的
     * {@code id}、name 与（可选的）对象 {@code input}；未知块类型按原生文法保留。
     * English summary: Opens one content block: the index must be unique and in range, the block must declare its type, and a
     * {@code tool_use} must carry a 1–128 {@code id}, a name and an optional object {@code input}; unknown block types stay
     * as the native grammar allows.
     *
     * 用法 / Usage: 仅由 {@link #renderFrame} 调用。
     * @param data 参数 content_block_start 的 data 对象；parameter the content_block_start data object.
     * @param openBlocks 参数 已开启块表，就地写入；parameter the opened-block table, written in place.
     * @param blockCount 参数 已开启块计数；parameter the opened-block counter.
     */
    private static void openBlock(JsonNode data, Map<Integer, String> openBlocks, AtomicInteger blockCount) {
        int index = blockIndex(data, "content_block_start");
        if (openBlocks.containsKey(index)) {
            throw protocolError("content_block_start repeated one index", "content_block_start");
        }
        if (blockCount.incrementAndGet() > MAX_CONTENT_BLOCKS) {
            throw protocolError("a Messages stream opened too many content blocks", "content_block_start");
        }
        JsonNode block = data.get("content_block");
        String type = block == null ? null : textField(block, "type");
        if (type == null) {
            throw protocolError("content_block_start carried no typed content_block", "content_block_start");
        }
        if ("tool_use".equals(type)
                && (optionalText(block, "id", MAX_TOOL_ID_CHARS) == null
                || StringUtils.isBlank(textField(block, "name"))
                || (block.has("input") && !block.path("input").isObject()))) {
            throw protocolError("a streamed tool_use block carried no id, no name or a non-object input", "tool_use");
        }
        openBlocks.put(index, type);
    }

    /**
     * 中文说明：校验并累积一个 delta：delta 必须落在已开启块上并声明类型；{@code text_delta} 带文本、
     * {@code thinking_delta} 带 thinking 文本、{@code signature_delta} 必须落在 thinking 块上并带签名（原样保留、不改写）、
     * {@code input_json_delta} 只把 {@code partial_json} 追加到该块的累加器——<b>本方法绝不解析半段 JSON</b>；未知
     * delta 类型原样转发。
     * English summary: Validates and accumulates one delta: it must land on an opened block and declare its type;
     * {@code text_delta} carries text, {@code thinking_delta} carries thinking text, {@code signature_delta} must belong to
     * a thinking block and carries its signature (kept verbatim, never rewritten), and {@code input_json_delta} only appends
     * its {@code partial_json} to that block's accumulator — <b>this method never parses half a JSON document</b>; an unknown
     * delta kind is forwarded unchanged.
     *
     * 用法 / Usage: 仅由 {@link #renderFrame} 调用。
     * @param data 参数 content_block_delta 的 data 对象；parameter the content_block_delta data object.
     * @param openBlocks 参数 已开启块表；parameter the opened-block table.
     * @param partialJson 参数 块 index→partial_json 累加器，就地追加；parameter the per-index partial_json accumulation, appended in place.
     */
    private static void applyDelta(ObjectNode data, Map<Integer, String> openBlocks,
                                   Map<Integer, StringBuilder> partialJson) {
        int index = blockIndex(data, "content_block_delta");
        String blockType = openBlocks.get(index);
        if (blockType == null) {
            throw protocolError("content_block_delta targeted an unopened block", "content_block_delta");
        }
        JsonNode delta = data.get("delta");
        String kind = delta == null ? null : textField(delta, "type");
        if (kind == null) {
            throw protocolError("content_block_delta carried no typed delta", "content_block_delta");
        }
        switch (kind) {
            case "text_delta" -> {
                if (textField(delta, "text") == null) {
                    throw protocolError("a text_delta carried no text", "text_delta");
                }
            }
            case "thinking_delta" -> {
                if (textField(delta, "thinking") == null && textField(delta, "text") == null) {
                    throw protocolError("a thinking_delta carried no thinking text", "thinking_delta");
                }
            }
            case "signature_delta" -> {
                if (!"thinking".equals(blockType) || textField(delta, "signature") == null) {
                    throw protocolError("a signature_delta did not belong to a signed thinking block", "signature_delta");
                }
            }
            case "input_json_delta" -> {
                String fragment = textField(delta, "partial_json");
                if (fragment == null) {
                    throw protocolError("an input_json_delta carried no partial_json", "input_json_delta");
                }
                StringBuilder accumulated = partialJson.computeIfAbsent(index, key -> new StringBuilder());
                if (accumulated.length() + fragment.length() > MAX_ASSEMBLED_INPUT_CHARS) {
                    throw protocolError("an assembled tool input exceeded the local ceiling", "input_json_delta");
                }
                accumulated.append(fragment);
            }
            default -> log.debug("llm messages unknown content block delta forwarded kind={}", kind);
        }
    }

    /**
     * 中文说明：关闭一个 content block，并在此——也只有在此——把该块累积的 {@code partial_json} 当作完整文档解析一次：
     * 必须可解析且是 JSON 对象，否则判 502 {@code upstream_protocol_error}（宁可断开也绝不把半段 JSON 当成完整入参发出）。
     * 出网帧仍是原片段序列，客户端自行重组，因此既没有半解析也没有丢帧。
     * English summary: Closes one content block and parses that block's accumulated {@code partial_json} exactly once here,
     * and only here, as a whole document: it must parse and must be a JSON object, otherwise the failure is a 502
     * {@code upstream_protocol_error}, since half a JSON document is never emitted as a complete tool input. The frames that
     * leave stay the original sequence, which the client reassembles, so nothing is half-parsed and nothing is dropped.
     *
     * 用法 / Usage: 仅由 {@link #renderFrame} 调用。
     * @param data 参数 content_block_stop 的 data 对象；parameter the content_block_stop data object.
     * @param openBlocks 参数 已开启块表，就地移除；parameter the opened-block table, entry removed in place.
     * @param partialJson 参数 累加器表，就地取走；parameter the accumulation table, drained in place.
     */
    private void closeBlock(ObjectNode data, Map<Integer, String> openBlocks,
                            Map<Integer, StringBuilder> partialJson) {
        int index = blockIndex(data, "content_block_stop");
        if (openBlocks.remove(index) == null) {
            throw protocolError("content_block_stop targeted an unopened block", "content_block_stop");
        }
        StringBuilder fragments = partialJson.remove(index);
        if (fragments == null || fragments.isEmpty()) {
            return;
        }
        JsonNode assembled;
        try {
            assembled = objectMapper.readTree(fragments.toString());
        } catch (JsonProcessingException | IllegalArgumentException failure) {
            log.warn("llm messages assembled tool input is unparsable, exception={}",
                    failure.getClass().getSimpleName());
            throw protocolError("the assembled tool input of one block was not valid JSON", "input_json_delta");
        }
        if (assembled == null || !assembled.isObject()) {
            throw protocolError("the assembled tool input of one block was not a JSON object", "input_json_delta");
        }
    }

    /**
     * 中文说明：单播成功路径的就地实现：在 {@code yuheng.llm.max-request-bytes} 之内读完 body（超限即 502，绝不截断成
     * 「看起来完整」），解析为对象后交 {@link #decodeUnaryResponse} 保真回写 alias，再以单一有界 DataBuffer 写回
     * {@code routed}；解析不出来就是 502，绝不回一份空成功。
     * English summary: The unary success path read in place: the body is drained inside {@code yuheng.llm.max-request-bytes}
     * (oversize is a 502 and never truncated into something that looks complete), parsed into an object, handed to
     * {@link #decodeUnaryResponse} for alias write-back and fidelity, and answered as one bounded DataBuffer; an
     * unparsable body is a 502 rather than an empty success.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 在 stream 位为假时调用。
     * @param command 参数 本次调用命令；parameter this invocation's command.
     * @param routed 参数 就地补全的原生结果；parameter the native result completed in place.
     * @param response 参数 已收到 2xx 头的上游响应；parameter the upstream response whose headers were 2xx.
     * @param channel 参数 本次尝试的渠道事实；parameter the channel facts of this attempt.
     * @param deadline 参数 整次请求的截止时刻；parameter the request-wide deadline.
     * @return 返回 已带 body 的原生结果；returns the native result carrying its body.
     */
    private LlmInvocationResultVO completeUnary(LlmInvocationCommandDTO command, LlmInvocationResultVO routed,
                                                HttpResponse<InputStream> response,
                                                LlmModelSnapshotBO.ChannelBO channel, Instant deadline) {
        byte[] payload;
        try (InputStream upstream = response.body()) {
            payload = readAll(upstream, deadline);
        } catch (IOException failure) {
            log.error("llm messages upstream body unreadable alias={} protocol={} channel={} exception={}",
                    command.getModel(), protocol(), channel.getChannelKey(), failure.getClass().getSimpleName());
            throw protocolError("the upstream Messages body could not be read", "usage");
        }
        JsonNode document;
        try {
            document = objectMapper.readTree(new String(payload, StandardCharsets.UTF_8));
        } catch (JsonProcessingException | RuntimeException failure) {
            log.warn("llm messages upstream body unparsable alias={} protocol={} channel={} exception={}",
                    command.getModel(), protocol(), channel.getChannelKey(), failure.getClass().getSimpleName());
            throw protocolError("the upstream Messages body was not one JSON object", "usage");
        }
        if (!(document instanceof ObjectNode success)) {
            throw protocolError("the upstream Messages body was not one JSON object", "usage");
        }
        byte[] encoded = serialize(decodeUnaryResponse(command, success));
        routed.setStatus(response.statusCode());
        routed.setBody(Flux.just(buffer(encoded)));
        log.info("llm messages unary committed alias={} protocol={} channel={} status={}",
                command.getModel(), protocol(), channel.getChannelKey(), response.statusCode());
        return routed;
    }

    /**
     * 中文说明：流式提交屏障：在整次请求预算内阻塞读取并按帧边界抽出完整帧，逐帧走 {@link #renderFrame} 文法，直到
     * {@code message_start} 通过校验（其前的 {@code ping} 允许并一并保留顺序）才进入 COMMITTED；在此之前任何失败都可以
     * 按原生状态抛出，因此调用方还能安全地试下一个候选。已验证的帧文本与尚未闭合的尾字节被拼成第一个 DataBuffer
     * （不重复解析也不丢字节），其余字节由 {@link Flux#generate} 一帧一取地读出（预取恒为 1），并按渠道空闲/总预算加上
     * 超时；最后把 {@code routed.body} 交给 {@link #encodeStream}。任何失败路径都关闭上游流，即取消传播。
     * English summary: The streaming commit barrier: within the request-wide budget the body is read blocking-wise, complete
     * frames are cut on the frame boundary and each runs through the {@link #renderFrame} grammar until
     * {@code message_start} validates ({@code ping} ahead of it is allowed and keeps its place) and COMMITTED is entered;
     * any failure before that may still raise natively so the caller may safely try the next candidate. The validated frame
     * texts and the not-yet-closed tail bytes are joined into the first DataBuffer (nothing re-parsed, nothing dropped),
     * the remaining bytes come out one item at a time through {@link Flux#generate} (prefetch stays one) bounded by the
     * channel's idle and total budgets, and {@code routed.body} is then handed to {@link #encodeStream}. Every failure path
     * closes the upstream stream, which is cancellation propagating.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 在 stream 位为真时调用。
     * @param command 参数 本次调用命令；parameter this invocation's command.
     * @param routed 参数 就地补全的原生结果；parameter the native result completed in place.
     * @param response 参数 已收到 2xx 头的上游响应；parameter the upstream response whose headers were 2xx.
     * @param channel 参数 本次尝试的渠道事实；parameter the channel facts of this attempt.
     * @param deadline 参数 整次请求的截止时刻；parameter the request-wide deadline.
     * @return 返回 已带同协议字节流的原生结果；returns the native result carrying the same-protocol byte flux.
     */
    private LlmInvocationResultVO commitStream(LlmInvocationCommandDTO command, LlmInvocationResultVO routed,
                                               HttpResponse<InputStream> response,
                                               LlmModelSnapshotBO.ChannelBO channel, Instant deadline) {
        InputStream upstream = response.body();
        byte[][] holder = new byte[][] {new byte[STREAM_BUFFER_BYTES]};
        int[] accumulated = new int[1];
        ByteArrayOutputStream lead = new ByteArrayOutputStream();
        AtomicBoolean started = new AtomicBoolean();
        AtomicBoolean terminated = new AtomicBoolean();
        Map<Integer, String> openBlocks = new HashMap<>();
        Map<Integer, StringBuilder> partialJson = new HashMap<>();
        AtomicInteger blockCount = new AtomicInteger();
        boolean handedOver = false;
        try {
            while (!started.get()) {
                requireDeadline(deadline, channel, command);
                byte[] chunk = readChunk(upstream);
                if (chunk.length == 0) {
                    break;
                }
                List<DataBuffer> frames = new ArrayList<>(2);
                try {
                    drainFrames(chunk, holder, accumulated, command.getModel(), started, terminated,
                            openBlocks, partialJson, blockCount, frames);
                    frames.forEach(emitted -> {
                        byte[] payload = readableBytes(emitted);
                        lead.write(payload, 0, payload.length);
                    });
                } finally {
                    frames.forEach(DataBufferUtils::release);
                }
            }
            if (!started.get()) {
                throw protocolError("the upstream Messages stream opened without a valid message_start", "message_start");
            }
            lead.write(holder[0], 0, accumulated[0]);
            byte[] committed = lead.toByteArray();
            Flux<DataBuffer> raw = Flux
                    .using(() -> upstream,
                            stream -> Flux.concat(Flux.just(buffer(committed)), chunks(stream, deadline)),
                            AnthropicMessagesProtocolStrategy::closeQuietly)
                    .timeout(Duration.ofMillis(channel.getIdleTimeoutMs()))
                    .onErrorMap(TimeoutException.class, failure -> upstreamTimeout());
            handedOver = true;
            routed.setStatus(response.statusCode());
            routed.setBody(encodeStream(command, raw));
            log.info("llm messages stream committed alias={} protocol={} channel={} status={}",
                    command.getModel(), protocol(), channel.getChannelKey(), response.statusCode());
            return routed;
        } catch (IOException failure) {
            log.error("llm messages upstream stream failed before commit alias={} protocol={} channel={} exception={}",
                    command.getModel(), protocol(), channel.getChannelKey(), failure.getClass().getSimpleName());
            throw protocolError("the upstream Messages stream failed before its first frame", "stream");
        } finally {
            if (!handedOver) {
                closeQuietly(upstream);
            }
        }
    }

    /**
     * 中文说明：把已持有的上游字节流变成每次一取的 DataBuffer 流：每次取前先核对整次请求 deadline（超时即 504
     * {@code upstream_timeout}），读到一个块就产一个 DataBuffer，EOF 即完成；本方法既不缓存整条流也不越界读取。
     * English summary: Turns the held upstream byte stream into a one-item-at-a-time DataBuffer flux: the request-wide
     * deadline is re-checked before every pull (expired is a 504 {@code upstream_timeout}), one read yields one
     * DataBuffer and EOF completes — neither the whole stream is buffered nor is any bound crossed.
     *
     * 用法 / Usage: 仅由 {@link #commitStream} 的 {@code Flux.using} 源函数调用。
     * @param upstream 参数 上游响应字节流；parameter the upstream response stream.
     * @param deadline 参数 整次请求的截止时刻；parameter the request-wide deadline.
     * @return 返回 有界字节流；returns the bounded byte flux.
     */
    private Flux<DataBuffer> chunks(InputStream upstream, Instant deadline) {
        return Flux.generate(() -> new byte[UPSTREAM_CHUNK_BYTES], (chunk, sink) -> {
            if (Instant.now().isAfter(deadline)) {
                sink.error(upstreamTimeout());
                return chunk;
            }
            byte[] read;
            try {
                read = readChunk(upstream);
            } catch (IOException failure) {
                sink.error(protocolError("the upstream Messages stream failed", "stream"));
                return chunk;
            }
            if (read.length == 0) {
                sink.complete();
                return chunk;
            }
            sink.next(buffer(read));
            return chunk;
        });
    }

    /**
     * 中文说明：非 2xx 上游的原生失败收束：先排空并关闭上游 body（永不把上游正文带给客户端），把状态改为
     * {@link LlmInvocationException} 携带的原生状态，只按白名单透传 429 的数字 {@code Retry-After}，并用
     * {@link #encodeError} 写出 Messages 原生错误文档。
     * English summary: The native landing point for a non-2xx upstream: the body is drained and closed first (upstream text
     * never reaches the client), the status becomes the one {@link LlmInvocationException} carries, only a numeric
     * {@code Retry-After} on a 429 passes the whitelist, and the Messages-native error document written by
     * {@link #encodeError} answers.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 在上游非 2xx 时调用。
     * @param routed 参数 就地补全的原生结果；parameter the native result completed in place.
     * @param response 参数 上游响应；parameter the upstream response.
     * @param error 参数 按上游状态构造的安全原生错误；parameter the safe native error built from the upstream status.
     * @param channel 参数 本次尝试的渠道事实；parameter the channel facts of this attempt.
     * @param command 参数 本次调用命令；parameter this invocation's command.
     * @return 返回 已带原生错误 body 的结果；returns the result carrying the native error body.
     */
    private LlmInvocationResultVO nativeFailure(LlmInvocationResultVO routed, HttpResponse<InputStream> response,
                                                LlmInvocationException error,
                                                LlmModelSnapshotBO.ChannelBO channel,
                                                LlmInvocationCommandDTO command) {
        String retryAfter = error.getStatus() == 429
                ? numericHeader(response, RETRY_AFTER_HEADER) : null;
        closeQuietly(response.body());
        Map<String, String> headers = new LinkedHashMap<>(
                routed.getHeaders() == null ? Map.of() : routed.getHeaders());
        headers.put(CONTENT_TYPE_HEADER, JSON_MEDIA_TYPE);
        if (retryAfter != null) {
            headers.put(RETRY_AFTER_HEADER, retryAfter);
        }
        routed.setStatus(error.getStatus());
        routed.setHeaders(headers);
        routed.setBody(Flux.just(buffer(serialize(encodeError(error)))));
        log.warn("llm messages upstream answered non-2xx alias={} protocol={} channel={} status={} code={}",
                command.getModel(), protocol(), channel.getChannelKey(), error.getStatus(), error.getCode());
        return routed;
    }

    /**
     * 中文说明：按 Messages 文法把对象编码为一帧 SSE 文本：{@code event: <type>} 加 {@code data: <json>} 加空行。
     * English summary: Encodes an object as one SSE frame per the Messages grammar: {@code event: <type>} plus
     * {@code data: <json>} plus the blank line.
     *
     * 用法 / Usage: 仅由 {@link #renderFrame} 与 {@link #encodeStream} 的错误收束调用。
     * @param type 参数 事件类型；parameter the event type.
     * @param payload 参数 帧内 data 对象；parameter the frame's data object.
     * @return 返回 帧文本；returns the frame text.
     */
    private static String sseFrame(String type, JsonNode payload) {
        return EVENT_FIELD + " " + type + "\n" + DATA_FIELD + " " + payload + "\n\n";
    }

    /**
     * 中文说明：把上游已经给出的原生错误类别重新封装成本进程可安全出网的 {@code error} 事件对象：类别只有落在八个原生
     * 类别白名单内才会被采纳，消息一律换成实现内置的稳定说明，因此上游原文（可能含账号、配额或内部标识）永不转送给
     * 客户端；形状与 {@link #encodeError} 完全一致，不引入 {@code code}/{@code param} 等非原生槽位。
     * English summary: Re-wraps the native error category upstream already gave into an egress-safe {@code error} event
     * object: a category is adopted only from the eight-member native whitelist and the message is always replaced by a
     * stable implementer-authored description, so upstream text (which may hold an account, a quota or an internal
     * identifier) never reaches the client; the shape stays exactly that of {@link #encodeError}, adding no non-native
     * {@code code} or {@code param} slot.
     *
     * 用法 / Usage: 仅由 {@link #renderFrame} 的上游 {@code error} 事件分支调用。
     * @param category 参数 已核实过的原生错误类别；parameter the verified native error category.
     * @return 返回 原生错误事件对象；returns the native error event object.
     */
    private ObjectNode closedErrorEvent(String category) {
        ObjectNode document = objectMapper.createObjectNode();
        document.put("type", "error");
        ObjectNode detail = document.putObject("error");
        detail.put("type", category);
        detail.put("message", "The upstream Messages stream reported an error of category " + category + ".");
        return document;
    }

    /**
     * 中文说明：把已经完整的帧文本按 UTF-8 包成出网 DataBuffer；UTF-8 编码由 {@link String#getBytes(java.nio.charset.Charset)}
     * 一次完成，帧边界判定在字节层做，因此两者不会互相切开同一个字符。
     * English summary: Wraps complete frame text into an outbound DataBuffer as UTF-8; the encoding happens once through
     * {@link String#getBytes(java.nio.charset.Charset)} and frame boundaries are judged at the byte layer, so the two never
     * split the same character.
     *
     * 用法 / Usage: 仅由 {@link #drainFrames} 与 {@link #closeWith} 调用。
     * @param frame 参数 完整帧文本（含终止空行）；parameter the complete frame text including its blank line.
     * @return 返回 出网缓冲区；returns the outbound buffer.
     */
    private static DataBuffer buffer(String frame) {
        return buffer(frame.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 中文说明：取走 DataBuffer 的可读字节；只读不改引用，释放由调用方负责，因此每个 buffer 恰好释放一次。
     * English summary: Takes the readable bytes of a DataBuffer without touching its release ownership, which the caller
     * performs exactly once.
     *
     * 用法 / Usage: 仅由 {@link #encodeStream}、{@link #commitStream} 与 {@link #chunks} 调用。
     * @param source 参数 入站缓冲区；parameter the inbound buffer.
     * @return 返回 可读字节副本；returns a copy of the readable bytes.
     */
    private static byte[] readableBytes(DataBuffer source) {
        byte[] bytes = new byte[source.readableByteCount()];
        source.read(bytes);
        return bytes;
    }

    /** 中文说明：把字节包成出网 DataBuffer，使用 Spring 共享的非池化工厂，避免任何新协作者 bean。 English summary: Wraps bytes into an outbound DataBuffer through Spring's shared non-pooled factory, which avoids any new collaborator bean. */
    private static DataBuffer buffer(byte[] bytes) {
        return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
    }

    /**
     * 中文说明：把块追加进拼装缓冲，必要时按 2 的幂扩容（受帧上限约束在前面已拒）。
     * English summary: Appends a chunk to the assembly buffer, growing it by powers of two when needed, which the frame
     * ceiling has already bounded.
     *
     * 用法 / Usage: 仅由 {@link #drainFrames} 调用。
     * @param holder 参数 承载缓冲的单元素数组；parameter the one-element array carrying the buffer.
     * @param size 参数 当前已用字节数；parameter the bytes currently held.
     * @param chunk 参数 待追加字节；parameter the bytes to append.
     * @return 返回 追加后的字节数；returns the byte count after appending.
     */
    private static int append(byte[][] holder, int size, byte[] chunk) {
        byte[] buffer = holder[0];
        if (size + chunk.length > buffer.length) {
            int capacity = buffer.length;
            while (capacity < size + chunk.length) {
                capacity <<= 1;
            }
            buffer = Arrays.copyOf(buffer, capacity);
            holder[0] = buffer;
        }
        System.arraycopy(chunk, 0, buffer, size, chunk.length);
        return size + chunk.length;
    }

    /**
     * 中文说明：找出第一个 SSE 帧终止符（空行：LF LF、CRLF CRLF、CR CR 或 LF CRLF LF）的起点；找不到返回 -1。
     * 由于 UTF-8 续字节恒 ≥0x80，按字节找换行永不切开多字节字符。
     * English summary: Finds the start of the first SSE frame terminator (a blank line: LF LF, CRLF CRLF, CR CR or LF CRLF
     * LF) or -1 when none is closed yet. Because UTF-8 continuation bytes are always ≥ 0x80, scanning newlines by byte
     * never splits a multi-byte character.
     *
     * 用法 / Usage: 仅由 {@link #drainFrames} 调用。
     * @param buffer 参数 拼装缓冲；parameter the assembly buffer.
     * @param size 参数 有效字节数；parameter the meaningful byte count.
     * @return 返回 帧尾起点或 -1；returns the frame end offset or -1.
     */
    private static int frameEnd(byte[] buffer, int size) {
        for (int index = 0; index < size; index++) {
            int first = endLength(buffer, size, index);
            if (first <= 0) {
                continue;
            }
            if (endLength(buffer, size, index + first) > 0) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 中文说明：给定位置处帧终止符的总长度；与 {@link #frameEnd} 同一判据，用于把已消费字节移出缓冲。
     * English summary: The total length of the frame terminator at the given offset, judged exactly as {@link #frameEnd}
     * does, used to retire the consumed bytes.
     *
     * 用法 / Usage: 仅由 {@link #drainFrames} 调用。
     * @param buffer 参数 拼装缓冲；parameter the assembly buffer.
     * @param size 参数 有效字节数；parameter the meaningful byte count.
     * @param at 参数 帧尾起点；parameter the frame end offset.
     * @return 返回 终止符长度；returns the delimiter length.
     */
    private static int delimiterLength(byte[] buffer, int size, int at) {
        int first = endLength(buffer, size, at);
        int second = endLength(buffer, size, at + first);
        return first > 0 && second > 0 ? first + second : 0;
    }

    /**
     * 中文说明：单个行结束符长度：LF=1、CRLF=2、独立 CR=1；缓冲尾部的孤立 CR 返回 -1 表示还需更多字节，
     * 因此绝不会把一帧过早判定为闭合。
     * English summary: The length of one line terminator: LF is 1, CRLF is 2, a lone CR is 1; a trailing lone CR yields -1,
     * meaning more bytes are needed, so a frame is never closed prematurely.
     *
     * 用法 / Usage: 仅由 {@link #frameEnd} 与 {@link #delimiterLength} 调用。
     * @param buffer 参数 拼装缓冲；parameter the assembly buffer.
     * @param size 参数 有效字节数；parameter the meaningful byte count.
     * @param at 参数 判定起点；parameter the offset to judge.
     * @return 返回 长度、0（非行尾）或 -1（需更多字节）；returns the length, 0 for not-an-end-of-line or -1 to await bytes.
     */
    private static int endLength(byte[] buffer, int size, int at) {
        if (at >= size) {
            return -1;
        }
        if (buffer[at] == LINE_FEED) {
            return 1;
        }
        if (buffer[at] != CARRIAGE_RETURN) {
            return 0;
        }
        if (at + 1 >= size) {
            return -1;
        }
        return buffer[at + 1] == LINE_FEED ? 2 : 1;
    }

    /**
     * 中文说明：把已出帧的前缀移出缓冲，返回剩余未闭合字节数。
     * English summary: Retires the emitted frame prefix and returns the unclosed remainder.
     *
     * 用法 / Usage: 仅由 {@link #drainFrames} 调用。
     * @param buffer 参数 拼装缓冲；parameter the assembly buffer.
     * @param consumed 参数 已消费字节数；parameter the bytes consumed.
     * @param size 参数 当前有效字节数；parameter the meaningful byte count.
     * @return 返回 剩余字节数；returns the remaining byte count.
     */
    private static int consume(byte[] buffer, int consumed, int size) {
        int remainder = size - consumed;
        System.arraycopy(buffer, consumed, buffer, 0, remainder);
        return remainder;
    }

    /**
     * 中文说明：读一个块，EOF 返回零长数组（调用方据此完成），阻塞由外层 deadline 与 Reactor 超时共同约束。
     * English summary: Reads one chunk, returning a zero-length array at EOF (which completes the caller), where the blocking
     * is bounded by the outer deadline and the Reactor timeouts.
     *
     * 用法 / Usage: 由 {@link #commitStream} 与 {@link #chunks} 调用。
     * @param upstream 参数 上游字节流；parameter the upstream stream.
     * @return 返回 读到的字节，EOF 为空数组；returns the bytes read or an empty array at EOF.
     * @throws IOException 上游读失败；raised when the upstream read fails.
     */
    private static byte[] readChunk(InputStream upstream) throws IOException {
        byte[] chunk = new byte[UPSTREAM_CHUNK_BYTES];
        int read = upstream.read(chunk);
        return read < 0 ? new byte[0] : Arrays.copyOf(chunk, read);
    }

    /**
     * 中文说明：在 deadline 与 {@code yuheng.llm.max-request-bytes} 之内读完单播 body；超时是 504、超限与读失败是 502，
     * 永不返回半截「看起来成功」的字节。
     * English summary: Drains the unary body within the deadline and {@code yuheng.llm.max-request-bytes}: an expiry is a 504,
     * an oversize or a read failure a 502, and half a body never comes back as a seeming success.
     *
     * 用法 / Usage: 仅由 {@link #completeUnary} 调用。
     * @param upstream 参数 上游字节流；parameter the upstream stream.
     * @param deadline 参数 整次请求的截止时刻；parameter the request-wide deadline.
     * @return 返回 完整 body 字节；returns the whole body bytes.
     * @throws IOException 上游读失败；raised when the upstream read fails.
     */
    private byte[] readAll(InputStream upstream, Instant deadline) throws IOException {
        ByteArrayOutputStream collected = new ByteArrayOutputStream();
        byte[] chunk;
        while ((chunk = readChunk(upstream)).length > 0) {
            if (collected.size() + chunk.length > gatewayProperties.getMaxRequestBytes()) {
                throw protocolError("the upstream Messages response exceeded yuheng.llm.max-request-bytes", "usage");
            }
            collected.write(chunk, 0, chunk.length);
            if (Instant.now().isAfter(deadline)) {
                throw upstreamTimeout();
            }
        }
        return collected.toByteArray();
    }

    /**
     * 中文说明：核对渠道事实可支撑一次尝试：渠道已解析且启用、协议必须与入口协议一致（同协议是选择条件而非分支）、
     * 地址、部署形态、四段超时预算与并发预算为正；CLOUD 必须配置凭据引用，无认证 LOCAL 可为空；否则 503
     * {@code model_unavailable}，日志只给渠道 key。
     * English summary: Proves the channel facts support one attempt: the channel resolved and is enabled, its protocol equals
     * the ingress protocol (same-protocol is a selection criterion rather than a branch), the deployment/address and the
     * four timeout and concurrency budgets are valid; CLOUD needs a credential reference, while auth-free LOCAL may omit
     * one. Otherwise a 503 {@code model_unavailable} whose log
     * names only the channel key.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 调用。
     * @param route 参数 已放行的候选 route；parameter the admitted candidate route.
     * @param command 参数 本次调用命令；parameter this invocation's command.
     * @return 返回 可用渠道事实；returns the usable channel facts.
     */
    private LlmModelSnapshotBO.ChannelBO requireUsableChannel(LlmModelSnapshotBO.RouteBO route,
                                                             LlmInvocationCommandDTO command) {
        LlmModelSnapshotBO.ChannelBO channel = route.getChannel();
        if (channel == null || !Boolean.TRUE.equals(channel.getEnabled())
                || channel.getProtocol() != protocol()
                || (channel.getDeployment() != LlmDeploymentEnum.LOCAL
                && channel.getDeployment() != LlmDeploymentEnum.CLOUD)
                || StringUtils.isBlank(channel.getBaseUrl())
                || (channel.getDeployment() == LlmDeploymentEnum.CLOUD
                && (StringUtils.isBlank(channel.getSecretRef())
                || StringUtils.isBlank(gatewayProperties.getSecretsRoot())))
                || !positive(channel.getConnectTimeoutMs()) || !positive(channel.getHeaderTimeoutMs())
                || !positive(channel.getIdleTimeoutMs()) || !positive(channel.getTotalTimeoutMs())
                || !positive(channel.getMaxConcurrent())) {
            log.warn("llm messages route is unusable alias={} protocol={} channel={}", command.getModel(),
                    protocol(), route.getChannelKey());
            throw modelUnavailable();
        }
        return channel;
    }

    /**
     * 中文说明：解析并校验出网端点：只接受管理配置的绝对 http(s) 地址、无 userinfo/query/fragment，路径恒为
     * {@code /v1/messages}；不合即 503，且任何日志与错误都不出现该地址。
     * English summary: Parses and validates the egress endpoint: only an administrator-configured absolute http(s) URL without
     * userinfo, query or fragment is accepted, with the path always {@code /v1/messages}; otherwise a 503 in which neither a
     * log nor an error ever carries that address.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 调用。
     * @param channel 参数 渠道事实；parameter the channel facts.
     * @param command 参数 本次调用命令，仅用于日志；parameter this invocation's command, used only for logging.
     * @return 返回 出网绝对地址；returns the absolute egress URI.
     */
    private URI endpoint(LlmModelSnapshotBO.ChannelBO channel, LlmInvocationCommandDTO command) {
        try {
            URI parsed = URI.create(StringUtils.removeEnd(StringUtils.trimToEmpty(channel.getBaseUrl()), "/")
                    + MESSAGES_PATH);
            boolean https = "https".equalsIgnoreCase(parsed.getScheme());
            boolean localHttp = channel.getDeployment() == LlmDeploymentEnum.LOCAL
                    && "http".equalsIgnoreCase(parsed.getScheme());
            if (!parsed.isAbsolute() || StringUtils.isBlank(parsed.getHost()) || !(https || localHttp)
                    || parsed.getUserInfo() != null || parsed.getQuery() != null || parsed.getFragment() != null) {
                throw new IllegalArgumentException("unusable endpoint");
            }
            return parsed;
        } catch (IllegalArgumentException | NullPointerException rejected) {
            log.warn("llm messages channel endpoint is unusable alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            throw modelUnavailable();
        }
    }

    /**
     * 中文说明：解析上游凭据：只允许把 {@code secretRef} 解析到 {@code yuheng.llm.secrets-root} 之下并规范化后仍留在
     * 根内（{@code ..} 与绝对引用一律拒绝），必须是可读普通文件且内容非空；失败都是 503
     * {@code model_unavailable}，异常信息里既没有凭据值也没有挂载路径。
     * English summary: Resolves the upstream credential: {@code secretRef} may only be resolved under
     * {@code yuheng.llm.secrets-root} and must stay inside that normalized root (traversal and absolute references are
     * refused), must be a readable regular file and must not be blank; every failure is a 503
     * {@code model_unavailable} whose text holds neither the credential nor the mount path.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 调用。
     * @param channel 参数 渠道事实；parameter the channel facts.
     * @param command 参数 本次调用命令，仅用于日志；parameter this invocation's command, used only for logging.
     * @return 返回 凭据值，只交给出网请求头；returns the credential value, handed only to the egress request headers.
     */
    private String credential(LlmModelSnapshotBO.ChannelBO channel, LlmInvocationCommandDTO command) {
        if (channel.getSecretRef() == null && channel.getDeployment() == LlmDeploymentEnum.LOCAL) {
            return null;
        }
        String reference = StringUtils.trimToNull(channel.getSecretRef());
        String root = StringUtils.trimToNull(gatewayProperties.getSecretsRoot());
        if (root == null || reference == null) {
            log.error("llm messages secrets root is unconfigured alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            throw modelUnavailable();
        }
        try {
            Path mountRoot = Path.of(root).toAbsolutePath().normalize();
            Path referencePath = Path.of(reference);
            Path secret = mountRoot.resolve(referencePath).normalize();
            if (reference.contains("..") || referencePath.isAbsolute()
                    || !secret.startsWith(mountRoot) || !Files.isRegularFile(secret)) {
                throw modelUnavailable();
            }
            String value = Files.readString(secret, StandardCharsets.UTF_8).trim();
            if (value.isEmpty()) {
                throw modelUnavailable();
            }
            return value;
        } catch (IOException | RuntimeException failure) {
            // InvalidPathException 是 RuntimeException，因此越界引用与读失败在此合流，都只按渠道不可用收束。
            log.error("llm messages credential is unusable alias={} protocol={} channel={} exception={}",
                    command.getModel(), protocol(), channel.getChannelKey(), failure.getClass().getSimpleName());
            throw modelUnavailable();
        }
    }

    /**
     * 中文说明：发一次同协议上游请求：HTTP/1.1、永不自动跳转、连接超时取渠道预算；状态映射固定为
     * {@link HttpTimeoutException}→504 {@code upstream_timeout}、{@link RejectedExecutionException} 饱和→429
     * {@code rate_limit_exceeded}（且 retryable）、{@link InterruptedException}→504 并回位中断标志、其余 IO 失败→503
     * {@code model_unavailable}；全部只记渠道 key 与异常类型名，绝不记地址或凭据。
     * English summary: Issues one same-protocol request: HTTP/1.1, never following redirects automatically, with the connect
     * timeout from the channel budget; the status mapping is fixed at {@link HttpTimeoutException}→504
     * {@code upstream_timeout}, {@link RejectedExecutionException} saturation→429 {@code rate_limit_exceeded} (retryable),
     * {@link InterruptedException}→504 with the interrupt flag restored and any other IO failure→503
     * {@code model_unavailable}; only the channel key and the exception type name are logged, never an address or a
     * credential.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 调用。
     * @param request 参数 已构建的上游请求；parameter the built upstream request.
     * @param channel 参数 渠道事实；parameter the channel facts.
     * @param command 参数 本次调用命令；parameter this invocation's command.
     * @return 返回 已收到头与字节流的上游响应；returns the upstream response whose headers and byte stream arrived.
     */
    private HttpResponse<InputStream> send(HttpRequest request, LlmModelSnapshotBO.ChannelBO channel,
                                           LlmInvocationCommandDTO command) {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofMillis(channel.getConnectTimeoutMs()))
                .build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (HttpTimeoutException timeout) {
            log.warn("llm messages upstream timed out alias={} protocol={} channel={} budget={}ms",
                    command.getModel(), protocol(), channel.getChannelKey(), channel.getTotalTimeoutMs());
            throw upstreamTimeout();
        } catch (RejectedExecutionException saturated) {
            log.warn("llm messages egress saturated alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            throw saturated();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            log.warn("llm messages upstream wait interrupted alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            throw upstreamTimeout();
        } catch (IOException failure) {
            log.error("llm messages upstream unreachable alias={} protocol={} channel={} exception={}",
                    command.getModel(), protocol(), channel.getChannelKey(), failure.getClass().getSimpleName());
            throw modelUnavailable();
        }
    }

    /** 中文说明：单播出网请求头的构建与响应体类型在 {@link #exchange} 内一次决定，本方法只负责序列化请求文档；序列化不可能失败的树如果被拒，也只报 502。 English summary: Header building and the response body type are decided once inside {@link #exchange}; this method only serializes the request document, and even the theoretically impossible rejection stays a 502. */
    private byte[] serialize(ObjectNode document) {
        try {
            byte[] encoded = objectMapper.writeValueAsBytes(document);
            if (encoded.length > gatewayProperties.getMaxRequestBytes()) {
                throw requestTooLarge();
            }
            return encoded;
        } catch (JsonProcessingException failure) {
            log.error("llm messages request encoding failed, exception={}", failure.getClass().getSimpleName());
            throw protocolError("the encoded Messages request could not be serialized", "messages");
        }
    }

    /**
     * 中文说明：整请求字节上限：以 {@link ObjectNode#toString()} 的 UTF-8 长度判定，越界即 413
     * {@code request_too_large}（下一次请求可重试，但本次绝不截断送出去）。
     * English summary: The whole-request ceiling judged from the UTF-8 length of {@link ObjectNode#toString()}: beyond it a 413
     * {@code request_too_large}, retryable by a next request but never truncated and sent this time.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。
     * @param encoded 参数 已编码的原生请求文档；parameter the encoded native request document.
     * @param alias 参数 客户端 alias，仅用于日志；parameter the client alias, used only for logging.
     */
    private void enforceRequestCeiling(ObjectNode encoded, String alias) {
        int size = encoded.toString().getBytes(StandardCharsets.UTF_8).length;
        if (size > gatewayProperties.getMaxRequestBytes()) {
            log.warn("llm messages request exceeds the ceiling alias={} protocol={} bytes={} ceiling={}",
                    alias, protocol(), size, gatewayProperties.getMaxRequestBytes());
            throw requestTooLarge();
        }
    }

    /**
     * 中文说明：拒绝任何自证出网/凭据字段：只看被检对象的直接字段名（大小写不敏感），因此工具
     * {@code input_schema} 里合法的同名属性不受影响；命中即 400 {@code unsupported_parameter}。
     * English summary: Refuses any self-asserted egress or credential field by looking only at the direct field names of the
     * checked object, case-insensitively, so a legitimately同名 property inside a tool {@code input_schema} is untouched; a
     * match is a 400 {@code unsupported_parameter}.
     *
     * 用法 / Usage: 由 {@link #encodeRequest} 及其消息/块校验调用。
     * @param document 参数 被检对象；parameter the object under inspection.
     * @param scope 参数 范围名，仅用于日志；parameter the scope name, used only for logging.
     */
    private static void rejectSelfAssertedEgress(JsonNode document, String scope) {
        for (Iterator<String> names = document.fieldNames(); names.hasNext();) {
            String name = names.next();
            if (SELF_ASSERTED_EGRESS_FIELDS.contains(name.toLowerCase(Locale.ROOT))) {
                throw unsupported(name);
            }
        }
    }

    /** 中文说明：alias 必须存在、与命令一致且不含空白，否则 400；这是「只改 model」的前提。 English summary: The alias must be present, agree with the command and hold no whitespace, else a 400; that is the precondition of rewriting nothing but {@code model}. */
    private static void requireAlias(ObjectNode payload, String alias) {
        String declared = textField(payload, "model");
        if (declared == null || StringUtils.isBlank(alias)
                || StringUtils.containsWhitespace(declared) || !declared.equals(alias)) {
            throw unsupported("model");
        }
    }

    /** 中文说明：{@code max_tokens} 是 Messages 必填的正整数输出预算；借其它协议的预算字段视为未声明扩展，同样 400。 English summary: {@code max_tokens} is Messages' required positive integer output budget; borrowing another protocol's budget field is an undeclared extension and also a 400. */
    private static void requireOutputBudget(ObjectNode payload) {
        for (String borrowed : new String[] {"max_completion_tokens", "max_output_tokens"}) {
            if (payload.has(borrowed)) {
                throw unsupported(borrowed);
            }
        }
        JsonNode budget = payload.get("max_tokens");
        if (budget == null || !budget.isIntegralNumber() || !budget.canConvertToInt() || budget.asInt() < 1) {
            throw unsupported("max_tokens");
        }
    }

    /** 中文说明：{@code system} 只允许是顶层文本或 text blocks 数组；本方法从不把它折进 {@code messages}。 English summary: {@code system} may only be top-level text or an array of text blocks, and this method never folds it into {@code messages}. */
    private static void requireSystem(ObjectNode payload) {
        JsonNode system = payload.get("system");
        if (system == null || system.isNull()) {
            return;
        }
        if (system.isTextual()) {
            return;
        }
        if (!system.isArray() || system.isEmpty()) {
            throw unsupported("system");
        }
        for (JsonNode block : system) {
            if (!"text".equals(textField(block, "type")) || textField(block, "text") == null) {
                throw unsupported("system");
            }
        }
    }

    /**
     * 中文说明：messages 必须是 1–100 条、角色只有 user/assistant（{@code system} 想冒充消息即 400）、content 为文本或
     * 1–100 个块；块序由 {@link ObjectNode#deepCopy()} 原样保留，本方法只判不重排。
     * English summary: Messages must hold 1–100 entries whose roles are only user/assistant ({@code system} posing as a
     * message is a 400) with content being text or 1–100 blocks; block order survives verbatim through
     * {@link ObjectNode#deepCopy()} since this method only judges and never re-sorts.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。
     * @param payload 参数 原生请求文档；parameter the native request document.
     * @param offered 参数 route 声明的能力集合；parameter the capabilities the route declares.
     */
    private static void requireMessages(ObjectNode payload, Set<LlmCapabilityEnum> offered) {
        JsonNode messages = payload.get("messages");
        if (messages == null || !messages.isArray() || messages.isEmpty() || messages.size() > MAX_MESSAGES) {
            throw unsupported("messages");
        }
        for (JsonNode message : messages) {
            rejectSelfAssertedEgress(message, "messages");
            String role = textField(message, "role");
            if (role == null || !MESSAGE_ROLES.contains(role)) {
                throw unsupported("messages.role");
            }
            JsonNode content = message.get("content");
            if (content == null || content.isNull()) {
                throw unsupported("messages.content");
            }
            if (content.isTextual()) {
                continue;
            }
            if (!content.isArray() || content.isEmpty() || content.size() > MAX_CONTENT_BLOCKS) {
                throw unsupported("messages.content");
            }
            for (JsonNode block : content) {
                requireRequestBlock(block, role, offered);
            }
        }
    }

    /**
     * 中文说明：逐块校验请求 content：text 带文本；image 需 VISION 且 source 为对象；tool_use 只能出现在 assistant 且
     * 需 FUNCTION_TOOLS、带 1–128 的 id、name 与对象 input；tool_result 只能出现在 user 且带 tool_use_id 与文本或
     * text blocks 内容；thinking 需 REASONING 且必带 signature；redacted_thinking 需 REASONING 且带 data；未知块类型
     * 是未声明扩展，按 400 关闭。
     * English summary: Validates each request content block: text carries text; image needs VISION and an object source;
     * tool_use only occurs on assistant, needs FUNCTION_TOOLS, a 1–128 id, a name and an object input; tool_result only
     * occurs on user, carrying tool_use_id plus text or text blocks; thinking needs REASONING and must carry its signature;
     * redacted_thinking needs REASONING and its data; an unknown block type is an undeclared extension closed with 400.
     *
     * 用法 / Usage: 仅由 {@link #requireMessages} 调用。
     * @param block 参数 内容块；parameter the content block.
     * @param role 参数 所在消息角色；parameter the enclosing message role.
     * @param offered 参数 route 声明的能力集合；parameter the capabilities the route declares.
     */
    private static void requireRequestBlock(JsonNode block, String role, Set<LlmCapabilityEnum> offered) {
        rejectSelfAssertedEgress(block, "messages.content");
        String type = textField(block, "type");
        if (type == null) {
            throw unsupported("messages.content.type");
        }
        switch (type) {
            case "text" -> {
                if (textField(block, "text") == null) {
                    throw unsupported("messages.content.text");
                }
            }
            case "image" -> {
                requireCapability(offered, LlmCapabilityEnum.VISION, "messages.content.image");
                if (!block.path("source").isObject()) {
                    throw unsupported("messages.content.source");
                }
            }
            case "tool_use" -> {
                requireCapability(offered, LlmCapabilityEnum.FUNCTION_TOOLS, "messages.content.tool_use");
                if (!"assistant".equals(role)
                        || optionalText(block, "id", MAX_TOOL_ID_CHARS) == null
                        || optionalText(block, "name", MAX_TOOL_NAME_CHARS) == null
                        || !block.path("input").isObject()) {
                    throw unsupported("messages.content.tool_use");
                }
            }
            case "tool_result" -> {
                requireCapability(offered, LlmCapabilityEnum.FUNCTION_TOOLS, "messages.content.tool_result");
                if (!"user".equals(role)
                        || optionalText(block, "tool_use_id", MAX_TOOL_ID_CHARS) == null
                        || (block.has("is_error") && !block.path("is_error").isBoolean())) {
                    throw unsupported("messages.content.tool_result");
                }
                requireTextOrTextBlocks(block.get("content"), "messages.content.tool_result");
            }
            case "thinking" -> {
                requireCapability(offered, LlmCapabilityEnum.REASONING, "messages.content.thinking");
                if (textField(block, "thinking") == null || textField(block, "signature") == null) {
                    throw unsupported("messages.content.thinking");
                }
            }
            case "redacted_thinking" -> {
                requireCapability(offered, LlmCapabilityEnum.REASONING, "messages.content.redacted_thinking");
                if (textField(block, "data") == null) {
                    throw unsupported("messages.content.redacted_thinking");
                }
            }
            default -> throw unsupported("messages.content.type");
        }
    }

    /**
     * 中文说明：{@code tools} 最多 64 项、name 唯一且落在 {@code [A-Za-z0-9_-]{1,64}}、{@code input_schema} 必须是对象，
     * 出现工具族即要求 route 声明 FUNCTION_TOOLS；{@code description} 等其它字段原样送出，本方法不解释 schema 语义。
     * English summary: {@code tools} holds at most 64 entries whose names are unique and inside
     * {@code [A-Za-z0-9_-]{1,64}} and whose {@code input_schema} must be an object, and the tool family requires the route
     * to declare FUNCTION_TOOLS; every other field, {@code description} included, leaves untouched because this method
     * interprets no schema semantics.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。
     * @param payload 参数 原生请求文档；parameter the native request document.
     * @param offered 参数 route 声明的能力集合；parameter the capabilities the route declares.
     */
    private static void requireTools(ObjectNode payload, Set<LlmCapabilityEnum> offered) {
        JsonNode tools = payload.get("tools");
        if (tools == null || tools.isNull()) {
            return;
        }
        if (!tools.isArray() || tools.isEmpty() || tools.size() > MAX_TOOLS) {
            throw unsupported("tools");
        }
        requireCapability(offered, LlmCapabilityEnum.FUNCTION_TOOLS, "tools");
        if (declaredToolNames(tools).size() != tools.size()) {
            throw unsupported("tools.name");
        }
    }

    /**
     * 中文说明：收集并校验已声明的工具名集合，同时逐块拒绝自证出网字段；返回集合供 {@code tool_choice} 核实函数名引用。
     * English summary: Collects and validates the declared tool names while refusing a self-asserted egress field per entry,
     * and returns the set so {@code tool_choice} can verify its function-name reference.
     *
     * 用法 / Usage: 由 {@link #requireTools} 与 {@link #requireToolChoice} 调用。
     * @param tools 参数 工具定义数组；parameter the tool definition array.
     * @return 返回 声明过的工具名集合；returns the declared tool names.
     */
    private static Set<String> declaredToolNames(JsonNode tools) {
        Set<String> names = new LinkedHashSet<>();
        for (JsonNode tool : tools) {
            rejectSelfAssertedEgress(tool, "tools");
            String name = textField(tool, "name");
            if (name == null || !TOOL_NAME_PATTERN.matcher(name).matches()) {
                throw unsupported("tools.name");
            }
            if (!tool.path("input_schema").isObject()) {
                throw unsupported("tools.input_schema");
            }
            names.add(name);
        }
        return names;
    }

    /**
     * 中文说明：{@code tool_choice} 只接受 auto/none/any/tool 四种类型，{@code tool} 必须带一个已在 {@code tools} 里声明过的
     * {@code name}；{@code disable_parallel_tool_use} 等可选字段按原生文法原样保留。
     * English summary: {@code tool_choice} accepts only auto/none/any/tool, and {@code tool} must carry a {@code name}
     * already declared by {@code tools}; optional fields such as {@code disable_parallel_tool_use} stay exactly as the native
     * grammar allows.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。
     * @param payload 参数 原生请求文档；parameter the native request document.
     * @param offered 参数 route 声明的能力集合；parameter the capabilities the route declares.
     */
    private static void requireToolChoice(ObjectNode payload, Set<LlmCapabilityEnum> offered) {
        JsonNode choice = payload.get("tool_choice");
        if (choice == null || choice.isNull()) {
            return;
        }
        if (!choice.isObject()) {
            throw unsupported("tool_choice");
        }
        requireCapability(offered, LlmCapabilityEnum.FUNCTION_TOOLS, "tool_choice");
        rejectSelfAssertedEgress(choice, "tool_choice");
        String type = textField(choice, "type");
        if (type == null || !TOOL_CHOICE_TYPES.contains(type)) {
            throw unsupported("tool_choice.type");
        }
        if (!"tool".equals(type)) {
            return;
        }
        String name = textField(choice, "name");
        if (name == null) {
            throw unsupported("tool_choice.name");
        }
        JsonNode tools = payload.get("tools");
        if (tools != null && tools.isArray() && !declaredToolNames(tools).contains(name)) {
            throw unsupported("tool_choice.name");
        }
    }

    /**
     * 中文说明：采样参数只在被提交时判定：{@code temperature} 与 {@code top_p} 必须是 [0,1] 内的数值，{@code top_k} 必须
     * 是非负整数；未声明的采样旋钮由顶层已知字段关卡挡在 400。
     * English summary: Sampling knobs are judged only when sent: {@code temperature} and {@code top_p} must be numbers inside
     * [0,1] and {@code top_k} a non-negative integer, while an undeclared knob is already closed with 400 by the top-level
     * known-field gate.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。
     * @param payload 参数 原生请求文档；parameter the native request document.
     */
    private static void requireSampling(ObjectNode payload) {
        requireNumberInRange(payload, "temperature", 0d, 1d);
        requireNumberInRange(payload, "top_p", 0d, 1d);
        JsonNode topK = payload.get("top_k");
        if (topK != null && !topK.isNull()
                && (!topK.isIntegralNumber() || !topK.canConvertToInt() || topK.asInt() < 0)) {
            throw unsupported("top_k");
        }
    }

    /** 中文说明：可选数值字段的区间关卡，越界与非数值都是 400。 English summary: The range gate for an optional numeric field, where out-of-range and non-numbers are both a 400. */
    private static void requireNumberInRange(ObjectNode payload, String field, double min, double max) {
        JsonNode value = payload.get(field);
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isNumber()) {
            throw unsupported(field);
        }
        double numeric = value.doubleValue();
        if (Double.isNaN(numeric) || numeric < min || numeric > max) {
            throw unsupported(field);
        }
    }

    /**
     * 中文说明：{@code stop_sequences} 最多 4 项、每项都是 1–200 字符文本。 English summary: {@code stop_sequences} holds at
     * most four entries, each a 1–200 character text.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。
     * @param payload 参数 原生请求文档；parameter the native request document.
     */
    private static void requireStopSequences(ObjectNode payload) {
        JsonNode stops = payload.get("stop_sequences");
        if (stops == null || stops.isNull()) {
            return;
        }
        if (!stops.isArray() || stops.size() > MAX_STOP_SEQUENCES) {
            throw unsupported("stop_sequences");
        }
        for (JsonNode stop : stops) {
            String value = textValue(stop);
            if (value == null || value.isEmpty() || value.length() > MAX_STOP_SEQUENCE_CHARS) {
                throw unsupported("stop_sequences");
            }
        }
    }

    /**
     * 中文说明：{@code thinking} 只在 route 声明 REASONING 时同协议透传：类型恒为 enabled/disabled，若带
     * {@code budget_tokens} 则必须是正整数且小于本请求 {@code max_tokens}。本方法<b>不</b>替供应商核它的绝对下限：
     * 那属于「模型更严格约束由同协议上游返回原生400」（Spec §9.0.1），本地拒绝只会把合法请求挡在门外；同样，本方法
     * 绝不改写 thinking 与其签名，也不把它降级成文本。
     * English summary: {@code thinking} passes through in the same protocol only when the route declares REASONING: the
     * type is always enabled or disabled, and a {@code budget_tokens} must be a positive integer below this request's
     * {@code max_tokens}. This method deliberately does <b>not</b> adjudicate the vendor's absolute floor on behalf of the
     * provider, because a stricter model-side constraint is what upstream answers with its own native 400 (Spec §9.0.1)
     * and a local refusal would only lock a legal request out; equally, it never rewrites thinking nor its signature and
     * never degrades it into text.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。
     * @param payload 参数 原生请求文档；parameter the native request document.
     * @param offered 参数 route 声明的能力集合；parameter the capabilities the route declares.
     */
    private static void requireThinking(ObjectNode payload, Set<LlmCapabilityEnum> offered) {
        JsonNode thinking = payload.get("thinking");
        if (thinking == null || thinking.isNull()) {
            return;
        }
        if (!thinking.isObject()) {
            throw unsupported("thinking");
        }
        requireCapability(offered, LlmCapabilityEnum.REASONING, "thinking");
        rejectSelfAssertedEgress(thinking, "thinking");
        String type = textField(thinking, "type");
        if (type == null || !THINKING_TYPES.contains(type)) {
            throw unsupported("thinking.type");
        }
        JsonNode budget = thinking.get("budget_tokens");
        if (budget == null || budget.isNull()) {
            return;
        }
        if (!budget.isIntegralNumber() || !budget.canConvertToInt() || budget.asInt() < 1) {
            throw unsupported("thinking.budget_tokens");
        }
        JsonNode output = payload.get("max_tokens");
        if (output != null && output.isIntegralNumber() && output.canConvertToInt()
                && budget.asInt() >= output.asInt()) {
            throw unsupported("thinking.budget_tokens");
        }
    }

    /**
     * 中文说明：{@code metadata} 必须是对象，其 {@code user_id} 若存在则为 1–256 字符文本且永不进日志；其余键按未知但
     * 安全字段原样保留。 English summary: {@code metadata} must be an object whose optional {@code user_id} is 1–256
     * characters of text that never reaches a log, while its other keys stay as unknown-but-safe fields.
     *
     * 用法 / Usage: 仅由 {@link #encodeRequest} 调用。
     * @param payload 参数 原生请求文档；parameter the native request document.
     */
    private static void requireMetadata(ObjectNode payload) {
        JsonNode metadata = payload.get("metadata");
        if (metadata == null || metadata.isNull()) {
            return;
        }
        if (!metadata.isObject()) {
            throw unsupported("metadata");
        }
        rejectSelfAssertedEgress(metadata, "metadata");
        JsonNode userId = metadata.get("user_id");
        if (userId != null && !userId.isNull()) {
            String value = textValue(userId);
            if (value == null || value.isEmpty() || value.length() > MAX_METADATA_USER_ID_CHARS) {
                throw unsupported("metadata.user_id");
            }
        }
    }

    /** 中文说明：{@code stream} 只接受真正的布尔值；最终出网值恒由命令的 stream 位改写，避免两处不一致。 English summary: {@code stream} accepts a real boolean only, and the value that leaves is always the command's stream flag, which keeps the two from ever disagreeing. */
    private static void requireStreamFlag(ObjectNode payload) {
        JsonNode stream = payload.get("stream");
        if (stream != null && !stream.isNull() && !stream.isBoolean()) {
            throw unsupported("stream");
        }
    }

    /**
     * 中文说明：逐块校验响应 content 的保真要求：{@code text} 带文本、{@code tool_use} 带 1–128 的 id、name 与对象
     * {@code input}、{@code thinking} 必带 {@code thinking} 与 {@code signature}、{@code redacted_thinking} 必带
     * {@code data}；未知块类型按原生文法原样保留。
     * English summary: Validates the fidelity demands of each response content block: {@code text} carries text,
     * {@code tool_use} a 1–128 id, name and object {@code input}, {@code thinking} both its thinking text and its
     * {@code signature}, {@code redacted_thinking} its {@code data}; an unknown block type stays exactly as the native
     * grammar allows.
     *
     * 用法 / Usage: 仅由 {@link #decodeUnaryResponse} 调用。
     * @param block 参数 响应内容块；parameter the response content block.
     */
    private static void requireResponseBlock(JsonNode block) {
        String type = textField(block, "type");
        if (type == null) {
            throw protocolError("a response content block declared no type", "content");
        }
        switch (type) {
            case "text" -> {
                if (textField(block, "text") == null) {
                    throw protocolError("a text block carried no text", "content");
                }
            }
            case "tool_use" -> {
                if (optionalText(block, "id", MAX_TOOL_ID_CHARS) == null
                        || StringUtils.isBlank(textField(block, "name"))
                        || !block.path("input").isObject()) {
                    throw protocolError("a tool_use block carried no id, no name or a non-object input", "content");
                }
            }
            case "thinking" -> {
                if (textField(block, "thinking") == null || textField(block, "signature") == null) {
                    throw protocolError("a thinking block carried no thinking text or no signature", "content");
                }
            }
            case "redacted_thinking" -> {
                if (textField(block, "data") == null) {
                    throw protocolError("a redacted_thinking block carried no data", "content");
                }
            }
            default -> log.debug("llm messages unknown response content block kept type={}", type);
        }
    }

    /** 中文说明：块事件必须落在已经 {@code message_start} 之后，否则是文法违例。 English summary: A block event must land after {@code message_start}, otherwise the grammar is violated. */
    private static void requireStarted(AtomicBoolean started) {
        if (!started.get()) {
            throw protocolError("block events arrived before message_start", "message_start");
        }
    }

    /** 中文说明：读出帧里的 content block 索引：必须是非负整数，索引彼此独立不共享。 English summary: Reads the frame's content block index, which must be a non-negative integer because indices are independent. */
    private static int blockIndex(JsonNode data, String frameType) {
        JsonNode index = data.get("index");
        if (index == null || !index.isIntegralNumber() || !index.canConvertToInt() || index.asInt() < 0) {
            throw protocolError(frameType + " carried no usable content block index", "index");
        }
        return index.asInt();
    }

    /** 中文说明：可选对象字段的形状关卡：存在且非 null 时必须是对象。 English summary: The shape gate of an optional object field: when present and not null it must be an object. */
    private static void requireOptionalObject(JsonNode data, String field, String frameType) {
        JsonNode value = data.get(field);
        if (value != null && !value.isNull() && !value.isObject()) {
            throw protocolError(frameType + " carried a " + field + " that is not an object", field);
        }
    }

    /** 中文说明：可选「文本或 text blocks」字段的关卡，{@code system} 与 {@code tool_result.content} 共用；空数组与混合块型都是 400。 English summary: The gate for an optional "text or text blocks" field, shared by {@code system} and {@code tool_result.content}; an empty array or a mixed block type is a 400. */
    private static void requireTextOrTextBlocks(JsonNode value, String param) {
        if (value == null || value.isNull()) {
            throw unsupported(param);
        }
        if (value.isTextual()) {
            return;
        }
        if (!value.isArray() || value.isEmpty() || value.size() > MAX_CONTENT_BLOCKS) {
            throw unsupported(param);
        }
        for (JsonNode block : value) {
            if (!"text".equals(textField(block, "type")) || textField(block, "text") == null) {
                throw unsupported(param);
            }
        }
    }

    /** 中文说明：能力声明关卡：route 没声明的能力不是「静默丢弃」而是 400，客户端必须换一个声明了该能力的模型。 English summary: The capability gate: a capability the route does not declare is a 400 rather than a silent drop, so the client must pick a model that declares it. */
    private static void requireCapability(Set<LlmCapabilityEnum> offered, LlmCapabilityEnum required, String param) {
        if (!offered.contains(required)) {
            throw unsupported(param);
        }
    }

    /**
     * 中文说明：提交前每一取都核对整次请求 deadline：越界即 504 {@code upstream_timeout}，日志只给渠道 key 与预算。
     * English summary: Re-checks the request-wide deadline before every pre-commit pull: crossing it is a 504
     * {@code upstream_timeout} whose log names only the channel key and the budget.
     *
     * 用法 / Usage: 仅由 {@link #commitStream} 调用。
     * @param deadline 参数 整次请求的截止时刻；parameter the request-wide deadline.
     * @param channel 参数 渠道事实；parameter the channel facts.
     * @param command 参数 本次调用命令，仅用于日志；parameter this invocation's command, used only for logging.
     */
    private void requireDeadline(Instant deadline, LlmModelSnapshotBO.ChannelBO channel,
                                 LlmInvocationCommandDTO command) {
        if (Instant.now().isBefore(deadline)) {
            return;
        }
        log.warn("llm messages upstream budget exhausted before commit alias={} protocol={} channel={} budget={}ms",
                command.getModel(), protocol(), channel.getChannelKey(), channel.getTotalTimeoutMs());
        throw upstreamTimeout();
    }

    private static boolean positive(Integer value) {
        return value != null && value > 0;
    }

    /** 中文说明：只接受纯数字的响应头值，避免把任何非预期文本带给客户端。 English summary: Accepts only a purely numeric header value, which keeps unexpected text from ever reaching the client. */
    private static String numericHeader(HttpResponse<InputStream> response, String name) {
        return response.headers().firstValue(name).filter(StringUtils::isNumeric).orElse(null);
    }

    /** 中文说明：静默关闭上游流：取消传播与失败收束都不允许再抛出一个新异常盖掉真正的成因。 English summary: Closes the upstream stream quietly: neither cancel propagation nor a failure closure may raise a second exception over the real cause. */
    private static void closeQuietly(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException | RuntimeException failure) {
            log.debug("llm messages upstream stream close ignored, exception={}", failure.getClass().getSimpleName());
        }
    }

    /** 中文说明：把一个节点自身的文本值读出来，非文本或缺失返回 {@code null} 由调用方判定。 English summary: Reads a node's own textual value, yielding {@code null} for a missing or non-textual node so the caller judges it. */
    private static String textValue(JsonNode node) {
        return node == null || !node.isTextual() ? null : node.textValue();
    }

    /** 中文说明：读出对象某个字段的文本值；缺失、null 与非文本都归一为 {@code null}，调用方决定是 400 还是 502。 English summary: Reads an object field's textual value, normalizing absence, null and a non-textual node into {@code null} so the caller decides between 400 and 502. */
    private static String textField(JsonNode container, String field) {
        return container == null ? null : textValue(container.get(field));
    }

    /** 中文说明：读出必填且非空白的文本字段，越界或空白一律按缺失处理返回 {@code null}。 English summary: Reads a required non-blank text field, treating oversize and blank alike as absence by yielding {@code null}. */
    private static String optionalText(JsonNode container, String field, int maxChars) {
        String value = textField(container, field);
        return value == null || value.isEmpty() || value.length() > maxChars ? null : value;
    }

    /** 中文说明：400 {@code unsupported_parameter}：请求用了未声明的扩展、越界的参数或本 route 未声明的能力；结果确定可诊断，下一次请求修正后才可重试。 English summary: The 400 {@code unsupported_parameter}: the request used an undeclared extension, an out-of-range parameter or a capability this route does not declare; deterministic and diagnosable for a corrected next request. */
    private static LlmInvocationException unsupported(String param) {
        return new LlmInvocationException(400, "unsupported_parameter", param,
                "The Messages request uses a parameter, a value or a capability this endpoint does not accept", false);
    }

    /** 中文说明：502 {@code upstream_protocol_error}：上游违反了本协议文法；{@code reason} 只能是实现内置常量短语，绝不含上游原文。 English summary: The 502 {@code upstream_protocol_error}: upstream violated this protocol's grammar, where {@code reason} is an implementer-authored constant phrase that never carries upstream text. */
    private static LlmInvocationException protocolError(String reason, String param) {
        return new LlmInvocationException(502, "upstream_protocol_error", param,
                "The upstream Messages response violated the protocol: " + reason, false);
    }

    /** 中文说明：413 {@code request_too_large}：整份文档越过 {@code yuheng.llm.max-request-bytes}，本次绝不截断送出去。 English summary: The 413 {@code request_too_large}: the document crossed {@code yuheng.llm.max-request-bytes} and is never truncated and sent this time. */
    private static LlmInvocationException requestTooLarge() {
        return new LlmInvocationException(413, "request_too_large", null,
                "The Messages request exceeds the configured request size limit", false);
    }

    /** 中文说明：429 {@code rate_limit_exceeded}：出网并发已满，只能发生在提交之前，调用方按 {@code Retry-After} 退避。 English summary: The 429 {@code rate_limit_exceeded}: egress concurrency is exhausted, which can only happen before commit so the caller backs off per {@code Retry-After}. */
    private static LlmInvocationException saturated() {
        return new LlmInvocationException(429, "rate_limit_exceeded", null,
                "The model gateway is serving its maximum number of concurrent requests", true);
    }

    /** 中文说明：503 {@code model_unavailable}：渠道事实不成立、地址不合白名单契约、凭据不可解析或上游不可达；提交前判定，绝不猜渠道。 English summary: The 503 {@code model_unavailable}: unusable channel facts, an address outside the allow-list contract, an unresolvable credential or an unreachable upstream, all judged before commit and never on a guessed channel. */
    private static LlmInvocationException modelUnavailable() {
        return new LlmInvocationException(503, "model_unavailable", null,
                "No usable same-protocol Messages channel is available for this model", true);
    }

    /** 中文说明：504 {@code upstream_timeout}：预算耗尽且结果可能未知，因此永不标记为可重试。 English summary: The 504 {@code upstream_timeout}: the budget expired with an outcome that may be unknown, so it is never marked retryable. */
    private static LlmInvocationException upstreamTimeout() {
        return new LlmInvocationException(504, "upstream_timeout", null,
                "The upstream Messages call exceeded its time budget", false);
    }

    /** 中文说明：EOF 缺少 {@code message_stop} 的 ABORTED 判定：按上游协议非法收束，绝不伪造终态。 English summary: The ABORTED verdict for an EOF without {@code message_stop}: it lands as an upstream protocol violation and never fabricates a terminal marker. */
    private static LlmInvocationException abort() {
        return new LlmInvocationException(502, "upstream_protocol_error", "message_stop",
                "The upstream Messages stream ended before its terminal message_stop", false);
    }

    /**
     * 中文说明：把上游非 2xx 状态映射成本进程可安全出网的原生错误载体：状态原样保留，机器码取 Spec 错误表字面量，
     * 说明是实现内置短语；上游正文、上游错误类别与任何凭据都不参与构造。
     * English summary: Maps a non-2xx upstream status into this process's safely egress-able native error carrier: the status
     * survives verbatim, the machine code is a Spec error-table literal and the description an implementer-authored phrase,
     * while upstream text, upstream categories and any credential take no part in construction.
     *
     * 用法 / Usage: 仅由 {@link #exchange} 经 {@link #nativeFailure} 调用。
     * @param status 参数 上游返回的状态码；parameter the status upstream answered with.
     * @return 返回 与该状态一致的安全错误载体；returns the safe carrier carrying that status.
     */
    private static LlmInvocationException upstreamFailure(int status) {
        return switch (status) {
            case 400, 422 -> new LlmInvocationException(status, "unsupported_parameter", null,
                    "The upstream model rejected the Messages request document", false);
            case 401 -> new LlmInvocationException(status, "model_unavailable", null,
                    "The upstream Messages channel refused the credential", true);
            case 403 -> new LlmInvocationException(status, "model_unavailable", null,
                    "The upstream Messages channel refused access to this model", true);
            case 404 -> new LlmInvocationException(status, "model_unavailable", null,
                    "The upstream model is not available on this channel", true);
            case 413 -> requestTooLarge();
            case 429 -> saturated();
            case 504 -> upstreamTimeout();
            case 529 -> new LlmInvocationException(status, "model_unavailable", null,
                    "The upstream Messages channel is overloaded", true);
            default -> status >= 500
                    ? new LlmInvocationException(status, "model_unavailable", null,
                    "The upstream Messages channel failed to serve this model", true)
                    : new LlmInvocationException(status, "upstream_protocol_error", null,
                    "The upstream Messages call failed before it committed", false);
        };
    }

    /**
     * 中文说明：{@code error.type} 的取值规则：异常机器码本身就是 Messages 原生类别时按原值出网，否则按状态映射到八个
     * 原生类别之一；映射表是穷尽的，任何输入都得到一个合法类别，绝不产出 {@code code}/{@code param} 之类的非原生槽位。
     * English summary: How {@code error.type} is chosen: when the exception's machine code already is a native Messages
     * category it travels verbatim, otherwise the status maps onto one of the eight native categories; the mapping is
     * exhaustive, so every input yields a legal category and a non-native slot such as {@code code} or {@code param} never
     * appears.
     *
     * 用法 / Usage: 仅由 {@link #encodeError} 调用。
     * @param error 参数 已脱敏的原生错误载体；parameter the masked native error carrier.
     * @return 返回 Messages 原生错误类别；returns the native Messages error category.
     */
    private static String errorType(LlmInvocationException error) {
        String code = StringUtils.trimToEmpty(error.getCode()).toLowerCase(Locale.ROOT);
        if (NATIVE_ERROR_TYPES.contains(code)) {
            return code;
        }
        return switch (error.getStatus()) {
            case 400, 409, 422 -> "invalid_request_error";
            case 401 -> "authentication_error";
            case 403 -> "permission_error";
            case 404 -> "not_found_error";
            case 413 -> "request_too_large";
            case 429 -> "rate_limit_error";
            case 529 -> "overloaded_error";
            default -> error.getStatus() >= 500 ? "api_error" : "invalid_request_error";
        };
    }
}
