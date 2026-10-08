package top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthAuthorizationMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthAuthorizationConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthAuthorizationPO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthAuthorizationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthPayloadCipherComponent;

@Repository("oAuthAuthorizationRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthAuthorizationRepositoryImpl extends EgonColaRepository<OAuthAuthorizationMapper, OAuthAuthorizationPO>
        implements OAuthAuthorizationRepository {

    private static final String SERVICE_ACTOR = "service:tianquan-shoubing";

    @Getter
    @Qualifier("OAuthAuthorizationMapper")
    private final OAuthAuthorizationMapper baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("OAuthAuthorizationConverterImpl")
    private final OAuthAuthorizationConverter converter;
    @Qualifier("oauthPayloadCipherComponent")
    private final OAuthPayloadCipherComponent payloadCipher;
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthAuthorizationBO> findById(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long id) {
        Objects.requireNonNull(id, "id");
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                        baseMapper.selectActiveById(tenantId, id))
                .map(value -> toTarget(trustedRealm, value)));
    }

    @Override
    @Transactional
    public Optional<OAuthAuthorizationBO> findByIdForUpdate(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long id) {
        Objects.requireNonNull(id, "id");
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                        baseMapper.selectByIdForUpdate(tenantId, id))
                .map(value -> toTarget(trustedRealm, value)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthAuthorizationBO> findByPrincipalName(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Size(max = 128) String principalName,
            @NotBlank @Pattern(regexp = "ACTIVE|REVOKED|EXPIRED") String status,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit) {
        String principal = required(principalName, "principalName");
        requireLimit(limit);
        return withTrustedStoragePartition(trustedRealm, tenantId -> baseMapper
                .selectByPrincipalName(tenantId, principal, status, afterId, limit).stream()
                .map(value -> toTarget(trustedRealm, value))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthAuthorizationBO> findExpiredBefore(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull Instant expiresBefore,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit) {
        Objects.requireNonNull(expiresBefore, "expiresBefore");
        requireLimit(limit);
        return withTrustedStoragePartition(trustedRealm, tenantId -> baseMapper
                .selectExpired(tenantId, expiresBefore, afterId, limit).stream()
                .map(value -> toTarget(trustedRealm, value))
                .toList());
    }

    @Override
    @Transactional
    public OAuthAuthorizationBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthAuthorizationBO authorization,
            @PositiveOrZero long expectedVersion) {
        Objects.requireNonNull(authorization, "authorization");
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            try {
                OAuthAuthorizationPO current = null;
                long id;
                if (authorization.getId() == null) {
                    if (expectedVersion != 0L || authorization.getVersion() != 0L) {
                        throw stale(expectedVersion, -1L);
                    }
                    id = SnowflakeIdGenerator.nextLongId();
                    if (id <= 0L) {
                        throw new IllegalStateException("OAUTH_AUTHORIZATION_ID_INVALID");
                    }
                } else {
                    id = authorization.getId();
                    current = baseMapper.selectByIdForUpdate(tenantId, id);
                    if (current == null) {
                        if (expectedVersion != 0L || authorization.getVersion() != 0L) {
                            throw stale(expectedVersion, -1L);
                        }
                    } else if (current.getVersion() != expectedVersion
                            || authorization.getVersion() != Math.addExact(expectedVersion, 1L)) {
                        throw stale(expectedVersion, current == null ? -1L : current.getVersion());
                    } else {
                        requireImmutableIdentity(current, authorization);
                        if (!Objects.equals(toTarget(trustedRealm, current).getAttributes(),
                                authorization.getAttributes())) {
                            throw new IllegalArgumentException("OAUTH_AUTHORIZATION_ATTRIBUTES_IMMUTABLE");
                        }
                    }
                }

                OAuthAuthorizationPO value = converter.toSource(
                        trustedRealm, payloadCipher, objectMapper, Long.toString(id), authorization);
                value.setId(id);
                value.setTenantId(tenantId);
                if (current == null) {
                    value.setVersion(0L);
                    if (expectedVersion != 0L || !super.save(value)) {
                        throw new IllegalStateException("OAUTH_AUTHORIZATION_INSERT_NOT_APPLIED");
                    }
                } else {
                    value.setVersion(Math.addExact(expectedVersion, 1L));
                    value.setUpdateUserId(userIdProvider.currentUserId());
                    value.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
                    if (baseMapper.updateExpectedVersion(tenantId, value, expectedVersion) != 1) {
                        throw stale(expectedVersion, current.getVersion());
                    }
                }
                return toTarget(trustedRealm, value);
            } catch (DataIntegrityViolationException failure) {
                throw new IllegalStateException("OAUTH_AUTHORIZATION_CONFLICT", failure);
            }
        });
    }

    @Override
    @Transactional
    public boolean deleteVersionedById(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long id,
            @PositiveOrZero long expectedVersion) {
        Objects.requireNonNull(id, "id");
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            OAuthAuthorizationPO value = new OAuthAuthorizationPO();
            value.setId(id);
            value.setTenantId(tenantId);
            value.setVersion(expectedVersion);
            value.setUpdateUserId(userIdProvider.currentUserId());
            value.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
            return baseMapper.deleteVersionedById(tenantId, value, expectedVersion) == 1;
        });
    }

    private OAuthAuthorizationBO toTarget(TrustedRealmBO trustedRealm, OAuthAuthorizationPO value) {
        return converter.toTarget(trustedRealm, payloadCipher, objectMapper, Long.toString(value.getId()), value);
    }

    private <T> T withTrustedStoragePartition(
            TrustedRealmBO trustedRealm,
            Function<Long, T> operation) {
        Objects.requireNonNull(trustedRealm, "trustedRealm").validateTrustedPartition();
        Objects.requireNonNull(operation, "operation");
        String tenantKey = properties.getTenantId().getMdcKey();
        String userKey = properties.getAudit().getUserIdMdcKey();
        if (tenantKey.equals(userKey)) {
            throw new IllegalStateException("OAUTH_AUTHORIZATION_MDC_KEYS_INVALID");
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
                throw new IllegalStateException("OAUTH_AUTHORIZATION_TENANT_CONTEXT_MISMATCH");
            }
            return operation.apply(tenantId);
        } finally {
            restoreMdc(tenantKey, previousTenant);
            restoreMdc(userKey, previousUser);
        }
    }

    private static void requireImmutableIdentity(
            OAuthAuthorizationPO current,
            OAuthAuthorizationBO requested) {
        boolean matches = Objects.equals(current.getClientPk(), requested.getClientPk())
                && Objects.equals(current.getPrincipalName(), requested.getPrincipalName())
                && Objects.equals(current.getPrincipalType(), requested.getPrincipalType())
                && Objects.equals(current.getGrantType(), requested.getGrantType())
                && Objects.equals(current.getAuthorizedScopes(), requested.getAuthorizedScopes())
                && Objects.equals(current.getAudiences(), requested.getAudiences())
                && Objects.equals(current.getAuthTime(), requested.getAuthTime())
                && Objects.equals(current.getAcr(), requested.getAcr())
                && Objects.equals(current.getAbsoluteExpiresAt(), requested.getAbsoluteExpiresAt());
        if (!matches) {
            throw new IllegalArgumentException("OAUTH_AUTHORIZATION_IDENTITY_IMMUTABLE");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException("OAUTH_AUTHORIZATION_" + field.toUpperCase() + "_INVALID");
        }
        return value;
    }

    private static void requireLimit(int limit) {
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("OAUTH_AUTHORIZATION_QUERY_LIMIT_INVALID");
        }
    }

    private static void restoreMdc(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    private static OptimisticLockingFailureException stale(long expectedVersion, long actualVersion) {
        return new OptimisticLockingFailureException(
                "authorization expected version " + expectedVersion + " but was " + actualVersion);
    }
}
