package top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthRealmMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthRealmConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthRealmPO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Repository("oAuthRealmRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthRealmRepositoryImpl extends EgonColaRepository<OAuthRealmMapper, OAuthRealmPO>
        implements OAuthRealmRepository {

    private static final String SERVICE_ACTOR = "service:tianquan-shoubing";

    @Getter
    @Qualifier("OAuthRealmMapper")
    private final OAuthRealmMapper baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("OAuthRealmConverterImpl")
    private final OAuthRealmConverter converter;
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthRealmBO> findByIssuer(@NotNull @Valid TrustedRealmBO trustedRealm) {
        return withTrustedStoragePartition(trustedRealm, tenantId ->
                Optional.ofNullable(baseMapper.selectByIssuer(
                                tenantId, trustedRealm.getIssuer().toASCIIString()))
                        .map(converter::toTarget));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthRealmBO> findByRealmKey(@NotNull @Valid TrustedRealmBO trustedRealm) {
        return withTrustedStoragePartition(trustedRealm, tenantId ->
                Optional.ofNullable(baseMapper.selectByRealmKey(
                                tenantId, trustedRealm.getRealmKey()))
                        .map(converter::toTarget));
    }

    @Override
    @Transactional
    public Optional<OAuthRealmBO> findByRealmKeyForUpdate(@NotNull @Valid TrustedRealmBO trustedRealm) {
        return withTrustedStoragePartition(trustedRealm, tenantId ->
                Optional.ofNullable(baseMapper.selectByRealmKeyForUpdate(
                                tenantId, trustedRealm.getRealmKey()))
                        .map(converter::toTarget));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthRealmBO> findAllByStoragePartition(@NotNull @Valid TrustedRealmBO trustedRealm) {
        return withTrustedStoragePartition(trustedRealm, tenantId -> baseMapper
                .selectDirectory(tenantId).stream()
                .map(converter::toTarget)
                .toList());
    }

    @Override
    @Transactional
    public OAuthRealmBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthRealmBO realm,
            @PositiveOrZero long expectedVersion) {
        Objects.requireNonNull(realm, "realm");
        trustedRealm.validateTrustedPartition();
        requireMatchingTrustedRealm(trustedRealm, realm);
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            try {
                OAuthRealmPO current = baseMapper.selectByRealmKeyForUpdate(
                        tenantId, trustedRealm.getRealmKey());
                OAuthRealmPO value = converter.toSource(realm);
                value.setTenantId(tenantId);
                if (current == null) {
                    if (expectedVersion != 0L || realm.getVersion() != 0L || realm.getId() != null) {
                        throw stale(trustedRealm.getRealmKey(), expectedVersion, -1L);
                    }
                    if (!super.save(value)) {
                        throw new IllegalStateException("OAUTH_REALM_INSERT_NOT_APPLIED");
                    }
                    return converter.toTarget(value);
                }

                requireCurrentMatchesTrustedRealm(trustedRealm, current);
                if (realm.getTrustVersion() < current.getTrustVersion()) {
                    throw new IllegalStateException("OAUTH_REALM_TRUST_VERSION_REGRESSION");
                }
                long nextVersion = Math.addExact(expectedVersion, 1L);
                if (current.getVersion() != expectedVersion || realm.getVersion() != nextVersion
                        || (realm.getId() != null && !realm.getId().equals(current.getId()))) {
                    throw stale(trustedRealm.getRealmKey(), expectedVersion, current.getVersion());
                }
                value.setId(current.getId());
                value.setVersion(nextVersion);
                value.setUpdateUserId(userIdProvider.currentUserId());
                value.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
                if (baseMapper.updateExpectedVersion(tenantId, value, expectedVersion) != 1) {
                    throw stale(trustedRealm.getRealmKey(), expectedVersion, current.getVersion());
                }
                return converter.toTarget(value);
            } catch (DataIntegrityViolationException failure) {
                throw new IllegalStateException("OAUTH_REALM_CONFLICT", failure);
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
            throw new IllegalStateException("OAUTH_REALM_MDC_KEYS_INVALID");
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
                throw new IllegalStateException("OAUTH_REALM_TENANT_CONTEXT_MISMATCH");
            }
            return operation.apply(tenantId);
        } finally {
            restoreMdc(tenantKey, previousTenant);
            restoreMdc(userKey, previousUser);
        }
    }

    private static void restoreMdc(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    private static void requireMatchingTrustedRealm(TrustedRealmBO trustedRealm, OAuthRealmBO realm) {
        boolean matches = realm.getIssuer() != null
                && trustedRealm.getIssuer().toASCIIString().equals(realm.getIssuer().toASCIIString())
                && trustedRealm.getRealmKey().equals(realm.getRealmKey())
                && trustedRealm.getRealmType() == realm.getRealmType()
                && trustedRealm.getStoragePartitionId().equals(realm.getStoragePartitionId())
                && Objects.equals(trustedRealm.getBusinessTenantId(), realm.getBusinessTenantId());
        if (!matches) {
            throw new IllegalArgumentException("OAUTH_REALM_TRUSTED_CONTEXT_MISMATCH");
        }
    }

    private static void requireCurrentMatchesTrustedRealm(
            TrustedRealmBO trustedRealm,
            OAuthRealmPO current) {
        boolean matches = trustedRealm.getIssuer().toASCIIString().equals(current.getIssuer())
                && trustedRealm.getRealmKey().equals(current.getRealmKey())
                && trustedRealm.getRealmType() == current.getRealmType()
                && trustedRealm.getStoragePartitionId().equals(current.getStoragePartitionId())
                && Objects.equals(trustedRealm.getBusinessTenantId(), current.getBusinessTenantId());
        if (!matches) {
            throw new IllegalStateException("OAUTH_REALM_PERSISTED_IDENTITY_MISMATCH");
        }
    }

    private static OptimisticLockingFailureException stale(
            String realmKey,
            long expectedVersion,
            long actualVersion) {
        return new OptimisticLockingFailureException(
                "realm " + realmKey + " expected version " + expectedVersion + " but was " + actualVersion);
    }
}
