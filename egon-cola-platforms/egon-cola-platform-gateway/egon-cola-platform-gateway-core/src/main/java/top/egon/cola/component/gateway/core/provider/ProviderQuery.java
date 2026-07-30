package top.egon.cola.component.gateway.core.provider;

public record ProviderQuery(
        String env,
        String namespace,
        ProviderProtocolType protocolType
) {

    public ProviderQuery {
        if (env == null || env.isBlank()) {
            throw new IllegalArgumentException("env is required");
        }
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace is required");
        }
    }

    public boolean matches(ProviderServiceKey key) {
        return env.equals(key.env())
                && namespace.equals(key.namespace())
                && (protocolType == null || protocolType == key.protocolType());
    }
}
