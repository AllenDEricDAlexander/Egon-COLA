package top.egon.cola.component.gateway.core.security;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record GatewaySecurityPolicy(
        String policyId,
        AuthenticationMode authenticationMode,
        List<String> credentialExtractorIds,
        List<String> authenticationProviderIds,
        List<String> authorizationProviderIds,
        AuthorizationDecisionMode decisionMode,
        String identityMapperId,
        Duration providerTimeout,
        SecurityFailureMode failureMode
) {

    public GatewaySecurityPolicy {
        policyId = required(policyId, "policyId");
        authenticationMode = Objects.requireNonNull(
                authenticationMode,
                "authenticationMode"
        );
        credentialExtractorIds = identifiers(
                credentialExtractorIds,
                "credentialExtractorIds"
        );
        authenticationProviderIds = identifiers(
                authenticationProviderIds,
                "authenticationProviderIds"
        );
        authorizationProviderIds = identifiers(
                authorizationProviderIds,
                "authorizationProviderIds"
        );
        decisionMode = Objects.requireNonNull(decisionMode, "decisionMode");
        identityMapperId = optional(identityMapperId);
        providerTimeout = Objects.requireNonNull(
                providerTimeout,
                "providerTimeout"
        );
        failureMode = Objects.requireNonNull(failureMode, "failureMode");
        if (providerTimeout.isNegative()
                || providerTimeout.isZero()
                || providerTimeout.compareTo(Duration.ofSeconds(30)) > 0) {
            throw new IllegalArgumentException(
                    "providerTimeout must be within (0, 30s]"
            );
        }
        if (failureMode != SecurityFailureMode.FAIL_CLOSED) {
            throw new IllegalArgumentException(
                    "only FAIL_CLOSED is supported"
            );
        }
        if (authenticationMode == AuthenticationMode.REQUIRED
                && (credentialExtractorIds.isEmpty()
                || authenticationProviderIds.isEmpty())) {
            throw new IllegalArgumentException(
                    "REQUIRED needs extractors and authentication providers"
            );
        }
        if (decisionMode == AuthorizationDecisionMode.ANY_ALLOW
                && authorizationProviderIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "ANY_ALLOW needs authorization providers"
            );
        }
    }

    private static List<String> identifiers(
            List<String> values,
            String field) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> normalized = values.stream()
                .map(value -> required(value, field))
                .toList();
        if (normalized.size() != normalized.stream().distinct().count()) {
            throw new IllegalArgumentException(field + " contains duplicates");
        }
        return normalized;
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > 128) {
            throw new IllegalArgumentException(field + " exceeds 128");
        }
        return normalized;
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : required(value, "id");
    }
}
