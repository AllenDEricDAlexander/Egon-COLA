package top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.impl;

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
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.dao.OAuthGrantMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.OAuthGrantConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.po.OAuthGrantPO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;

@Repository("oAuthGrantRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthGrantRepositoryImpl extends EgonColaRepository<OAuthGrantMapper, OAuthGrantPO>
        implements OAuthGrantRepository {

    private static final String SERVICE_ACTOR = "service:tianquan-shoubing";

    @Getter
    @Qualifier("OAuthGrantMapper")
    private final OAuthGrantMapper baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("OAuthGrantConverterImpl")
    private final OAuthGrantConverter converter;
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthGrantBO> findGrant(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long clientPk,
            @NotBlank @Size(max = 128) String resourceServerId,
            @NotNull ResourceGrantType grantType) {
        Long client = requiredClientPk(clientPk);
        String resource = required(resourceServerId, "resourceServerId");
        ResourceGrantType type = Objects.requireNonNull(grantType, "grantType");
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                baseMapper.selectGrant(tenantId, client, resource, type)).map(converter::toTarget));
    }

    @Override
    @Transactional
    public Optional<OAuthGrantBO> findGrantForUpdate(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long clientPk,
            @NotBlank @Size(max = 128) String resourceServerId,
            @NotNull ResourceGrantType grantType) {
        Long client = requiredClientPk(clientPk);
        String resource = required(resourceServerId, "resourceServerId");
        ResourceGrantType type = Objects.requireNonNull(grantType, "grantType");
        return withTrustedStoragePartition(trustedRealm, tenantId -> Optional.ofNullable(
                baseMapper.selectGrantForUpdate(tenantId, client, resource, type)).map(converter::toTarget));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthGrantBO> listByClient(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @Positive Long clientPk,
            @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(65) int limit) {
        Long client = requiredClientPk(clientPk);
        if (limit < 1 || limit > 65) {
            throw new IllegalArgumentException("OAUTH_GRANT_LIST_LIMIT_INVALID");
        }
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            return baseMapper.listByClient(tenantId, client, limit).stream()
                    .map(converter::toTarget)
                    .toList();
        });
    }

    @Override
    @Transactional
    public OAuthGrantBO save(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthGrantBO grant,
            @PositiveOrZero long expectedVersion) {
        List<String> canonicalScopes = validateGrant(grant);
        return withTrustedStoragePartition(trustedRealm, tenantId -> {
            try {
                OAuthGrantPO current = baseMapper.selectGrantForUpdate(
                        tenantId, grant.getClientPk(), grant.getResourceServerId(), grant.getGrantType());
                OAuthGrantPO value = converter.toSource(grant);
                value.setTenantId(tenantId);
                value.setAllowedScopes(canonicalScopes);
                if (current == null) {
                    if (expectedVersion != 0L || grant.getVersion() != 0L || grant.getId() != null) {
                        throw stale(grant, expectedVersion, -1L);
                    }
                    if (!super.save(value)) {
                        throw new IllegalStateException("OAUTH_GRANT_INSERT_NOT_APPLIED");
                    }
                    return converter.toTarget(value);
                }

                long nextVersion = Math.addExact(expectedVersion, 1L);
                if (current.getVersion() != expectedVersion || grant.getVersion() != nextVersion
                        || !Objects.equals(grant.getId(), current.getId())
                        || !Objects.equals(grant.getClientPk(), current.getClientPk())
                        || !Objects.equals(grant.getResourceServerId(), current.getResourceServerId())
                        || !Objects.equals(grant.getGrantType().name(), current.getGrantType())) {
                    throw stale(grant, expectedVersion, current.getVersion());
                }
                if (grant.getResourceVersion() < current.getResourceVersion()) {
                    throw new IllegalStateException("OAUTH_GRANT_RESOURCE_VERSION_REGRESSION");
                }
                value.setId(current.getId());
                value.setVersion(nextVersion);
                value.setUpdateUserId(userIdProvider.currentUserId());
                value.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
                if (baseMapper.updateExpectedVersion(tenantId, value, expectedVersion) != 1) {
                    throw stale(grant, expectedVersion, current.getVersion());
                }
                return converter.toTarget(value);
            } catch (DataIntegrityViolationException failure) {
                throw new IllegalStateException("OAUTH_GRANT_CONFLICT", failure);
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
            throw new IllegalStateException("OAUTH_GRANT_MDC_KEYS_INVALID");
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
                throw new IllegalStateException("OAUTH_GRANT_TENANT_CONTEXT_MISMATCH");
            }
            return operation.apply(tenantId);
        } finally {
            restoreMdc(tenantKey, previousTenant);
            restoreMdc(userKey, previousUser);
        }
    }

    private static List<String> validateGrant(OAuthGrantBO grant) {
        Objects.requireNonNull(grant, "grant");
        requiredClientPk(grant.getClientPk());
        required(grant.getResourceServerId(), "resourceServerId");
        Objects.requireNonNull(grant.getGrantType(), "grantType");
        Objects.requireNonNull(grant.getStatus(), "status");
        if (grant.getResourceVersion() == null || grant.getResourceVersion() < 0L) {
            throw new IllegalArgumentException("resourceVersion is invalid");
        }
        List<String> scopes = Objects.requireNonNull(grant.getAllowedScopes(), "allowedScopes");
        if (scopes.size() > 64) {
            throw new IllegalArgumentException("allowedScopes is invalid");
        }
        TreeSet<String> uniqueScopes = new TreeSet<>();
        for (String scope : scopes) {
            String value = required(scope, "scope");
            if (value.length() > 256 || !value.matches("[A-Za-z0-9][A-Za-z0-9:._~/-]*")
                    || !uniqueScopes.add(value)) {
                throw new IllegalArgumentException("allowedScopes is invalid");
            }
        }
        if (grant.getVersion() < 0L) {
            throw new IllegalArgumentException("version is invalid");
        }
        return List.copyOf(uniqueScopes);
    }

    private static Long requiredClientPk(Long value) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException("clientPk is invalid");
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

    private static OptimisticLockingFailureException stale(
            OAuthGrantBO grant,
            long expectedVersion,
            long actualVersion) {
        return new OptimisticLockingFailureException("grant " + grant.getClientPk() + "/"
                + grant.getResourceServerId() + "/" + grant.getGrantType()
                + " expected version " + expectedVersion + " but was " + actualVersion);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }
}
