package top.egon.cola.component.tianshu.admin.config;

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
                    "Tianshu Admin JWT issuer is required"
            );
            required(
                    security.getJwt().getAudience(),
                    "Tianshu Admin JWT audience is required"
            );
            if (!properties.getRpc().isSignatureEnabled()) {
                throw new IllegalStateException(
                        "Tianshu RPC signatures are required outside local-dev"
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
                    "Tianshu registration settings are required"
            );
        }
        required(
                registration.getRequiredScope(),
                "Tianshu registration scope is required"
        );
        if (registration.getResourceUri() != null
                && !registration.getResourceUri().isAbsolute()) {
            throw new IllegalStateException(
                    "Tianshu registration Resource URI must be absolute"
            );
        }
    }

    private static void validateRpc(DdcAdminProperties.Rpc rpc) {
        if (rpc.getAllowedClockSkewSeconds() <= 0) {
            throw new IllegalStateException(
                    "Tianshu RPC allowed clock skew must be positive"
            );
        }
        if (rpc.getNonceCacheMaxSize() <= 0) {
            throw new IllegalStateException(
                    "Tianshu RPC nonce cache size must be positive"
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
                    "Tianshu RPC credentials are required"
            );
        }
        Set<String> credentialIds = new HashSet<>();
        Set<String> accessKeys = new HashSet<>();
        for (DdcAdminProperties.Credential credential : credentials) {
            String credentialId = required(
                    credential.getCredentialId(),
                    "Tianshu RPC credential id is required"
            );
            if (!credentialIds.add(credentialId)) {
                throw new IllegalStateException(
                        "Duplicate Tianshu RPC credential id: "
                                + credentialId
                );
            }
            String accessKey = required(
                    credential.getAccessKey(),
                    "Tianshu RPC credential access key is required: "
                            + credentialId
            );
            if (!accessKeys.add(accessKey)) {
                throw new IllegalStateException(
                        "Duplicate Tianshu RPC credential access key"
                );
            }
            required(
                    credential.getSecret(),
                    "Tianshu RPC credential secret is required: "
                            + credentialId
            );
            required(
                    credential.getClientType(),
                    "Tianshu RPC credential client type is required: "
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
