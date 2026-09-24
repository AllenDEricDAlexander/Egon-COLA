package top.egon.cola.component.yuheng.llm.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：{@code LlmGatewayProperties} 是 LLM engine 的运行边界配置：出网白名单、密钥挂载根、请求/帧上限、尝试次数、
 * 流式线程数与本进程的资源标识。它与 admin、MCP 两进程使用同一组键名（{@code yuheng.llm.*}），三个 profile 必须全等，
 * 因为「哪台主机可以出网」是安全事实而不是调优参数。这里<b>没有</b>任何宽松默认值：{@code enabled} 缺省为假，白名单缺省
 * 为空即「不调用」，字节/次数/线程数为必填正数，缺失即在绑定阶段失败关闭；{@code secrets-root} 只接受根之下的挂载路径，
 * 不允许 {@code ..}；身份 {@code resource-uri}/{@code service-token-audience} 由外部注入，没有硬编码兜底。
 * 部署租户与 schema 指纹不在本类，它们已由 {@link LlmPersistenceProperties} 的 {@code yuheng.persistence.*} 唯一持有。
 * English summary: {@code LlmGatewayProperties} is the runtime boundary of the LLM engine: the outbound allow-lists, the
 * secret mount root, request/frame caps, attempt count, streaming thread count and this process's resource identity. It
 * uses the same {@code yuheng.llm.*} key names as the admin and MCP processes and must be identical across all three
 * profiles, because "which host may be reached" is a security fact rather than a tuning knob. Nothing here is permissive:
 * {@code enabled} defaults to false, empty allow-lists mean "make no call", byte/attempt/thread caps are required positive
 * numbers so a missing value fails closed at binding, {@code secrets-root} accepts only a non-traversing path under a
 * root, and the identity {@code resource-uri}/{@code service-token-audience} are injected externally with no hardcoded
 * fallback. Deployment tenancy and schema fingerprint are <b>not</b> here; {@code yuheng.persistence.*} in
 * {@link LlmPersistenceProperties} remains their single owner.
 *
 * 用法 / Usage: 由 {@code LlmGatewayConfiguration} 启用，被路由选择与错误编码器按 bean 名注入；
 * 业务代码不得据 {@code enabled} 放宽授权，它只决定引擎是否出网。
 */
@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ConfigurationProperties(prefix = "yuheng.llm", ignoreUnknownFields = false)
public class LlmGatewayProperties {

    /** 中文说明：{@code @EnableConfigurationProperties} 生成的 bean 名（{@code prefix-全限定类名}），注入点据此消歧。
     *  English summary: the bean name {@code @EnableConfigurationProperties} generates ({@code prefix-FQCN}) that injection points use to disambiguate. */
    public static final String BEAN_NAME = "yuheng.llm-top.egon.cola.component.yuheng.llm.config.LlmGatewayProperties";

    /** 中文说明：引擎出网总开关，缺省为假；为假时任何模型调用都在路由阶段就被 503 拒绝。 English summary: the master egress switch, false unless configured; when false every call is refused with 503 during routing. */
    private boolean enabled;

    /** 中文说明：LOCAL 渠道允许的私网 CIDR 或主机名；空列表表示本地渠道一律不可用。 English summary: private CIDRs or host names a LOCAL channel may use; an empty list makes every LOCAL channel ineligible. */
    @Size(max = 64)
    private List<@NotBlank @Size(max = 253) @Pattern(regexp =
            "^((25[0-5]|2[0-4][0-9]|1?[0-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1?[0-9]?[0-9])/(3[0-2]|[12]?[0-9])$"
                    + "|^[a-z0-9][a-z0-9.-]{0,252}$") String> allowedLocalCidrs = new ArrayList<>();

    /** 中文说明：CLOUD 渠道允许的主机名白名单；空列表表示云端渠道一律不可用，与 embedding 无关。 English summary: the host allow-list CLOUD channels may use; an empty list makes every CLOUD channel ineligible, independent of embedding. */
    @Size(max = 256)
    private List<@NotBlank @Size(max = 253) @Pattern(regexp = "^[a-z0-9][a-z0-9.-]{0,252}$") String>
            allowedCloudHosts = new ArrayList<>();

    /** 中文说明：secretRef 解析的挂载根；secretRef 是引用名，解析值永不写入配置或日志。 English summary: the mount root secretRef resolves against; a secretRef is a name whose value is never stored in configuration or logs. */
    @Pattern(regexp = "^/(?!.*\\.\\.)[A-Za-z0-9._/-]{0,127}$")
    private String secretsRoot;

    /** 中文说明：整个请求体上限（字节），与协议面的 2MiB 合同一致。 English summary: the whole-request body cap in bytes, matching the protocol face's 2MiB contract. */
    @Min(1_048_576)
    @Max(8_388_608)
    private long maxRequestBytes;

    /** 中文说明：单个协议帧上限（字节），与协议面的 1MiB/frame 合同一致。 English summary: the per-frame cap in bytes, matching the protocol face's 1MiB-per-frame contract. */
    @Min(65_536)
    @Max(4_194_304)
    private int maxFrameBytes;

    /** 中文说明：一个请求允许的安全尝试次数上限，最多两次且提交后不重试。 English summary: the cap on safe attempts for one request, at most two and never after commit. */
    @Min(1)
    @Max(2)
    private int maximumAttempts;

    /** 中文说明：流式输出线程数，硬上限 64，零等待队列，满时在提交前 429。 English summary: the streaming output thread count, hard-capped at 64 with a zero-wait queue that 429s before submission when saturated. */
    @Min(1)
    @Max(64)
    private int streamingThreads;

    /** 中文说明：本进程的受管资源标识，用于 SERVICE token 的 audience 校验。 English summary: this process's managed resource identity, used to check the SERVICE token audience. */
    @NotNull
    @Valid
    private Identity identity;

    /**
     * 中文说明：返回 allowedLocalCidrs 的不可变快照副本，避免路由阶段并发改写白名单。
     * English summary: Returns an immutable snapshot of the LOCAL allow-list so routing can never observe a concurrent
     * widening of the allow-list.
     */
    public List<String> getAllowedLocalCidrs() {
        return allowedLocalCidrs == null
                ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(allowedLocalCidrs));
    }

    /**
     * 中文说明：写入时复制 allowedLocalCidrs。
     * English summary: Defensively copies the incoming LOCAL allow-list.
     */
    public LlmGatewayProperties setAllowedLocalCidrs(List<String> allowedLocalCidrs) {
        this.allowedLocalCidrs = allowedLocalCidrs == null ? new ArrayList<>() : new ArrayList<>(allowedLocalCidrs);
        return this;
    }

    /**
     * 中文说明：返回 allowedCloudHosts 的不可变快照副本。
     * English summary: Returns an immutable snapshot of the CLOUD host allow-list.
     */
    public List<String> getAllowedCloudHosts() {
        return allowedCloudHosts == null
                ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(allowedCloudHosts));
    }

    /**
     * 中文说明：写入时复制 allowedCloudHosts。
     * English summary: Defensively copies the incoming CLOUD host allow-list.
     */
    public LlmGatewayProperties setAllowedCloudHosts(List<String> allowedCloudHosts) {
        this.allowedCloudHosts = allowedCloudHosts == null ? new ArrayList<>() : new ArrayList<>(allowedCloudHosts);
        return this;
    }

    /**
     * 中文说明：启用出网时必须同时给出密钥挂载根与资源标识，否则一个「开着但没有凭据根」的进程会在第一次调用时才失败；
     * 这里把它提前到绑定阶段。关闭时允许缺省，便于只跑管理面的安装域。
     * English summary: When egress is enabled the secret mount root and the resource identity must both be present,
     * otherwise a process that is "on but has no secret root" would only fail on its first call; this moves that failure
     * into binding. While disabled both may stay absent, which suits installations that run only the management face.
     */
    @AssertTrue(message = "yuheng.llm.secrets-root and yuheng.llm.identity.* are required when yuheng.llm.enabled is true")
    public boolean isManagedMaterialConfigured() {
        return !enabled || (secretsRoot != null && !secretsRoot.isBlank() && identity != null);
    }

    /**
     * 中文说明：{@code yuheng.llm.identity.*} 子组：本进程被注册为哪个受管资源、SERVICE token 必须签发给哪个 audience。
     * 两个值都由外部注入，缺省即失败关闭，因为它们决定「谁在调用」。
     * English summary: The {@code yuheng.llm.identity.*} group: which managed resource this process is registered as and
     * which audience a SERVICE token must be issued for. Both are externally injected and fail closed when absent,
     * because they decide who is calling.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class Identity {

        /** 中文说明：本引擎注册的资源 URI，必须是 HTTPS 绝对地址且无 userinfo/query/fragment。 English summary: this engine's registered resource URI: an HTTPS absolute URL without userinfo, query or fragment. */
        @NotBlank
        @Size(max = 2048)
        @Pattern(regexp = "^https://[A-Za-z0-9._~-]{1,253}(:[0-9]{1,5})?(/[A-Za-z0-9._~/-]{0,255}){0,8}$")
        private String resourceUri;

        /** 中文说明：SERVICE token 必须携带的 audience，不接受调用方自报。 English summary: the SERVICE token audience that must be presented; never self-declared by the caller. */
        @NotBlank
        @Size(max = 128)
        private String serviceTokenAudience;
    }
}
