package top.egon.cola.platform.tianquan.shoubing.starter.audit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
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
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HexFormat;
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
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.shoubing.core.audit.TokenUsageAuditEvent;
import top.egon.cola.platform.tianquan.shoubing.core.observability.IdpSecurityMetricsRecorder;
import top.egon.cola.platform.tianquan.shoubing.core.observability.SecurityMetricObservationBO;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.domain.dto.TokenUsageAuditReceiptDTO;

/** Durable, secret-free audit handoff for resource servers that do not own a database. */
@RequiredArgsConstructor
@Slf4j
@Validated
public class TokenUsageAuditComponent {

    private static final int MAGIC = 0x4f415531;
    private static final int MAC_BYTES = 32;
    private static final int MAX_EVENT_BYTES = 16_384;

    @Qualifier("idpAuditRedissonClient")
    private final RedissonClient redisson;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("oauthAuditProperties")
    private final OAuthAuditProperties properties;
    @Qualifier("auditDeliveryProperties")
    private final AuditDeliveryProperties delivery;
    @Qualifier("idpClock")
    private final Clock clock;
    @Qualifier("defaultValidator")
    private final Validator validator;
    @Qualifier("idpSecurityMetricsRecorder")
    private final IdpSecurityMetricsRecorder metrics;
    private final java.util.concurrent.atomic.AtomicBoolean metricFailureLogged = new java.util.concurrent.atomic.AtomicBoolean();
    private String replayCursor = "";
    private final Object spoolLock = new Object();

    /** Source-compatible legacy factory adapter; managed hosts use the generated full constructor. */
    public TokenUsageAuditComponent(RedissonClient redisson, ObjectMapper mapper, OAuthAuditProperties properties) {
        this(redisson, mapper, properties, new AuditDeliveryProperties(), Clock.systemUTC(), legacyValidator());
    }

    /** Preserves the previous configured constructor and its qualified parameter metadata. */
    public TokenUsageAuditComponent(@Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            @Qualifier("jacksonObjectMapper") ObjectMapper mapper,
            @Qualifier("oauthAuditProperties") OAuthAuditProperties properties,
            @Qualifier("auditDeliveryProperties") AuditDeliveryProperties delivery,
            @Qualifier("idpClock") Clock clock, @Qualifier("defaultValidator") Validator validator) {
        this(redisson, mapper, properties, delivery, clock, validator, IdpSecurityMetricsRecorder.NOOP);
    }

    private static volatile Validator compatibilityValidator;

    private static synchronized Validator legacyValidator() {
        if (compatibilityValidator == null) {
            var bean = new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean();
            bean.afterPropertiesSet();
            compatibilityValidator = bean;
        }
        return compatibilityValidator;
    }

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
        TokenUsageAuditEvent value = new ValidationUtils(validator)
                .validate(Objects.requireNonNull(event, "event"));
        byte[] payload = serialize(value);
        try {
            stage(value.eventId(), payload, true);
            observeDelivery("STAGED");
        } catch (IOException failure) {
            observeDelivery("FAILED");
            throw new IllegalStateException("OAUTH_AUDIT_DELIVERY_UNAVAILABLE", failure);
        }
        StreamMessageId messageId;
        try {
            messageId = publish(value);
        } catch (RuntimeException failure) {
            observeDelivery("FAILED");
            throw new IllegalStateException("OAUTH_AUDIT_STREAM_UNAVAILABLE", failure);
        }
        try {
            markSent(eventPath(value.eventId()), messageId, true);
            observeDelivery("SENT");
        } catch (IOException failure) {
            observeDelivery("FAILED");
            throw new IllegalStateException("OAUTH_AUDIT_SPOOL_MARK_FAILED", failure);
        }
        return messageId;
    }

    public StreamMessageId append(TokenUsageAuditEvent event) {
        TokenUsageAuditEvent value = new ValidationUtils(validator)
                .validate(Objects.requireNonNull(event, "event"));
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

    public String reference(@jakarta.validation.constraints.NotNull java.net.URI issuer,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Pattern(regexp = "[1-9][0-9]{0,18}") String tokenId) {
        if (issuer == null || !issuer.isAbsolute() || !"https".equalsIgnoreCase(issuer.getScheme())
                || issuer.getHost() == null || issuer.getUserInfo() != null || issuer.getQuery() != null || issuer.getFragment() != null
                || tokenId == null || !tokenId.matches("[1-9][0-9]{0,18}")) {
            throw new IllegalArgumentException("OAUTH_AUDIT_REFERENCE_INVALID");
        }
        try {
            return HexFormat.of().formatHex(mac((issuer.toASCIIString() + "\n" + tokenId).getBytes(StandardCharsets.UTF_8)));
        } catch (IOException failure) {
            throw new IllegalStateException("OAUTH_AUDIT_HMAC_UNAVAILABLE", failure);
        }
    }

    public TokenUsageAuditReceiptDTO createReceipt(@Valid TokenUsageAuditEvent event, String outcome, Instant confirmedAt) {
        new ValidationUtils(validator).validate(event);
        if (outcome == null || !Set.of("STORED", "EXPIRED", "DEAD").contains(outcome)) {
            throw new IllegalArgumentException("OAUTH_AUDIT_RECEIPT_OUTCOME_INVALID");
        }
        TokenUsageAuditReceiptDTO receipt = TokenUsageAuditReceiptDTO.builder().schemaVersion(1).eventId(event.eventId())
                .eventFingerprint(fingerprint(event)).outcome(outcome).confirmedAt(Objects.requireNonNull(confirmedAt)).build();
        receipt.setSignature(receiptSignature(receipt));
        return new ValidationUtils(validator).validate(receipt);
    }

    public String encodeReceipt(@Valid TokenUsageAuditReceiptDTO receipt) {
        new ValidationUtils(validator).validate(receipt);
        try {
            return objectMapper.writeValueAsString(receipt);
        } catch (IOException failure) {
            throw new IllegalStateException("OAUTH_AUDIT_RECEIPT_INVALID", failure);
        }
    }

    public String fingerprint(@Valid TokenUsageAuditEvent event) {
        new ValidationUtils(validator).validate(event);
        try {
            byte[] bytes = canonical(event.eventId(), Integer.toString(event.schemaVersion()), event.issuer().toASCIIString(),
                    event.resource(), event.subjectRef(), event.clientRef(), event.tokenRef(), event.result(), event.reasonCode(),
                    event.occurredAt().toString(), event.traceId());
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (IOException | java.security.NoSuchAlgorithmException failure) {
            throw new IllegalStateException("OAUTH_AUDIT_FINGERPRINT_UNAVAILABLE", failure);
        }
    }

    private static byte[] canonical(String... fields) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            for (String field : fields) {
                if (field == null) { output.writeInt(-1); continue; }
                byte[] value = field.getBytes(StandardCharsets.UTF_8);
                output.writeInt(value.length);
                output.write(value);
            }
        }
        return bytes.toByteArray();
    }

    private String receiptSignature(TokenUsageAuditReceiptDTO receipt) {
        try {
            byte[] fields = canonical(Integer.toString(receipt.getSchemaVersion()), receipt.getEventId(), receipt.getEventFingerprint(),
                    receipt.getOutcome(), receipt.getConfirmedAt().toString());
            byte[] domain = "audit-receipt-v1".getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bytes.write(domain);
            bytes.write(fields);
            return HexFormat.of().formatHex(mac(bytes.toByteArray()));
        } catch (IOException failure) { throw new IllegalStateException("OAUTH_AUDIT_HMAC_UNAVAILABLE", failure); }
    }

    private boolean receiptMatches(TokenUsageAuditReceiptDTO receipt, TokenUsageAuditEvent event) {
        if (receipt == null || receipt.getSchemaVersion() != 1 || receipt.getConfirmedAt() == null
                || !event.eventId().equals(receipt.getEventId()) || !fingerprint(event).equals(receipt.getEventFingerprint())
                || receipt.getOutcome() == null || !Set.of("STORED", "EXPIRED", "DEAD").contains(receipt.getOutcome())
                || receipt.getSignature() == null || !receipt.getSignature().matches("[0-9a-f]{64}")) { return false; }
        return MessageDigest.isEqual(HexFormat.of().parseHex(receiptSignature(receipt)),
                HexFormat.of().parseHex(receipt.getSignature()));
    }

    public int replayPending() {
        Path directory = properties.spoolDirectory();
        if (directory == null || !Files.isDirectory(directory)) { return 0; }
        synchronized (spoolLock) {
            new ValidationUtils(validator).validate(delivery);
            try {
                List<Path> pending = replayBatch(directory, replayCursor);
                if (pending.isEmpty()) { pending = replayBatch(directory, ""); }
                int delivered = 0;
                RuntimeException failed = null;
                for (Path file : pending) {
                    replayCursor = file.getFileName().toString();
                    try {
                        if (!file.getFileName().toString().endsWith(".evt")) {
                            Path frame = file.resolveSibling(file.getFileName().toString().replaceFirst("\\.sent$", ""));
                            if (!Files.exists(frame)) { Files.deleteIfExists(file); forceDirectory(); }
                            continue;
                        }
                        TokenUsageAuditEvent event = read(file);
                        acknowledge(event.eventId());
                        if (!Files.exists(file)) { continue; }
                        Path dead = file.resolveSibling(file.getFileName() + ".dead");
                        if (Files.exists(dead)) {
                            TokenUsageAuditReceiptDTO receipt = decodeReceipt(Files.readString(dead, StandardCharsets.UTF_8));
                            if (!receiptMatches(receipt, event) || !"DEAD".equals(receipt.getOutcome())) {
                                throw new IOException("OAUTH_AUDIT_DEAD_MARKER_INVALID");
                            }
                            continue;
                        }
                        Path sent = sentPath(file);
                        if (Files.exists(sent) && Files.getLastModifiedTime(sent).toInstant()
                                .plus(delivery.getConfirmationTimeout()).isAfter(clock.instant())) { continue; }
                        markSent(file, publish(event), true);
                        observeDelivery("SENT");
                        delivered++;
                    } catch (IOException | RuntimeException failure) {
                        observeDelivery("FAILED");
                        log.error("OAuth audit spool replay failed; failureType={}", failure.getClass().getSimpleName());
                        if (failed == null) { failed = new IllegalStateException("OAUTH_AUDIT_SPOOL_REPLAY_FAILED", failure); }
                        else { failed.addSuppressed(failure); }
                    }
                }
                observeSpool();
                if (failed != null) { throw failed; }
                return delivered;
            } catch (IOException failure) {
                throw new IllegalStateException("OAUTH_AUDIT_SPOOL_REPLAY_FAILED", failure);
            }
        }
    }

    /** Bounded memory and a rotating key cursor keep old unconfirmed frames from starving later ones. */
    private List<Path> replayBatch(Path directory, String after) throws IOException {
        Comparator<Path> order = Comparator.comparing(path -> path.getFileName().toString());
        java.util.PriorityQueue<Path> selected = new java.util.PriorityQueue<>(order.reversed());
        try (var files = Files.list(directory)) {
            var iterator = files.iterator();
            while (iterator.hasNext()) {
                Path path = iterator.next();
                String name = path.getFileName().toString();
                if ((!name.endsWith(".evt") && !name.endsWith(".evt.sent")) || name.compareTo(after) <= 0) { continue; }
                if (name.endsWith(".evt.sent")
                        && Files.exists(path.resolveSibling(name.substring(0, name.length() - 5)))) { continue; }
                selected.offer(path);
                if (selected.size() > delivery.getBatchSize()) { selected.poll(); }
            }
        }
        return selected.stream().sorted(order).toList();
    }

    private TokenUsageAuditReceiptDTO decodeReceipt(String value) throws IOException {
        if (value.length() > MAX_EVENT_BYTES) { throw new IOException("OAUTH_AUDIT_RECEIPT_SIZE_INVALID"); }
        var tree = objectMapper.readTree(value);
        if (!tree.isObject() || tree.size() != 6 || !tree.path("schemaVersion").isIntegralNumber()
                || !tree.path("eventId").isTextual() || !tree.path("eventFingerprint").isTextual()
                || !tree.path("outcome").isTextual() || !tree.path("confirmedAt").isTextual()
                || !tree.path("signature").isTextual()) {
            throw new IllegalArgumentException("OAUTH_AUDIT_RECEIPT_SCHEMA_INVALID");
        }
        TokenUsageAuditReceiptDTO receipt = objectMapper.readerFor(TokenUsageAuditReceiptDTO.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .with(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readValue(value);
        return new ValidationUtils(validator).validate(receipt);
    }

    public void acknowledge(String eventId) {
        if (eventId == null || !eventId.matches("[A-Za-z0-9._~-]{1,64}")) { throw new IllegalArgumentException("OAUTH_AUDIT_EVENT_ID_INVALID"); }
        synchronized (spoolLock) {
            Path frame = eventPath(eventId);
            if (!Files.exists(frame)) { return; }
            try {
                String value = redisson.<String>getBucket(properties.stream() + ":receipt:" + eventId, StringCodec.INSTANCE).get();
                if (value == null) { return; }
                TokenUsageAuditReceiptDTO receipt = decodeReceipt(value);
                TokenUsageAuditEvent event = read(frame);
                if (!receiptMatches(receipt, event)) { return; }
                if ("DEAD".equals(receipt.getOutcome())) {
                    Path marker = frame.resolveSibling(frame.getFileName() + ".dead");
                    if (Files.exists(marker) && Files.readString(marker, StandardCharsets.UTF_8).equals(value)) { return; }
                    writeMarker(marker, value.getBytes(StandardCharsets.UTF_8), true);
                    observeReceipt(receipt, "DEAD");
                    return;
                }
                Files.deleteIfExists(frame);
                Files.deleteIfExists(sentPath(frame));
                forceDirectory();
                observeReceipt(receipt, "EXPIRED".equals(receipt.getOutcome()) ? "EXPIRED" : "CONFIRMED");
            } catch (com.fasterxml.jackson.core.JsonProcessingException | ConstraintViolationException | IllegalArgumentException invalid) {
                log.warn("OAuth receipt rejected; eventId={} failureType={}",eventId, invalid.getClass().getSimpleName());
            } catch (IOException failure) { throw new IllegalStateException("OAUTH_AUDIT_SPOOL_ACK_FAILED", failure); }
        }
    }

    private void observeReceipt(TokenUsageAuditReceiptDTO receipt, String outcome) {
        Duration age = Duration.between(receipt.getConfirmedAt(), clock.instant());
        observeMetric(SecurityMetricObservationBO.builder().signal("receipt.age").result("CONFIRMED")
                .duration(age.isNegative() ? Duration.ZERO : age).build());
        observeDelivery(outcome);
    }

    private void observeDelivery(String result) {
        observeMetric(SecurityMetricObservationBO.builder().signal("oauth.audit.delivery").result(result).build());
        observeSpool();
    }

    private void observeSpool() {
        if (metrics == IdpSecurityMetricsRecorder.NOOP) { return; }
        try {
            long size = Files.isDirectory(properties.spoolDirectory()) ? spoolBytes() : 0;
            observeMetric(SecurityMetricObservationBO.builder().signal("spool.bytes").result("CURRENT").value(size).build());
        } catch (IOException | RuntimeException failure) {
            if (metricFailureLogged.compareAndSet(false, true)) {
                log.warn("Audit spool observation unavailable; failureType={}", failure.getClass().getSimpleName());
            }
        }
    }

    private void observeMetric(SecurityMetricObservationBO sample) {
        if (metrics == IdpSecurityMetricsRecorder.NOOP) { return; }
        try { metrics.record(sample); }
        catch (RuntimeException failure) {
            if (metricFailureLogged.compareAndSet(false, true)) {
                log.warn("Audit observation unavailable; failureType={}", failure.getClass().getSimpleName());
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
            byte[] provided = HexFormat.of().parseHex(signature);
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
        return new ValidationUtils(validator)
                .validate(objectMapper.readValue(payload, TokenUsageAuditEvent.class));
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
            fields.put("signature", HexFormat.of().formatHex(mac(
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
            return files.filter(Files::isRegularFile)
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
        writeMarker(sentPath(eventPath), messageId.toString().getBytes(StandardCharsets.US_ASCII), strict);
    }

    private void writeMarker(Path marker, byte[] bytes, boolean strict) throws IOException {
        long existing = Files.exists(marker) ? Files.size(marker) : 0;
        if (spoolBytes() - existing + bytes.length > properties.spoolMaxBytes()) {
            throw new IOException("OAUTH_AUDIT_SPOOL_FULL");
        }
        Path temporary = Files.createTempFile(properties.spoolDirectory(), ".audit-marker-", ".tmp");
        try {
            setOwnerOnly(temporary, false);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) { channel.write(buffer); }
                Files.setLastModifiedTime(temporary, java.nio.file.attribute.FileTime.from(clock.instant()));
                channel.force(true);
            }
            try { atomicMove(temporary, marker); }
            catch (AtomicMoveNotSupportedException unsupported) {
                if (strict) { throw unsupported; }
                Files.move(temporary, marker, StandardCopyOption.REPLACE_EXISTING);
            }
            if (strict) { forceDirectory(); }
        } finally { Files.deleteIfExists(temporary); }
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
