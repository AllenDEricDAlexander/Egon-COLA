package top.egon.cola.platform.tianquan.shoubing.starter.security.impl;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;
import org.redisson.api.RScript;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.contract.OAuthSecurityVersionBO;
import top.egon.cola.platform.tianquan.shoubing.contract.PrincipalType;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;

/** Reads issuer generation and deny state in one same-slot Redis operation. */
@Slf4j
@RequiredArgsConstructor
@Validated
public class RedisOAuthRevocationReader implements OAuthRevocationReader {

    private static final String REDIS_PREFIX = "egon:oauth:security:v2:{%s}:";

    public static final String READ_SECURITY_SNAPSHOT_SCRIPT = """
            local readyEpoch = redis.call('HGET', KEYS[1], 'epoch') or ''
            local readyRevisionValue = redis.call('HGET', KEYS[1], 'lastRevision') or ''
            local readyStatus = redis.call('HGET', KEYS[1], 'status') or ''
            if readyEpoch ~= ARGV[1] or readyStatus ~= 'READY' then
              return {'U', 'OAUTH_SECURITY_READY_NOT_AVAILABLE'}
            end
            local readyRevision = tonumber(readyRevisionValue)
            if not readyRevision then
              return {'U', 'OAUTH_SECURITY_READY_INVALID'}
            end
            local function checkGeneration(key, expectedGeneration)
              local generation = redis.call('HGET', key, 'generation') or ''
              local enabled = redis.call('HGET', key, 'enabled') or ''
              local revision = tonumber(redis.call('HGET', key, 'revision') or '')
              if generation == '' or enabled == '' or not revision then
                return {'U', 'OAUTH_SECURITY_GENERATION_MISSING'}
              end
              if revision > readyRevision then
                return {'U', 'OAUTH_SECURITY_READY_REVISION_STALE'}
              end
              if (enabled ~= '1' and enabled ~= 'true') or generation ~= expectedGeneration then
                return {'R', 'JWT_SECURITY_VERSION_STALE'}
              end
              return nil
            end
            local result = checkGeneration(KEYS[2], ARGV[3])
            if result then return result end
            result = checkGeneration(KEYS[3], ARGV[4])
            if result then return result end
            if ARGV[2] == 'USER' then
              result = checkGeneration(KEYS[4], ARGV[5])
              if result then return result end
            end
            if redis.call('EXISTS', KEYS[5]) == 1 then
              return {'R', 'JWT_REVOKED'}
            end
            if redis.call('EXISTS', KEYS[6]) == 1 then
              return {'R', 'JWT_AUTHORIZATION_REVOKED'}
            end
            if redis.call('EXISTS', KEYS[7]) == 1 then
              return {'R', 'JWT_SIGNING_KEY_REVOKED'}
            end
            return {'V', 'OAUTH_SECURITY_CURRENT'}
            """;

    @Qualifier("rbac3RuntimeRedissonClient")
    private final RedissonClient redisson;
    @Qualifier("trustedIssuerRegistry")
    private final TrustedIssuerRegistry trustedIssuerRegistry;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("idpStarterProperties")
    private final IdpStarterProperties properties;

    @PostConstruct
    public void validateConfiguration() {
        if (trustedIssuerRegistry.isEmpty()) {
            return;
        }
        properties.validate();
        validatePrimaryEndpoint(redisson.getConfig(),
                properties.getRevocation().getPrimaryEndpoint(), objectMapper);
    }

    @Override
    public Result check(VerifiedJwtBO verifiedJwt) {
        VerifiedJwtBO token = Objects.requireNonNull(verifiedJwt, "verifiedJwt");
        token.validate();
        TrustedIssuerRegistry.TrustedIssuerVO issuer = token.getTrustedIssuer();
        TrustedIssuerRegistry.TrustedIssuerVO current = trustedIssuerRegistry
                .findByIssuer(issuer.issuer().toASCIIString())
                .orElse(null);
        if (current == null || current.trustVersion() != issuer.trustVersion()) {
            return Result.revoked("JWT_ISSUER_NOT_ACTIVE");
        }
        String issuerHash = TrustedIssuerRegistry.issuerHash(issuer.issuer());
        RFuture<List<Object>> future;
        try {
            List<Object> keys = redisKeys(token, issuerHash);
            List<Object> arguments = redisArguments(token, properties.getRevocation().getEpoch());
            future = redisson.getScript(StringCodec.INSTANCE).evalAsync(
                    RScript.Mode.READ_ONLY,
                    READ_SECURITY_SNAPSHOT_SCRIPT,
                    RScript.ReturnType.MULTI,
                    keys,
                    arguments.toArray());
            List<Object> values = future.get(
                    properties.getRevocation().getTimeout().toMillis(), TimeUnit.MILLISECONDS);
            return interpret(values);
        } catch (TimeoutException timeout) {
            log.warn("OAuth security state read timed out; issuerHash={}", issuerHash);
            return Result.unavailable("OAUTH_SECURITY_STATE_TIMEOUT");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            log.warn("OAuth security state read was interrupted; issuerHash={}", issuerHash);
            return Result.unavailable("OAUTH_SECURITY_STATE_UNAVAILABLE");
        } catch (ExecutionException | RuntimeException failure) {
            log.warn("OAuth security state read is unavailable; issuerHash={}", issuerHash);
            return Result.unavailable("OAUTH_SECURITY_STATE_UNAVAILABLE");
        }
    }

    public static List<Object> redisKeys(
            VerifiedJwtBO token,
            String issuerHash) {
        String prefix = REDIS_PREFIX.formatted(issuerHash);
        List<Object> keys = new ArrayList<>(List.of(
                prefix + "ready",
                prefix + "generation:realm",
                prefix + "generation:client:" + token.getClientRegistrationId(),
                prefix + "generation:subject:" + sha256(token.getSubject()),
                prefix + "deny:jti:" + token.getTokenId(),
                prefix + "deny:authorization:" + token.getAuthorizationId(),
                prefix + "deny:kid:" + token.getKeyId()));
        return List.copyOf(keys);
    }

    public static List<Object> redisArguments(VerifiedJwtBO token, String epoch) {
        OAuthSecurityVersionBO version = Objects.requireNonNull(
                token.getSecurityVersion(), "securityVersion");
        PrincipalType principalType = Objects.requireNonNull(token.getPrincipalType(), "principalType");
        List<Object> arguments = new ArrayList<>(List.of(
                Objects.requireNonNull(epoch, "epoch"),
                principalType.name(),
                Long.toString(version.getRealm()),
                Long.toString(version.getClient()),
                version.getSubject() == null ? "" : Long.toString(version.getSubject())));
        return List.copyOf(arguments);
    }

    public static Result interpret(List<Object> values) {
        if (values == null || values.size() != 2) {
            return Result.unavailable("OAUTH_SECURITY_STATE_RESPONSE_INVALID");
        }
        String status = text(values.get(0));
        String reason = text(values.get(1));
        if ("V".equals(status)) {
            return Result.valid();
        }
        if ("R".equals(status)) {
            return Result.revoked(reason);
        }
        if ("U".equals(status)) {
            return Result.unavailable(reason);
        }
        return Result.unavailable("OAUTH_SECURITY_STATE_RESPONSE_INVALID");
    }

    public static void validatePrimaryEndpoint(
            Config config,
            String expectedEndpoint,
            ObjectMapper objectMapper) {
        Objects.requireNonNull(config, "redissonConfig");
        URI expected = URI.create(Objects.requireNonNull(expectedEndpoint, "primaryEndpoint"));
        if (!config.isSingleConfig()) {
            throw new IllegalStateException("OAuth revocation requires a fenced single PRIMARY client");
        }
        URI configured;
        try {
            JsonNode single = objectMapper.readTree(config.toJSON()).path("singleServerConfig");
            String address = single.path("address").asText(null);
            if (address == null || address.isBlank()) {
                throw new IllegalStateException("OAuth revocation PRIMARY address is not configured");
            }
            configured = URI.create(address);
        } catch (IOException malformed) {
            throw new IllegalStateException("OAuth revocation Redisson config cannot be inspected", malformed);
        }
        if (!sameEndpoint(expected, configured)) {
            throw new IllegalStateException("OAuth revocation Redis client does not match primary-endpoint");
        }
    }

    private static boolean sameEndpoint(URI expected, URI configured) {
        return expected.getScheme().equalsIgnoreCase(configured.getScheme())
                && expected.getHost().equalsIgnoreCase(configured.getHost())
                && effectivePort(expected) == effectivePort(configured);
    }

    private static int effectivePort(URI uri) {
        return uri.getPort() < 0 ? 6379 : uri.getPort();
    }

    private static String text(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return value == null ? "" : value.toString();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
