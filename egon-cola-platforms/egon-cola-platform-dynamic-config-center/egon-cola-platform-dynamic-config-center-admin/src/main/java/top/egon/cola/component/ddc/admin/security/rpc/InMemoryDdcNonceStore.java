package top.egon.cola.component.ddc.admin.security.rpc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

public final class InMemoryDdcNonceStore implements DdcNonceStore {

    private final DdcNonceCache cache;

    public InMemoryDdcNonceStore(int maximumSize) {
        cache = new DdcNonceCache(maximumSize);
    }

    @Override
    public boolean markIfAbsent(
            String credentialId,
            String nonce,
            Duration timeToLive) {
        validate(credentialId, nonce, timeToLive);
        return cache.markIfAbsent(
                digest(credentialId) + ':' + digest(nonce),
                System.currentTimeMillis(),
                timeToLive.toMillis()
        );
    }

    static String digest(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(
                            value.getBytes(StandardCharsets.UTF_8)
                    )
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 digest is unavailable",
                    exception
            );
        }
    }

    static void validate(
            String credentialId,
            String nonce,
            Duration timeToLive) {
        if (credentialId == null || credentialId.isBlank()) {
            throw new IllegalArgumentException("credentialId is required");
        }
        if (nonce == null || nonce.isBlank()) {
            throw new IllegalArgumentException("nonce is required");
        }
        if (timeToLive == null
                || timeToLive.isZero()
                || timeToLive.isNegative()) {
            throw new IllegalArgumentException("timeToLive must be positive");
        }
    }
}
