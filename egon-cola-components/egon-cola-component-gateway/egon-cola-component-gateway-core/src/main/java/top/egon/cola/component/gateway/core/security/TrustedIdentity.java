package top.egon.cola.component.gateway.core.security;

import java.util.Map;

public record TrustedIdentity(
        Map<String, String> httpHeaders,
        Map<String, String> rpcMetadata
) {

    public TrustedIdentity {
        httpHeaders = Map.copyOf(httpHeaders == null
                ? Map.of()
                : httpHeaders);
        rpcMetadata = Map.copyOf(rpcMetadata == null
                ? Map.of()
                : rpcMetadata);
    }

    public static TrustedIdentity empty() {
        return new TrustedIdentity(Map.of(), Map.of());
    }
}
