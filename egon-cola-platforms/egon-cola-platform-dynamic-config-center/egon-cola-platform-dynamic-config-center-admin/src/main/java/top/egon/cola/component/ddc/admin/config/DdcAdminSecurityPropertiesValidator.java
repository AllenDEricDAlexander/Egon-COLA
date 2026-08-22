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
            if (!properties.getRpc().isSignatureEnabled()) {
                throw new IllegalStateException(
                        "DDC RPC signatures are required outside local-dev"
                );
            }
        }
        validateRegistration(properties.getRegistration());
        validateRpc(properties.getRpc());
    }

    private static void validateRegistration(
            DdcAdminProperties.Registration registration) {
        if (registration == null) {
            throw new IllegalStateException(
                    "DDC registration settings are required"
            );
        }
        required(
                registration.getRequiredScope(),
                "DDC registration scope is required"
        );
        if (registration.getResourceUri() != null
                && !registration.getResourceUri().isAbsolute()) {
            throw new IllegalStateException(
                    "DDC registration Resource URI must be absolute"
            );
        }
    }

    private static void validateRpc(DdcAdminProperties.Rpc rpc) {
        if (rpc.getAllowedClockSkewSeconds() <= 0) {
            throw new IllegalStateException(
                    "DDC RPC allowed clock skew must be positive"
            );
        }
        if (rpc.getNonceCacheMaxSize() <= 0) {
            throw new IllegalStateException(
                    "DDC RPC nonce cache size must be positive"
            );
        }
        if (!rpc.isSignatureEnabled()) {
            return;
        }
        List<DdcAdminProperties.Credential> credentials =
                rpc.getCredentials() == null
                        ? List.of()
                        : rpc.getCredentials();
        if (credentials.isEmpty()) {
            throw new IllegalStateException(
                    "DDC RPC credentials are required"
            );
        }
        Set<String> credentialIds = new HashSet<>();
        Set<String> accessKeys = new HashSet<>();
        for (DdcAdminProperties.Credential credential : credentials) {
            String credentialId = required(
                    credential.getCredentialId(),
                    "DDC RPC credential id is required"
            );
            if (!credentialIds.add(credentialId)) {
                throw new IllegalStateException(
                        "Duplicate DDC RPC credential id: "
                                + credentialId
                );
            }
            String accessKey = required(
                    credential.getAccessKey(),
                    "DDC RPC credential access key is required: "
                            + credentialId
            );
            if (!accessKeys.add(accessKey)) {
                throw new IllegalStateException(
                        "Duplicate DDC RPC credential access key"
                );
            }
            required(
                    credential.getSecret(),
                    "DDC RPC credential secret is required: "
                            + credentialId
            );
            required(
                    credential.getClientType(),
                    "DDC RPC credential client type is required: "
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
