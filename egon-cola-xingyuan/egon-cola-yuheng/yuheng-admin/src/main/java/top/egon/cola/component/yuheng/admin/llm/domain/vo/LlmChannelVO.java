package top.egon.cola.component.yuheng.admin.llm.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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

/**
 * 中文说明：{@code LlmChannelVO} 是原 API-004/005 的完整渠道投影；只暴露 secretRef 引用名，
 * 不含任何已解析密钥值，也不含内部 id/tenant/version/审计列。
 * English summary: {@code LlmChannelVO} is the complete API-004/005 channel projection; it carries the
 * secretRef name only and never the resolved secret or internal persistence columns.
 *
 * 用法 / Usage: 由渠道 Converter 的 forward 投影方法填充；{@code revision} 是已提交乐观版本，正整数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class LlmChannelVO {

    /** 稳定配置键，创建后不改名 / stable key, immutable after creation. */
    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
    private String key;

    /** 展示名称 / trimmed display name. */
    @NotBlank
    @Size(max = 128)
    private String name;

    /** 部署形态 / trusted deployment. */
    @NotNull
    private LlmDeploymentEnum deployment;

    /** 上游协议 / upstream protocol. */
    @NotNull
    private LlmProtocolEnum protocol;

    /** 受白名单约束的绝对 API 根地址 / whitelisted absolute API root. */
    @NotBlank
    @Size(max = 2048)
    private String baseUrl;

    /** 密钥引用名，LOCAL 无认证时为 null，永不含密钥值 / secret reference name, null only for auth-free LOCAL. */
    @Schema(nullable = true)
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

    /** 已提交乐观版本，正整数 / committed optimistic revision, positive. */
    @NotNull
    @Min(1)
    private Long revision;
}
