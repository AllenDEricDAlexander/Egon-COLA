package top.egon.cola.component.gateway.core.provider;

import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

public record ProviderServiceKey(
        String bizCode,
        String appCode,
        String env,
        String namespace,
        ProviderProtocolType protocolType,
        String serviceName,
        String group,
        String version,
        String transport
) implements Comparable<ProviderServiceKey> {

    private static final Comparator<ProviderServiceKey> ORDER = Comparator
            .comparing(ProviderServiceKey::bizCode)
            .thenComparing(ProviderServiceKey::appCode)
            .thenComparing(ProviderServiceKey::env)
            .thenComparing(ProviderServiceKey::namespace)
            .thenComparing(ProviderServiceKey::protocolType)
            .thenComparing(ProviderServiceKey::serviceName)
            .thenComparing(ProviderServiceKey::group)
            .thenComparing(ProviderServiceKey::version)
            .thenComparing(ProviderServiceKey::transport);

    public ProviderServiceKey {
        bizCode = required(bizCode, "bizCode");
        appCode = required(appCode, "appCode");
        env = required(env, "env");
        namespace = required(namespace, "namespace");
        protocolType = Objects.requireNonNull(protocolType, "protocolType");
        serviceName = required(serviceName, "serviceName");
        group = required(group, "group");
        version = required(version, "version");
        transport = required(transport, "transport").toLowerCase(Locale.ROOT);
        if (protocolType == ProviderProtocolType.HTTP
                && !transport.equals("http")
                && !transport.equals("https")) {
            throw new IllegalArgumentException(
                    "HTTP provider transport must be http or https"
            );
        }
        if (protocolType == ProviderProtocolType.RPC
                && !transport.equals("grpc")) {
            throw new IllegalArgumentException(
                    "RPC provider transport must be grpc"
            );
        }
    }

    public String canonicalValue() {
        return String.join(
                ":",
                bizCode,
                appCode,
                env,
                namespace,
                protocolType.name(),
                serviceName,
                group,
                version,
                transport
        );
    }

    @Override
    public int compareTo(ProviderServiceKey other) {
        return ORDER.compare(this, other);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || value.contains(":")) {
            throw new IllegalArgumentException(
                    field + " is required and must not contain ':'"
            );
        }
        return value.trim();
    }
}
