package top.egon.cola.component.yuheng.admin.llm.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
import top.egon.cola.component.yuheng.admin.llm.domain.dto.LlmRouteBindingDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmModelKindEnum;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmProtocolEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：{@code LlmModelVO} 是原 API-006/007 的完整模型投影，枚举按原 wire 字符串输出，
 * routes 复用 {@link LlmRouteBindingDTO} 一条顶层载体；不含内部 id/tenant/version/审计列。
 * English summary: {@code LlmModelVO} is the complete API-006/007 model projection with wire-string enums and
 * routes reusing the top-level {@link LlmRouteBindingDTO}; internal persistence columns stay absent.
 *
 * 用法 / Usage: 集合 getter 返回不可变快照，避免响应对象被下游修改；
 * CHAT 的 dimensions/embeddingSpaceId 保持 null，不用空字符串代替。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class LlmModelVO {

    /** 稳定配置键 / stable key. */
    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
    private String key;

    /** 展示名称 / trimmed display name. */
    @NotBlank
    @Size(max = 128)
    private String name;

    /** CHAT 或 EMBEDDING / model kind. */
    @NotNull
    private LlmModelKindEnum kind;

    /** 支持入口协议集合，1–4 唯一 / supported ingress protocols. */
    @NotNull
    @Size(min = 1, max = 4)
    private List<@NotNull LlmProtocolEnum> protocols;

    /** 是否用于新调用 / enablement for new calls. */
    @NotNull
    private Boolean enabled;

    /** 嵌入维度，CHAT 为 null / embedding dimensions, null for CHAT. */
    @Schema(nullable = true)
    @Min(1)
    @Max(16_000)
    private Integer dimensions;

    /** 不可变嵌入空间标识，CHAT 为 null / embedding space identity, null for CHAT. */
    @Schema(nullable = true)
    @Size(max = 128)
    private String embeddingSpaceId;

    /** 已验证 SERVICE subject 列表，1–100 / verified service subjects. */
    @NotNull
    @Size(min = 1, max = 100)
    private List<@NotBlank String> allowedSubjects;

    /** 显式渠道映射，1–16 / explicit route bindings. */
    @NotNull
    @Size(min = 1, max = 16)
    @Valid
    private List<LlmRouteBindingDTO> routes;

    /** 已提交乐观版本，正整数 / committed optimistic revision, positive. */
    @NotNull
    @Min(1)
    private Long revision;

    /**
     * 中文说明：返回 protocols 的不可变快照副本。
     * English summary: Returns an immutable snapshot copy of the protocols list.
     */
    public List<LlmProtocolEnum> getProtocols() {
        return protocols == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(protocols));
    }

    /**
     * 中文说明：写入时复制 protocols，响应集合永不 null。
     * English summary: Defensively copies the incoming protocols list; the wire value is never null.
     */
    public LlmModelVO setProtocols(List<LlmProtocolEnum> protocols) {
        this.protocols = protocols == null ? null : new ArrayList<>(protocols);
        return this;
    }

    /**
     * 中文说明：返回 allowedSubjects 的不可变快照副本。
     * English summary: Returns an immutable snapshot copy of the allowed subjects.
     */
    public List<String> getAllowedSubjects() {
        return allowedSubjects == null
                ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(allowedSubjects));
    }

    /**
     * 中文说明：写入时复制 allowedSubjects。
     * English summary: Defensively copies the incoming allowed subjects.
     */
    public LlmModelVO setAllowedSubjects(List<String> allowedSubjects) {
        this.allowedSubjects = allowedSubjects == null ? null : new ArrayList<>(allowedSubjects);
        return this;
    }

    /**
     * 中文说明：返回 routes 的不可变快照副本。
     * English summary: Returns an immutable snapshot copy of the route bindings.
     */
    public List<LlmRouteBindingDTO> getRoutes() {
        return routes == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(routes));
    }

    /**
     * 中文说明：写入时复制 routes。
     * English summary: Defensively copies the incoming route bindings.
     */
    public LlmModelVO setRoutes(List<LlmRouteBindingDTO> routes) {
        this.routes = routes == null ? null : new ArrayList<>(routes);
        return this;
    }
}
