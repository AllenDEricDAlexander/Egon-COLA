package top.egon.cola.component.yuheng.llm.proxy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.exception.CommonException;
import top.egon.cola.component.yuheng.llm.config.LlmGatewayProperties;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.dto.LlmInvocationCommandDTO;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmModelKindEnum;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * 中文说明：{@code LlmRouteSelectionStrategy} 是模型路由的选择策略：把 {@code LlmModelSnapshotBO}（一个 alias 的业务
 * 事实 + 其显式声明的有序 {@code routes[]} + 每条 route 解析出的渠道事实）收敛成一份<b>有界、有序</b>的候选 route 列表。
 * 收敛顺序就是不可协商的安全顺序——引擎开关、SERVICE subject 授权、alias 启停、alias 声明协议、嵌入空间与出域限制、
 * 渠道启用位、同协议与能力覆盖、baseUrl 出网白名单，全部完成后才允许 priority 与同优先级 weight 参与；因此「加权」
 * 永远不可能把一条未授权或云端的 embedding 渠道带回候选集。四条规则在此被硬编码为不可放宽：kind=EMBEDDING 的候选
 * 只可能是 {@code LOCAL}（配置成云端即 422，而不是降级）、embedding 必须声明不可变的
 * {@code embeddingSpaceId}+{@code dimensions}（缺失即 422，绝不允许仅凭「维度相同」跨模型容灾）、
 * {@link LlmModelSnapshotBO.RouteBO#getChannel()} 为 {@code null} 的 route 一律拒绝（渠道被删或目录读未解析，
 * null 绝不是「随便挑一个」的许可），以及空白名单即该类渠道不可用。空集合直接 503 拒绝，绝不扩大到全部渠道；
 * 日志与错误只带 alias、channelKey 与计数，绝不回显 baseUrl、upstreamModel、secretRef 或任何凭据。
 * English summary: {@code LlmRouteSelectionStrategy} narrows an {@code LlmModelSnapshotBO} (one alias's business facts plus
 * its explicitly declared ordered {@code routes[]}, each carrying its resolved channel facts) into a <b>bounded, ordered</b>
 * candidate route list. That narrowing order is the non-negotiable security order — engine switch, SERVICE subject
 * authorization, alias enablement, alias-declared protocol, embedding space and data-egress limits, channel enablement,
 * same-protocol and capability coverage, then the baseUrl outbound allow-list — and only after all of them may priority and
 * same-priority weight take part, so weighting can never bring an unauthorized or cloud embedding channel back into the set.
 * Four rules are pinned so they cannot be widened: candidates of kind=EMBEDDING can only ever be {@code LOCAL} (a cloud
 * configuration is a 422, not a degradation), an embedding alias must declare its immutable
 * {@code embeddingSpaceId}+{@code dimensions} (absence is a 422, and equal dimensions alone may never make one embedding model
 * the failover of another), a route whose {@link LlmModelSnapshotBO.RouteBO#getChannel()} is {@code null} is always rejected
 * (the channel was deleted or a catalog read resolved nothing, and null is never licence to "pick any channel"), and an empty
 * allow-list makes that channel class unavailable. An empty result is a 503 and is never widened to all channels, and logs
 * and errors carry only the alias, the channelKey and counts — never a base URL, upstream model name, secretRef or credential.
 *
 * 用法 / Usage: 由 {@code LlmInvocationServiceImpl} 在只读快照事务结束之后调用，本类不开事务、不发 HTTP；调用方取列表
 * 首元素作为唯一尝试，安全失败时按候选顺序做至多 {@code yuheng.llm.maximum-attempts} 次尝试，全程复用同一份快照。
 * / Called by {@code LlmInvocationServiceImpl} after the read-only snapshot transaction has closed; this class opens no
 * transaction and sends no HTTP. The caller attempts the head element and, on a safe failure, follows the candidate order
 * for at most {@code yuheng.llm.maximum-attempts} attempts while reusing the same snapshot.
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@Service("llmRouteSelectionStrategy")
public class LlmRouteSelectionStrategy {

    /** 中文说明：候选列表的硬上界，与 {@code routes[]} 的 1–16 合同同宽；超出部分按加权顺序截断。 English summary: the hard candidate ceiling, as wide as the 1–16 routes contract, with the tail truncated in weighted order. */
    public static final int MAX_CANDIDATES = 16;

    /** 中文说明：本进程唯一的出网边界配置（白名单与引擎开关），是本策略唯一的外部依赖。 English summary: this process's only egress boundary configuration (allow-lists plus the engine switch), and the strategy's only collaborator. */
    @Qualifier(LlmGatewayProperties.BEAN_NAME)
    private final LlmGatewayProperties gatewayProperties;

    /**
     * 中文说明：按固定顺序收敛候选：先整体判定（引擎启用、subject 授权、alias 启用与声明协议、embedding 云端配置与空间
     * 声明），再逐条判定（渠道可解析且启用、同协议、能力覆盖、出域许可、白名单），最后才做 priority 升序 + 同优先级
     * weight 不放回加权抽样（{@code priority} 越小越先尝试）。
     * English summary: Narrows candidates in a fixed order: whole-scope decisions first (engine enabled, subject authorized,
     * alias enabled and declaring the protocol, embedding cloud configuration and space declaration), then per-route decisions
     * (channel resolvable and enabled, same protocol, capability coverage, egress allowance, allow-list), and only last the
     * ascending priority bands with weighted draws without replacement inside one band (a smaller {@code priority} is tried first).
     *
     * 用法 / Usage: 调用方式 / Usage: {@code LlmRouteSelectionStrategy.select(command, snapshot)}。
     * @param command 参数 已校验的调用命令，提供 alias、协议、能力与可信出域许可；parameter the validated command supplying alias, protocol, capabilities and the trusted egress allowance.
     * @param snapshot 参数 同一 alias 的只读一致快照，routes 顺序即持久层声明顺序；parameter the read-only coherent snapshot of one alias, whose routes keep the persisted declaration order.
     * @return 返回 至多 {@value #MAX_CANDIDATES} 条有序候选 route，保证非空；returns at most {@value #MAX_CANDIDATES} ordered candidate routes, guaranteed non-empty.
     * @throws CommonException 403 未授权 alias、404 alias 缺失或停用、422 embedding 云端配置或空间未声明、503 引擎关闭、无同协议路由或无可用候选；code 即原生 HTTP 状态；the code is the native HTTP status.
     */
    public List<LlmModelSnapshotBO.RouteBO> select(
            LlmInvocationCommandDTO command,
            LlmModelSnapshotBO snapshot) {
        Objects.requireNonNull(command, "command");
        if (!gatewayProperties.isEnabled()) {
            throw new CommonException(503, "LLM_ENGINE_DISABLED",
                    "The LLM engine is not enabled for outbound model calls");
        }
        if (snapshot == null || snapshot.getRoutes() == null || snapshot.getRoutes().isEmpty()) {
            throw new CommonException(404, "LLM_MODEL_NOT_FOUND",
                    "No configured model alias is bound to the requested alias");
        }
        List<LlmModelSnapshotBO.RouteBO> declared = snapshot.getRoutes();
        authorize(command, snapshot);
        Set<LlmDeploymentEnum> egress = egressAllowance(command, snapshot);
        requireLocalEmbeddingSpace(snapshot, declared);
        List<Predicate<LlmModelSnapshotBO.RouteBO>> eligibility = List.of(
                this::channelResolved,
                this::channelEnabled,
                route -> protocolMatches(route, command),
                route -> capabilitiesCovered(route, command),
                route -> egress.contains(deploymentOf(route)),
                this::outboundAllowed);
        List<LlmModelSnapshotBO.RouteBO> eligible = declared.stream()
                .filter(route -> eligibility.stream().allMatch(criterion -> criterion.test(route)))
                .toList();
        if (eligible.isEmpty()) {
            log.warn("llm routing rejected alias={} protocol={} declared={} authorizedEgress={}",
                    command.getModel(), command.getProtocol(), declared.size(), egress);
            throw new CommonException(503, "LLM_NO_ELIGIBLE_ROUTE",
                    "No enabled, authorized and allow-listed channel serves this alias and protocol");
        }
        List<LlmModelSnapshotBO.RouteBO> ordered = orderByPriorityThenWeight(eligible);
        if (log.isDebugEnabled()) {
            log.debug("llm routing alias={} protocol={} revision={} candidates={} channels={}", command.getModel(),
                    command.getProtocol(), snapshot.getRevision(), ordered.size(), ordered.stream()
                            .map(LlmModelSnapshotBO.RouteBO::getChannelKey).toList());
        }
        return ordered;
    }

    /**
     * 中文说明：整体准入判定。identity 只信 Tianquan 结论：subject 不在 alias 的 allowedSubjects 内即 403，且缺失或空的
     * 授权列表一律按「禁止全部」而非「放行」；停用 alias 与未授权 alias 都不得泄漏任何上游信息，因此两者只回稳定说明。
     * English summary: Whole-scope admission. Only the Tianquan conclusion is trusted as identity: a subject outside the alias's
     * allowedSubjects is a 403, and an absent or empty authorization list means "forbid all" rather than "allow all". Neither a
     * disabled nor an unauthorized alias may leak upstream detail, so both answer with a stable description only.
     */
    private void authorize(LlmInvocationCommandDTO command, LlmModelSnapshotBO alias) {
        List<String> allowedSubjects = alias.getAllowedSubjects();
        if (allowedSubjects == null || !allowedSubjects.contains(command.getCallerSubject())) {
            throw new CommonException(403, "LLM_MODEL_NOT_AUTHORIZED",
                    "The verified service identity is not authorized for this model alias");
        }
        if (!Boolean.TRUE.equals(alias.getEnabled())) {
            throw new CommonException(404, "LLM_MODEL_NOT_AVAILABLE",
                    "The model alias is absent or disabled for new calls");
        }
        if (alias.getProtocols() == null || !alias.getProtocols().contains(command.getProtocol())) {
            throw new CommonException(503, "LLM_NO_ELIGIBLE_ROUTE",
                    "The model alias declares no route for the requested protocol");
        }
    }

    /**
     * 中文说明：出域许可求交：调用者声明的许可只能收窄、不能放宽，未声明即无许可；kind=EMBEDDING 无论声明什么都被强制为
     * LOCAL-only，这正是「本地向量化失败不得转云」的实现点。
     * English summary: Intersects egress: a caller's declared allowance may only narrow, never widen, and nothing declared means
     * no allowance; kind=EMBEDDING is forced to LOCAL-only regardless of what was declared, which is exactly where "a local
     * vectorization failure must not fall back to cloud" is enforced.
     */
    private Set<LlmDeploymentEnum> egressAllowance(
            LlmInvocationCommandDTO command, LlmModelSnapshotBO alias) {
        if (alias.getKind() == LlmModelKindEnum.EMBEDDING) {
            return EnumSet.of(LlmDeploymentEnum.LOCAL);
        }
        Set<LlmDeploymentEnum> declared = command.getAllowedDeployments();
        if (declared == null || declared.isEmpty()) {
            return EnumSet.noneOf(LlmDeploymentEnum.class);
        }
        return EnumSet.copyOf(declared);
    }

    /**
     * 中文说明：本地 embedding 的两条不可放宽规则：嵌入空间身份与维度是 alias 级不可变事实，缺失即 422（空间身份被资料
     * 引用后不可变更，因此绝不按「维度相同」跨空间容灾）；声明路由里出现任何 CLOUD 渠道即 422 配置错误，且此判定发生在
     * 过滤之前，因此云端 embedding 渠道的实际调用次数恒为 0。
     * English summary: The two non-widenable local-embedding rules: the embedding space identity and its dimensions are immutable
     * alias-level facts, so absence is a 422 (space identity is immutable once referenced by material, hence equal dimensions
     * alone may never cross spaces), and any CLOUD channel among the declared routes is a 422 configuration error — decided
     * before filtering, so the number of calls actually made to a cloud embedding channel stays zero.
     */
    private void requireLocalEmbeddingSpace(
            LlmModelSnapshotBO alias, List<LlmModelSnapshotBO.RouteBO> declared) {
        if (alias.getKind() != LlmModelKindEnum.EMBEDDING) {
            return;
        }
        if (declared.stream()
                .map(LlmModelSnapshotBO.RouteBO::getChannel)
                .filter(Objects::nonNull)
                .anyMatch(channel -> channel.getDeployment() == LlmDeploymentEnum.CLOUD)) {
            throw new CommonException(422, "LLM_EMBEDDING_CLOUD_CONFIGURED",
                    "The embedding alias binds a cloud channel, which egress policy never permits");
        }
        if (StringUtils.isBlank(alias.getEmbeddingSpaceId()) || alias.getDimensions() == null) {
            throw new CommonException(422, "LLM_EMBEDDING_SPACE_UNDECLARED",
                    "The embedding alias is missing its immutable space identity or dimensions");
        }
    }

    /** 中文说明：渠道事实必须已解析；null 表示引用悬空或本次为目录读，按拒绝处理，绝不放宽到全部渠道。 English summary: resolved channel facts are mandatory; null means a dangling reference or a catalog read, handled as rejection and never as licence to widen to all channels. */
    private boolean channelResolved(LlmModelSnapshotBO.RouteBO route) {
        return route.getChannel() != null;
    }

    /** 中文说明：渠道启用位，停用渠道在过滤阶段拒绝，使「配置缺失」与「配置停用」在审计上可区分。 English summary: the channel enablement flag; disabled channels are rejected during filtering so that missing and disabled configuration stay separable in the audit trail. */
    private boolean channelEnabled(LlmModelSnapshotBO.RouteBO route) {
        LlmModelSnapshotBO.ChannelBO channel = route.getChannel();
        return channel != null && Boolean.TRUE.equals(channel.getEnabled());
    }

    /** 中文说明：渠道协议必须与请求入口协议一致——同协议路由是选择条件而非分支判断，因此新增协议只需新增一个 Strategy。 English summary: the channel protocol must equal the ingress protocol; same-protocol routing is a selection criterion rather than a branch, so a new protocol only needs a new Strategy. */
    private boolean protocolMatches(
            LlmModelSnapshotBO.RouteBO route, LlmInvocationCommandDTO command) {
        LlmModelSnapshotBO.ChannelBO channel = route.getChannel();
        return channel != null && channel.getProtocol() == command.getProtocol();
    }

    /** 中文说明：能力覆盖：route 声明的能力集合必须包含请求端点要求的全部能力，缺失即不进入候选。 English summary: capability coverage: the capability set a route declares must contain every capability the endpoint requires, or the route never enters the candidate set. */
    private boolean capabilitiesCovered(
            LlmModelSnapshotBO.RouteBO route, LlmInvocationCommandDTO command) {
        return route.getCapabilities() != null
                && route.getCapabilities().containsAll(command.getRequiredCapabilities());
    }

    /**
     * 中文说明：SSRF 边界：baseUrl 只能是管理员配置的绝对 http(s) 地址，无 userinfo/query/fragment；LOCAL 必须命中私网
     * CIDR/主机白名单，CLOUD 必须 HTTPS 且命中主机白名单；任一白名单为空即该类渠道不可用（「空白名单不调用」）。
     * English summary: The SSRF boundary: a baseUrl may only be an administrator-configured absolute http(s) URL without userinfo,
     * query or fragment; LOCAL must hit the private CIDR/host allow-list and CLOUD must be HTTPS and hit the host allow-list;
     * when either allow-list is empty its channel class is simply unavailable.
     */
    private boolean outboundAllowed(LlmModelSnapshotBO.RouteBO route) {
        LlmModelSnapshotBO.ChannelBO channel = route.getChannel();
        if (channel == null) {
            return false;
        }
        URI endpoint = parseBaseUrl(channel);
        if (endpoint == null) {
            return false;
        }
        String host = normalizeHost(endpoint.getHost());
        if (channel.getDeployment() == LlmDeploymentEnum.CLOUD) {
            return "https".equalsIgnoreCase(endpoint.getScheme())
                    && matchesHost(host, gatewayProperties.getAllowedCloudHosts());
        }
        if (channel.getDeployment() != LlmDeploymentEnum.LOCAL) {
            return false;
        }
        return ("https".equalsIgnoreCase(endpoint.getScheme()) || "http".equalsIgnoreCase(endpoint.getScheme()))
                && matchesCidrOrHost(host, gatewayProperties.getAllowedLocalCidrs());
    }

    /**
     * 中文说明：解析管理员配置的 baseUrl，非绝对地址、空主机或携带 userinfo/query/fragment 均视为不可出网；解析异常按
     * 拒绝处理，不回显原始地址。
     * English summary: Parses the administrator-configured base URL, treating a non-absolute URL, a blank host or a userinfo,
     * query or fragment component as non-egressable; a parse failure is refusal and never echoes the original URL.
     */
    private URI parseBaseUrl(LlmModelSnapshotBO.ChannelBO channel) {
        try {
            URI endpoint = URI.create(StringUtils.trimToEmpty(channel.getBaseUrl()));
            if (!endpoint.isAbsolute() || StringUtils.isBlank(endpoint.getHost())
                    || endpoint.getUserInfo() != null || endpoint.getQuery() != null
                    || endpoint.getFragment() != null) {
                return null;
            }
            return endpoint;
        } catch (IllegalArgumentException rejected) {
            log.warn("llm channel base url unparsable channel={}", channel.getChannelKey());
            return null;
        }
    }

    private LlmDeploymentEnum deploymentOf(LlmModelSnapshotBO.RouteBO route) {
        LlmModelSnapshotBO.ChannelBO channel = route.getChannel();
        return channel == null ? null : channel.getDeployment();
    }

    /**
     * 中文说明：把候选按 priority 升序分组（数值越小越先尝试），组内按 weight 不放回加权抽样，截断到
     * {@value #MAX_CANDIDATES}；加权只在已经通过全部准入判定的候选之间发生。
     * English summary: Groups candidates into ascending priority bands (a smaller value is tried first), draws each band by
     * weighted sampling without replacement and truncates to {@value #MAX_CANDIDATES}; weighting only ever happens among
     * candidates that already passed every admission test.
     */
    private List<LlmModelSnapshotBO.RouteBO> orderByPriorityThenWeight(
            List<LlmModelSnapshotBO.RouteBO> eligible) {
        List<LlmModelSnapshotBO.RouteBO> remaining = new ArrayList<>(eligible);
        List<LlmModelSnapshotBO.RouteBO> ordered = new ArrayList<>(Math.min(MAX_CANDIDATES, remaining.size()));
        while (!remaining.isEmpty() && ordered.size() < MAX_CANDIDATES) {
            int topPriority = remaining.stream()
                    .map(LlmModelSnapshotBO.RouteBO::getPriority)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(0);
            List<LlmModelSnapshotBO.RouteBO> group = remaining.stream()
                    .filter(route -> Objects.equals(route.getPriority(), topPriority))
                    .toList();
            remaining.removeAll(group);
            ordered.addAll(weightDraw(group));
        }
        return List.copyOf(ordered);
    }

    /**
     * 中文说明：同优先级内的 weight 抽样：按权重不放回地依次抽取，weight 缺失或非正时按 1 参与，不改变已授权的候选集合。
     * English summary: Weighted ordering inside one priority: draws without replacement proportional to weight, treating a missing
     * or non-positive weight as 1, without ever changing the already authorized candidate set.
     */
    private List<LlmModelSnapshotBO.RouteBO> weightDraw(List<LlmModelSnapshotBO.RouteBO> group) {
        List<LlmModelSnapshotBO.RouteBO> pool = new ArrayList<>(group);
        List<LlmModelSnapshotBO.RouteBO> drawn = new ArrayList<>(pool.size());
        while (!pool.isEmpty()) {
            int totalWeight = pool.stream().mapToInt(this::weightOf).sum();
            int ticket = totalWeight <= 0 ? 0 : ThreadLocalRandom.current().nextInt(totalWeight);
            int cursor = 0;
            int picked = 0;
            for (int index = 0; index < pool.size(); index++) {
                cursor += weightOf(pool.get(index));
                if (ticket < cursor) {
                    picked = index;
                    break;
                }
            }
            drawn.add(pool.remove(picked));
        }
        return drawn;
    }

    private int weightOf(LlmModelSnapshotBO.RouteBO route) {
        Integer weight = route.getWeight();
        return weight == null || weight < 1 ? 1 : weight;
    }

    private boolean matchesHost(String host, List<String> allowList) {
        return allowList != null && allowList.stream()
                .anyMatch(entry -> host.equalsIgnoreCase(normalizeHost(entry)));
    }

    private boolean matchesCidrOrHost(String host, List<String> allowList) {
        return allowList != null && allowList.stream().anyMatch(entry -> entry.contains("/")
                ? inCidr(host, entry) : host.equalsIgnoreCase(normalizeHost(entry)));
    }

    /**
     * 中文说明：IPv4 CIDR 归属判定；主机名或非法定字面量一律不匹配，宁可拒绝也不做 DNS 解析式猜测。
     * English summary: IPv4 CIDR containment; host names and ill-formed literals never match, preferring refusal over any
     * DNS-resolution guess.
     */
    private boolean inCidr(String host, String cidr) {
        String[] network = StringUtils.split(cidr, '/');
        if (network == null || network.length != 2 || !isPrefixLiteral(network[1])) {
            return false;
        }
        byte[] candidate = ipv4(host);
        byte[] base = ipv4(network[0]);
        if (candidate == null || base == null) {
            return false;
        }
        int prefix = Integer.parseInt(network[1]);
        if (prefix > 32) {
            return false;
        }
        for (int bit = 0; bit < prefix; bit++) {
            int mask = 0x80 >>> (bit % 8);
            if ((candidate[bit / 8] & mask) != (base[bit / 8] & mask)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 中文说明：前缀长度只接受一至两位十进制数字，因此永不抛出数字解析异常；非法写法一律按不匹配处理。
     * English summary: a prefix length accepts only one or two decimal digits, so no numeric parse exception can escape;
     * ill-formed spellings simply never match.
     */
    private boolean isPrefixLiteral(String prefix) {
        return prefix.length() >= 1 && prefix.length() <= 2 && StringUtils.isNumeric(prefix);
    }

    private byte[] ipv4(String literal) {
        String[] octets = StringUtils.split(literal, '.');
        if (octets == null || octets.length != 4) {
            return null;
        }
        byte[] bytes = new byte[4];
        for (int index = 0; index < 4; index++) {
            if (!StringUtils.isNumeric(octets[index]) || octets[index].length() > 3) {
                return null;
            }
            int value = Integer.parseInt(octets[index]);
            if (value > 255) {
                return null;
            }
            bytes[index] = (byte) value;
        }
        return bytes;
    }

    private String normalizeHost(String host) {
        String trimmed = StringUtils.trimToEmpty(host).toLowerCase();
        return StringUtils.removeEnd(trimmed, ".");
    }
}
