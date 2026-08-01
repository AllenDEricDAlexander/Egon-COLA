package top.egon.cola.component.ddc.model.registry;

import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;

import java.util.Comparator;

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

    private static final Comparator<DdcServiceKey> ORDER = Comparator
            .comparing(DdcServiceKey::bizCode)
            .thenComparing(DdcServiceKey::env)
            .thenComparing(DdcServiceKey::appCode)
            .thenComparing(DdcServiceKey::serviceKind)
            .thenComparing(DdcServiceKey::protocol)
            .thenComparing(DdcServiceKey::serviceName)
            .thenComparing(DdcServiceKey::group)
            .thenComparing(DdcServiceKey::version);

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

    public String serviceId() {
        return Digests.sha256Hex(canonicalValue());
    }

    /**
     * @deprecated namespace is an authorization view and is not part of the
     * physical service identity.
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
     * @deprecated namespace is no longer part of the physical service key.
     */
    @Deprecated(forRemoval = true)
    public String namespace() {
        return "";
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
