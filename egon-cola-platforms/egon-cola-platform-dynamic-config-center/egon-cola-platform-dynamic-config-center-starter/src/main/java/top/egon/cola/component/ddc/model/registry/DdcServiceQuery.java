package top.egon.cola.component.ddc.model.registry;

import top.egon.cola.component.ddc.model.enums.DdcServiceKind;

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

    public boolean hasExactCatalogScope() {
        return bizCode != null
                && env != null
                && appCode != null
                && serviceKind != null
                && protocol != null;
    }

    /**
     * @deprecated namespace is an authorization view and is not a registry filter.
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
     * @deprecated namespace is no longer part of registry discovery.
     */
    @Deprecated(forRemoval = true)
    public String namespace() {
        return "";
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.equals(actual);
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
