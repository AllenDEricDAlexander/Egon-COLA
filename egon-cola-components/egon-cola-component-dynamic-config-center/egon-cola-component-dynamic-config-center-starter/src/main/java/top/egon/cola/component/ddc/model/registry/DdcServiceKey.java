package top.egon.cola.component.ddc.model.registry;

import top.egon.cola.component.ddc.model.enums.DdcServiceKind;

import java.util.Comparator;

public record DdcServiceKey(
        String env,
        String namespace,
        DdcServiceKind serviceKind,
        String serviceName,
        String group,
        String version,
        String protocol
) implements Comparable<DdcServiceKey> {

    private static final Comparator<DdcServiceKey> ORDER = Comparator
            .comparing(DdcServiceKey::env)
            .thenComparing(DdcServiceKey::namespace)
            .thenComparing(DdcServiceKey::serviceKind)
            .thenComparing(DdcServiceKey::protocol)
            .thenComparing(DdcServiceKey::serviceName)
            .thenComparing(DdcServiceKey::group)
            .thenComparing(DdcServiceKey::version);

    public DdcServiceKey {
        env = require(env, "env");
        namespace = require(namespace, "namespace");
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

    public String canonicalValue() {
        return String.join(
                "\n",
                env,
                namespace,
                serviceKind.name(),
                protocol,
                serviceName,
                group,
                version
        );
    }

    public static DdcServiceKey parse(String canonicalValue) {
        if (canonicalValue == null) {
            throw new IllegalArgumentException("canonical service key is required");
        }
        String[] parts = canonicalValue.split("\n", -1);
        if (parts.length != 7) {
            throw new IllegalArgumentException("invalid canonical service key");
        }
        return new DdcServiceKey(
                parts[0],
                parts[1],
                DdcServiceKind.valueOf(parts[2]),
                parts[4],
                parts[5],
                parts[6],
                parts[3]
        );
    }

    @Override
    public int compareTo(DdcServiceKey other) {
        return ORDER.compare(this, other);
    }

    private static String defaulted(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : require(value, "service key value");
    }

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
