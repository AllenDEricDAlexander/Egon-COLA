package top.egon.cola.platform.tianquan.shoubing.admin.audit.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.AutoClaimResult;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RScript;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.core.audit.TokenUsageAuditEvent;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.AuditDeliveryProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.OAuthAuditProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.TokenUsageAuditComponent;
import top.egon.cola.platform.tianquan.shoubing.starter.audit.domain.dto.TokenUsageAuditReceiptDTO;

@Service("oauthAuditConsumer")
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
public class OAuthAuditConsumer {

    private static final String GROUP = "oauth-audit-persist-v1";
    private static final String DEAD_STREAM = "egon:oauth:audit:dead:v1";
    private static final int BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS = 30;
    private static final Duration PENDING_IDLE = Duration.ofSeconds(60);
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "eventId", "schemaVersion", "issuer", "auditKind", "eventType", "resource", "subjectRef",
            "clientRef", "tokenRef", "result", "reasonCode", "occurredAt", "actorSub", "targetSub",
            "identityTenantId", "traceId", "sourceBucket", "signature");
    @Qualifier("rbac3RuntimeRedissonClient")
    private final RedissonClient redisson;
    @Qualifier("oAuthAuditRepository")
    private final OAuthAuditRepository audits;
    @Qualifier("tenantIssuerService")
    private final TenantIssuerService issuers;
    @Qualifier("tokenUsageAuditComponent")
    private final TokenUsageAuditComponent durableAudit;
    @Qualifier("oauthAuditProperties")
    private final OAuthAuditProperties properties;
    @Qualifier("idpClock")
    private final Clock clock;
    @Qualifier("auditDeliveryProperties")
    private final AuditDeliveryProperties delivery;

    public OAuthAuditConsumer(RedissonClient redisson, OAuthAuditRepository audits, TenantIssuerService issuers,
            TokenUsageAuditComponent durableAudit, OAuthAuditProperties properties, Clock clock) {
        this(redisson, audits, issuers, durableAudit, properties, clock, new AuditDeliveryProperties());
    }

    private final String consumerName = "audit-" + UUID.randomUUID().toString().replace("-", "");
    private volatile boolean groupReady;

    @Scheduled(fixedDelayString = "${egon.cola.platform.tianquan.shoubing.audit.consumer-delay:1000}")
    public void consumeScheduled() {
        consumeBatch();
    }

    public int consumeBatch() {
        RStream<String, String> stream = redisson.getStream(properties.stream(), StringCodec.INSTANCE);
        ensureGroup(stream);
        AutoClaimResult<String, String> claimed = stream.autoClaim(
                GROUP, consumerName, PENDING_IDLE.toMillis(), TimeUnit.MILLISECONDS, StreamMessageId.MIN, BATCH_SIZE);
        int processed = process(stream, claimed == null ? Map.of() : claimed.getMessages());
        Map<StreamMessageId, Map<String, String>> fresh = stream.readGroup(
                GROUP, consumerName, StreamReadGroupArgs.neverDelivered().count(BATCH_SIZE));
        return processed + process(stream, fresh == null ? Map.of() : fresh);
    }

    private void ensureGroup(RStream<String, String> stream) {
        if (groupReady) {
            return;
        }
        synchronized (this) {
            if (groupReady) {
                return;
            }
            try {
                stream.createGroup(StreamCreateGroupArgs.name(GROUP).id(StreamMessageId.ALL).makeStream());
            } catch (RedisException existsOrUnavailable) {
                String message = existsOrUnavailable.getMessage();
                if (message == null || !message.contains("BUSYGROUP")) {
                    throw existsOrUnavailable;
                }
            }
            groupReady = true;
        }
    }

    private int process(RStream<String, String> stream, Map<StreamMessageId, Map<String, String>> messages) {
        int processed = 0;
        for (Map.Entry<StreamMessageId, Map<String, String>> message : messages.entrySet()) {
            String eventId = message.getValue().get("eventId");
            try {
                AuditEvent event = parse(message.getValue());
                if (message.getValue().get("signature") != null) {
                    try { issuers.resolveExactIssuer(event.issuer()); }
                    catch (OAuth2AuthenticationException | IllegalArgumentException untrusted) {
                        throw new InvalidAuditEvent("OAUTH_AUDIT_ISSUER_UNTRUSTED", untrusted);
                    }
                }
                boolean expired = event.occurredAt().isBefore(clock.instant().minus(Duration.ofDays(180)));
                if (!expired) {
                    persist(event);
                }
                if (message.getValue().get("signature") != null) {
                    TokenUsageAuditEvent usage = usageEvent(event);
                    String outcome = expired ? "EXPIRED" : "STORED";
                    publishReceipt(durableAudit.createReceipt(usage, outcome, clock.instant()));
                }
                stream.ack(GROUP, message.getKey());
                attempts(event.eventId()).delete();
                processed++;
            } catch (InvalidAuditEvent invalid) {
                moveToDeadLetter(stream, message.getKey(), message.getValue(), "OAUTH_AUDIT_EVENT_INVALID");
                processed++;
            } catch (RuntimeException failure) {
                String safeId = safeEventId(eventId);
                long attempts = attempts(safeId).incrementAndGet();
                log.warn("OAuth audit persistence failed; eventId={} attempt={} failureType={}",
                        safeId, attempts, failure.getClass().getSimpleName());
                if (attempts >= MAX_ATTEMPTS) {
                    moveToDeadLetter(stream, message.getKey(), message.getValue(), "OAUTH_AUDIT_PERSIST_RETRIES_EXHAUSTED");
                    processed++;
                }
            }
        }
        return processed;
    }

    private void publishReceipt(TokenUsageAuditReceiptDTO receipt) {
        String encoded = durableAudit.encodeReceipt(receipt);
        // The fingerprint guard and SET PX form one PRIMARY operation, including duplicate TTL renewal.
        Long confirmed = redisson.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, """
                local current = redis.call('GET', KEYS[1])
                if current then
                    local ok, previous = pcall(cjson.decode, current)
                    if not ok or type(previous) ~= 'table' or previous.schemaVersion ~= 1
                        or previous.eventId ~= ARGV[3] or previous.eventFingerprint ~= ARGV[4] then
                        return 0
                    end
                end
                redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
                return 1
                """, RScript.ReturnType.INTEGER,
                java.util.List.of(properties.stream() + ":receipt:" + receipt.getEventId()),
                encoded, delivery.getReceiptTtl().toMillis(), receipt.getEventId(), receipt.getEventFingerprint());
        if (!Long.valueOf(1).equals(confirmed)) {
            throw new IllegalStateException("OAUTH_AUDIT_RECEIPT_CONFLICT_OR_UNCONFIRMED");
        }
    }

    private TokenUsageAuditEvent usageEvent(AuditEvent event) {
        return new TokenUsageAuditEvent(event.eventId(), 1, event.issuer(), event.resource(), event.subjectRef(),
                event.clientRef(), event.tokenRef(), event.result(), event.reasonCode(), event.occurredAt(), event.traceId());
    }

    private void persist(AuditEvent event) {
        TrustedRealmBO realm;
        try {
            realm = issuers.resolveExactIssuer(event.issuer());
        } catch (OAuth2AuthenticationException | IllegalArgumentException untrustedIssuer) {
            throw new InvalidAuditEvent("OAUTH_AUDIT_ISSUER_UNTRUSTED", untrustedIssuer);
        }
        if ("IDENTITY".equals(event.auditKind()) && realm.getRealmType() != RealmTypeEnum.PLATFORM) {
            throw new InvalidAuditEvent("IDENTITY_AUDIT_PLATFORM_REALM_REQUIRED");
        }
        OAuthAuditBO audit = OAuthAuditBO.builder()
                .version(0L)
                .eventId(event.eventId())
                .eventType(event.eventType())
                .subjectRef(event.subjectRef())
                .clientRef(event.clientRef())
                .tokenRef(event.tokenRef())
                .resourceRef(event.resource())
                .result(event.result())
                .reasonCode(event.reasonCode())
                .traceId(event.traceId())
                .occurredAt(event.occurredAt())
                .payload(payload(event))
                .auditKind(event.auditKind())
                .publicId(event.eventId())
                .actorSub(event.actorSub())
                .targetSub(event.targetSub())
                .identityTenantId(event.identityTenantId())
                .build();
        audits.save(realm, audit, 0L);
    }

    private JsonNode payload(AuditEvent event) {
        if ("OAUTH".equals(event.auditKind())) {
            return JsonNodeFactory.instance.objectNode();
        }
        var payload = JsonNodeFactory.instance.objectNode();
        payload.put("schemaVersion", 1);
        payload.put("sourceBucket", event.sourceBucket());
        return payload;
    }

    private AuditEvent parse(Map<String, String> fields) {
        if (fields == null || fields.keySet().stream().anyMatch(name -> !ALLOWED_FIELDS.contains(name))) {
            throw new InvalidAuditEvent("OAUTH_AUDIT_FIELDS_UNSUPPORTED");
        }
        String eventId = required(fields, "eventId");
        String schemaVersion = required(fields, "schemaVersion");
        String issuerValue = required(fields, "issuer");
        String auditKind = fields.getOrDefault("auditKind", "OAUTH");
        String eventType = fields.getOrDefault("eventType", required(fields, "reasonCode"));
        String result = required(fields, "result");
        String reasonCode = required(fields, "reasonCode");
        Instant occurredAt;
        URI issuer;
        try {
            issuer = URI.create(issuerValue);
            occurredAt = Instant.parse(required(fields, "occurredAt"));
        } catch (RuntimeException invalid) {
            throw new InvalidAuditEvent("OAUTH_AUDIT_TIME_OR_ISSUER_INVALID", invalid);
        }
        if (!eventId.matches("[A-Za-z0-9._~-]{1,64}") || !"1".equals(schemaVersion)
                || !Set.of("OAUTH", "IDENTITY").contains(auditKind)
                || !eventType.matches("[A-Za-z0-9._~:-]{1,128}")
                || !reasonCode.matches("[A-Za-z0-9._~:-]{1,128}")
                || !Set.of("SUCCESS", "FAILURE", "DENIED", "FAILED").contains(result)
                || !issuer.isAbsolute() || !"https".equalsIgnoreCase(issuer.getScheme())
                || issuer.getHost() == null || issuer.getUserInfo() != null || issuer.getQuery() != null
                || issuer.getFragment() != null) {
            throw new InvalidAuditEvent("OAUTH_AUDIT_EVENT_INVALID");
        }
        String resource = optional(fields, "resource");
        String subjectRef = optional(fields, "subjectRef");
        String clientRef = optional(fields, "clientRef");
        String tokenRef = optional(fields, "tokenRef");
        String actorSub = optional(fields, "actorSub");
        String targetSub = optional(fields, "targetSub");
        String identityTenantId = optional(fields, "identityTenantId");
        String traceId = optional(fields, "traceId");
        String sourceBucket = optional(fields, "sourceBucket");
        String signature = optional(fields, "signature");
        if (traceId != null && !traceId.matches("[A-Za-z0-9._~-]{1,128}")) {
            throw new InvalidAuditEvent("OAUTH_AUDIT_TRACE_INVALID");
        }
        if ("OAUTH".equals(auditKind)) {
            if (resource == null || resource.length() > 2048
                    || clientRef != null && !clientRef.matches("[0-9a-f]{64}")
                    || subjectRef != null && !subjectRef.matches("[0-9a-f]{64}")
                    || tokenRef != null && !tokenRef.matches("[0-9a-f]{64}")
                    || (tokenRef != null && signature == null || signature != null
                            && !durableAudit.verifySignature(issuer.toASCIIString(), tokenRef, signature))) {
                throw new InvalidAuditEvent("OAUTH_AUDIT_EVENT_REFERENCE_INVALID");
            }
        } else if (signature != null || actorSub == null || targetSub == null || sourceBucket == null
                || !actorSub.matches("[A-Za-z0-9._~-]{1,64}") || !targetSub.matches("[A-Za-z0-9._~-]{1,64}")
                || !sourceBucket.matches("[A-Za-z0-9._~:-]{1,128}")
                || traceId != null && !traceId.matches("[A-Za-z0-9._~-]{1,128}")
                || identityTenantId != null && !identityTenantId.matches("[A-Za-z0-9._~-]{1,64}")) {
            throw new InvalidAuditEvent("IDENTITY_AUDIT_EVENT_INVALID");
        }
        return new AuditEvent(eventId, issuer, auditKind, eventType, resource, subjectRef, clientRef,
                tokenRef, result, reasonCode, occurredAt, actorSub, targetSub, identityTenantId, traceId,
                sourceBucket);
    }

    private void moveToDeadLetter(
            RStream<String, String> stream,
            StreamMessageId id,
            Map<String, String> fields,
            String failureCode) {
        try {
            Map<String, String> deadLetter = new LinkedHashMap<>();
            if (fields != null) {
                fields.forEach((name, value) -> {
                    if (ALLOWED_FIELDS.contains(name)) {
                        deadLetter.put(name, value);
                    }
                });
            }
            deadLetter.put("failureCode", failureCode);
            StreamMessageId deadLetterId = redisson.<String, String>getStream(DEAD_STREAM, StringCodec.INSTANCE)
                    .add(StreamAddArgs.entries(deadLetter));
            if (deadLetterId == null) {
                throw new IllegalStateException("OAUTH_AUDIT_DEAD_LETTER_NOT_CONFIRMED");
            }
            if (fields != null && fields.get("signature") != null) {
                try {
                    AuditEvent event = parse(fields);
                    issuers.resolveExactIssuer(event.issuer());
                    publishReceipt(durableAudit.createReceipt(usageEvent(event), "DEAD", clock.instant()));
                } catch (InvalidAuditEvent | OAuth2AuthenticationException | IllegalArgumentException invalid) {
                    // Malformed or untrusted input cannot establish a fact that a producer may confirm.
                    log.warn("OAuth dead-letter has no reconstructable trusted receipt; failureType={}",
                            invalid.getClass().getSimpleName());
                }
            }
            stream.ack(GROUP, id);
            String eventId = fields == null ? null : fields.get("eventId");
            if (eventId != null && eventId.matches("[A-Za-z0-9._~-]{1,128}")) {
                attempts(eventId).delete();
            }
        } catch (RuntimeException failure) {
            log.error("OAuth audit dead-letter delivery failed; failureType={}",
                    failure.getClass().getSimpleName());
        }
    }

    private RAtomicLong attempts(String eventId) {
        return redisson.getAtomicLong("egon:oauth:audit:attempt:" + eventId);
    }

    private static String safeEventId(String value) {
        return value != null && value.matches("[A-Za-z0-9._~-]{1,64}") ? value : "invalid-event";
    }

    private static String required(Map<String, String> fields, String name) {
        String value = fields == null ? null : fields.get(name);
        if (value == null || value.isBlank()) {
            throw new InvalidAuditEvent("OAUTH_AUDIT_FIELD_REQUIRED");
        }
        return value;
    }

    private static String optional(Map<String, String> fields, String name) {
        String value = fields.get(name);
        return value == null || value.isBlank() ? null : value;
    }

    private static final class InvalidAuditEvent extends RuntimeException {
        private InvalidAuditEvent(String message) {
            super(message);
        }

        private InvalidAuditEvent(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record AuditEvent(
            String eventId,
            URI issuer,
            String auditKind,
            String eventType,
            String resource,
            String subjectRef,
            String clientRef,
            String tokenRef,
            String result,
            String reasonCode,
            Instant occurredAt,
            String actorSub,
            String targetSub,
            String identityTenantId,
            String traceId,
            String sourceBucket) {
    }
}
