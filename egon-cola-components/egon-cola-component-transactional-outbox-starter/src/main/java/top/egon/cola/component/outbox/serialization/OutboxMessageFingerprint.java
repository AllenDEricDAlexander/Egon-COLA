package top.egon.cola.component.outbox.serialization;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public final class OutboxMessageFingerprint {

    private OutboxMessageFingerprint() {
    }

    public static String sha256(
            String channel,
            String destination,
            String payload,
            String contentType,
            String schemaVersion,
            Map<String, String> headers
    ) {
        MessageDigest digest = newSha256();
        update(digest, channel);
        update(digest, destination);
        update(digest, payload);
        update(digest, contentType);
        update(digest, schemaVersion);
        Map<String, String> canonicalHeaders = headers == null ? Map.of() : new TreeMap<>(headers);
        updateLength(digest, canonicalHeaders.size());
        canonicalHeaders.forEach((name, value) -> {
            update(digest, name);
            update(digest, value);
        });
        return toHex(digest.digest());
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static void update(MessageDigest digest, String value) {
        if (value == null) {
            updateLength(digest, -1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        updateLength(digest, bytes.length);
        digest.update(bytes);
    }

    private static void updateLength(MessageDigest digest, int length) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(length).array());
    }

    private static String toHex(byte[] bytes) {
        return java.util.HexFormat.of().formatHex(bytes);
    }
}
