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
        verify(durableAudit).acknowledge("identity-event-1");
    }

    @Test
    void requiredArgsConstructorRetainsEveryQualifierForTheStreamConsumer() {
        var constructor = OAuthAuditConsumer.class.getDeclaredConstructors()[0];
        var qualifierValues = java.util.Arrays.stream(constructor.getParameterAnnotations())
                .map(annotations -> java.util.Arrays.stream(annotations)
                        .filter(Qualifier.class::isInstance)
                        .map(Qualifier.class::cast)
                        .map(Qualifier::value)
                        .findFirst()
                        .orElse(""))
                .toList();

        assertThat(qualifierValues).containsExactly("rbac3RuntimeRedissonClient", "oAuthAuditRepository",
                "tenantIssuerService", "tokenUsageAuditComponent", "oauthAuditProperties", "idpClock");
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
        verify(durableAudit).acknowledge("expired-event");
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
