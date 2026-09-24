package top.egon.cola.component.yuheng.llm.proxy.domain.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmCapabilityEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 中文说明：{@code LlmInvocationCommandDTO} 是模型入口的一次调用命令：协议 enum、客户端 alias、stream 位、已解析但
 * <b>尚未按协议校验</b>的原生 {@link ObjectNode}，以及一段<b>可信身份封套</b>（已由 Tianquan 验证的 SERVICE subject、
 * 引擎收窄后的出域许可、本次入口要求的协议能力）。它是 engine 内部唯一被强校验的业务载体：任意 JSON 字段既不被当作
 * 已授权对象，也不允许携带 baseUrl、secretRef、Authorization、Host 等自证字段——那些只存在于服务端受管配置里。
 * English summary: {@code LlmInvocationCommandDTO} is one model-entry command inside the engine: the protocol enum, the
 * client alias, the stream flag, the parsed-but-<b>not-yet-protocol-validated</b> native {@link ObjectNode}, and a
 * <b>trusted identity envelope</b> (the Tianquan-verified SERVICE subject, the engine-narrowed egress allowance and the
 * capabilities this entry point requires). It is the only strongly validated business carrier in the engine: arbitrary
 * JSON fields are never treated as authorized objects and may never carry baseUrl, secretRef, Authorization or Host
 * self-assertions, which exist only in server-side managed configuration.
 *
 * 用法 / Usage: 由 {@code LlmApiController}（Step 11）在鉴权之后构造，交 {@code LlmInvocationService} 以默认校验组触发；
 * 集合在 getter/setter 上做防御性快照，避免异步流任务改写输入。/ Built by {@code LlmApiController} (Step 11) after
 * authentication and validated by {@code LlmInvocationService} in the default group; the sets are snapshotted defensively.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class LlmInvocationCommandDTO {

    /** 中文说明：本次入口协议，四种受管协议之一，绝不由 body 推断。 English summary: the ingress protocol, one of the four managed ones, never inferred from the body. */
    @NotNull
    private LlmProtocolEnum protocol;

    /** 中文说明：客户端使用的模型 alias（{@code gateway_llm_model.model_key}），不是供应商真实模型名。 English summary: the client-facing model alias, not the vendor's real model name. */
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
    private String model;

    /** 中文说明：stream 位；缺失由协议入口显式写 false，null 直接拒绝，不猜默认值。 English summary: the stream flag; absent ingress writes false explicitly, null is rejected rather than defaulted. */
    @NotNull
    private Boolean stream;

    /** 中文说明：原生协议请求文档，结构/数值/数组顺序保真；已知危险字段由外层与协议 Strategy 双重拒绝。 English summary: the native protocol request document kept structurally faithful; known-dangerous fields are rejected twice. */
    @NotNull
    private ObjectNode payload;

    /** 中文说明：已验证的 SERVICE subject，仅取自 Tianquan 结论；普通调用者不能声明更宽出域权限。 English summary: the verified SERVICE subject taken only from Tianquan; ordinary callers cannot widen egress. */
    @NotBlank
    @Size(max = 128)
    private String callerSubject;

    /** 中文说明：引擎收窄后的允许部署形态；embedding 请求在这里就被永久限制为 LOCAL。 English summary: the engine-narrowed allowed deployments; embedding requests are pinned to LOCAL here for good. */
    @NotNull
    @Size(min = 1, max = 2)
    private Set<LlmDeploymentEnum> allowedDeployments;

    /** 中文说明：本次入口必须具备的协议能力集合，由协议与已校验字段派生，1–5 唯一。 English summary: the capabilities this entry must offer, derived from the validated protocol fields, 1–5 unique. */
    @NotNull
    @Size(min = 1, max = 5)
    private Set<LlmCapabilityEnum> requiredCapabilities;

    /**
     * 中文说明：返回 allowedDeployments 的不可变快照副本，避免已进入异步流任务的命令被外部改写而扩大出域范围。
     * English summary: Returns an immutable snapshot of allowedDeployments so an in-flight streaming command can never be
     * widened into a broader egress scope by an external holder.
     */
    public Set<LlmDeploymentEnum> getAllowedDeployments() {
        return allowedDeployments == null
                ? null : Collections.unmodifiableSet(new LinkedHashSet<>(allowedDeployments));
    }

    /**
     * 中文说明：写入时复制 allowedDeployments，保持声明顺序但不共享调用方集合。
     * English summary: Defensively copies allowedDeployments, keeping declaration order without sharing the caller's set.
     */
    public LlmInvocationCommandDTO setAllowedDeployments(Set<LlmDeploymentEnum> allowedDeployments) {
        this.allowedDeployments = allowedDeployments == null ? null : new LinkedHashSet<>(allowedDeployments);
        return this;
    }

    /**
     * 中文说明：返回 requiredCapabilities 的不可变快照副本。
     * English summary: Returns an immutable snapshot of the required capabilities.
     */
    public Set<LlmCapabilityEnum> getRequiredCapabilities() {
        return requiredCapabilities == null
                ? null : Collections.unmodifiableSet(new LinkedHashSet<>(requiredCapabilities));
    }

    /**
     * 中文说明：写入时复制 requiredCapabilities。
     * English summary: Defensively copies the required capabilities.
     */
    public LlmInvocationCommandDTO setRequiredCapabilities(Set<LlmCapabilityEnum> requiredCapabilities) {
        this.requiredCapabilities = requiredCapabilities == null ? null : new LinkedHashSet<>(requiredCapabilities);
        return this;
    }
}
