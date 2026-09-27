package top.egon.cola.component.yuheng.llm.proxy.domain.bo;

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
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmCapabilityEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmModelKindEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 中文说明：{@code LlmModelSnapshotBO} 是单次请求的<b>只读一致配置快照</b>：一个模型 alias 的业务事实加上它显式声明的
 * 有序 route，每条 route 携一个可空 {@link RouteBO#getChannel()} 渠道事实。它是纯业务载体，不是 ORM 身份——没有 id、
 * tenantId、审计列，也没有 MP 的 {@code version}，因此被 web/service 层持有也不会把持久化生命周期带进请求；
 * 渠道的 {@code baseUrl}/{@code secretRef} 只允许出现在服务端调用链里，绝不进入任何 VO、错误或日志。
 * 过滤顺序（已授权 subject ∩ alias 启用 ∩ 协议 ∩ 能力 ∩ 出域限制 ∩ 显式 bindings ∩ 渠道启用）由
 * {@code LlmRouteSelectionStrategy} 与调用方 Service 负责，本类只如实呈现读到的内容，不伪造渠道、不隐藏悬空引用。
 * English summary: {@code LlmModelSnapshotBO} is the <b>read-only coherent configuration snapshot</b> for one request:
 * the business facts of one model alias plus its explicitly declared ordered routes, where every route carries one
 * nullable {@link RouteBO#getChannel()} channel fact. It is a plain business carrier and not an ORM identity — no id,
 * tenantId, audit columns and no MP {@code version} — so holding it in the web or service layer never drags a persistence
 * lifecycle into the request; the channel's {@code baseUrl}/{@code secretRef} may only appear in the server-side call
 * chain and never in a VO, an error or a log. The filtering order (authorized subject ∩ alias enabled ∩ protocol ∩
 * capabilities ∩ egress limit ∩ explicit bindings ∩ channel enablement) belongs to {@code LlmRouteSelectionStrategy} and
 * the calling service: this class reports only what was read, fabricating no channel and hiding no dangling reference.
 *
 * 用法 / Usage: 由 {@code MpLlmConfigurationRepository} 在短只读事务内构造并在事务结束后交给调用方，集合字段在
 * getter/setter 上做防御性快照，因此异步流任务无法改写已经进入请求的配置；{@code routes} 保持持久层的声明顺序，
 * priority/weight 加权只在该顺序之上进行。校验由 Spring 的方法校验在端口边界触发，全部约束位于默认组；
 * 目录读（API-003）复用同一载体但只使用模型侧字段，{@code channel} 为 {@code null} 表示未解析或引用悬空，
 * 消费方必须按“无渠道即拒绝”处理，绝不放宽到全部渠道。
 * / Built by {@code MpLlmConfigurationRepository} inside a short read-only transaction and handed over after that
 * transaction ends, with defensive collection snapshots so an asynchronous stream task cannot rewrite configuration that
 * already entered a request; {@code routes} keep the persisted declaration order and priority/weighting only happens on
 * top of it. Validation is triggered by Spring method validation at the port boundary and every constraint lives in the
 * default group; the catalog read (API-003) reuses this carrier with only the model-side fields, where a {@code null}
 * {@code channel} means unresolved or dangling and consumers must treat "no channel" as rejection rather than as licence
 * to widen to all channels.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
public class LlmModelSnapshotBO {

    /** 中文说明：客户端 alias（{@code gateway_llm_model.model_key}），不透明且永不由上游响应推断。 English summary: the client alias, opaque and never inferred from an upstream response. */
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
    private String modelKey;

    /** 中文说明：展示名称，用于目录与审计，不参与路由判定。 English summary: the display name used by the catalog and audit, never by routing. */
    @NotBlank
    @Size(max = 128)
    private String name;

    /** 中文说明：模型 alias 的持久化创建时刻，目录协议将其转换为 Unix 秒。 English summary: the persisted model alias creation instant, converted to Unix seconds by the catalog protocol. */
    @NotNull
    private Instant createdAt;

    /** 中文说明：模型种类 CHAT/EMBEDDING；embedding 的本地化约束由消费方在本载体之上强制。 English summary: the CHAT/EMBEDDING kind; the embedding locality constraint is enforced by consumers above this carrier. */
    @NotNull
    private LlmModelKindEnum kind;

    /** 中文说明：alias 启停位；停用的 alias 不进入任何候选，也不迁移到其他模型。 English summary: the alias enablement flag; a disabled alias enters no candidate set and is never migrated onto another model. */
    @NotNull
    private Boolean enabled;

    /** 中文说明：alias 声明的协议集合，非空且最多四种，顺序即持久层声明顺序。 English summary: the protocols the alias declares, non-empty and at most four, in persisted order. */
    @NotNull
    @Size(min = 1, max = 4)
    private List<LlmProtocolEnum> protocols;

    /** 中文说明：向量维度，EMBEDDING 为 1..16000 且必须等于部署实际维度，CHAT 为 null。 English summary: the vector dimension, 1..16000 and equal to the deployed dimension for EMBEDDING, null for CHAT. */
    @Min(1)
    @Max(16_000)
    private Integer dimensions;

    /** 中文说明：嵌入空间稳定身份，被资料引用后不可变更；CHAT 为 null。 English summary: the stable embedding-space identity, immutable once referenced by material; null for CHAT. */
    @Size(max = 128)
    private String embeddingSpaceId;

    /** 中文说明：被授权调用本 alias 的 SERVICE subject 列表；缺失或空表示禁止全部，绝不视为放行。 English summary: the SERVICE subjects authorized for this alias; absence or emptiness forbids all callers and is never read as permission. */
    @NotNull
    @Size(max = 100)
    private List<@NotBlank @Size(max = 128) String> allowedSubjects;

    /** 中文说明：业务修订号，用于审计与缓存一致性核对，与 MP 技术 version 无关。 English summary: the business revision used for audit and cache coherence, unrelated to the technical MP version. */
    @NotNull
    @Min(1)
    private Long revision;

    /** 中文说明：显式声明的有序 route，1–16 条；空集合意味着配置不合法而不是“试所有渠道”。 English summary: the explicitly declared ordered routes, 1–16 entries; an empty set means invalid configuration, not "try every channel". */
    @NotNull
    @Valid
    @Size(min = 1, max = 16)
    private List<RouteBO> routes;

    /**
     * 中文说明：返回 routes 的不可变快照副本，防止请求持有者改写已进入异步流任务的候选顺序。
     * English summary: Returns an immutable snapshot of the routes so a request holder cannot rewrite the candidate
     * order that already reached an asynchronous stream task.
     */
    public List<RouteBO> getRoutes() {
        return routes == null ? null : Collections.unmodifiableList(new ArrayList<>(routes));
    }

    /**
     * 中文说明：写入时复制 routes，保持声明顺序但不共享 repository 内部列表。
     * English summary: Defensively copies routes, keeping declaration order without sharing the repository's list.
     */
    public LlmModelSnapshotBO setRoutes(List<RouteBO> routes) {
        this.routes = routes == null ? null : new ArrayList<>(routes);
        return this;
    }

    /**
     * 中文说明：返回 protocols 的不可变快照副本。
     * English summary: Returns an immutable snapshot of the declared protocols.
     */
    public List<LlmProtocolEnum> getProtocols() {
        return protocols == null ? null : Collections.unmodifiableList(new ArrayList<>(protocols));
    }

    /**
     * 中文说明：写入时复制 protocols。
     * English summary: Defensively copies the declared protocols.
     */
    public LlmModelSnapshotBO setProtocols(List<LlmProtocolEnum> protocols) {
        this.protocols = protocols == null ? null : new ArrayList<>(protocols);
        return this;
    }

    /**
     * 中文说明：返回 allowedSubjects 的不可变快照副本。
     * English summary: Returns an immutable snapshot of the authorized subjects.
     */
    public List<String> getAllowedSubjects() {
        return allowedSubjects == null ? null : Collections.unmodifiableList(new ArrayList<>(allowedSubjects));
    }

    /**
     * 中文说明：写入时复制 allowedSubjects。
     * English summary: Defensively copies the authorized subjects.
     */
    public LlmModelSnapshotBO setAllowedSubjects(List<String> allowedSubjects) {
        this.allowedSubjects = allowedSubjects == null ? null : new ArrayList<>(allowedSubjects);
        return this;
    }

    /**
     * 中文说明：{@code RouteBO} 是模型 {@code routes[]} 中一条路由绑定的业务投影：渠道 key、上游真实模型名、priority、
     * weight 与能力集合，再加上被解析出来的渠道事实。它是只读快照的一部分，不是可写命令，因此没有 expectedRevision，
     * 也没有任何持久化身份。
     * English summary: {@code RouteBO} is the business projection of one {@code routes[]} binding: the channel key, the
     * real upstream model name, priority, weight and capabilities, plus the resolved channel facts. It belongs to a
     * read-only snapshot rather than a writable command, so it carries no expectedRevision and no persistence identity.
     *
     * 用法 / Usage: 由 repository 逐条按持久层顺序构造；{@link #getChannel()} 为 {@code null} 时该 route 必须被拒绝
     * （渠道被删除或目录读未解析），调用方不得把 null 当成“随便挑一个”。集合 getter/setter 做防御性快照。
     * / Built entry by entry by the repository in persisted order; when {@link #getChannel()} is {@code null} the route
     * must be rejected (the channel was deleted or the catalog read resolved nothing), and null must never be read as
     * "pick any channel". Collections are snapshotted defensively in the getter and setter.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Accessors(chain = true)
    public static class RouteBO {

        /** 中文说明：逻辑引用的渠道 key；数据库无伪外键，因此可能悬空。 English summary: the logically referenced channel key; with no fake foreign key in the database it may dangle. */
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
        private String channelKey;

        /** 中文说明：上游真实模型名，只在出站请求里出现，不出现在给客户端的响应中。 English summary: the real upstream model name, present only in the egress request and never in a client response. */
        @NotBlank
        @Size(max = 128)
        private String upstreamModel;

        /** 中文说明：优先级，数值越小越先尝试；同一 priority 组内再按 weight 加权。 English summary: the priority, lower first, with weighting inside the same priority band. */
        @NotNull
        @Min(0)
        @Max(1_000)
        private Integer priority;

        /** 中文说明：权重，1..1000，只在同优先级内决定选择概率。 English summary: the weight, 1..1000, deciding selection probability only within one priority band. */
        @NotNull
        @Min(1)
        @Max(1_000)
        private Integer weight;

        /** 中文说明：该 route 要求同时满足的能力集合，1–5 唯一。 English summary: the capabilities this route must satisfy together, 1–5 unique. */
        @NotNull
        @Size(min = 1, max = 5)
        private Set<@NotNull LlmCapabilityEnum> capabilities;

        /** 中文说明：解析后的渠道事实；null 表示引用悬空或本次为目录读，消费方必须拒绝该 route。 English summary: the resolved channel facts; null means a dangling reference or a catalog read, and consumers must reject that route. */
        @Valid
        private ChannelBO channel;

        /**
         * 中文说明：返回 capabilities 的不可变快照副本。
         * English summary: Returns an immutable snapshot of the route capabilities.
         */
        public Set<LlmCapabilityEnum> getCapabilities() {
            return capabilities == null ? null : Collections.unmodifiableSet(new LinkedHashSet<>(capabilities));
        }

        /**
         * 中文说明：写入时复制 capabilities，保持声明顺序。
         * English summary: Defensively copies the route capabilities, keeping declaration order.
         */
        public RouteBO setCapabilities(Set<LlmCapabilityEnum> capabilities) {
            this.capabilities = capabilities == null ? null : new LinkedHashSet<>(capabilities);
            return this;
        }
    }

    /**
     * 中文说明：{@code ChannelBO} 是一次尝试真正需要的渠道事实：部署形态、协议、地址、密钥引用、启停与超时/并发预算。
     * 它是业务投影而非 ORM 行，没有 id/tenant/审计列，因此可以在只读事务关闭之后继续安全地存在于请求里。
     * English summary: {@code ChannelBO} holds the channel facts one attempt really needs: deployment form, protocol,
     * base URL, secret reference, enablement and the timeout/concurrency budget. It is a business projection rather than
     * an ORM row, carrying no id, tenant or audit columns, so it may keep existing safely inside a request after the
     * read-only transaction has closed.
     *
     * 用法 / Usage: 由 repository 从受守卫的具名查询映射；{@code baseUrl}/{@code secretRef} 只允许被出站客户端与
     * 凭据解析器读取，日志与错误必须脱敏。{@code enabled == false} 的渠道仍会出现在快照中，由过滤阶段拒绝，
     * 以便“配置缺失”与“配置停用”在审计上可区分。
     * / Mapped by the repository from the guarded named queries; {@code baseUrl}/{@code secretRef} may only be read by
     * the egress client and the credential resolver, and logs and errors must mask them. A disabled channel still shows
     * up in the snapshot and is rejected during filtering so that missing and disabled configuration stay separable in
     * the audit trail.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Accessors(chain = true)
    public static class ChannelBO {

        /** 中文说明：渠道 key，与 route 的 channelKey 对应。 English summary: the channel key matching the route's channelKey. */
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[a-z0-9][a-z0-9-]{0,63}$")
        private String channelKey;

        /** 中文说明：部署形态 LOCAL/CLOUD；embedding 只接受 LOCAL。 English summary: the LOCAL/CLOUD deployment form; embedding accepts LOCAL only. */
        @NotNull
        private LlmDeploymentEnum deployment;

        /** 中文说明：渠道协议，必须与 alias 声明协议以及请求入口协议一致。 English summary: the channel protocol, which must agree with both the alias-declared protocols and the ingress protocol. */
        @NotNull
        private LlmProtocolEnum protocol;

        /** 中文说明：规范化 HTTPS base URL；敏感，禁止进入日志与错误体。 English summary: the normalized HTTPS base URL; sensitive and forbidden in logs and error bodies. */
        @NotBlank
        @Size(max = 2048)
        private String baseUrl;

        /** 中文说明：部署密钥引用（不是凭据值）；仅无认证 LOCAL 渠道可为空，敏感且禁止进入日志/错误体。 English summary: the deployment secret reference rather than a credential value; it may be null only for an unauthenticated LOCAL channel and is forbidden in logs/errors. */
        @Size(max = 128)
        @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9/_-]{0,127}$")
        private String secretRef;

        /** 中文说明：渠道启停位；停用在过滤阶段拒绝，不在本层伪造替换渠道。 English summary: the channel enablement flag; disabled channels are rejected during filtering and never silently substituted here. */
        @NotNull
        private Boolean enabled;

        /** 中文说明：连接超时毫秒。 English summary: the connect timeout in milliseconds. */
        @NotNull
        @Min(1)
        private Integer connectTimeoutMs;

        /** 中文说明：首包/头部超时毫秒。 English summary: the header or first-packet timeout in milliseconds. */
        @NotNull
        @Min(1)
        private Integer headerTimeoutMs;

        /** 中文说明：流式空闲超时毫秒。 English summary: the streaming idle timeout in milliseconds. */
        @NotNull
        @Min(1)
        private Integer idleTimeoutMs;

        /** 中文说明：整次请求总超时毫秒，与请求级 deadline 取较小者。 English summary: the total per-request timeout in milliseconds, capped by the request-wide deadline. */
        @NotNull
        @Min(1)
        private Integer totalTimeoutMs;

        /** 中文说明：该渠道在本引擎内的并发上限，驱动进程内许可与公平性。 English summary: this channel's concurrency ceiling inside the engine, driving in-process permits and fairness. */
        @NotNull
        @Min(1)
        private Integer maxConcurrent;
    }
}
