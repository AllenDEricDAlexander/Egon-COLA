package top.egon.cola.platform.idp.contract;

import java.util.Objects;
import java.util.Set;

public record OAuthClientView(
        String clientId,
        String name,
        Status status,
        boolean pkceRequired,
        Set<String> redirectUris,
        Set<String> audiences
) {

    public OAuthClientView {
        clientId = required(clientId, "clientId");
        name = required(name, "name");
        status = Objects.requireNonNull(status, "status");
        redirectUris = immutableNonEmpty(redirectUris, "redirectUris");
        audiences = immutableNonEmpty(audiences, "audiences");
    }

    private static Set<String> immutableNonEmpty(
            Set<String> values,
            String fieldName
    ) {
        Set<String> result = Set.copyOf(Objects.requireNonNull(
                values,
                fieldName
        ));
        if (result.isEmpty() || result.stream().anyMatch(
                value -> value == null || value.isBlank()
        )) {
            throw new IllegalArgumentException(
                    fieldName + " must contain only non-blank values"
            );
        }
        return result;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public enum Status {
        ACTIVE,
        DISABLED
    }
}
