package top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.impl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.dao.OAuthClientMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthClientConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.po.OAuthClientPO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Repository("oAuthClientRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthClientRepositoryImpl extends EgonColaRepository<OAuthClientMapper, OAuthClientPO>
        implements OAuthClientRepository {

    private static final String SERVICE_ACTOR = "service:tianquan-shoubing";

    @Getter
    @Qualifier("OAuthClientMapper")
    private final OAuthClientMapper baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("OAuthClientConverterImpl")
    private final OAuthClientConverter converter;
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthClientBO> findById(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long id) {
        Objects.requireNonNull(id, "id");
        return withTrustedStoragePartition(trustedRealm, tenantId ->
                Optional.ofNullable(baseMapper.selectActiveById(tenantId, id))
                        .map(converter::toTarget));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthClientBO> findByClientId(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Size(max = 128) String clientId) {
        String value = required(clientId, "clientId");
        return withTrustedStoragePartition(trustedRealm, tenantId ->
                Optional.ofNullable(baseMapper.selectByClientId(tenantId, value))
                        .map(converter::toTarget));
    }

    @Override
    @Transactional
    public Optional<OAuthClientBO> findByClientIdForUpdate(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotBlank @Size(max = 128) String clientId) {
        String value = required(clientId, "clientId");
        return withTrustedStoragePartition(trustedRealm, tenantId ->
                Optional.ofNullable(baseMapper.selectByClientIdForUpdate(tenantId, value))
                        .map(converter::toTarget));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthClientBO> findAllByRealm(@NotNull @Valid TrustedRealmBO trustedRealm) {
        return withTrustedStoragePartition(trustedRealm, tenantId -> baseMapper
                .selectDirectory(tenantId).stream()
                .map(converter::toTarget)
                .toList());
    }

    @Override
    @Transactional
    public OAuthClientBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthClientBO client,
            @PositiveOrZero long expectedVersion) {
        Objects.requireNonNull(client, "client");
        trustedRealm.validateTrustedPartition();
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            try {
                OAuthClientPO current = baseMapper.selectByClientIdForUpdate(
                        tenantId, required(client.getClientId(), "clientId"));
                OAuthClientPO value = converter.toSource(client);
                value.setTenantId(tenantId);
                if (current == null) {
                    if (expectedVersion != 0L || client.getVersion() != 0L || client.getId() != null) {
                        throw stale(client.getClientId(), expectedVersion, -1L);
                    }
                    if (!super.save(value)) {
                        throw new IllegalStateException("OAUTH_CLIENT_INSERT_NOT_APPLIED");
                    }
                    return converter.toTarget(value);
                }

                long nextVersion = Math.addExact(expectedVersion, 1L);
                if (current.getVersion() != expectedVersion || client.getVersion() != nextVersion
                        || !Objects.equals(client.getId(), current.getId())
                        || !Objects.equals(client.getAppId(), current.getAppId())
                        || client.getClientType() == null
                        || !client.getClientType().name().equals(current.getClientType())
                        || client.getPurpose() != current.getPurpose()) {
                    throw stale(client.getClientId(), expectedVersion, current.getVersion());
                }
                if (client.getCredentialVersion() < current.getCredentialVersion()) {
                    throw new IllegalStateException("OAUTH_CLIENT_CREDENTIAL_VERSION_REGRESSION");
                }
                value.setId(current.getId());
                value.setVersion(nextVersion);
                value.setUpdateUserId(userIdProvider.currentUserId());
                value.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
                if (baseMapper.updateExpectedVersion(tenantId, value, expectedVersion) != 1) {
                    throw stale(client.getClientId(), expectedVersion, current.getVersion());
                }
                return converter.toTarget(value);
            } catch (DataIntegrityViolationException failure) {
                throw new IllegalStateException("OAUTH_CLIENT_CONFLICT", failure);
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
            throw new IllegalStateException("OAUTH_CLIENT_MDC_KEYS_INVALID");
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
                throw new IllegalStateException("OAUTH_CLIENT_TENANT_CONTEXT_MISMATCH");
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

    private static OptimisticLockingFailureException stale(
            String clientId,
            long expectedVersion,
            long actualVersion) {
        return new OptimisticLockingFailureException(
                "client " + clientId + " expected version " + expectedVersion + " but was " + actualVersion);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }
}
