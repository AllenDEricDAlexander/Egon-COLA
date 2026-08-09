package top.egon.cola.component.ddc.model.registry;

import org.springframework.lang.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * 路由决策所依赖的 {@code gateway.*} 实例元数据类型化视图。 /
 * Typed view over the {@code gateway.*} instance metadata that routing decisions depend on.
 *
 * <p>这些键并非全部新增。提供者此前已经写入 {@code gateway.weight}、{@code gateway.zone}、
 * {@code gateway.tags} 等键，但约定以重复字符串字面量存在，写入校验和网关读取没有共享定义，
 * 对格式错误值的处理也不一致。本 record 与 {@link ServiceInstanceMetaCodec} 将该约定统一为类型化模型。 /
 * These keys are not new. Providers have been writing {@code gateway.weight},
 * {@code gateway.zone}, {@code gateway.tags} and friends for some time, but the convention
 * lived as duplicated string literals — written and validated in the RPC provider's metadata
 * merger, read again in the gateway's provider instance model, with no shared definition and
 * no agreement on what a malformed value means. This record and
 * {@link ServiceInstanceMetaCodec} make that convention a single typed thing.
 *
 * <p>注册表模型保持不变：没有向注册 record 添加字段，线格式仍为扁平 {@code Map<String, String>}。
 * 未写入这些键的提供者会解码为下述默认值并保持原有行为。 /
 * The registry model is untouched: no field is added to any registry record, and the wire
 * format is still a flat {@code Map<String, String>}. A provider that writes none of these keys
 * decodes to exactly the defaults below and behaves as it did before.
 *
 * <p>只有 {@link #warmupSeconds()}、{@link #healthState()} 和 {@link #lastHealthCheckAt()} 是新增键；
 * 其余字段均映射到已有键。 / Only {@link #warmupSeconds()}, {@link #healthState()} and {@link #lastHealthCheckAt()} are
 * genuinely new keys; every other field maps to a key that already existed.
 *
 * @param weight            相对负载均衡权重，范围 {@value #MIN_WEIGHT} 到 {@value #MAX_WEIGHT} / relative load-balancing weight from {@value #MIN_WEIGHT} through {@value #MAX_WEIGHT}
 * @param region            地理区域，未指定时为空 / geographic region, empty when unspecified
 * @param zone              可用区，未指定时为空 / availability zone, empty when unspecified
 * @param tags              用于金丝雀与灰度发布匹配的键值标签 / key/value labels for canary and gray-release matching
 * @param protocolVersion   具体线协议版本，例如 {@code HTTP/1.1}、{@code h2} 或 {@code grpc} / concrete wire-protocol version
 * @param definitionSetId   当前实例所提供接口定义集合的指纹 / fingerprint of the interface definition set served by this instance
 * @param artifactVersion   构建制品版本 / build artifact version
 * @param buildId           构建标识 / build identifier
 * @param managementPath    主动健康探测使用的 actuator 或管理基础路径 / actuator or management base path used by active health probes
 * @param warmupSeconds     有效权重从零线性增长的预热窗口秒数，避免新实例立即过载 / warm-up window in seconds over which effective weight scales linearly from zero
 * @param healthState       最近观测到的健康状态 / most recently observed health state
 * @param lastHealthCheckAt 最近一次探测时间，从未探测时为空 / time of the most recent probe, null when never probed
 */
public record ServiceInstanceMeta(
        int weight,
        String region,
        String zone,
        Map<String, String> tags,
        String protocolVersion,
        String definitionSetId,
        String artifactVersion,
        String buildId,
        String managementPath,
        int warmupSeconds,
        InstanceHealthState healthState,
        @Nullable Instant lastHealthCheckAt
) {

    /**
     * 默认相对负载均衡权重。 / Default relative load-balancing weight.
     */
    public static final int DEFAULT_WEIGHT = 100;
    /**
     * 允许的最小相对权重。 / Minimum permitted relative weight.
     */
    public static final int MIN_WEIGHT = 1;
    /**
     * 允许的最大相对权重。 / Maximum permitted relative weight.
     */
    public static final int MAX_WEIGHT = 10_000;
    /**
     * 允许的最大预热窗口秒数。 / Maximum permitted warm-up window in seconds.
     */
    public static final int MAX_WARMUP_SECONDS = 3600;
    /**
     * 与 RPC 提供者元数据合并器既有约束一致的最大标签数。 / Maximum tag count matching the RPC provider merger's existing bound.
     */
    public static final int MAX_TAGS = 32;

    /**
     * 所有字段均采用默认值的共享不可变实例。 / Shared immutable instance containing default values for every field.
     */
    private static final ServiceInstanceMeta DEFAULTS = new ServiceInstanceMeta(
            DEFAULT_WEIGHT, "", "", Map.of(), "", "", "", "", "", 0,
            InstanceHealthState.UNKNOWN, null);

    /**
     * 校验数值边界并归一化可选文本、标签与健康状态。 /
     * Validates numeric bounds and normalizes optional text, tags, and health state.
     *
     * @throws IllegalArgumentException 当权重、预热秒数或标签数超出允许范围时 / when weight, warm-up seconds, or tag count is out of range
     */
    public ServiceInstanceMeta {
        if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
            throw new IllegalArgumentException(
                    "weight must be between " + MIN_WEIGHT + " and " + MAX_WEIGHT + " but was " + weight);
        }
        if (warmupSeconds < 0 || warmupSeconds > MAX_WARMUP_SECONDS) {
            throw new IllegalArgumentException(
                    "warmupSeconds must be between 0 and " + MAX_WARMUP_SECONDS + " but was " + warmupSeconds);
        }
        region = normalize(region);
        zone = normalize(zone);
        protocolVersion = normalize(protocolVersion);
        definitionSetId = normalize(definitionSetId);
        artifactVersion = normalize(artifactVersion);
        buildId = normalize(buildId);
        managementPath = normalize(managementPath);
        tags = normalizeTags(tags);
        healthState = healthState == null ? InstanceHealthState.UNKNOWN : healthState;
    }

    /**
     * 返回权重 100、无位置且健康状态为 {@code UNKNOWN} 的全默认实例。 /
     * Returns the all-defaults instance: weight 100, no placement, and {@code UNKNOWN} health.
     *
     * @return 共享的全默认元数据 / shared all-default metadata
     */
    public static ServiceInstanceMeta defaults() {
        return DEFAULTS;
    }

    /**
     * 结合健康状态与预热进度计算实例当前应使用的权重。 /
     * Calculates the weight to use for this instance now, folding in health and warm-up.
     *
     * <p>必须排除的实例返回 0，调用方可一次遍历汇总有效权重并识别“无实例可路由”。 /
     * Returns 0 for instances that must not be selected, so a caller can sum effective
     * weights and detect "nothing is routable" in a single pass.
     *
     * @param now          当前时间 / current time
     * @param registeredAt 实例加入时间；为空时禁用预热缩放 / time the instance joined; null disables warm-up scaling
     * @return 应用于路由选择的当前有效权重 / current effective weight for routing selection
     */
    public int effectiveWeight(@Nullable Instant now,
                               @Nullable Instant registeredAt) {
        int base = weight * healthState.weightPercent() / 100;
        if (base <= 0) {
            return 0;
        }
        int warmed = applyWarmup(base, now, registeredAt);
        // A selectable instance never drops to zero, otherwise it could never warm up at all.
        return Math.max(warmed, 1);
    }

    /**
     * 按实例注册后的经过时间缩放基础权重。 / Scales a base weight by elapsed time since instance registration.
     *
     * @param base         健康状态调整后的基础权重 / base weight after health adjustment
     * @param now          当前时间 / current time
     * @param registeredAt 实例注册时间 / instance registration time
     * @return 预热进度调整后的权重 / weight adjusted for warm-up progress
     */
    private int applyWarmup(int base,
                            @Nullable Instant now,
                            @Nullable Instant registeredAt) {
        if (warmupSeconds <= 0 || registeredAt == null || now == null) {
            return base;
        }
        long elapsed = Duration.between(registeredAt, now).getSeconds();
        if (elapsed >= warmupSeconds) {
            return base;
        }
        if (elapsed <= 0) {
            return 0;
        }
        return (int) (base * elapsed / warmupSeconds);
    }

    /**
     * 判断实例是否位于指定可用区；空可用区永不匹配。 /
     * Determines whether this instance is in the given zone; blank zones never match.
     *
     * @param candidateZone 待匹配的可用区 / candidate availability zone
     * @return 忽略大小写匹配且实例可用区非空时返回 {@code true} / {@code true} for a case-insensitive match with a nonblank instance zone
     */
    public boolean inZone(String candidateZone) {
        return !zone.isEmpty() && zone.equalsIgnoreCase(candidateZone);
    }

    /**
     * 判断实例是否携带指定 {@code tag=value}，用于金丝雀匹配。 /
     * Determines whether this instance carries {@code tag=value}, for canary matching.
     *
     * @param tag   标签键 / tag key
     * @param value 标签值 / tag value
     * @return 标签键值匹配时返回 {@code true} / {@code true} when the tag key and value match
     */
    public boolean hasTag(@Nullable String tag,
                          @Nullable String value) {
        if (tag == null || value == null) {
            return false;
        }
        return value.equals(tags.get(tag.toLowerCase(Locale.ROOT)));
    }

    /**
     * 复制当前元数据并替换健康状态与探测时间。 /
     * Copies this metadata with a replacement health state and probe time.
     *
     * @param state     新健康状态，空值归一化为 {@link InstanceHealthState#UNKNOWN} / new health state, normalized to {@link InstanceHealthState#UNKNOWN} when null
     * @param checkedAt 健康探测时间 / health-probe time
     * @return 更新后的不可变元数据 / updated immutable metadata
     */
    public ServiceInstanceMeta withHealthState(
            @Nullable InstanceHealthState state,
            @Nullable Instant checkedAt) {
        return new ServiceInstanceMeta(weight, region, zone, tags, protocolVersion, definitionSetId,
                artifactVersion, buildId, managementPath, warmupSeconds, state, checkedAt);
    }

    /**
     * 复制当前元数据并替换配置权重。 / Copies this metadata with a replacement configured weight.
     *
     * @param newWeight 新相对权重 / new relative weight
     * @return 更新后的不可变元数据 / updated immutable metadata
     * @throws IllegalArgumentException 当新权重超出允许范围时 / when the new weight is out of range
     */
    public ServiceInstanceMeta withWeight(int newWeight) {
        return new ServiceInstanceMeta(newWeight, region, zone, tags, protocolVersion, definitionSetId,
                artifactVersion, buildId, managementPath, warmupSeconds, healthState, lastHealthCheckAt);
    }

    /**
     * 将可选文本归一化为已去除首尾空白的非空引用。 /
     * Normalizes optional text to a nonnull, trimmed reference.
     *
     * @param value 待归一化文本 / text to normalize
     * @return 已去除首尾空白的文本，空值返回空字符串 / trimmed text, or an empty string for null
     */
    private static String normalize(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 清理、规范化并按键排序标签，同时返回不可变映射。 /
     * Cleans, normalizes, and key-sorts tags, returning an immutable map.
     *
     * @param tags 原始标签映射 / raw tag map
     * @return 规范化的不可变标签映射 / normalized immutable tag map
     * @throws IllegalArgumentException 当有效标签数量超过上限时 / when the number of valid tags exceeds the limit
     */
    private static Map<String, String> normalizeTags(
            @Nullable Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Map.of();
        }
        // Sorted: the wire form must be deterministic, and the provider-side validator
        // rejects tag strings whose entries are not in ascending order.
        TreeMap<String, String> copy = new TreeMap<>();
        tags.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null) {
                return;
            }
            copy.put(key.trim().toLowerCase(Locale.ROOT), value.trim());
        });
        if (copy.size() > MAX_TAGS) {
            throw new IllegalArgumentException("tags must contain at most " + MAX_TAGS + " entries");
        }
        return Collections.unmodifiableMap(copy);
    }
}
