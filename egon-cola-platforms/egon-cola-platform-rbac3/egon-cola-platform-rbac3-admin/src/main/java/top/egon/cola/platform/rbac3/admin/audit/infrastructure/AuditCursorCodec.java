package top.egon.cola.platform.rbac3.admin.audit.infrastructure;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Signs audit cursors and binds them to the effective tenant and exact filter.
 */
public final class AuditCursorCodec {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] secret;

    public AuditCursorCodec(byte[] secret) {
        Objects.requireNonNull(secret, "secret");
        if (secret.length < 32) {
            throw new IllegalArgumentException("audit cursor signing key must be at least 32 bytes");
        }
        this.secret = Arrays.copyOf(secret, secret.length);
    }

    public String encode(
            CursorPosition position,
            String tenantId,
            String filterDigest) {
        Objects.requireNonNull(position, "position");
        String payload = position.createdAt().toEpochMilli() + ":" + position.id();
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        byte[] signature = sign(binding(tenantId, filterDigest, payload));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes)
                + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    public CursorPosition decode(
            String cursor,
            String tenantId,
            String filterDigest) {
        try {
            String[] parts = Objects.requireNonNull(cursor, "cursor").split("\\.", -1);
            if (parts.length != 2) {
                throw invalid();
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[0]);
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            byte[] expectedSignature = sign(binding(tenantId, filterDigest, payload));
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw invalid();
            }
            String[] values = payload.split(":", -1);
            if (values.length != 2) {
                throw invalid();
            }
            long epochMillis = Long.parseLong(values[0]);
            long id = Long.parseLong(values[1]);
            if (id < 1L) {
                throw invalid();
            }
            return new CursorPosition(Instant.ofEpochMilli(epochMillis), id);
        } catch (IllegalArgumentException | NullPointerException error) {
            if (error instanceof IllegalArgumentException illegal
                    && "audit cursor is invalid".equals(illegal.getMessage())) {
                throw illegal;
            }
            throw invalid();
        }
    }

    private byte[] binding(String tenantId, String filterDigest, String payload) {
        String bound = required(tenantId, "tenantId") + '\u001f'
                + required(filterDigest, "filterDigest") + '\u001f' + payload;
        return bound.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] sign(byte[] value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return mac.doFinal(value);
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("cannot sign audit cursor", error);
        }
    }

    private IllegalArgumentException invalid() {
        return new IllegalArgumentException("audit cursor is invalid");
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    public record CursorPosition(Instant createdAt, long id) {
        public CursorPosition {
            createdAt = Objects.requireNonNull(createdAt, "createdAt");
            if (id < 1L) {
                throw new IllegalArgumentException("id must be positive");
            }
        }
    }
}
