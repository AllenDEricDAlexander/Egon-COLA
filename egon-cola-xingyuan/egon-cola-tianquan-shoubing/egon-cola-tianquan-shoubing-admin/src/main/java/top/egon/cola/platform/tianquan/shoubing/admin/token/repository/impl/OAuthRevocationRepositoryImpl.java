package top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;
import top.egon.cola.component.common.mybatis.business.EgonColaTenantIdProvider;
import top.egon.cola.component.common.mybatis.business.EgonColaUserIdProvider;
import top.egon.cola.component.common.mybatis.extension.EgonColaRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthRevocationMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthRevocationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthRevocationConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthRevocationPO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthRevocationRepository;

@Repository("oAuthRevocationRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthRevocationRepositoryImpl extends EgonColaRepository<OAuthRevocationMapper, OAuthRevocationPO>
        implements OAuthRevocationRepository {

    private static final String SERVICE_ACTOR = "service:tianquan-shoubing";

    @Getter
    @Qualifier("OAuthRevocationMapper")
    private final OAuthRevocationMapper baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("OAuthRevocationConverterImpl")
    private final OAuthRevocationConverter converter;
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthRevocationBO> findByTarget(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull OAuthSecurityTargetEnum targetType,
            @NotBlank @Size(max = 128) String targetKey) {
        String key = requireRevocationTarget(targetType, targetKey);
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                        baseMapper.selectByTarget(tenantId, targetType, key))
                .map(converter::toTarget));
    }

    @Override
    @Transactional
    public Optional<OAuthRevocationBO> findByTargetForUpdate(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull OAuthSecurityTargetEnum targetType,
            @NotBlank @Size(max = 128) String targetKey) {
        String key = requireRevocationTarget(targetType, targetKey);
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                        baseMapper.selectByTargetForUpdate(tenantId, targetType, key))
                .map(converter::toTarget));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthRevocationBO> findByProjectionStatus(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Size(max = 16) String projectionStatus,
            @PositiveOrZero long afterRevision,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit) {
        String status = requireProjectionStatus(projectionStatus);
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("OAUTH_REVOCATION_QUERY_LIMIT_INVALID");
        }
        return withTrustedStoragePartition(trustedRealm, tenantId -> baseMapper
                .selectByProjectionStatus(tenantId, status, afterRevision, afterId, limit).stream()
                .map(converter::toTarget)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthRevocationBO> findExpiredBefore(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull Instant cutoff,
            @PositiveOrZero long afterId,
            @Min(1) @Max(500) int limit) {
        Objects.requireNonNull(cutoff, "cutoff");
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("OAUTH_REVOCATION_QUERY_LIMIT_INVALID");
        }
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
            OAuthRevocationPO current = baseMapper.selectActiveById(tenantId, id);
            if (current == null) {
                return false;
            }
            current.setUpdateUserId(userIdProvider.currentUserId());
            current.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
            return baseMapper.deleteVersionedById(tenantId, current, expectedVersion) == 1;
        });
    }

    @Override
    @Transactional
    public OAuthRevocationBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthRevocationBO revocation,
            @PositiveOrZero long expectedVersion) {
        Objects.requireNonNull(revocation, "revocation");
        String targetKey = requireRevocationTarget(revocation.getTargetType(), revocation.getTargetKey());
        requireProjectionStatus(revocation.getProjectionStatus());
        Objects.requireNonNull(revocation.getRevokedAt(), "revokedAt");
        if (revocation.getExpiresAt() != null && !revocation.getExpiresAt().isAfter(revocation.getRevokedAt())) {
            throw new IllegalArgumentException("OAUTH_REVOCATION_EXPIRY_INVALID");
        }
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            try {
                OAuthRevocationPO current = baseMapper.selectByTargetForUpdate(
                        tenantId, revocation.getTargetType(), targetKey);
                OAuthRevocationPO value = converter.toSource(revocation);
                value.setTenantId(tenantId);
                if (current == null) {
                    if (revocation.getId() != null || expectedVersion != 0L || revocation.getVersion() != 0L
                            || !"PENDING".equals(revocation.getProjectionStatus())) {
                        throw stale(expectedVersion, -1L);
                    }
                    value.setVersion(0L);
                    if (!super.save(value)) {
                        throw new IllegalStateException("OAUTH_REVOCATION_INSERT_NOT_APPLIED");
                    }
                    return converter.toTarget(value);
                }
                long nextVersion = Math.addExact(expectedVersion, 1L);
                if (current.getVersion() != expectedVersion || revocation.getVersion() != nextVersion
                        || !Objects.equals(revocation.getId(), current.getId())
                        || revocation.getTargetType() != current.getTargetType()
                        || !Objects.equals(revocation.getTargetKey(), current.getTargetKey())
                        || !Objects.equals(revocation.getReasonCode(), current.getReasonCode())
                        || !Objects.equals(revocation.getRevokedAt(), current.getRevokedAt())
                        || expiryWasShortened(current.getExpiresAt(), revocation.getExpiresAt())
                        || revocation.getRevision() < current.getRevision()
                        || "APPLIED".equals(current.getProjectionStatus())
                                && "PENDING".equals(revocation.getProjectionStatus())) {
                    throw stale(expectedVersion, current.getVersion());
                }
                value.setId(current.getId());
                value.setTenantId(tenantId);
                value.setVersion(nextVersion);
                value.setUpdateUserId(userIdProvider.currentUserId());
                value.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
                if (baseMapper.updateExpectedVersion(tenantId, value, expectedVersion) != 1) {
                    throw stale(expectedVersion, current.getVersion());
                }
                return converter.toTarget(value);
            } catch (DataIntegrityViolationException failure) {
                throw new IllegalStateException("OAUTH_REVOCATION_CONFLICT", failure);
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
            throw new IllegalStateException("OAUTH_REVOCATION_MDC_KEYS_INVALID");
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
                throw new IllegalStateException("OAUTH_REVOCATION_TENANT_CONTEXT_MISMATCH");
            }
            return operation.apply(tenantId);
        } finally {
            restoreMdc(tenantKey, previousTenant);
            restoreMdc(userKey, previousUser);
        }
    }

    private static String requireRevocationTarget(OAuthSecurityTargetEnum targetType, String targetKey) {
        if (targetType == null || !targetType.isRevocationTarget()
                || targetKey == null || targetKey.isBlank() || !targetKey.equals(targetKey.trim())
                || targetKey.length() > 128) {
            throw new IllegalArgumentException("OAUTH_REVOCATION_TARGET_INVALID");
        }
        boolean keyMatches = switch (targetType) {
            case JTI -> targetKey.matches("[0-9]{1,64}");
            case AUTHORIZATION -> isPositiveLong(targetKey);
            case KID -> targetKey.matches("[A-Za-z0-9_-]{1,128}");
            default -> false;
        };
        if (!keyMatches) {
            throw new IllegalArgumentException("OAUTH_REVOCATION_TARGET_INVALID");
        }
        return targetKey;
    }

    private static boolean isPositiveLong(String value) {
        if (!value.matches("[1-9][0-9]{0,18}")) {
            return false;
        }
        try {
            return Long.parseLong(value) > 0L;
        } catch (NumberFormatException invalid) {
            return false;
        }
    }

    private static String requireProjectionStatus(String status) {
        if (!"PENDING".equals(status) && !"APPLIED".equals(status)) {
            throw new IllegalArgumentException("OAUTH_REVOCATION_PROJECTION_STATUS_INVALID");
        }
        return status;
    }

    private static boolean expiryWasShortened(Instant current, Instant requested) {
        return current == null ? requested != null : requested != null && requested.isBefore(current);
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
                "revocation expected version " + expectedVersion + " but was " + actualVersion);
    }
}
