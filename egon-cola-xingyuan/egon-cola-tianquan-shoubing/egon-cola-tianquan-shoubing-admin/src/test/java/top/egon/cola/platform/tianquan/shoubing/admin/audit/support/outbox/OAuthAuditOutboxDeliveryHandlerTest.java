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
    void deliversIdentityFailureCompatibilityEvents() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RStream<String, String> stream = mock(RStream.class);
        org.mockito.Mockito.doReturn(stream).when(redisson)
                .getStream(eq(OAuthAuditOutboxDeliveryHandler.STREAM), eq(StringCodec.INSTANCE));
        when(stream.add(any(StreamAddArgs.class))).thenReturn(mock(StreamMessageId.class));
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        OAuthAuditOutboxDeliveryHandler handler = new OAuthAuditOutboxDeliveryHandler(redisson, mapper);
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
        OAuthAuditOutboxDeliveryHandler handler = new OAuthAuditOutboxDeliveryHandler(redisson, mapper);
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
        OAuthAuditOutboxDeliveryHandler handler = new OAuthAuditOutboxDeliveryHandler(redisson, mapper);
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
