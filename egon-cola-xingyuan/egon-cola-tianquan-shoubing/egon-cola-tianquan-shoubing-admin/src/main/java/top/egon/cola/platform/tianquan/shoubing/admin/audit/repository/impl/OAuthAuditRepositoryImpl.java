package top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.impl;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.dao.OAuthAuditMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.converter.OAuthAuditConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.dto.IdentityAuditQueryDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.po.OAuthAuditPO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;

@Repository("oAuthAuditRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthAuditRepositoryImpl extends EgonColaRepository<OAuthAuditMapper, OAuthAuditPO>
        implements OAuthAuditRepository {

    private static final String SERVICE_ACTOR = "service:tianquan-shoubing";
    private static final int MAXIMUM_IDENTITY_PAGE_SIZE = 200;
    private static final int MAXIMUM_OAUTH_QUERY_WINDOW_DAYS = 31;
    private static final int MAXIMUM_PAYLOAD_BYTES = 16_384;
    private static final Set<String> FORBIDDEN_AUDIT_PAYLOAD_KEYS = Set.of(
            "authorization", "cookie", "code", "codeverifier", "codechallenge", "nonce", "verifier", "jti",
            "token", "accesstoken", "refreshtoken", "idtoken");

    @Getter
    @Qualifier("OAuthAuditMapper")
    private final OAuthAuditMapper baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("OAuthAuditConverterImpl")
    private final OAuthAuditConverter converter;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthAuditBO> findByEventId(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Size(max = 64) String eventId) {
        String value = required(eventId, "eventId", 64);
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                        baseMapper.selectByEventId(tenantId, value))
                .map(converter::toTarget));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthAuditBO> findByOccurredAt(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull Instant fromInclusive,
            @NotNull Instant toExclusive,
            Instant afterOccurredAt,
            @PositiveOrZero Long afterId,
            @Min(1) @Max(500) int limit) {
        validateWindow(fromInclusive, toExclusive);
        validateCursor(afterOccurredAt, afterId);
        requireLimit(limit, 500);
        return withTrustedStoragePartition(trustedRealm, tenantId -> baseMapper
                .selectByOccurredAt(tenantId, fromInclusive, toExclusive, afterOccurredAt, afterId, limit).stream()
                .map(converter::toTarget)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthAuditBO> findExpiredBefore(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull Instant cutoff,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit) {
        Objects.requireNonNull(cutoff, "cutoff");
        requireLimit(limit, 500);
        return withTrustedStoragePartition(trustedRealm, tenantId -> baseMapper
                .selectExpired(tenantId, cutoff, afterId, limit).stream()
                .map(converter::toTarget)
                .toList());
    }

    @Override
    @Transactional
    public boolean deleteVersionedById(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Positive Long id,
            @PositiveOrZero long expectedVersion) {
        Objects.requireNonNull(id, "id");
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            OAuthAuditPO current = baseMapper.selectActiveById(tenantId, id);
            if (current == null) {
                return false;
            }
            String userKey = properties.getAudit().getUserIdMdcKey();
            current.setUpdateUserId(Objects.requireNonNull(MDC.get(userKey), "trustedAuditActor"));
            current.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
            return baseMapper.deleteVersionedById(tenantId, current, expectedVersion) == 1;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthAuditBO> findIdentityPage(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotNull @Valid IdentityAuditQueryDTO query) {
        validateIdentityQuery(platformRealm, query);
        long offset = Math.multiplyExact((long) query.page(), query.size());
        return withPlatformStoragePartition(platformRealm, tenantId -> baseMapper
                .selectIdentityPage(tenantId, query.actorSub(), query.eventType(), query.result(),
                        query.traceId(), query.from(), query.to(), offset, query.size())
                .stream()
                .map(converter::toTarget)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countIdentityPage(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotNull @Valid IdentityAuditQueryDTO query) {
        validateIdentityQuery(platformRealm, query);
        return withPlatformStoragePartition(platformRealm, tenantId -> baseMapper.countIdentityPage(
                tenantId, query.actorSub(), query.eventType(), query.result(), query.traceId(), query.from(), query.to()));
    }

    @Override
    @Transactional
    public OAuthAuditBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthAuditBO audit,
            @PositiveOrZero long expectedVersion) {
        Objects.requireNonNull(audit, "audit");
        validateAudit(trustedRealm, audit);
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            try {
                OAuthAuditPO current = baseMapper.selectByEventIdForUpdate(tenantId, audit.getEventId());
                if (current != null) {
                    if (expectedVersion != current.getVersion() || audit.getVersion() != current.getVersion()
                            || audit.getId() != null && !Objects.equals(audit.getId(), current.getId())
                            || !sameEvent(current, audit)) {
                        throw new IllegalStateException("OAUTH_AUDIT_EVENT_CONFLICT");
                    }
                    return converter.toTarget(current);
                }
                if (audit.getId() != null || expectedVersion != 0L || audit.getVersion() != 0L) {
                    throw new IllegalStateException("OAUTH_AUDIT_EXPECTED_VERSION_CONFLICT");
                }
                OAuthAuditPO value = converter.toSource(audit);
                value.setTenantId(tenantId);
                value.setVersion(0L);
                if (!super.save(value)) {
                    throw new IllegalStateException("OAUTH_AUDIT_INSERT_NOT_APPLIED");
                }
                return converter.toTarget(value);
            } catch (DataIntegrityViolationException failure) {
                throw new IllegalStateException("OAUTH_AUDIT_CONFLICT", failure);
            }
        });
    }

    private <T> T withTrustedStoragePartition(
            TrustedRealmBO trustedRealm,
            Function<Long, T> operation) {
        Objects.requireNonNull(trustedRealm, "trustedRealm").validateTrustedPartition();
        Objects.requireNonNull(operation, "operation");
        String tenantKey = properties.getTenantId().getMdcKey();
        String userKey = properties.getAudit().getUserIdMdcKey();
        if (tenantKey.equals(userKey)) {
            throw new IllegalStateException("OAUTH_AUDIT_MDC_KEYS_INVALID");
        }
        String previousTenant = MDC.get(tenantKey);
        String previousUser = MDC.get(userKey);
        try {
            MDC.put(tenantKey, Long.toString(trustedRealm.getStoragePartitionId()));
            if (previousUser == null || previousUser.isBlank()) {
                MDC.put(userKey, SERVICE_ACTOR);
            }
            Long tenantId = EgonColaTenantIdProvider.currentTenantId();
            if (!tenantId.equals(trustedRealm.getStoragePartitionId())) {
                throw new IllegalStateException("OAUTH_AUDIT_TENANT_CONTEXT_MISMATCH");
            }
            return operation.apply(tenantId);
        } finally {
            restoreMdc(tenantKey, previousTenant);
            restoreMdc(userKey, previousUser);
        }
    }

    private <T> T withPlatformStoragePartition(
            TrustedRealmBO trustedRealm,
            Function<Long, T> operation) {
        if (trustedRealm.getRealmType() != RealmTypeEnum.PLATFORM) {
            throw new IllegalArgumentException("OAUTH_IDENTITY_AUDIT_PLATFORM_REALM_REQUIRED");
        }
        return withTrustedStoragePartition(trustedRealm, operation);
    }

    private static void validateIdentityQuery(TrustedRealmBO platformRealm, IdentityAuditQueryDTO query) {
        Objects.requireNonNull(platformRealm, "platformRealm").validateTrustedPartition();
        Objects.requireNonNull(query, "query");
        if (platformRealm.getRealmType() != RealmTypeEnum.PLATFORM
                || query.page() < 0 || query.size() < 1 || query.size() > MAXIMUM_IDENTITY_PAGE_SIZE
                || query.from() != null && query.to() != null && !query.to().isAfter(query.from())) {
            throw new IllegalArgumentException("IDENTITY_AUDIT_QUERY_INVALID");
        }
        validateOptionalFilter(query.actorSub(), 64, "actorSub");
        validateOptionalFilter(query.eventType(), 128, "eventType");
        validateOptionalFilter(query.result(), 16, "result");
        validateOptionalFilter(query.traceId(), 128, "traceId");
    }

    private static void validateAudit(TrustedRealmBO trustedRealm, OAuthAuditBO audit) {
        Objects.requireNonNull(trustedRealm, "trustedRealm").validateTrustedPartition();
        required(audit.getEventId(), "eventId", 64);
        required(audit.getEventType(), "eventType", 128);
        required(audit.getResult(), "result", 16);
        required(audit.getReasonCode(), "reasonCode", 128);
        required(audit.getAuditKind(), "auditKind", 16);
        required(audit.getPublicId(), "publicId", 64);
        Objects.requireNonNull(audit.getOccurredAt(), "occurredAt");
        if (!Set.of("OAUTH", "IDENTITY").contains(audit.getAuditKind())
                || !Set.of("SUCCESS", "DENIED", "FAILED", "FAILURE").contains(audit.getResult())) {
            throw new IllegalArgumentException("OAUTH_AUDIT_EVENT_ENUM_INVALID");
        }
        if ("IDENTITY".equals(audit.getAuditKind())
                && trustedRealm.getRealmType() != RealmTypeEnum.PLATFORM) {
            throw new IllegalArgumentException("OAUTH_IDENTITY_AUDIT_PLATFORM_REALM_REQUIRED");
        }
        JsonNode payload = Objects.requireNonNull(audit.getPayload(), "payload");
        if (!payload.isObject() || payload.toString().getBytes(StandardCharsets.UTF_8).length > MAXIMUM_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("OAUTH_AUDIT_PAYLOAD_INVALID");
        }
        if (payload.size() != 0
                && (!payload.path("schemaVersion").canConvertToInt()
                        || payload.path("schemaVersion").asInt() != 1)) {
            throw new IllegalArgumentException("OAUTH_AUDIT_PAYLOAD_SCHEMA_UNSUPPORTED");
        }
        requireSafePayloadKeys(payload);
        if (audit.getTokenRef() != null && !audit.getTokenRef().matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("OAUTH_AUDIT_TOKEN_REF_INVALID");
        }
    }

    private static void requireSafePayloadKeys(JsonNode value) {
        if (value.isObject()) {
            Iterator<String> names = value.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                String normalized = name.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
                if (normalized.contains("password") || normalized.contains("secret")
                        || normalized.contains("privatekey") || FORBIDDEN_AUDIT_PAYLOAD_KEYS.contains(normalized)) {
                    throw new IllegalArgumentException("OAUTH_AUDIT_PAYLOAD_SENSITIVE_FIELD_FORBIDDEN");
                }
                requireSafePayloadKeys(value.get(name));
            }
        } else if (value.isArray()) {
            for (JsonNode element : value) {
                requireSafePayloadKeys(element);
            }
        }
    }

    private static boolean sameEvent(OAuthAuditPO current, OAuthAuditBO requested) {
        return Objects.equals(current.getEventId(), requested.getEventId())
                && Objects.equals(current.getEventType(), requested.getEventType())
                && Objects.equals(current.getSubjectRef(), requested.getSubjectRef())
                && Objects.equals(current.getClientRef(), requested.getClientRef())
                && Objects.equals(current.getTokenRef(), requested.getTokenRef())
                && Objects.equals(current.getResourceRef(), requested.getResourceRef())
                && Objects.equals(current.getResult(), requested.getResult())
                && Objects.equals(current.getReasonCode(), requested.getReasonCode())
                && Objects.equals(current.getTraceId(), requested.getTraceId())
                && Objects.equals(current.getOccurredAt(), requested.getOccurredAt())
                && Objects.equals(current.getPayload(), requested.getPayload())
                && Objects.equals(current.getAuditKind(), requested.getAuditKind())
                && Objects.equals(current.getPublicId(), requested.getPublicId())
                && Objects.equals(current.getActorSub(), requested.getActorSub())
                && Objects.equals(current.getTargetSub(), requested.getTargetSub())
                && Objects.equals(current.getIdentityTenantId(), requested.getIdentityTenantId());
    }

    private static String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || !value.equals(value.trim()) || value.length() > maxLength) {
            throw new IllegalArgumentException("OAUTH_AUDIT_" + field.toUpperCase() + "_INVALID");
        }
        return value;
    }

    private static void validateOptionalFilter(String value, int maxLength, String field) {
        if (value != null && (value.isBlank() || !value.equals(value.trim()) || value.length() > maxLength)) {
            throw new IllegalArgumentException("IDENTITY_AUDIT_" + field.toUpperCase() + "_INVALID");
        }
    }

    private static void validateWindow(Instant fromInclusive, Instant toExclusive) {
        Objects.requireNonNull(fromInclusive, "fromInclusive");
        Objects.requireNonNull(toExclusive, "toExclusive");
        if (!toExclusive.isAfter(fromInclusive)
                || Duration.between(fromInclusive, toExclusive).compareTo(
                Duration.ofDays(MAXIMUM_OAUTH_QUERY_WINDOW_DAYS)) > 0) {
            throw new IllegalArgumentException("OAUTH_AUDIT_QUERY_WINDOW_INVALID");
        }
    }

    private static void validateCursor(Instant afterOccurredAt, Long afterId) {
        if ((afterOccurredAt == null) != (afterId == null) || afterId != null && afterId < 0L) {
            throw new IllegalArgumentException("OAUTH_AUDIT_QUERY_CURSOR_INVALID");
        }
    }

    private static void requireLimit(int limit, int maximum) {
        if (limit < 1 || limit > maximum) {
            throw new IllegalArgumentException("OAUTH_AUDIT_QUERY_LIMIT_INVALID");
        }
    }

    private static void restoreMdc(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }
}
