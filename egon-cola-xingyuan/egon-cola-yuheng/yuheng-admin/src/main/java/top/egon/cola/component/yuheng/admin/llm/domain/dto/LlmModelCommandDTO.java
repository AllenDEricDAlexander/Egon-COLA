package top.egon.cola.component.yuheng.admin.llm.domain.dto;

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
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmModelKindEnum;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.admin.llm.validation.ValidLlmModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：{@code LlmModelCommandDTO} 是 CQE Command 载体，承载原 API-007 完整保存模型 alias 的请求体；
 * 同一路径覆盖创建与完整替换，字段合同一致，另由类级 {@link ValidLlmModel} 复核 kind/dimensions/space/routes 的跨字段规则。
 * English summary: {@code LlmModelCommandDTO} is the CQE command carrier of the API-007 full model replace
 * request; the class-level {@link ValidLlmModel} constraint adds the cross-field kind consistency rules.
 *
 * 用法 / Usage: 集合字段在 setter/getter 上做防御性快照；重复协议/主体不静默合并；
 * 未识别的枚举 wire 字符串由 {@code @JsonCreator} 直接拒绝并映射 422。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@ValidLlmModel
public class LlmModelCommandDTO {

    /** 稳定配置键，创建后不改名 / stable key, immutable after creation. */
    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
    private String key;

    /** 展示名称，trim 后 1–128 字符 / trimmed display name. */
    @NotBlank
    @Size(max = 128)
    private String name;

    /** CHAT 或 EMBEDDING，决定空间字段是否为空 / model kind driving the space fields. */
    @NotNull
    private LlmModelKindEnum kind;

    /** 支持入口协议集合 1–4 唯一 / supported ingress protocols, 1–4 unique. */
    @NotNull
    @Size(min = 1, max = 4)
    private List<@NotNull LlmProtocolEnum> protocols;

    /** 是否用于新调用 / enablement for new calls. */
    @NotNull
    private Boolean enabled;

    /** 嵌入维度 1..16000，CHAT 必须为 null / embedding dimensions, null for CHAT. */
    @Schema(nullable = true)
    @Min(1)
    @Max(16_000)
    private Integer dimensions;

    /** 不可变嵌入空间版本标识，CHAT 必须为 null，不能仅用维度代替 / immutable embedding space identity, null for CHAT. */
    @Schema(nullable = true)
    @Size(min = 1, max = 128)
    private String embeddingSpaceId;

    /** 可调用此模型的已验证 SERVICE subject，1–100，非请求方自报 / verified service subjects, 1–100. */
    @NotNull
    @Size(min = 1, max = 100)
    private List<@NotBlank String> allowedSubjects;

    /** 显式模型渠道映射 1–16，无匹配不扩大到全部渠道 / explicit route bindings, 1–16. */
    @NotNull
    @Size(min = 1, max = 16)
    @Valid
    private List<LlmRouteBindingDTO> routes;

    /** 创建为 0，替换须等于当前 revision / 0 on create, current revision on replace. */
    @NotNull
    @Min(0)
    private Long expectedRevision;

    /**
     * 中文说明：返回 protocols 的不可变快照副本。
     * English summary: Returns an immutable snapshot copy of the protocols list.
     */
    public List<LlmProtocolEnum> getProtocols() {
        return protocols == null ? null : Collections.unmodifiableList(new ArrayList<>(protocols));
    }

    /**
     * 中文说明：写入时复制 protocols，避免异步任务期间被外部修改。
     * English summary: Defensively copies the incoming protocols list.
     */
    public LlmModelCommandDTO setProtocols(List<LlmProtocolEnum> protocols) {
        this.protocols = protocols == null ? null : new ArrayList<>(protocols);
        return this;
    }

    /**
     * 中文说明：返回 allowedSubjects 的不可变快照副本。
     * English summary: Returns an immutable snapshot copy of the allowed subjects.
     */
    public List<String> getAllowedSubjects() {
        return allowedSubjects == null ? null : Collections.unmodifiableList(new ArrayList<>(allowedSubjects));
    }

    /**
     * 中文说明：写入时复制 allowedSubjects。
     * English summary: Defensively copies the incoming allowed subjects.
     */
    public LlmModelCommandDTO setAllowedSubjects(List<String> allowedSubjects) {
        this.allowedSubjects = allowedSubjects == null ? null : new ArrayList<>(allowedSubjects);
        return this;
    }

    /**
     * 中文说明：返回 routes 的不可变快照副本。
     * English summary: Returns an immutable snapshot copy of the route bindings.
     */
    public List<LlmRouteBindingDTO> getRoutes() {
        return routes == null ? null : Collections.unmodifiableList(new ArrayList<>(routes));
    }

    /**
     * 中文说明：写入时复制 routes；元素仍由 {@code @Valid} 逐个校验。
     * English summary: Defensively copies the incoming route bindings; elements stay validated by {@code @Valid}.
     */
    public LlmModelCommandDTO setRoutes(List<LlmRouteBindingDTO> routes) {
        this.routes = routes == null ? null : new ArrayList<>(routes);
        return this;
    }
}
