package top.egon.cola.component.gateway.core.provider;

public record ProviderQuery(
        String bizCode,
        String appCode,
        String env,
        String namespace,
        ProviderProtocolType protocolType
) {

    public ProviderQuery {
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
    }

    public boolean matches(ProviderServiceKey key) {
        return bizCode.equals(key.bizCode())
                && appCode.equals(key.appCode())
                && env.equals(key.env())
                && namespace.equals(key.namespace())
                && (protocolType == null || protocolType == key.protocolType());
    }
}
