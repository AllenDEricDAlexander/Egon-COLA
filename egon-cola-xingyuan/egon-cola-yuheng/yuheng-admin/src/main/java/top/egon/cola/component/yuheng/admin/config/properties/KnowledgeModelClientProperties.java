package top.egon.cola.component.yuheng.admin.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * 中文说明：{@code KnowledgeModelClientProperties} 是知识面调用企业 engine 的边界配置
 * （Spec §15 {@code yuheng.knowledge.model-client.*}：URL 与资源必填，SERVICE token 是外部 secret，
 * 正文永不入日志）。这里刻意<b>没有</b>任何宽松默认：{@link #baseUrl} 与 {@link #serviceTokenRef} 没有兜底值，
 * 未配置就是 null，一旦被使用即失败关闭；{@code https} 前缀、无 userinfo/query/fragment 的地址形状由
 * {@code @Pattern} 守住，与 {@code LlmGatewayProperties.Identity#resourceUri} 同一条形状规则，因此不可能被
 * {@code http://attacker} 或带凭据的 URL 绕过；{@link #serviceTokenRef} 只是<b>引用名</b>，字符集里既没有
 * {@code /} 也没有 {@code ..}，所以它无法被改写成任意文件路径，其解析值也永不进入本类、配置或日志。
 * 必填项<b>不</b>在绑定期用 {@code @NotBlank} 拒绝，因为知识能力默认关闭（{@code yuheng.knowledge.enabled=false}），
 * 让一个未启用的能力缺 URL 就拖垮整个容器启动是错误方向的严格；取而代之的是 {@link #requireUsable()}：
 * 客户端在第一次出网前必须调用它，缺失/空白即以稳定机器码抛出，绝不回落到任何默认 URL。
 * 本类前缀是叶子（{@code yuheng.knowledge.model-client}），因此可以安全地收紧未知键，让拼错的键在启动期就暴露。
 * English summary: {@code KnowledgeModelClientProperties} is the boundary configuration the knowledge face uses to reach
 * the enterprise engine (Spec §15 {@code yuheng.knowledge.model-client.*}: the URL and the resource are mandatory, the
 * SERVICE token is an external secret and bodies never reach the log). Nothing here is permissive: {@link #baseUrl} and
 * {@link #serviceTokenRef} have no fallback, an unset value stays null and fails closed the moment it is used, and the
 * {@code https} prefix plus the userinfo/query/fragment-free shape is held by {@code @Pattern} using the very same rule
 * as {@code LlmGatewayProperties.Identity#resourceUri}, so neither {@code http://attacker} nor a credential-bearing URL
 * can slip through. {@link #serviceTokenRef} is only a <b>reference name</b> whose character set contains neither
 * {@code /} nor {@code ..}, so it cannot be rewritten into an arbitrary file path, and its resolved value never enters
 * this class, the configuration or the log. The mandatory pair is deliberately <b>not</b> rejected with
 * {@code @NotBlank} during binding because knowledge is disabled by default
 * ({@code yuheng.knowledge.enabled=false}); letting an unenabled capability break the whole container start for a missing
 * URL is strictness pointing the wrong way. {@link #requireUsable()} replaces that: the client must call it before its
 * first egress, a missing or blank value then raises a stable machine code, and no default URL is ever consulted. Since
 * this class owns a leaf prefix ({@code yuheng.knowledge.model-client}) unknown keys can be refused safely here, which
 * surfaces a typo at startup instead of at the first call.
 *
 * 用法 / Usage: 由 {@code KnowledgeConfiguration} 以 {@code @EnableConfigurationProperties} 注册，注入点用
 * {@link #BEAN_NAME} 消歧；{@code KnowledgeModelClientServiceImpl} 每次调用前先 {@link #requireUsable()}，
 * 并把抛出的 {@link IllegalStateException} 收口为 {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE}，不回显上游正文。
 */
@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@ConfigurationProperties(prefix = "yuheng.knowledge.model-client", ignoreUnknownFields = false)
public class KnowledgeModelClientProperties {

    /** 中文说明：{@code @EnableConfigurationProperties} 生成的 bean 名（{@code prefix-全限定类名}），注入点据此消歧。
     *  English summary: the bean name {@code @EnableConfigurationProperties} generates ({@code prefix-FQCN}) that injection points use to disambiguate. */
    public static final String BEAN_NAME
            = "yuheng.knowledge.model-client-top.egon.cola.component.yuheng.admin.config.properties"
            + ".KnowledgeModelClientProperties";

    /** 中文说明：机器码：engine 地址缺失或空白，第一次出网即失败关闭。 English summary: machine code: the engine base URL is absent or blank, so the first egress fails closed. */
    public static final String MISSING_BASE_URL_CODE = "KNOWLEDGE_MODEL_CLIENT_BASE_URL_MISSING";

    /** 中文说明：机器码：SERVICE token 引用缺失或空白；引用的解析值本身从不参与比较，也不入日志。 English summary: machine code: the SERVICE token reference is absent or blank; the resolved value itself never takes part in the comparison and never reaches the log. */
    public static final String MISSING_TOKEN_REF_CODE = "KNOWLEDGE_MODEL_CLIENT_TOKEN_REF_MISSING";

    /** 中文说明：engine 的 HTTPS 绝对基址（{@code POST /v1/embeddings}、{@code POST /v1/chat/completions} 都从它拼出）；无默认值，形状见类说明。 English summary: the absolute HTTPS base URL of the engine, from which {@code POST /v1/embeddings} and {@code POST /v1/chat/completions} are built; no default, shape explained on the type. */
    @Size(max = 2048)
    @Pattern(regexp = "^https://[A-Za-z0-9._~-]{1,253}(:[0-9]{1,5})?(/[A-Za-z0-9._~/-]{0,255}){0,8}$")
    private String baseUrl;

    /** 中文说明：SERVICE token 的部署侧引用名（不是 token 值），只允许无分隔符的短名，因此无法越出 allowlist 指向任意文件。 English summary: the deployment-side reference NAME of the SERVICE token, never the token value, restricted to a short separator-free segment so it cannot escape its allowlist into an arbitrary file. */
    @Size(max = 256)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._~-]{0,255}$")
    private String serviceTokenRef;

    /** 中文说明：连接超时（Spec 请求限额：连接 3s），必须为正；它与 {@link #readTimeout} 一起构成有界 deadline，绝不无限等待。 English summary: the connect timeout (Spec request limit: 3s to connect), required positive; together with {@link #readTimeout} it forms the bounded deadline that never waits indefinitely. */
    @NotNull
    @Builder.Default
    private Duration connectTimeout = Duration.ofSeconds(3);

    /** 中文说明：读取超时（Spec 请求限额：响应头 30s；模型调用总预算 120s 由调用侧的整体 deadline 收口），必须为正。 English summary: the read timeout (Spec request limit: 30s for response headers, while the 120s total model budget closes on the caller's overall deadline), required positive. */
    @NotNull
    @Builder.Default
    private Duration readTimeout = Duration.ofSeconds(30);

    /** 中文说明：单个响应体字节上限（默认 8MiB），用于杜绝「无无界内存聚合」：64 条向量的响应也必须被截断在有限预算内，越限即失败而不是先吃进堆。 English summary: the per-response body cap (8MiB by default) that rules out unbounded in-memory aggregation — even a 64-vector response is cut off at a finite budget, failing rather than being absorbed by the heap. */
    @Min(1_048_576)
    @Max(33_554_432)
    @Builder.Default
    private long maxResponseBytes = 8_388_608L;

    /**
     * 中文说明：首次使用前的强制自检：把「URL 与 SERVICE token 引用必填」这条 Spec 合同落在真正出网之前，
     * 而不是靠宽松默认值让请求发往某个兜底地址。抛出的消息只含机器码与键名，绝不含 {@link #baseUrl} 的查询片段、
     * {@link #serviceTokenRef} 的解析值或任何凭据；超时与非零响应上限属于形状约束，已由类级
     * {@code @Validated} 在绑定阶段守住，因此这里只补绑定守不住的两项。
     * English summary: The mandatory pre-use self-check that lands the Spec's "the URL and the SERVICE token reference are
     * required" contract before a request actually leaves the process rather than letting a permissive default send it to
     * some fallback address. The raised message carries only the machine code and the key name — never a query fragment of
     * {@link #baseUrl}, never the resolved value of {@link #serviceTokenRef}, never a credential. Timeouts and the non-zero
     * response cap are shape constraints already held by the class-level {@code @Validated} during binding, so only the two
     * things binding cannot hold are added here.
     *
     * 用法 / Usage: {@code KnowledgeModelClientServiceImpl} 在构造任何 HTTP 请求之前调用一次；
     * 调用方把 {@link IllegalStateException} 映射为 {@code 503 KNOWLEDGE_MODEL_UNAVAILABLE}。
     * @throws IllegalStateException 基址或 token 引用为空白；a blank base URL or token reference.
     */
    public void requireUsable() {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException(MISSING_BASE_URL_CODE
                    + ": yuheng.knowledge.model-client.base-url is mandatory and has no default");
        }
        if (serviceTokenRef == null || serviceTokenRef.isBlank()) {
            throw new IllegalStateException(MISSING_TOKEN_REF_CODE
                    + ": yuheng.knowledge.model-client.service-token-ref is mandatory and has no default");
        }
    }
}
