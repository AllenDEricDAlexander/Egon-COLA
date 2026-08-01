package top.egon.cola.platform.rbac3.admin.auth.application;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Auditable JWT public-key lifecycle. Private key material is intentionally absent.
 */
public final class JwtKeyRingService {

    private static final Set<String> PUBLIC_JWK_FIELDS = Set.of(
            "kty", "kid", "use", "alg", "n", "e", "crv", "x", "y");

    private final Map<String, KeyDescriptor> keys = new LinkedHashMap<>();
    private final Duration minimumVerificationRetention;

    public JwtKeyRingService(
            Collection<KeyDescriptor> initialKeys,
            Duration minimumVerificationRetention) {
        this.minimumVerificationRetention = Objects.requireNonNull(
                minimumVerificationRetention, "minimumVerificationRetention");
        if (minimumVerificationRetention.isNegative() || minimumVerificationRetention.isZero()) {
            throw new IllegalArgumentException("minimumVerificationRetention must be positive");
        }
        for (KeyDescriptor key : initialKeys) {
            if (keys.putIfAbsent(key.kid(), sanitize(key)) != null) {
                throw new IllegalArgumentException("duplicate kid " + key.kid());
            }
        }
        requireAtMostOneSigning();
    }

    public synchronized void publishPrepared(KeyDescriptor prepared) {
        if (prepared.state() != KeyState.PREPARED) {
            throw new IllegalArgumentException("new key must be PREPARED");
        }
        if (keys.putIfAbsent(prepared.kid(), sanitize(prepared)) != null) {
            throw new IllegalArgumentException("duplicate kid " + prepared.kid());
        }
    }

    public synchronized void promoteToSigning(String kid, Instant now) {
        KeyDescriptor target = requiredKey(kid);
        if (target.state() != KeyState.PREPARED) {
            throw new IllegalStateException("only PREPARED key can become SIGNING");
        }
        keys.replaceAll((keyId, current) -> current.state() == KeyState.SIGNING
                ? current.transition(
                        KeyState.VERIFY_ONLY,
                        current.signingSince(),
                        now.plus(minimumVerificationRetention))
                : current);
        keys.put(kid, target.transition(KeyState.SIGNING, now, null));
        requireAtMostOneSigning();
    }

    public synchronized void retire(String kid, Instant now) {
        KeyDescriptor current = requiredKey(kid);
        if (current.state() != KeyState.VERIFY_ONLY) {
            throw new IllegalStateException("only VERIFY_ONLY key can be retired");
        }
        if (current.retireNotBefore() == null || now.isBefore(current.retireNotBefore())) {
            throw new IllegalStateException("verification retention window has not elapsed");
        }
        keys.put(kid, current.transition(
                KeyState.RETIRED,
                current.signingSince(),
                current.retireNotBefore()));
    }

    public synchronized KeyDescriptor signingKey() {
        List<KeyDescriptor> signing = keys.values().stream()
                .filter(key -> key.state() == KeyState.SIGNING)
                .toList();
        if (signing.size() != 1) {
            throw new IllegalStateException("exactly one SIGNING key is required");
        }
        return signing.getFirst();
    }

    public synchronized Map<String, Object> publicJwks() {
        List<Map<String, Object>> visible = keys.values().stream()
                .filter(key -> key.state() != KeyState.RETIRED)
                .sorted(Comparator.comparing(KeyDescriptor::kid))
                .map(KeyDescriptor::publicJwk)
                .toList();
        return Map.of("keys", visible);
    }

    public synchronized List<KeyDescriptor> snapshot() {
        return List.copyOf(new ArrayList<>(keys.values()));
    }

    private KeyDescriptor requiredKey(String kid) {
        KeyDescriptor key = keys.get(kid);
        if (key == null) {
            throw new IllegalArgumentException("unknown kid " + kid);
        }
        return key;
    }

    private void requireAtMostOneSigning() {
        long count = keys.values().stream()
                .filter(key -> key.state() == KeyState.SIGNING)
                .count();
        if (count > 1) {
            throw new IllegalArgumentException("only one SIGNING key is allowed");
        }
    }

    private static KeyDescriptor sanitize(KeyDescriptor descriptor) {
        Map<String, Object> publicJwk = new LinkedHashMap<>();
        descriptor.publicJwk().forEach((name, value) -> {
            if (PUBLIC_JWK_FIELDS.contains(name)) {
                publicJwk.put(name, value);
            }
        });
        publicJwk.put("kid", descriptor.kid());
        publicJwk.put("alg", descriptor.algorithm());
        return new KeyDescriptor(
                descriptor.kid(),
                descriptor.algorithm(),
                publicJwk,
                descriptor.state(),
                descriptor.signingSince(),
                descriptor.retireNotBefore());
    }

    public record KeyDescriptor(
            String kid,
            String algorithm,
            Map<String, Object> publicJwk,
            KeyState state,
            Instant signingSince,
            Instant retireNotBefore
    ) {

        public KeyDescriptor {
            kid = required(kid, "kid");
            algorithm = required(algorithm, "algorithm");
            if (!"RS256".equals(algorithm)) {
                throw new IllegalArgumentException("only RS256 is supported");
            }
            publicJwk = Map.copyOf(Objects.requireNonNull(publicJwk, "publicJwk"));
            state = Objects.requireNonNull(state, "state");
            if (state == KeyState.SIGNING && signingSince == null) {
                throw new IllegalArgumentException("SIGNING key requires signingSince");
            }
        }

        KeyDescriptor transition(
                KeyState nextState,
                Instant nextSigningSince,
                Instant nextRetireNotBefore) {
            return new KeyDescriptor(
                    kid,
                    algorithm,
                    publicJwk,
                    nextState,
                    nextSigningSince,
                    nextRetireNotBefore);
        }
    }

    public enum KeyState {
        PREPARED,
        SIGNING,
        VERIFY_ONLY,
        RETIRED
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
