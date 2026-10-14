package top.egon.cola.platform.tianquan.shoubing.admin.audit.support.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryResult;

class OAuthAuditOutboxDeliveryHandlerTest {

    @Test
    void invalidConfiguredStreamsFailBeforeAnyRedisWrite() {
        RedissonClient redisson = mock(RedissonClient.class);
        for (String streamName : java.util.List.of("", "bad stream", "bad\nstream", "a".repeat(129))) {
            new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                    .withUserConfiguration(OAuthAuditOutboxDeliveryHandler.class)
                    .withBean("rbac3RuntimeRedissonClient", RedissonClient.class, () -> redisson)
                    .withBean("jacksonObjectMapper", ObjectMapper.class, ObjectMapper::new)
                    .withPropertyValues("egon.cola.platform.tianquan.shoubing.audit.stream=" + streamName)
                    .run(context -> assertThat(context).hasFailed().getFailure()
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage("OAUTH_AUDIT_STREAM_INVALID"));
        }
        org.mockito.Mockito.verifyNoInteractions(redisson);
    }

    @Test
    void usesTheConfiguredTaskStreamThroughConstructorBinding() {
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RStream<String, String> stream = mock(RStream.class);
        String streamName = "xingyuan-it:run-123:oauth:audit:v1";
        org.mockito.Mockito.doReturn(stream).when(redisson).getStream(eq(streamName), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenReturn(new StreamMessageId(8L, 1L));
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(OAuthAuditOutboxDeliveryHandler.class)
                .withBean("rbac3RuntimeRedissonClient", RedissonClient.class, () -> redisson)
                .withBean("jacksonObjectMapper", ObjectMapper.class, () -> new ObjectMapper().findAndRegisterModules())
                .withPropertyValues("egon.cola.platform.tianquan.shoubing.audit.stream=" + streamName)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var handler = context.getBean("oauthAuditOutboxDeliveryHandler", OAuthAuditOutboxDeliveryHandler.class);
                    String payload = """
                            {"eventId":"task-event-1","schemaVersion":"1","issuer":"https://idp.example/platform",
                             "auditKind":"IDENTITY","eventType":"IDENTITY_LOGIN_FAILED","actorSub":"admin-sub",
                             "targetSub":"alice-sub","result":"FAILURE","reasonCode":"BAD_CREDENTIALS",
                             "sourceBucket":"ADMIN_API","occurredAt":"2026-10-08T08:00:00Z"}
                            """;
                    var delivery = new DeliveryContext("task-event-1", OAuthAuditOutboxDeliveryHandler.CHANNEL,
                            OAuthAuditOutboxDeliveryHandler.IDENTITY_DESTINATION, payload, "application/json", "1",
                            Map.of(), null, 1, 5, Instant.parse("2026-10-08T08:00:00Z"));
                    assertThat(handler.deliver(delivery).kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
                    org.mockito.Mockito.verify(redisson).getStream(streamName, StringCodec.INSTANCE);
                    org.mockito.Mockito.verify(redisson, org.mockito.Mockito.never())
                            .getStream(OAuthAuditOutboxDeliveryHandler.STREAM, StringCodec.INSTANCE);
                });
    }

    @Test
    void deliversIdentityFailureCompatibilityEvents() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RStream<String, String> stream = mock(RStream.class);
        org.mockito.Mockito.doReturn(stream).when(redisson)
                .getStream(eq(OAuthAuditOutboxDeliveryHandler.STREAM), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenReturn(mock(StreamMessageId.class));
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        OAuthAuditOutboxDeliveryHandler handler = new OAuthAuditOutboxDeliveryHandler(
                redisson, mapper, OAuthAuditOutboxDeliveryHandler.STREAM);
        String eventId = "identity-event-1";
        String payload = mapper.writeValueAsString(Map.ofEntries(
                Map.entry("eventId", eventId),
                Map.entry("schemaVersion", "1"),
                Map.entry("issuer", "https://idp.example/platform"),
                Map.entry("auditKind", "IDENTITY"),
                Map.entry("eventType", "IDENTITY_LOGIN_FAILED"),
                Map.entry("actorSub", "admin-sub"),
                Map.entry("targetSub", "alice-sub"),
                Map.entry("result", "FAILURE"),
                Map.entry("reasonCode", "BAD_CREDENTIALS"),
                Map.entry("sourceBucket", "ADMIN_API"),
                Map.entry("occurredAt", Instant.parse("2026-10-08T08:00:00Z").toString())));
        DeliveryContext context = new DeliveryContext(
                eventId, "oauth-audit", "oauth.audit.identity.v1", payload,
                "application/json", "1", Map.of(), null, 1, 5, Instant.parse("2026-10-08T08:00:00Z"));

        assertThat(handler.deliver(context).kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
    }

    @Test
    void permanentlyRejectsSecretFieldsBeforeWritingToRedis() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        OAuthAuditOutboxDeliveryHandler handler = new OAuthAuditOutboxDeliveryHandler(
                redisson, mapper, OAuthAuditOutboxDeliveryHandler.STREAM);
        String eventId = "identity-event-2";
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>(Map.ofEntries(
                Map.entry("eventId", eventId),
                Map.entry("schemaVersion", "1"),
                Map.entry("issuer", "https://idp.example/platform"),
                Map.entry("auditKind", "IDENTITY"),
                Map.entry("eventType", "IDENTITY_LOGIN_FAILED"),
                Map.entry("actorSub", "admin-sub"),
                Map.entry("targetSub", "alice-sub"),
                Map.entry("result", "FAILURE"),
                Map.entry("reasonCode", "BAD_CREDENTIALS"),
                Map.entry("sourceBucket", "ADMIN_API"),
                Map.entry("occurredAt", Instant.parse("2026-10-08T08:00:00Z").toString())));
        body.put("token", "must-not-enter-the-stream");
        DeliveryContext context = new DeliveryContext(
                eventId, "oauth-audit", "oauth.audit.identity.v1", mapper.writeValueAsString(body),
                "application/json", "1", Map.of(), null, 1, 5, Instant.parse("2026-10-08T08:00:00Z"));

        assertThat(handler.deliver(context).kind()).isEqualTo(DeliveryResult.Kind.PERMANENT_FAILURE);
        org.mockito.Mockito.verifyNoInteractions(redisson);
    }

    @Test
    void deliversClientlessSigningKeyRotationAuditEvents() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RStream<String, String> stream = mock(RStream.class);
        org.mockito.Mockito.doReturn(stream).when(redisson)
                .getStream(eq(OAuthAuditOutboxDeliveryHandler.STREAM), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenReturn(new StreamMessageId(8L, 1L));
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        OAuthAuditOutboxDeliveryHandler handler = new OAuthAuditOutboxDeliveryHandler(
                redisson, mapper, OAuthAuditOutboxDeliveryHandler.STREAM);
        String eventId = "key-rotation-1";
        String payload = mapper.writeValueAsString(Map.of(
                "eventId", eventId,
                "schemaVersion", "1",
                "issuer", "https://idp.example/tenants/1001",
                "auditKind", "OAUTH",
                "eventType", "SIGNING_KEY_ACTIVATED",
                "resource", "https://idp.example/tenants/1001",
                "result", "SUCCESS",
                "reasonCode", "AUTOMATED_KEY_ROTATION",
                "occurredAt", Instant.parse("2026-10-08T08:00:00Z").toString()));
        DeliveryContext context = new DeliveryContext(
                eventId, "oauth-audit", OAuthAuditOutboxDeliveryHandler.DESTINATION, payload,
                "application/json", "1", Map.of(), null, 1, 5, Instant.parse("2026-10-08T08:00:00Z"));

        assertThat(handler.deliver(context).kind()).isEqualTo(DeliveryResult.Kind.SUCCESS);
    }
}
