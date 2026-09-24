package top.egon.cola.component.yuheng.admin.llm.service.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.yuheng.admin.knowledge.domain.vo.KnowledgePageVO;
import top.egon.cola.component.yuheng.admin.llm.domain.bo.LlmChannelBO;
import top.egon.cola.component.yuheng.admin.llm.domain.bo.LlmModelBO;
import top.egon.cola.component.yuheng.admin.llm.domain.dto.LlmChannelCommandDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.dto.LlmModelCommandDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.dto.LlmRouteBindingDTO;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmDeploymentEnum;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmModelKindEnum;
import top.egon.cola.component.yuheng.admin.llm.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.admin.llm.domain.vo.LlmChannelVO;
import top.egon.cola.component.yuheng.admin.llm.domain.vo.LlmModelVO;
import top.egon.cola.component.yuheng.admin.llm.repository.LlmConfigurationRepository;
import top.egon.cola.component.yuheng.admin.llm.service.LlmConfigurationService;
import top.egon.cola.component.yuheng.admin.observability.domain.bo.GatewayAuditLogBO;
import top.egon.cola.component.yuheng.admin.observability.repository.GatewayAuditLogRepository;
import top.egon.cola.component.yuheng.admin.shared.domain.AdminActor;
import top.egon.cola.component.yuheng.admin.shared.domain.RequestAuditContext;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminNotFoundException;
import top.egon.cola.component.yuheng.admin.shared.domain.exception.GatewayAdminRevisionConflictException;

/**
 * 中文说明：{@code LlmConfigurationServiceImpl} 是 {@link LlmConfigurationService} 的实现，逐字落实原
 * API-004–007 的业务顺序：注解校验先于身份、身份先于当前状态与业务 {@code revision}、再是有界仓储复核，
 * 最后才是同事务写入与审计。渠道保存额外复核 baseUrl 结构、timeout 支配关系与 LOCAL embedding 反向引用；
 * 模型保存额外按库内已存渠道复核每条 route 的存在性、协议集合与 EMBEDDING 的 LOCAL 约束，
 * 并保证已使用嵌入空间的 {@code embeddingSpaceId}/{@code dimensions} 不可变更。
 * English summary: {@code LlmConfigurationServiceImpl} implements {@link LlmConfigurationService} and keeps the original
 * API-004–007 ordering: annotation validation before identity, identity before current state and business {@code revision},
 * then bounded repository reads, and only then the write plus its audit inside one transaction. Channel saves add the
 * baseUrl shape, the timeout dominance rule and the LOCAL embedding reverse references; model saves re-check every route
 * against the stored channels (existence, protocol set membership and the EMBEDDING LOCAL constraint) and keep
 * {@code embeddingSpaceId}/{@code dimensions} immutable once the space is in use.
 *
 * 用法 / Usage: 由 {@code llmConfigurationController} 以限定名注入；写事务由本类的 {@code @Transactional} 持有，
 * 仓储只做受守卫的 CAS 与权威值回写，跨进程不新增状态。任何失败一律按既有机制抛出：
 * 缺失资源 {@link GatewayAdminNotFoundException}（404）、版本/key/状态冲突
 * {@link GatewayAdminRevisionConflictException}（409）、字段不合法 {@link IllegalArgumentException}（422），
 * 日志只记录稳定 key、协议与 revision，绝不记录 baseUrl 之外的敏感内容或任何密钥解析值。/ Inject it by qualifier into the
 * controller; this class owns the write transaction while the repository only performs guarded compare-and-set and
 * authoritative write-back, and no cross-process state is added. Failures reuse the existing mechanisms only, and logs
 * carry stable keys, protocols and revisions, never a resolved secret.
 */
@Slf4j
@Validated
@Service("llmConfigurationServiceImpl")
@RequiredArgsConstructor
public class LlmConfigurationServiceImpl implements LlmConfigurationService {

    /** 渠道审计资源类型 / audit resource type of one LLM channel. */
    private static final String CHANNEL_RESOURCE = "YUHENG_LLM_CHANNEL";

    /** 模型审计资源类型 / audit resource type of one LLM model alias. */
    private static final String MODEL_RESOURCE = "YUHENG_LLM_MODEL";

    /** 审计来源标识，与其他管理端写入一致 / audit source marker shared with the other management writes. */
    private static final String MANAGEMENT_SOURCE = "MANAGEMENT_API";

    /** 创建意图的乐观版本哨兵值 / the optimistic revision sentinel that means create intent. */
    private static final long CREATE_REVISION = 0L;

    @Qualifier("llmConfigurationRepository")
    private final LlmConfigurationRepository configuration;

    @Qualifier("gatewayAuditLogRepository")
    private final GatewayAuditLogRepository audits;

    @Qualifier("gatewayProjectionClock")
    private final Clock clock;

    /**
     * 中文说明：执行 listChannels 操作；只读短事务内先取匹配总数再取当页，投影为纯业务 VO，
     * 空页返回 {@code []}，不触发健康探测也不解析任何密钥。
     * English summary: Executes the listChannels operation; one read-only transaction takes the matched total and then the
     * page, projects pure business VOs, answers {@code []} for an empty page, and neither probes health nor resolves a
     * secret.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationServiceImpl.listChannels(page, size)}。
     * @param page 参数 页码，从 1 开始；parameter one-based page number.
     * @param size 参数 页大小，1–100；parameter effective page size.
     * @return 返回 渠道分页投影；returns the paged channel projection.
     */
    @Override
    @Transactional(readOnly = true)
    public KnowledgePageVO<LlmChannelVO> listChannels(
            int page,
            int size) {
        long total = configuration.countChannels();
        List<LlmChannelVO> items = configuration.findChannelPage(page, size)
                .stream()
                .map(this::channelView)
                .toList();
        return new KnowledgePageVO<>(items, page, size, total);
    }

    /**
     * 中文说明：执行 replaceChannel 操作；在同一写事务内完成“路径与命令 key 一致 → baseUrl/timeout 结构复核 →
     * 读取当前行并应用 404/409 规则 → LOCAL embedding 反向引用复核 → 受守卫 CAS 保存 → 同事务审计”，
     * 影响 0 行由仓储如实抛出冲突而不是伪造成功。
     * English summary: Executes the replaceChannel operation inside one write transaction in the order
     * “path key equals command key → baseUrl/timeout shape → load the current row and apply the 404/409 rules →
     * LOCAL embedding reverse references → guarded compare-and-set save → audit in the same transaction”, and a
     * zero-row effect is surfaced honestly by the repository instead of a fake success.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationServiceImpl.replaceChannel(channelKey, command, actor, request)}；
     * 返回体的 {@code revision} 是服务端推进后的权威值，控制器据此结合 {@code expectedRevision} 决定 201 或 200。
     * @param channelKey 参数 路径渠道稳定 key；parameter channel stable key from the path.
     * @param command 参数 完整保存命令；parameter the full replace command.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param request 参数 请求审计上下文；parameter the request audit context.
     * @return 返回 已提交的渠道投影；returns the committed channel projection.
     */
    @Override
    @Transactional
    public LlmChannelVO replaceChannel(
            String channelKey,
            LlmChannelCommandDTO command,
            AdminActor actor,
            RequestAuditContext request) {
        LlmChannelBO current = configuration.findChannel(channelKey).orElse(null);
        Long stored = current == null ? null : current.getRevision();
        assertKeyMatchesPath(channelKey, command.getKey(), stored);
        assertBaseUrl(command.getDeployment(), command.getBaseUrl());
        assertTimeoutRelations(command);
        assertRevision(stored, command.getExpectedRevision(), channelKey, "channel");
        assertEmbeddingReverseReferences(channelKey, command.getDeployment(), command.getProtocol());
        LlmChannelBO saved = configuration.saveChannel(channelCarrier(channelKey, command));
        audit(
                actor,
                request,
                CHANNEL_RESOURCE,
                channelKey,
                current == null ? "CREATE" : "UPDATE",
                current == null ? null : channelSummary(current),
                channelSummary(saved)
        );
        log.info(
                "YUHENG_LLM_CHANNEL_SAVED channelKey={} protocol={} deployment={} revision={}",
                channelKey,
                saved.getProtocol(),
                saved.getDeployment(),
                saved.getRevision()
        );
        return channelView(saved);
    }

    /**
     * 中文说明：执行 listModels 操作；只读短事务内按总数＋当页返回模型 alias 与能力映射投影，
     * routes 原样复用 {@link LlmRouteBindingDTO}，不在此处发起任何上游调用。
     * English summary: Executes the listModels operation; one read-only transaction returns the matched total plus the
     * page of model aliases and their capability mappings, reusing {@link LlmRouteBindingDTO} verbatim and issuing no
     * upstream call.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationServiceImpl.listModels(page, size)}。
     * @param page 参数 页码，从 1 开始；parameter one-based page number.
     * @param size 参数 页大小，1–100；parameter effective page size.
     * @return 返回 模型分页投影；returns the paged model projection.
     */
    @Override
    @Transactional(readOnly = true)
    public KnowledgePageVO<LlmModelVO> listModels(
            int page,
            int size) {
        long total = configuration.countModels();
        List<LlmModelVO> items = configuration.findModelPage(page, size)
                .stream()
                .map(this::modelView)
                .toList();
        return new KnowledgePageVO<>(items, page, size, total);
    }

    /**
     * 中文说明：执行 replaceModel 操作；在渠道操作的同一套 key/版本规则之上，按库内已存渠道复核每条 route：
     * 渠道必须存在、其协议必须落在模型 {@code protocols} 集合内，且 EMBEDDING 模型的每条 route 必须落在
     * {@code deployment=LOCAL} 的渠道上，违反按字段不合法拒绝而不扩大到全部渠道；
     * 已绑定嵌入空间的 alias 不得改变 {@code embeddingSpaceId} 或 {@code dimensions}。
     * English summary: Executes the replaceModel operation on top of the same key and revision rules as the channel
     * operation and re-checks every route against the stored channels: the channel must exist, its protocol must belong to
     * the model {@code protocols} set, and every route of an EMBEDDING model must sit on a {@code deployment=LOCAL}
     * channel, a violation being rejected as an invalid field instead of a widening to all channels; an alias already
     * bound to an embedding space can change neither {@code embeddingSpaceId} nor {@code dimensions}.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code llmConfigurationServiceImpl.replaceModel(modelKey, command, actor, request)}。
     * @param modelKey 参数 路径模型稳定 key；parameter model stable key from the path.
     * @param command 参数 完整保存命令；parameter the full replace command.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param request 参数 请求审计上下文；parameter the request audit context.
     * @return 返回 已提交的模型投影；returns the committed model projection.
     */
    @Override
    @Transactional
    public LlmModelVO replaceModel(
            String modelKey,
            LlmModelCommandDTO command,
            AdminActor actor,
            RequestAuditContext request) {
        LlmModelBO current = configuration.findModel(modelKey).orElse(null);
        Long stored = current == null ? null : current.getRevision();
        assertKeyMatchesPath(modelKey, command.getKey(), stored);
        assertRevision(stored, command.getExpectedRevision(), modelKey, "model");
        assertRoutes(command);
        assertEmbeddingSpaceImmutable(current, command);
        LlmModelBO saved = configuration.saveModel(modelCarrier(modelKey, command));
        audit(
                actor,
                request,
                MODEL_RESOURCE,
                modelKey,
                current == null ? "CREATE" : "UPDATE",
                current == null ? null : modelSummary(current),
                modelSummary(saved)
        );
        log.info(
                "YUHENG_LLM_MODEL_SAVED modelKey={} kind={} routes={} revision={}",
                modelKey,
                saved.getKind(),
                saved.getRoutes() == null ? 0 : saved.getRoutes().size(),
                saved.getRevision()
        );
        return modelView(saved);
    }

    /**
     * 中文说明：路径 key 与命令 key 必须一致，否则属于原合同声明的 key/状态冲突（409）；
     * 命中已存在行时如实携带库中权威 revision，未知资源则为创建哨兵值。
     * English summary: The path key and the command key must agree or the contract's key/state conflict (409) applies; when
     * the row exists the stored authoritative revision is carried honestly and otherwise the create sentinel is used.
     * @param pathKey 参数 路径 key；parameter the path key.
     * @param commandKey 参数 命令 key；parameter the command key.
     * @param storedRevision 参数 库中 revision，缺失为 null；parameter the stored revision, null when absent.
     */
    private static void assertKeyMatchesPath(
            String pathKey,
            String commandKey,
            Long storedRevision) {
        if (!Objects.equals(pathKey, commandKey)) {
            throw new GatewayAdminRevisionConflictException(
                    storedRevision == null ? CREATE_REVISION : storedRevision
            );
        }
    }

    /**
     * 中文说明：应用原合同的乐观版本规则：缺失且 {@code expectedRevision>0} 为 404，存在且 expected 不等于库中
     * revision 为 409 并携带现值；{@code expectedRevision=0} 且缺失即创建意图。
     * English summary: Applies the contract's optimistic revision rules: absent with a positive expectation is 404, present
     * with a mismatched expectation is 409 carrying the stored revision, and absent with {@code expectedRevision=0} is the
     * create intent.
     * @param storedRevision 参数 库中 revision，缺失为 null；parameter the stored revision, null when absent.
     * @param expectedRevision 参数 调用方期望 revision；parameter the caller reported expectation.
     * @param resourceKey 参数 资源稳定 key；parameter the stable resource key.
     * @param resourceWord 参数 审计与消息中的资源词；parameter the resource word used in messages.
     */
    private static void assertRevision(
            Long storedRevision,
            long expectedRevision,
            String resourceKey,
            String resourceWord) {
        if (storedRevision == null) {
            if (expectedRevision != CREATE_REVISION) {
                throw new GatewayAdminNotFoundException(
                        "LLM " + resourceWord + " " + resourceKey + " was not found"
                );
            }
            return;
        }
        if (storedRevision != expectedRevision) {
            throw new GatewayAdminRevisionConflictException(storedRevision);
        }
    }

    /**
     * 中文说明：结构复核 baseUrl：必须是绝对 http/https URI，不得携带 userinfo、query 或 fragment，
     * CLOUD 部署只允许 HTTPS；主机白名单属于运行期出域策略与运维配置，管理面不复制该职责。
     * English summary: Re-checks the baseUrl structurally: an absolute http/https URI without userinfo, query or fragment,
     * with HTTPS mandatory for CLOUD; the host allowlist stays a runtime egress and operator configuration concern and is
     * not duplicated by the management plane.
     * @param deployment 参数 部署形态；parameter the trusted deployment.
     * @param baseUrl 参数 上游根地址；parameter the upstream API root.
     */
    private static void assertBaseUrl(
            LlmDeploymentEnum deployment,
            String baseUrl) {
        URI uri = parseBaseUrl(baseUrl);
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("baseUrl scheme must be http or https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("baseUrl must carry an explicit host");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("baseUrl must not carry user information");
        }
        if (uri.getQuery() != null) {
            throw new IllegalArgumentException("baseUrl must not carry a query component");
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("baseUrl must not carry a fragment component");
        }
        if (LlmDeploymentEnum.CLOUD == deployment && !"https".equals(scheme)) {
            throw new IllegalArgumentException("CLOUD deployment requires an https baseUrl");
        }
    }

    /**
     * 中文说明：按 JDK URI 语法解析根地址，语法错误按字段不合法（422）如实报告，不放宽为部分接受。
     * English summary: Parses the API root with the JDK URI syntax and reports a syntax failure honestly as an invalid
     * field (422) instead of accepting it partially.
     * @param baseUrl 参数 上游根地址；parameter the upstream API root.
     * @return 返回 解析后的绝对 URI；returns the parsed absolute URI.
     */
    private static URI parseBaseUrl(String baseUrl) {
        try {
            URI uri = new URI(StringUtils.trim(baseUrl));
            if (!uri.isAbsolute()) {
                throw new IllegalArgumentException("baseUrl must be an absolute URL");
            }
            return uri;
        } catch (URISyntaxException invalid) {
            throw new IllegalArgumentException("baseUrl is not a valid absolute URL");
        }
    }

    /**
     * 中文说明：复核 {@code totalTimeoutMs} 不小于 connect、header、idle 三个 timeout；这是跨字段规则，
     * 单字段范围由载体注解负责。
     * English summary: Checks that {@code totalTimeoutMs} dominates the connect, header and idle timeouts; this is the
     * cross-field rule while the single-field ranges stay with the carrier annotations.
     * @param command 参数 渠道命令；parameter the channel command.
     */
    private static void assertTimeoutRelations(LlmChannelCommandDTO command) {
        int total = command.getTotalTimeoutMs();
        if (total < command.getConnectTimeoutMs()) {
            throw new IllegalArgumentException("totalTimeoutMs must not be smaller than connectTimeoutMs");
        }
        if (total < command.getHeaderTimeoutMs()) {
            throw new IllegalArgumentException("totalTimeoutMs must not be smaller than headerTimeoutMs");
        }
        if (total < command.getIdleTimeoutMs()) {
            throw new IllegalArgumentException("totalTimeoutMs must not be smaller than idleTimeoutMs");
        }
    }

    /**
     * 中文说明：LOCAL embedding 反向引用复核：任何把 route 指向本渠道的 EMBEDDING 模型都必须继续落在
     * {@code deployment=LOCAL} 的渠道上，且本渠道协议仍在该模型 {@code protocols} 集合内；
     * 违反属于原合同声明的状态冲突（409），绝不静默放宽或转云。停用（enabled=false）不删历史引用，因此不在此处放宽。
     * English summary: Re-checks the LOCAL embedding reverse references: every EMBEDDING model routing at this channel must
     * keep pointing at a {@code deployment=LOCAL} channel and this channel's protocol must stay inside that model's
     * {@code protocols} set; a violation is the contract's state conflict (409) and never a silent widening or a cloud
     * fallback. Disabling keeps historical references, so it is not relaxed here.
     * @param channelKey 参数 渠道稳定 key；parameter the stable channel key.
     * @param deployment 参数 新的部署形态；parameter the incoming deployment.
     * @param protocol 参数 新的上游协议；parameter the incoming upstream protocol.
     */
    private void assertEmbeddingReverseReferences(
            String channelKey,
            LlmDeploymentEnum deployment,
            LlmProtocolEnum protocol) {
        for (LlmModelBO model : configuration.findModelsByChannelKey(channelKey)) {
            if (LlmModelKindEnum.EMBEDDING != model.getKind()) {
                continue;
            }
            if (LlmDeploymentEnum.LOCAL != deployment) {
                throw new GatewayAdminRevisionConflictException(
                        model.getRevision()
                );
            }
            if (!model.getProtocols().contains(protocol)) {
                throw new GatewayAdminRevisionConflictException(
                        model.getRevision()
                );
            }
        }
    }

    /**
     * 中文说明：按库内已存渠道复核模型 route：渠道存在、协议属于 {@code protocols}、EMBEDDING 只允许 LOCAL 渠道；
     * 一次性批量读取路由渠道（最多 16 条）而不是逐条查询，失败按字段不合法（422）给出可定位路径。
     * English summary: Re-checks model routes against the stored channels: the channel must exist, its protocol must belong
     * to {@code protocols}, and EMBEDDING may only route at LOCAL channels. The routed channels are read in one bounded
     * batch of at most 16 keys instead of per-route queries, and a failure names the addressable field path (422).
     * @param command 参数 模型命令；parameter the model command.
     */
    private void assertRoutes(LlmModelCommandDTO command) {
        List<LlmRouteBindingDTO> routes = command.getRoutes();
        Set<String> keys = new LinkedHashSet<>();
        for (LlmRouteBindingDTO route : routes) {
            keys.add(route.getChannelKey());
        }
        Map<String, LlmChannelBO> channels = channelsByRouteKey(keys);
        List<LlmProtocolEnum> protocols = command.getProtocols();
        boolean embedding = LlmModelKindEnum.EMBEDDING == command.getKind();
        for (int index = 0; index < routes.size(); index = index + 1) {
            LlmRouteBindingDTO route = routes.get(index);
            LlmChannelBO channel = channels.get(route.getChannelKey());
            if (channel == null) {
                throw new IllegalArgumentException(
                        "routes[" + index + "].channelKey references an unknown LLM channel"
                );
            }
            if (!protocols.contains(channel.getProtocol())) {
                throw new IllegalArgumentException(
                        "routes[" + index + "].channelKey protocol is not inside the model protocols"
                );
            }
            if (embedding && LlmDeploymentEnum.LOCAL != channel.getDeployment()) {
                throw new IllegalArgumentException(
                        "routes[" + index + "].channelKey must address a LOCAL deployment channel for an EMBEDDING model"
                );
            }
        }
    }

    /**
     * 中文说明：把路由指向的渠道 key 集合一次性解析为按 key 索引的渠道行；重复 key 只读一次，未知 key 不进入映射。
     * English summary: Resolves the routed channel keys into rows indexed by key in one batch; a repeated key is read once
     * and an unknown key simply stays absent from the map.
     * @param channelKeys 参数 去重后的路由渠道 key 集合；parameter the deduplicated routed channel keys.
     * @return 返回 按 key 索引的渠道行；returns the rows indexed by key.
     */
    private Map<String, LlmChannelBO> channelsByRouteKey(Collection<String> channelKeys) {
        Map<String, LlmChannelBO> indexed = new LinkedHashMap<>();
        if (channelKeys.isEmpty()) {
            return indexed;
        }
        for (LlmChannelBO channel : configuration.findChannelsByKeys(channelKeys)) {
            indexed.putIfAbsent(channel.getChannelKey(), channel);
        }
        return indexed;
    }

    /**
     * 中文说明：嵌入空间不变式：一旦库中行已携带 {@code embeddingSpaceId}，命令不得改变嵌入空间标识或维度，
     * 需要新语义时使用新 alias，避免历史向量被错误解释。
     * English summary: The embedding space invariant: once the stored row carries an {@code embeddingSpaceId}, the command
     * may change neither that identifier nor the dimensions, because new semantics require a new alias instead of silently
     * misreading historical vectors.
     * @param current 参数 当前模型行，可为空；parameter the current model row, possibly absent.
     * @param command 参数 模型命令；parameter the model command.
     */
    private static void assertEmbeddingSpaceImmutable(
            LlmModelBO current,
            LlmModelCommandDTO command) {
        if (current == null || current.getEmbeddingSpaceId() == null) {
            return;
        }
        if (!Objects.equals(current.getEmbeddingSpaceId(), command.getEmbeddingSpaceId())
                || !Objects.equals(current.getDimensions(), command.getDimensions())) {
            throw new IllegalArgumentException(
                    "embeddingSpaceId and dimensions are immutable once the embedding space is in use"
            );
        }
    }

    /**
     * 中文说明：把受信命令映射为渠道业务载体：展示名称 trim、密钥只保留引用名、
     * {@code revision} 取调用方期望值交由仓储做 CAS 与递增。
     * English summary: Maps the trusted command onto the channel business carrier: the display name is trimmed, only the
     * secret reference name is kept, and {@code revision} carries the caller expectation for the repository to
     * compare-and-set and increment.
     * @param channelKey 参数 路径渠道 key；parameter the path channel key.
     * @param command 参数 渠道命令；parameter the channel command.
     * @return 返回 渠道业务载体；returns the channel business carrier.
     */
    private static LlmChannelBO channelCarrier(
            String channelKey,
            LlmChannelCommandDTO command) {
        return LlmChannelBO.builder()
                .channelKey(channelKey)
                .name(StringUtils.trim(command.getName()))
                .deployment(command.getDeployment())
                .protocol(command.getProtocol())
                .baseUrl(StringUtils.trim(command.getBaseUrl()))
                .secretRef(StringUtils.trimToNull(command.getSecretRef()))
                .enabled(command.getEnabled())
                .connectTimeoutMs(command.getConnectTimeoutMs())
                .headerTimeoutMs(command.getHeaderTimeoutMs())
                .idleTimeoutMs(command.getIdleTimeoutMs())
                .totalTimeoutMs(command.getTotalTimeoutMs())
                .maxConcurrent(command.getMaxConcurrent())
                .revision(command.getExpectedRevision())
                .build();
    }

    /**
     * 中文说明：把受信命令映射为模型业务载体，集合字段以防御性副本进入载体，避免命令在异步序列化期间被改写。
     * English summary: Maps the trusted command onto the model business carrier with defensive copies of the collection
     * fields so a command cannot be mutated while it is being processed.
     * @param modelKey 参数 路径模型 key；parameter the path model key.
     * @param command 参数 模型命令；parameter the model command.
     * @return 返回 模型业务载体；returns the model business carrier.
     */
    private static LlmModelBO modelCarrier(
            String modelKey,
            LlmModelCommandDTO command) {
        return LlmModelBO.builder()
                .modelKey(modelKey)
                .name(StringUtils.trim(command.getName()))
                .kind(command.getKind())
                .protocols(copy(command.getProtocols()))
                .enabled(command.getEnabled())
                .dimensions(command.getDimensions())
                .embeddingSpaceId(StringUtils.trimToNull(command.getEmbeddingSpaceId()))
                .allowedSubjects(copy(command.getAllowedSubjects()))
                .routes(copy(command.getRoutes()))
                .revision(command.getExpectedRevision())
                .build();
    }

    /**
     * 中文说明：写入时复制来源列表，null 保持 null 以尊重 CHAT 的空可选字段合同，不用空集合冒充值。
     * English summary: Copies the incoming list defensively and keeps null as null so the CHAT empty-optional contract is
     * respected instead of faking a value with an empty collection.
     * @param source 参数 来源列表；parameter the source list.
     * @return 返回 副本或 null；returns the copy or null.
     */
    private static <T> List<T> copy(List<T> source) {
        return source == null ? null : List.copyOf(source);
    }

    /**
     * 中文说明：把渠道业务载体投影为 API-004/005 完整响应，只输出 secretRef 引用名。
     * English summary: Projects the channel business carrier onto the API-004/005 response, emitting the secretRef name only.
     * @param carrier 参数 渠道载体；parameter the channel carrier.
     * @return 返回 渠道响应；returns the channel response.
     */
    private LlmChannelVO channelView(LlmChannelBO carrier) {
        return LlmChannelVO.builder()
                .key(carrier.getChannelKey())
                .name(carrier.getName())
                .deployment(carrier.getDeployment())
                .protocol(carrier.getProtocol())
                .baseUrl(carrier.getBaseUrl())
                .secretRef(carrier.getSecretRef())
                .enabled(carrier.getEnabled())
                .connectTimeoutMs(carrier.getConnectTimeoutMs())
                .headerTimeoutMs(carrier.getHeaderTimeoutMs())
                .idleTimeoutMs(carrier.getIdleTimeoutMs())
                .totalTimeoutMs(carrier.getTotalTimeoutMs())
                .maxConcurrent(carrier.getMaxConcurrent())
                .revision(carrier.getRevision())
                .build();
    }

    /**
     * 中文说明：把模型业务载体投影为 API-006/007 完整响应，枚举保持原 wire 字符串，CHAT 的空字段不被替换为空字符串。
     * English summary: Projects the model business carrier onto the API-006/007 response, keeping wire-string enums and
     * leaving the CHAT empties unset instead of substituting empty strings.
     * @param carrier 参数 模型载体；parameter the model carrier.
     * @return 返回 模型响应；returns the model response.
     */
    private LlmModelVO modelView(LlmModelBO carrier) {
        return LlmModelVO.builder()
                .key(carrier.getModelKey())
                .name(carrier.getName())
                .kind(carrier.getKind())
                .protocols(carrier.getProtocols())
                .enabled(carrier.getEnabled())
                .dimensions(carrier.getDimensions())
                .embeddingSpaceId(carrier.getEmbeddingSpaceId())
                .allowedSubjects(carrier.getAllowedSubjects())
                .routes(carrier.getRoutes())
                .revision(carrier.getRevision())
                .build();
    }

    /**
     * 中文说明：渠道审计摘要，只取可信配置事实与 revision，不含 baseUrl 之外的敏感内容与任何密钥值。
     * English summary: The channel audit summary, carrying only the trusted configuration facts and the revision, never a
     * resolved secret.
     * @param carrier 参数 渠道载体；parameter the channel carrier.
     * @return 返回 审计摘要；returns the audit summary.
     */
    private static Map<String, Object> channelSummary(LlmChannelBO carrier) {
        return Map.of(
                "deployment", String.valueOf(carrier.getDeployment()),
                "protocol", String.valueOf(carrier.getProtocol()),
                "enabled", String.valueOf(carrier.getEnabled()),
                "revision", String.valueOf(carrier.getRevision())
        );
    }

    /**
     * 中文说明：模型审计摘要，记录 kind、路由数量与 revision，不记录上游模型名或调用方主体清单。
     * English summary: The model audit summary, recording kind, route count and revision but neither upstream model names
     * nor the caller subject list.
     * @param carrier 参数 模型载体；parameter the model carrier.
     * @return 返回 审计摘要；returns the audit summary.
     */
    private static Map<String, Object> modelSummary(LlmModelBO carrier) {
        return Map.of(
                "kind", String.valueOf(carrier.getKind()),
                "routes", String.valueOf(carrier.getRoutes() == null ? 0 : carrier.getRoutes().size()),
                "enabled", String.valueOf(carrier.getEnabled()),
                "revision", String.valueOf(carrier.getRevision())
        );
    }

    /**
     * 中文说明：在调用方事务内写入一条管理端审计；摘要先经 {@code sanitized} 过滤，
     * 任何包含 secret/token/authorization/cookie 的键都不会落库。
     * English summary: Writes one management audit row inside the caller's transaction, the summaries first passing through
     * {@code sanitized} so no key containing secret, token, authorization or cookie reaches storage.
     * @param actor 参数 已验证的管理身份；parameter the verified management actor.
     * @param request 参数 请求审计上下文；parameter the request audit context.
     * @param resourceType 参数 资源类型；parameter the resource type.
     * @param resourceId 参数 资源稳定 key；parameter the stable resource key.
     * @param action 参数 动作；parameter the action.
     * @param before 参数 变更前后摘要；parameter the before summary, null on create.
     * @param after 参数 变更后摘要；parameter the after summary.
     */
    private void audit(
            AdminActor actor,
            RequestAuditContext request,
            String resourceType,
            String resourceId,
            String action,
            Map<String, Object> before,
            Map<String, Object> after) {
        audits.save(new GatewayAuditLogBO(
                SnowflakeIdGenerator.nextId(),
                actor.actorId(),
                actor.actorType().name(),
                MANAGEMENT_SOURCE,
                request.requestId(),
                request.traceId(),
                resourceType,
                resourceId,
                action,
                GatewayAuditLogBO.sanitized(before),
                GatewayAuditLogBO.sanitized(after),
                null,
                null,
                true,
                null,
                clock.instant()
        ));
    }
}
