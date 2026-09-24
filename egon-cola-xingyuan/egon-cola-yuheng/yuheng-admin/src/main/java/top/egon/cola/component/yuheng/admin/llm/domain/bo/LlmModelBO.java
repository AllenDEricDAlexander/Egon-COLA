package top.egon.cola.component.yuheng.admin.llm.domain.bo;

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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：{@code LlmModelBO} 是 {@code gateway_llm_model} 的业务载体，承载模型 alias 的业务事实：
 * {@code protocols}/{@code allowedSubjects}/{@code routes} 三列在库内是 jsonb，在这里是结构化集合
 * （routes 复用 {@link LlmRouteBindingDTO} 这一条顶层载体，不用 Map 代替鉴权数据），
 * {@code revision} 与 {@code createdAt}/{@code updatedAt} 是服务端权威投影；
 * 它不继承 {@code EgonModel}，也不承载租户与操作者列。
 * English summary: {@code LlmModelBO} is the business carrier of {@code gateway_llm_model}: the
 * {@code protocols}/{@code allowedSubjects}/{@code routes} jsonb columns surface as structured collections here
 * (routes reuse the top-level {@link LlmRouteBindingDTO} carrier rather than an untyped Map), while {@code revision}
 * and the audit projections stay server-authoritative; it neither extends {@code EgonModel} nor carries tenant or
 * operator columns.
 *
 * 用法 / Usage: 只由 {@code LlmModelPersistenceConverter} 与行模型互转，并经 {@code LlmConfigurationRepository} 端口流动。
 * kind 与 {@code dimensions}/{@code embeddingSpaceId} 的跨字段一致性由输入侧的 {@code @ValidLlmModel}（绑定
 * {@code LlmModelCommandDTO}）与业务入口负责，本载体只保留单字段原生约束与 {@code @Valid} 嵌套校验，
 * 集合读写一律防御性快照，避免异步任务改写已进入持久边界的数据。/ Structural mapping happens only in
 * {@code LlmModelPersistenceConverter}; the cross-field kind consistency stays with the input-side
 * {@code @ValidLlmModel} contract and the owning service, while this carrier keeps single-field constraints and
 * {@code @Valid} nesting, and every collection read or write is a defensive snapshot.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class LlmModelBO {

    /** 不透明主键的十进制文本，插入前为 null / decimal text of the assigned key, null before an insert. */
    private String id;

    /** 稳定配置键，创建后不改名，与 {@code gateway_llm_model.model_key} 同列 / stable model key, immutable after creation. */
    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
    private String modelKey;

    /** 展示名称 / trimmed display name. */
    @NotBlank
    @Size(max = 128)
    private String name;

    /** CHAT 或 EMBEDDING，决定空间字段是否为空 / model kind driving the space fields. */
    @NotNull
    private LlmModelKindEnum kind;

    /** 支持入口协议集合，1–4 唯一 / supported ingress protocols, 1–4 unique. */
    @NotNull
    @Size(min = 1, max = 4)
    private List<@NotNull LlmProtocolEnum> protocols;

    /** 是否用于新调用 / enablement for new calls. */
    @NotNull
    private Boolean enabled;

    /** 嵌入维度，CHAT 为 null / embedding dimensions, null for CHAT. */
    @Min(1)
    @Max(16_000)
    private Integer dimensions;

    /** 不可变嵌入空间标识，CHAT 为 null / immutable embedding space identity, null for CHAT. */
    @Size(max = 128)
    private String embeddingSpaceId;

    /** 已验证 SERVICE subject 列表，1–100，非请求方自报 / verified service subjects, 1–100. */
    @NotNull
    @Size(min = 1, max = 100)
    private List<@NotBlank String> allowedSubjects;

    /** 显式模型渠道映射，1–16，无匹配不扩大到全部渠道 / explicit route bindings, 1–16. */
    @NotNull
    @Size(min = 1, max = 16)
    @Valid
    private List<LlmRouteBindingDTO> routes;

    /** 调用方期望的乐观版本：创建意图为 0 哨兵值，替换为库中现值，落库后由仓储回写权威值 / caller expectation, 0 on create intent and the stored revision on replace, overwritten by the authoritative value after a save. */
    @Min(0)
    private long revision;

    /** 投影自 {@code create_time} 的创建时刻 / creation instant projected from {@code create_time}. */
    private Instant createdAt;

    /** 投影自 {@code update_time} 的更新时刻 / update instant projected from {@code update_time}. */
    private Instant updatedAt;

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
    public LlmModelBO setProtocols(List<LlmProtocolEnum> protocols) {
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
    public LlmModelBO setAllowedSubjects(List<String> allowedSubjects) {
        this.allowedSubjects = allowedSubjects == null ? null : new ArrayList<>(allowedSubjects);
        return this;
    }

    /**
     * 中文说明：返回 routes 的不可变快照副本；元素仍由 {@code @Valid} 逐个校验。
     * English summary: Returns an immutable snapshot copy of the route bindings.
     */
    public List<LlmRouteBindingDTO> getRoutes() {
        return routes == null ? null : Collections.unmodifiableList(new ArrayList<>(routes));
    }

    /**
     * 中文说明：写入时复制 routes。
     * English summary: Defensively copies the incoming route bindings.
     */
    public LlmModelBO setRoutes(List<LlmRouteBindingDTO> routes) {
        this.routes = routes == null ? null : new ArrayList<>(routes);
        return this;
    }
}
