package top.egon.cola.platform.tianquan.shoubing.admin.audit.support.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;

@Component("oauthAuditOutboxDeliveryHandler")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
public final class OAuthAuditOutboxDeliveryHandler implements DeliveryHandler {

    public static final String CHANNEL = "oauth-audit";
    public static final String DESTINATION = "oauth.audit.token-issued.v1";
    public static final String IDENTITY_DESTINATION = "oauth.audit.identity.v1";
    public static final String STREAM = "egon:oauth:audit:v1";
    private static final java.util.Set<String> ALLOWED_FIELDS = java.util.Set.of(
            "eventId", "schemaVersion", "issuer", "resource", "subjectRef", "clientRef",
            "result", "reasonCode", "occurredAt", "auditKind", "eventType", "actorSub", "targetSub",
            "identityTenantId", "traceId", "sourceBucket");

    @Qualifier("rbac3RuntimeRedissonClient")
    private final RedissonClient redisson;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Value("${egon.cola.platform.tianquan.shoubing.audit.stream:" + STREAM + "}")
    private final String auditStream;

    @PostConstruct
    void validateConfiguration() {
        if (auditStream == null || !auditStream.matches("[A-Za-z0-9:._-]{1,128}")) {
            throw new IllegalStateException("OAUTH_AUDIT_STREAM_INVALID");
        }
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public void validateDestination(String destination) {
        if (!DESTINATION.equals(destination) && !IDENTITY_DESTINATION.equals(destination)) {
            throw new IllegalArgumentException("OAUTH_AUDIT_OUTBOX_DESTINATION_UNSUPPORTED");
        }
    }

    @Override
    public DeliveryResult deliver(DeliveryContext context) {
        try {
            validateDestination(context.destination());
            JsonNode event = objectMapper.readTree(context.payload());
            Map<String, String> fields = streamFields(context, event);
            RStream<String, String> stream = redisson.getStream(auditStream, StringCodec.INSTANCE);
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

    static Map<String, String> streamFields(DeliveryContext context, JsonNode event) {
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
        String result = text(event, "result");
        String reasonCode = text(event, "reasonCode");
        String occurredAt = text(event, "occurredAt");
        String auditKind = optionalText(event, "auditKind");
        if (auditKind == null) {
            auditKind = "OAUTH";
        }
        String eventType = optionalText(event, "eventType");
        if (eventType == null) {
            eventType = reasonCode;
        }
        if (!Objects.equals(context.messageId(), eventId) || !"1".equals(schemaVersion)
                || !issuer.isAbsolute() || !"https".equalsIgnoreCase(issuer.getScheme())
                || issuer.getHost() == null || issuer.getUserInfo() != null
                || issuer.getQuery() != null || issuer.getFragment() != null
                || !eventType.matches("[A-Za-z0-9._~:-]{1,128}")
                || reasonCode.isBlank() || occurredAt.isBlank()) {
            throw new IllegalArgumentException("OAUTH_AUDIT_EVENT_INVALID");
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("eventId", eventId);
        fields.put("schemaVersion", schemaVersion);
        fields.put("issuer", issuer.toASCIIString());
        fields.put("auditKind", auditKind);
        fields.put("eventType", eventType);
        if ("OAUTH".equals(auditKind)) {
            String resource = text(event, "resource");
            String clientRef = optionalText(event, "clientRef");
            if (!Objects.equals(context.destination(), DESTINATION)
                    || resource.length() > 2048
                    || clientRef != null && !clientRef.matches("[0-9a-f]{64}")
                    || !"SUCCESS".equals(result)) {
                throw new IllegalArgumentException("OAUTH_AUDIT_EVENT_INVALID");
            }
            fields.put("resource", resource);
            if (clientRef != null) {
                fields.put("clientRef", clientRef);
            }
            String subjectRef = optionalText(event, "subjectRef");
            if (subjectRef != null) {
                if (!subjectRef.matches("[0-9a-f]{64}")) {
                    throw new IllegalArgumentException("OAUTH_AUDIT_SUBJECT_REF_INVALID");
                }
                fields.put("subjectRef", subjectRef);
            }
        } else if ("IDENTITY".equals(auditKind)) {
            String actorSub = safeIdentitySubject(text(event, "actorSub"));
            String targetSub = safeIdentitySubject(text(event, "targetSub"));
            String sourceBucket = text(event, "sourceBucket");
            if (!Objects.equals(context.destination(), IDENTITY_DESTINATION)
                    || !Set.of("SUCCESS", "FAILURE").contains(result)
                    || sourceBucket.length() > 128
                    || !sourceBucket.matches("[A-Za-z0-9._~:-]{1,128}")) {
                throw new IllegalArgumentException("IDENTITY_AUDIT_EVENT_INVALID");
            }
            fields.put("actorSub", actorSub);
            fields.put("targetSub", targetSub);
            fields.put("sourceBucket", sourceBucket);
            String identityTenantId = optionalText(event, "identityTenantId");
            if (identityTenantId != null) {
                if (!identityTenantId.matches("[A-Za-z0-9._~-]{1,64}")) {
                    throw new IllegalArgumentException("IDENTITY_AUDIT_TENANT_INVALID");
                }
                fields.put("identityTenantId", identityTenantId);
            }
            String traceId = optionalText(event, "traceId");
            if (traceId != null) {
                if (!traceId.matches("[A-Za-z0-9._~-]{1,128}")) {
                    throw new IllegalArgumentException("IDENTITY_AUDIT_TRACE_INVALID");
                }
                fields.put("traceId", traceId);
            }
        } else {
            throw new IllegalArgumentException("OAUTH_AUDIT_KIND_INVALID");
        }
        fields.put("result", result);
        fields.put("reasonCode", reasonCode);
        fields.put("occurredAt", occurredAt);
        return fields;
    }

    private static String safeIdentitySubject(String value) {
        if (value.length() > 64 || !value.matches("[A-Za-z0-9._~-]{1,64}")) {
            throw new IllegalArgumentException("IDENTITY_AUDIT_SUBJECT_INVALID");
        }
        return value;
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
