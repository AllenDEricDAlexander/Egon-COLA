package top.egon.cola.component.yuheng.llm.proxy.repository.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.llm.config.LlmPersistenceContextComponent;
import top.egon.cola.component.yuheng.llm.proxy.dao.LlmChannelDAO;
import top.egon.cola.component.yuheng.llm.proxy.dao.LlmModelDAO;
import top.egon.cola.component.yuheng.llm.proxy.domain.bo.LlmModelSnapshotBO;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmCapabilityEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.enums.LlmProtocolEnum;
import top.egon.cola.component.yuheng.llm.proxy.domain.po.LlmChannelPO;
import top.egon.cola.component.yuheng.llm.proxy.domain.po.LlmModelPO;
import top.egon.cola.component.yuheng.llm.proxy.repository.LlmConfigurationRepository;
import top.egon.cola.component.yuheng.llm.proxy.repository.LlmModelSnapshotConverter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * 中文说明：{@code MpLlmConfigurationRepository} 是 {@link LlmConfigurationRepository} 的 MyBatis-Plus 实现，也是本进程
 * 读取 {@code gateway_llm_model}/{@code gateway_llm_channel} 的唯一受守卫边界：只调用 {@code LlmModelDAO.xml} 与
 * {@code LlmChannelDAO.xml} 里声明的具名只读语句，每条语句自带 {@code deleted_at IS NULL}，租户条件由组件的
 * {@code TenantLine} 依受信 MDC 注入并在执行前由最终 SQL 守卫复核，因此本类既不拼接 SQL、不接受调用方传入的租户，
 * 也不写任何 {@code QueryWrapper}。所有语句都在 {@link LlmPersistenceContextComponent#call(Callable)} 安装的部署身份内
 * 执行；本门面<b>不</b>声明 {@code @Transactional}：只读事务与 REPEATABLE_READ 快照隔离由调用方 Service 持有，
 * 事务必须在发起出站 HTTP 之前结束，所以这里连“顺手开一个事务”的余地都不提供。
 * PO 与持久对象一律不出端口：jsonb 列以编码文本读回后由 Jackson 解析为类型化 BO，缺行如实返回
 * {@link Optional#empty()}，配置不成立（route 数组缺失/越界/协议或能力 wire 值未知/渠道必填列为空）时抛出稳定错误码
 * 失败关闭，绝不伪造渠道、不吞异常、也不把读失败伪装成空成功。
 * English summary: {@code MpLlmConfigurationRepository} is the MyBatis-Plus implementation of
 * {@link LlmConfigurationRepository} and the only guarded boundary through which this process reads
 * {@code gateway_llm_model} and {@code gateway_llm_channel}. It calls only the named read statements declared in
 * {@code LlmModelDAO.xml} and {@code LlmChannelDAO.xml}, each of which carries its own {@code deleted_at IS NULL}, while
 * tenancy is injected from the trusted MDC by the component's {@code TenantLine} and re-proved by the final-SQL guard
 * before execution, so this class composes no SQL, accepts no caller-supplied tenant and writes no
 * {@code QueryWrapper}. Every statement runs inside the deployment identity installed by
 * {@link LlmPersistenceContextComponent#call(Callable)} and the facade declares <b>no</b> {@code @Transactional}: the
 * read-only transaction and its REPEATABLE_READ snapshot belong to the calling service and must end before any egress
 * HTTP, so not even an incidental transaction is opened here. Neither a PO nor a persistence object leaves the port:
 * jsonb columns come back as encoded text and are parsed by Jackson into typed BOs, a missing row is reported honestly
 * as {@link Optional#empty()}, and configuration that does not hold (an absent or out-of-range route array, an unknown
 * protocol or capability wire value, an empty required channel column; {@code secret_ref} is nullable only for a LOCAL
 * channel with no authentication) raises a stable error code instead of
 * fabricating a channel, swallowing the failure or pretending an empty success.
 *
 * 用法 / Usage: 由业务 Service 以 bean 名 {@code mpLlmConfigurationRepository} 注入；快照读一次请求一次，至多两次尝试
 * 复用同一份 BO。日志只记录 alias、条数与耗时量级的稳定标识，永不输出 {@code base_url}、{@code secret_ref} 或请求体。
 * / Inject it by the bean name {@code mpLlmConfigurationRepository}; the snapshot is read once per request and reused by
 * its at most two attempts. Logs carry only the alias, the row counts and other stable identities, never
 * {@code base_url}, {@code secret_ref} or a request body.
 */
@Slf4j
@Repository("mpLlmConfigurationRepository")
@RequiredArgsConstructor
@Validated
public class MpLlmConfigurationRepository implements LlmConfigurationRepository {

    /** 中文说明：单个模型 alias 允许声明的 route 上限，逐字对应 Spec §11.2.2 的 1–16 有序 route 记录。 English summary: the route ceiling for one alias, matching the ordered 1–16 route records of Spec §11.2.2 verbatim. */
    private static final int MAX_ROUTE_COUNT = 16;

    /** 中文说明：一次批量渠道点查的 key 上限，逐字对应 Spec §11.2 的 batch≤64。 English summary: the channel key ceiling of one batch lookup, matching the batch ≤ 64 bound of Spec §11.2 verbatim. */
    private static final int MAX_CHANNEL_KEYS = 64;

    /** 中文说明：目录读的行上限；两张配置表都被部署约束在各自 1000 行以内，因此有界读永不退化为全表扫描。 English summary: the catalog row ceiling; both configuration tables are deployment-capped at 1000 rows each, so a bounded read never degrades into a scan. */
    private static final int MAX_CATALOG_ROWS = 1000;

    /** 中文说明：必填整数列上界，仅用于拒绝明显损坏的配置。 English summary: the ceiling for required integer columns, used only to reject obviously damaged configuration. */
    private static final int MAX_TIMEOUT_MS = 86_400_000;

    /** 中文说明：jsonb 列解码映射器，只解析文本，不启用 default typing，也不参与任何 DTO/PO 的业务往返映射。 English summary: the jsonb decoding mapper; it parses text only, keeps default typing off and performs no DTO/PO business roundtrip. */
    private static final ObjectMapper JSONB_MAPPER = JsonMapper.builder().build();

    /** 中文说明：{@code gateway_llm_model} 的映射器，语句集合见 {@code mybatis/mapper/llm/LlmModelDAO.xml}。 English summary: the mapper for {@code gateway_llm_model}; its statements live in {@code mybatis/mapper/llm/LlmModelDAO.xml}. */
    @Qualifier("llmModelDAO")
    private final LlmModelDAO llmModelDAO;

    /** 中文说明：{@code gateway_llm_channel} 的映射器，语句集合见 {@code mybatis/mapper/llm/LlmChannelDAO.xml}。 English summary: the mapper for {@code gateway_llm_channel}; its statements live in {@code mybatis/mapper/llm/LlmChannelDAO.xml}. */
    @Qualifier("llmChannelDAO")
    private final LlmChannelDAO llmChannelDAO;

    /** 中文说明：部署绑定的受信持久化身份上下文，保证每条语句都在正确租户与技术服务主体的 MDC 下执行并在 finally 还原。 English summary: the trusted deployment-bound persistence identity context, so every statement runs under the right tenant and technical service principal MDC and is restored in a finally block. */
    @Qualifier("llmPersistenceContextComponent")
    private final LlmPersistenceContextComponent persistenceContextComponent;

    @Qualifier("llmModelSnapshotConverter")
    private final LlmModelSnapshotConverter snapshotConverter;

    /**
     * 中文说明：读取一个 alias 的只读一致快照：先按 {@code model_key} 具名点查模型行，再按其 route 声明的渠道 key
     * 发起<b>一次</b>有界 IN 查询，最后在同一次调用里合成 BO。缺行返回 {@link Optional#empty()}，route 引用了不存在
     * 或被软删的渠道时该 route 的渠道为 {@code null}，由调用方按“无渠道即拒绝”过滤。
     * English summary: Reads the read-only coherent snapshot of one alias: a named point read by {@code model_key}
     * first, then <b>one</b> bounded IN query for the channel keys its routes declare, composed into the BO within the
     * same call. An absent row yields {@link Optional#empty()}, and a route whose channel is missing or logically
     * deleted keeps a {@code null} channel for the caller to reject as "no channel".
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpLlmConfigurationRepository.findSnapshot(modelKey)}。
     * @param modelKey 参数 已校验的客户端 alias；parameter the validated client alias.
     * @return 返回 快照或 {@link Optional#empty()}；returns the snapshot or {@link Optional#empty()}.
     */
    @Override
    public Optional<LlmModelSnapshotBO> findSnapshot(String modelKey) {
        return read("snapshot", () -> {
            LlmModelPO model = llmModelDAO.selectActiveByModelKey(modelKey);
            if (model == null) {
                log.debug("llm configuration snapshot miss, modelKey={}", modelKey);
                return Optional.empty();
            }
            List<LlmModelSnapshotBO.RouteBO> routes = parseRoutes(model);
            Map<String, LlmChannelPO> channels = loadChannels(routes, modelKey);
            log.debug("llm configuration snapshot read, modelKey={} routes={} resolvedChannels={}",
                    modelKey, routes.size(), channels.size());
            return Optional.of(toSnapshot(model, routes, channels));
        });
    }

    /**
     * 中文说明：有界读取活跃模型目录，按稳定 {@code id} 升序；目录只解析 route 以维持载体不变式，<b>不</b>解析渠道，
     * 因此每条 route 的渠道恒为 {@code null}，目录读不能被当作路由依据。
     * English summary: Reads the active model catalog bounded and ordered by the stable {@code id} ascending; the catalog
     * parses routes only to keep the carrier invariant and deliberately resolves <b>no</b> channel, so every route
     * channel stays {@code null} and a catalog read is never a routing decision.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code MpLlmConfigurationRepository.findCatalog()}。
     * @return 返回 有界、按 id 升序的快照列表，无配置时为空列表；returns the bounded id-ascending snapshots, an empty list when nothing is configured.
     */
    @Override
    public List<LlmModelSnapshotBO> findCatalog() {
        return read("catalog", () -> {
            List<LlmModelPO> rows = llmModelDAO.selectActiveCatalog(MAX_CATALOG_ROWS);
            if (rows == null || rows.isEmpty()) {
                return List.of();
            }
            List<LlmModelSnapshotBO> snapshots = new ArrayList<>(rows.size());
            for (LlmModelPO row : rows) {
                snapshots.add(toSnapshot(row, parseRoutes(row), Map.of()));
            }
            log.debug("llm configuration catalog read, rows={} ceiling={}", snapshots.size(), MAX_CATALOG_ROWS);
            return List.copyOf(snapshots);
        });
    }

    /**
     * 中文说明：在部署绑定的受信持久化身份内执行一次只读工作；受守卫抛出的运行时异常原样传播，不吞栈也不伪造结果，
     * 受检异常统一收敛为稳定错误码，因为调用方只能按码判断“配置读失败”，绝不能把它当成空配置。
     * English summary: Runs one read operation inside the trusted deployment-bound persistence identity; guarded runtime
     * exceptions propagate unchanged so no stack is swallowed and no result is fabricated, while checked exceptions
     * collapse into a stable error code because a caller may only learn "the configuration read failed" from it and
     * must never mistake it for empty configuration.
     *
     * 用法 / Usage: 仅由本类的端口方法调用；/ Called only by the port methods of this class.
     * @param operation 参数 只读操作名，用于日志定位；parameter the read operation name used to locate the log line.
     * @param work 参数 需要在受守卫身份内执行的只读工作；parameter the read work that must run inside the guarded identity.
     * @param <T> 工作的返回类型 / the work's return type.
     * @return 返回 工作自身的结果；returns whatever the work itself produces.
     */
    private <T> T read(String operation, Callable<T> work) {
        try {
            return persistenceContextComponent.call(work);
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            log.error("llm configuration read failed, operation={}", operation, failure);
            throw new IllegalStateException("YUHENG_LLM_CONFIGURATION_READ_FAILED", failure);
        }
    }

    /**
     * 中文说明：把 {@code routes} jsonb 文本解析为有序类型化 route，字段严格为
     * channelKey/upstreamModel/priority/weight/capabilities；数量必须在 1..16 之内，未知协议或能力 wire 值由枚举的
     * {@code fromWire} 失败关闭。
     * English summary: Parses the {@code routes} jsonb text into ordered typed routes whose fields are exactly
     * channelKey/upstreamModel/priority/weight/capabilities, keeps the count inside 1..16, and fails closed on an
     * unknown protocol or capability wire value through the enums' {@code fromWire}.
     *
     * 用法 / Usage: 仅由本类的读取路径调用。/ Called only by the read paths of this class.
     * @param model 参数 已读到的活跃模型行；parameter the active model row that was read.
     * @return 返回 与持久层顺序一致的 route 列表；returns the routes in persisted order.
     */
    private List<LlmModelSnapshotBO.RouteBO> parseRoutes(LlmModelPO model) {
        JsonNode routes = readJson(model.getRoutes(), "routes", model.getModelKey());
        if (!routes.isArray() || routes.isEmpty() || routes.size() > MAX_ROUTE_COUNT) {
            throw invalid(model.getModelKey(), "routes must hold 1.." + MAX_ROUTE_COUNT + " entries");
        }
        List<LlmModelSnapshotBO.RouteBO> parsed = new ArrayList<>(routes.size());
        for (JsonNode route : routes) {
            Set<LlmCapabilityEnum> capabilities = new LinkedHashSet<>();
            JsonNode capabilityNode = route.get("capabilities");
            if (capabilityNode == null || !capabilityNode.isArray() || capabilityNode.isEmpty()
                    || capabilityNode.size() > LlmCapabilityEnum.values().length) {
                throw invalid(model.getModelKey(), "route capabilities must be a non-empty unique array");
            }
            for (JsonNode capability : capabilityNode) {
                capabilities.add(LlmCapabilityEnum.fromWire(text(capability)));
            }
            parsed.add(snapshotConverter.toRoute(
                    requiredText(route, "channelKey", 64, model.getModelKey()),
                    requiredText(route, "upstreamModel", 128, model.getModelKey()),
                    requiredInt(route, "priority", 0, 1_000, model.getModelKey()),
                    requiredInt(route, "weight", 1, 1_000, model.getModelKey()),
                    capabilities));
        }
        return parsed;
    }

    /**
     * 中文说明：按 route 声明的渠道 key 去重后发起<b>一次</b>有界 IN 查询；集合非空且不超过 64 才允许发出，
     * 否则直接失败关闭，避免空集合退化为无界读。返回按 key 索引的活跃渠道行，未命中的 key 交由调用方视为悬空引用。
     * English summary: Issues <b>one</b> bounded IN query over the deduplicated channel keys the routes declare, and only
     * when the collection is non-empty and no larger than 64, so an empty one can never degrade into an unbounded read.
     * The result indexes the active channel rows by key, and a key that misses stays a dangling reference for the caller.
     *
     * 用法 / Usage: 仅由 {@link #findSnapshot(String)} 调用。/ Called only by {@link #findSnapshot(String)}.
     * @param routes 参数 已解析的 route 列表；parameter the parsed routes.
     * @param modelKey 参数 出错的 alias，用于失败关闭定位；parameter the alias at fault, used to locate the failure.
     * @return 返回 按渠道 key 索引的活跃渠道行；returns the active channel rows indexed by channel key.
     */
    private Map<String, LlmChannelPO> loadChannels(List<LlmModelSnapshotBO.RouteBO> routes, String modelKey) {
        Set<String> channelKeys = new LinkedHashSet<>();
        for (LlmModelSnapshotBO.RouteBO route : routes) {
            channelKeys.add(route.getChannelKey());
        }
        if (channelKeys.isEmpty() || channelKeys.size() > MAX_CHANNEL_KEYS) {
            throw invalid(modelKey, "route channel keys must be non-empty and at most " + MAX_CHANNEL_KEYS);
        }
        List<LlmChannelPO> rows = llmChannelDAO.selectActiveByChannelKeys(channelKeys);
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<String, LlmChannelPO> byKey = new LinkedHashMap<>();
        for (LlmChannelPO row : rows) {
            byKey.put(row.getChannelKey(), row);
        }
        return byKey;
    }

    /**
     * 中文说明：把模型行、已解析 route 与可选的渠道行合成只读快照；渠道缺失或被软删时该 route 的渠道保持
     * {@code null}，本方法不补造任何渠道。
     * English summary: Composes the read-only snapshot from the model row, the parsed routes and the optional channel
     * rows, keeping a route channel {@code null} when the channel is missing or logically deleted rather than
     * manufacturing one.
     *
     * 用法 / Usage: 仅由本类的读取路径调用。/ Called only by the read paths of this class.
     * @param model 参数 已读到的活跃模型行；parameter the active model row that was read.
     * @param routes 参数 已解析的有序 route；parameter the parsed ordered routes.
     * @param channels 参数 按渠道 key 索引的活跃渠道行，目录读传空表；parameter the active channel rows indexed by key, empty for the catalog read.
     * @return 返回 合成后的只读快照；returns the composed read-only snapshot.
     */
    private LlmModelSnapshotBO toSnapshot(LlmModelPO model,
                                          List<LlmModelSnapshotBO.RouteBO> routes,
                                          Map<String, LlmChannelPO> channels) {
        required(model.getModelKey(), "model_key", model.getModelKey());
        required(model.getName(), "name", model.getModelKey());
        required(model.getKind(), "kind", model.getModelKey());
        required(model.getEnabled(), "enabled", model.getModelKey());
        required(model.getRevision(), "revision", model.getModelKey());
        List<LlmModelSnapshotBO.RouteBO> resolved = new ArrayList<>(routes.size());
        for (LlmModelSnapshotBO.RouteBO route : routes) {
            LlmChannelPO channel = channels.get(route.getChannelKey());
            resolved.add(snapshotConverter.withChannel(route, channel == null ? null : toChannel(channel)));
        }
        return snapshotConverter.toTarget(snapshotConverter.toProjection(
                model,
                parseProtocols(model),
                parseAllowedSubjects(model),
                resolved));
    }

    private static List<LlmProtocolEnum> parseProtocols(LlmModelPO model) {
        JsonNode protocolNode = readJson(model.getProtocols(), "protocols", model.getModelKey());
        if (!protocolNode.isArray() || protocolNode.isEmpty() || protocolNode.size() > LlmProtocolEnum.values().length) {
            throw invalid(model.getModelKey(), "protocols must be a non-empty array of at most four entries");
        }
        Set<LlmProtocolEnum> distinctProtocols = new LinkedHashSet<>();
        for (JsonNode protocol : protocolNode) {
            distinctProtocols.add(LlmProtocolEnum.fromWire(text(protocol)));
        }
        if (distinctProtocols.size() != protocolNode.size()) {
            throw invalid(model.getModelKey(), "protocols must not repeat");
        }
        return new ArrayList<>(distinctProtocols);
    }

    private static List<String> parseAllowedSubjects(LlmModelPO model) {
        List<String> subjects = new ArrayList<>();
        JsonNode subjectNode = readJson(model.getAllowedSubjects(), "allowed_subjects", model.getModelKey());
        if (!subjectNode.isArray() || subjectNode.size() > 100) {
            throw invalid(model.getModelKey(), "allowedSubjects must be an array of at most 100 entries");
        }
        for (JsonNode subject : subjectNode) {
            subjects.add(text(subject));
        }
        return subjects;
    }

    /**
     * 中文说明：把活跃渠道行映射为一次尝试需要的渠道事实；必填列缺失即失败关闭；仅 LOCAL 无认证渠道可令
     * {@code secret_ref} 为 SQL NULL。
     * English summary: Maps an active channel row onto the channel facts one attempt needs, failing closed on a missing
     * required column; only an unauthenticated LOCAL channel may carry SQL NULL for {@code secret_ref}.
     *
     * 用法 / Usage: 仅由 {@link #toSnapshot(LlmModelPO, List, Map)} 调用。/ Called only by
     * {@link #toSnapshot(LlmModelPO, List, Map)}.
     * @param channel 参数 已读到的活跃渠道行；parameter the active channel row that was read.
     * @return 返回 渠道业务投影；returns the business projection of the channel.
     */
    private LlmModelSnapshotBO.ChannelBO toChannel(LlmChannelPO channel) {
        String channelKey = required(channel.getChannelKey(), "channel_key", channel.getChannelKey());
        required(channel.getDeployment(), "deployment", channelKey);
        required(channel.getProtocol(), "protocol", channelKey);
        required(channel.getBaseUrl(), "base_url", channelKey);
        required(channel.getEnabled(), "enabled", channelKey);
        requiredTimeout(channel.getConnectTimeoutMs(), channelKey);
        requiredTimeout(channel.getHeaderTimeoutMs(), channelKey);
        requiredTimeout(channel.getIdleTimeoutMs(), channelKey);
        requiredTimeout(channel.getTotalTimeoutMs(), channelKey);
        requiredTimeout(channel.getMaxConcurrent(), channelKey);
        return snapshotConverter.toChannel(channel);
    }

    /**
     * 中文说明：读取一个 jsonb 列的编码文本并解析为树；文本缺失或不可解析都属于配置不成立，失败关闭且不外泄原文。
     * English summary: Reads the encoded text of one jsonb column and parses it into a tree; missing or unparsable text
     * is damaged configuration and fails closed without echoing the payload.
     *
     * 用法 / Usage: 仅由本类的解析方法调用。/ Called only by the parsing helpers of this class.
     * @param json 参数 列中的 JSON 文本；parameter the JSON text held by the column.
     * @param column 参数 列名，只用于日志定位；parameter the column name, used only to locate the log line.
     * @param modelKey 参数 出错的 alias；parameter the alias whose configuration is damaged.
     * @return 返回 解析后的 JSON 树；returns the parsed JSON tree.
     */
    private static JsonNode readJson(String json, String column, String modelKey) {
        if (StringUtils.isBlank(json)) {
            throw invalid(modelKey, column + " must not be blank");
        }
        try {
            return JSONB_MAPPER.readTree(json);
        } catch (Exception failure) {
            throw invalid(modelKey, column + " cannot be parsed: " + failure.getClass().getSimpleName());
        }
    }

    /**
     * 中文说明：读取一个必须存在的非空字符串字段并限制长度。
     * English summary: Reads a string field that must be present, non-blank and within the length ceiling.
     *
     * 用法 / Usage: 仅由本类的解析方法调用。/ Called only by the parsing helpers of this class.
     * @param node 参数 所在对象节点；parameter the enclosing object node.
     * @param field 参数 字段名；parameter the field name.
     * @param maxLength 参数 字段长度上限；parameter the field length ceiling.
     * @param modelKey 参数 出错的 alias；parameter the alias whose configuration is damaged.
     * @return 返回 字段文本；returns the field text.
     */
    private static String requiredText(JsonNode node, String field, int maxLength, String modelKey) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || StringUtils.isBlank(value.asText())
                || value.asText().length() > maxLength) {
            throw invalid(modelKey, "route field " + field + " is invalid");
        }
        return value.asText();
    }

    /**
     * 中文说明：读取一个必须存在的整数字段并限制在给定闭区间内。
     * English summary: Reads an integer field that must be present and inside the given closed range.
     *
     * 用法 / Usage: 仅由本类的解析方法调用。/ Called only by the parsing helpers of this class.
     * @param node 参数 所在对象节点；parameter the enclosing object node.
     * @param field 参数 字段名；parameter the field name.
     * @param minimum 参数 允许的最小值；parameter the permitted minimum.
     * @param maximum 参数 允许的最大值；parameter the permitted maximum.
     * @param modelKey 参数 出错的 alias；parameter the alias whose configuration is damaged.
     * @return 返回 字段整数值；returns the field value.
     */
    private static Integer requiredInt(JsonNode node, String field, int minimum, int maximum, String modelKey) {
        JsonNode value = node.get(field);
        if (value == null || !value.isInt() || value.asInt() < minimum || value.asInt() > maximum) {
            throw invalid(modelKey, "route field " + field + " is out of range");
        }
        return value.asInt();
    }

    /**
     * 中文说明：读取数组元素中的 wire 文本，空元素直接失败关闭；错误只报告“元素不可用”，不回显原文。
     * English summary: Reads the wire text of an array element, failing closed on a blank one; the error only reports that
     * the element is unusable and never echoes it back.
     *
     * 用法 / Usage: 仅由本类的解析方法调用。/ Called only by the parsing helpers of this class.
     * @param node 参数 数组元素节点；parameter the array element node.
     * @return 返回 元素文本；returns the element text.
     */
    private static String text(JsonNode node) {
        if (node == null || !node.isTextual() || StringUtils.isBlank(node.asText())) {
            throw new IllegalStateException("YUHENG_LLM_CONFIGURATION_INVALID");
        }
        return node.asText();
    }

    /**
     * 中文说明：校验列值必须存在，用于把 {@code null} 的必填业务列挡在 BO 构造之前。
     * English summary: Requires a column value to be present so that a {@code null} required business column never
     * reaches the carrier constructor.
     *
     * 用法 / Usage: 仅由本类的映射方法调用。/ Called only by the mapping helpers of this class.
     * @param value 参数 列值；parameter the column value.
     * @param column 参数 列名，用于日志；parameter the column name for logging.
     * @param subject 参数 出错的 alias 或渠道 key；parameter the alias or channel key at fault.
     * @param <V> 列值类型 / the column value type.
     * @return 返回 非空列值；returns the non-null column value.
     */
    private static <V> V required(V value, String column, String subject) {
        if (value == null || (value instanceof String candidate && StringUtils.isBlank(candidate))) {
            log.error("llm configuration required column is empty, column={} subject={}", column, subject);
            throw new IllegalStateException("YUHENG_LLM_CONFIGURATION_INVALID");
        }
        return value;
    }

    /**
     * 中文说明：校验超时与并发预算列必须是 1 到上界之间的正整数。
     * English summary: Requires a timeout or concurrency budget column to be a positive integer within the ceiling.
     *
     * 用法 / Usage: 仅由 {@link #toChannel(LlmChannelPO)} 调用。/ Called only by {@link #toChannel(LlmChannelPO)}.
     * @param value 参数 列值；parameter the column value.
     * @param channelKey 参数 出错的渠道 key；parameter the channel key at fault.
     * @return 返回 合法的正数列值；returns the validated positive value.
     */
    private static Integer requiredTimeout(Integer value, String channelKey) {
        if (value == null || value < 1 || value > MAX_TIMEOUT_MS) {
            log.error("llm configuration channel timeout is invalid, channelKey={}", channelKey);
            throw new IllegalStateException("YUHENG_LLM_CONFIGURATION_INVALID");
        }
        return value;
    }

    /**
     * 中文说明：产出统一失败关闭错误，消息只含 alias 与原因短语，不含 JSON 原文、地址或密钥引用。
     * English summary: Produces the uniform fail-closed error whose message carries only the alias and a short reason,
     * never the raw JSON, an address or a secret reference.
     *
     * 用法 / Usage: 仅由本类的校验路径调用。/ Called only by the validation paths of this class.
     * @param modelKey 参数 出错的 alias；parameter the alias at fault.
     * @param reason 参数 原因短语；parameter the short reason.
     * @return 返回 待抛出的异常；returns the exception to throw.
     */
    private static IllegalStateException invalid(String modelKey, String reason) {
        log.error("llm configuration is not usable, modelKey={} reason={}", modelKey, reason);
        return new IllegalStateException("YUHENG_LLM_CONFIGURATION_INVALID");
    }
}
