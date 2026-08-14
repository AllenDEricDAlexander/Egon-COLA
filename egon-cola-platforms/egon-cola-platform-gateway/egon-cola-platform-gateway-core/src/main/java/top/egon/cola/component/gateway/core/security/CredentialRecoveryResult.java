package top.egon.cola.component.gateway.core.security;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Result of a single recovery attempt; token values remain in-process only.
 */
public record CredentialRecoveryResult(
        Outcome outcome,
        GatewayCredential credential,
        Set<String> fieldsToRemove,
        Map<String, List<String>> responseHeaders
) {

    public enum Outcome {
        RECOVERED,
        NOT_RECOVERABLE,
        FAILED
    }

    public CredentialRecoveryResult {
        outcome = Objects.requireNonNull(outcome, "outcome");
        fieldsToRemove = Set.copyOf(fieldsToRemove == null
                ? Set.of()
                : fieldsToRemove);
        responseHeaders = immutableHeaders(responseHeaders);
        if (outcome == Outcome.RECOVERED && credential == null) {
            throw new IllegalArgumentException(
                    "RECOVERED requires a credential");
        }
        if (outcome != Outcome.RECOVERED && credential != null) {
            throw new IllegalArgumentException(
                    "only RECOVERED may contain a credential");
        }
    }

    public static CredentialRecoveryResult recovered(
            GatewayCredential credential,
            Set<String> fieldsToRemove,
            Map<String, List<String>> responseHeaders) {
        return new CredentialRecoveryResult(
                Outcome.RECOVERED,
                credential,
                fieldsToRemove,
                responseHeaders);
    }

    public static CredentialRecoveryResult notRecoverable() {
        return new CredentialRecoveryResult(
                Outcome.NOT_RECOVERABLE,
                null,
                Set.of(),
                Map.of());
    }

    public static CredentialRecoveryResult failed() {
        return new CredentialRecoveryResult(
                Outcome.FAILED,
                null,
                Set.of(),
                Map.of());
    }

    private static Map<String, List<String>> immutableHeaders(
            Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> copy = new java.util.LinkedHashMap<>();
        source.forEach((name, values) -> {
            if (name == null || !"set-cookie".equalsIgnoreCase(name)) {
                throw new IllegalArgumentException(
                        "only Set-Cookie recovery headers are allowed");
            }
            if (values == null || values.isEmpty()) {
                throw new IllegalArgumentException(
                        "recovery response header values are required");
            }
            copy.put("set-cookie", List.copyOf(values));
        });
        return Map.copyOf(copy);
    }
}
