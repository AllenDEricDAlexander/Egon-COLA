package top.egon.cola.platform.tianquan.shoubing.starter.security;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;

/** Applies the shared shrinking tenant allowlist only after the legacy JWT is signature-verified. */
@Slf4j
@RequiredArgsConstructor
@Validated
public class LegacyIssuerCutoverPolicy {

    @Qualifier("rbac3RuntimeRedissonClient")
    private final RedissonClient redisson;
    @Qualifier("idpStarterProperties")
    private final IdpStarterProperties properties;

    public boolean isLegacyIssuerHint(String compactToken) {
        String expected = legacyIssuer();
        if (expected == null || compactToken == null || compactToken.isBlank()) {
            return false;
        }
        try {
            JWT parsed = JWTParser.parse(compactToken);
            String issuer = parsed.getJWTClaimsSet().getIssuer();
            return expected.equals(issuer);
        } catch (java.text.ParseException malformed) {
            return false;
        }
    }

    public boolean isLegacyIssuer(String issuer) {
        String expected = legacyIssuer();
        return expected != null && expected.equals(issuer);
    }

    public Decision check(String verifiedIssuer, String verifiedLegacyTenantId) {
        if (!isLegacyIssuer(verifiedIssuer)) {
            return Decision.BLOCKED;
        }
        IdpStarterProperties.Migration migration = properties.getMigration();
        String key = migration.getLegacyTenantAllowlistKey();
        if (key == null || key.isBlank()) {
            return Decision.ALLOWED;
        }
        if (verifiedLegacyTenantId == null
                || !verifiedLegacyTenantId.matches("[1-9][0-9]{0,18}")) {
            return Decision.BLOCKED;
        }
        try {
            RSet<String> allowed = redisson.getSet(key, StringCodec.INSTANCE);
            return allowed.containsAsync(verifiedLegacyTenantId).get(
                    properties.getRevocation().getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    ? Decision.ALLOWED : Decision.BLOCKED;
        } catch (TimeoutException timeout) {
            log.warn("Legacy issuer cutover state timed out");
            return Decision.UNAVAILABLE;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            log.warn("Legacy issuer cutover state was interrupted");
            return Decision.UNAVAILABLE;
        } catch (ExecutionException | RuntimeException unavailable) {
            log.warn("Legacy issuer cutover state is unavailable");
            return Decision.UNAVAILABLE;
        }
    }

    private String legacyIssuer() {
        String configured = properties.getMigration().getLegacyIssuer();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return properties.getTrustedIssuers().isEmpty()
                ? properties.getIssuer() : null;
    }

    public enum Decision {
        ALLOWED,
        BLOCKED,
        UNAVAILABLE
    }
}
