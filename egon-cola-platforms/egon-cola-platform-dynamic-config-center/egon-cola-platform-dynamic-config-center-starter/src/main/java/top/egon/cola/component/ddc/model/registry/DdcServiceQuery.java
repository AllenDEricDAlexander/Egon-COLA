package top.egon.cola.component.ddc.model.registry;

import top.egon.cola.component.ddc.model.registry.DdcServiceKind;

/**
 * 服务目录查询条件；空字段表示该维度不筛选。
 * / Service catalog query; a null field means no filtering on that dimension.
 *
 * @param bizCode     业务编码 / business code
 * @param env         运行环境 / runtime environment
 * @param appCode     应用编码 / application code
 * @param serviceKind 服务类型 / service kind
 * @param protocol    传输协议 / transport protocol
 * @param serviceName 服务名称 / service name
 * @param group       服务分组 / service group
 * @param version     服务版本 / service version
 */
public record DdcServiceQuery(
        String bizCode,
        String env,
        String appCode,
        DdcServiceKind serviceKind,
        String protocol,
        String serviceName,
        String group,
        String version
) {

    /**
     * 去除查询文本首尾空白，将空白值转为空值，并将协议转为小写。
     * / Trims query text, converts blank values to null, and lower-cases the protocol.
     */
    public DdcServiceQuery {
        bizCode = normalized(bizCode);
        env = normalized(env);
        appCode = normalized(appCode);
        protocol = normalized(protocol);
        protocol = protocol == null ? null : protocol.toLowerCase(java.util.Locale.ROOT);
        serviceName = normalized(serviceName);
        group = normalized(group);
        version = normalized(version);
    }

    /**
     * 判断服务键是否满足全部非空查询条件。
     * / Determines whether a service key satisfies every non-null query criterion.
     *
     * @param key 待匹配服务键 / service key to test
     * @return 匹配时为 {@code true} / {@code true} when the key matches
     * @throws NullPointerException 服务键为空时抛出 / if the service key is {@code null}
     */
    public boolean matches(DdcServiceKey key) {
        return matches(bizCode, key.bizCode())
                && matches(env, key.env())
                && matches(appCode, key.appCode())
                && (serviceKind == null || serviceKind == key.serviceKind())
                && matches(protocol, key.protocol())
                && matches(serviceName, key.serviceName())
                && matches(group, key.group())
                && matches(version, key.version());
    }

    /**
     * 判断查询是否包含订阅服务目录所需的精确主题范围。
     * / Indicates whether the query contains the exact topic scope required for catalog subscription.
     *
     * @return 业务、环境、应用、服务类型和协议均存在时为 {@code true}
     * / {@code true} when business, environment, application, service kind, and protocol are present
     */
    public boolean hasExactCatalogScope() {
        return bizCode != null
                && env != null
                && appCode != null
                && serviceKind != null
                && protocol != null;
    }

    /**
     * 使用旧版 namespace 参数构造查询；namespace 会被忽略。
     * / Constructs a query with the legacy namespace parameter, which is ignored.
     *
     * @param bizCode     业务编码 / business code
     * @param appCode     应用编码 / application code
     * @param env         运行环境 / runtime environment
     * @param namespace   已忽略的授权视图 / ignored authorization view
     * @param serviceKind 服务类型 / service kind
     * @param protocol    传输协议 / transport protocol
     * @param serviceName 服务名称 / service name
     * @param group       服务分组 / service group
     * @param version     服务版本 / service version
     * @deprecated namespace 是授权视图，不是注册中心筛选条件。
     * / namespace is an authorization view and not a registry filter.
     */
    @Deprecated(forRemoval = true)
    public DdcServiceQuery(
            String bizCode,
            String appCode,
            String env,
            String namespace,
            DdcServiceKind serviceKind,
            String protocol,
            String serviceName,
            String group,
            String version) {
        this(bizCode, env, appCode, serviceKind, protocol, serviceName, group, version);
    }

    /**
     * 返回已移除的 namespace 兼容值。
     * / Returns the removed namespace compatibility value.
     *
     * @return 始终为空字符串 / always an empty string
     * @deprecated namespace 不再参与注册中心发现。
     * / namespace is no longer part of registry discovery.
     */
    @Deprecated(forRemoval = true)
    public String namespace() {
        return "";
    }

    /**
     * 对单个可选文本条件执行精确匹配。
     * / Applies exact matching for one optional text criterion.
     *
     * @param expected 查询期望值，为空表示通配 / expected query value, null as a wildcard
     * @param actual   服务键实际值 / actual service-key value
     * @return 条件为空或值相等时为 {@code true} / {@code true} when the criterion is null or values are equal
     */
    private boolean matches(String expected, String actual) {
        return expected == null || expected.equals(actual);
    }

    /**
     * 规范化可选查询文本。
     * / Normalizes optional query text.
     *
     * @param value 原始查询值 / original query value
     * @return 去除首尾空白后的值；空白输入返回 {@code null}
     * / trimmed value, or {@code null} for blank input
     */
    private static String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
