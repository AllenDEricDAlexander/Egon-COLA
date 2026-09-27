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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
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
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.exception.LlmInvocationException;
import top.egon.cola.component.yuheng.llm.proxy.domain.vo.LlmInvocationResultVO;

/**
 * 中文说明：{@code OpenAiEmbeddingProtocolStrategy} 是 {@link LlmProtocolStrategy} 的 <b>Embeddings 协议面</b>实现，
 * 以 bean 名 {@code openAiEmbeddingProtocolStrategy} 落在 {@code llmProtocolStrategyRegistry} 的固定四条映射里，
 * 只服务 {@link LlmProtocolEnum#OPENAI_EMBEDDING}，永不回落 Chat、永不按协议写 if/switch。本类承担 Spec §9.2.2
 * 交给协议面的四项职责与唯一编排入口：① {@link #encodeRequest} 只接受已知必需字段
 * {@code model}/{@code input}/{@code encoding_format}（外加可选的向量维度声明 {@code dimensions}），要求
 * {@code input} 为单个非空字符串或 1–64 项非空字符串数组且顺序保真、UTF-8 总量在合同内，
 * {@code encoding_format} 只能是 {@code float}（禁止无声改成 base64），任何 {@code stream} 位、未声明扩展以及
 * {@code upstream_url}/{@code baseUrl}/{@code host}/{@code credential}/{@code secretRef} 之类自证出网或凭据字段一律
 * 400 {@code unsupported_parameter}，随后只把 {@code model} 改写为 {@code route.upstreamModel}——这是上游真实模型名
 * 唯一被允许出网的地方；整个文档还须落在 {@code yuheng.llm.max-request-bytes} 内，越界即 413
 * {@code request_too_large}。② {@link #decodeUnaryResponse} 保真透传：{@code data[].embedding} 与
 * {@code data[].index} 原样保留、{@code usage} 上游没报就绝不伪造，只把 {@code model} 回写为客户端 alias；
 * 两条不可放宽的硬不变式在此强制——每条向量的长度必须等于声明维度且彼此等长、有限、非空，
 * {@code data} 条数必须恰等于 {@code input} 条数且 index 构成 0..N-1 的无重复全集；任何不符都是
 * 502 {@code upstream_protocol_error}，<b>绝不</b>截断、补齐、重排，也绝不把失败伪装成空成功。本操作按 SPI 固定签名只拿到
 * 命令与上游文档，拿不到 {@code LlmModelSnapshotBO}，因此「alias 声明的维度」由两处共同证明：路由阶段
 * （{@code LlmRouteSelectionStrategy}）已把「EMBEDDING 必须声明 {@code dimensions}」变成前置条件，协议面则把该维度经
 * 请求侧唯一合法的 {@code dimensions} 字段带进来核对每一条向量；请求未携带维度时仍然强制「全体向量等长、有限、非空且
 * 条数与 {@code input} 全等」，即一个参差不齐或数量不符的向量集在任何配置下都不可能通过，只可能少一个数字上界核对。
 * ③ {@link #encodeStream}：Embeddings 是纯单播协议，本方法<b>不</b>实现任何帧文法，而是在提交前立即失败关闭
 * （502 {@code upstream_protocol_error}），同时把每一个进入的 {@link DataBuffer} 恰好释放一次，永不伪造
 * {@code [DONE]}。④ {@link #encodeError} 只产出 {@code {"error":{message,type,param,code}}} 这一个顶层键，四个值全部
 * 取自 {@link LlmInvocationException} 的安全字段，绝不套 admin 的 {@code code}/{@code data} 信封，也绝不带出
 * token、密钥、baseUrl、upstreamModel、prompt 正文或任何向量数值。
 * {@link #exchange} 是唯一编排入口：先经 {@link #encodeRequest} 拒绝 {@code stream}（因此流式请求根本不会出网），
 * 再校验渠道必须是<b>已解析、已启用、同协议的 LOCAL</b> 渠道（本地向量化失败永不改投云端，这条在协议面再证一次），
 * 凭据只允许由 {@code yuheng.llm.secrets-root} 之下对 {@code secretRef} 的规范化解析取得（越界、穿越、缺失即 503
 * {@code model_unavailable}，值与引用名都不入日志），上游用 JDK 原生 {@link HttpClient} 按渠道四段超时预算就地构建，
 * 非 2xx 按其原生状态交 {@link #encodeError} 写回 {@code routed}，成功才进 {@link #decodeUnaryResponse}；线程池饱和
 * （{@code TaskRejectedException}）在提交前表现为 429 {@code rate_limit_exceeded}，全程无重试、无换模型、
 * 无事务与锁内等待。日志只有 alias、协议、渠道 key、状态、条数与「维度是否不符」这类布尔量。
 * English summary: {@code OpenAiEmbeddingProtocolStrategy} is the <b>Embeddings face</b> of
 * {@link LlmProtocolStrategy}, registered as {@code openAiEmbeddingProtocolStrategy} in the fixed four-entry
 * {@code llmProtocolStrategyRegistry} and serving only {@link LlmProtocolEnum#OPENAI_EMBEDDING} — it never falls back to
 * Chat and never branches on a protocol. It carries the four duties Spec §9.2.2 assigns to the protocol face plus the one
 * orchestration entry: (1) {@link #encodeRequest} accepts only the known required fields {@code model}/{@code input}/
 * {@code encoding_format} (with the optional declared width {@code dimensions}), demanding {@code input} to be one
 * non-blank string or an ordered 1–64 array of non-blank strings inside the UTF-8 ceiling, {@code encoding_format} to be
 * {@code float} only (never silently switched to base64), rejecting any {@code stream} flag, any undeclared extension and
 * any self-asserted egress or credential field such as {@code upstream_url}/{@code baseUrl}/{@code host}/
 * {@code credential}/{@code secretRef} as a 400 {@code unsupported_parameter}, and then rewriting only {@code model} to
 * {@code route.upstreamModel} — the single place the real upstream model name may egress; the whole document must stay
 * within {@code yuheng.llm.max-request-bytes} or it is a 413 {@code request_too_large}. (2)
 * {@link #decodeUnaryResponse} passes the native JSON through faithfully: {@code data[].embedding} keeps its
 * {@code data[].index}, an absent {@code usage} is never fabricated, and only {@code model} is written back to the client
 * alias; two invariants are non-widenable here — every vector's length must equal the declared dimensions while staying
 * uniform, finite and non-empty across the set, and the number of {@code data} entries must equal the number of
 * {@code input} entries with the indexes forming the duplicate-free full set 0..N-1 — any disagreement is a 502
 * {@code upstream_protocol_error}, never a truncated, padded or re-ordered result and never an empty success. Because the
 * SPI pins this operation's signature to the command plus the upstream document, the snapshot is not reachable here, so the
 * alias-declared width is proved from two sides together: the routing face ({@code LlmRouteSelectionStrategy}) already
 * makes "an embedding alias must declare its {@code dimensions}" a precondition, and this face checks every vector
 * against that width as it arrives through the request's one legitimate {@code dimensions} field; when a request carries no
 * width the uniform-length, finite, non-empty and exact-count-with-{@code input} demands are still enforced, so a ragged or
 * miscounted vector set can never pass under any configuration.
 * (3) {@link #encodeStream}: embeddings are unary-only, so this method implements no frame grammar at all and fails closed
 * immediately before commit (502 {@code upstream_protocol_error}) while releasing every incoming {@link DataBuffer} exactly
 * once and never emitting a fake {@code [DONE]}. (4) {@link #encodeError} emits only the top-level {@code error} key with
 * {@code message}, {@code type}, {@code param} and {@code code} taken from the safe fields of
 * {@link LlmInvocationException}, never the admin {@code code}/{@code data} envelope and never a token, secret, base URL,
 * upstream model, prompt text or vector value. {@link #exchange} is the single orchestration entry:
 * {@link #encodeRequest} first refuses the stream flag (so a streaming embeddings call never leaves the process), the
 * channel must be a <b>resolved, enabled, same-protocol LOCAL</b> channel (a local vectorization failure is never
 * re-routed to cloud, re-proved at this face), credentials come only from resolving {@code secretRef} under
 * {@code yuheng.llm.secrets-root} (an escape, a {@code ..} traversal or an absent value is a 503
 * {@code model_unavailable} whose detail never reaches a log), the upstream call uses a JDK-native {@link HttpClient}
 * built locally from the route channel's four timeout budgets, a non-2xx leaves with the upstream's native status through
 * {@link #encodeError}, and only a success reaches {@link #decodeUnaryResponse}; pool saturation
 * ({@code TaskRejectedException}) presents as a pre-commit 429 {@code rate_limit_exceeded}, with no retry, no model
 * switch and no wait inside a transaction or a lock. Logs carry only the alias, protocol, channel key, status, counts and
 * dimension-mismatch booleans.
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
@Component("openAiEmbeddingProtocolStrategy")
public class OpenAiEmbeddingProtocolStrategy implements LlmProtocolStrategy {

    /** 中文说明：Embeddings 请求面唯一已知字段；{@code dimensions} 是可选的向量维度声明，其余未声明字段一律按扩展拒绝。 English summary: the only fields the embeddings request face knows; {@code dimensions} is the optional vector-width declaration and everything else is rejected as an extension. */
    private static final Set<String> DECLARED_REQUEST_FIELDS =
            Set.of("model", "input", "encoding_format", "dimensions");

    /** 中文说明：客户端可自证的出网/凭据字段名——这些事实只存在于服务端受管配置里，出现即 400 且绝不回显其值。 English summary: self-asserted egress or credential field names, facts that live only in server-side managed configuration, refused with a 400 that never echoes their value. */
    private static final Set<String> SELF_ASSERTED_FIELDS = Set.of(
            "upstream_url", "upstreamUrl", "base_url", "baseUrl", "url", "endpoint", "host",
            "credential", "credentials", "api_key", "apiKey", "secret", "secret_ref", "secretRef",
            "authorization", "headers", "timeout");

    /** 中文说明：协议固定字段名，逐字对应 Spec §9.2.2 的原生 wire 键。 English summary: the protocol's fixed field names, verbatim from the native wire keys of Spec §9.2.2. */
    private static final String MODEL_FIELD = "model";
    private static final String INPUT_FIELD = "input";
    private static final String ENCODING_FORMAT_FIELD = "encoding_format";
    private static final String DIMENSIONS_FIELD = "dimensions";
    private static final String STREAM_FIELD = "stream";
    private static final String DATA_FIELD = "data";
    private static final String INDEX_FIELD = "index";
    private static final String EMBEDDING_FIELD = "embedding";
    private static final String USAGE_FIELD = "usage";

    /** 中文说明：本面唯一允许的编码格式；base64 等其它写法必须 400 拒绝，不得无声改写。 English summary: the only encoding this face allows; base64 and any other spelling is a 400 rather than a silent rewrite. */
    private static final String FLOAT_ENCODING = "float";

    /** 中文说明：{@code input} 的条数与 UTF-8 总量上界，逐字对应 Spec §9.2.2 的 1–64 项与 ≤256KiB。 English summary: the entry and total UTF-8 ceilings of {@code input}, matching the 1–64 entries and ≤256KiB bounds of Spec §9.2.2 verbatim. */
    private static final int MAX_INPUT_ITEMS = 64;
    private static final int MAX_INPUT_UTF8_BYTES = 262_144;

    /** 中文说明：声明维度的合法区间，与 {@code LlmModelSnapshotBO.dimensions} 的 1..16000 合同同宽。 English summary: the legal dimension window, as wide as the 1..16000 contract of {@code LlmModelSnapshotBO.dimensions}. */
    private static final int MIN_DIMENSIONS = 1;
    private static final int MAX_DIMENSIONS = 16_000;

    /** 中文说明：出网路径段：渠道 baseUrl 已含 API 根（如 {@code /v1}），本面只在其后追加 {@code embeddings}。 English summary: the egress path segment: a channel base URL already carries the API root (for example {@code /v1}), so this face only appends {@code embeddings}. */
    private static final String EMBEDDINGS_PATH_SEGMENT = "embeddings";

    /** 中文说明：解析出的凭据长度上界；超限按「不可用」处理，避免异常挂载文件把引擎拖垮。 English summary: the ceiling of a resolved credential; an oversized one is treated as unavailable so an anomalous mount cannot exhaust the engine. */
    private static final int MAX_SECRET_CHARACTERS = 4_096;

    /** 中文说明：非注入的缓冲工厂共享实例：本面是单播协议，只产出一个 JSON 文档缓冲。 English summary: the shared non-injected buffer factory: this face is unary and produces exactly one JSON document buffer. */
    private static final DataBufferFactory BUFFER_FACTORY = DefaultDataBufferFactory.sharedInstance;

    /** 中文说明：{@code yuheng.llm.*} 运行边界：字节上限 {@code max-request-bytes} 与凭据挂载根 {@code secrets-root}，按名注入且无宽松默认。 English summary: the {@code yuheng.llm.*} runtime boundary: the {@code max-request-bytes} ceiling and the {@code secrets-root} mount, injected by name with no permissive default. */
    @Qualifier(LlmGatewayProperties.BEAN_NAME)
    private final LlmGatewayProperties gatewayProperties;

    /** 中文说明：容器唯一的 Jackson 栈，用于原生文档的读取与写出，禁止另起第二套 JSON 实现。 English summary: the container's single Jackson stack used to read and write the native documents, so no second JSON implementation can appear. */
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：本实现唯一负责的协议是 Embeddings 面常量，注册表据此把本 bean 归入该键，不符即启动失败。
     * English summary: This implementation owns only the Embeddings protocol constant, which is how the registry files this
     * bean under that key; a disagreement fails startup.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OpenAiEmbeddingProtocolStrategy.protocol()}。
     * @return 返回 {@link LlmProtocolEnum#OPENAI_EMBEDDING}，永不为 null；returns {@link LlmProtocolEnum#OPENAI_EMBEDDING}, never null.
     */
    @Override
    public LlmProtocolEnum protocol() {
        return LlmProtocolEnum.OPENAI_EMBEDDING;
    }

    /**
     * 中文说明：职责①请求编码。只核对已知必需字段：{@code model} 必须与命令里的客户端 alias 全等、{@code input} 是
     * 单个非空字符串或 1–64 项非空字符串数组（内容与顺序原样保留，UTF-8 总量 ≤256KiB）、{@code encoding_format}
     * 只接受 {@code float}、可选 {@code dimensions} 必须是 1..16000 的整数；任何 {@code stream} 位、未声明扩展与自证
     * 出网/凭据字段都被拒绝，随后仅把 {@code model} 改写为 {@code route.upstreamModel} 并按固定顺序重新产出文档，
     * 整个文档大小受 {@code yuheng.llm.max-request-bytes} 约束。
     * English summary: Duty (1) request encoding. Only the known required fields are verified: {@code model} must equal the
     * command's client alias, {@code input} is one non-blank string or an ordered 1–64 array of non-blank strings (content
     * and order preserved, at most 256KiB of UTF-8 in total), {@code encoding_format} accepts only {@code float} and the
     * optional {@code dimensions} must be an integer within 1..16000; any {@code stream} flag, undeclared extension or
     * self-asserted egress or credential field is refused, after which only {@code model} is rewritten to
     * {@code route.upstreamModel} and the document is re-emitted in a fixed order under
     * {@code yuheng.llm.max-request-bytes}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OpenAiEmbeddingProtocolStrategy.encodeRequest(command, route)}，返回体直接作为上游请求正文。
     * @param command 参数 已校验的调用命令，提供 alias、stream 位与原生请求文档；parameter the validated command carrying the alias, the stream flag and the native request document.
     * @param route 参数 已选定的候选 route，其 {@code upstreamModel} 是唯一允许替换 alias 的上游模型名；parameter the selected candidate route whose {@code upstreamModel} is the only name allowed to replace the alias.
     * @return 返回 原生 Embeddings 请求文档（{@code model}、有序 {@code input}、{@code encoding_format:"float"}，声明时含 {@code dimensions}）；returns the native embeddings document ({@code model}, the ordered {@code input}, {@code encoding_format:"float"} and {@code dimensions} when declared).
     * @throws LlmInvocationException 400 {@code unsupported_parameter}（{@code stream}、未知扩展、自证出网/凭据字段、字段形态非法）、413 {@code request_too_large}（输入或文档超限）、503 {@code model_unavailable}（route 未给出 upstreamModel）；状态即原生状态。
     */
    @Override
    public ObjectNode encodeRequest(LlmInvocationCommandDTO command, LlmModelSnapshotBO.RouteBO route) {
        ObjectNode payload = command == null ? null : command.getPayload();
        if (payload == null) {
            throw refused(INPUT_FIELD, "The embeddings request carries no native document");
        }
        if (Boolean.TRUE.equals(command.getStream()) || payload.has(STREAM_FIELD)) {
            throw refused(STREAM_FIELD, "The embeddings protocol is unary and accepts no stream parameter");
        }
        rejectSelfAssertedFields(payload);
        rejectUndeclaredFields(payload);
        requireClientAlias(payload, command.getModel());
        requireFloatEncoding(payload);
        Integer declaredDimensions = requireDeclaredDimensions(payload);
        JsonNode input = requireInput(payload);
        String upstreamModel = route == null ? null : StringUtils.trimToNull(route.getUpstreamModel());
        if (upstreamModel == null) {
            throw new LlmInvocationException(503, "model_unavailable", MODEL_FIELD,
                    "The selected route resolves to no upstream embedding model", false);
        }
        ObjectNode request = objectMapper.createObjectNode();
        request.put(MODEL_FIELD, upstreamModel);
        request.set(INPUT_FIELD, input.deepCopy());
        request.put(ENCODING_FORMAT_FIELD, FLOAT_ENCODING);
        if (declaredDimensions != null) {
            request.put(DIMENSIONS_FIELD, declaredDimensions.intValue());
        }
        requireWithinRequestCeiling(toBytes(request));
        log.info("llm embeddings request encoded alias={} protocol={} channel={} entries={}",
                command.getModel(), protocol(), route.getChannelKey(), countInputEntries(payload));
        return request;
    }

    /**
     * 中文说明：职责②单播解码/透传。只处理成功文档：整份原生 JSON 深拷贝后仅回写 {@code model} 为客户端 alias，
     * {@code data[].embedding}、{@code data[].index} 与上游原生 {@code usage} 一律保真（缺 {@code usage} 不伪造 0，
     * 未知但安全的字段不丢，数组不重排）。两条硬不变式在此强制：每条向量必须是<b>有限、非空、彼此等长</b>的
     * 数字数组且在请求声明了维度时恰等于该维度；{@code data} 条数必须恰等于 {@code input} 条数、index 必须构成
     * 0..N-1 的无重复全集。任何不符都是 502 {@code upstream_protocol_error}，绝不截断、补齐或返回空成功。
     * English summary: Duty (2) unary decoding and pass-through. Success documents only: the whole native JSON is deep-copied
     * and only {@code model} is written back to the client alias, while {@code data[].embedding}, {@code data[].index} and
     * the native {@code usage} stay faithful (a missing {@code usage} is never fabricated as zero, unknown but safe fields
     * survive and no array is re-ordered). Two hard invariants are enforced here: every vector must be a finite,
     * non-empty, mutually equal-length numeric array matching the requested width when one is declared, and the number of
     * {@code data} entries must equal the number of {@code input} entries with indexes forming the duplicate-free full set
     * 0..N-1. Any disagreement is a 502 {@code upstream_protocol_error}, never a truncated, padded or empty success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OpenAiEmbeddingProtocolStrategy.decodeUnaryResponse(command, upstreamBody)}。
     * @param command 参数 本次调用命令，提供回写 alias、{@code input} 条数与声明维度；parameter this invocation's command, supplying the alias to write back, the input entry count and the declared width.
     * @param upstreamBody 参数 上游成功响应的原生 JSON 文档；parameter the native JSON document of the successful upstream response.
     * @return 返回 保真后的原生响应文档，{@code model} 已恢复为客户端 alias；returns the fidelity-preserving native document with {@code model} restored to the client alias.
     * @throws LlmInvocationException 502 {@code upstream_protocol_error}（缺 {@code data}、条数与 {@code input} 不符、index 重复或缺项、向量空/非有限/长度不符）；400 {@code unsupported_parameter}（本侧 {@code input} 或 {@code dimensions} 无法据以核对）。
     */
    @Override
    public ObjectNode decodeUnaryResponse(LlmInvocationCommandDTO command, ObjectNode upstreamBody) {
        ObjectNode payload = command == null ? null : command.getPayload();
        int expectedEntries = countInputEntries(payload);
        Integer declaredDimensions = requireDeclaredDimensions(payload);
        JsonNode data = upstreamBody == null ? null : upstreamBody.get(DATA_FIELD);
        if (data == null || !data.isArray() || data.isEmpty()) {
            rejectDecode(command, false);
        }
        if (data.size() != expectedEntries) {
            log.warn("llm embeddings decode rejected alias={} protocol={} expectedEntries={} actualEntries={} "
                    + "countMismatch=true", command.getModel(), protocol(), expectedEntries, data.size());
            throw new LlmInvocationException(502, "upstream_protocol_error", DATA_FIELD,
                    "The embedding response returned a different number of vectors than the submitted input", false);
        }
        requireVectorSet(command, data, expectedEntries, declaredDimensions);
        JsonNode usage = upstreamBody.get(USAGE_FIELD);
        if (usage != null && !usage.isObject()) {
            rejectDecode(command, false);
        }
        ObjectNode decoded = upstreamBody.deepCopy();
        decoded.put(MODEL_FIELD, command.getModel());
        log.info("llm embeddings decoded alias={} protocol={} vectors={} declaredDimensions={}",
                command.getModel(), protocol(), expectedEntries, declaredDimensions);
        return decoded;
    }

    /**
     * 中文说明：职责③流式帧解码与编码——对 Embeddings 面而言是<b>纯拒绝</b>操作：本协议没有流式文法，因此本方法
     * 不解析任何帧、不产出任何字节，而是在提交之前立即以 502 {@code upstream_protocol_error}（param {@code stream}）
     * 收束，同时把每一个进入的 {@link DataBuffer} 恰好释放一次（{@code doOnNext} 释放、{@code doOnDiscard} 兜底），
     * 并且永不伪造 {@code [DONE]}。正常链路根本到不了这里：{@link #encodeRequest} 已在出网前拒绝 {@code stream}。
     * English summary: Duty (3) streaming frame decoding and encoding — for the Embeddings face a <b>pure refusal</b>: this
     * protocol has no frame grammar, so nothing is parsed and no byte is produced; instead a 502
     * {@code upstream_protocol_error} (param {@code stream}) closes the call immediately before commit while every incoming
     * {@link DataBuffer} is released exactly once (released in {@code doOnNext}, with {@code doOnDiscard} as the backstop),
     * and a fake {@code [DONE]} is never emitted. A well-formed call never reaches here because {@link #encodeRequest}
     * refuses {@code stream} before any egress.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OpenAiEmbeddingProtocolStrategy.encodeStream(command, upstreamFrames)}；订阅即失败，
     * 返回的 publisher 只带一个终态错误信号，交给 {@code LlmServletStreamComponent} 也不会写出任何帧。
     * / subscribing fails at once, the returned publisher carries only its terminal error, so
     * {@code LlmServletStreamComponent} would write out no frame at all.
     * @param command 参数 本次调用命令，仅用于脱敏日志的 alias 与协议；parameter this invocation's command, used only for the masked alias and protocol log.
     * @param upstreamFrames 参数 被逐帧释放的进入字节流；parameter the incoming byte flux, released frame by frame.
     * @return 返回 只以 502 {@code upstream_protocol_error} 终止的空流，绝无任何数据帧；returns an empty flux terminated solely by a 502 {@code upstream_protocol_error}, carrying no data frame at all.
     * @throws LlmInvocationException 提交前 502 {@code upstream_protocol_error}（本协议不支持流式）；a pre-commit 502 {@code upstream_protocol_error} because this protocol is unary.
     */
    @Override
    public Flux<DataBuffer> encodeStream(LlmInvocationCommandDTO command, Flux<DataBuffer> upstreamFrames) {
        log.warn("llm embeddings stream refused alias={} protocol={} reason=unary_protocol",
                command == null ? null : command.getModel(), protocol());
        LlmInvocationException refused = new LlmInvocationException(502, "upstream_protocol_error", STREAM_FIELD,
                "The embeddings protocol is unary and emits no stream frame", false);
        return upstreamFrames
                .doOnNext(DataBufferUtils::release)
                .doOnDiscard(DataBuffer.class, DataBufferUtils::release)
                .thenMany(Flux.<DataBuffer>error(refused));
    }

    /**
     * 中文说明：职责④原生错误编码。OpenAI Embeddings 的错误对象只有一个顶层键 {@code error}，其内层按
     * {@code message}、{@code type}、{@code param}、{@code code} 四键产出，取值全部来自
     * {@link LlmInvocationException} 的安全字段（{@code type} 由本面按状态自行映射，{@code param} 缺失即原生 null）；
     * 绝不使用 admin 的 {@code code}/{@code data} 信封，也绝不把 token、密钥、baseUrl、upstreamModel、prompt 正文或
     * 向量数值写进任何字段。
     * English summary: Duty (4) native error encoding. An OpenAI embeddings error object has exactly one top-level
     * {@code error} key whose inner object carries {@code message}, {@code type}, {@code param} and {@code code}, all taken
     * from the safe fields of {@link LlmInvocationException} (this face maps {@code type} from the status itself and a
     * missing {@code param} is a native null); the admin {@code code}/{@code data} envelope is never used and no token,
     * secret, base URL, upstream model, prompt text or vector value ever enters a field.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OpenAiEmbeddingProtocolStrategy.encodeError(error)}，状态取
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
     * 中文说明：唯一编排入口，顺序固定：{@link #encodeRequest}（因此在任何出网之前就拒绝 {@code stream}）→
     * 校验 route 的渠道为<b>已解析、已启用、同协议的 LOCAL</b> 渠道（本地向量化失败永不改投云端，此处再证一次；
     * 只在唯一这条 route 上尝试，绝不换渠道、换模型或重试）→ 仅在 {@code yuheng.llm.secrets-root} 之下按
     * {@code secretRef} 的规范化路径解析凭据（越界、{@code ..} 穿越、非普通文件与空白即 503，值不进日志）→
     * 用 JDK 原生 {@link HttpClient} 按渠道四段超时预算就地发起 POST（不跟随重定向）→ 非 2xx 按上游原生状态经
     * {@link #encodeError} 写回 {@code routed}（绝不伪装成空成功）→ 2xx 才解析并 {@link #decodeUnaryResponse}，
     * 把有界单缓冲 body 与原生状态写回 {@code routed}。响应文档受同一字节上限约束；线程池饱和
     * （{@code TaskRejectedException}/{@code RejectedExecutionException}）在提交前翻译为 429；本方法不开事务、不加锁，
     * 也绝不在事务或锁内等待上游。
     * English summary: The single orchestration entry in a fixed order: {@link #encodeRequest} (so a {@code stream} is
     * refused before anything egresses) → the route's channel must be a <b>resolved, enabled, same-protocol LOCAL</b>
     * channel (a local vectorization failure is never re-routed to cloud, re-proved here, and only this one route is ever
     * attempted, never another channel or model, never a retry) → credentials resolve only as a normalized
     * {@code secretRef} path under {@code yuheng.llm.secrets-root} (an escape, a {@code ..} traversal, a non-regular file
     * or a blank value is a 503 whose detail never reaches a log) → a JDK-native {@link HttpClient} built from the
     * channel's four timeout budgets issues the POST without following redirects → a non-2xx leaves through
     * {@link #encodeError} with the upstream's native status written back into {@code routed} (never dressed as an empty
     * success) → only a 2xx is parsed and handed to {@link #decodeUnaryResponse}, whose bounded single-buffer body and
     * native status complete {@code routed}. The response document obeys the same byte ceiling; pool saturation
     * ({@code TaskRejectedException}/{@code RejectedExecutionException}) becomes a pre-commit 429; this method opens no
     * transaction and takes no lock, and never waits on upstream inside one.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code OpenAiEmbeddingProtocolStrategy.exchange(command, route, routed)}，由
     * {@code LlmApiController} 在 {@code LlmInvocationService.invoke(command)} 之后调用。
     * @param command 参数 已校验的调用命令；parameter the validated invocation command.
     * @param route 参数 已被路由策略放行的首条候选 route；parameter the head candidate route the routing strategy already admitted.
     * @param routed 参数 路由阶段产出的原生结果，携带安全响应头，本方法就地补全状态与 body；parameter the routed native result carrying the safe headers, whose status and body this method completes in place.
     * @return 返回 已带原生状态、安全头与有界 body 的原生结果（成功为解码文档，上游失败为原生错误文档）；returns the native result with status, safe headers and a bounded body (the decoded document on success, the native error document on an upstream failure).
     * @throws LlmInvocationException 提交前的本地与上游失败：400/413（请求面）、429 {@code rate_limit_exceeded}（饱和）、502 {@code upstream_protocol_error}（文档越界或不变式不符）、503 {@code model_unavailable}（渠道或凭据不可用、传输失败）、504 {@code upstream_timeout}；状态即原生状态。
     */
    @Override
    public LlmInvocationResultVO exchange(LlmInvocationCommandDTO command,
            LlmModelSnapshotBO.RouteBO route,
            LlmInvocationResultVO routed) {
        ObjectNode request = encodeRequest(command, route);
        byte[] requestBody = toBytes(request);
        LlmModelSnapshotBO.ChannelBO channel = requireLocalChannel(command, route);
        URI endpoint = egressEndpoint(channel);
        String credential = resolveCredential(command, channel);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(channel.getConnectTimeoutMs().longValue()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        log.info("llm embeddings egress alias={} protocol={} channel={} bytes={}",
                command.getModel(), protocol(), channel.getChannelKey(), requestBody.length);
        try {
            HttpRequest.Builder upstreamRequestBuilder = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofMillis(responseBudgetMs(channel)))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            if (credential != null) {
                upstreamRequestBuilder.header("Authorization", "Bearer " + credential);
            }
            HttpRequest upstreamRequest = upstreamRequestBuilder
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                    .build();
            HttpResponse<InputStream> response =
                    client.send(upstreamRequest, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            try (InputStream upstreamBody = response.body()) {
                if (status < 200 || status > 299) {
                    return handUpstreamError(routed, channel, status);
                }
                ObjectNode decoded = decodeUnaryResponse(command, successDocument(response, upstreamBody));
                byte[] responseBody = toBytes(decoded);
                requireWithinResponseCeiling(responseBody);
                routed.setStatus(status);
                routed.setBody(Flux.just(BUFFER_FACTORY.wrap(responseBody)));
                return routed;
            }
        } catch (HttpTimeoutException timedOut) {
            log.warn("llm embeddings upstream timed out alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            throw new LlmInvocationException(504, "upstream_timeout", null,
                    "The local embedding channel did not answer within its timeout budget", false);
        } catch (TaskRejectedException saturated) {
            throw refusedOnSaturation(command, channel, saturated);
        } catch (RejectedExecutionException saturated) {
            throw refusedOnSaturation(command, channel, saturated);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            log.warn("llm embeddings aborted alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            throw new LlmInvocationException(503, "model_unavailable", null,
                    "The local embedding call was aborted", false);
        } catch (IOException transportFailure) {
            log.warn("llm embeddings upstream transport failed alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            throw new LlmInvocationException(503, "model_unavailable", null,
                    "The local embedding channel is unreachable", true);
        }
    }

    /** 中文说明：饱和即提交前 429：不排队、不重试、不改投任何渠道，日志只到渠道 key 为止。 English summary: saturation is a pre-commit 429: no queueing, no retry and no re-routing, with the log stopping at the channel key. */
    private LlmInvocationException refusedOnSaturation(
            LlmInvocationCommandDTO command, LlmModelSnapshotBO.ChannelBO channel, RuntimeException saturated) {
        log.warn("llm embeddings refused pre-commit on saturation alias={} protocol={} channel={}",
                command.getModel(), protocol(), channel.getChannelKey());
        return new LlmInvocationException(429, "rate_limit_exceeded", null,
                "The embedding engine is saturated; retry this request later", true);
    }

    /** 中文说明：出网前的渠道自检：渠道必须已解析、已启用、协议为本面且部署为 LOCAL——路由策略已把 embedding 强制为 LOCAL，此处是协议面的第二道证明，任何不符只拒绝本次尝试而绝不改投云端或别的渠道。 English summary: the pre-egress channel self-check: a channel must be resolved, enabled, speak this face's protocol and be LOCAL — the routing strategy already pins embeddings to LOCAL and this is the protocol face's second proof, so any disagreement rejects only this attempt and never re-routes to cloud or another channel. */
    private LlmModelSnapshotBO.ChannelBO requireLocalChannel(
            LlmInvocationCommandDTO command, LlmModelSnapshotBO.RouteBO route) {
        LlmModelSnapshotBO.ChannelBO channel = route == null ? null : route.getChannel();
        if (channel == null || !Boolean.TRUE.equals(channel.getEnabled())) {
            log.warn("llm embeddings channel unresolved or disabled alias={} protocol={} channel={}",
                    command.getModel(), protocol(), route == null ? null : route.getChannelKey());
            throw new LlmInvocationException(503, "model_unavailable", null,
                    "The selected embedding route resolves to no enabled channel", false);
        }
        if (channel.getDeployment() != LlmDeploymentEnum.LOCAL || channel.getProtocol() != protocol()
                || command.getProtocol() != protocol()) {
            log.warn("llm embeddings channel rejected alias={} protocol={} channel={} deployment={} channelProtocol={}",
                    command.getModel(), protocol(), channel.getChannelKey(),
                    channel.getDeployment(), channel.getProtocol());
            throw new LlmInvocationException(503, "model_unavailable", null,
                    "Embeddings egress is permitted only on a local channel of this protocol", false);
        }
        return channel;
    }

    /** 中文说明：把管理员配置的 baseUrl 与本协议固定路径段拼成出网 URI：必须绝对 http(s)、无 userinfo/query/fragment；解析失败只回 503，绝不回显原始地址。 English summary: joins the administrator-configured base URL with this protocol's fixed path segment: absolute http(s) without userinfo, query or fragment; a parse failure answers with a 503 that never echoes the original address. */
    private URI egressEndpoint(LlmModelSnapshotBO.ChannelBO channel) {
        try {
            URI base = URI.create(StringUtils.trimToEmpty(channel.getBaseUrl()));
            boolean schemeAllowed = "https".equalsIgnoreCase(base.getScheme())
                    || "http".equalsIgnoreCase(base.getScheme());
            if (!base.isAbsolute() || !schemeAllowed || StringUtils.isBlank(base.getHost())
                    || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null) {
                throw new IllegalArgumentException("the configured embedding channel base url is not egressable");
            }
            String root = StringUtils.removeEnd(StringUtils.trimToEmpty(base.getRawPath()), "/");
            String path = root.isEmpty() ? "/" + EMBEDDINGS_PATH_SEGMENT : root + "/" + EMBEDDINGS_PATH_SEGMENT;
            return new URI(base.getScheme(), null, base.getHost(), base.getPort(), path, null, null);
        } catch (URISyntaxException | IllegalArgumentException rejected) {
            log.warn("llm embeddings channel base url unparsable channel={}", channel.getChannelKey());
            throw new LlmInvocationException(503, "model_unavailable", null,
                    "The configured embedding channel address cannot be used for egress", false);
        }
    }

    /** 中文说明：本地 embedding 有 secretRef 时只解析 {@code yuheng.llm.secrets-root} 下的规范化文件；无认证 LOCAL 可省略引用并不发送上游凭据头，引用无效或读取失败即拒绝本次尝试。 English summary: when a local embedding channel has a secretRef it resolves only to a normalized file under {@code yuheng.llm.secrets-root}; an auth-free LOCAL channel may omit it and sends no credential header, while invalid/unreadable references refuse this attempt. */
    private String resolveCredential(LlmInvocationCommandDTO command, LlmModelSnapshotBO.ChannelBO channel) {
        if (channel.getSecretRef() == null) {
            return null;
        }
        String reference = StringUtils.trimToNull(channel.getSecretRef());
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
            log.warn("llm embeddings secret unreadable alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            throw unresolvableCredential(command, channel);
        }
        if (secret.isEmpty() || secret.length() > MAX_SECRET_CHARACTERS) {
            log.warn("llm embeddings secret unusable alias={} protocol={} channel={}",
                    command.getModel(), protocol(), channel.getChannelKey());
            throw unresolvableCredential(command, channel);
        }
        return secret;
    }

    private LlmInvocationException unresolvableCredential(
            LlmInvocationCommandDTO command, LlmModelSnapshotBO.ChannelBO channel) {
        log.warn("llm embeddings credential unresolvable alias={} protocol={} channel={}",
                command.getModel(), protocol(), channel.getChannelKey());
        return new LlmInvocationException(503, "model_unavailable", null,
                "The managed credential of the local embedding channel is unavailable", false);
    }

    /** 中文说明：把 2xx 上游响应读成原生对象文档：先按 Content-Length 预检，再以 {@code readNBytes} 至多读到
     *  {@code yuheng.llm.max-request-bytes}+1 字节，因此一个撒谎的或失控的本地服务也无法把引擎撑爆；空文档、越界
     *  文档与非对象文档都是 502，绝不退化为空成功。单播 JSON 文档必须整体解析，这与「不得把整条流缓存成
     *  byte[]」的流式约束无关（本面没有流）。
     *  English summary: reads a 2xx upstream response into the native object document: Content-Length is pre-checked and the
     *  body is then read with {@code readNBytes} up to {@code yuheng.llm.max-request-bytes}+1 bytes, so neither a lying nor a
     *  runaway local server can inflate the engine; an empty, oversized or non-object document is a 502 rather than a
     *  degraded empty success. A unary JSON document has to be parsed whole, which is unrelated to the "never buffer a
     *  whole stream into byte[]" rule for the streaming face (this face has no stream). */
    private ObjectNode successDocument(HttpResponse<InputStream> response, InputStream upstreamBody) {
        long ceiling = gatewayProperties.getMaxRequestBytes();
        if (ceiling < 1 || response.headers().firstValueAsLong("Content-Length").orElse(-1) > ceiling) {
            throw new LlmInvocationException(502, "upstream_protocol_error", DATA_FIELD,
                    "The embedding response document exceeds the configured byte ceiling", false);
        }
        byte[] body;
        try {
            body = upstreamBody.readNBytes((int) Math.min(ceiling, Integer.MAX_VALUE - 1L) + 1);
        } catch (IOException unreadable) {
            log.warn("llm embeddings response body unreadable protocol={}", protocol());
            throw new LlmInvocationException(502, "upstream_protocol_error", DATA_FIELD,
                    "The embedding response document could not be read", false);
        }
        if (body.length == 0 || body.length > ceiling) {
            throw new LlmInvocationException(502, "upstream_protocol_error", DATA_FIELD,
                    "The embedding response document is empty or exceeds the configured byte ceiling", false);
        }
        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(body);
        } catch (IOException unparsable) {
            log.warn("llm embeddings response document unparsable protocol={}", protocol());
            throw new LlmInvocationException(502, "upstream_protocol_error", DATA_FIELD,
                    "The embedding response document cannot be parsed", false);
        }
        if (!(parsed instanceof ObjectNode document)) {
            throw new LlmInvocationException(502, "upstream_protocol_error", DATA_FIELD,
                    "The embedding response is not a native JSON object", false);
        }
        return document;
    }

    /** 中文说明：上游非 2xx 的原生收束：状态逐字保留、body 由 {@link #encodeError} 按本协议形状产出，上游正文一个字节都不回显；这是「上游失败」与「空成功」的分界点。 English summary: the native close-out of a non-2xx upstream: the status is kept verbatim and the body comes from {@link #encodeError} in this protocol's shape without echoing a single upstream byte; this is the boundary between an upstream failure and an empty success. */
    private LlmInvocationResultVO handUpstreamError(
            LlmInvocationResultVO routed, LlmModelSnapshotBO.ChannelBO channel, int status) {
        log.warn("llm embeddings upstream rejected protocol={} channel={} status={}",
                protocol(), channel.getChannelKey(), status);
        LlmInvocationException error = new LlmInvocationException(status, codeFor(status), null,
                descriptionFor(status), status == 429 || status == 503);
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
            return "The local embedding channel is rate limited";
        }
        if (status == 504) {
            return "The local embedding channel did not answer within its timeout budget";
        }
        if (status >= 500) {
            return "The local embedding channel failed";
        }
        return "The local embedding channel rejected the request";
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

    /** 中文说明：请求面字段/能力问题的固定拒绝载体：param 只放安全的协议参数名。 English summary: the fixed refusal carrier for request-face field or capability failures: param carries only a safe protocol parameter name. */
    private LlmInvocationException refused(String parameter, String description) {
        return new LlmInvocationException(400, "unsupported_parameter", parameter, description, false);
    }

    /** 中文说明：拒绝任何自证出网或凭据字段：这类事实只存在于服务端受管配置里，且只有形态安全的字段名才可回显。 English summary: refuses any self-asserted egress or credential field: such facts live only in server-side managed configuration, and only a well-shaped field name may ever be echoed. */
    private void rejectSelfAssertedFields(ObjectNode payload) {
        for (Iterator<String> names = payload.fieldNames(); names.hasNext(); ) {
            String name = names.next();
            if (SELF_ASSERTED_FIELDS.contains(name.toLowerCase())) {
                log.warn("llm embeddings request asserted a managed field protocol={}", protocol());
                throw refused(safeParameterName(name),
                        "The embeddings request asserts a server-managed egress or credential field");
            }
        }
    }

    /** 中文说明：未声明扩展一律 400：本面只认 §9.2.2 给定的四个键，工具、响应格式、用户标识等能力都属未声明。 English summary: an undeclared extension is always a 400: this face knows only the four keys of §9.2.2, so tools, response formats, user identity and the like stay undeclared. */
    private void rejectUndeclaredFields(ObjectNode payload) {
        for (Iterator<String> names = payload.fieldNames(); names.hasNext(); ) {
            String name = names.next();
            if (!DECLARED_REQUEST_FIELDS.contains(name)) {
                throw refused(safeParameterName(name),
                        "The embeddings request declares a parameter this protocol does not support");
            }
        }
    }

    /** 中文说明：{@code model} 必须与命令里的客户端 alias 全等，防止一份命令打两个模型。 English summary: {@code model} must equal the command's client alias so one command can never hit two models. */
    private void requireClientAlias(ObjectNode payload, String alias) {
        JsonNode model = payload.get(MODEL_FIELD);
        if (model == null || !model.isTextual() || StringUtils.isBlank(model.asText())
                || !StringUtils.equals(StringUtils.trim(model.asText()), alias)) {
            throw refused(MODEL_FIELD, "The embeddings request model must be the requested client alias");
        }
    }

    /** 中文说明：{@code encoding_format} 只接受 {@code float}：禁止把向量无声改成 base64 之类的其它表示。 English summary: {@code encoding_format} accepts only {@code float}, forbidding a silent re-encoding of vectors into base64 or any other representation. */
    private void requireFloatEncoding(ObjectNode payload) {
        JsonNode encoding = payload.get(ENCODING_FORMAT_FIELD);
        if (encoding != null && (!encoding.isTextual() || !FLOAT_ENCODING.equals(encoding.asText()))) {
            throw refused(ENCODING_FORMAT_FIELD, "Only the float encoding format is supported");
        }
    }

    /** 中文说明：可选 {@code dimensions} 声明：必须是 1..16000 的整数，与 alias 声明维度同宽，越界即 400；本面唯一的维度事实来源，用于解码侧的硬不变式核对。 English summary: the optional {@code dimensions} declaration: an integer within 1..16000, as wide as the alias-declared window and a 400 beyond it; the face's only source of the vector width against which the decode-side invariant is proved. */
    private Integer requireDeclaredDimensions(ObjectNode payload) {
        if (payload == null) {
            return null;
        }
        JsonNode dimensions = payload.get(DIMENSIONS_FIELD);
        if (dimensions == null || dimensions.isNull()) {
            return null;
        }
        if (!dimensions.isIntegralNumber() || !dimensions.canConvertToInt()
                || dimensions.intValue() < MIN_DIMENSIONS || dimensions.intValue() > MAX_DIMENSIONS) {
            throw refused(DIMENSIONS_FIELD, "The declared embedding dimensions are outside the supported range");
        }
        return dimensions.intValue();
    }

    /** 中文说明：{@code input} 形态与体量核对：单个非空字符串或 1–64 项非空字符串数组，UTF-8 总量 ≤256KiB（累计越界即刻收束，不再扫描剩余项）。 English summary: the shape and size check of {@code input}: one non-blank string or 1–64 non-blank strings, at most 256KiB of UTF-8 in total (bailing out as soon as the running size crosses the cap instead of scanning the rest). */
    private JsonNode requireInput(ObjectNode payload) {
        JsonNode input = payload.get(INPUT_FIELD);
        if (input == null || input.isNull()) {
            throw refused(INPUT_FIELD, "The embeddings request carries no input");
        }
        if (input.isTextual()) {
            requireNonBlankText(input.asText());
            requireInputBytes(utf8Length(input.asText()));
            return input;
        }
        if (!input.isArray() || input.isEmpty() || input.size() > MAX_INPUT_ITEMS) {
            throw refused(INPUT_FIELD, "The embeddings input must be 1 to 64 non-empty text entries");
        }
        long total = 0;
        for (JsonNode entry : input) {
            if (!entry.isTextual()) {
                throw refused(INPUT_FIELD, "Every embeddings input entry must be text");
            }
            requireNonBlankText(entry.asText());
            total += utf8Length(entry.asText());
            requireInputBytes(total);
        }
        return input;
    }

    /** 中文说明：空白输入项是形态错误而非「零条向量」，因此永远不可能凑成空成功。 English summary: a blank input entry is a shape violation rather than "zero vectors", so it can never add up to an empty success. */
    private void requireNonBlankText(String value) {
        if (StringUtils.isBlank(value)) {
            throw refused(INPUT_FIELD, "An embeddings input entry must not be blank");
        }
    }

    /** 中文说明：输入正文体量越界是 413 {@code request_too_large}（Spec §9.2.2 错误表），与协议形态错误区分开。 English summary: an oversized input body is a 413 {@code request_too_large} from the Spec §9.2.2 error table, kept distinct from a shape violation. */
    private void requireInputBytes(long totalUtf8Bytes) {
        if (totalUtf8Bytes > MAX_INPUT_UTF8_BYTES) {
            throw new LlmInvocationException(413, "request_too_large", INPUT_FIELD,
                    "The embeddings input exceeds the permitted UTF-8 size", false);
        }
    }

    private int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    /** 中文说明：整个请求文档必须落在 {@code yuheng.llm.max-request-bytes} 之内，越界即 413，且绝不发起上游调用。 English summary: the whole request document must stay within {@code yuheng.llm.max-request-bytes}, a 413 rather than an upstream call once it does not. */
    private void requireWithinRequestCeiling(byte[] documentBytes) {
        long ceiling = gatewayProperties.getMaxRequestBytes();
        if (ceiling < 1 || documentBytes.length > ceiling) {
            throw new LlmInvocationException(413, "request_too_large", null,
                    "The embeddings request exceeds the configured byte ceiling", false);
        }
    }

    /** 中文说明：解码后的文档同样受字节上限约束，越界是 502（上游产出失控）而不是 413（客户端的错）。 English summary: the decoded document obeys the same byte ceiling too, where a breach is a 502 (runaway upstream) rather than a 413 (client fault). */
    private void requireWithinResponseCeiling(byte[] documentBytes) {
        long ceiling = gatewayProperties.getMaxRequestBytes();
        if (ceiling < 1 || documentBytes.length > ceiling) {
            throw new LlmInvocationException(502, "upstream_protocol_error", DATA_FIELD,
                    "The embedding response document exceeds the configured byte ceiling", false);
        }
    }

    /** 中文说明：条数不变式的依据：应有的向量数只取自本侧 {@code input}，形态不合法时按请求错误拒绝而不是猜。 English summary: the basis of the count invariant: the expected number of vectors comes only from this side's {@code input}, and an ill-shaped input is refused as a request error rather than guessed. */
    private int countInputEntries(ObjectNode payload) {
        if (payload == null) {
            throw refused(INPUT_FIELD, "The embeddings request carries no native document");
        }
        JsonNode input = payload.get(INPUT_FIELD);
        if (input == null) {
            throw refused(INPUT_FIELD, "The embeddings request carries no input");
        }
        if (input.isTextual()) {
            return 1;
        }
        if (!input.isArray() || input.isEmpty() || input.size() > MAX_INPUT_ITEMS) {
            throw refused(INPUT_FIELD, "The embeddings input must be 1 to 64 non-empty text entries");
        }
        return input.size();
    }

    /** 中文说明：向量硬不变式核对：条数已由调用方固定，此处要求 index 构成 0..N-1 的无重复全集、每条向量为有限非空数字数组、彼此等长，并在请求声明了维度时要求长度恰等于该维度；任何不符都只给 502，日志只回条数与布尔量。 English summary: the vector hard-invariant check: with the count already pinned by the caller, the indexes must form the duplicate-free full set 0..N-1, every vector must be a finite non-empty numeric array of one common length, and that length must equal the declared width when the request declares one; any disagreement answers only with a 502 while the log carries counts and booleans. */
    private void requireVectorSet(LlmInvocationCommandDTO command, JsonNode data, int expectedEntries,
            Integer declaredDimensions) {
        Set<Integer> seenIndexes = new HashSet<>();
        int commonLength = -1;
        for (int position = 0; position < data.size(); position++) {
            JsonNode entry = data.get(position);
            if (!entry.isObject()) {
                rejectDecode(command, false);
            }
            JsonNode index = entry.get(INDEX_FIELD);
            if (!index.isIntegralNumber() || !index.canConvertToInt()) {
                rejectDecode(command, false);
            }
            int declaredIndex = index.intValue();
            if (declaredIndex < 0 || declaredIndex >= expectedEntries || !seenIndexes.add(declaredIndex)) {
                rejectDecode(command, false);
            }
            JsonNode vector = entry.get(EMBEDDING_FIELD);
            if (vector == null || !vector.isArray() || vector.isEmpty()) {
                rejectDecode(command, true);
            }
            for (JsonNode component : vector) {
                if (!component.isNumber() || !Double.isFinite(component.asDouble())) {
                    rejectDecode(command, false);
                }
            }
            if (commonLength == -1) {
                commonLength = vector.size();
            } else if (commonLength != vector.size()) {
                rejectDecode(command, true);
            }
            if (declaredDimensions != null && vector.size() != declaredDimensions.intValue()) {
                rejectDecode(command, true);
            }
        }
    }

    /** 中文说明：解码失败的统一出口：只记 alias、协议、条数与「维度是否不符」的布尔量，绝不记向量、正文或密钥，随后抛 502 {@code upstream_protocol_error}。 English summary: the single decode-failure exit: it logs only the alias, protocol, counts and the dimension-mismatch boolean, never a vector, text or key, and then raises the 502 {@code upstream_protocol_error}. */
    private void rejectDecode(LlmInvocationCommandDTO command, boolean dimensionMismatch) {
        log.warn("llm embeddings decode rejected alias={} protocol={} dimensionMismatch={}",
                command == null ? null : command.getModel(), protocol(), dimensionMismatch);
        throw new LlmInvocationException(502, "upstream_protocol_error", DATA_FIELD,
                "The embedding response violates the native embeddings contract", false);
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
            log.warn("llm embeddings document is not serializable protocol={}", protocol());
            throw new LlmInvocationException(502, "upstream_protocol_error", null,
                    "The embeddings document cannot be serialized", false);
        }
    }

    /** 中文说明：只有形态安全的协议参数名才可作为 {@code param} 回显，避免把任意 JSON 键名带上响应。 English summary: only a parameter name in a safe shape may be echoed as {@code param}, keeping an arbitrary JSON key off the response. */
    private String safeParameterName(String field) {
        return field != null && field.matches("^[A-Za-z0-9_-]{1,32}$") ? field : null;
    }
}
