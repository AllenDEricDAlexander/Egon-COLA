package top.egon.cola.platform.rbac3.admin.integration.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Supplies a short-lived read-only OAuth credential without storing it in properties.
 */
@FunctionalInterface
public interface GatewayAdminStatusCredentialProvider {

    Optional<BearerCredential> current();

    static GatewayAdminStatusCredentialProvider rotatingFile(
            Path tokenFile,
            ObjectMapper objectMapper,
            Clock clock) {
        Objects.requireNonNull(tokenFile, "tokenFile");
        Objects.requireNonNull(objectMapper, "objectMapper");
        Objects.requireNonNull(clock, "clock");
        return () -> {
            if (!Files.isRegularFile(tokenFile)) {
                return Optional.empty();
            }
            try {
                JsonNode json = objectMapper.readTree(Files.readString(tokenFile));
                String token = json.path("accessToken").asText();
                String expiresAt = json.path("expiresAt").asText();
                if (token.isBlank() || expiresAt.isBlank()) {
                    return Optional.empty();
                }
                BearerCredential credential = new BearerCredential(
                        token, Instant.parse(expiresAt));
                return credential.expiresAt().isAfter(clock.instant())
                        ? Optional.of(credential)
                        : Optional.empty();
            } catch (Exception invalid) {
                return Optional.empty();
            }
        };
    }

    record BearerCredential(String accessToken, Instant expiresAt) {

        public BearerCredential {
            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalArgumentException("accessToken is required");
            }
            accessToken = accessToken.trim();
            expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        }

        @Override
        public String toString() {
            return "BearerCredential[accessToken=<redacted>, expiresAt=" + expiresAt + ']';
        }
    }
}
