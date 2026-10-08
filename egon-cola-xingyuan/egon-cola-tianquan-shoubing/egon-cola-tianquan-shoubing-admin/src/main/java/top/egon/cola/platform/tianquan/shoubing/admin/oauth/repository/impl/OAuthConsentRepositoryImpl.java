package top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
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
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthConsentMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthConsentBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthConsentConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthConsentPO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthConsentRepository;

@Repository("oAuthConsentRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthConsentRepositoryImpl extends EgonColaRepository<OAuthConsentMapper, OAuthConsentPO>
        implements OAuthConsentRepository {

    private static final String SERVICE_ACTOR = "service:tianquan-shoubing";

    @Getter
    @Qualifier("OAuthConsentMapper")
    private final OAuthConsentMapper baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("OAuthConsentConverterImpl")
    private final OAuthConsentConverter converter;
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthConsentBO> findConsent(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long clientPk,
            @NotBlank @Size(max = 128) String subject) {
        Objects.requireNonNull(clientPk, "clientPk");
        String principal = required(subject);
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                        baseMapper.selectConsent(tenantId, clientPk, principal))
                .map(converter::toTarget));
    }

    @Override
    @Transactional
    public Optional<OAuthConsentBO> findConsentForUpdate(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long clientPk,
            @NotBlank @Size(max = 128) String subject) {
        Objects.requireNonNull(clientPk, "clientPk");
        String principal = required(subject);
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                        baseMapper.selectConsentForUpdate(tenantId, clientPk, principal))
                .map(converter::toTarget));
    }

    @Override
    @Transactional
    public OAuthConsentBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthConsentBO consent,
            @PositiveOrZero long expectedVersion) {
        Objects.requireNonNull(consent, "consent");
        String principal = required(consent.getSubject());
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            try {
                OAuthConsentPO current = baseMapper.selectConsentForUpdate(
                        tenantId, consent.getClientPk(), principal);
                OAuthConsentPO value = converter.toSource(consent);
                value.setTenantId(tenantId);
                if (current == null) {
                    if (consent.getId() != null || expectedVersion != 0L || consent.getVersion() != 0L) {
                        throw stale(expectedVersion, -1L);
                    }
                    value.setVersion(0L);
                    if (!super.save(value)) {
                        throw new IllegalStateException("OAUTH_CONSENT_INSERT_NOT_APPLIED");
                    }
                    return converter.toTarget(value);
                }
                long nextVersion = Math.addExact(expectedVersion, 1L);
                if (current.getVersion() != expectedVersion || consent.getVersion() != nextVersion
                        || !Objects.equals(consent.getId(), current.getId())
                        || !Objects.equals(consent.getClientPk(), current.getClientPk())
                        || !Objects.equals(consent.getSubject(), current.getSubject())) {
                    throw stale(expectedVersion, current.getVersion());
                }
                value.setId(current.getId());
                value.setVersion(nextVersion);
                value.setUpdateUserId(userIdProvider.currentUserId());
                value.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
                if (baseMapper.updateExpectedVersion(tenantId, value, expectedVersion) != 1) {
                    throw stale(expectedVersion, current.getVersion());
                }
                return converter.toTarget(value);
            } catch (DataIntegrityViolationException failure) {
                throw new IllegalStateException("OAUTH_CONSENT_CONFLICT", failure);
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
            OAuthConsentPO value = new OAuthConsentPO();
            value.setId(id);
            value.setTenantId(tenantId);
            value.setVersion(expectedVersion);
            value.setUpdateUserId(userIdProvider.currentUserId());
            value.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
            return baseMapper.deleteVersionedById(tenantId, value, expectedVersion) == 1;
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
            throw new IllegalStateException("OAUTH_CONSENT_MDC_KEYS_INVALID");
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
                throw new IllegalStateException("OAUTH_CONSENT_TENANT_CONTEXT_MISMATCH");
            }
            return operation.apply(tenantId);
        } finally {
            restoreMdc(tenantKey, previousTenant);
            restoreMdc(userKey, previousUser);
        }
    }

    private static String required(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim()) || value.length() > 128) {
            throw new IllegalArgumentException("OAUTH_CONSENT_SUBJECT_INVALID");
        }
        return value;
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
                "consent expected version " + expectedVersion + " but was " + actualVersion);
    }
}
