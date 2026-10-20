package top.egon.cola.platform.tianquan.shoubing.admin.token.repository.impl;

import java.time.Clock;
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
import top.egon.cola.platform.tianquan.shoubing.admin.token.dao.OAuthSecurityStateMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSecurityStateBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.converter.OAuthSecurityStateConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.po.OAuthSecurityStatePO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSecurityStateRepository;

@Repository("oAuthSecurityStateRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthSecurityStateRepositoryImpl extends EgonColaRepository<OAuthSecurityStateMapper, OAuthSecurityStatePO>
        implements OAuthSecurityStateRepository {

    private static final String SERVICE_ACTOR = "service:tianquan-shoubing";

    @Getter
    @Qualifier("OAuthSecurityStateMapper")
    private final OAuthSecurityStateMapper baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("OAuthSecurityStateConverterImpl")
    private final OAuthSecurityStateConverter converter;
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthSecurityStateBO> findByTarget(
            TrustedRealmBO trustedRealm,
            OAuthSecurityTargetEnum targetType,
            String targetKey) {
        String key = requireStateTarget(targetType, targetKey);
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                        baseMapper.selectByTarget(tenantId, targetType, key))
                .map(converter::toTarget));
    }

    @Override
    @Transactional
    public Optional<OAuthSecurityStateBO> findByTargetForUpdate(
            TrustedRealmBO trustedRealm,
            OAuthSecurityTargetEnum targetType,
            String targetKey) {
        String key = requireStateTarget(targetType, targetKey);
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                        baseMapper.selectByTargetForUpdate(tenantId, targetType, key))
                .map(converter::toTarget));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthSecurityStateBO> findByRevisionAfter(
            TrustedRealmBO trustedRealm,
            long afterRevision,
            long afterId,
            int limit) {
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("OAUTH_SECURITY_STATE_QUERY_LIMIT_INVALID");
        }
        return withTrustedStoragePartition(trustedRealm, tenantId -> baseMapper
                .selectByRevisionAfter(tenantId, afterRevision, afterId, limit).stream()
                .map(converter::toTarget)
                .toList());
    }

    @Override
    @Transactional
    public OAuthSecurityStateBO save(
            TrustedRealmBO trustedRealm,
            OAuthSecurityStateBO state,
            long expectedVersion) {
        Objects.requireNonNull(state, "state");
        String targetKey = requireStateTarget(state.getTargetType(), state.getTargetKey());
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            try {
                OAuthSecurityStatePO current = baseMapper.selectByTargetForUpdate(
                        tenantId, state.getTargetType(), targetKey);
                if (current == null) {
                    if (state.getId() != null || expectedVersion != 0L || state.getVersion() != 0L) {
                        throw stale(expectedVersion, -1L);
                    }
                    OAuthSecurityStatePO value = converter.toSource(state);
                    value.setTenantId(tenantId);
                    value.setVersion(0L);
                    if (!super.save(value)) {
                        throw new IllegalStateException("OAUTH_SECURITY_STATE_INSERT_NOT_APPLIED");
                    }
                    return converter.toTarget(value);
                }
                long nextVersion = Math.addExact(expectedVersion, 1L);
                if (current.getVersion() != expectedVersion || state.getVersion() != nextVersion
                        || !Objects.equals(state.getId(), current.getId())
                        || state.getGeneration() < current.getGeneration()
                        || state.getRevision() < current.getRevision()
                        || (state.isEnabled() != current.getEnabled()
                                && state.getGeneration() <= current.getGeneration())
                        || (state.getGeneration() > current.getGeneration()
                                && state.getRevision() <= current.getRevision())) {
                    throw stale(expectedVersion, current.getVersion());
                }
                OAuthSecurityStatePO value = converter.toSource(state);
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
                throw new IllegalStateException("OAUTH_SECURITY_STATE_CONFLICT", failure);
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
            throw new IllegalStateException("OAUTH_SECURITY_STATE_MDC_KEYS_INVALID");
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
                throw new IllegalStateException("OAUTH_SECURITY_STATE_TENANT_CONTEXT_MISMATCH");
            }
            return operation.apply(tenantId);
        } finally {
            restoreMdc(tenantKey, previousTenant);
            restoreMdc(userKey, previousUser);
        }
    }

    private static String requireStateTarget(OAuthSecurityTargetEnum targetType, String targetKey) {
        if (targetType == null || !targetType.isSecurityStateTarget()
                || targetKey == null || targetKey.isBlank() || !targetKey.equals(targetKey.trim())
                || targetKey.length() > 128) {
            throw new IllegalArgumentException("OAUTH_SECURITY_STATE_TARGET_INVALID");
        }
        boolean keyMatches = switch (targetType) {
            case REALM -> "self".equals(targetKey);
            case CLIENT -> isPositiveLong(targetKey);
            case SUBJECT -> targetKey.length() <= 64;
            default -> false;
        };
        if (!keyMatches) {
            throw new IllegalArgumentException("OAUTH_SECURITY_STATE_TARGET_INVALID");
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

    private static void restoreMdc(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    private static OptimisticLockingFailureException stale(long expectedVersion, long actualVersion) {
        return new OptimisticLockingFailureException(
                "security state expected version " + expectedVersion + " but was " + actualVersion);
    }
}
