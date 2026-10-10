package top.egon.cola.platform.tianquan.shoubing.admin.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.api.RStream;
import org.redisson.api.StreamMessageId;
import org.redisson.client.codec.StringCodec;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamAddParams;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.egon.cola.platform.tianquan.shoubing.core.audit.TokenUsageAuditEvent;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;

/** Real codec and independent sender volumes; Redis and time remain controlled test inputs. */
class TokenUsageAuditReceiptContractTest {
    private static final String STREAM = "egon:oauth:audit:v1";
    private static final Instant NOW = Instant.parse("2026-10-10T00:00:00Z");
    @TempDir Path temporary;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final RedissonClient redis = mock(RedissonClient.class);
    @SuppressWarnings("unchecked") private final RStream<String, String> stream = mock(RStream.class);
    @SuppressWarnings("unchecked") private final RBucket<String> bucket = mock(RBucket.class);
    private final List<String> publishedIds = new ArrayList<>();
    private final AuditDeliveryProperties delivery = new AuditDeliveryProperties();
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private Path key;

    @BeforeEach
    void prepare() throws Exception {
        validator.afterPropertiesSet();
        key = temporary.resolve("audit.key");
        Files.write(key, new byte[32]);
        Files.setPosixFilePermissions(key, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        doReturn(bucket).when(redis).getBucket(anyString(), eq(StringCodec.INSTANCE));
        doReturn(stream).when(redis).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenAnswer(call -> {
            StreamAddParams<String, String> entries = call.getArgument(0);
            publishedIds.add(entries.getEntries().get("eventId"));
            return new StreamMessageId(publishedIds.size(), 1);
        });
    }

    @Test
    void missingReceiptKeepsTheSendersDurableFact() throws Exception {
        var sender = component("sender");
        sender.appendStrict(event("event-1", "https://api.example"));
        component("receiver").acknowledge("event-1");
        sender.acknowledge("event-1");
        assertThat(Files.exists(frame("sender", "event-1"))).isTrue();
    }

    @Test
    void storedReceiptClearsOnlyTheMatchingLocalFactAcrossIndependentVolumes() throws Exception {
        var sender = component("sender");
        var other = component("other");
        var fact = event("event-1", "https://api.example/one");
        sender.appendStrict(fact);
        other.appendStrict(event("event-1", "https://api.example/two"));
        when(bucket.get()).thenReturn(sender.encodeReceipt(sender.createReceipt(fact, "STORED", NOW)));
        component("receiver").acknowledge("event-1");
        assertThat(Files.exists(frame("sender", "event-1"))).isTrue();
        other.acknowledge("event-1");
        assertThat(Files.exists(frame("other", "event-1"))).isTrue();
        sender.acknowledge("event-1");
        assertThat(Files.exists(frame("sender", "event-1"))).isFalse();
        assertThat(Files.exists(frame("sender", "event-1").resolveSibling("event-1.evt.sent"))).isFalse();
    }

    @Test
    void forgedIdFingerprintSignatureSchemaAndUnknownFieldsNeverDeleteFacts() throws Exception {
        var sender = component("sender");
        var fact = event("event-1", "https://api.example");
        sender.appendStrict(fact);
        String valid = sender.encodeReceipt(sender.createReceipt(fact, "STORED", NOW));
        List<String> invalid = new ArrayList<>();
        for (String field : List.of("eventId", "eventFingerprint", "signature", "schemaVersion", "outcome", "confirmedAt")) {
            var json = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(valid);
            switch (field) {
                case "eventId" -> json.put(field, "other-event");
                case "eventFingerprint", "signature" -> json.put(field, "f".repeat(64));
                case "schemaVersion" -> json.put(field, 2);
                case "outcome" -> json.put(field, "OTHER");
                default -> json.putNull(field);
            }
            invalid.add(mapper.writeValueAsString(json));
        }
        invalid.add(valid.substring(0, valid.length() - 1) + ",\"untrusted\":true}");
        invalid.add("{invalid-json");
        invalid.add(valid.replace("\"schemaVersion\":1", "\"schemaVersion\":\"1\""));
        invalid.add(valid.replace("\"schemaVersion\":1", "\"schemaVersion\":1.0"));
        invalid.add(valid.substring(0, valid.length() - 1) + ",\"schemaVersion\":1}");
        for (String receipt : invalid) {
            when(bucket.get()).thenReturn(receipt);
            sender.acknowledge("event-1");
            assertThat(Files.exists(frame("sender", "event-1"))).as(receipt).isTrue();
        }
    }

    @Test
    void expiredReceiptClearsWithoutPretendingItWasStored() throws Exception {
        var sender = component("sender");
        var fact = event("event-1", "https://api.example");
        sender.appendStrict(fact);
        when(bucket.get()).thenReturn(sender.encodeReceipt(sender.createReceipt(fact, "EXPIRED", NOW)));
        sender.acknowledge("event-1");
        assertThat(Files.exists(frame("sender", "event-1"))).isFalse();
    }

    @Test
    void deadReceiptSurvivesRestartStopsReplayAndStillConsumesCapacity() throws Exception {
        var sender = component("sender");
        var fact = event("event-1", "https://api.example");
        sender.appendStrict(fact);
        when(bucket.get()).thenReturn(sender.encodeReceipt(sender.createReceipt(fact, "DEAD", NOW)));
        sender.acknowledge("event-1");
        Path original = frame("sender", "event-1");
        assertThat(Files.exists(original)).isTrue();
        assertThat(Files.exists(original.resolveSibling("event-1.evt.dead"))).isTrue();
        when(bucket.get()).thenReturn(null);
        assertThat(component("sender").replayPending()).isZero();
        long bytes;
        try (var files = Files.list(original.getParent())) { bytes = files.mapToLong(path -> path.toFile().length()).sum(); }
        var full = new TokenUsageAuditComponent(redis, mapper,
                new OAuthAuditProperties(STREAM, original.getParent(), bytes, key), delivery, Clock.fixed(NOW, ZoneOffset.UTC), validator);
        assertThatThrownBy(() -> full.appendStrict(event("event-2", "https://api.example")))
                .isInstanceOf(IllegalStateException.class).hasMessage("OAUTH_AUDIT_DELIVERY_UNAVAILABLE");
        assertThat(publishedIds).containsExactly("event-1");
    }

    @Test
    void expiredSentMarkersRotateByBoundedCursorAndRefreshTheirTimeAcrossRestart() throws Exception {
        delivery.setBatchSize(1);
        var sender = component("sender");
        sender.appendStrict(event("event-1", "https://api.example"));
        sender.appendStrict(event("event-2", "https://api.example"));
        for (String id : List.of("event-1", "event-2")) {
            Files.setLastModifiedTime(frame("sender", id).resolveSibling(id + ".evt.sent"), FileTime.from(NOW.minusSeconds(60)));
        }
        publishedIds.clear();
        assertThat(sender.replayPending()).isEqualTo(1);
        assertThat(sender.replayPending()).isEqualTo(1);
        assertThat(sender.replayPending()).isZero();
        assertThat(publishedIds).containsExactly("event-1", "event-2");
        assertThat(Files.getLastModifiedTime(frame("sender", "event-1").resolveSibling("event-1.evt.sent")).toInstant()).isEqualTo(NOW);
        assertThat(component("sender").replayPending()).isZero();
        Files.writeString(frame("sender", "orphan").resolveSibling("orphan.evt.sent"), "99-1");
        assertThat(sender.replayPending()).isZero();
        assertThat(sender.replayPending()).isZero();
        assertThat(Files.exists(frame("sender", "orphan").resolveSibling("orphan.evt.sent"))).isFalse();
    }

    @Test
    void failedEarlierFrameDoesNotStarveLaterUnconfirmedFrame() throws Exception {
        delivery.setBatchSize(1);
        var sender = component("sender");
        for (String id : List.of("event-1", "event-2")) {
            sender.appendStrict(event(id, "https://api.example"));
            Files.setLastModifiedTime(frame("sender", id).resolveSibling(id + ".evt.sent"), FileTime.from(NOW.minusSeconds(60)));
        }
        when(stream.add(any(StreamAddArgs.class))).thenThrow(new org.redisson.client.RedisException("unknown XADD"))
                .thenReturn(new StreamMessageId(20, 1));
        assertThatThrownBy(sender::replayPending).hasMessage("OAUTH_AUDIT_SPOOL_REPLAY_FAILED");
        assertThat(sender.replayPending()).isEqualTo(1);
        assertThat(Files.readString(frame("sender", "event-2").resolveSibling("event-2.evt.sent"))).isEqualTo("20-1");
        assertThat(Files.exists(frame("sender", "event-1"))).isTrue();
    }

    @Test
    void configurationAndReceiptAnnotationsRejectInvalidBoundaries() {
        assertThat(validator.validate(new AuditDeliveryProperties())).isEmpty();
        assertThat(validator.validate(new AuditDeliveryProperties().setReceiptTtl(Duration.ofSeconds(59)))).isNotEmpty();
        assertThat(validator.validate(new AuditDeliveryProperties().setConfirmationTimeout(Duration.ofSeconds(301)))).isNotEmpty();
        assertThat(validator.validate(new AuditDeliveryProperties().setBatchSize(0))).isNotEmpty();
    }

    @Test
    void malformedDeadMarkerNeverSuppressesOrDeletesItsDurableFact() throws Exception {
        var sender = component("sender");
        sender.appendStrict(event("event-1", "https://api.example"));
        Files.writeString(frame("sender", "event-1").resolveSibling("event-1.evt.dead"), "{}");
        assertThatThrownBy(sender::replayPending).hasMessage("OAUTH_AUDIT_SPOOL_REPLAY_FAILED");
        assertThat(Files.exists(frame("sender", "event-1"))).isTrue();
    }

    @Test
    void canonicalFingerprintAndDomainSeparatedMacMatchIndependentGoldenValues() throws Exception {
        var sender = component("sender");
        var fact = event("event-1", "https://api.example");
        var receipt = sender.createReceipt(fact, "STORED", NOW);
        assertThat(sender.fingerprint(fact)).isEqualTo("40c7cad80bbbccf534b3a3bcbb9a1d9ed3aac790497609310cbb5cb8c27b96e8");
        assertThat(receipt.getSignature()).isEqualTo("c79afcc86cac44d670da81af9f1b0fbc573d3e1486728b45c85df31632352d03");
        var json = mapper.readTree(sender.encodeReceipt(receipt));
        assertThat(json.size()).isEqualTo(6);
        assertThat(json.path("confirmedAt").asText()).isEqualTo(NOW.toString());
        assertThat(validator.validate(receipt)).isEmpty();
    }

    private TokenUsageAuditComponent component(String directory) {
        return new TokenUsageAuditComponent(redis, mapper, new OAuthAuditProperties(STREAM, temporary.resolve(directory),
                1_048_576, key), delivery, Clock.fixed(NOW, ZoneOffset.UTC), validator);
    }

    private Path frame(String directory, String id) { return temporary.resolve(directory).resolve(id + ".evt"); }

    private static TokenUsageAuditEvent event(String id, String resource) {
        return new TokenUsageAuditEvent(id, 1, URI.create("https://idp.example/tenants/1001"), resource,
                null, null, null, "SUCCESS", "TOKEN_USE_VERIFIED", NOW, null);
    }
}
