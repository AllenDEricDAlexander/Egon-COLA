package top.egon.cola.platform.tianquan.shoubing.starter.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.CRC32C;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import top.egon.cola.platform.tianquan.shoubing.core.audit.TokenUsageAuditEvent;

/** Durable, secret-free audit handoff for resource servers that do not own a database. */
@RequiredArgsConstructor
@Slf4j
@Validated
public class TokenUsageAuditComponent {

    private static final int MAGIC = 0x4f415531;
    private static final int MAC_BYTES = 32;
    private static final int MAX_EVENT_BYTES = 16_384;

    @Qualifier("rbac3RuntimeRedissonClient")
    private final RedissonClient redisson;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("oauthAuditProperties")
    private final OAuthAuditProperties properties;
    private final Object spoolLock = new Object();
    private volatile byte[] hmacKey;

    @PostConstruct
    void prepareDurableStore() {
        synchronized (spoolLock) {
            try {
                prepareSpool();
            } catch (IOException failure) {
                throw new IllegalStateException("OAUTH_AUDIT_DURABLE_STORE_INVALID", failure);
            }
        }
    }

    /** Strict handoff requires both durable media before returning to a protected caller. */
    public StreamMessageId appendStrict(@Valid TokenUsageAuditEvent event) {
        TokenUsageAuditEvent value = Objects.requireNonNull(event, "event");
        byte[] payload = serialize(value);
        try {
            stage(value.eventId(), payload, true);
        } catch (IOException failure) {
            throw new IllegalStateException("OAUTH_AUDIT_DELIVERY_UNAVAILABLE", failure);
        }
        StreamMessageId messageId;
        try {
            messageId = publish(value);
        } catch (RuntimeException failure) {
            throw new IllegalStateException("OAUTH_AUDIT_STREAM_UNAVAILABLE", failure);
        }
        try {
            markSent(eventPath(value.eventId()), messageId, true);
        } catch (IOException failure) {
            throw new IllegalStateException("OAUTH_AUDIT_SPOOL_MARK_FAILED", failure);
        }
        return messageId;
    }

    public StreamMessageId append(TokenUsageAuditEvent event) {
        TokenUsageAuditEvent value = Objects.requireNonNull(event, "event");
        byte[] payload = serialize(value);
        try {
            stage(value.eventId(), payload);
        } catch (IOException spoolFailure) {
            try {
                return publish(value);
            } catch (RuntimeException streamFailure) {
                spoolFailure.addSuppressed(streamFailure);
                log.error("OAuth audit spool and stream are unavailable; eventId={}", value.eventId());
                throw new IllegalStateException("OAUTH_AUDIT_DELIVERY_UNAVAILABLE", spoolFailure);
            }
        }
        try {
            StreamMessageId messageId = publish(value);
            try {
                markSent(eventPath(value.eventId()), messageId);
            } catch (IOException failure) {
                throw new IllegalStateException("OAUTH_AUDIT_SPOOL_MARK_FAILED", failure);
            }
            return messageId;
        } catch (RuntimeException unavailable) {
            log.warn("OAuth audit stream is unavailable; eventId={} remains in the durable spool",
                    value.eventId());
            throw new IllegalStateException("OAUTH_AUDIT_STREAM_UNAVAILABLE", unavailable);
        }
    }

    public int replayPending() {
        Path directory = properties.spoolDirectory();
        if (directory == null || !Files.isDirectory(directory)) {
            return 0;
        }
        synchronized (spoolLock) {
            try (var files = Files.list(directory)) {
                List<Path> pending = files.filter(path -> path.getFileName().toString().endsWith(".evt"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList();
                int delivered = 0;
                for (Path file : pending) {
                    if (Files.exists(sentPath(file))) {
                        continue;
                    }
                    TokenUsageAuditEvent event = read(file);
                    StreamMessageId messageId = publish(event);
                    markSent(file, messageId);
                    delivered++;
                }
                return delivered;
            } catch (IOException | RuntimeException failure) {
                log.error("OAuth audit spool replay failed; failureType={}", failure.getClass().getSimpleName());
                throw new IllegalStateException("OAUTH_AUDIT_SPOOL_REPLAY_FAILED", failure);
            }
        }
    }

    public void acknowledge(String eventId) {
        if (eventId == null || !eventId.matches("[A-Za-z0-9._~-]{1,128}")) {
            throw new IllegalArgumentException("OAUTH_AUDIT_EVENT_ID_INVALID");
        }
        synchronized (spoolLock) {
            try {
                Files.deleteIfExists(eventPath(eventId));
                Files.deleteIfExists(sentPath(eventPath(eventId)));
            } catch (IOException failure) {
                throw new IllegalStateException("OAUTH_AUDIT_SPOOL_ACK_FAILED", failure);
            }
        }
    }

    public boolean verifySignature(String issuer, String tokenRef, String signature) {
        if (issuer == null || signature == null || !signature.matches("[0-9a-f]{64}")) {
            return false;
        }
        try {
            byte[] expected = mac((issuer + "\n" + Objects.toString(tokenRef, ""))
                    .getBytes(StandardCharsets.UTF_8));
            byte[] provided = java.util.HexFormat.of().parseHex(signature);
            return java.security.MessageDigest.isEqual(expected, provided);
        } catch (IOException failure) {
            throw new IllegalStateException("OAUTH_AUDIT_HMAC_UNAVAILABLE", failure);
        }
    }

    @Scheduled(initialDelayString = "${egon.cola.platform.tianquan.shoubing.audit.replay-initial-delay:5000}",
            fixedDelayString = "${egon.cola.platform.tianquan.shoubing.audit.replay-delay:5000}")
    public void replayPendingScheduled() {
        replayPending();
    }

    private void stage(String eventId, byte[] payload) throws IOException {
        stage(eventId, payload, false);
    }

    private void stage(String eventId, byte[] payload, boolean strict) throws IOException {
        synchronized (spoolLock) {
            prepareSpool();
            Path target = eventPath(eventId);
            if (Files.exists(target)) {
                TokenUsageAuditEvent existing = read(target);
                if (!existing.eventId().equals(eventId)
                        || !java.util.Arrays.equals(payload, serialize(existing))) {
                    throw new IOException("OAUTH_AUDIT_SPOOL_EVENT_CONFLICT");
                }
                if (strict) { forceFile(target); forceDirectory(); }
                return;
            }
            byte[] frame = frame(payload);
            if (spoolBytes() + frame.length > properties.spoolMaxBytes()) {
                throw new IOException("OAUTH_AUDIT_SPOOL_FULL");
            }
            Path temporary = Files.createTempFile(properties.spoolDirectory(), ".audit-", ".tmp");
            setOwnerOnly(temporary, false);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(frame);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            try {
                atomicMove(temporary, target);
            } catch (FileAlreadyExistsException duplicate) {
                Files.deleteIfExists(temporary);
                TokenUsageAuditEvent existing = read(target);
                if (!java.util.Arrays.equals(payload, serialize(existing))) {
                    throw new IOException("OAUTH_AUDIT_SPOOL_EVENT_CONFLICT", duplicate);
                }
            } catch (AtomicMoveNotSupportedException unsupported) {
                if (strict) { throw unsupported; }
                Files.move(temporary, target);
            } finally {
                Files.deleteIfExists(temporary);
            }
            if (strict) { forceDirectory(); }
        }
    }

    private TokenUsageAuditEvent read(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length < Integer.BYTES * 3 + MAC_BYTES || bytes.length > MAX_EVENT_BYTES + 128) {
            throw new IOException("OAUTH_AUDIT_SPOOL_FRAME_INVALID");
        }
        ByteBuffer frame = ByteBuffer.wrap(bytes);
        if (frame.getInt() != MAGIC) {
            throw new IOException("OAUTH_AUDIT_SPOOL_FRAME_INVALID");
        }
        int length = frame.getInt();
        int crcValue = frame.getInt();
        if (length < 1 || length > MAX_EVENT_BYTES || frame.remaining() != length + MAC_BYTES) {
            throw new IOException("OAUTH_AUDIT_SPOOL_FRAME_INVALID");
        }
        byte[] payload = new byte[length];
        frame.get(payload);
        byte[] storedMac = new byte[MAC_BYTES];
        frame.get(storedMac);
        CRC32C crc = new CRC32C();
        crc.update(payload);
        if ((int) crc.getValue() != crcValue || !java.security.MessageDigest.isEqual(storedMac, mac(payload))) {
            throw new IOException("OAUTH_AUDIT_SPOOL_FRAME_AUTHENTICATION_FAILED");
        }
        return objectMapper.readValue(payload, TokenUsageAuditEvent.class);
    }

    private byte[] frame(byte[] payload) throws IOException {
        CRC32C crc = new CRC32C();
        crc.update(payload);
        ByteBuffer frame = ByteBuffer.allocate(Integer.BYTES * 3 + payload.length + MAC_BYTES);
        frame.putInt(MAGIC).putInt(payload.length).putInt((int) crc.getValue()).put(payload).put(mac(payload));
        return frame.array();
    }

    private byte[] serialize(TokenUsageAuditEvent event) {
        try {
            byte[] value = objectMapper.writeValueAsBytes(event);
            if (value.length < 1 || value.length > MAX_EVENT_BYTES) {
                throw new IllegalArgumentException("OAUTH_AUDIT_EVENT_SIZE_INVALID");
            }
            return value;
        } catch (IOException failure) {
            throw new IllegalStateException("OAUTH_AUDIT_EVENT_SERIALIZATION_FAILED", failure);
        }
    }

    private StreamMessageId publish(TokenUsageAuditEvent event) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("eventId", event.eventId());
        fields.put("schemaVersion", Integer.toString(event.schemaVersion()));
        fields.put("issuer", event.issuer().toASCIIString());
        fields.put("resource", event.resource());
        if (event.subjectRef() != null) {
            fields.put("subjectRef", event.subjectRef());
        }
        if (event.clientRef() != null) {
            fields.put("clientRef", event.clientRef());
        }
        if (event.tokenRef() != null) {
            fields.put("tokenRef", event.tokenRef());
        }
        fields.put("result", event.result());
        fields.put("reasonCode", event.reasonCode());
        fields.put("eventType", event.reasonCode());
        fields.put("occurredAt", event.occurredAt().toString());
        if (event.traceId() != null) {
            fields.put("traceId", event.traceId());
        }
        fields.put("auditKind", "OAUTH");
        try {
            fields.put("signature", java.util.HexFormat.of().formatHex(mac(
                    (event.issuer().toASCIIString() + "\n" + Objects.toString(event.tokenRef(), ""))
                            .getBytes(StandardCharsets.UTF_8))));
        } catch (IOException failure) {
            throw new IllegalStateException("OAUTH_AUDIT_HMAC_UNAVAILABLE", failure);
        }
        StreamMessageId result = redisson.<String, String>getStream(properties.stream(), StringCodec.INSTANCE)
                .add(StreamAddArgs.entries(fields));
        if (result == null) {
            throw new IllegalStateException("OAUTH_AUDIT_STREAM_NOT_CONFIRMED");
        }
        return result;
    }

    private void prepareSpool() throws IOException {
        Path directory = Objects.requireNonNull(properties.spoolDirectory(), "spoolDirectory");
        if (!directory.isAbsolute()) {
            throw new IOException("OAUTH_AUDIT_SPOOL_DIRECTORY_NOT_ABSOLUTE");
        }
        Files.createDirectories(directory);
        setOwnerOnly(directory, true);
        if (hmacKey == null) {
            hmacKey = readKey();
        }
    }

    private byte[] readKey() throws IOException {
        Path keyFile = Objects.requireNonNull(properties.hmacKeyFile(), "hmacKeyFile");
        if (!keyFile.isAbsolute() || !Files.isRegularFile(keyFile)) {
            throw new IOException("OAUTH_AUDIT_HMAC_KEY_FILE_INVALID");
        }
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(keyFile);
        if (permissions.contains(PosixFilePermission.GROUP_READ)
                || permissions.contains(PosixFilePermission.GROUP_WRITE)
                || permissions.contains(PosixFilePermission.GROUP_EXECUTE)
                || permissions.contains(PosixFilePermission.OTHERS_READ)
                || permissions.contains(PosixFilePermission.OTHERS_WRITE)
                || permissions.contains(PosixFilePermission.OTHERS_EXECUTE)) {
            throw new IOException("OAUTH_AUDIT_HMAC_KEY_FILE_PERMISSIONS_INVALID");
        }
        byte[] key = Files.readAllBytes(keyFile);
        if (key.length < 32 || key.length > 4096) {
            throw new IOException("OAUTH_AUDIT_HMAC_KEY_LENGTH_INVALID");
        }
        return key;
    }

    private byte[] mac(byte[] payload) throws IOException {
        if (hmacKey == null) {
            hmacKey = readKey();
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return mac.doFinal(payload);
        } catch (GeneralSecurityException failure) {
            throw new IOException("OAUTH_AUDIT_HMAC_UNAVAILABLE", failure);
        }
    }

    private long spoolBytes() throws IOException {
        try (var files = Files.list(properties.spoolDirectory())) {
            return files.filter(path -> path.getFileName().toString().endsWith(".evt"))
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException failure) {
                            throw new java.io.UncheckedIOException(failure);
                        }
                    }).sum();
        } catch (java.io.UncheckedIOException failure) {
            throw failure.getCause();
        }
    }

    private Path eventPath(String eventId) {
        return properties.spoolDirectory().resolve(eventId + ".evt");
    }

    private Path sentPath(Path eventPath) {
        return eventPath.resolveSibling(eventPath.getFileName() + ".sent");
    }

    private void markSent(Path eventPath, StreamMessageId messageId) throws IOException {
        markSent(eventPath, messageId, false);
    }

    private void markSent(Path eventPath, StreamMessageId messageId, boolean strict) throws IOException {
        Path marker = sentPath(eventPath);
        if (Files.exists(marker)) {
            if (strict) { forceFile(marker); forceDirectory(); }
            return;
        }
        Path temporary = Files.createTempFile(properties.spoolDirectory(), ".audit-sent-", ".tmp");
        setOwnerOnly(temporary, false);
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer bytes = StandardCharsets.US_ASCII.encode(messageId.toString());
            while (bytes.hasRemaining()) {
                channel.write(bytes);
            }
            channel.force(true);
        }
        try {
            atomicMove(temporary, marker);
        } catch (FileAlreadyExistsException duplicate) {
            Files.deleteIfExists(temporary);
        } catch (AtomicMoveNotSupportedException unsupported) {
            if (strict) { throw unsupported; }
            Files.move(temporary, marker);
        } finally {
            Files.deleteIfExists(temporary);
        }
        if (strict) { forceDirectory(); }
    }

    void atomicMove(Path source, Path target) throws IOException {
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    }

    void forceDirectory() throws IOException {
        try (FileChannel directory = FileChannel.open(properties.spoolDirectory(), StandardOpenOption.READ)) {
            directory.force(true);
        }
    }

    private void forceFile(Path path) throws IOException {
        try (FileChannel file = FileChannel.open(path, StandardOpenOption.WRITE)) {
            file.force(true);
        }
    }

    private static void setOwnerOnly(Path path, boolean directory) throws IOException {
        try {
            Set<PosixFilePermission> permissions = directory
                    ? EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE)
                    : EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException unsupported) {
            throw new IOException("OAUTH_AUDIT_SPOOL_REQUIRES_POSIX_PERMISSIONS", unsupported);
        }
    }
}
