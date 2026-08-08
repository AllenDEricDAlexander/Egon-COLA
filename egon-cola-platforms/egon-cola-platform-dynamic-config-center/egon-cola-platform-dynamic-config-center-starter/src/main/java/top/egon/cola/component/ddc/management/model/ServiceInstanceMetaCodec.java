package top.egon.cola.component.ddc.management.model;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@code gateway.*} 实例元数据约定的规范定义，以及该约定与 {@link ServiceInstanceMeta} 之间的双向投影。 /
 * Canonical definition of the {@code gateway.*} instance-metadata convention, and the
 * bidirectional projection between it and {@link ServiceInstanceMeta}.
 *
 * <p>在本类出现前，该约定只以重复字符串字面量存在：RPC 提供者在写出时校验，网关实例模型在读取时再次
 * 解析，双方没有共享常量，对错误输入也有不同处理。本类统一了两端行为。 /
 * Before this class the convention existed only as duplicated string literals: the RPC
 * provider metadata merger validated {@code gateway.weight} on the way out, and the gateway's
 * provider instance model parsed it again on the way in, with no shared constant and two
 * different opinions about malformed input. Both sides now delegate here.
 *
 * <p><strong>编码会省略默认值。</strong>等于默认值的字段不产生条目，因此全默认实例与引入本类型之前
 * 注册的实例在线格式上完全一致，也可将元数据容量留给业务键。 /
 * <strong>Encoding omits defaults.</strong> A field equal to its default contributes no
 * entry, so an all-default instance registers byte-identically to one registered before this
 * type existed, and the metadata budget stays free for business keys.
 *
 * <p><strong>解码不会抛出异常。</strong>解码位于路由热路径，输入可能来自不同版本的进程；错误值逐字段
 * 回退到默认值，避免异常导致服务退出发现。写入由 {@link ServiceInstanceMeta} 构造器与
 * {@link #validate} 严格校验，读取则保持容错，这种不对称是有意设计。 /
 * <strong>Decoding never throws.</strong> Decode runs on the routing hot path against data
 * written by another process, possibly an older or newer one. A malformed value falls back to
 * its default rather than propagating an exception that would drop a service out of discovery.
 * Writes are validated ({@link ServiceInstanceMeta}'s constructor and {@link #validate}); reads
 * are tolerant. That asymmetry is deliberate.
 */
public final class ServiceInstanceMetaCodec {

    /** 结构化实例元数据的保留键前缀。 / Reserved key prefix for structured instance metadata. */
    public static final String PREFIX = "gateway.";

    /** 相对权重元数据键。 / Metadata key for relative weight. */
    public static final String KEY_WEIGHT = PREFIX + "weight";
    /** 地理区域元数据键。 / Metadata key for geographic region. */
    public static final String KEY_REGION = PREFIX + "region";
    /** 可用区元数据键。 / Metadata key for availability zone. */
    public static final String KEY_ZONE = PREFIX + "zone";
    /** 规范标签集合元数据键。 / Metadata key for the canonical tag set. */
    public static final String KEY_TAGS = PREFIX + "tags";
    /** 线协议版本元数据键。 / Metadata key for wire-protocol version. */
    public static final String KEY_PROTOCOL_VERSION = PREFIX + "protocol-version";
    /** 接口定义集合指纹元数据键。 / Metadata key for the interface-definition-set fingerprint. */
    public static final String KEY_DEFINITION_SET_ID = PREFIX + "definition-set-id";
    /** 构建制品版本元数据键。 / Metadata key for build artifact version. */
    public static final String KEY_ARTIFACT_VERSION = PREFIX + "artifact-version";
    /** 构建标识元数据键。 / Metadata key for the build identifier. */
    public static final String KEY_BUILD_ID = PREFIX + "build-id";
    /** 健康探测管理路径元数据键。 / Metadata key for the health-probe management path. */
    public static final String KEY_MANAGEMENT_PATH = PREFIX + "management-path";

    /** 预热渐增窗口秒数元数据键。 / Metadata key for the warm-up ramp window in seconds. */
    public static final String KEY_WARMUP_SECONDS = PREFIX + "warmup-seconds";
    /** 最近观测健康状态元数据键。 / Metadata key for the last observed health state. */
    public static final String KEY_HEALTH_STATE = PREFIX + "health-state";
    /** 最近一次健康探测时间元数据键。 / Metadata key for the timestamp of the last health probe. */
    public static final String KEY_HEALTH_CHECKED_AT = PREFIX + "health-checked-at";

    /** 单个实例可携带的已知保留键数量。 / Number of known reserved keys a single instance can carry. */
    public static final int RESERVED_KEY_COUNT = 12;

    /** 区域和可用区短位置标识的校验模式。 / Validation pattern for short region and zone identifiers. */
    public static final Pattern LOCATION = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    /** 版本及标识类元数据值的校验模式。 / Validation pattern for version and identifier metadata values. */
    public static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:+-]{0,127}");
    /** 标签键的校验模式。 / Validation pattern for tag keys. */
    public static final Pattern TAG_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,31}");
    /** 标签值的校验模式。 / Validation pattern for tag values. */
    public static final Pattern TAG_VALUE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:/+-]{0,63}");
    /** 管理基础路径的校验模式。 / Validation pattern for management base paths. */
    public static final Pattern MANAGEMENT_PATH = Pattern.compile("/[A-Za-z0-9/_.{}-]{0,255}");

    /** 编码后标签字符串的最大长度。 / Maximum length of an encoded tag string. */
    private static final int MAX_TAGS_LENGTH = 256;
    /** 线格式中的标签条目分隔符。 / Tag-entry separator in the wire format. */
    private static final String TAG_SEPARATOR = ",";
    /** 线格式中的标签键值分隔符。 / Tag key/value separator in the wire format. */
    private static final String TAG_ASSIGNMENT = "=";

    /**
     * 禁止实例化纯静态编解码工具。 / Prevents instantiation of this static codec utility.
     */
    private ServiceInstanceMetaCodec() {
    }

    /**
     * 判断元数据键是否属于保留的结构化命名空间。 /
     * Determines whether a metadata key belongs to the reserved structured namespace.
     *
     * @param key 元数据键 / metadata key
     * @return 键以保留前缀开头时返回 {@code true} / {@code true} when the key starts with the reserved prefix
     */
    public static boolean isReservedKey(String key) {
        return key != null && key.toLowerCase(Locale.ROOT).startsWith(PREFIX);
    }

    /**
     * 将 {@code meta} 的非默认字段渲染为元数据条目。 /
     * Renders the non-default fields of {@code meta} as metadata entries.
     *
     * @param meta 待编码的类型化实例元数据，空值视为全默认值 / typed instance metadata to encode, with null treated as all defaults
     * @return 仅含保留键的可变映射；全默认值时为空 / mutable map containing only reserved keys; empty for all defaults
     */
    public static Map<String, String> encode(ServiceInstanceMeta meta) {
        Map<String, String> encoded = new LinkedHashMap<>();
        if (meta == null) {
            return encoded;
        }
        ServiceInstanceMeta defaults = ServiceInstanceMeta.defaults();
        if (meta.weight() != defaults.weight()) {
            encoded.put(KEY_WEIGHT, Integer.toString(meta.weight()));
        }
        putIfPresent(encoded, KEY_REGION, meta.region());
        putIfPresent(encoded, KEY_ZONE, meta.zone());
        if (!meta.tags().isEmpty()) {
            encoded.put(KEY_TAGS, encodeTags(meta.tags()));
        }
        putIfPresent(encoded, KEY_PROTOCOL_VERSION, meta.protocolVersion());
        putIfPresent(encoded, KEY_DEFINITION_SET_ID, meta.definitionSetId());
        putIfPresent(encoded, KEY_ARTIFACT_VERSION, meta.artifactVersion());
        putIfPresent(encoded, KEY_BUILD_ID, meta.buildId());
        putIfPresent(encoded, KEY_MANAGEMENT_PATH, meta.managementPath());
        if (meta.warmupSeconds() != defaults.warmupSeconds()) {
            encoded.put(KEY_WARMUP_SECONDS, Integer.toString(meta.warmupSeconds()));
        }
        if (meta.healthState() != defaults.healthState()) {
            encoded.put(KEY_HEALTH_STATE, meta.healthState().name());
        }
        if (meta.lastHealthCheckAt() != null) {
            encoded.put(KEY_HEALTH_CHECKED_AT, meta.lastHealthCheckAt().toString());
        }
        return encoded;
    }

    /**
     * 从 {@code metadata} 读取保留键，并逐字段回退到默认值。 /
     * Reads reserved keys from {@code metadata}, falling back to defaults field by field.
     *
     * <p>无法识别或格式错误的值会被忽略而非拒绝，详见类型说明。 /
     * Unrecognized or malformed values are ignored rather than rejected. See the class note.
     *
     * @param metadata 待解码的扁平实例元数据 / flat instance metadata to decode
     * @return 类型化实例元数据；空映射或空值返回全默认实例 / typed instance metadata; all defaults for an empty map or null
     */
    public static ServiceInstanceMeta decode(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return ServiceInstanceMeta.defaults();
        }
        ServiceInstanceMeta defaults = ServiceInstanceMeta.defaults();
        int weight = clamp(readInt(metadata, KEY_WEIGHT, defaults.weight()),
                ServiceInstanceMeta.MIN_WEIGHT, ServiceInstanceMeta.MAX_WEIGHT, defaults.weight());
        int warmupSeconds = clamp(readInt(metadata, KEY_WARMUP_SECONDS, defaults.warmupSeconds()),
                0, ServiceInstanceMeta.MAX_WARMUP_SECONDS, defaults.warmupSeconds());
        return new ServiceInstanceMeta(
                weight,
                readMatching(metadata, KEY_REGION, LOCATION),
                readMatching(metadata, KEY_ZONE, LOCATION),
                decodeTags(metadata.get(KEY_TAGS)),
                readMatching(metadata, KEY_PROTOCOL_VERSION, IDENTIFIER),
                readMatching(metadata, KEY_DEFINITION_SET_ID, IDENTIFIER),
                readMatching(metadata, KEY_ARTIFACT_VERSION, IDENTIFIER),
                readMatching(metadata, KEY_BUILD_ID, IDENTIFIER),
                readMatching(metadata, KEY_MANAGEMENT_PATH, MANAGEMENT_PATH),
                warmupSeconds,
                InstanceHealthState.fromWire(rawString(metadata, KEY_HEALTH_STATE)),
                readInstant(metadata, KEY_HEALTH_CHECKED_AT));
    }

    /**
     * 将 {@code meta} 的编码形式合并到 {@code businessMetadata}。 /
     * Merges the encoded form of {@code meta} into {@code businessMetadata}.
     *
     * <p>先删除已有保留键，因此重新注册会整体替换结构化元数据，不会在字段恢复默认值时留下旧条目。 /
     * Reserved keys already present are dropped first, so a re-registration replaces
     * structured metadata wholesale instead of leaving a stale field behind when a value
     * reverts to its default.
     *
     * @param businessMetadata 调用方业务元数据 / caller-owned business metadata
     * @param meta 待编码并合并的结构化元数据 / structured metadata to encode and merge
     * @return 新映射，两个入参均不会被修改 / new map; neither argument is modified
     */
    public static Map<String, String> merge(Map<String, String> businessMetadata, ServiceInstanceMeta meta) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (businessMetadata != null) {
            businessMetadata.forEach((key, value) -> {
                if (!isReservedKey(key)) {
                    merged.put(key, value);
                }
            });
        }
        merged.putAll(encode(meta));
        return merged;
    }

    /**
     * 删除结构化条目，仅返回调用方自己的业务元数据。 /
     * Returns only caller-owned business metadata with structured entries removed.
     *
     * @param metadata 包含业务键与保留键的元数据 / metadata containing business and reserved keys
     * @return 保持迭代顺序的业务元数据映射 / business metadata map preserving iteration order
     */
    public static Map<String, String> businessMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return metadata.entrySet().stream()
                .filter(entry -> !isReservedKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (left, right) -> right, LinkedHashMap::new));
    }

    /**
     * 严格校验一个保留元数据条目，供写路径使用。 /
     * Strictly validates one reserved metadata entry for use on the write path.
     *
     * <p>未知 {@code gateway.*} 键会被接受，以兼容较新提供者上报当前版本尚不认识的键。 /
     * Unknown {@code gateway.*} keys are accepted: this convention must tolerate a newer
     * provider reporting a key this version does not know about.
     *
     * @param key 待校验的元数据键 / metadata key to validate
     * @param value 待校验的元数据值 / metadata value to validate
     * @throws IllegalArgumentException 当已知保留键的值无效时 / when the value is invalid for a known reserved key
     */
    public static void validate(String key, String value) {
        if (key == null) {
            return;
        }
        switch (key.toLowerCase(Locale.ROOT)) {
            case KEY_WEIGHT -> requireIntInRange(key, value,
                    ServiceInstanceMeta.MIN_WEIGHT, ServiceInstanceMeta.MAX_WEIGHT);
            case KEY_WARMUP_SECONDS -> requireIntInRange(key, value,
                    0, ServiceInstanceMeta.MAX_WARMUP_SECONDS);
            case KEY_TAGS -> requireValidTags(value);
            case KEY_ZONE, KEY_REGION -> requirePattern(key, value, LOCATION);
            case KEY_PROTOCOL_VERSION, KEY_DEFINITION_SET_ID, KEY_ARTIFACT_VERSION, KEY_BUILD_ID ->
                    requirePattern(key, value, IDENTIFIER);
            case KEY_MANAGEMENT_PATH -> requirePattern(key, value, MANAGEMENT_PATH);
            case KEY_HEALTH_STATE -> requireHealthState(value);
            case KEY_HEALTH_CHECKED_AT -> requireInstant(value);
            default -> {
                // Forward compatibility: a newer provider may report keys this build predates.
            }
        }
    }

    /**
     * 对 {@code metadata} 中每个保留条目应用 {@link #validate}。 /
     * Applies {@link #validate} to every reserved entry in {@code metadata}.
     *
     * @param metadata 待校验的实例元数据 / instance metadata to validate
     * @throws IllegalArgumentException 当任一已知保留键的值无效时 / when any known reserved value is invalid
     */
    public static void validateAll(Map<String, String> metadata) {
        if (metadata == null) {
            return;
        }
        metadata.forEach((key, value) -> {
            if (isReservedKey(key)) {
                validate(key, value);
            }
        });
    }

    /**
     * 将标签编码为按键排序的 {@code k=v,k=v} 线格式。 /
     * Encodes tags to the key-sorted {@code k=v,k=v} wire form.
     *
     * @param tags 待编码的标签 / tags to encode
     * @return 规范标签字符串；空映射或空值返回空字符串 / canonical tag string; empty for an empty map or null
     */
    public static String encodeTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return new TreeMap<>(tags).entrySet().stream()
                .map(entry -> entry.getKey() + TAG_ASSIGNMENT + entry.getValue())
                .collect(Collectors.joining(TAG_SEPARATOR));
    }

    /**
     * 解码 {@code k=v,k=v} 线格式并跳过格式错误的条目。 /
     * Decodes the {@code k=v,k=v} wire form while skipping malformed entries.
     *
     * @param value 待解码的标签字符串 / tag string to decode
     * @return 按键排序的不可变标签映射 / immutable tag map sorted by key
     */
    public static Map<String, String> decodeTags(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        TreeMap<String, String> tags = new TreeMap<>();
        for (String entry : value.split(TAG_SEPARATOR, -1)) {
            int split = entry.indexOf(TAG_ASSIGNMENT);
            if (split <= 0 || split == entry.length() - 1) {
                continue;
            }
            String tagKey = entry.substring(0, split).trim().toLowerCase(Locale.ROOT);
            String tagValue = entry.substring(split + 1).trim();
            if (tagKey.isEmpty() || tagValue.isEmpty() || tags.size() >= ServiceInstanceMeta.MAX_TAGS) {
                continue;
            }
            tags.put(tagKey, tagValue);
        }
        return Map.copyOf(tags);
    }

    /**
     * 值非空时将条目写入目标映射。 / Adds an entry to the target map when its value is nonempty.
     *
     * @param target 目标映射 / target map
     * @param key 元数据键 / metadata key
     * @param value 元数据值 / metadata value
     */
    private static void putIfPresent(Map<String, String> target, String key, String value) {
        if (value != null && !value.isEmpty()) {
            target.put(key, value);
        }
    }

    /**
     * 读取并去除元数据值的首尾空白，缺失时返回空字符串。 /
     * Reads and trims a metadata value, returning an empty string when absent.
     *
     * @param metadata 元数据映射 / metadata map
     * @param key 待读取的键 / key to read
     * @return 已去除首尾空白的值或空字符串 / trimmed value or an empty string
     */
    private static String rawString(Map<String, String> metadata, String key) {
        String value = metadata.get(key);
        return value == null ? "" : value.trim();
    }

    /**
     * 读取字符串键并丢弃不符合约定模式的值。 /
     * Reads a string key and discards values that do not match the convention's pattern.
     *
     * @param metadata 元数据映射 / metadata map
     * @param key 待读取的键 / key to read
     * @param pattern 允许值的模式 / allowed-value pattern
     * @return 匹配的已去除首尾空白值，否则为空字符串 / matching trimmed value, or an empty string
     */
    private static String readMatching(Map<String, String> metadata, String key, Pattern pattern) {
        String value = rawString(metadata, key);
        if (value.isEmpty() || !pattern.matcher(value).matches()) {
            return "";
        }
        return value;
    }

    /**
     * 容错读取十进制整数，缺失或格式错误时使用回退值。 /
     * Tolerantly reads a decimal integer, using a fallback when absent or malformed.
     *
     * @param metadata 元数据映射 / metadata map
     * @param key 待读取的键 / key to read
     * @param fallback 回退值 / fallback value
     * @return 解析后的整数或回退值 / parsed integer or fallback value
     */
    private static int readInt(Map<String, String> metadata, String key, int fallback) {
        String value = metadata.get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /**
     * 仅保留闭区间内的值，越界时返回回退值。 /
     * Retains a value only within an inclusive range, otherwise returning a fallback.
     *
     * @param value 待检查值 / value to inspect
     * @param min 最小允许值 / minimum permitted value
     * @param max 最大允许值 / maximum permitted value
     * @param fallback 越界时的回退值 / fallback for an out-of-range value
     * @return 区间内的原值或回退值 / original in-range value or fallback
     */
    private static int clamp(int value, int min, int max, int fallback) {
        return value < min || value > max ? fallback : value;
    }

    /**
     * 容错读取 ISO-8601 时间，缺失或格式错误时返回空值。 /
     * Tolerantly reads an ISO-8601 instant, returning null when absent or malformed.
     *
     * @param metadata 元数据映射 / metadata map
     * @param key 待读取的键 / key to read
     * @return 解析后的时间，无法解析时为空 / parsed instant, or null when unavailable
     */
    private static Instant readInstant(Map<String, String> metadata, String key) {
        String value = metadata.get(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /**
     * 要求值为指定闭区间内的非负十进制整数。 /
     * Requires a value to be a nonnegative decimal integer in an inclusive range.
     *
     * @param key 用于错误消息的元数据键 / metadata key used in error messages
     * @param value 待校验值 / value to validate
     * @param min 最小允许值 / minimum permitted value
     * @param max 最大允许值 / maximum permitted value
     * @throws IllegalArgumentException 当值不是有效整数或超出范围时 / when the value is not a valid integer or is out of range
     */
    private static void requireIntInRange(String key, String value, int min, int max) {
        if (value == null || !value.matches("[0-9]+")) {
            throw invalid(key);
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                throw invalid(key);
            }
        } catch (NumberFormatException ex) {
            throw invalid(key);
        }
    }

    /**
     * 要求标签字符串满足数量、长度、格式、唯一性与规范排序约束。 /
     * Requires a tag string to satisfy count, length, format, uniqueness, and canonical-order constraints.
     *
     * @param value 待校验标签字符串 / tag string to validate
     * @throws IllegalArgumentException 当标签字符串不规范时 / when the tag string is not canonical
     */
    private static void requireValidTags(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_TAGS_LENGTH) {
            throw invalid(KEY_TAGS);
        }
        String[] entries = value.split(TAG_SEPARATOR, -1);
        if (entries.length > ServiceInstanceMeta.MAX_TAGS) {
            throw invalid(KEY_TAGS);
        }
        TreeMap<String, String> seen = new TreeMap<>();
        for (String entry : entries) {
            String[] pair = entry.split(TAG_ASSIGNMENT, -1);
            if (pair.length != 2
                    || !TAG_KEY.matcher(pair[0]).matches()
                    || !TAG_VALUE.matcher(pair[1]).matches()
                    || seen.put(pair[0], pair[1]) != null) {
                throw invalid(KEY_TAGS);
            }
        }
        // The wire form must be canonical so two equivalent tag sets hash identically.
        if (!encodeTags(seen).equals(value)) {
            throw invalid(KEY_TAGS);
        }
    }

    /**
     * 要求值为已识别的健康状态线格式。 / Requires a recognized health-state wire value.
     *
     * @param value 待校验健康状态 / health-state value to validate
     * @throws IllegalArgumentException 当状态无法识别时 / when the state is unrecognized
     */
    private static void requireHealthState(String value) {
        if (!InstanceHealthState.isKnownWireValue(value)) {
            throw invalid(KEY_HEALTH_STATE);
        }
    }

    /**
     * 要求值为有效的 ISO-8601 时间。 / Requires a valid ISO-8601 instant.
     *
     * @param value 待校验时间文本 / instant text to validate
     * @throws IllegalArgumentException 当值为空或无法解析时 / when the value is null or cannot be parsed
     */
    private static void requireInstant(String value) {
        if (value == null) {
            throw invalid(KEY_HEALTH_CHECKED_AT);
        }
        try {
            Instant.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw invalid(KEY_HEALTH_CHECKED_AT);
        }
    }

    /**
     * 要求值匹配指定模式。 / Requires a value to match a specified pattern.
     *
     * @param key 用于错误消息的元数据键 / metadata key used in error messages
     * @param value 待校验值 / value to validate
     * @param pattern 允许值的模式 / allowed-value pattern
     * @throws IllegalArgumentException 当值为空或不匹配时 / when the value is null or does not match
     */
    private static void requirePattern(String key, String value, Pattern pattern) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw invalid(key);
        }
    }

    /**
     * 创建包含无效元数据键的统一参数异常。 / Creates a uniform argument exception naming the invalid metadata key.
     *
     * @param key 无效值对应的元数据键 / metadata key whose value is invalid
     * @return 新建的参数异常 / newly created argument exception
     */
    private static IllegalArgumentException invalid(String key) {
        return new IllegalArgumentException("instance metadata value is invalid for " + key);
    }
}
