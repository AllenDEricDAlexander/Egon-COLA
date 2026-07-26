package top.egon.cola.component.ddc.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@EnableConfigurationProperties(DdcAdminProperties.class)
public final class DdcAdminSecurityPropertiesValidator {

    public DdcAdminSecurityPropertiesValidator(
            DdcAdminProperties properties) {
        validate(properties);
    }

    static void validate(DdcAdminProperties properties) {
        DdcAdminProperties.Security security = properties.getSecurity();
        if (!security.isLocalDev()) {
            required(
                    security.getJwt().getIssuer(),
                    "DDC Admin JWT issuer is required"
            );
            required(
                    security.getJwt().getAudience(),
                    "DDC Admin JWT audience is required"
            );
            if (!properties.getOpenapi().isSignatureEnabled()) {
                throw new IllegalStateException(
                        "DDC OpenAPI signatures are required outside local-dev"
                );
            }
        }
        validateOpenApi(properties.getOpenapi(), security.isLocalDev());
    }

    private static void validateOpenApi(
            DdcAdminProperties.Openapi openapi,
            boolean localDev) {
        if (openapi.getAllowedClockSkewSeconds() <= 0) {
            throw new IllegalStateException(
                    "DDC OpenAPI allowed clock skew must be positive"
            );
        }
        if (openapi.getNonceCacheMaxSize() <= 0) {
            throw new IllegalStateException(
                    "DDC OpenAPI nonce cache size must be positive"
            );
        }
        if (!openapi.isSignatureEnabled()) {
            return;
        }
        List<DdcAdminProperties.Credential> credentials =
                openapi.getCredentials() == null
                        ? List.of()
                        : openapi.getCredentials();
        if (credentials.isEmpty()) {
            if (localDev
                    && hasText(openapi.getAccessKey())
                    && hasText(openapi.getSecretKey())) {
                return;
            }
            throw new IllegalStateException(
                    "DDC OpenAPI credentials are required"
            );
        }
        Set<String> credentialIds = new HashSet<>();
        Set<String> accessKeys = new HashSet<>();
        for (DdcAdminProperties.Credential credential : credentials) {
            String credentialId = required(
                    credential.getCredentialId(),
                    "DDC OpenAPI credential id is required"
            );
            if (!credentialIds.add(credentialId)) {
                throw new IllegalStateException(
                        "Duplicate DDC OpenAPI credential id: "
                                + credentialId
                );
            }
            String accessKey = required(
                    credential.getAccessKey(),
                    "DDC OpenAPI credential access key is required: "
                            + credentialId
            );
            if (!accessKeys.add(accessKey)) {
                throw new IllegalStateException(
                        "Duplicate DDC OpenAPI credential access key"
                );
            }
            required(
                    credential.getSecret(),
                    "DDC OpenAPI credential secret is required: "
                            + credentialId
            );
            required(
                    credential.getClientType(),
                    "DDC OpenAPI credential client type is required: "
                            + credentialId
            );
        }
    }

    private static String required(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalStateException(message);
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
