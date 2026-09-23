package top.egon.cola.component.yuheng.admin.llm.domain.dto;

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
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmCapabilityEnum;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 中文说明：{@code LlmRouteBindingDTO} 是模型 routes JSONB 的一条顶层绑定载体，承载原 API-006/007 的
 * {@code channelKey/upstreamModel/capabilities/priority/weight}；不使用任意 Map 代替鉴权数据。
 * English summary: {@code LlmRouteBindingDTO} is the top-level route binding carrier mapped one-to-one to a
 * model routes JSONB element of API-006/007 instead of an untyped Map.
 *
 * 用法 / Usage: 由 {@code LlmModelCommandDTO}/{@code LlmModelVO} 以 {@code @Valid} 嵌套校验；
 * capabilities 在 setter/getter 上做防御性快照，避免异步任务改写输入。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class LlmRouteBindingDTO {

    /** 已存在且能力匹配的渠道稳定 key / existing channel stable key. */
    @NotBlank
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
    private String channelKey;

    /** 上游精确模型名，1–128，不构成新的客户端权限 / exact upstream model name. */
    @NotBlank
    @Size(max = 128)
    private String upstreamModel;

    /** 此绑定允许的协议特性，有界唯一枚举 / allowed protocol capabilities. */
    @NotNull
    private Set<LlmCapabilityEnum> capabilities;

    /** 候选优先级 0–1000，越大越优先 / candidate priority. */
    @NotNull
    @Min(0)
    @Max(1_000)
    private Integer priority;

    /** 同优先级随机权重 1–1000 / random weight inside one priority. */
    @NotNull
    @Min(1)
    @Max(1_000)
    private Integer weight;

    /**
     * 中文说明：返回 capabilities 的不可变快照副本，避免外部持有者修改已进入异步任务的数据。
     * English summary: Returns an immutable snapshot copy of the capabilities set.
     */
    public Set<LlmCapabilityEnum> getCapabilities() {
        return capabilities == null ? null : Collections.unmodifiableSet(new LinkedHashSet<>(capabilities));
    }

    /**
     * 中文说明：写入时复制 capabilities，重复项按原合同拒绝而非静默合并由上层集合校验负责。
     * English summary: Defensively copies the incoming capabilities set.
     */
    public LlmRouteBindingDTO setCapabilities(Set<LlmCapabilityEnum> capabilities) {
        this.capabilities = capabilities == null ? null : new LinkedHashSet<>(capabilities);
        return this;
    }
}
