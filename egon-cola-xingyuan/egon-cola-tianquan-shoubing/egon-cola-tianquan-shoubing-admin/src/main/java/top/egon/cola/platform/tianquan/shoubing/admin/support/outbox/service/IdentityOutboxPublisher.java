package top.egon.cola.platform.tianquan.shoubing.admin.support.outbox.service;

import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityUserState;
import top.egon.cola.platform.tianquan.shoubing.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.tianquan.shoubing.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.service.IdentityStateProjection;
import top.egon.cola.platform.tianquan.shoubing.core.port.IdentityUserStatePort;

/** Projects identity state after commit and persists identity audit facts through MP and component Outbox. */
@Component("identityOutboxPublisher")
@RequiredArgsConstructor
@Slf4j
public class IdentityOutboxPublisher implements IdentityUserStatePort, IdentitySecurityEventPort, IdentityStateProjection {

    private static final String AUDIT_CHANNEL = "oauth-audit";
    private static final String AUDIT_DESTINATION = "oauth.audit.identity.v1";
    private static final Pattern SAFE_SUBJECT = Pattern.compile("[A-Za-z0-9._~-]{1,64}");

    @Qualifier("oAuthAuditRepository")
    private final OAuthAuditRepository audits;
    @Qualifier("transactionalOutbox")
    private final TransactionalOutbox outbox;
    @Qualifier("rbac3RuntimeRedissonClient")
    private final RedissonClient redisson;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("oauthManagementRealmResolver")
    private final OAuthManagementRealmResolver realmResolver;
    @Value("${egon.tianquan-shoubing.identity-state-key-prefix:identity:v1:user:}")
    private final String stateKeyPrefix;
    @Qualifier("idpClock")
    private final Clock clock;

    @PostConstruct
    void validateConfiguration() {
        validPrefix(stateKeyPrefix);
    }

    @Override
    @Transactional
    public void publish(IdentityUserState state) {
        IdentityUserState value = Objects.requireNonNull(state, "state");
        String subject = subject(value.subject());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    project(subject, value);
                }
            });
        } else {
            project(subject, value);
        }
    }

    @Override
    public void project(IdentityUserState state) {
        IdentityUserState value = Objects.requireNonNull(state, "state");
        project(subject(value.subject()), value);
    }

    @Override
    @Transactional
    public void append(IdentitySecurityEvent event) {
        IdentitySecurityEvent value = Objects.requireNonNull(event, "event");
        String subject = subject(value.identitySub());
        String eventType = eventValue(value.eventType(), "eventType");
        String reason = eventValue(value.reason(), "reason");
        String sourceBucket = eventValue(value.sourceBucket(), "sourceBucket");
        Objects.requireNonNull(value.occurredAt(), "occurredAt");
        String eventId = nextId();
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.matches("[A-Za-z0-9._~-]{1,128}")) {
            traceId = null;
        }
        TrustedRealmBO platformRealm = realmResolver.platformRealm();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", 1);
        payload.put("sourceBucket", sourceBucket);
        OAuthAuditBO audit = OAuthAuditBO.builder()
                .version(0L)
                .eventId(eventId)
                .eventType(eventType)
                .actorSub(subject)
                .targetSub(subject)
                .result(eventType.endsWith("FAILED") ? "FAILURE" : "SUCCESS")
                .reasonCode(reason)
                .traceId(traceId)
                .occurredAt(value.occurredAt())
                .payload(objectMapper.valueToTree(payload))
                .auditKind("IDENTITY")
                .publicId(eventId)
                .build();
        audits.save(platformRealm, audit, 0L);

        Map<String, Object> streamEvent = new LinkedHashMap<>();
        streamEvent.put("eventId", eventId);
        streamEvent.put("schemaVersion", "1");
        streamEvent.put("issuer", platformRealm.getIssuer().toASCIIString());
        streamEvent.put("auditKind", "IDENTITY");
        streamEvent.put("eventType", eventType);
        streamEvent.put("actorSub", subject);
        streamEvent.put("targetSub", subject);
        streamEvent.put("result", audit.getResult());
        streamEvent.put("reasonCode", reason);
        streamEvent.put("sourceBucket", sourceBucket);
        streamEvent.put("occurredAt", value.occurredAt().toString());
        if (traceId != null) {
            streamEvent.put("traceId", traceId);
        }
        outbox.enqueue(OutboxMessage.builder()
                .messageId(eventId)
                .idempotencyKey("identity-audit:" + eventId)
                .channel(AUDIT_CHANNEL)
                .destination(AUDIT_DESTINATION)
                .payload(streamEvent)
                .schemaVersion("1")
                .availableAt(clock.instant())
                .build());
    }

    private void project(String subject, IdentityUserState state) {
        Map<String, String> payload = Map.of(
                "subject", subject,
                "status", state.status().name(),
                "updatedAt", state.updatedAt().toString());
        RBucket<String> bucket = redisson.getBucket(stateKeyPrefix + subject, StringCodec.INSTANCE);
        try {
            bucket.set(objectMapper.writeValueAsString(payload));
        } catch (com.fasterxml.jackson.core.JsonProcessingException failure) {
            throw new IllegalStateException("IDENTITY_STATE_SERIALIZATION_FAILED", failure);
        }
    }

    private String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String subject(String value) {
        if (value == null || !SAFE_SUBJECT.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid identity subject");
        }
        return value;
    }

    private static String eventValue(String value, String field) {
        if (value == null || value.isBlank() || value.length() > 128
                || !value.matches("[A-Za-z0-9._~:-]+")) {
            throw new IllegalArgumentException("invalid " + field);
        }
        return value;
    }

    private static String validPrefix(String value) {
        if (value == null || value.isBlank() || !value.endsWith(":") || value.contains(" ")) {
            throw new IllegalArgumentException("invalid state key prefix");
        }
        return value;
    }
}
