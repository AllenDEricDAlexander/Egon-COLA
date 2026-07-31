package top.egon.cola.component.ddc.model.registry;

import top.egon.cola.component.ddc.model.enums.DdcServiceKind;

public record DdcServiceQuery(
        String bizCode,
        String appCode,
        String env,
        String namespace,
        DdcServiceKind serviceKind,
        String protocol,
        String serviceName,
        String group,
        String version
) {

    public DdcServiceQuery {
        if (bizCode == null || bizCode.isBlank()) {
            throw new IllegalArgumentException("bizCode is required");
        }
        if (appCode == null || appCode.isBlank()) {
            throw new IllegalArgumentException("appCode is required");
        }
        if (env == null || env.isBlank()) {
            throw new IllegalArgumentException("env is required");
        }
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace is required");
        }
        if (serviceKind == null) {
            throw new IllegalArgumentException("serviceKind is required");
        }
        if (protocol == null || protocol.isBlank()) {
            throw new IllegalArgumentException("protocol is required");
        }
        protocol = protocol.toLowerCase(java.util.Locale.ROOT);
    }

    public boolean matches(DdcServiceKey key) {
        return bizCode.equals(key.bizCode())
                && appCode.equals(key.appCode())
                && env.equals(key.env())
                && namespace.equals(key.namespace())
                && serviceKind == key.serviceKind()
                && protocol.equals(key.protocol())
                && matches(serviceName, key.serviceName())
                && matches(group, key.group())
                && matches(version, key.version());
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.isBlank() || expected.equals(actual);
    }
}
