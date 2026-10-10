package top.egon.cola.platform.tianquan.shoubing.admin.audit.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.redisson.api.AutoClaimResult;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;

class OAuthAuditConsumerTest {

    private static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    private static final String STREAM = "egon:oauth:audit:v1";
    private static final TrustedRealmBO PLATFORM = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example/platform"))
            .realmKey("platform")
            .realmType(RealmTypeEnum.PLATFORM)
            .storagePartitionId(9000L)
            .trustVersion(0L)
            .build();

    private final RedissonClient redisson = mock(RedissonClient.class);
    @SuppressWarnings("unchecked")
    private final RStream<String, String> stream = mock(RStream.class);
    private final OAuthAuditRepository audits = mock(OAuthAuditRepository.class);
    private final TenantIssuerService issuers = mock(TenantIssuerService.class);
    private final TokenUsageAuditComponent durableAudit = mock(TokenUsageAuditComponent.class);
    private final RAtomicLong attempts = mock(RAtomicLong.class);
    private OAuthAuditConsumer consumer;

    @BeforeEach
    void setUp() {
        doReturn(stream).when(redisson).getStream(eq(STREAM), eq(StringCodec.INSTANCE));
        when(redisson.getAtomicLong(anyString())).thenReturn(attempts);
        when(issuers.resolveExactIssuer(PLATFORM.getIssuer())).thenReturn(PLATFORM);
        OAuthAuditProperties properties = new OAuthAuditProperties(STREAM,
                Path.of("/tmp/egon-audit-spool"), 1024L, Path.of("/tmp/egon-audit-hmac"));
        consumer = new OAuthAuditConsumer(redisson, audits, issuers, durableAudit, properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void commitsIdentityAuditBeforeAcknowledgingItsStreamMessage() {
        StreamMessageId id = new StreamMessageId(1L, 0L);
        when(stream.readGroup(eq("oauth-audit-persist-v1"), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(Map.of(id, identityFields("identity-event-1")));
        when(audits.save(eq(PLATFORM), any(OAuthAuditBO.class), eq(0L)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        assertThat(consumer.consumeBatch()).isEqualTo(1);

        var audit = org.mockito.ArgumentCaptor.forClass(OAuthAuditBO.class);
        verify(audits).save(eq(PLATFORM), audit.capture(), eq(0L));
        assertThat(audit.getValue().getAuditKind()).isEqualTo("IDENTITY");
        assertThat(audit.getValue().getActorSub()).isEqualTo("admin-sub");
        assertThat(audit.getValue().getTargetSub()).isEqualTo("alice-sub");
        assertThat(audit.getValue().getPayload().path("sourceBucket").asText()).isEqualTo("ADMIN_API");
        verify(stream).ack("oauth-audit-persist-v1", id);
        verify(durableAudit, never()).acknowledge("identity-event-1");
    }

    @Test
    void requiredArgsConstructorRetainsEveryQualifierForTheStreamConsumer() {
        var constructor = java.util.Arrays.stream(OAuthAuditConsumer.class.getDeclaredConstructors())
                .filter(value -> value.getParameterCount() == 7).findFirst().orElseThrow();
        var qualifierValues = java.util.Arrays.stream(constructor.getParameterAnnotations())
                .map(annotations -> java.util.Arrays.stream(annotations)
                        .filter(Qualifier.class::isInstance)
                        .map(Qualifier.class::cast)
                        .map(Qualifier::value)
                        .findFirst()
                        .orElse(""))
                .toList();

        assertThat(qualifierValues).containsExactly("rbac3RuntimeRedissonClient", "oAuthAuditRepository",
                "tenantIssuerService", "tokenUsageAuditComponent", "oauthAuditProperties", "idpClock", "auditDeliveryProperties");
    }

    @Test
    void acknowledgesEventsOlderThanRetentionWithoutRecreatingAuditRows() {
        StreamMessageId id = new StreamMessageId(2L, 0L);
        Map<String, String> fields = identityFields("expired-event").entrySet().stream().collect(
                java.util.stream.Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getKey().equals("occurredAt")
                                ? "2025-01-01T00:00:00Z" : entry.getValue()));
        when(stream.readGroup(eq("oauth-audit-persist-v1"), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(Map.of(id, fields));

        assertThat(consumer.consumeBatch()).isEqualTo(1);

        verify(audits, never()).save(any(), any(), anyLong());
        verify(stream).ack("oauth-audit-persist-v1", id);
        verify(durableAudit, never()).acknowledge("expired-event");
    }

    @Test
    void sendsMalformedOrForgedUsageEventsToTheDeadLetterStream() {
        StreamMessageId id = new StreamMessageId(3L, 0L);
        Map<String, String> fields = new java.util.LinkedHashMap<>(identityFields("forged-event"));
        fields.put("auditKind", "OAUTH");
        fields.put("resource", "https://api.example/resource");
        fields.put("tokenRef", "a".repeat(64));
        fields.put("signature", "0".repeat(64));
        when(durableAudit.verifySignature(PLATFORM.getIssuer().toASCIIString(), "a".repeat(64), "0".repeat(64)))
                .thenReturn(false);
        when(stream.readGroup(eq("oauth-audit-persist-v1"), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(Map.of(id, fields));
        @SuppressWarnings("unchecked")
        RStream<String, String> deadStream = mock(RStream.class);
        doReturn(deadStream).when(redisson).getStream(eq("egon:oauth:audit:dead:v1"), eq(StringCodec.INSTANCE));
        when(deadStream.add(any(StreamAddArgs.class))).thenReturn(new StreamMessageId(4L, 0L));

        assertThat(consumer.consumeBatch()).isEqualTo(1);

        verify(audits, never()).save(any(), any(), anyLong());
        verify(deadStream).add(any(StreamAddArgs.class));
        verify(stream).ack("oauth-audit-persist-v1", id);
    }

    @Test
    void deadLettersAnUnregisteredIssuerWithoutCountingTransientRetries() {
        StreamMessageId id = new StreamMessageId(4L, 0L);
        URI unregistered = URI.create("https://idp.example/tenants/9999");
        Map<String, String> fields = new java.util.LinkedHashMap<>(identityFields("unknown-issuer"));
        fields.put("issuer", unregistered.toASCIIString());
        when(issuers.resolveExactIssuer(unregistered)).thenThrow(new OAuth2AuthenticationException(
                new OAuth2Error("invalid_request", "Request rejected", null)));
        when(stream.readGroup(eq("oauth-audit-persist-v1"), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(Map.of(id, fields));
        @SuppressWarnings("unchecked")
        RStream<String, String> deadStream = mock(RStream.class);
        doReturn(deadStream).when(redisson).getStream(eq("egon:oauth:audit:dead:v1"), eq(StringCodec.INSTANCE));
        when(deadStream.add(any(StreamAddArgs.class))).thenReturn(new StreamMessageId(5L, 0L));

        assertThat(consumer.consumeBatch()).isEqualTo(1);

        verify(audits, never()).save(any(), any(), anyLong());
        verify(attempts, never()).incrementAndGet();
        verify(deadStream).add(any(StreamAddArgs.class));
        verify(stream).ack("oauth-audit-persist-v1", id);
    }

    @Test
    void leavesTransientDatabaseFailuresPendingForRetry() {
        StreamMessageId id = new StreamMessageId(5L, 0L);
        when(stream.readGroup(eq("oauth-audit-persist-v1"), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(Map.of(id, identityFields("retry-event")));
        when(audits.save(eq(PLATFORM), any(OAuthAuditBO.class), eq(0L)))
                .thenThrow(new IllegalStateException("database unavailable"));
        when(attempts.incrementAndGet()).thenReturn(1L);

        assertThat(consumer.consumeBatch()).isZero();

        verify(stream, never()).ack("oauth-audit-persist-v1", id);
        verify(durableAudit, never()).acknowledge("retry-event");
    }

    @Test
    void signedUsageCommitAndReceiptAreConfirmedBeforeXackEvenWithoutTokenReference() {
        var script = mock(org.redisson.api.RScript.class);
        when(redisson.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.eval(any(org.redisson.api.RScript.Mode.class), anyString(), any(org.redisson.api.RScript.ReturnType.class),
                any(java.util.List.class), anyString(), anyLong(), anyString(), anyString())).thenReturn(1L);
        var event = new top.egon.cola.platform.tianquan.shoubing.core.audit.TokenUsageAuditEvent("usage-1", 1,
                PLATFORM.getIssuer(), "https://api.example/resource", null, null, null, "SUCCESS", "TOKEN_USE_VERIFIED", NOW, null);
        var receipt = top.egon.cola.platform.tianquan.shoubing.starter.audit.domain.dto.TokenUsageAuditReceiptDTO.builder()
                .schemaVersion(1).eventId("usage-1").eventFingerprint("a".repeat(64)).outcome("STORED")
                .confirmedAt(NOW).signature("b".repeat(64)).build();
        when(durableAudit.verifySignature(PLATFORM.getIssuer().toASCIIString(), null, "c".repeat(64))).thenReturn(true);
        when(durableAudit.createReceipt(event, "STORED", NOW)).thenReturn(receipt);
        when(durableAudit.encodeReceipt(receipt)).thenReturn("receipt-json");
        StreamMessageId id = new StreamMessageId(7, 0);
        when(stream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(Map.of(id, usageFields("usage-1", NOW)));
        assertThat(consumer.consumeBatch()).isEqualTo(1);
        var ordered = org.mockito.Mockito.inOrder(audits, script, stream);
        ordered.verify(audits).save(eq(PLATFORM), any(OAuthAuditBO.class), eq(0L));
        ordered.verify(script).eval(eq(org.redisson.api.RScript.Mode.READ_WRITE), anyString(),
                eq(org.redisson.api.RScript.ReturnType.INTEGER), eq(java.util.List.of(STREAM + ":receipt:usage-1")),
                eq("receipt-json"), eq(3600000L), eq("usage-1"), eq("a".repeat(64)));
        ordered.verify(stream).ack("oauth-audit-persist-v1", id);
        verify(durableAudit, never()).acknowledge(anyString());
    }

    @Test
    void unconfirmedReceiptKeepsCommittedUsagePending() {
        var script = mock(org.redisson.api.RScript.class);
        when(redisson.getScript(StringCodec.INSTANCE)).thenReturn(script);
        var receipt = top.egon.cola.platform.tianquan.shoubing.starter.audit.domain.dto.TokenUsageAuditReceiptDTO.builder()
                .schemaVersion(1).eventId("usage-2").eventFingerprint("a".repeat(64)).build();
        when(durableAudit.verifySignature(anyString(), org.mockito.ArgumentMatchers.isNull(), anyString())).thenReturn(true);
        when(durableAudit.createReceipt(any(), eq("STORED"), eq(NOW))).thenReturn(receipt);
        when(durableAudit.encodeReceipt(receipt)).thenReturn("receipt-json");
        StreamMessageId id = new StreamMessageId(8, 0);
        when(stream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(Map.of(id, usageFields("usage-2", NOW)));
        assertThat(consumer.consumeBatch()).isZero();
        verify(audits).save(any(), any(), anyLong());
        verify(stream, never()).ack(anyString(), any());
    }

    @Test
    void signedDatabaseRollbackNeverPublishesReceiptOrXacks() {
        when(durableAudit.verifySignature(anyString(), org.mockito.ArgumentMatchers.isNull(), anyString())).thenReturn(true);
        when(audits.save(any(), any(), anyLong())).thenThrow(new IllegalStateException("unknown commit"));
        when(stream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(Map.of(new StreamMessageId(9, 0), usageFields("usage-3", NOW)));
        assertThat(consumer.consumeBatch()).isZero();
        verify(durableAudit, never()).createReceipt(any(), anyString(), any());
        verify(redisson, never()).getScript(any(org.redisson.client.codec.Codec.class));
        verify(stream, never()).ack(anyString(), any());
    }

    @Test
    void expiredSignedUsageGetsExpiredReceiptWithoutRecreatingAuditRows() {
        var script = receiptScript();
        var receipt = receipt("expired-usage", "EXPIRED");
        when(durableAudit.verifySignature(anyString(), org.mockito.ArgumentMatchers.isNull(), anyString())).thenReturn(true);
        when(durableAudit.createReceipt(any(), eq("EXPIRED"), eq(NOW))).thenReturn(receipt);
        when(durableAudit.encodeReceipt(receipt)).thenReturn("expired-json");
        StreamMessageId id = new StreamMessageId(10, 0);
        when(stream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(Map.of(id, usageFields("expired-usage", NOW.minusSeconds(181 * 86400L))));
        assertThat(consumer.consumeBatch()).isEqualTo(1);
        verify(audits, never()).save(any(), any(), anyLong());
        verify(durableAudit).createReceipt(any(), eq("EXPIRED"), eq(NOW));
        verify(stream).ack("oauth-audit-persist-v1", id);
    }

    @Test
    void lostXackRedeliversTheSameFactAndRenewsReceiptAfterPersistence() {
        var script = receiptScript();
        var receipt = receipt("repeat-usage", "STORED");
        when(durableAudit.verifySignature(anyString(), org.mockito.ArgumentMatchers.isNull(), anyString())).thenReturn(true);
        when(durableAudit.createReceipt(any(), eq("STORED"), eq(NOW))).thenReturn(receipt);
        when(durableAudit.encodeReceipt(receipt)).thenReturn("stored-json");
        StreamMessageId id = new StreamMessageId(11, 0);
        when(stream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(Map.of(id, usageFields("repeat-usage", NOW)));
        when(stream.ack("oauth-audit-persist-v1", id)).thenThrow(new IllegalStateException("unknown XACK")).thenReturn(1L);
        assertThat(consumer.consumeBatch()).isZero();
        assertThat(consumer.consumeBatch()).isEqualTo(1);
        verify(audits, org.mockito.Mockito.times(2)).save(any(), any(), anyLong());
        verify(durableAudit, org.mockito.Mockito.times(2)).createReceipt(any(), eq("STORED"), eq(NOW));
    }

    @Test
    void exhaustedTrustedUsageConfirmsDeadStreamBeforeDeadReceiptAndXack() {
        var script = receiptScript();
        var receipt = receipt("dead-usage", "DEAD");
        when(durableAudit.verifySignature(anyString(), org.mockito.ArgumentMatchers.isNull(), anyString())).thenReturn(true);
        when(durableAudit.createReceipt(any(), eq("DEAD"), eq(NOW))).thenReturn(receipt);
        when(durableAudit.encodeReceipt(receipt)).thenReturn("dead-json");
        when(audits.save(any(), any(), anyLong())).thenThrow(new IllegalStateException("unavailable"));
        when(attempts.incrementAndGet()).thenReturn(30L);
        @SuppressWarnings("unchecked") RStream<String, String> dead = mock(RStream.class);
        doReturn(dead).when(redisson).getStream(eq("egon:oauth:audit:dead:v1"), eq(StringCodec.INSTANCE));
        when(dead.add(any(StreamAddArgs.class))).thenReturn(new StreamMessageId(21, 0));
        StreamMessageId id = new StreamMessageId(12, 0);
        when(stream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(Map.of(id, usageFields("dead-usage", NOW)));
        assertThat(consumer.consumeBatch()).isEqualTo(1);
        var ordered = org.mockito.Mockito.inOrder(dead, durableAudit, script, stream);
        ordered.verify(dead).add(any(StreamAddArgs.class));
        ordered.verify(durableAudit).createReceipt(any(), eq("DEAD"), eq(NOW));
        ordered.verify(durableAudit).encodeReceipt(receipt);
        ordered.verify(script).eval(any(org.redisson.api.RScript.Mode.class), anyString(),
                any(org.redisson.api.RScript.ReturnType.class), any(java.util.List.class),
                anyString(), anyLong(), anyString(), anyString());
        ordered.verify(stream).ack("oauth-audit-persist-v1", id);
        verify(durableAudit, never()).acknowledge(anyString());
    }

    @Test
    void unconfirmedDeadStreamNeverCreatesReceiptOrXacks() {
        when(durableAudit.verifySignature(anyString(), org.mockito.ArgumentMatchers.isNull(), anyString())).thenReturn(true);
        when(audits.save(any(), any(), anyLong())).thenThrow(new IllegalStateException("unavailable"));
        when(attempts.incrementAndGet()).thenReturn(30L);
        doReturn(mock(RStream.class)).when(redisson).getStream(eq("egon:oauth:audit:dead:v1"), eq(StringCodec.INSTANCE));
        when(stream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(Map.of(new StreamMessageId(13, 0), usageFields("dead-usage", NOW)));
        consumer.consumeBatch();
        verify(durableAudit, never()).createReceipt(any(), anyString(), any());
        verify(stream, never()).ack(anyString(), any());
    }

    @Test
    void expiredSignedUsageStillRequiresAnExactTrustedIssuer() {
        when(durableAudit.verifySignature(anyString(), org.mockito.ArgumentMatchers.isNull(), anyString())).thenReturn(true);
        when(issuers.resolveExactIssuer(PLATFORM.getIssuer())).thenThrow(new OAuth2AuthenticationException(new OAuth2Error("invalid_request")));
        @SuppressWarnings("unchecked") RStream<String, String> dead = mock(RStream.class);
        doReturn(dead).when(redisson).getStream(eq("egon:oauth:audit:dead:v1"), eq(StringCodec.INSTANCE));
        when(dead.add(any(StreamAddArgs.class))).thenReturn(new StreamMessageId(30, 0));
        when(stream.readGroup(anyString(), anyString(), any(StreamReadGroupArgs.class)))
                .thenReturn(Map.of(new StreamMessageId(14, 0), usageFields("expired-untrusted", NOW.minusSeconds(181 * 86400L))));
        consumer.consumeBatch();
        verify(audits, never()).save(any(), any(), anyLong());
        verify(durableAudit, never()).createReceipt(any(), anyString(), any());
    }

    @Test
    void managedConsumerSelectsTheQualifiedLombokConstructorAndKeepsLegacyConstructor() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withBean("rbac3RuntimeRedissonClient", RedissonClient.class, () -> redisson)
                .withBean("oAuthAuditRepository", OAuthAuditRepository.class, () -> audits)
                .withBean("tenantIssuerService", TenantIssuerService.class, () -> issuers)
                .withBean("tokenUsageAuditComponent", TokenUsageAuditComponent.class, () -> durableAudit)
                .withBean("oauthAuditProperties", OAuthAuditProperties.class, () -> new OAuthAuditProperties(STREAM,
                        Path.of("/tmp/audit"), 1024, Path.of("/tmp/key")))
                .withBean("idpClock", Clock.class, () -> Clock.fixed(NOW, ZoneOffset.UTC))
                .withBean("auditDeliveryProperties", top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties.class,
                        top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties::new)
                .withUserConfiguration(OAuthAuditConsumer.class)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(OAuthAuditConsumer.class));
    }

    private org.redisson.api.RScript receiptScript() {
        var script = mock(org.redisson.api.RScript.class);
        when(redisson.getScript(StringCodec.INSTANCE)).thenReturn(script);
        when(script.eval(any(org.redisson.api.RScript.Mode.class), anyString(), any(org.redisson.api.RScript.ReturnType.class),
                any(java.util.List.class), anyString(), anyLong(), anyString(), anyString())).thenReturn(1L);
        return script;
    }

    private static top.egon.cola.platform.tianquan.shoubing.starter.audit.domain.dto.TokenUsageAuditReceiptDTO receipt(String id, String outcome) {
        return top.egon.cola.platform.tianquan.shoubing.starter.audit.domain.dto.TokenUsageAuditReceiptDTO.builder()
                .schemaVersion(1).eventId(id).eventFingerprint("a".repeat(64)).outcome(outcome)
                .confirmedAt(NOW).signature("b".repeat(64)).build();
    }

    private static Map<String, String> usageFields(String eventId, Instant occurredAt) {
        return Map.ofEntries(Map.entry("eventId", eventId), Map.entry("schemaVersion", "1"),
                Map.entry("issuer", PLATFORM.getIssuer().toASCIIString()), Map.entry("auditKind", "OAUTH"),
                Map.entry("resource", "https://api.example/resource"), Map.entry("result", "SUCCESS"),
                Map.entry("reasonCode", "TOKEN_USE_VERIFIED"), Map.entry("occurredAt", occurredAt.toString()),
                Map.entry("signature", "c".repeat(64)));
    }

    private static Map<String, String> identityFields(String eventId) {
        return Map.ofEntries(
                Map.entry("eventId", eventId),
                Map.entry("schemaVersion", "1"),
                Map.entry("issuer", PLATFORM.getIssuer().toASCIIString()),
                Map.entry("auditKind", "IDENTITY"),
                Map.entry("eventType", "IDENTITY_LOGIN_FAILED"),
                Map.entry("actorSub", "admin-sub"),
                Map.entry("targetSub", "alice-sub"),
                Map.entry("result", "FAILURE"),
                Map.entry("reasonCode", "BAD_CREDENTIALS"),
                Map.entry("sourceBucket", "ADMIN_API"),
                Map.entry("occurredAt", NOW.toString()));
    }
}
