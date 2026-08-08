package top.egon.cola.component.ddc.model.registry;

import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;

import java.util.Comparator;

/**
 * 服务注册与发现使用的规范物理服务键。
 * / Canonical physical service key used for registration and discovery.
 *
 * @param bizCode 业务编码 / business code
 * @param env 运行环境 / runtime environment
 * @param appCode 应用编码 / application code
 * @param serviceKind 服务类型 / service kind
 * @param serviceName 服务名称 / service name
 * @param group 服务分组，空值默认为 {@code default} / service group, defaulting to {@code default}
 * @param version 服务版本，空值默认为 {@code 1.0.0} / service version, defaulting to {@code 1.0.0}
 * @param protocol 小写传输协议 / lower-case transport protocol
 */
public record DdcServiceKey(
        String bizCode,
        String env,
        String appCode,
        DdcServiceKind serviceKind,
        String serviceName,
        String group,
        String version,
        String protocol
) implements Comparable<DdcServiceKey> {

    /** 服务键的确定性排序规则。 / Deterministic ordering for service keys. */
    private static final Comparator<DdcServiceKey> ORDER = Comparator
            .comparing(DdcServiceKey::bizCode)
            .thenComparing(DdcServiceKey::env)
            .thenComparing(DdcServiceKey::appCode)
            .thenComparing(DdcServiceKey::serviceKind)
            .thenComparing(DdcServiceKey::protocol)
            .thenComparing(DdcServiceKey::serviceName)
            .thenComparing(DdcServiceKey::group)
            .thenComparing(DdcServiceKey::version);

    /**
     * 校验、补全并规范化服务键。
     * / Validates, defaults, and normalizes the service key.
     *
     * @throws IllegalArgumentException 必填值或 HTTP 协议无效时抛出
     * / if a required value or HTTP protocol is invalid
     */
    public DdcServiceKey {
        bizCode = require(bizCode, "bizCode");
        env = require(env, "env");
        appCode = require(appCode, "appCode");
        if (serviceKind == null) {
            throw new IllegalArgumentException("serviceKind is required");
        }
        serviceName = require(serviceName, "serviceName");
        group = defaulted(group, "default");
        version = defaulted(version, "1.0.0");
        protocol = require(protocol, "protocol").toLowerCase(java.util.Locale.ROOT);
        if (serviceKind == DdcServiceKind.HTTP_PROVIDER
                && !"http".equals(protocol)
                && !"https".equals(protocol)) {
            throw new IllegalArgumentException(
                    "HTTP_PROVIDER protocol must be http or https"
            );
        }
    }

    /**
     * 返回版本化、可逆的规范服务键文本。
     * / Returns the versioned and reversible canonical service-key text.
     *
     * @return 规范服务键文本 / canonical service-key text
     */
    public String canonicalValue() {
        return String.join(
                "\n",
                "ddc-service-key-v3",
                bizCode,
                env,
                appCode,
                serviceKind.name(),
                protocol,
                serviceName,
                group,
                version
        );
    }

    /**
     * 解析规范服务键文本。
     * / Parses canonical service-key text.
     *
     * @param canonicalValue 由 {@link #canonicalValue()} 生成的文本 / text produced by {@link #canonicalValue()}
     * @return 解析后的服务键 / parsed service key
     * @throws IllegalArgumentException 文本为空、结构无效或字段值无效时抛出
     * / if the text is null, malformed, or contains invalid field values
     */
    public static DdcServiceKey parse(String canonicalValue) {
        if (canonicalValue == null) {
            throw new IllegalArgumentException("canonical service key is required");
        }
        String[] parts = canonicalValue.split("\n", -1);
        if (parts.length != 9) {
            throw new IllegalArgumentException("invalid canonical service key");
        }
        return new DdcServiceKey(
                parts[1],
                parts[2],
                parts[3],
                DdcServiceKind.valueOf(parts[4]),
                parts[6],
                parts[7],
                parts[8],
                parts[5]
        );
    }

    /**
     * 计算规范服务键的稳定 SHA-256 标识。
     * / Computes the stable SHA-256 identifier of the canonical service key.
     *
     * @return 十六进制服务标识 / hexadecimal service identifier
     */
    public String serviceId() {
        return Digests.sha256Hex(canonicalValue());
    }

    /**
     * 使用旧版 namespace 参数构造服务键；namespace 会被忽略。
     * / Constructs a service key with the legacy namespace parameter, which is ignored.
     *
     * @param bizCode 业务编码 / business code
     * @param appCode 应用编码 / application code
     * @param env 运行环境 / runtime environment
     * @param namespace 已忽略的授权视图 / ignored authorization view
     * @param serviceKind 服务类型 / service kind
     * @param serviceName 服务名称 / service name
     * @param group 服务分组 / service group
     * @param version 服务版本 / service version
     * @param protocol 传输协议 / transport protocol
     * @throws IllegalArgumentException 服务键字段无效时抛出 / if a service-key field is invalid
     * @deprecated namespace 是授权视图，不属于物理服务身份。
     * / namespace is an authorization view and is not part of physical service identity.
     */
    @Deprecated(forRemoval = true)
    public DdcServiceKey(
            String bizCode,
            String appCode,
            String env,
            String namespace,
            DdcServiceKind serviceKind,
            String serviceName,
            String group,
            String version,
            String protocol) {
        this(bizCode, env, appCode, serviceKind, serviceName, group, version, protocol);
    }

    /**
     * 返回已移除的 namespace 兼容值。
     * / Returns the removed namespace compatibility value.
     *
     * @return 始终为空字符串 / always an empty string
     * @deprecated namespace 不再属于物理服务键。
     * / namespace is no longer part of the physical service key.
     */
    @Deprecated(forRemoval = true)
    public String namespace() {
        return "";
    }

    /**
     * 按服务键的规范字段顺序进行比较。
     * / Compares service keys by their canonical field order.
     *
     * @param other 待比较服务键 / service key to compare with
     * @return 负数、零或正数，分别表示小于、等于或大于
     * / negative, zero, or positive when less than, equal to, or greater than
     */
    @Override
    public int compareTo(DdcServiceKey other) {
        return ORDER.compare(this, other);
    }

    /**
     * 对空白值应用默认值，并校验最终值。
     * / Applies a default to a blank value and validates the resulting value.
     *
     * @param value 原始值 / original value
     * @param defaultValue 默认值 / default value
     * @return 原始有效值或默认值 / original valid value or the default value
     * @throws IllegalArgumentException 最终值包含不支持的控制字符时抛出
     * / if the resulting value contains an unsupported control character
     */
    private static String defaulted(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : require(value, "service key value");
    }

    /**
     * 校验服务键必填文本且禁止换行控制字符。
     * / Validates required service-key text and rejects line-break control characters.
     *
     * @param value 待校验值 / value to validate
     * @param fieldName 用于错误消息的字段名 / field name used in the error message
     * @return 原始有效值 / original valid value
     * @throws IllegalArgumentException 值为空白或包含不支持的控制字符时抛出
     * / if the value is blank or contains an unsupported control character
     */
    private static String require(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(fieldName + " contains an unsupported control character");
        }
        return value;
    }
}
