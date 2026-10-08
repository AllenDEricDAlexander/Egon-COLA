package top.egon.cola.platform.tianquan.shoubing.starter.security;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;

/** Exposes the finite, exact issuer trust snapshot configured for this Resource Server. */
public final class TrustedIssuerRegistry {

    private final int maximumIssuers;
    private volatile Map<String, TrustedIssuerVO> issuers = Map.of();

    public TrustedIssuerRegistry(IdpStarterProperties properties) {
        IdpStarterProperties settings = Objects.requireNonNull(properties, "properties");
        this.maximumIssuers = settings.getJwksCache().getMaxIssuers();
        if (!settings.getTrustedIssuers().isEmpty()) {
            settings.validate();
        }
        replaceConfiguredEntries(settings.getTrustedIssuers());
    }

    /** Atomically replaces the local trust snapshot after an authorized configuration refresh. */
    public synchronized void replaceConfiguredEntries(
            Map<String, IdpStarterProperties.TrustedIssuerSettings> configuredIssuers) {
        Map<String, IdpStarterProperties.TrustedIssuerSettings> source =
                Objects.requireNonNull(configuredIssuers, "configuredIssuers");
        if (source.size() > maximumIssuers) {
            throw new IllegalArgumentException("trusted issuer count exceeds configured maximum");
        }
        Map<String, TrustedIssuerVO> snapshot = new LinkedHashMap<>();
        source.forEach((issuer, value) -> {
            IdpStarterProperties.TrustedIssuerSettings trusted =
                    Objects.requireNonNull(value, "trustedIssuerSettings");
            trusted.validate(issuer);
            TrustedIssuerVO mapped = new TrustedIssuerVO(
                    URI.create(trusted.getIssuer()),
                    URI.create(trusted.getJwksUri()),
                    trusted.getRealmKey(),
                    RealmType.valueOf(trusted.getRealmType()),
                    trusted.getTenantId(),
                    Status.valueOf(trusted.getStatus()),
                    trusted.getTrustVersion());
            String canonicalIssuer = mapped.issuer().toASCIIString();
            TrustedIssuerVO previous = this.issuers.get(canonicalIssuer);
            if (previous != null && mapped.trustVersion() < previous.trustVersion()) {
                throw new IllegalArgumentException("trustVersion must not decrease");
            }
            if (previous != null && mapped.trustVersion() == previous.trustVersion()
                    && !previous.equals(mapped)) {
                throw new IllegalArgumentException(
                        "issuer metadata changes require a higher trustVersion");
            }
            if (snapshot.putIfAbsent(canonicalIssuer, mapped) != null) {
                throw new IllegalArgumentException("trusted issuer URI is duplicated");
            }
        });
        this.issuers = Map.copyOf(snapshot);
    }

    /** Finds an exact configured ACTIVE issuer; this method never performs discovery or networking. */
    public Optional<TrustedIssuerVO> findByIssuer(String exactIssuer) {
        if (exactIssuer == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(issuers.get(exactIssuer))
                .filter(issuer -> issuer.status() == Status.ACTIVE);
    }

    /** Returns the immutable ACTIVE issuer snapshot in deterministic issuer order. */
    public List<TrustedIssuerVO> activeIssuers() {
        List<TrustedIssuerVO> result = new ArrayList<>();
        issuers.values().stream()
                .filter(issuer -> issuer.status() == Status.ACTIVE)
                .sorted((left, right) -> left.issuer().toASCIIString()
                        .compareTo(right.issuer().toASCIIString()))
                .forEach(result::add);
        return List.copyOf(result);
    }

    public boolean isEmpty() {
        return issuers.isEmpty();
    }

    public static String issuerHash(URI issuer) {
        Objects.requireNonNull(issuer, "issuer");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(issuer.toASCIIString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public enum RealmType {
        TENANT,
        PLATFORM
    }

    public enum Status {
        ACTIVE,
        INACTIVE
    }

    /** Immutable validated trust entry copied from configuration at auto-configuration time. */
    public record TrustedIssuerVO(
            URI issuer,
            URI jwksUri,
            String realmKey,
            RealmType realmType,
            String tenantId,
            Status status,
            long trustVersion
    ) {

        public TrustedIssuerVO {
            issuer = Objects.requireNonNull(issuer, "issuer");
            jwksUri = Objects.requireNonNull(jwksUri, "jwksUri");
            realmKey = required(realmKey, "realmKey");
            realmType = Objects.requireNonNull(realmType, "realmType");
            status = Objects.requireNonNull(status, "status");
            if (realmType == RealmType.TENANT) {
                tenantId = positiveTenantId(tenantId);
            } else if (tenantId != null) {
                throw new IllegalArgumentException("PLATFORM issuer must not have tenantId");
            }
            if (trustVersion < 0L) {
                throw new IllegalArgumentException("trustVersion must be non-negative");
            }
        }

        private static String positiveTenantId(String value) {
            String normalized = required(value, "tenantId");
            if (!normalized.matches("[1-9][0-9]{0,18}")) {
                throw new IllegalArgumentException("tenantId must be a positive decimal Long");
            }
            try {
                if (Long.parseLong(normalized) <= 0L) {
                    throw new IllegalArgumentException("tenantId must be positive");
                }
            } catch (NumberFormatException failure) {
                throw new IllegalArgumentException("tenantId exceeds Long range", failure);
            }
            return normalized;
        }

        private static String required(String value, String field) {
            if (value == null || value.isBlank() || !value.equals(value.trim())) {
                throw new IllegalArgumentException(field + " is required");
            }
            return value;
        }
    }
}
