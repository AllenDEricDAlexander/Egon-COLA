package top.egon.cola.component.yuheng.admin.llm.domain.bo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmProtocolEnum;

import java.time.Instant;

/**
 * 中文说明：{@code LlmChannelBO} 是 {@code gateway_llm_channel} 的业务载体，承载渠道配置在领域与仓储之间流动的
 * 全部业务事实（含服务端权威的 {@code revision} 与投影自 MP 审计列的 {@code createdAt}/{@code updatedAt}）；
 * 它不继承 {@code EgonModel}，不携带任何持久化列注解，也不承载租户与操作者列——那些技术事实只属于持久边界。
 * English summary: {@code LlmChannelBO} is the business carrier of {@code gateway_llm_channel}, holding every channel
 * configuration fact that moves between the domain and the repository, including the server-authoritative
 * {@code revision} and the {@code createdAt}/{@code updatedAt} projections of the MyBatis-Plus audit columns; it neither
 * extends {@code EgonModel} nor carries column annotations, and it deliberately omits the tenant and operator columns
 * that belong to the persistence boundary only.
 *
 * 用法 / Usage: 由 {@code LlmChannelPersistenceConverter} 与行模型互转，并由
 * {@code LlmConfigurationRepository} 端口读写；约束与 API-004/005 的 {@code LlmChannelVO}/{@code LlmChannelCommandDTO}
 * 保持同一口径（同一字段只有一处上界定义），{@code secretRef} 只承载引用名，任何字段都不携带已解析密钥值。
 * Convert to and from the row model only through {@code LlmChannelPersistenceConverter} and move it through the
 * {@code LlmConfigurationRepository} port; the constraints stay on the same footing as API-004/005's
 * {@code LlmChannelVO} and {@code LlmChannelCommandDTO}, and {@code secretRef} names a reference instead of a secret.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class LlmChannelBO {

    /** 不透明主键的十进制文本，插入前为 null / decimal text of the assigned key, null before an insert. */
    private String id;

    /** 稳定配置键，创建后不改名，与 {@code gateway_llm_channel.channel_key} 同列 / stable channel key, immutable after creation. */
    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
    private String channelKey;

    /** 展示名称 / trimmed display name. */
    @NotBlank
    @Size(max = 128)
    private String name;

    /** 部署形态，决定候选出域策略 / trusted deployment driving egress policy. */
    @NotNull
    private LlmDeploymentEnum deployment;

    /** 上游协议 / upstream protocol. */
    @NotNull
    private LlmProtocolEnum protocol;

    /** 白名单约束的绝对 API 根地址 / whitelisted absolute API root. */
    @NotBlank
    @Size(max = 2048)
    private String baseUrl;

    /** 密钥引用名，LOCAL 无认证时为 null，永不含密钥值 / secret reference name, null only for auth-free LOCAL. */
    @Size(max = 128)
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9/_-]{0,127}$")
    private String secretRef;

    /** 是否用于新调用 / enablement for new calls. */
    @NotNull
    private Boolean enabled;

    /** 连接超时 ms / connect timeout in milliseconds. */
    @NotNull
    @Min(1)
    @Max(10_000)
    private Integer connectTimeoutMs;

    /** 响应头超时 ms / header timeout in milliseconds. */
    @NotNull
    @Min(1)
    @Max(120_000)
    private Integer headerTimeoutMs;

    /** 流空闲超时 ms / idle timeout in milliseconds. */
    @NotNull
    @Min(1)
    @Max(120_000)
    private Integer idleTimeoutMs;

    /** 全请求截止 ms / total request deadline in milliseconds. */
    @NotNull
    @Min(1)
    @Max(600_000)
    private Integer totalTimeoutMs;

    /** 单 engine/channel 最大并发 / per engine-channel concurrency limit. */
    @NotNull
    @Min(1)
    @Max(256)
    private Integer maxConcurrent;

    /** 调用方期望的乐观版本：创建意图为 0 哨兵值，替换为库中现值，落库后由仓储回写权威值 / caller expectation, 0 on create intent and the stored revision on replace, overwritten by the authoritative value after a save. */
    @Min(0)
    private long revision;

    /** 投影自 {@code create_time} 的创建时刻 / creation instant projected from {@code create_time}. */
    private Instant createdAt;

    /** 投影自 {@code update_time} 的更新时刻 / update instant projected from {@code update_time}. */
    private Instant updatedAt;
}
