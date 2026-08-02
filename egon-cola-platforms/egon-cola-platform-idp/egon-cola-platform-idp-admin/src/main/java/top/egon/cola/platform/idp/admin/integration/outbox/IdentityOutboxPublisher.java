package top.egon.cola.platform.idp.admin.integration.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.idp.admin.audit.domain.IdentityAuditLogEntity;
import top.egon.cola.platform.idp.admin.audit.infrastructure.IdentityAuditLogRepository;
import top.egon.cola.platform.idp.admin.identity.application.IdentityUserStateReconciler;
import top.egon.cola.platform.idp.admin.outbox.domain.IdentityOutboxEventEntity;
import top.egon.cola.platform.idp.admin.outbox.infrastructure.IdentityOutboxEventRepository;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.idp.core.port.IdentityUserStatePort;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Projects public identity state and persists durable security events.
 */
public class IdentityOutboxPublisher
        implements IdentityUserStatePort,
        IdentitySecurityEventPort,
        IdentityUserStateReconciler.StateProjection {

    private static final Pattern SAFE_SUBJECT = Pattern.compile(
            "[A-Za-z0-9._~-]{1,64}"
    );

    private final IdentityOutboxEventRepository outbox;
    private final IdentityAuditLogRepository audits;
    private final RefreshTokenStore refreshTokens;
    private final RedissonClient redisson;
    private final ObjectMapper objectMapper;
    private final Supplier<String> idGenerator;
    private final String stateKeyPrefix;
    private final Clock clock;

    public IdentityOutboxPublisher(
            IdentityOutboxEventRepository outbox,
            IdentityAuditLogRepository audits,
            RefreshTokenStore refreshTokens,
            RedissonClient redisson,
            ObjectMapper objectMapper,
            Supplier<String> idGenerator,
            String stateKeyPrefix
    ) {
        this(
                outbox,
                audits,
                refreshTokens,
                redisson,
                objectMapper,
                idGenerator,
                stateKeyPrefix,
                Clock.systemUTC()
        );
    }

    public IdentityOutboxPublisher(
            IdentityOutboxEventRepository outbox,
            IdentityAuditLogRepository audits,
            RefreshTokenStore refreshTokens,
            RedissonClient redisson,
            ObjectMapper objectMapper,
            Supplier<String> idGenerator,
            String stateKeyPrefix,
            Clock clock
    ) {
        this.outbox = Objects.requireNonNull(outbox, "outbox");
        this.audits = Objects.requireNonNull(audits, "audits");
        this.refreshTokens = Objects.requireNonNull(
                refreshTokens,
                "refreshTokens"
        );
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.stateKeyPrefix = validPrefix(stateKeyPrefix);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    @Transactional
    public void publish(IdentityUserState state) {
        Objects.requireNonNull(state, "state");
        String subject = subject(state.subject());
        String payload = statePayload(state, subject);
        persistOutbox(
                subject,
                "IDENTITY_USER_STATE_CHANGED",
                payload,
                state.updatedAt()
        );
        project(subject, payload);
    }

    @Override
    public void project(IdentityUserState state) {
        Objects.requireNonNull(state, "state");
        String subject = subject(state.subject());
        project(subject, statePayload(state, subject));
    }

    private void project(String subject, String payload) {
        redisson.<String>getBucket(
                stateKeyPrefix + subject,
                StringCodec.INSTANCE
        ).set(payload);
    }

    private String statePayload(IdentityUserState state, String subject) {
        return json(Map.of(
                "subject", subject,
                "status", state.status().name(),
                "tokenVersion", state.tokenVersion(),
                "updatedAt", state.updatedAt().toString()
        ));
    }

    @Override
    @Transactional
    public void revokeFamilies(
            String identitySub,
            long tokenVersion,
            String reason
    ) {
        String subject = subject(identitySub);
        if (tokenVersion < 0L) {
            throw new IllegalArgumentException("invalid tokenVersion");
        }
        String safeReason = eventValue(reason, "reason");
        Instant now = clock.instant();
        refreshTokens.revokeSubject(subject, safeReason, now);
        persistOutbox(
                subject,
                "IDENTITY_REFRESH_FAMILIES_REVOKED",
                json(Map.of(
                        "subject", subject,
                        "tokenVersion", tokenVersion,
                        "reason", safeReason,
                        "occurredAt", now.toString()
                )),
                now
        );
    }

    @Override
    @Transactional
    public void append(IdentitySecurityEvent event) {
        Objects.requireNonNull(event, "event");
        String subject = subject(event.identitySub());
        String eventType = eventValue(event.eventType(), "eventType");
        String reason = eventValue(event.reason(), "reason");
        String sourceBucket = eventValue(
                event.sourceBucket(),
                "sourceBucket"
        );
        if (event.tokenVersion() < 0L) {
            throw new IllegalArgumentException("invalid tokenVersion");
        }
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("subject", subject);
        attributes.put("reason", reason);
        attributes.put("sourceBucket", sourceBucket);
        attributes.put("tokenVersion", event.tokenVersion());
        attributes.put("occurredAt", event.occurredAt().toString());
        String payload = json(attributes);
        audits.save(IdentityAuditLogEntity.record(
                nextId(),
                eventType,
                subject,
                subject,
                eventType.endsWith("FAILED") ? "FAILURE" : "SUCCESS",
                reason,
                payload,
                event.occurredAt()
        ));
        persistOutbox(subject, eventType, payload, event.occurredAt());
    }

    private void persistOutbox(
            String aggregateId,
            String eventType,
            String payload,
            Instant occurredAt
    ) {
        outbox.save(IdentityOutboxEventEntity.pending(
                nextId(),
                "IDENTITY_USER",
                aggregateId,
                eventType,
                payload,
                occurredAt
        ));
    }

    private String nextId() {
        String value = idGenerator.get();
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalStateException("ID generator returned invalid value");
        }
        return value;
    }

    private String json(Map<String, ?> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "identity event serialization failed",
                    exception
            );
        }
    }

    private static String subject(String value) {
        if (value == null || !SAFE_SUBJECT.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid identity subject");
        }
        return value;
    }

    private static String eventValue(String value, String field) {
        if (value == null
                || value.isBlank()
                || value.length() > 128
                || !value.matches("[A-Za-z0-9._~-]+")) {
            throw new IllegalArgumentException("invalid " + field);
        }
        return value;
    }

    private static String validPrefix(String value) {
        if (value == null
                || value.isBlank()
                || !value.endsWith(":")
                || value.contains(" ")) {
            throw new IllegalArgumentException("invalid state key prefix");
        }
        return value;
    }
}
