package top.egon.cola.component.ddc.admin.security.openapi;

import org.springframework.stereotype.Component;
import top.egon.cola.component.ddc.admin.config.DdcAdminProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public final class DdcHmacCredentialRegistry {

    private final Map<String, DdcHmacCredential> credentialsByAccessKey;

    public DdcHmacCredentialRegistry(DdcAdminProperties properties) {
        credentialsByAccessKey = build(properties.getOpenapi());
    }

    public Optional<DdcHmacCredential> resolve(String accessKey) {
        if (accessKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(credentialsByAccessKey.get(accessKey));
    }

    public boolean isEmpty() {
        return credentialsByAccessKey.isEmpty();
    }

    private Map<String, DdcHmacCredential> build(
            DdcAdminProperties.Openapi openapi) {
        List<DdcAdminProperties.Credential> configured =
                openapi.getCredentials() == null
                        ? List.of()
                        : openapi.getCredentials();
        LinkedHashMap<String, DdcHmacCredential> result =
                new LinkedHashMap<>();
        for (DdcAdminProperties.Credential value : configured) {
            DdcHmacCredential credential = new DdcHmacCredential(
                    value.getCredentialId(),
                    value.getAccessKey(),
                    value.getSecret(),
                    value.getClientType(),
                    Set.copyOf(value.getAppCodePatterns()),
                    Set.copyOf(value.getEnvPatterns()),
                    bizCodePatterns(value),
                    Set.copyOf(value.getAllowedOperations())
            );
            if (result.putIfAbsent(
                    credential.accessKey(),
                    credential
            ) != null) {
                throw new IllegalStateException(
                        "Duplicate DDC HMAC access key"
                );
            }
        }
        if (result.isEmpty()
                && hasText(openapi.getAccessKey())
                && hasText(openapi.getSecretKey())) {
            DdcHmacCredential legacy = new DdcHmacCredential(
                    "legacy-service",
                    openapi.getAccessKey(),
                    openapi.getSecretKey(),
                    "*",
                    Set.of("*"),
                    Set.of("*"),
                    Set.of("*"),
                    Set.of("*")
            );
            result.put(legacy.accessKey(), legacy);
        }
        return Map.copyOf(result);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Set<String> bizCodePatterns(
            DdcAdminProperties.Credential credential) {
        List<String> configured = credential.getBizCodePatterns();
        if (configured == null || configured.isEmpty()) {
            configured = credential.getNamespacePatterns();
        }
        return configured == null ? Set.of() : Set.copyOf(configured);
    }
}
