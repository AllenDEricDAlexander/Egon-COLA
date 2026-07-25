package top.egon.cola.component.gateway.engine.cors;

import java.util.Set;

public record RuntimeCorsPolicy(
        String policyId,
        Set<String> allowedOrigins,
        Set<String> allowedMethods,
        Set<String> allowedHeaders,
        Set<String> exposedHeaders,
        boolean allowCredentials,
        long maxAgeSeconds,
        boolean enabled
) {

    public RuntimeCorsPolicy {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("policyId is required");
        }
        allowedOrigins = Set.copyOf(allowedOrigins);
        allowedMethods = Set.copyOf(allowedMethods);
        allowedHeaders = Set.copyOf(allowedHeaders);
        exposedHeaders = Set.copyOf(exposedHeaders);
        if (allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException(
                    "CORS allowedOrigins must not be empty"
            );
        }
        if (allowedMethods.isEmpty()) {
            throw new IllegalArgumentException(
                    "CORS allowedMethods must not be empty"
            );
        }
        if (allowCredentials && allowedOrigins.contains("*")) {
            throw new IllegalArgumentException(
                    "credentialed CORS must not use wildcard origin"
            );
        }
        if (maxAgeSeconds < 0) {
            throw new IllegalArgumentException(
                    "CORS maxAgeSeconds must not be negative"
            );
        }
    }
}
