package top.egon.cola.platform.tianquan.shoubing.admin.audit.support.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;

@Component("oauthAuditOutboxDeliveryHandler")
public final class OAuthAuditOutboxDeliveryHandler implements DeliveryHandler {

    public static final String CHANNEL = "oauth-audit";
    public static final String DESTINATION = "oauth.audit.token-issued.v1";
    public static final String STREAM = "egon:oauth:audit:v1";
    private static final java.util.Set<String> ALLOWED_FIELDS = java.util.Set.of(
            "eventId", "schemaVersion", "issuer", "resource", "subjectRef", "clientRef",
            "result", "reasonCode", "occurredAt");

    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;

    public OAuthAuditOutboxDeliveryHandler(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            @Qualifier("jacksonObjectMapper") ObjectMapper objectMapper) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public void validateDestination(String destination) {
        if (!DESTINATION.equals(destination)) {
            throw new IllegalArgumentException("OAUTH_AUDIT_OUTBOX_DESTINATION_UNSUPPORTED");
        }
    }

    @Override
    public DeliveryResult deliver(DeliveryContext context) {
        try {
            validateDestination(context.destination());
            JsonNode event = objectMapper.readTree(context.payload());
            Map<String, String> fields = streamFields(context, event);
            RStream<String, String> stream = redisson.getStream(STREAM, StringCodec.INSTANCE);
            StreamMessageId messageId = stream.add(StreamAddArgs.entries(fields));
            if (messageId == null) {
                return DeliveryResult.retryableFailure(
                        "OAUTH_AUDIT_STREAM_NOT_CONFIRMED", "OAuth audit stream did not confirm the event");
            }
            return DeliveryResult.success();
        } catch (JsonProcessingException | IllegalArgumentException invalid) {
            return DeliveryResult.permanentFailure("OAUTH_AUDIT_EVENT_INVALID", "OAuth audit event is invalid");
        } catch (RuntimeException unavailable) {
            return DeliveryResult.retryableFailure(
                    "OAUTH_AUDIT_STREAM_UNAVAILABLE", "OAuth audit stream is unavailable");
        }
    }

    private static Map<String, String> streamFields(DeliveryContext context, JsonNode event) {
        if (event == null || !event.isObject()) {
            throw new IllegalArgumentException("OAUTH_AUDIT_EVENT_OBJECT_REQUIRED");
        }
        event.fieldNames().forEachRemaining(name -> {
            if (!ALLOWED_FIELDS.contains(name)) {
                throw new IllegalArgumentException("OAUTH_AUDIT_EVENT_FIELD_UNSUPPORTED");
            }
        });
        String eventId = text(event, "eventId");
        String schemaVersion = text(event, "schemaVersion");
        URI issuer = URI.create(text(event, "issuer"));
        String resource = text(event, "resource");
        String clientRef = text(event, "clientRef");
        String result = text(event, "result");
        String reasonCode = text(event, "reasonCode");
        String occurredAt = text(event, "occurredAt");
        if (!Objects.equals(context.messageId(), eventId) || !"1".equals(schemaVersion)
                || !issuer.isAbsolute() || !"https".equalsIgnoreCase(issuer.getScheme())
                || issuer.getHost() == null || issuer.getUserInfo() != null
                || issuer.getQuery() != null || issuer.getFragment() != null
                || resource.length() > 2048 || clientRef.length() != 64
                || !clientRef.matches("[0-9a-f]{64}")
                || !"SUCCESS".equals(result) || reasonCode.isBlank() || occurredAt.isBlank()) {
            throw new IllegalArgumentException("OAUTH_AUDIT_EVENT_INVALID");
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("eventId", eventId);
        fields.put("schemaVersion", schemaVersion);
        fields.put("issuer", issuer.toASCIIString());
        fields.put("resource", resource);
        String subjectRef = optionalText(event, "subjectRef");
        if (subjectRef != null) {
            if (!subjectRef.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("OAUTH_AUDIT_SUBJECT_REF_INVALID");
            }
            fields.put("subjectRef", subjectRef);
        }
        fields.put("clientRef", clientRef);
        fields.put("result", result);
        fields.put("reasonCode", reasonCode);
        fields.put("occurredAt", occurredAt);
        return fields;
    }

    private static String text(JsonNode event, String name) {
        JsonNode node = event.get(name);
        if (node == null || node.isNull() || !node.isValueNode()) {
            throw new IllegalArgumentException("OAUTH_AUDIT_EVENT_FIELD_REQUIRED");
        }
        String value = node.asText();
        if (value.isBlank()) {
            throw new IllegalArgumentException("OAUTH_AUDIT_EVENT_FIELD_REQUIRED");
        }
        return value;
    }

    private static String optionalText(JsonNode event, String name) {
        JsonNode node = event.get(name);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
}
