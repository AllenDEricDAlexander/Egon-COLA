package top.egon.cola.component.yuheng.admin.llm.domain.dto;

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
 * 中文说明：{@code LlmChannelCommandDTO} 是 CQE Command 载体，承载原 API-005 完整保存渠道的请求体；
 * 同一路径同时覆盖创建（{@code expectedRevision=0}）与完整替换，因此字段合同在两种意图下保持一致。
 * English summary: {@code LlmChannelCommandDTO} is the CQE command carrier of the API-005 full channel
 * replace request, shared by creation (expectedRevision 0) and replacement with one identical field contract.
 *
 * 用法 / Usage: Controller/Service 以注解校验并按 Create/Update 分组复用；
 * {@code totalTimeoutMs} 不小于其余三个 timeout 的关系是跨字段规则，由业务入口与 Service 复核，
 * secretRef 只承载引用名，任何层都不得携带已解析密钥值。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class LlmChannelCommandDTO {

    /** 稳定配置键，创建后不改名 / stable key, 1–64 lowercase alphanumeric or hyphen. */
    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
    private String key;

    /** 展示名称，trim 后 1–128 字符 / trimmed display name. */
    @NotBlank
    @Size(max = 128)
    private String name;

    /** 部署形态，决定候选出域策略 / trusted deployment used by egress policy. */
    @NotNull
    private LlmDeploymentEnum deployment;

    /** 上游协议四选一 / upstream protocol. */
    @NotNull
    private LlmProtocolEnum protocol;

    /** 白名单约束的绝对 API 根地址，禁止 userinfo/query/fragment / absolute API root, varchar(2048). */
    @NotBlank
    @Size(max = 2048)
    private String baseUrl;

    /** 部署密钥引用名，仅 LOCAL 无认证时可为 null，绝不含密钥值 / secret reference name only, null only for auth-free LOCAL. */
    @Schema(nullable = true)
    @Size(max = 128)
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9/_-]{0,127}$")
    private String secretRef;

    /** 是否用于新调用 / enablement for new calls. */
    @NotNull
    private Boolean enabled;

    /** 连接超时 1–10000ms / connect timeout in milliseconds. */
    @NotNull
    @Min(1)
    @Max(10_000)
    private Integer connectTimeoutMs;

    /** 响应头超时 1–120000ms / header timeout in milliseconds. */
    @NotNull
    @Min(1)
    @Max(120_000)
    private Integer headerTimeoutMs;

    /** 流空闲超时 1–120000ms / idle timeout in milliseconds. */
    @NotNull
    @Min(1)
    @Max(120_000)
    private Integer idleTimeoutMs;

    /** 全请求含重试截止 1–600000ms，且不小于其余 timeout / total deadline in milliseconds. */
    @NotNull
    @Min(1)
    @Max(600_000)
    private Integer totalTimeoutMs;

    /** 单 engine/channel 最大并发 1–256，非全局配额 / per engine-channel concurrency limit. */
    @NotNull
    @Min(1)
    @Max(256)
    private Integer maxConcurrent;

    /** 创建为 0，替换须等于当前 revision / 0 on create, current revision on replace. */
    @NotNull
    @Min(0)
    private Long expectedRevision;
}
