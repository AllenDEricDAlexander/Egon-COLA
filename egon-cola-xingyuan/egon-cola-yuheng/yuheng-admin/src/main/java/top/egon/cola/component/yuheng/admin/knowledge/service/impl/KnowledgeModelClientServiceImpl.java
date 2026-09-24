package top.egon.cola.component.yuheng.admin.knowledge.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yuheng.admin.config.properties.KnowledgeModelClientProperties;
import top.egon.cola.component.yuheng.admin.knowledge.domain.bo.KnowledgeBaseBO;
import top.egon.cola.component.yuheng.admin.knowledge.repository.KnowledgeRepository;
import top.egon.cola.component.yuheng.admin.knowledge.service.KnowledgeModelClientService;

/**
 * 中文说明：{@code KnowledgeModelClientServiceImpl} 是 {@link KnowledgeModelClientService} 的唯一实现，
 * 只承担两件事：按知识库<b>冻结的 embedding alias</b> 取本地向量，以及按该知识库的 chat alias 做一次接地生成。
 * 出网对象是<b>同进程部署的企业 engine</b>（{@code POST {baseUrl}/v1/embeddings} 与
 * {@code POST {baseUrl}/v1/chat/completions}），凭据是 {@code knowledge-service} 的 SERVICE token：
 * 它<b>只</b>来自 {@code yuheng.knowledge.model-client.service-token-ref} 指向的文件引用，
 * 解析时以 {@link Path#toRealPath()} 做包含性核对（拒绝空白、{@code ..} 穿越、符号链接逃逸、非普通文件、
 * 超长内容），解析值与引用名都不写日志、不进错误、不进响应，因此代码里永远没有字面量令牌。
 * 本类<b>不</b>引入任何厂商 SDK、<b>不</b>新增 Maven 依赖，只用 JDK 原生 {@link HttpClient}（就地惰性构建、
 * 不跟随重定向、连接与读超时取配置）与注入的容器 Jackson 栈；响应体按
 * {@code yuheng.knowledge.model-client.max-response-bytes} 有界读取（先验 Content-Length，再
 * {@code readNBytes(ceiling + 1)}），超限/空文档/非对象文档一律 503，绝不回显上游正文。
 * English summary: {@code KnowledgeModelClientServiceImpl} is the only implementation of
 * {@link KnowledgeModelClientService} and does exactly two things: fetch local vectors through a knowledge base's
 * <b>frozen embedding alias</b> and perform one grounded generation through its chat alias. The egress target is the
 * <b>same-process deployed enterprise engine</b> ({@code POST {baseUrl}/v1/embeddings} and
 * {@code POST {baseUrl}/v1/chat/completions}) authenticated with the {@code knowledge-service} SERVICE token, which
 * comes <b>only</b> from the file reference in {@code yuheng.knowledge.model-client.service-token-ref} and is resolved
 * under a {@link Path#toRealPath()} containment check (a blank reference, a {@code ..} traversal, a symlink escape, a
 * non-regular file or an oversized value all refuse; neither the value nor the reference name ever reaches a log, an
 * error or a response, so no literal token exists anywhere in the code). This class pulls in <b>no</b> vendor SDK and
 * <b>no</b> new Maven dependency: only the JDK-native {@link HttpClient} (built locally on first use, redirects
 * disabled, connect and read timeouts from configuration) plus the injected container Jackson stack. A response body is
 * read under the {@code yuheng.knowledge.model-client.max-response-bytes} ceiling (Content-Length pre-checked, then
 * {@code readNBytes(ceiling + 1)}), and an oversized, empty or non-object document is a 503 that never echoes upstream
 * text.
 *
 * 用法 / Usage: 由 {@code DocumentIngestionStrategy}（按批次 ≤ 64）与 Step 13 的检索实现以限定名
 * {@code knowledgeModelClientServiceImpl} 注入；两个方法都在数据库事务与锁之外调用，本类也不开任何事务。
 * 每次调用先按 {@code kbId} 读取知识库权威行（缺失即 {@code 404 KNOWLEDGE_RESOURCE_NOT_FOUND}，与跨租户同口径），
 * 再复核嵌入侧的 {@code embeddingModel}/{@code embeddingSpaceId}/{@code dimensions} 与生成侧的
 * {@code chatModel}/{@code egressPolicy} 都已配置且与冻结值一致——调用方期望的维度与库中冻结维度不一致是状态分歧
 * （{@code 422 KNOWLEDGE_VALIDATION_FAILED}），未配置或不可解析才是依赖不可用（{@code 503}）。
 * 嵌入响应逐条硬校验：条数恰等于文本数、{@code index} 构成 0..N-1 无重复全集、每条长度恰等于期望维度、
 * 每个分量在 {@code double} 与 {@code float} 两侧都有限、向量非全零；任何一条不成立都 503 收束，
 * <b>绝不</b>补零、截断、重排、复用旧向量或改道云端（文档侧根本没有云端嵌入路径），
 * 生成侧助手文本为空同样 503 而不是"清空的 200"。日志只有 kbId、alias、状态、条数、维度与耗时。
 * / Inject it by qualifier {@code knowledgeModelClientServiceImpl} into {@code DocumentIngestionStrategy} (batches of at
 * most 64) and into the Step 13 retrieval implementation; both calls happen outside any database transaction or lock and
 * this class opens none. Every call first reads the knowledge base's authoritative row by {@code kbId} (an absent one is
 * {@code 404 KNOWLEDGE_RESOURCE_NOT_FOUND}, the same verdict as a foreign-tenant row), then re-checks that
 * {@code embeddingModel}/{@code embeddingSpaceId}/{@code dimensions} for embedding and {@code chatModel}/
 * {@code egressPolicy} for generation are configured and match the frozen values — an expected dimension that disagrees
 * with the stored one is a state divergence ({@code 422 KNOWLEDGE_VALIDATION_FAILED}) while an unset or unresolvable
 * dependency is {@code 503}. The embedding response is then proved entry by entry: the count equals the text count, the
 * indexes form the duplicate-free full set 0..N-1, every vector is exactly the expected length, every component is
 * finite on both the {@code double} and {@code float} side, and no vector is all zero; any breach closes out as 503 and
 * is <b>never</b> padded, truncated, re-ordered, served from a stale vector or rerouted to cloud (documents have no
 * cloud embedding path at all), and an empty assistant text is likewise a 503 rather than an emptied 200. Logs carry
 * only kbId, alias, status, counts, dimensions and latency.
 */
@Slf4j
@Validated
@Service("knowledgeModelClientServiceImpl")
@RequiredArgsConstructor
public class KnowledgeModelClientServiceImpl implements KnowledgeModelClientService {

    /** 中文说明：{@code yuheng.knowledge.model-client} 配置载体由 {@code @EnableConfigurationProperties} 注册，bean 名即 {@code prefix-全限定类名}。 English summary: the {@code yuheng.knowledge.model-client} holder is registered by {@code @EnableConfigurationProperties}, whose generated bean name is {@code prefix-FQCN}. */
    private static final String MODEL_CLIENT_PROPERTIES_BEAN =
            "yuheng.knowledge.model-client-top.egon.cola.component.yuheng.admin.config.properties."
                    + "KnowledgeModelClientProperties";

    /** 中文说明：依赖不可用的稳定机器码，本类所有失败收束点共用（含凭据不可解析、上游非 2xx、响应结构不合）。 English summary: the stable machine code of an unavailable dependency, shared by every close-out here (unresolvable credential, upstream non-2xx, structurally wrong response). */
    private static final int MODEL_UNAVAILABLE = 503;
    private static final String MODEL_UNAVAILABLE_STATUS = "KNOWLEDGE_MODEL_UNAVAILABLE";

    /** 中文说明：知识库行不存在或不可见的稳定机器码，与端口"跨租户/软删按不存在"口径一致。 English summary: the machine code of an absent or invisible knowledge base, matching the port's foreign-tenant/soft-deleted-reads-as-absent rule. */
    private static final int RESOURCE_NOT_FOUND = 404;
    private static final String RESOURCE_NOT_FOUND_STATUS = "KNOWLEDGE_RESOURCE_NOT_FOUND";

    /** 中文说明：调用方期望维度与库内冻结维度分歧的稳定机器码。 English summary: the machine code of an expected dimension that diverges from the stored frozen one. */
    private static final int VALIDATION_FAILED = 422;
    private static final String VALIDATION_FAILED_STATUS = "KNOWLEDGE_VALIDATION_FAILED";

    /** 中文说明：engine 根之后的两条固定协议路径段；{@code baseUrl} 是不含 {@code /v1} 的服务根，本类只追加固定段，绝不接受调用方自证地址。 English summary: the two fixed protocol paths below the engine root; {@code baseUrl} is the service root without {@code /v1}, only these fixed segments are appended and a caller-asserted address is never accepted. */
    private static final String EMBEDDINGS_PATH = "v1/embeddings";
    private static final String CHAT_PATH = "v1/chat/completions";

    /** 中文说明：解析出的 SERVICE token 字符上限，超限按不可用处理，避免异常挂载文件把进程撑爆。 English summary: the character ceiling of a resolved SERVICE token; beyond it the token counts as unavailable so an anomalous mount cannot inflate the process. */
    private static final int MAX_TOKEN_CHARACTERS = 4_096;

    /** 中文说明：嵌入协议字段名（{@code model}/{@code input}/{@code encoding_format}/{@code dimensions}）与响应字段名（{@code data}/{@code index}/{@code embedding}），逐字对应 OpenAI 兼容面。 English summary: the embedding request field names ({@code model}/{@code input}/{@code encoding_format}/{@code dimensions}) and response names ({@code data}/{@code index}/{@code embedding}), verbatim on the OpenAI-compatible face. */
    private static final String MODEL_FIELD = "model";
    private static final String INPUT_FIELD = "input";
    private static final String ENCODING_FORMAT_FIELD = "encoding_format";
    private static final String DIMENSIONS_FIELD = "dimensions";
    private static final String DATA_FIELD = "data";
    private static final String INDEX_FIELD = "index";
    private static final String EMBEDDING_FIELD = "embedding";

    /** 中文说明：生成协议字段名与安全取值：本类只发单播、非流式、无工具调用的最小文档。 English summary: the generation field names and safe values: this class sends only a minimal unary, non-streaming, tool-free document. */
    private static final String MESSAGES_FIELD = "messages";
    private static final String ROLE_FIELD = "role";
    private static final String CONTENT_FIELD = "content";
    private static final String MAX_TOKENS_FIELD = "max_tokens";
    private static final String STREAM_FIELD = "stream";
    private static final String CHOICES_FIELD = "choices";
    private static final String MESSAGE_FIELD = "message";
    private static final String USER_ROLE = "user";

    /** 中文说明：向量编码格式固定 {@code float}，禁止 base64 之类的其它表示导致无声改写。 English summary: the vector encoding is pinned to {@code float}, forbidding a silent rewrite through base64 or any other representation. */
    private static final String FLOAT_ENCODING = "float";

    /** 非注入：本进程唯一持有一次构建的 JDK 客户端，超时取配置，故首次使用后不再变化。 / the process's single lazily built JDK client, whose timeouts come from configuration. */
    private volatile HttpClient httpClient;

    @Qualifier(MODEL_CLIENT_PROPERTIES_BEAN)
    private final KnowledgeModelClientProperties modelClientProperties;

    @Qualifier("knowledgeRepository")
    private final KnowledgeRepository knowledgeRepository;

    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    /**
     * 中文说明：执行 embed 操作；顺序固定为「读知识库权威行 → 复核冻结的 embedding alias/空间/维度 →
     * 解析 SERVICE token → 有界出网 → 逐条硬校验向量」，任何一步不成立都以
     * {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE}（维度分歧为 {@code 422}）收束，绝不返回部分结果或截断成功。
     * 出站文档只含 {@code model}（库内冻结 alias）、有序 {@code input}、{@code encoding_format=float} 与
     * {@code dimensions}；不存在任何改道分支，因此"本地嵌入失败回落云端"在本实现里没有代码路径。
     * English summary: Executes the embed operation in a fixed order — load the knowledge base's authoritative row,
     * re-check the frozen embedding alias/space/dimensions, resolve the SERVICE token, egress under a byte ceiling, then
     * prove every vector entry by entry — and any breach closes out as {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE} (a
     * dimension divergence as {@code 422}) instead of returning a partial or truncated success. The outbound document
     * carries only {@code model} (the stored alias), the ordered {@code input}, {@code encoding_format=float} and
     * {@code dimensions}; there is no rerouting branch at all, so "cloud fallback after a local embedding failure" has no
     * code path in this implementation.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code knowledgeModelClientServiceImpl.embed(kbId, texts, expectedDimensions)}；单批 ≤ 64 条由端口注解约束，
     * 本方法总在事务与锁之外调用，日志只到 kbId/alias/条数/维度/耗时为止。
     * @param kbId 参数 知识库十进制字符串 id，决定冻结 alias 与维度；parameter decimal-string knowledge base id carrying the frozen alias and dimensions.
     * @param texts 参数 待嵌入文本，与返回向量同序；parameter texts to embed, in the same order as the returned vectors.
     * @param expectedDimensions 参数 期望向量维度，必须等于库内冻结值；parameter expected vector width, equal to the stored frozen one.
     * @return 返回 与入参同序、每条长度为期望维度的向量列表；returns the vectors in input order, each exactly the expected width.
     */
    @Override
    public List<float[]> embed(String kbId, List<String> texts, int expectedDimensions) {
        long startedAt = System.nanoTime();
        KnowledgeBaseBO base = requireBase(kbId);
        String alias = requireEmbeddingAlias(kbId, base, expectedDimensions);
        ObjectNode request = objectMapper.createObjectNode();
        request.put(MODEL_FIELD, alias);
        ArrayNode input = request.putArray(INPUT_FIELD);
        texts.forEach(input::add);
        request.put(ENCODING_FORMAT_FIELD, FLOAT_ENCODING);
        request.put(DIMENSIONS_FIELD, expectedDimensions);
        JsonNode document = post(kbId, alias, EMBEDDINGS_PATH, request, "embedding");
        List<float[]> vectors = requireVectorSet(kbId, alias, document, texts.size(), expectedDimensions);
        log.info("knowledge embedding served kb={} alias={} entries={} dimensions={} latencyMs={}",
                kbId, alias, vectors.size(), expectedDimensions, elapsedMillis(startedAt));
        return vectors;
    }

    /**
     * 中文说明：执行 generate 操作；只向库内冻结的 {@code chatModel} alias 发一份单播文档并取回助手文本，
     * 出域策略与 alias 都取自服务端权威行（不接受任何自证地址或模型名）。助手文本缺失、空白或结构不合一律
     * {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE}，绝不伪造空答案，也绝不把失败伪装成"成功的空字符串"。
     * English summary: Executes the generate operation; one unary document goes to the stored {@code chatModel} alias and
     * the assistant text comes back, with both the egress policy and the alias read from the authoritative row (a
     * self-asserted address or model is never accepted). A missing, blank or structurally wrong assistant text is
     * {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE}; an empty answer is never fabricated and a failure is never dressed as an
     * empty success.
     *
     * 用法 / Usage: 调用方式 / Usage:
     * {@code knowledgeModelClientServiceImpl.generate(kbId, prompt, maxTokens)}；提示词只出现在出站请求里、
     * 绝不写日志；本调用总在事务之外发起。
     * @param kbId 参数 知识库十进制字符串 id，决定 chat alias 与出域策略；parameter decimal-string knowledge base id deciding the chat alias and egress policy.
     * @param prompt 参数 接地提示词；parameter the grounded prompt.
     * @param maxTokens 参数 生成令牌上限；parameter the generation token bound.
     * @return 返回 原始助手文本，保证非空白；returns the raw assistant text, guaranteed non-blank.
     */
    @Override
    public String generate(String kbId, String prompt, int maxTokens) {
        long startedAt = System.nanoTime();
        KnowledgeBaseBO base = requireBase(kbId);
        String alias = requireChatAlias(kbId, base);
        ObjectNode request = objectMapper.createObjectNode();
        request.put(MODEL_FIELD, alias);
        ArrayNode messages = request.putArray(MESSAGES_FIELD);
        ObjectNode message = messages.addObject();
        message.put(ROLE_FIELD, USER_ROLE);
        message.put(CONTENT_FIELD, prompt);
        request.put(MAX_TOKENS_FIELD, maxTokens);
        request.put(STREAM_FIELD, false);
        JsonNode document = post(kbId, alias, CHAT_PATH, request, "chat");
        String answer = requireAssistantText(kbId, alias, document);
        log.info("knowledge chat served kb={} alias={} maxTokens={} answerChars={} latencyMs={}",
                kbId, alias, maxTokens, answer.length(), elapsedMillis(startedAt));
        return answer;
    }

    /** 中文说明：读取知识库权威行；缺失/跨租户/软删一律 404，与 {@code KnowledgeRepository} 的"按不存在处理"口径一致，绝不返回零值占位。 English summary: reads the knowledge base's authoritative row, mapping an absent, foreign-tenant or soft-deleted one to 404 exactly as {@code KnowledgeRepository} promises, never a zero-valued placeholder. */
    private KnowledgeBaseBO requireBase(String kbId) {
        return knowledgeRepository.findBase(kbId)
                .orElseGet(() -> {
                    log.warn("knowledge model client has no knowledge base kb={}", kbId);
                    throw new CommonException(RESOURCE_NOT_FOUND, RESOURCE_NOT_FOUND_STATUS,
                            "The knowledge base behind this model call does not exist");
                });
    }

    /** 中文说明：嵌入前置复核：embedding alias、嵌入空间与维度都是首次上传冻结的事实，任一空白即依赖未配置（503）；调用方期望维度与冻结维度不一致是状态分歧（422），两者都绝不改道。 English summary: the embedding pre-check: alias, space and dimensions are all frozen at first upload, so a blank one is an unconfigured dependency (503) while an expected width that disagrees with the frozen one is a state divergence (422) — neither ever reroutes. */
    private String requireEmbeddingAlias(String kbId, KnowledgeBaseBO base, int expectedDimensions) {
        String alias = StringUtils.trimToNull(base.getEmbeddingModel());
        if (alias == null || StringUtils.isBlank(base.getEmbeddingSpaceId())) {
            log.warn("knowledge embedding alias or space unset kb={} aliasSet={} spaceSet={}",
                    kbId, alias != null, StringUtils.isNotBlank(base.getEmbeddingSpaceId()));
            throw unavailable("The knowledge base freezes no local embedding alias");
        }
        Integer frozen = base.getDimensions();
        if (frozen == null || frozen <= 0) {
            log.warn("knowledge embedding dimensions unset kb={} alias={}", kbId, alias);
            throw unavailable("The knowledge base freezes no embedding dimensions");
        }
        if (frozen.intValue() != expectedDimensions) {
            log.warn("knowledge embedding dimension mismatch kb={} alias={} frozen={} expected={}",
                    kbId, alias, frozen, expectedDimensions);
            throw new CommonException(VALIDATION_FAILED, VALIDATION_FAILED_STATUS,
                    "The expected embedding dimensions disagree with the knowledge base's frozen value");
        }
        return alias;
    }

    /** 中文说明：生成前置复核：chat alias 与出域策略必须已在库内配置；出域许可本身由企业 engine 的渠道策略在同一 SERVICE token 口径下裁决，本实现只保证永不携带自证地址、永不在文档里出现第二个模型名。 English summary: the generation pre-check: the chat alias and egress policy must already be stored; egress permission itself is decided by the engine's channel policy under the same SERVICE token, while this implementation only guarantees that no self-asserted address and no second model name ever leave. */
    private String requireChatAlias(String kbId, KnowledgeBaseBO base) {
        String alias = StringUtils.trimToNull(base.getChatModel());
        if (alias == null || base.getEgressPolicy() == null) {
            log.warn("knowledge chat alias or egress policy unset kb={} aliasSet={} policySet={}",
                    kbId, alias != null, base.getEgressPolicy() != null);
            throw unavailable("The knowledge base freezes no chat alias and egress policy");
        }
        return alias;
    }

    /** 中文说明：唯一出网入口：构建请求体（受字节上限约束）、以 SERVICE token 发 POST、非 2xx 直接 503 且一个上游字节都不回显、2xx 才在 {@code max-response-bytes} 内读成 JSON 对象文档；超时/传输失败/中断同样 503，且不带出上游文本。 English summary: the single egress entry: it serializes the request under the byte ceiling, posts it with the SERVICE token, answers a non-2xx with a 503 that echoes not one upstream byte, and only on 2xx reads a JSON object document within {@code max-response-bytes}; a timeout, transport failure or interruption is a 503 too, without upstream text. */
    private JsonNode post(String kbId, String alias, String pathSuffix, ObjectNode body, String capability) {
        long ceiling = responseCeiling();
        byte[] payload = serialize(body, capability, kbId, alias);
        if (ceiling < 1 || payload.length > ceiling) {
            log.warn("knowledge {} request over ceiling kb={} alias={} bytes={}", capability, kbId, alias, payload.length);
            throw unavailable("The model request exceeds the configured byte ceiling");
        }
        String token = resolveServiceToken(kbId, capability, alias);
        URI endpoint = endpoint(pathSuffix, capability, kbId, alias);
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(readTimeout())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();
        log.info("knowledge {} egress kb={} alias={} bytes={}", capability, kbId, alias, payload.length);
        try {
            HttpResponse<InputStream> response =
                    httpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            try (InputStream upstream = response.body()) {
                if (status < 200 || status > 299) {
                    log.warn("knowledge {} upstream rejected kb={} alias={} status={}",
                            capability, kbId, alias, status);
                    throw unavailable("The enterprise engine rejected the knowledge model call");
                }
                return readDocument(upstream, ceiling, capability, kbId, alias);
            }
        } catch (HttpTimeoutException timedOut) {
            log.warn("knowledge {} upstream timed out kb={} alias={}", capability, kbId, alias);
            throw unavailable("The enterprise engine did not answer the knowledge model call in time");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            log.warn("knowledge {} aborted kb={} alias={}", capability, kbId, alias);
            throw unavailable("The knowledge model call was aborted");
        } catch (IOException transportFailure) {
            log.warn("knowledge {} upstream transport failed kb={} alias={}", capability, kbId, alias);
            throw unavailable("The enterprise engine is unreachable for this knowledge model call");
        }
    }

    /** 中文说明：有界读取 2xx 响应为 JSON 对象文档：先验 Content-Length，再至多读 {@code ceiling + 1} 字节，空文档、越界文档与非对象文档都是 503，绝不退化为空成功。 English summary: reads a 2xx response into a JSON object document under a bound: Content-Length is pre-checked and at most {@code ceiling + 1} bytes are taken, so an empty, oversized or non-object document is a 503 rather than a degraded empty success. */
    private JsonNode readDocument(
            InputStream upstream, long ceiling, String capability, String kbId, String alias) {
        byte[] body;
        try {
            body = upstream.readNBytes((int) Math.min(ceiling, Integer.MAX_VALUE - 1L) + 1);
        } catch (IOException unreadable) {
            log.warn("knowledge {} response unreadable kb={} alias={}", capability, kbId, alias);
            throw unavailable("The knowledge model response could not be read");
        }
        if (body.length == 0 || body.length > ceiling) {
            log.warn("knowledge {} response over ceiling or empty kb={} alias={} bytes={}",
                    capability, kbId, alias, body.length);
            throw unavailable("The knowledge model response is empty or exceeds the configured byte ceiling");
        }
        JsonNode parsed;
        try {
            parsed = objectMapper.readTree(body);
        } catch (IOException unparsable) {
            log.warn("knowledge {} response unparsable kb={} alias={}", capability, kbId, alias);
            throw unavailable("The knowledge model response is not a JSON document");
        }
        if (parsed == null || !parsed.isObject()) {
            log.warn("knowledge {} response is not an object kb={} alias={}", capability, kbId, alias);
            throw unavailable("The knowledge model response is not a native JSON object");
        }
        return parsed;
    }

    /** 中文说明：向量硬不变式核对：条数恰等于文本数、{@code index} 为 0..N-1 无重复全集、每条长度恰等于期望维度、每个分量在 double 与 float 两侧都有限、向量非全零，并按 {@code index} 回填以保持入参顺序；任何不符都是 503，日志只回条数与布尔量，绝不回向量。 English summary: the vector hard-invariant check: the count equals the text count, the indexes form the duplicate-free full set 0..N-1, every vector is exactly the expected width, every component is finite on both the double and the float side and no vector is all zero, and entries are placed by {@code index} to preserve input order; any breach is a 503 logged only as counts and booleans, never as vector data. */
    private List<float[]> requireVectorSet(
            String kbId, String alias, JsonNode document, int expectedEntries, int expectedDimensions) {
        JsonNode data = document == null ? null : document.get(DATA_FIELD);
        if (data == null || !data.isArray() || data.size() != expectedEntries) {
            log.warn("knowledge embedding count rejected kb={} alias={} expectedEntries={} actualEntries={}",
                    kbId, alias, expectedEntries, data == null ? -1 : data.size());
            throw unavailable("The embedding response returned a different number of vectors than the submitted texts");
        }
        float[][] placed = new float[expectedEntries][];
        Set<Integer> seenIndexes = new HashSet<>();
        for (JsonNode entry : data) {
            if (!entry.isObject()) {
                throw rejectedVector(kbId, alias, "shape");
            }
            JsonNode index = entry.get(INDEX_FIELD);
            if (index == null || !index.isIntegralNumber() || !index.canConvertToInt()) {
                throw rejectedVector(kbId, alias, "index");
            }
            int position = index.intValue();
            if (position < 0 || position >= expectedEntries || !seenIndexes.add(position)) {
                throw rejectedVector(kbId, alias, "index");
            }
            JsonNode vector = entry.get(EMBEDDING_FIELD);
            if (vector == null || !vector.isArray() || vector.size() != expectedDimensions) {
                throw rejectedVector(kbId, alias, "dimensions");
            }
            float[] values = new float[expectedDimensions];
            boolean nonZero = false;
            for (int component = 0; component < expectedDimensions; component++) {
                JsonNode item = vector.get(component);
                if (item == null || !item.isNumber() || !Double.isFinite(item.asDouble())) {
                    throw rejectedVector(kbId, alias, "notFinite");
                }
                float cast = (float) item.asDouble();
                if (!Float.isFinite(cast)) {
                    throw rejectedVector(kbId, alias, "notFinite");
                }
                nonZero |= cast != 0f;
                values[component] = cast;
            }
            if (!nonZero) {
                throw rejectedVector(kbId, alias, "zeroVector");
            }
            placed[position] = values;
        }
        List<float[]> vectors = new ArrayList<>(expectedEntries);
        for (int position = 0; position < expectedEntries; position++) {
            if (placed[position] == null) {
                throw rejectedVector(kbId, alias, "missingEntry");
            }
            vectors.add(placed[position]);
        }
        return vectors;
    }

    /** 中文说明：向量核对失败的统一出口：只记 kbId、alias 与失败类别，绝不记向量数值或正文。 English summary: the single vector-check failure exit: it logs only kbId, alias and the failure category, never a vector value or text. */
    private CommonException rejectedVector(String kbId, String alias, String reason) {
        log.warn("knowledge embedding vector rejected kb={} alias={} reason={}", kbId, alias, reason);
        return unavailable("The embedding response violates the local embedding contract");
    }

    /** 中文说明：助手文本核对：{@code choices[0].message.content} 必须存在且非空白，否则 503；日志只到字符数为止。 English summary: the assistant-text check: {@code choices[0].message.content} must be present and non-blank, otherwise a 503, and the log stops at a character count. */
    private String requireAssistantText(String kbId, String alias, JsonNode document) {
        JsonNode choices = document == null ? null : document.get(CHOICES_FIELD);
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            log.warn("knowledge chat response carries no choice kb={} alias={}", kbId, alias);
            throw unavailable("The generation response returned no assistant choice");
        }
        JsonNode content = choices.get(0).path(MESSAGE_FIELD).path(CONTENT_FIELD);
        String answer = content.isTextual() ? content.asText() : null;
        if (StringUtils.isBlank(answer)) {
            log.warn("knowledge chat returned no assistant text kb={} alias={} contentKind={}",
                    kbId, alias, content.getNodeType());
            throw unavailable("The generation response returned no assistant text");
        }
        return answer;
    }

    /**
     * 中文说明：SERVICE token 解析：只接受 {@code serviceTokenRef} 指向的规范化普通文件。纪律是
     * 先拒绝空白与 {@code ..} 穿越，再以父目录 {@code toRealPath()} 作为包含根，把文件真实路径限制在该根内，
     * 因此符号链接逃逸同样被拒；随后要求它是普通文件、字节数不超上限、内容 trim 后非空白且长度合规。
     * 任何一步失败都是 {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE}，日志只到 kbId/capability 为止，
     * <b>绝不</b>输出 token 值、引用名或其路径。
     * English summary: SERVICE token resolution: only the canonicalized regular file behind {@code serviceTokenRef} is
     * accepted. A blank reference and any {@code ..} traversal are refused first, the parent directory's
     * {@link Path#toRealPath()} then becomes the containment root inside which the file's real path must stay (so a
     * symlink escape is caught too), and the file must be regular, within the byte ceiling and non-blank and
     * length-compliant once trimmed. Every breach is {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE} logged only as
     * kbId/capability — never the token value, the reference name or its path.
     *
     * 用法 / Usage: 内部辅助 / Internal helper: {@code resolveServiceToken(kbId, capability, alias)}。
     * @param kbId 参数 知识库 id，仅用于安全日志；parameter knowledge base id, used only in the safe log.
     * @param capability 参数 能力名（embedding/chat），仅用于安全日志；parameter capability name (embedding/chat), log-only.
     * @param alias 参数 目标 alias，仅用于安全日志；parameter target alias, log-only.
     * @return 返回 解析出的 SERVICE token；returns the resolved SERVICE token.
     */
    private String resolveServiceToken(String kbId, String capability, String alias) {
        String reference = StringUtils.trimToNull(modelClientProperties.getServiceTokenRef());
        if (reference == null || reference.contains("..")) {
            throw unresolvableToken(kbId, capability, alias);
        }
        Path absolute;
        Path root;
        Path target;
        try {
            absolute = Path.of(reference).toAbsolutePath().normalize();
            Path parent = absolute.getParent();
            String fileName = absolute.getFileName() == null ? null : absolute.getFileName().toString();
            if (parent == null || fileName == null || fileName.contains("..")) {
                throw new IOException("the service token reference has no contained file name");
            }
            root = parent.toRealPath();
            target = root.resolve(fileName).toRealPath();
        } catch (IOException | RuntimeException unreachable) {
            throw unresolvableToken(kbId, capability, alias);
        }
        if (!target.startsWith(root) || !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw unresolvableToken(kbId, capability, alias);
        }
        long ceiling = responseCeiling();
        try {
            if (ceiling < 1 || Files.size(target) > ceiling) {
                throw unresolvableToken(kbId, capability, alias);
            }
            String token = Files.readString(target, StandardCharsets.UTF_8).trim();
            if (token.isEmpty() || token.length() > MAX_TOKEN_CHARACTERS || token.indexOf('\n') >= 0) {
                throw unresolvableToken(kbId, capability, alias);
            }
            return token;
        } catch (CommonException refused) {
            throw refused;
        } catch (IOException unreadable) {
            throw unresolvableToken(kbId, capability, alias);
        }
    }

    /** 中文说明：凭据不可解析的统一出口：只记 kbId、capability 与 alias，绝不记引用名或值。 English summary: the credential failure exit: it logs kbId, capability and alias only, never the reference name or the value. */
    private CommonException unresolvableToken(String kbId, String capability, String alias) {
        log.warn("knowledge {} service token unresolvable kb={} alias={}", capability, kbId, alias);
        return unavailable("The SERVICE token of the knowledge model client is unavailable");
    }

    /** 中文说明：把配置的 engine 根与固定协议路径段拼成出网 URI：必须绝对 http(s)、有主机、无 userinfo/query/fragment；解析失败只回 503 且绝不回显配置地址。 English summary: joins the configured engine root with the fixed protocol path into the egress URI: absolute http(s) with a host and no userinfo, query or fragment; a parse failure answers 503 and never echoes the configured address. */
    private URI endpoint(String pathSuffix, String capability, String kbId, String alias) {
        String configured = StringUtils.trimToNull(modelClientProperties.getBaseUrl());
        if (configured == null) {
            log.warn("knowledge {} base url unset kb={} alias={}", capability, kbId, alias);
            throw unavailable("The knowledge model client has no configured engine address");
        }
        try {
            URI base = new URI(configured);
            boolean schemeAllowed = "https".equalsIgnoreCase(base.getScheme())
                    || "http".equalsIgnoreCase(base.getScheme());
            if (!base.isAbsolute() || !schemeAllowed || StringUtils.isBlank(base.getHost())
                    || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null) {
                throw new IllegalArgumentException("the configured knowledge engine address is not egressable");
            }
            String root = StringUtils.removeEnd(StringUtils.trimToEmpty(base.getRawPath()), "/");
            String path = (root.isEmpty() ? "" : root) + "/" + pathSuffix;
            return new URI(base.getScheme(), null, base.getHost(), base.getPort(), path, null, null);
        } catch (URISyntaxException | IllegalArgumentException rejected) {
            log.warn("knowledge {} base url unparsable kb={} alias={}", capability, kbId, alias);
            throw unavailable("The configured knowledge engine address cannot be used for egress");
        }
    }

    /** 中文说明：请求体序列化：失败只按 503 收束且不回显文档内容（文档里有提示词与向量口径）。 English summary: serializes the request document: a failure closes out as 503 without echoing the document, which carries the prompt and the vector shape. */
    private byte[] serialize(ObjectNode document, String capability, String kbId, String alias) {
        try {
            return objectMapper.writeValueAsBytes(document);
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException rejected) {
            log.warn("knowledge {} request not serializable kb={} alias={}", capability, kbId, alias);
            throw unavailable("The knowledge model request cannot be serialized");
        }
    }

    /** 中文说明：就地惰性构建 JDK 客户端：连接超时取配置、禁止跟随重定向；配置缺失或非正即 503，绝不采用宽松默认。 English summary: builds the JDK client locally on first use: the connect timeout comes from configuration and redirects are disabled; a missing or non-positive setting is a 503 rather than a permissive default. */
    private HttpClient httpClient() {
        HttpClient built = httpClient;
        if (built != null) {
            return built;
        }
        synchronized (this) {
            if (httpClient == null) {
                Duration connect = modelClientProperties.getConnectTimeout();
                if (connect == null || connect.isZero() || connect.isNegative()) {
                    log.warn("knowledge model client connect timeout unset");
                    throw unavailable("The knowledge model client has no valid connect timeout");
                }
                httpClient = HttpClient.newBuilder()
                        .connectTimeout(connect)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
            }
            return httpClient;
        }
    }

    /** 中文说明：整体读超时预算：缺失或非正即 503，绝不让出站调用无限等待。 English summary: the overall read budget: a missing or non-positive value is a 503 so an egress call never waits indefinitely. */
    private Duration readTimeout() {
        Duration read = modelClientProperties.getReadTimeout();
        if (read == null || read.isZero() || read.isNegative()) {
            log.warn("knowledge model client read timeout unset");
            throw unavailable("The knowledge model client has no valid read timeout");
        }
        return read;
    }

    /** 中文说明：响应字节上限：缺失或非正即 503，与 {@code max-response-bytes} 同键。 English summary: the response byte ceiling: a missing or non-positive value is a 503, the same key as {@code max-response-bytes}. */
    private long responseCeiling() {
        long ceiling = modelClientProperties.getMaxResponseBytes();
        if (ceiling < 1) {
            log.warn("knowledge model client response ceiling unset");
            throw unavailable("The knowledge model client has no valid response byte ceiling");
        }
        return ceiling;
    }

    /** 中文说明：依赖不可用的固定异常：只携带稳定机器码与安全说明，绝不拼接上游正文、地址或凭据。 English summary: the fixed unavailable-dependency exception: a stable machine code and a safe description only, never upstream text, an address or a credential. */
    private CommonException unavailable(String description) {
        return new CommonException(MODEL_UNAVAILABLE, MODEL_UNAVAILABLE_STATUS, description);
    }

    /** 中文说明：单调时钟耗时（毫秒），只用于运维日志。 English summary: elapsed monotonic milliseconds, for the operational log only. */
    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
