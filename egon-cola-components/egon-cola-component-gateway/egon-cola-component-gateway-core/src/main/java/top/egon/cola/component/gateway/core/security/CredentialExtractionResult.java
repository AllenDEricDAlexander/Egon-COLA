package top.egon.cola.component.gateway.core.security;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record CredentialExtractionResult(
        List<GatewayCredential> credentials,
        Set<String> fieldsToRemove,
        String errorCode
) {

    public CredentialExtractionResult {
        credentials = List.copyOf(Objects.requireNonNull(
                credentials,
                "credentials"
        ));
        if (credentials.size() > 8) {
            throw new IllegalArgumentException(
                    "credential count exceeds 8"
            );
        }
        fieldsToRemove = Objects.requireNonNull(
                fieldsToRemove,
                "fieldsToRemove"
        ).stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        errorCode = errorCode == null || errorCode.isBlank()
                ? null
                : errorCode.trim();
    }

    public static CredentialExtractionResult empty() {
        return new CredentialExtractionResult(List.of(), Set.of(), null);
    }

    public static CredentialExtractionResult invalid() {
        return new CredentialExtractionResult(
                List.of(),
                Set.of(),
                "GATEWAY_CREDENTIAL_INVALID"
        );
    }

    public boolean valid() {
        return errorCode == null;
    }
}
