package top.egon.cola.component.yuheng.llm.proxy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.task.TaskRejectedException;
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
 * 中文说明：{@code OpenAiChatProtocolStrategy} 是 {@link LlmProtocolStrategy} 的 <b>Chat Completions 协议面</b>实现，
 * 以 bean 名 {@code openAiChatProtocolStrategy} 落在 {@code llmProtocolStrategyRegistry} 的固定四条映射里，只服务
 * {@link LlmProtocolEnum#OPENAI_CHAT}，永不回落别的协议、永不按协议写 if/switch。本类承担 Spec §9.2.2 交给协议面的
 * 四项职责与唯一编排入口：① {@link #encodeRequest} 在<b>已解析但未按协议校验</b>的原生 {@link ObjectNode} 上核对已知
 * 必需字段——非空 {@code messages}（1–100 项，role 限 system/developer/user/assistant/tool，assistant 携带
 * {@code tool_calls} 时 content 可为 null，tool 消息必须带 {@code tool_call_id}）、本面自己的输出预算字段
 * （{@code max_tokens} 与 {@code max_completion_tokens} <b>二选一</b>、正整数 ≤8192、不得以另一协议字段替换）、
 * {@code temperature} 0–2、{@code top_p} 0–1、{@code n} 只接受 1、{@code tools} ≤64 且仅 function 形状、
 * {@code tool_choice} 为 none/auto/required 或引用已声明函数、{@code parallel_tool_calls} 布尔、
 * {@code stream_options.include_usage} 仅在 {@code stream=true} 时可传——并拒绝任何自证出网/凭据字段
 * （{@code upstream_url}/{@code base_url}/{@code api_key}/{@code authorization}/{@code x-api-key}/
 * {@code credential}/{@code host}）与未声明请求扩展（{@code response_format}、{@code modalities}、
 * {@code reasoning_effort} 等只有在 route 显式声明对应 STRUCTURED_OUTPUT/VISION/REASONING 能力时才同协议透传）。
 * 通过校验后<b>只</b>把 {@code payload.model} 从客户端 alias 改写为 {@code route.upstreamModel}：实现方式是整份文档
 * 深拷贝后替换该一个键，因此 {@code tools}/{@code tool_choice}/{@code parallel_tool_calls}/
 * {@code stream_options.include_usage} 以及所有数值表示（含 {@code 1.0} 这类尾零）与数组顺序都逐字保留；
 * 整个文档还须落在 {@code yuheng.llm.max-request-bytes} 内，越界即 413 {@code request_too_large}。
 * ② {@link #decodeUnaryResponse} 全保真透传：整份原生 JSON 深拷贝后仅回写 {@code model} 为客户端 alias，
 * {@code choices[].message}（含 {@code tool_calls} 的 {@code id}/{@code type}/{@code function.name}/字符串
 * {@code arguments}）、{@code finish_reason}、{@code usage} 与全部未知但安全的字段一律保留，绝不缩减成自定义 VO、
 * 绝不重排数组、绝不把上游缺失的 {@code usage} 伪造成 0；只有本协议必需的结构（{@code choices} 非空数组、每项的
 * {@code message} 对象、{@code tool_calls} 与 {@code usage} 的形状）缺失或变形才判 502
 * {@code upstream_protocol_error}。③ {@link #encodeStream} 解析上游 SSE 的 {@code data:} 帧（跨缓冲半帧可续、
 * 未知但合法字段保留），逐帧验证 chunk 结构（{@code choices[].delta}、{@code delta.tool_calls[].index} 的片段序、
 * 终帧 {@code finish_reason}）并把 {@code model} 回写 alias，再按 Chat 文法重新出帧，终帧之后恰好一个
 * {@code data: [DONE]}；EOF 没有 {@code [DONE]} 即判 ABORTED 并<b>绝不</b>补发伪终态帧，流内失败只能以错误终止；
 * 单帧受 {@code yuheng.llm.max-frame-bytes} 约束、上游预取恒为 1、下游取消传播为上游取消、每个 {@link DataBuffer}
 * 恰好释放一次（本类唯一可变状态是「不超过一帧」的残帧缓冲，绝不整条流缓存成 {@code byte[]}）。
 * ④ {@link #encodeError} 只产出 {@code {"error":{message,type,param,code}}} 这一个顶层键，四个值全部取自
 * {@link LlmInvocationException} 的安全字段，绝不套 admin 的 {@code code}/{@code data} 信封，也绝不带出 token、密钥、
 * baseUrl、upstreamModel、prompt 正文或工具参数。
 * {@link #exchange} 是唯一编排入口：{@link #encodeRequest} → 渠道必须<b>已解析、已启用、同协议</b>且部署形态为
 * LOCAL/CLOUD 之一 → 凭据只由 {@code yuheng.llm.secrets-root} 之下对 {@code secretRef} 的规范化路径解析（越界、
 * {@code ..} 穿越、非普通文件、空白即 503 {@code model_unavailable}，值与引用名都不入日志）→ JDK 原生
 * {@link HttpClient} 就地按渠道 connect/header/idle/total 四段预算构建（HTTP/1.1、绝不跟随跳转）→ 非 2xx 按上游
 * <b>原生状态</b>经 {@link #encodeError} 写回 {@code routed}，永不伪装成空 200 → 2xx 由 {@code stream} 位分流：
 * 单播走 {@link #decodeUnaryResponse}，流式必须先在本方法内读出并验证<b>一个完整合法帧</b>才写回 publisher，这就是
 * 提交屏障——屏障之后禁止换模型、禁止重试、禁止改写 HTTP 状态、禁止伪造 {@code [DONE]}，只能终止本流；线程池饱和
 * （{@code TaskRejectedException}/{@code RejectedExecutionException}）在提交前表现为 429
 * {@code rate_limit_exceeded}。本方法不开事务、不加锁，也绝不在事务或锁内等待上游。日志只有 alias、协议、渠道 key、
 * priority、状态、字节数与帧数，永不含正文、prompt、工具参数与密钥。
 * English summary: {@code OpenAiChatProtocolStrategy} is the <b>Chat Completions face</b> of
 * {@link LlmProtocolStrategy}, registered as {@code openAiChatProtocolStrategy} in the fixed four-entry
 * {@code llmProtocolStrategyRegistry} and serving only {@link LlmProtocolEnum#OPENAI_CHAT}, never falling back to another
 * protocol and never branching on a protocol. It carries the four duties Spec §9.2.2 assigns to the protocol face plus the
 * one orchestration entry. (1) {@link #encodeRequest} verifies the known required fields on the parsed-but-not-yet
 * protocol-validated native {@link ObjectNode} — a non-empty {@code messages} array (1–100 entries, roles limited to
 * system/developer/user/assistant/tool, a null content only for an assistant entry carrying {@code tool_calls}, a
 * {@code tool_call_id} mandatory on tool entries), this face's own output budget ({@code max_tokens} versus
 * {@code max_completion_tokens}, exactly one of them, a positive integer ≤8192 never substituted by another protocol's
 * field), {@code temperature} 0–2, {@code top_p} 0–1, {@code n} accepted only as 1, {@code tools} ≤64 in function shape
 * only, {@code tool_choice} of none/auto/required or referencing a declared function, the {@code parallel_tool_calls}
 * boolean and {@code stream_options.include_usage} permitted only with {@code stream=true} — and refuses every
 * self-asserted egress or credential field ({@code upstream_url}, {@code base_url}, {@code api_key},
 * {@code authorization}, {@code x-api-key}, {@code credential}, {@code host}) plus every undeclared request extension
 * ({@code response_format}, {@code modalities}, {@code reasoning_effort} and friends pass through only when the route
 * explicitly declares the matching STRUCTURED_OUTPUT/VISION/REASONING capability). Only then is
 * {@code payload.model} rewritten from the client alias to {@code route.upstreamModel}, by deep-copying the whole
 * document and replacing that one key, so {@code tools}/{@code tool_choice}/{@code parallel_tool_calls}/
 * {@code stream_options.include_usage}, every numeric representation (including a trailing {@code 1.0}) and every array
 * order survive verbatim; the document must stay within {@code yuheng.llm.max-request-bytes} or it is a 413
 * {@code request_too_large}. (2) {@link #decodeUnaryResponse} is a full-fidelity pass-through: the native JSON is
 * deep-copied, only {@code model} is written back to the client alias, and {@code choices[].message} (with each
 * {@code tool_calls} entry's {@code id}/{@code type}/{@code function.name}/string {@code arguments}),
 * {@code finish_reason}, {@code usage} and all unknown-but-safe fields survive — never reduced into a bespoke VO, never
 * re-ordered, never fabricating an absent {@code usage} as zero; only a genuinely required structure that is missing or
 * malformed (a non-empty {@code choices} array, each entry's {@code message} object, the shape of {@code tool_calls} and
 * {@code usage}) becomes a 502 {@code upstream_protocol_error}. (3) {@link #encodeStream} parses the upstream SSE
 * {@code data:} frames (a half frame continues across buffers, legitimate unknown fields survive), validates each chunk
 * ({@code choices[].delta}, the fragment ordering of {@code delta.tool_calls[].index}, the terminal
 * {@code finish_reason}), writes the alias back into {@code model} and re-emits Chat grammar ending with exactly one
 * {@code data: [DONE]} after the terminal chunk; an EOF without {@code [DONE]} is ABORTED and a fake terminal frame is
 * <b>never</b> emitted, an in-stream failure ends only with an error; each frame is capped at
 * {@code yuheng.llm.max-frame-bytes}, upstream prefetch never exceeds one frame, a downstream cancellation propagates
 * upstream and every {@link DataBuffer} is released exactly once (the only mutable state is a sub-frame residual buffer,
 * never a whole stream as {@code byte[]}). (4) {@link #encodeError} emits only the top-level {@code error} key with
 * {@code message}, {@code type}, {@code param} and {@code code} taken from the safe fields of
 * {@link LlmInvocationException}, never the admin {@code code}/{@code data} envelope and never a token, secret, base URL,
 * upstream model, prompt text or tool argument. {@link #exchange} is the single orchestration entry:
 * {@link #encodeRequest} → a <b>resolved, enabled, same-protocol</b> channel of LOCAL or CLOUD deployment → credentials
 * only from a normalized {@code secretRef} path under {@code yuheng.llm.secrets-root} (an escape, a {@code ..} traversal,
 * a non-regular file or a blank value is a 503 {@code model_unavailable} whose detail never reaches a log) → a locally
 * built JDK-native {@link HttpClient} honouring the channel's connect/header/idle/total budgets over HTTP/1.1 without
 * following redirects → a non-2xx leaving through {@link #encodeError} at the upstream's <b>native</b> status into
 * {@code routed}, never dressed as an empty 200 → a 2xx split by the stream flag: unary through
 * {@link #decodeUnaryResponse}, streaming only after this method has read and validated <b>one complete frame</b>, which
 * <b>is</b> the commit barrier: past it no model switch, retry, HTTP status rewrite nor fabricated {@code [DONE]} is
 * possible and only ending the stream is; pool saturation ({@code TaskRejectedException}/
 * {@code RejectedExecutionException}) presents as a pre-commit 429 {@code rate_limit_exceeded}. This method opens no
 * transaction, takes no lock and never waits on upstream inside one. Logs carry only the alias, protocol, channel key,
 * priority, status, byte and frame counts — never a body, prompt, tool argument or key.
 *
 * 用法 / Usage: 由 {@code LlmApiController} 经 {@code llmProtocolStrategyRegistry} 按名取得后调用：
 * {@code exchange(command, route, routed)} 完成同协议出网，四个编解码操作另由 {@code LlmProtocolContractTest} 直接驱动
 * 以在无真实模型的情况下固定原生 wire shape。构造合同与其余三个 Strategy 完全相同：只有两个 {@code private final}
 * 协作者（{@link LlmGatewayProperties} 按 {@code LlmGatewayProperties.BEAN_NAME} 限定、{@link ObjectMapper} 按
 * {@code jacksonObjectMapper} 限定），由 {@code @RequiredArgsConstructor} 注入，不新增任何 bean、协作者类型或工具类。
 * / Resolved by {@code LlmApiController} through {@code llmProtocolStrategyRegistry} by name:
 * {@code exchange(command, route, routed)} performs the same-protocol egress while the four codec operations are driven
 * directly by {@code LlmProtocolContractTest} to pin the native wire shape without a live model. The construction contract
 * is identical to the other three Strategies: exactly two {@code private final} collaborators
 * ({@link LlmGatewayProperties} qualified by {@code LlmGatewayProperties.BEAN_NAME} and {@link ObjectMapper} qualified by
 * {@code jacksonObjectMapper}) injected through {@code @RequiredArgsConstructor}, adding no bean, collaborator type or
 * helper class.
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@Component("openAiChatProtocolStrategy")
public class OpenAiChatProtocolStrategy implements LlmProtocolStrategy {

    /** 中文说明：Chat 请求面在本版无条件已知的字段；其余键只有落在下方能力门控表里且 route 显式声明能力时才透传。 English summary: the keys the Chat request face knows unconditionally; any other key passes only through the capability gate below when the route declares it. */
    private static final Set<String> DECLARED_REQUEST_FIELDS = Set.of(
            "model", "messages", "stream", "max_tokens", "max_completion_tokens", "temperature", "top_p",
            "tools", "tool_choice", "parallel_tool_calls", "stream_options", "n", "stop", "presence_penalty",
            "frequency_penalty", "user");

    /** 中文说明：受 route 能力门控的扩展字段：未声明对应能力即 400 {@code unsupported_parameter}，绝不静默降级为纯文本。 English summary: extension fields gated on the route's declared capabilities: without the capability a 400 {@code unsupported_parameter} is raised rather than a silent degradation to plain text. */
    private static final Map<String, LlmCapabilityEnum> CAPABILITY_GATED_FIELDS = Map.of(
            "response_format", LlmCapabilityEnum.STRUCTURED_OUTPUT,
            "json_schema", LlmCapabilityEnum.STRUCTURED_OUTPUT,
            "modalities", LlmCapabilityEnum.VISION,
            "image_url", LlmCapabilityEnum.VISION,
            "reasoning_effort", LlmCapabilityEnum.REASONING,
            "thinking", LlmCapabilityEnum.REASONING);

    /** 中文说明：客户端可自证的出网/凭据字段名——这些事实只存在于服务端受管配置里，出现即 400 且绝不回显其值。 English summary: self-asserted egress or credential field names, facts that live only in server-side managed configuration, refused with a 400 that never echoes their value. */
    private static final Set<String> SELF_ASSERTED_FIELDS = Set.of(
            "upstream_url", "upstreamUrl", "base_url", "baseUrl", "url", "endpoint", "host",
            "credential", "credentials", "api_key", "apikey", "x-api-key", "x_api_key", "secret",
            "secret_ref", "secretRef", "authorization", "headers", "timeout");

    /** 中文说明：{@code messages[].role} 的本协议合法集合（Spec §9.0.1 Chat 补充字段合同）。 English summary: the legal {@code messages[].role} set of this protocol (the Chat field contract of Spec §9.0.1). */
    private static final Set<String> MESSAGE_ROLES = Set.of("system", "developer", "user", "assistant", "tool");

    /** 中文说明：字符串型 {@code tool_choice} 的三个合法模式；对象型另行按 {@code {type:function,function:{name}}} 校验。 English summary: the three legal string {@code tool_choice} modes; the object form is checked separately as {@code {type:function,function:{name}}}. */
    private static final Set<String> TOOL_CHOICE_MODES = Set.of("none", "auto", "required");

    /** 中文说明：协议固定字段名，逐字对应 Spec §9.0.1/§9.2.2 的原生 wire 键。 English summary: the protocol's fixed field names, verbatim from the native wire keys of Spec §9.0.1/§9.2.2. */
    private static final String MODEL_FIELD = "model";
    private static final String MESSAGES_FIELD = "messages";
    private static final String STREAM_FIELD = "stream";
    private static final String TOOLS_FIELD = "tools";
    private static final String TOOL_CHOICE_FIELD = "tool_choice";
    private static final String PARALLEL_TOOL_CALLS_FIELD = "parallel_tool_calls";
    private static final String STREAM_OPTIONS_FIELD = "stream_options";
    private static final String INCLUDE_USAGE_FIELD = "include_usage";
    private static final String MAX_TOKENS_FIELD = "max_tokens";
    private static final String MAX_COMPLETION_TOKENS_FIELD = "max_completion_tokens";
    private static final String TEMPERATURE_FIELD = "temperature";
    private static final String TOP_P_FIELD = "top_p";
    private static final String N_FIELD = "n";
    private static final String ROLE_FIELD = "role";
    private static final String CONTENT_FIELD = "content";
    private static final String NAME_FIELD = "name";
    private static final String FUNCTION_FIELD = "function";
    private static final String TOOL_CALLS_FIELD = "tool_calls";
    private static final String TOOL_CALL_ID_FIELD = "tool_call_id";
    private static final String ARGUMENTS_FIELD = "arguments";
    private static final String TYPE_FIELD = "type";
    private static final String ID_FIELD = "id";
    private static final String INDEX_FIELD = "index";
    private static final String CHOICES_FIELD = "choices";
    private static final String MESSAGE_FIELD = "message";
    private static final String DELTA_FIELD = "delta";
    private static final String FINISH_REASON_FIELD = "finish_reason";
    private static final String USAGE_FIELD = "usage";

    /** 中文说明：SSE 的帧文法常量：本面只把 {@code data:} 计入载荷，终态标记是 {@code [DONE]}；
     * {@code event:}/{@code id:}/{@code retry:} 与 {@code:} 注释行是事件层控制字段，绝不参与 chunk 载荷。
     * English summary: the SSE grammar constants: only {@code data:} carries the payload and the terminal marker is
     * {@code [DONE]}, while {@code event:}/{@code id:}/{@code retry:} and {@code:} comment lines are event-level control
     * fields that never join the chunk payload. */
    private static final String DATA_PREFIX = "data:";
    private static final String EVENT_PREFIX = "event:";
    private static final String ID_PREFIX = "id:";
    private static final String RETRY_PREFIX = "retry:";
    private static final String COMMENT_PREFIX = ":";
    private static final String DONE_MARKER = "[DONE]";
    private static final String DONE_FRAME = "data: [DONE]\n\n";
    private static final String FRAME_SEPARATOR = "\n\n";
    private static final byte LINE_FEED = (byte) '\n';
    private static final byte CARRIAGE_RETURN = (byte) '\r';

    /** 中文说明：请求面体量与取值合同：1–100 条消息、≤64 个函数工具、输出预算 1..8192、采样参数闭区间。 English summary: the request-face volume and value contract: 1–100 messages, ≤64 function tools, an output budget of 1..8192 and closed sampling ranges. */
    private static final int MAX_MESSAGES = 100;
    private static final int MAX_TOOLS = 64;
    private static final int MAX_OUTPUT_TOKENS = 8_192;
    private static final double MIN_TEMPERATURE = 0D;
    private static final double MAX_TEMPERATURE = 2D;
    private static final double MIN_TOP_P = 0D;
    private static final double MAX_TOP_P = 1D;
    private static final int MAX_MESSAGE_CONTENT_BYTES = 1_048_576;

    /** 中文说明：出网路径段：渠道 baseUrl 已含 API 根（如 {@code /v1}），本面只在其后追加 {@code chat/completions}。 English summary: the egress path segments: a channel base URL already carries the API root (for example {@code /v1}), so this face only appends {@code chat/completions}. */
    private static final List<String> CHAT_PATH_SEGMENTS = List.of("chat", "completions");

    /** 中文说明：解析出的凭据长度上界；超限按「不可用」处理，避免异常挂载文件把引擎拖垮。 English summary: the ceiling of a resolved credential; an oversized one is treated as unavailable so an anomalous mount cannot exhaust the engine. */
    private static final int MAX_SECRET_CHARACTERS = 4_096;

    /** 中文说明：上游字节读取块大小与流式预取量：预取恒为 1 帧，绝不批量向要。 English summary: the upstream read chunk and the stream prefetch: prefetch is always exactly one frame, never a batch. */
    private static final int READ_CHUNK_BYTES = 8_192;
    private static final int PREFETCH_ONE = 1;

    /** 中文说明：非注入的缓冲工厂共享实例：出网字节与客户端帧都堆内单缓冲，便于恰好一次释放。 English summary: the shared non-injected buffer factory: egress bytes and client frames are single heap buffers so each release happens exactly once. */
    private static final DataBufferFactory BUFFER_FACTORY = DefaultDataBufferFactory.sharedInstance;

    /** 中文说明：{@code yuheng.llm.*} 运行边界：字节上限 {@code max-request-bytes}/{@code max-frame-bytes} 与凭据挂载根 {@code secrets-root}，按名注入且无宽松默认。 English summary: the {@code yuheng.llm.*} runtime boundary: the {@code max-request-bytes}/{@code max-frame-bytes} ceilings and the {@code secrets-root} mount, injected by name with no permissive default. */
    @Qualifier(LlmGatewayProperties.BEAN_NAME)
    private final LlmGatewayProperties gatewayProperties;

    /** 中文说明：容器唯一的 Jackson 栈，用于原生文档的读取与写出，禁止另起第二套 JSON 实现。 English summary: the container's single Jackson stack used to read and write the native documents, so no second JSON implementation can appear. */
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：本实现唯一负责的协议是 Chat 面常量，注册表据此把本 bean 归入该键，不符即启动失败。
     * English summary: This implementation owns only the Chat protocol constant, which is how the registry files this bean
     * under that key; a disagreement fails startup.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OpenAiChatProtocolStrategy.protocol()}。
     * @return 返回 {@link LlmProtocolEnum#OPENAI_CHAT}，永不为 null；returns {@link LlmProtocolEnum#OPENAI_CHAT}, never null.
     */
    @Override
    public LlmProtocolEnum protocol() {
        return LlmProtocolEnum.OPENAI_CHAT;
    }

    /**
     * 中文说明：职责①请求编码。在非空 {@code messages}（1–100 项、role 合法、assistant 才可在带 {@code tool_calls} 时
     * content 为 null、tool 项必须带 {@code tool_call_id}）之上核对输出预算（{@code max_tokens} 与
     * {@code max_completion_tokens} 二选一、正整数 ≤8192）、采样区间、{@code n==1}、{@code tools}/{@code tool_choice}/
     * {@code parallel_tool_calls} 形态与 {@code stream_options.include_usage} 仅限流式；自证出网/凭据字段与未声明扩展
     * 一律 400，能力门控字段只在 route 声明能力时透传。最后深拷贝整份文档、<b>仅</b>替换 {@code model} 为
     * {@code route.upstreamModel}，因而 {@code tools}/{@code tool_choice}/{@code parallel_tool_calls}/
     * {@code stream_options} 与其它字段的数值表示、结构与数组顺序逐字保持，并按
     * {@code yuheng.llm.max-request-bytes} 收束整文档。
     * English summary: Duty (1) request encoding. On top of a non-empty {@code messages} array (1–100 entries, legal roles,
     * a null content only for an assistant entry carrying {@code tool_calls}, {@code tool_call_id} mandatory on tool
     * entries) it verifies this face's output budget ({@code max_tokens} versus {@code max_completion_tokens}, exactly one,
     * a positive integer ≤8192), the sampling intervals, {@code n==1}, the shapes of {@code tools}/{@code tool_choice}/
     * {@code parallel_tool_calls} and that {@code stream_options.include_usage} is streaming-only; self-asserted egress or
     * credential fields and undeclared extensions are a 400 while capability-gated fields pass only on a route that
     * declares them. It finally deep-copies the document and replaces {@code model} with {@code route.upstreamModel}
     * <b>only</b>, so {@code tools}/{@code tool_choice}/{@code parallel_tool_calls}/{@code stream_options} and every other
     * field's numeric representation, structure and array order survive verbatim, under
     * {@code yuheng.llm.max-request-bytes} for the whole document.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OpenAiChatProtocolStrategy.encodeRequest(command, route)}，返回体直接作为上游请求正文。
     * @param command 参数 已校验的调用命令，提供 alias、stream 位与原生请求文档；parameter the validated command carrying the alias, the stream flag and the native request document.
     * @param route 参数 已选定的候选 route，其 {@code upstreamModel} 是唯一允许替换 alias 的上游模型名，其 {@code capabilities} 决定扩展字段能否透传；parameter the selected candidate route whose {@code upstreamModel} is the only name allowed to replace the alias and whose {@code capabilities} decide whether an extension may pass through.
     * @return 返回 原生 Chat 请求文档，除 {@code model} 外与客户端提交保持同构；returns the native Chat document, structurally identical to what the client submitted apart from {@code model}.
     * @throws LlmInvocationException 400 {@code unsupported_parameter}（未声明能力、自证出网/凭据字段、字段形态非法、stream 位不一致）、413 {@code request_too_large}、503 {@code model_unavailable}（route 未给出 upstreamModel）；状态即原生状态。
     */
    @Override
    public ObjectNode encodeRequest(LlmInvocationCommandDTO command, LlmModelSnapshotBO.RouteBO route) {
        ObjectNode payload = command == null ? null : command.getPayload();
        if (payload == null) {
            throw refused(MESSAGES_FIELD, "The chat request carries no native document");
        }
        rejectSelfAssertedFields(payload);
        rejectUndeclaredFields(payload, route);
        requireClientAlias(payload, command.getModel());
        boolean streaming = requireStreamAgreement(payload, command.getStream());
        requireMessages(payload);
        requireOutputBudget(payload);
        requireNumericRange(payload, TEMPERATURE_FIELD, MIN_TEMPERATURE, MAX_TEMPERATURE);
        requireNumericRange(payload, TOP_P_FIELD, MIN_TOP_P, MAX_TOP_P);
        requireSingleCandidate(payload);
        requireTools(payload);
        requireToolChoice(payload);
        requireParallelToolCalls(payload);
        requireStreamOptions(payload, streaming);
        String upstreamModel = route == null ? null : StringUtils.trimToNull(route.getUpstreamModel());
        if (upstreamModel == null) {
            throw new LlmInvocationException(503, "model_unavailable", MODEL_FIELD,
                    "The selected route resolves to no upstream chat model", false);
        }
        ObjectNode request = payload.deepCopy();
        request.put(MODEL_FIELD, upstreamModel);
        byte[] requestBytes = toBytes(request);
        requireWithinRequestCeiling(requestBytes);
        log.info("llm chat request encoded alias={} protocol={} channel={} priority={} stream={} messages={} bytes={}",
                command.getModel(), protocol(), route.getChannelKey(), route.getPriority(), streaming,
                payload.path(MESSAGES_FIELD).size(), requestBytes.length);
        return request;
    }

    /**
     * 中文说明：职责②单播解码/透传。整份上游成功文档深拷贝后仅回写 {@code model} 为客户端 alias；
     * {@code choices[].message}（含 {@code tool_calls} 的 id/type/function.name/字符串 arguments）、
     * {@code finish_reason}、{@code usage} 与全部未知但安全字段一律原样保留，不缩减、不重排、不把缺失的
     * {@code usage} 伪造成 0。只有本协议必需结构缺失或变形才是 502 {@code upstream_protocol_error}。
     * English summary: Duty (2) unary decoding and pass-through. The whole native success document is deep-copied and only
     * {@code model} is written back to the client alias; {@code choices[].message} (with each {@code tool_calls} entry's
     * id/type/function.name/string arguments), {@code finish_reason}, {@code usage} and every unknown-but-safe field
     * survive untouched — no reduction, no re-ordering, and an absent {@code usage} is never fabricated as zero. Only a
     * genuinely required structure that is missing or malformed answers with a 502 {@code upstream_protocol_error}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OpenAiChatProtocolStrategy.decodeUnaryResponse(command, upstreamBody)}。
     * @param command 参数 本次调用命令，提供回写给客户端的 alias；parameter this invocation's command, supplying the alias to write back to the client.
     * @param upstreamBody 参数 上游成功响应的原生 JSON 文档；parameter the native JSON document of the successful upstream response.
     * @return 返回 保真后的原生响应文档，{@code model} 已恢复为客户端 alias；returns the fidelity-preserving native response document with {@code model} restored to the client alias.
     * @throws LlmInvocationException 502 {@code upstream_protocol_error}（缺 {@code choices}、choice 非对象、缺 {@code message}、{@code tool_calls}/{@code usage} 变形）；a missing or malformed required structure.
     */
    @Override
    public ObjectNode decodeUnaryResponse(LlmInvocationCommandDTO command, ObjectNode upstreamBody) {
        JsonNode choices = upstreamBody == null ? null : upstreamBody.get(CHOICES_FIELD);
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            rejectDecode(command, CHOICES_FIELD, "a non-empty choices array");
        }
        for (JsonNode choice : choices) {
            if (!choice.isObject()) {
                rejectDecode(command, CHOICES_FIELD, "an object choice");
            }
            JsonNode message = choice.get(MESSAGE_FIELD);
            if (message == null || !message.isObject()) {
                rejectDecode(command, MESSAGE_FIELD, "an assistant message object");
            }
            requireToolCallEntries(message.get(TOOL_CALLS_FIELD), false, false);
            JsonNode finishReason = choice.get(FINISH_REASON_FIELD);
            if (finishReason != null && !finishReason.isNull() && !finishReason.isTextual()) {
                rejectDecode(command, FINISH_REASON_FIELD, "a textual or null finish reason");
            }
        }
        JsonNode usage = upstreamBody.get(USAGE_FIELD);
        if (usage != null && !usage.isNull() && !usage.isObject()) {
            rejectDecode(command, USAGE_FIELD, "an object usage");
        }
        ObjectNode decoded = upstreamBody.deepCopy();
        decoded.put(MODEL_FIELD, command.getModel());
        log.info("llm chat decoded alias={} protocol={} choices={} usagePresent={}", command.getModel(), protocol(),
                choices.size(), usage != null && !usage.isNull());
        return decoded;
    }

    /**
     * 中文说明：职责③流式帧解码与编码。把上游<b>原始字节</b>按 SSE 文法解析成事件帧：帧以空行结束，一行可由
     * {@code \n}、{@code \r\n} 或 {@code \r} 收尾；{@code data:} 只剥掉前缀与其后紧随的一个可选空格，
     * {@code event:}/{@code id:}/{@code retry:} 与 {@code :} 注释行都只是帧的元数据、绝不进内容，未带前缀的非控制行
     * 是上一条 {@code data:} 的续行并以 {@code \n} 拼接；半帧跨缓冲续装，多个整帧合并进同一缓冲时也逐帧吐出，
     * 空行一到即交付。{@code [DONE]} 之前的每个 chunk 必须能解析成本协议对象文档：{@code choices} 可为空数组（{@code include_usage}
     * 的用量帧），但 {@code choices[].delta} 必须是对象、{@code delta.tool_calls[].index} 必须是非负整数、
     * {@code arguments} 若出现必须是字符串片段、{@code finish_reason} 只能是字符串或 null），逐帧把 {@code model}
     * 回写为 alias 后按 Chat 文法重新出帧，终帧 {@code finish_reason} 之后恰好一个 {@code data: [DONE]}：{@code [DONE]}
     * 恒为携它那一批里的最后一帧，其后再出现的载荷一律丢弃并 warn，绝不下发第二份终态。
     * EOF 未见 {@code [DONE]} 即 ABORTED（502 {@code upstream_protocol_error}），<b>绝不</b>补发伪终态帧；流内任何失败
     * 只能以错误终止本流。整条流以 {@code limitRate(1)}+{@code concatMap(...,1)} 严格一帧一取，进入的每个
     * {@link DataBuffer} 在取出字节后恰好释放一次，未交付的帧由 {@code doOnDiscard} 兜底释放，
     * {@code takeUntil} 在 {@code [DONE]} 之后取消上游即取消传播，单帧受 {@code yuheng.llm.max-frame-bytes} 约束。
     * English summary: Duty (3) streaming frame decoding and encoding. The upstream <b>raw bytes</b> are parsed into SSE
     * event frames at the grammar's own boundaries: a frame ends on a blank line, one line may end in {@code \n},
     * {@code \r\n} or {@code \r}, {@code data:} loses only its prefix plus one optional following space,
     * {@code event:}/{@code id:}/{@code retry:} and {@code :} comment lines stay frame metadata and never enter the
     * content, and an unprefixed non-control line continues the previous data line joined by {@code \n}. A half frame
     * resumes across buffers and several whole frames coalesced into one buffer still leave frame by frame as soon as
     * their blank line arrives, so a run never buffers the stream. Every chunk
     * before {@code [DONE]} must parse as a native object document: {@code choices} may be an empty array — the usage frame
     * of {@code include_usage} — but {@code choices[].delta} must be an object, {@code delta.tool_calls[].index} a
     * non-negative integer, {@code arguments} a string fragment when present and {@code finish_reason} only a string or
     * null), each frame's {@code model} is rewritten to the alias and re-emitted in Chat grammar with exactly one
     * {@code data: [DONE]} after the terminal {@code finish_reason}: the marker is always the last frame of the batch that
     * carries it, and any payload after it is dropped with a warning instead of a second terminal. An EOF without
     * {@code [DONE]} is ABORTED (502
     * {@code upstream_protocol_error}) and a fabricated terminal frame is <b>never</b> emitted; any in-stream failure may
     * only end this stream with an error. The whole flux takes strictly one frame at a time through
     * {@code limitRate(1)} plus {@code concatMap(...,1)}, every incoming {@link DataBuffer} is released exactly once after
     * its bytes are taken, undelivered frames fall back onto {@code doOnDiscard}, the {@code takeUntil} after
     * {@code [DONE]} cancels upstream so the cancellation propagates, and each frame stays under
     * {@code yuheng.llm.max-frame-bytes}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OpenAiChatProtocolStrategy.encodeStream(command, upstreamFrames)}，交给
     * {@code LlmServletStreamComponent} 逐帧写出。/ hand the result to {@code LlmServletStreamComponent} which writes it out frame by frame.
     * @param command 参数 本次调用命令，提供逐帧回写 alias 所需的目标名；parameter this invocation's command, supplying the alias each frame must carry.
     * @param upstreamFrames 参数 上游响应体的有界字节流；parameter the bounded byte flux of the upstream response body.
     * @return 返回 同协议客户端字节流，含且仅含一个 {@code [DONE]} 终态帧；returns the same-protocol client byte flux carrying exactly one {@code [DONE]} terminal frame.
     * @throws LlmInvocationException 提交前：502 {@code upstream_protocol_error}、504 {@code upstream_timeout}；提交后只能终止本流，不得改状态。/ pre-commit only: after commit a failure may only end the stream and never change the status.
     */
    @Override
    public Flux<DataBuffer> encodeStream(LlmInvocationCommandDTO command, Flux<DataBuffer> upstreamFrames) {
        int ceiling = gatewayProperties.getMaxFrameBytes();
        String alias = command == null ? null : command.getModel();
        return Flux.defer(() -> {
            FrameBuffer pending = new FrameBuffer(ceiling + FRAME_SEPARATOR.length());
            StreamState state = new StreamState();
            return upstreamFrames
                    .limitRate(PREFETCH_ONE)
                    .concatMap(buffer -> {
                        List<DataBuffer> frames = new ArrayList<>(2);
                        RuntimeException rejected = null;
                        try {
                            byte[] bytes = takeBytes(buffer);
                            for (DataBuffer frame : toClientFrames(command, state, pending, bytes, alias, ceiling)) {
                                frames.add(frame);
                            }
                        } catch (RuntimeException failure) {
                            frames.forEach(DataBufferUtils::release);
                            frames.clear();
                            rejected = failure;
                        } finally {
                            DataBufferUtils.release(buffer);
                        }
                        // 帧内违例只以 error 信号收束，绝不从映射函数里抛出：抛出会让 Reactor 把刚交出的
                        // 上游缓冲再丢弃一次，从而违反「每个 DataBuffer 恰好释放一次」。
                        // A frame violation only travels as an error signal and is never thrown out of the mapper,
                        // because throwing would make Reactor discard the just-handed upstream buffer a second time.
                        return rejected == null ? Flux.fromIterable(frames) : Flux.<DataBuffer>error(rejected);
                    }, PREFETCH_ONE)
                    .takeUntil(frame -> frame == state.terminalFrame)
                    .concatWith(Flux.defer(() -> state.finished
                            ? Flux.<DataBuffer>empty()
                            : Flux.<DataBuffer>error(streamAborted(command, state))))
                    .doOnComplete(() -> log.debug("llm chat stream encoded alias={} protocol={} frames={} terminalChunk={}",
                            alias, protocol(), state.frames, state.terminalChunkSeen))
                    .doOnDiscard(DataBuffer.class, DataBufferUtils::release);
        });
    }

    /**
     * 中文说明：职责④原生错误编码。OpenAI Chat 的错误对象只有一个顶层键 {@code error}，其内层按
     * {@code message}、{@code type}、{@code param}、{@code code} 四键产出：{@code message} 取异常自带的安全说明，
     * {@code type} 由本面按状态映射（400/413/422→invalid_request_error、401→authentication_error、
     * 403→permission_error、404→not_found_error、429→rate_limit_error、5xx→api_error、其余 4xx→
     * invalid_request_error），{@code param} 取异常参数名（缺失即原生 null），{@code code} 取稳定机器码；
     * 绝不使用 admin 的 {@code code}/{@code data} 信封，也绝不把 token、密钥、baseUrl、upstreamModel、prompt 正文或
     * 工具参数写进任何字段。
     * English summary: Duty (4) native error encoding. An OpenAI chat error object has exactly one top-level
     * {@code error} key whose inner object carries {@code message}, {@code type}, {@code param} and {@code code}: the
     * message is the carrier's own safe description, the type is mapped by this face from the status (400/413/422 →
     * invalid_request_error, 401 → authentication_error, 403 → permission_error, 404 → not_found_error, 429 →
     * rate_limit_error, 5xx → api_error, any other 4xx → invalid_request_error), the param is the carrier's parameter
     * name (a native null when absent) and the code is the stable machine code; the admin {@code code}/{@code data}
     * envelope is never used and no token, secret, base URL, upstream model, prompt text or tool argument ever enters a
     * field.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OpenAiChatProtocolStrategy.encodeError(error)}，状态取
     * {@code error.getStatus()}；未认证与越权的原生错误同样由本方法产出。
     * / the status is {@code error.getStatus()}; the native errors for unauthenticated and unauthorized requests come from this same method.
     * @param error 参数 已脱敏的原生错误载体；parameter the masked native error carrier.
     * @return 返回 仅含顶层 {@code error} 的原生错误文档；returns the native error document whose only top-level key is {@code error}.
     */
    @Override
    public ObjectNode encodeError(LlmInvocationException error) {
        ObjectNode envelope = objectMapper.createObjectNode();
        ObjectNode nativeError = objectMapper.createObjectNode();
        nativeError.put("message", error.getMessage());
        nativeError.put("type", nativeErrorType(error.getStatus()));
        if (error.getParam() == null) {
            nativeError.putNull("param");
        } else {
            nativeError.put("param", error.getParam());
        }
        nativeError.put("code", error.getCode());
        envelope.set("error", nativeError);
        return envelope;
    }

    /**
     * 中文说明：唯一编排入口，顺序固定：{@link #encodeRequest}（400/413 在任何出网之前）→ 渠道必须已解析、已启用、
     * 同协议且部署形态为 LOCAL/CLOUD 之一 → 凭据只在 {@code yuheng.llm.secrets-root} 之下按 {@code secretRef}
     * 的规范化路径解析（越界、{@code ..} 穿越、非普通文件、空白即 503，值与引用名都不进日志）→ 就地用 JDK 原生
     * {@link HttpClient}（HTTP/1.1、绝不跟随跳转、connect 预算）发一次 POST，{@code stream} 位决定 Accept 与
     * 请求超时取 header 还是 total 预算 → 非 2xx 关闭上游流并按上游<b>原生状态</b>经 {@link #encodeError} 写回
     * {@code routed}（绝不伪装成空 200、绝不读上游正文）→ 2xx 单播在字节上限内整体解析后 {@link #decodeUnaryResponse}，
     * 2xx 流式先在本方法内读出并验证<b>一个完整合法帧</b>才写回 publisher，这一步就是提交屏障：屏障之后禁止换模型、
     * 禁止重试、禁止改写状态、禁止伪造 {@code [DONE]}，只能终止本流（idle 超时与总预算看门狗都只发错误）。
     * 饱和在提交前表现为 429 {@code rate_limit_exceeded}；本方法不开事务、不加锁，也绝不在事务或锁内等待上游。
     * English summary: The single orchestration entry in a fixed order: {@link #encodeRequest} (so a 400/413 precedes any
     * egress) → the channel must be resolved, enabled, of this protocol and either LOCAL or CLOUD → credentials resolve
     * only as a normalized {@code secretRef} path under {@code yuheng.llm.secrets-root} (an escape, a {@code ..} traversal,
     * a non-regular file or a blank value is a 503 whose detail never reaches a log) → one POST is issued through a
     * locally built JDK-native {@link HttpClient} (HTTP/1.1, never following a redirect, the connect budget), with the
     * stream flag choosing the Accept header and whether the request timeout takes the header or the total budget → a
     * non-2xx closes the upstream stream and leaves through {@link #encodeError} at the upstream's <b>native</b> status
     * into {@code routed} (never an empty 200, never reading the upstream body) → a 2xx unary is taken whole inside the
     * byte ceiling and decoded by {@link #decodeUnaryResponse}, while a 2xx stream must first yield and validate
     * <b>one complete frame</b> inside this method before any publisher is handed back, which <b>is</b> the commit
     * barrier: past it no model switch, retry, status rewrite nor fabricated {@code [DONE]} is possible and only ending
     * the stream is (both the idle timeout and the total-budget watchdog only emit an error). Saturation presents as a
     * pre-commit 429 {@code rate_limit_exceeded}; this method opens no transaction and takes no lock, and never waits on
     * upstream inside one.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OpenAiChatProtocolStrategy.exchange(command, route, routed)}，由
     * {@code LlmApiController} 在 {@code LlmInvocationService.invoke(command)} 之后调用。
     * @param command 参数 已校验的调用命令；parameter the validated invocation command.
     * @param route 参数 已被路由策略放行的首条候选 route；parameter the head candidate route the routing strategy already admitted.
     * @param routed 参数 路由阶段产出的原生结果，携带安全响应头，本方法就地补全状态与 body；parameter the routed native result carrying the safe headers, whose status and body this method completes in place.
     * @return 返回 已带原生状态、安全头与有界 body 的原生结果（单播为解码文档，流式为同协议帧流，上游失败为原生错误文档）；returns the native result with status, safe headers and a bounded body (the decoded document unary, the same-protocol frame flux when streaming, the native error document on an upstream failure).
     * @throws LlmInvocationException 提交前的本地与上游失败：400/413（请求面）、429 {@code rate_limit_exceeded}（饱和）、502 {@code upstream_protocol_error}（文档越界或帧非法）、503 {@code model_unavailable}（渠道或凭据不可用、传输失败）、504 {@code upstream_timeout}；状态即协议原生状态。
     */
    @Override
    public LlmInvocationResultVO exchange(LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.RouteBO route,
            LlmInvocationResultVO routed) {
        ObjectNode request = encodeRequest(command, route);
        byte[] requestBody = toBytes(request);
        LlmModelSnapshotBO.ChannelBO channel = requireUsableChannel(command, route);
        URI endpoint = egressEndpoint(channel);
        String credential = resolveCredential(command, channel);
        boolean streaming = Boolean.TRUE.equals(command.getStream());
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofMillis(channel.getConnectTimeoutMs().longValue()))
                .build();
        log.info("llm chat egress alias={} protocol={} channel={} priority={} deployment={} stream={} bytes={}",
                command.getModel(), protocol(), channel.getChannelKey(), route.getPriority(),
                channel.getDeployment(), streaming, requestBody.length);
        HttpRequest.Builder upstreamRequestBuilder = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofMillis(streaming
                        ? channel.getHeaderTimeoutMs().longValue() : responseBudgetMs(channel)))
                .header("Content-Type", "application/json")
                .header("Accept", streaming ? "text/event-stream" : "application/json");
        if (credential != null) {
            upstreamRequestBuilder.header("Authorization", "Bearer " + credential);
        }
        HttpRequest upstreamRequest = upstreamRequestBuilder
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();
        try {
            HttpResponse<InputStream> response =
                    client.send(upstreamRequest, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            InputStream body = response.body();
            if (status < 200 || status > 299) {
                closeQuietly(body);
                return handUpstreamError(routed, channel, status);
            }
            if (streaming) {
                Flux<DataBuffer> committed = commitStream(command, channel, body);
                routed.setStatus(status);
                routed.setBody(committed);
                return routed;
            }
            try (InputStream upstreamBody = body) {
                ObjectNode decoded = decodeUnaryResponse(command, successDocument(response, upstreamBody, command));
                byte[] responseBody = toBytes(decoded);
                requireWithinResponseCeiling(responseBody);
                routed.setStatus(status);
                routed.setBody(Flux.just(BUFFER_FACTORY.wrap(responseBody)));
                return routed;
            }
        } catch (HttpTimeoutException timedOut) {
            log.warn("llm chat upstream timed out alias={} protocol={} channel={} stream={}",
                    command.getModel(), protocol(), channel.getChannelKey(), streaming);
            throw new LlmInvocationException(504, "upstream_timeout", null,
                    "The chat channel did not answer within its timeout budget", false);
        } catch (TaskRejectedException saturated) {
            throw refusedOnSaturation(command, channel, saturated);
        } catch (RejectedExecutionException saturated) {
            throw refusedOnSaturation(command, channel, saturated);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            log.warn("llm chat aborted alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            throw new LlmInvocationException(503, "model_unavailable", null,
                    "The chat call was aborted before its first frame", false);
        } catch (IOException transportFailure) {
            log.warn("llm chat upstream transport failed alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            throw new LlmInvocationException(503, "model_unavailable", null,
                    "The chat channel is unreachable", true);
        }
    }

    // ------------------------------------------------------------------ streaming face

    /**
     * 中文说明：提交屏障的流式实现：在写回 publisher 之前，先从上游字节流里读出并验证<b>一个完整合法帧</b>
     * （probe 的残帧缓冲不超过单帧上限），验证不过就地关闭流并按原生状态抛出（502/504，仍在提交前）；
     * 通过后把已读字节原样前缀在剩余字节流之前交给 {@link #encodeStream}，并附加每帧 idle 预算、总预算
     * deadline 与取消时的上游关闭。本方法绝不在事务或锁内等待。
     * English summary: The streaming side of the commit barrier: before any publisher is handed back, <b>one complete
     * legal frame</b> is read and validated out of the upstream byte stream (the probe's residual buffer never exceeds the
     * per-frame ceiling); a failure closes the stream and throws natively (502/504, still pre-commit). On success the bytes
     * already read are prefixed to the remaining byte flux and handed to {@link #encodeStream}, plus a per-frame idle
     * budget, the total-budget deadline and an upstream close on cancellation. This method never waits inside a
     * transaction or a lock.
     */
    private Flux<DataBuffer> commitStream(LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.ChannelBO channel, InputStream body) {
        int ceiling = gatewayProperties.getMaxFrameBytes();
        long deadlineNanos = System.nanoTime() + Duration.ofMillis(channel.getTotalTimeoutMs().longValue()).toNanos();
        Duration idle = Duration.ofMillis(Math.max(1L,
                Math.min(channel.getIdleTimeoutMs().longValue(), channel.getHeaderTimeoutMs().longValue())));
        List<byte[]> prelude = new ArrayList<>(2);
        FrameBuffer probe = new FrameBuffer(ceiling + FRAME_SEPARATOR.length());
        boolean validated = false;
        try {
            while (!validated) {
                if (System.nanoTime() >= deadlineNanos) {
                    throw upstreamTimeout(command, channel);
                }
                byte[] chunk = readChunk(body);
                if (chunk == null) {
                    break;
                }
                prelude.add(chunk);
                for (String payload : probe.feed(chunk)) {
                    if (payload == null) {
                        continue;
                    }
                    if (!DONE_MARKER.equals(payload)) {
                        parseChunk(command, payload, command.getModel());
                    }
                    validated = true;
                    break;
                }
            }
            if (!validated) {
                throw new LlmInvocationException(502, "upstream_protocol_error", CHOICES_FIELD,
                        "The upstream stream offered no complete valid chat frame", false);
            }
        } catch (RuntimeException | IOException preCommitFailure) {
            closeQuietly(body);
            if (preCommitFailure instanceof LlmInvocationException nativeFailure) {
                throw nativeFailure;
            }
            log.warn("llm chat stream pre-commit read failed alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            throw new LlmInvocationException(503, "model_unavailable", null,
                    "The chat stream could not be read before its first frame", true);
        }
        return encodeStream(command, Flux.concat(
                        Flux.fromIterable(prelude).map(BUFFER_FACTORY::wrap),
                        chunks(body, command, channel, deadlineNanos)))
                .limitRate(PREFETCH_ONE)
                .timeout(idle)
                .onErrorMap(failure -> failure instanceof LlmInvocationException nativeFailure
                        ? nativeFailure : streamIdleExpired(command, channel, failure))
                .doOnCancel(() -> log.info("llm chat stream cancelled alias={} protocol={} channel={}",
                        command.getModel(), protocol(), channel.getChannelKey()))
                .doFinally(signal -> closeQuietly(body));
    }

    /** 中文说明：把上游剩余字节包成「每次请求一块」的冷字节流：{@link Flux#generate} 天然一元素一请求，因此预取恒为 1；每块之前核对总预算（到点即 504），EOF 正常收束，取消即停止读取并由 {@link #commitStream} 的 doFinally 关闭流。 English summary: wraps the remaining upstream bytes into a cold byte flux of one chunk per request: {@link Flux#generate} requests exactly one element at a time so prefetch stays one, the total budget is checked before each chunk (expiry is a 504), EOF completes normally and a cancellation stops reading while {@link #commitStream}'s doFinally closes the stream. */
    private Flux<DataBuffer> chunks(InputStream body, LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.ChannelBO channel, long deadlineNanos) {
        return Flux.<DataBuffer, byte[]>generate(() -> new byte[READ_CHUNK_BYTES], (pool, sink) -> {
            if (System.nanoTime() >= deadlineNanos) {
                sink.error(upstreamTimeout(command, channel));
                return pool;
            }
            try {
                int read = body.read(pool);
                if (read < 0) {
                    sink.complete();
                } else if (read > 0) {
                    sink.next(BUFFER_FACTORY.wrap(Arrays.copyOf(pool, read)));
                }
            } catch (IOException failure) {
                sink.error(new LlmInvocationException(503, "model_unavailable", null,
                        "The chat stream could not be read", true));
            }
            return pool;
        }).limitRate(PREFETCH_ONE);
    }

    /** 中文说明：把已扫出的帧载荷编成客户端字节：{@code [DONE]} 只放行一次并且<b>恒为本批最后一帧</b>（同一块里
     * 终态标记之后的载荷不再下发，只记一条低基数告警），chunk 逐帧验证并回写 alias，产出的单帧序列化后仍受
     * {@code yuheng.llm.max-frame-bytes} 约束；因此「一帧一缓冲」「一帧跨多缓冲」「多帧挤在一缓冲」三种上游
     * 形态下，客户端都按原序收到同样的字节，且终态之后绝无第二份 {@code [DONE]}。
     * English summary: turns scanned frame payloads into client bytes: {@code [DONE]} passes exactly once and is always
     * the LAST frame of its batch (payloads behind the terminal marker inside the same block stop travelling, with only
     * one low-cardinality warning logged), each chunk is validated with the alias written back and a produced frame still
     * obeys {@code yuheng.llm.max-frame-bytes}; so whether the upstream hands over one frame per buffer, one frame across
     * buffers or several frames in one buffer, the client sees the same bytes in the same order and never a second
     * {@code [DONE]} after the terminal state. */
    private List<DataBuffer> toClientFrames(LlmInvocationCommandDTO command, StreamState state, FrameBuffer pending,
            byte[] bytes, String alias, int ceiling) {
        List<DataBuffer> frames = new ArrayList<>(2);
        try {
            List<String> payloads = pending.feed(bytes);
            int trailingAfterTerminal = 0;
            for (int index = 0; index < payloads.size(); index++) {
                String payload = payloads.get(index);
                if (payload == null) {
                    continue;
                }
                if (state.finished) {
                    trailingAfterTerminal = payloads.size() - index;
                    break;
                }
                if (DONE_MARKER.equals(payload)) {
                    DataBuffer terminal = BUFFER_FACTORY.wrap(DONE_FRAME.getBytes(StandardCharsets.UTF_8));
                    state.finished = true;
                    state.terminalFrame = terminal;
                    frames.add(terminal);
                    trailingAfterTerminal = payloads.size() - index - 1;
                    break;
                }
                ObjectNode chunk = parseChunk(command, payload, alias);
                if (hasTerminalFinishReason(chunk)) {
                    state.terminalChunkSeen = true;
                }
                byte[] encoded = toBytes(chunk);
                if (encoded.length > ceiling) {
                    throw new LlmInvocationException(502, "upstream_protocol_error", CHOICES_FIELD,
                            "A chat stream frame exceeds the configured frame ceiling", false);
                }
                state.frames++;
                frames.add(BUFFER_FACTORY.wrap(withFrameFraming(encoded)));
            }
            if (trailingAfterTerminal > 0) {
                log.warn("llm chat stream stopped at its terminal marker alias={} protocol={} ignoredPayloads={}",
                        alias, protocol(), trailingAfterTerminal);
            }
        } catch (RuntimeException failure) {
            frames.forEach(DataBufferUtils::release);
            throw failure;
        }
        return frames;
    }

    /** 中文说明：解析并验证一个 {@code data:} chunk：必须是原生 JSON 对象、{@code choices} 必须是数组（用量帧可为空数组，此时必须有 {@code usage}），choice 的 {@code delta} 必须是对象、{@code delta.tool_calls[].index} 必须是非负整数且同帧内不重复、{@code arguments} 若出现必须是字符串片段、{@code finish_reason} 只能是字符串或 null；未知但安全字段原样保留，仅 {@code model} 回写为 alias。 English summary: parses and validates one {@code data:} chunk: a native JSON object whose {@code choices} is an array (empty is legal for a usage-only frame, which must then carry {@code usage}), each choice's {@code delta} an object, {@code delta.tool_calls[].index} a non-negative integer unique within the frame, {@code arguments} a string fragment when present and {@code finish_reason} only a string or null; unknown-but-safe fields survive and only {@code model} is written back to the alias. */
    private ObjectNode parseChunk(LlmInvocationCommandDTO command, String payload, String alias) {
        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(payload);
        } catch (IOException unparsable) {
            log.warn("llm chat stream frame unparsable alias={} protocol={}",
                    command == null ? null : command.getModel(), protocol());
            throw new LlmInvocationException(502, "upstream_protocol_error", CHOICES_FIELD,
                    "A chat stream frame cannot be parsed", false);
        }
        if (!(parsed instanceof ObjectNode chunk)) {
            throw new LlmInvocationException(502, "upstream_protocol_error", CHOICES_FIELD,
                    "A chat stream frame is not a native JSON object", false);
        }
        JsonNode choices = chunk.get(CHOICES_FIELD);
        if (choices == null || !choices.isArray()) {
            throw new LlmInvocationException(502, "upstream_protocol_error", CHOICES_FIELD,
                    "A chat stream frame carries no choices array", false);
        }
        if (choices.isEmpty() && chunk.get(USAGE_FIELD) == null) {
            throw new LlmInvocationException(502, "upstream_protocol_error", USAGE_FIELD,
                    "A chat frame without choices must still report usage", false);
        }
        Set<Integer> seenIndexes = new HashSet<>();
        for (JsonNode choice : choices) {
            if (!choice.isObject()) {
                throw streamFrameInvalid(command, CHOICES_FIELD);
            }
            JsonNode delta = choice.get(DELTA_FIELD);
            if (delta != null && !delta.isObject()) {
                throw streamFrameInvalid(command, DELTA_FIELD);
            }
            JsonNode finishReason = choice.get(FINISH_REASON_FIELD);
            if (finishReason != null && !finishReason.isNull() && !finishReason.isTextual()) {
                throw streamFrameInvalid(command, FINISH_REASON_FIELD);
            }
            if (delta == null) {
                continue;
            }
            JsonNode toolCalls = delta.get(TOOL_CALLS_FIELD);
            requireToolCallEntries(toolCalls, true, false);
            if (toolCalls != null && toolCalls.isArray()) {
                for (JsonNode call : toolCalls) {
                    JsonNode index = call.get(INDEX_FIELD);
                    if (index.isIntegralNumber() && index.canConvertToInt() && !seenIndexes.add(index.intValue())) {
                        throw streamFrameInvalid(command, TOOL_CALLS_FIELD);
                    }
                }
            }
        }
        JsonNode usage = chunk.get(USAGE_FIELD);
        if (usage != null && !usage.isNull() && !usage.isObject()) {
            throw streamFrameInvalid(command, USAGE_FIELD);
        }
        if (StringUtils.isNotBlank(alias)) {
            chunk.put(MODEL_FIELD, alias);
        }
        return chunk;
    }

    private byte[] withFrameFraming(byte[] documentBytes) {
        byte[] prefix = (DATA_PREFIX + " ").getBytes(StandardCharsets.UTF_8);
        byte[] frame = new byte[prefix.length + documentBytes.length + FRAME_SEPARATOR.length()];
        System.arraycopy(prefix, 0, frame, 0, prefix.length);
        System.arraycopy(documentBytes, 0, frame, prefix.length, documentBytes.length);
        System.arraycopy(FRAME_SEPARATOR.getBytes(StandardCharsets.UTF_8), 0,
                frame, prefix.length + documentBytes.length, FRAME_SEPARATOR.length());
        return frame;
    }

    private boolean hasTerminalFinishReason(ObjectNode chunk) {
        JsonNode choices = chunk.get(CHOICES_FIELD);
        if (choices == null || !choices.isArray()) {
            return false;
        }
        for (JsonNode choice : choices) {
            JsonNode reason = choice.get(FINISH_REASON_FIELD);
            if (reason != null && reason.isTextual()) {
                return true;
            }
        }
        return false;
    }

    private LlmInvocationException streamFrameInvalid(LlmInvocationCommandDTO command, String parameter) {
        log.warn("llm chat stream frame rejected alias={} protocol={} parameter={}",
                command == null ? null : command.getModel(), protocol(), parameter);
        return new LlmInvocationException(502, "upstream_protocol_error", parameter,
                "A chat stream frame violates the native chat completions contract", false);
    }

    /** 中文说明：EOF 缺少 {@code [DONE]} 的收束：判为 ABORTED 并只发错误，日志记帧数与是否见过终态 chunk，绝不补发伪终态帧。 English summary: the close-out of an EOF without {@code [DONE]}: judged ABORTED with only an error, logging the frame count and whether a terminal chunk was ever seen, and never emitting a fabricated terminal frame. */
    private LlmInvocationException streamAborted(LlmInvocationCommandDTO command, StreamState state) {
        log.warn("llm chat stream aborted alias={} protocol={} frames={} terminalChunk={} doneSeen=false",
                command == null ? null : command.getModel(), protocol(), state.frames, state.terminalChunkSeen);
        return new LlmInvocationException(502, "upstream_protocol_error", CHOICES_FIELD,
                "The upstream chat stream ended before its terminal marker", false);
    }

    private LlmInvocationException streamIdleExpired(LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.ChannelBO channel, Throwable failure) {
        if (failure instanceof TimeoutException) {
            log.warn("llm chat stream idle expired alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            return upstreamTimeout(command, channel);
        }
        log.warn("llm chat stream failed alias={} protocol={} channel={}",
                command.getModel(), protocol(), channel.getChannelKey());
        return new LlmInvocationException(503, "model_unavailable", null,
                "The chat stream failed and was terminated", true);
    }

    private LlmInvocationException upstreamTimeout(LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.ChannelBO channel) {
        log.warn("llm chat stream deadline expired alias={} protocol={} channel={}",
                command.getModel(), protocol(), channel.getChannelKey());
        return new LlmInvocationException(504, "upstream_timeout", null,
                "The chat channel exceeded its timeout budget", false);
    }

    /** 中文说明：读一块上游字节：EOF 返回 null，零字节读回空数组由调用方继续轮询总预算，IOException 交给提交屏障判定。 English summary: reads one upstream chunk: EOF yields null, a zero-byte read yields an empty array so the caller keeps polling the total budget, and an IOException is judged by the commit barrier. */
    private byte[] readChunk(InputStream body) throws IOException {
        byte[] pool = new byte[READ_CHUNK_BYTES];
        int read = body.read(pool);
        if (read < 0) {
            return null;
        }
        return read == 0 ? new byte[0] : Arrays.copyOf(pool, read);
    }

    private static void closeQuietly(InputStream stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException ignored) {
            // 关闭失败不改变已判定的结果 / a failed close never changes the outcome already decided
        }
    }

    /** 中文说明：取出进入缓冲的字节（不释放），交由调用方在 finally 里恰好释放一次。 English summary: takes the bytes of an incoming buffer without releasing it, leaving exactly one release to the caller. */
    private static byte[] takeBytes(DataBuffer buffer) {
        byte[] bytes = new byte[buffer.readableByteCount()];
        buffer.read(bytes);
        return bytes;
    }

    // ------------------------------------------------------------------ channel, credential and upstream plumbing

    /** 中文说明：饱和即提交前 429：不排队、不重试、不改投任何渠道，日志只到渠道 key 为止。 English summary: saturation is a pre-commit 429: no queueing, no retry and no re-routing, with the log stopping at the channel key. */
    private LlmInvocationException refusedOnSaturation(LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.ChannelBO channel, RuntimeException saturated) {
        log.warn("llm chat refused pre-commit on saturation alias={} protocol={} channel={}",
                command.getModel(), protocol(), channel.getChannelKey());
        return new LlmInvocationException(429, "rate_limit_exceeded", null,
                "The chat channel is saturated; retry this request later", true);
    }

    /** 中文说明：出网前的渠道自检：渠道必须已解析、已启用、协议为本面且部署形态是 LOCAL 或 CLOUD（Chat 两面皆合法，与 Embeddings 的强制 LOCAL 不同）；任何不符只拒绝本次尝试，绝不改投别的协议或渠道。 English summary: the pre-egress channel self-check: a channel must be resolved, enabled, speak this face's protocol and be either LOCAL or CLOUD (both are legal for Chat, unlike the forced LOCAL of embeddings); any disagreement rejects only this attempt and never re-routes to another protocol or channel. */
    private LlmModelSnapshotBO.ChannelBO requireUsableChannel(LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.RouteBO route) {
        LlmModelSnapshotBO.ChannelBO channel = route == null ? null : route.getChannel();
        String reason = null;
        if (!gatewayProperties.isEnabled()) {
            reason = "engine egress disabled";
        } else if (channel == null || !Boolean.TRUE.equals(channel.getEnabled())) {
            reason = "channel unresolved or disabled";
        } else if (channel.getProtocol() != protocol() || command.getProtocol() != protocol()) {
            reason = "channel protocol is not the ingress protocol";
        } else if (channel.getDeployment() == null) {
            reason = "channel deployment unresolved";
        } else if (channel.getDeployment() != LlmDeploymentEnum.LOCAL
                && channel.getDeployment() != LlmDeploymentEnum.CLOUD) {
            reason = "channel deployment is not egressable";
        } else if (StringUtils.isBlank(channel.getBaseUrl())) {
            reason = "channel base url missing";
        } else if (route.getCapabilities() == null
                || !route.getCapabilities().containsAll(command.getRequiredCapabilities())) {
            reason = "route capabilities do not cover the request";
        }
        if (reason != null) {
            log.warn("llm chat channel rejected alias={} protocol={} channel={} reason={}",
                    command.getModel(), protocol(), route == null ? null : route.getChannelKey(), reason);
            throw new LlmInvocationException(503, "model_unavailable", null,
                    "The selected chat route resolves to no usable channel", false);
        }
        return channel;
    }

    /** 中文说明：把管理员配置的 baseUrl 与本协议固定路径段拼成出网 URI：必须绝对 http(s)、无 userinfo/query/fragment；解析失败只回 503，绝不回显原始地址。 English summary: joins the administrator-configured base URL with this protocol's fixed path segments: absolute http(s) without userinfo, query or fragment; a parse failure answers with a 503 that never echoes the original address. */
    private URI egressEndpoint(LlmModelSnapshotBO.ChannelBO channel) {
        try {
            URI base = URI.create(StringUtils.trimToEmpty(channel.getBaseUrl()));
            boolean schemeAllowed = "https".equalsIgnoreCase(base.getScheme())
                    || channel.getDeployment() == LlmDeploymentEnum.LOCAL
                    && "http".equalsIgnoreCase(base.getScheme());
            if (!base.isAbsolute() || !schemeAllowed || StringUtils.isBlank(base.getHost())
                    || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null) {
                throw new IllegalArgumentException("the configured chat channel base url is not egressable");
            }
            String root = StringUtils.removeEnd(StringUtils.trimToEmpty(base.getRawPath()), "/");
            String path = root + "/" + String.join("/", CHAT_PATH_SEGMENTS);
            return new URI(base.getScheme(), null, base.getHost(), base.getPort(), path, null, null);
        } catch (URISyntaxException | IllegalArgumentException rejected) {
            log.warn("llm chat channel base url unparsable channel={}", channel.getChannelKey());
            throw new LlmInvocationException(503, "model_unavailable", null,
                    "The configured chat channel address cannot be used for egress", false);
        }
    }

    /** 中文说明：有 secretRef 时只解析 {@code yuheng.llm.secrets-root} 下的规范化文件；仅无认证 LOCAL 可省略引用并不发送上游凭据头，CLOUD 缺失引用仍失败关闭。 English summary: a configured secretRef resolves only to a normalized file below {@code yuheng.llm.secrets-root}; only auth-free LOCAL may omit it and send no upstream credential header, while CLOUD still fails closed without one. */
    private String resolveCredential(LlmInvocationCommandDTO command, LlmModelSnapshotBO.ChannelBO channel) {
        String configuredReference = channel.getSecretRef();
        if (configuredReference == null && channel.getDeployment() == LlmDeploymentEnum.LOCAL) {
            return null;
        }
        String reference = StringUtils.trimToNull(configuredReference);
        String root = StringUtils.trimToNull(gatewayProperties.getSecretsRoot());
        if (root == null || reference == null || reference.contains("..")) {
            throw unresolvableCredential(command, channel);
        }
        Path rootPath;
        Path target;
        try {
            if (Path.of(reference).isAbsolute()) {
                throw unresolvableCredential(command, channel);
            }
            rootPath = Path.of(root).toAbsolutePath().normalize();
            target = rootPath.resolve(reference).normalize();
        } catch (IllegalArgumentException rejected) {
            throw unresolvableCredential(command, channel);
        }
        if (!target.startsWith(rootPath) || target.equals(rootPath) || !Files.isRegularFile(target)) {
            throw unresolvableCredential(command, channel);
        }
        String secret;
        try {
            secret = Files.readString(target, StandardCharsets.UTF_8).trim();
        } catch (IOException unreadable) {
            log.warn("llm chat secret unreadable alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            throw unresolvableCredential(command, channel);
        }
        if (secret.isEmpty() || secret.length() > MAX_SECRET_CHARACTERS) {
            log.warn("llm chat secret unusable alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            throw unresolvableCredential(command, channel);
        }
        return secret;
    }

    private LlmInvocationException unresolvableCredential(LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.ChannelBO channel) {
        log.warn("llm chat credential unresolvable alias={} protocol={} channel={}",
                command.getModel(), protocol(), channel.getChannelKey());
        return new LlmInvocationException(503, "model_unavailable", null,
                "The managed credential of the selected chat channel is unavailable", false);
    }

    /** 中文说明：把 2xx 上游响应读成原生对象文档：先按 Content-Length 预检，再以 {@code readNBytes} 至多读到 {@code yuheng.llm.max-request-bytes}+1 字节，因此一个撒谎的或失控的上游服务也无法把引擎撑爆；空文档、越界文档与非对象文档都是 502，绝不退化为空成功。单播 JSON 文档必须整体解析，这与「不得把整条流缓存成 byte[]」的流式约束无关（本路径不是流）。 English summary: reads a 2xx upstream response into the native object document: Content-Length is pre-checked and the body is then read with {@code readNBytes} up to {@code yuheng.llm.max-request-bytes}+1 bytes, so neither a lying nor a runaway upstream can inflate the engine; an empty, oversized or non-object document is a 502 rather than a degraded empty success. A unary JSON document has to be parsed whole, which is unrelated to the "never buffer a whole stream into byte[]" rule that governs the streaming path (this path is not a stream). */
    private ObjectNode successDocument(HttpResponse<InputStream> response, InputStream upstreamBody,
            LlmInvocationCommandDTO command) {
        long ceiling = gatewayProperties.getMaxRequestBytes();
        if (ceiling < 1 || response.headers().firstValueAsLong("Content-Length").orElse(-1) > ceiling) {
            throw new LlmInvocationException(502, "upstream_protocol_error", CHOICES_FIELD,
                    "The chat response document exceeds the configured byte ceiling", false);
        }
        byte[] body;
        try {
            body = upstreamBody.readNBytes((int) Math.min(ceiling, Integer.MAX_VALUE - 1L) + 1);
        } catch (IOException unreadable) {
            log.warn("llm chat response body unreadable alias={} protocol={}", command.getModel(), protocol());
            throw new LlmInvocationException(502, "upstream_protocol_error", CHOICES_FIELD,
                    "The chat response document could not be read", false);
        }
        if (body.length == 0 || body.length > ceiling) {
            throw new LlmInvocationException(502, "upstream_protocol_error", CHOICES_FIELD,
                    "The chat response document is empty or exceeds the configured byte ceiling", false);
        }
        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(body);
        } catch (IOException unparsable) {
            log.warn("llm chat response document unparsable alias={} protocol={}", command.getModel(), protocol());
            throw new LlmInvocationException(502, "upstream_protocol_error", CHOICES_FIELD,
                    "The chat response document cannot be parsed", false);
        }
        if (!(parsed instanceof ObjectNode document)) {
            throw new LlmInvocationException(502, "upstream_protocol_error", CHOICES_FIELD,
                    "The chat response is not a native JSON object", false);
        }
        return document;
    }

    /** 中文说明：上游非 2xx 的原生收束：状态逐字保留、body 由 {@link #encodeError} 按本协议形状产出，上游正文一个字节都不回显；这是「上游失败」与「空成功」的分界点。 English summary: the native close-out of a non-2xx upstream: the status is kept verbatim and the body comes from {@link #encodeError} in this protocol's shape without echoing a single upstream byte; this is the boundary between an upstream failure and an empty success. */
    private LlmInvocationResultVO handUpstreamError(LlmInvocationResultVO routed,
            LlmModelSnapshotBO.ChannelBO channel, int status) {
        log.warn("llm chat upstream rejected protocol={} channel={} status={}",
                protocol(), channel.getChannelKey(), status);
        LlmInvocationException error = new LlmInvocationException(status, codeFor(status), null,
                descriptionFor(status), status == 429 || status >= 500);
        routed.setStatus(status);
        routed.setBody(Flux.just(BUFFER_FACTORY.wrap(toBytes(encodeError(error)))));
        return routed;
    }

    /** 中文说明：稳定机器码只取 Spec §9.2.2 错误表给定的字面量，按上游原生状态映射，绝不即兴发明。 English summary: stable machine codes are the literals of the Spec §9.2.2 error table, mapped from the upstream's native status and never improvised. */
    private String codeFor(int status) {
        return switch (status) {
            case 400, 404, 406, 409, 413, 422 -> "unsupported_parameter";
            case 401 -> "authentication_error";
            case 403 -> "model_forbidden";
            case 429 -> "rate_limit_exceeded";
            case 503 -> "model_unavailable";
            case 504 -> "upstream_timeout";
            default -> "upstream_protocol_error";
        };
    }

    /** 中文说明：安全说明按状态族给固定文本，绝不拼接上游原文、地址或凭据。 English summary: descriptions are fixed text per status family, never concatenating upstream text, addresses or credentials. */
    private String descriptionFor(int status) {
        if (status == 429) {
            return "The chat channel is rate limited";
        }
        if (status == 504) {
            return "The chat channel did not answer within its timeout budget";
        }
        if (status >= 500) {
            return "The chat channel failed";
        }
        return "The chat channel rejected the request";
    }

    /** 中文说明：原生错误对象的 {@code type} 由本面按状态自行映射（OpenAI 语义），与 {@code code} 互补而不泄漏细节。 English summary: this face maps the native error object's {@code type} from the status itself (OpenAI semantics), complementing {@code code} without leaking detail. */
    private String nativeErrorType(int status) {
        return switch (status) {
            case 401 -> "authentication_error";
            case 403 -> "permission_error";
            case 404 -> "not_found_error";
            case 429 -> "rate_limit_error";
            case 400, 413, 422 -> "invalid_request_error";
            default -> "api_error";
        };
    }

    // ------------------------------------------------------------------ request-face field checks

    /** 中文说明：请求面字段/能力问题的固定拒绝载体：param 只放安全的协议参数名。 English summary: the fixed refusal carrier for request-face field or capability failures: param carries only a safe protocol parameter name. */
    private LlmInvocationException refused(String parameter, String description) {
        return new LlmInvocationException(400, "unsupported_parameter", parameter, description, false);
    }

    /** 中文说明：拒绝任何自证出网或凭据字段：这类事实只存在于服务端受管配置里，且只有形态安全的字段名才可回显。 English summary: refuses any self-asserted egress or credential field: such facts live only in server-side managed configuration, and only a well-shaped field name may ever be echoed. */
    private void rejectSelfAssertedFields(ObjectNode payload) {
        for (Iterator<String> names = payload.fieldNames(); names.hasNext(); ) {
            String name = names.next();
            if (SELF_ASSERTED_FIELDS.contains(name.toLowerCase())) {
                log.warn("llm chat request asserted a managed field protocol={}", protocol());
                throw refused(safeParameterName(name),
                        "The chat request asserts a server-managed egress or credential field");
            }
        }
    }

    /** 中文说明：未声明扩展一律 400：能力门控字段要求 route 显式声明对应能力（未声明即拒绝，绝不静默降级为纯文本），其余未知键同样拒绝。 English summary: an undeclared extension is always a 400: a capability-gated field demands the route's explicit declaration (without it the field is refused rather than silently degraded to plain text) and every other unknown key is refused too. */
    private void rejectUndeclaredFields(ObjectNode payload, LlmModelSnapshotBO.RouteBO route) {
        Set<LlmCapabilityEnum> declared = route == null || route.getCapabilities() == null
                ? Set.of() : route.getCapabilities();
        for (Iterator<String> names = payload.fieldNames(); names.hasNext(); ) {
            String name = names.next();
            if (DECLARED_REQUEST_FIELDS.contains(name)) {
                continue;
            }
            LlmCapabilityEnum required = CAPABILITY_GATED_FIELDS.get(name);
            if (required != null && declared.contains(required)) {
                continue;
            }
            throw refused(safeParameterName(name),
                    "The chat request declares a parameter or capability this route does not support");
        }
    }

    /** 中文说明：{@code model} 必须与命令里的客户端 alias 全等，防止一份命令打两个模型。 English summary: {@code model} must equal the command's client alias so one command can never hit two models. */
    private void requireClientAlias(ObjectNode payload, String alias) {
        JsonNode model = payload.get(MODEL_FIELD);
        if (model == null || !model.isTextual() || StringUtils.isBlank(model.asText())
                || !StringUtils.equals(StringUtils.trim(model.asText()), alias)) {
            throw refused(MODEL_FIELD, "The chat request model must be the requested client alias");
        }
    }

    /** 中文说明：stream 位必须与命令一致（命令是唯一事实来源），返回该位以供后续校验。 English summary: the document's stream flag must agree with the command's, which is the single source of truth; the flag is returned for the later checks. */
    private boolean requireStreamAgreement(ObjectNode payload, Boolean streamFlag) {
        boolean streaming = Boolean.TRUE.equals(streamFlag);
        JsonNode stream = payload.get(STREAM_FIELD);
        if (stream != null && !stream.isNull() && !stream.isBoolean()) {
            throw refused(STREAM_FIELD, "The chat stream parameter must be a boolean");
        }
        boolean documented = stream != null && stream.booleanValue();
        if (documented != streaming) {
            throw refused(STREAM_FIELD, "The chat stream parameter disagrees with the requested stream mode");
        }
        return streaming;
    }

    /** 中文说明：{@code messages} 核对：1–100 项、每项是对象、role 合法、content 为字符串/多模态数组/（仅带 tool_calls 的 assistant）null、tool 项必须带非空 {@code tool_call_id}、assistant 的 {@code tool_calls} 逐条按 Chat 形状校验（id/type/function.name/字符串 arguments），字符串内容合计受单消息与整请求上限约束。 English summary: the {@code messages} check: 1–100 entries, each an object with a legal role, a content that is a string, a multimodal array or (only for an assistant entry carrying {@code tool_calls}) null, a non-empty {@code tool_call_id} on tool entries, assistant {@code tool_calls} validated entry by entry in Chat shape (id/type/function.name/string arguments), with string content bounded per message and per request. */
    private void requireMessages(ObjectNode payload) {
        JsonNode messages = payload.get(MESSAGES_FIELD);
        if (messages == null || !messages.isArray() || messages.isEmpty() || messages.size() > MAX_MESSAGES) {
            throw refused(MESSAGES_FIELD, "The chat request needs 1 to 100 messages");
        }
        for (JsonNode message : messages) {
            if (!message.isObject()) {
                throw refused(MESSAGES_FIELD, "Every chat message must be an object");
            }
            JsonNode role = message.get(ROLE_FIELD);
            if (role == null || !role.isTextual() || !MESSAGE_ROLES.contains(role.asText())) {
                throw refused(ROLE_FIELD, "An unsupported message role was submitted");
            }
            String value = role.asText();
            JsonNode content = message.get(CONTENT_FIELD);
            boolean absent = content == null || content.isNull();
            if (absent && !("assistant".equals(value) && message.get(TOOL_CALLS_FIELD) != null)) {
                throw refused(CONTENT_FIELD, "Only an assistant tool-call message may omit its content");
            }
            if (!absent && !content.isTextual() && !content.isArray()) {
                throw refused(CONTENT_FIELD, "A chat message content must be text or content parts");
            }
            if (content != null && content.isTextual() && utf8Length(content.asText()) > MAX_MESSAGE_CONTENT_BYTES) {
                throw new LlmInvocationException(413, "request_too_large", CONTENT_FIELD,
                        "A chat message exceeds the permitted content size", false);
            }
            if ("tool".equals(value)
                    && (message.get(TOOL_CALL_ID_FIELD) == null
                            || !message.get(TOOL_CALL_ID_FIELD).isTextual()
                            || StringUtils.isBlank(message.get(TOOL_CALL_ID_FIELD).asText()))) {
                throw refused(TOOL_CALL_ID_FIELD, "A tool result message must identify its tool call");
            }
            requireToolCallEntries(message.get(TOOL_CALLS_FIELD), false, true);
        }
    }

    /** 中文说明：{@code tool_calls} 形状核对：必须是数组，每项含 {@code id}、{@code type=function}、{@code function} 对象与 {@code function.name}，{@code arguments} 必须是<b>字符串</b>（绝不当对象解析）；{@code fragmentMode} 为真时按流式片段放宽（携带 {@code index} 的非负整数定序、id/name/arguments 可缺，但已出现的 {@code arguments} 仍是字符串片段）。 English summary: the shape check of {@code tool_calls}: an array whose every entry carries {@code id}, {@code type=function} and a {@code function} object with {@code function.name}, where {@code arguments} must be a <b>string</b> (never parsed as an object); in {@code fragmentMode} the streaming shape is accepted instead (a non-negative {@code index} orders the fragments and id/name/arguments may be absent, but a present {@code arguments} is still a string fragment). */
    private void requireToolCallEntries(JsonNode toolCalls, boolean fragmentMode, boolean requestSide) {
        if (toolCalls == null || toolCalls.isNull()) {
            return;
        }
        if (!toolCalls.isArray()) {
            throw toolCallFailure(requestSide, TOOL_CALLS_FIELD, "The tool calls must be an array");
        }
        for (JsonNode call : toolCalls) {
            if (!call.isObject()) {
                throw toolCallFailure(requestSide, TOOL_CALLS_FIELD, "Every tool call must be an object");
            }
            if (fragmentMode) {
                JsonNode index = call.get(INDEX_FIELD);
                if (index == null || !index.isIntegralNumber() || !index.canConvertToInt()
                        || index.intValue() < 0) {
                    throw toolCallFailure(requestSide, INDEX_FIELD,
                            "A streamed tool call fragment must carry a non-negative index");
                }
            } else if (!call.path(ID_FIELD).isTextual() || StringUtils.isBlank(call.path(ID_FIELD).asText())) {
                throw toolCallFailure(requestSide, ID_FIELD, "Every tool call must carry its identifier");
            }
            JsonNode type = call.get(TYPE_FIELD);
            if (type != null && !type.isNull() && !FUNCTION_FIELD.equals(type.asText())) {
                throw toolCallFailure(requestSide, TYPE_FIELD, "Only function tool calls are supported");
            }
            JsonNode function = call.get(FUNCTION_FIELD);
            if (function == null || function.isNull()) {
                if (!fragmentMode) {
                    throw toolCallFailure(requestSide, FUNCTION_FIELD,
                            "Every tool call must carry its function");
                }
                continue;
            }
            if (!function.isObject()) {
                throw toolCallFailure(requestSide, FUNCTION_FIELD, "A tool call function must be an object");
            }
            JsonNode name = function.get(NAME_FIELD);
            if (name != null && !name.isNull() && !name.isTextual()) {
                throw toolCallFailure(requestSide, NAME_FIELD, "A tool call function name must be text");
            }
            if (!fragmentMode && (!function.path(NAME_FIELD).isTextual()
                    || StringUtils.isBlank(function.path(NAME_FIELD).asText()))) {
                throw toolCallFailure(requestSide, NAME_FIELD,
                        "Every tool call function must carry its name");
            }
            JsonNode arguments = function.get(ARGUMENTS_FIELD);
            if (arguments != null && !arguments.isNull() && !arguments.isTextual()) {
                throw toolCallFailure(requestSide, ARGUMENTS_FIELD,
                        "Tool call arguments must travel as a string");
            }
        }
    }

    /** 中文说明：工具调用形态问题的两处出口：请求侧是 400 {@code unsupported_parameter}（客户端的错），响应/流帧侧是 502 {@code upstream_protocol_error}（上游的错），绝不混淆归因。 English summary: the two exits for a tool-call shape violation: the request side is a 400 {@code unsupported_parameter} (the client's fault) while the response or frame side is a 502 {@code upstream_protocol_error} (the upstream's fault), so attribution is never confused. */
    private LlmInvocationException toolCallFailure(boolean requestSide, String parameter, String description) {
        if (requestSide) {
            return refused(parameter, description);
        }
        return new LlmInvocationException(502, "upstream_protocol_error", parameter, description, false);
    }

    /** 中文说明：{@code tools} 核对：数组、≤64、每项 {@code {type:"function",function:{name,...}}}，函数名 1–64 的 {@code [A-Za-z0-9_-]+} 且唯一；本面只声明 function 工具，供应商托管工具一律拒绝。 English summary: the {@code tools} check: an array of at most 64 entries, each {@code {type:"function",function:{name,...}}}, with function names of 1–64 characters from {@code [A-Za-z0-9_-]+} and unique; this face declares function tools only, so provider-hosted tools are refused. */
    private void requireTools(ObjectNode payload) {
        JsonNode tools = payload.get(TOOLS_FIELD);
        if (tools == null || tools.isNull()) {
            return;
        }
        if (!tools.isArray() || tools.size() > MAX_TOOLS) {
            throw refused(TOOLS_FIELD, "The chat tools must be an array of at most 64 entries");
        }
        Set<String> names = new HashSet<>();
        for (JsonNode tool : tools) {
            if (!tool.isObject() || !FUNCTION_FIELD.equals(tool.path(TYPE_FIELD).asText())) {
                throw refused(TOOLS_FIELD, "Only function tools are supported");
            }
            JsonNode function = tool.get(FUNCTION_FIELD);
            if (function == null || !function.isObject() || !function.path(NAME_FIELD).isTextual()) {
                throw refused(TOOLS_FIELD, "Every function tool must carry its name");
            }
            String name = function.path(NAME_FIELD).asText();
            if (!name.matches("^[A-Za-z0-9_-]{1,64}$") || !names.add(name)) {
                throw refused(NAME_FIELD, "A function tool name must be 1-64 characters and unique");
            }
            JsonNode parameters = function.get("parameters");
            if (parameters != null && !parameters.isNull() && !parameters.isObject()) {
                throw refused("parameters", "A function tool parameter schema must be a JSON object");
            }
        }
    }

    /** 中文说明：{@code tool_choice} 核对：字符串只允许 none/auto/required，对象形态必须是 {@code {type:"function",function:{name}}} 且引用的函数已在 {@code tools} 里声明。 English summary: the {@code tool_choice} check: a string is limited to none/auto/required, an object must be {@code {type:"function",function:{name}}} and may only reference a function already declared in {@code tools}. */
    private void requireToolChoice(ObjectNode payload) {
        JsonNode choice = payload.get(TOOL_CHOICE_FIELD);
        if (choice == null || choice.isNull()) {
            return;
        }
        if (choice.isTextual()) {
            if (!TOOL_CHOICE_MODES.contains(choice.asText())) {
                throw refused(TOOL_CHOICE_FIELD, "The requested tool choice mode is unsupported");
            }
            return;
        }
        if (!choice.isObject() || !FUNCTION_FIELD.equals(choice.path(TYPE_FIELD).asText())
                || !choice.path(FUNCTION_FIELD).path(NAME_FIELD).isTextual()) {
            throw refused(TOOL_CHOICE_FIELD, "An object tool choice must name a declared function");
        }
        String chosen = choice.path(FUNCTION_FIELD).path(NAME_FIELD).asText();
        if (!declaresTool(payload, chosen)) {
            throw refused(TOOL_CHOICE_FIELD, "An object tool choice must name a declared function");
        }
    }

    /** 中文说明：{@code parallel_tool_calls} 只能是布尔；缺省即缺失，绝不补写默认值。 English summary: {@code parallel_tool_calls} may only be a boolean; absent means absent and no default is ever injected. */
    private void requireParallelToolCalls(ObjectNode payload) {
        JsonNode parallel = payload.get(PARALLEL_TOOL_CALLS_FIELD);
        if (parallel != null && !parallel.isNull() && !parallel.isBoolean()) {
            throw refused(PARALLEL_TOOL_CALLS_FIELD, "The parallel tool calls parameter must be a boolean");
        }
    }

    /** 中文说明：{@code stream_options} 只在 {@code stream=true} 时可传，且本面只知 {@code include_usage} 一键；原样保留，不改写、不补齐。 English summary: {@code stream_options} may travel only with {@code stream=true} and this face knows only {@code include_usage}; it is preserved verbatim, never rewritten nor padded. */
    private void requireStreamOptions(ObjectNode payload, boolean streaming) {
        JsonNode options = payload.get(STREAM_OPTIONS_FIELD);
        if (options == null || options.isNull()) {
            return;
        }
        if (!streaming) {
            throw refused(STREAM_OPTIONS_FIELD, "Stream options are accepted only for streaming requests");
        }
        if (!options.isObject()) {
            throw refused(STREAM_OPTIONS_FIELD, "The stream options must be an object");
        }
        for (Iterator<String> names = options.fieldNames(); names.hasNext(); ) {
            String name = names.next();
            if (!INCLUDE_USAGE_FIELD.equals(name)) {
                throw refused(safeParameterName(name), "An unsupported stream option was submitted");
            }
            if (!options.get(name).isBoolean()) {
                throw refused(INCLUDE_USAGE_FIELD, "The include usage flag must be a boolean");
            }
        }
    }

    /** 中文说明：本面输出预算：{@code max_tokens} 与 {@code max_completion_tokens} <b>二选一</b>（同时出现即 400，不得以另一协议字段替换），取值必须是 1..8192 的正整数。 English summary: this face's output budget: {@code max_tokens} versus {@code max_completion_tokens} with <b>exactly one</b> of them (both at once is a 400 and one protocol's field never substitutes for the other), each a positive integer within 1..8192. */
    private void requireOutputBudget(ObjectNode payload) {
        JsonNode maxTokens = payload.get(MAX_TOKENS_FIELD);
        JsonNode maxCompletionTokens = payload.get(MAX_COMPLETION_TOKENS_FIELD);
        boolean firstAbsent = maxTokens == null || maxTokens.isNull();
        boolean secondAbsent = maxCompletionTokens == null || maxCompletionTokens.isNull();
        if (!firstAbsent && !secondAbsent) {
            throw refused(MAX_COMPLETION_TOKENS_FIELD,
                    "Only one of max_tokens and max_completion_tokens may be submitted");
        }
        if (firstAbsent && secondAbsent) {
            return;
        }
        JsonNode budget = firstAbsent ? maxCompletionTokens : maxTokens;
        String parameter = firstAbsent ? MAX_COMPLETION_TOKENS_FIELD : MAX_TOKENS_FIELD;
        if (!budget.isIntegralNumber() || !budget.canConvertToInt()
                || budget.intValue() < 1 || budget.intValue() > MAX_OUTPUT_TOKENS) {
            throw refused(parameter, "The chat output budget must be a positive integer within 8192");
        }
    }

    /** 中文说明：本版合同只支持 {@code n==1}：缺省即缺失（由上游默认），其它取值明确拒绝。 English summary: this release supports only {@code n==1}: absence stays absence (the upstream default decides) while any other value is explicitly refused. */
    private void requireSingleCandidate(ObjectNode payload) {
        JsonNode candidates = payload.get(N_FIELD);
        if (candidates == null || candidates.isNull()) {
            return;
        }
        if (!candidates.isIntegralNumber() || !candidates.canConvertToInt() || candidates.intValue() != 1) {
            throw refused(N_FIELD, "Only a single completion candidate is supported");
        }
    }

    /** 中文说明：采样参数闭区间核对（只校验已声明的已知键，未知键在上一步已拒绝），非有限值或越界即 400；缺失保持缺失。 English summary: the closed-interval check of the sampling parameters (only a declared known key reaches here, unknown keys were refused one step earlier) with a non-finite or out-of-range value a 400; absence stays absence. */
    private void requireNumericRange(ObjectNode payload, String parameter, double minimum, double maximum) {
        JsonNode value = payload.get(parameter);
        if (value == null || value.isNull()) {
            return;
        }
        if (!value.isNumber() || !Double.isFinite(value.asDouble())
                || value.asDouble() < minimum || value.asDouble() > maximum) {
            throw refused(parameter, "The requested sampling parameter is outside its supported range");
        }
    }

    private boolean declaresTool(ObjectNode payload, String name) {
        JsonNode tools = payload.get(TOOLS_FIELD);
        if (tools == null || !tools.isArray() || StringUtils.isBlank(name)) {
            return false;
        }
        for (JsonNode tool : tools) {
            if (name.equals(tool.path(FUNCTION_FIELD).path(NAME_FIELD).asText())) {
                return true;
            }
        }
        return false;
    }

    /** 中文说明：整个请求文档必须落在 {@code yuheng.llm.max-request-bytes} 之内，越界即 413，且绝不发起上游调用。 English summary: the whole request document must stay within {@code yuheng.llm.max-request-bytes}, a 413 rather than an upstream call once it does not. */
    private void requireWithinRequestCeiling(byte[] documentBytes) {
        long ceiling = gatewayProperties.getMaxRequestBytes();
        if (ceiling < 1 || documentBytes.length > ceiling) {
            throw new LlmInvocationException(413, "request_too_large", null,
                    "The chat request exceeds the configured byte ceiling", false);
        }
    }

    /** 中文说明：解码后的文档同样受字节上限约束，越界是 502（上游产出失控）而不是 413（客户端的错）。 English summary: the decoded document obeys the same byte ceiling too, where a breach is a 502 (runaway upstream) rather than a 413 (client fault). */
    private void requireWithinResponseCeiling(byte[] documentBytes) {
        long ceiling = gatewayProperties.getMaxRequestBytes();
        if (ceiling < 1 || documentBytes.length > ceiling) {
            throw new LlmInvocationException(502, "upstream_protocol_error", CHOICES_FIELD,
                    "The chat response document exceeds the configured byte ceiling", false);
        }
    }

    /** 中文说明：解码失败的统一出口：只记 alias、协议与缺失的结构名，绝不记正文、工具参数或密钥，随后抛 502 {@code upstream_protocol_error}。 English summary: the single decode-failure exit: it logs only the alias, the protocol and the name of the missing structure, never a body, tool argument or key, and then raises the 502 {@code upstream_protocol_error}. */
    private void rejectDecode(LlmInvocationCommandDTO command, String parameter, String expectation) {
        log.warn("llm chat decode rejected alias={} protocol={} parameter={} expected={}",
                command == null ? null : command.getModel(), protocol(), parameter, expectation);
        throw new LlmInvocationException(502, "upstream_protocol_error", parameter,
                "The chat response violates the native chat completions contract", false);
    }

    /** 中文说明：单播请求的超时预算：连接段取渠道 {@code connectTimeoutMs}，整体收束取 {@code totalTimeoutMs} 与 {@code headerTimeoutMs+idleTimeoutMs} 的较小者，绝不在预算之外无限等待。 English summary: the timeout budget of a unary request: the connect segment takes the channel's {@code connectTimeoutMs} while the overall close-out takes the smaller of {@code totalTimeoutMs} and {@code headerTimeoutMs} plus {@code idleTimeoutMs}, so a call never waits beyond its budget. */
    private long responseBudgetMs(LlmModelSnapshotBO.ChannelBO channel) {
        long headroom = channel.getHeaderTimeoutMs().longValue() + channel.getIdleTimeoutMs().longValue();
        return Math.max(1L, Math.min(channel.getTotalTimeoutMs().longValue(), headroom));
    }

    /** 中文说明：文档序列化为 UTF-8 字节，失败只按 502 {@code upstream_protocol_error} 收束，不回显文档内容。 English summary: serializes a document into UTF-8 bytes, closing out with a 502 {@code upstream_protocol_error} on failure and never echoing the document. */
    private byte[] toBytes(ObjectNode document) {
        try {
            return objectMapper.writeValueAsBytes(document);
        } catch (JsonProcessingException rejected) {
            log.warn("llm chat document is not serializable protocol={}", protocol());
            throw new LlmInvocationException(502, "upstream_protocol_error", null,
                    "The chat document cannot be serialized", false);
        }
    }

    /** 中文说明：只有形态安全的协议参数名才可作为 {@code param} 回显，避免把任意 JSON 键名带上响应。 English summary: only a parameter name in a safe shape may be echoed as {@code param}, keeping an arbitrary JSON key off the response. */
    private String safeParameterName(String field) {
        return field != null && field.matches("^[A-Za-z0-9_-]{1,32}$") ? field : null;
    }

    private int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    // ------------------------------------------------------------------ frame grammar carriers

    /**
     * 中文说明：Chat 帧流的可变状态：是否已出 {@code [DONE]}、是否见过携带 {@code finish_reason} 的终帧与已编码帧数，
     * 全部只在一次订阅内可见（{@link Flux#defer} 每次订阅新建），既不是共享状态也不是缓存整条流的载体。
     * English summary: The mutable state of the Chat frame flux: whether {@code [DONE]} has left, whether a terminal chunk
     * carrying {@code finish_reason} was ever seen and how many frames were encoded — all scoped to one subscription (a
     * fresh instance per {@link Flux#defer}), so it is neither shared state nor a whole-stream cache.
     */
    private static final class StreamState {

        /** 中文说明：已交付的客户端帧数，仅用于日志计数。 English summary: the number of client frames delivered, used only as a log count. */
        private int frames;

        /** 中文说明：是否见过终态 {@code finish_reason} chunk；只影响诊断，绝不据此伪造 {@code [DONE]}。 English summary: whether a terminal {@code finish_reason} chunk was ever seen; diagnostic only and never a licence to fabricate {@code [DONE]}. */
        private boolean terminalChunkSeen;

        /** 中文说明：是否已经出且只出过一个 {@code data: [DONE]}。 English summary: whether exactly one {@code data: [DONE]} has already been emitted. */
        private boolean finished;

        /** 中文说明：本流唯一一次交出的 {@code [DONE]} 缓冲本体：帧流按对象身份在此收束，因此同一批里排在它之前的
         * chunk 全部照发、它自己作为最后一帧照发、它之后的一律不再下发，取消也在此刻传播到上游。
         * English summary: the one and only {@code [DONE]} buffer this stream hands over: the frame flux closes on that
         * buffer by identity, so every chunk ordered before it inside the same batch still travels, it travels last, nothing
         * behind it travels at all, and the cancellation reaches upstream at exactly that moment. */
        private DataBuffer terminalFrame;
    }

    /**
     * 中文说明：SSE 残帧缓冲：容量恒为「单帧上限 + 帧分隔符」，因此内存有界；只有确认为一帧之内的字节才驻留，
     * 装不下即 502（一个撒谎的或失控的上游也无法把引擎撑爆）。{@link #drainFrames()} 只消费已经以空行收束的完整帧，
     * 半帧留在缓冲里等待下一块字节续装。
     * English summary: The SSE residual buffer: its capacity is always exactly one frame ceiling plus the frame separator,
     * so memory stays bounded; only bytes proven to fit inside one frame are retained and a refusal to fit is a 502 (so
     * neither a lying nor a runaway upstream can inflate the engine). {@link #drainFrames()} consumes only frames already
     * closed by a blank line, leaving a half frame in place until the next chunk continues it.
     */
    private static final class FrameBuffer {

        /** 中文说明：帧缓冲本体与其当前长度。 English summary: the frame buffer itself together with its current length. */
        private final byte[] data;
        private int length;

        private FrameBuffer(int capacity) {
            this.data = new byte[Math.max(capacity, FRAME_SEPARATOR.length())];
        }

        /** 中文说明：按剩余空间分片喂入字节，每片之后抽取已收束的完整帧；缓冲装满仍拼不出一个完整帧即判单帧超限（502），因此驻留字节恒不超过一帧。 English summary: feeds bytes in windows of the free space, draining every closed frame after each window; a full buffer that still yields no complete frame is an oversized frame (502), so what is ever retained stays inside one frame. */
        private List<String> feed(byte[] bytes) {
            List<String> payloads = new ArrayList<>(2);
            int offset = 0;
            while (bytes != null && offset < bytes.length) {
                int take = Math.min(data.length - length, bytes.length - offset);
                if (take <= 0) {
                    throw new LlmInvocationException(502, "upstream_protocol_error", CHOICES_FIELD,
                            "A single chat stream frame exceeds the configured frame ceiling", false);
                }
                System.arraycopy(bytes, offset, data, length, take);
                length += take;
                offset += take;
                payloads.addAll(drainFrames());
            }
            return payloads;
        }

        /** 中文说明：取出缓冲区里所有已收束的完整帧的 {@code data:} 载荷；纯注释/其它字段的事件返回 null 元素以便调用方保留顺序。 English summary: takes the {@code data:} payloads of every complete frame closed inside the buffer; an event made only of comments or other fields yields a null element so the caller keeps the ordering. */
        private List<String> drainFrames() {
            List<String> payloads = new ArrayList<>(2);
            while (true) {
                int end = frameEnd();
                if (end < 0) {
                    return payloads;
                }
                String event = new String(data, 0, end, StandardCharsets.UTF_8);
                System.arraycopy(data, end, data, 0, length - end);
                length -= end;
                payloads.add(dataPayloadOf(event));
            }
        }

        /** 中文说明：寻找第一个以空行收束的帧尾：逐行走过 {@code \n}、{@code \r\n} 与 {@code \r} 三种行终止符，
         * 行首恰落在终止符处即为空行（帧界）；未收束即 -1，绝不把半帧当完整帧解析，缓冲区末尾尚未看全的终止符
         * 也只当作普通行尾，因此半帧与多帧同块都按顺序各处理一次。
         * English summary: finds the first frame closed by a blank line, walking the line terminators
         * ({@code \n}, {@code \r\n} and {@code \r}) so a terminator sitting exactly at the start of a line is the blank
         * line that closes the frame; -1 while nothing is closed, so a half frame is never parsed as complete and a
         * terminator still cut at the end of the buffer only counts as an ordinary line end, which keeps one pass per
         * frame whether several frames share a block or one frame spans blocks. */
        private int frameEnd() {
            int lineStart = 0;
            int cursor = 0;
            while (cursor < length) {
                byte current = data[cursor];
                if (current != LINE_FEED && current != CARRIAGE_RETURN) {
                    cursor++;
                    continue;
                }
                int next = cursor + 1;
                if (current == CARRIAGE_RETURN && next < length && data[next] == LINE_FEED) {
                    next++;
                }
                if (lineStart == cursor) {
                    return next;
                }
                lineStart = next;
                cursor = next;
            }
            return -1;
        }
    }

    /**
     * 中文说明：按 SSE 文法把一个事件块拼成 chunk 载荷：行终止符支持 {@code \n}、{@code \r\n} 与 {@code \r}，
     * 空行不产生任何载荷，{@code:} 开头的注释行与 {@code event:}/{@code id:}/{@code retry:} 控制字段只服务事件语义、
     * 一律不计入载荷；{@code data:} 行去掉前缀及其后一个可选空格后计入载荷，多条 {@code data:} 行按 SSE 以换行连接。
     * 一个 JSON chunk 被上游拆成多条物理行时（美化或历史 {@code data:} 块的写法），既非控制字段也非空行的行就是
     * 该载荷的续行，同样以换行并入，因此一个事件块恒好等于一个 chunk，绝不会被截成半份 JSON。
     * 整块没有任何载荷即 null（注释、心跳或纯控制字段事件），由调用方原样跳过。
     * English summary: rebuilds one chunk payload out of an event block per the SSE grammar: the line terminators
     * {@code \n}, {@code \r\n} and {@code \r} all count, a blank line carries no payload, a {@code:} comment line and the
     * {@code event:}/{@code id:}/{@code retry:} control fields speak only to event semantics and never enter the payload,
     * while a {@code data:} line loses its prefix plus one optional following space and several {@code data:} lines join
     * with a newline. When an upstream wraps one JSON chunk over several physical lines (the pretty-printed or historic
     * {@code data:} block style) every remaining non-blank, non-control line is a continuation of that payload and joins
     * it with a newline too, so one event block always equals exactly one chunk and is never cut into half a JSON
     * document. No payload at all means null (a comment, heartbeat or control-only event) and the caller skips it.
     */
    private static String dataPayloadOf(String event) {
        StringBuilder payload = null;
        int cursor = 0;
        while (cursor < event.length()) {
            int terminator = nextLineTerminator(event, cursor);
            String line = event.substring(cursor, terminator < 0 ? event.length() : terminator);
            cursor = terminator < 0 ? event.length() : afterLineTerminator(event, terminator);
            if (line.isEmpty() || isSseControlLine(line)) {
                continue;
            }
            String value = line.startsWith(DATA_PREFIX)
                    ? withoutOneLeadingSpace(line.substring(DATA_PREFIX.length())) : line;
            if (payload == null) {
                payload = new StringBuilder(value);
            } else {
                payload.append('\n').append(value);
            }
        }
        return payload == null ? null : payload.toString();
    }

    /** 中文说明：下一个行终止符下标（{@code \n} 或 {@code \r}），没有即 -1。 English summary: the index of the next line terminator ({@code \n} or {@code \r}), or -1 when the text holds none. */
    private static int nextLineTerminator(String event, int from) {
        for (int index = from; index < event.length(); index++) {
            char current = event.charAt(index);
            if (current == '\n' || current == '\r') {
                return index;
            }
        }
        return -1;
    }

    /** 中文说明：行终止符之后第一个字符的下标，{@code \r\n} 作为一个终止符整体跨过。 English summary: the index just past a line terminator, crossing {@code \r\n} as the single terminator it is. */
    private static int afterLineTerminator(String event, int terminator) {
        return event.charAt(terminator) == '\r' && terminator + 1 < event.length()
                && event.charAt(terminator + 1) == '\n' ? terminator + 2 : terminator + 1;
    }

    /** 中文说明：SSE 的事件层控制字段与注释行——它们描述「哪个事件、哪个 id、多久重连」，与 Chat 的 chunk 载荷无关。 English summary: SSE's event-level control fields and comment lines, which say "which event, which id, how fast to reconnect" and have nothing to do with a chat chunk payload. */
    private static boolean isSseControlLine(String line) {
        return line.startsWith(COMMENT_PREFIX) || line.startsWith(EVENT_PREFIX)
                || line.startsWith(ID_PREFIX) || line.startsWith(RETRY_PREFIX);
    }

    /** 中文说明：按 SSE 只剥离载荷值的一个可选前导空格，其余空白一律保留，绝不做二次修剪。 English summary: strips exactly the one optional leading space SSE allows and never trims anything further. */
    private static String withoutOneLeadingSpace(String value) {
        return value.startsWith(" ") ? value.substring(1) : value;
    }
}
