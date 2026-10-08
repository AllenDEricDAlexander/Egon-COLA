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
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.dao.OAuthResourceMapper;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.OAuthResourceConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.po.OAuthResourcePO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthResourceRepository;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServer;

import java.net.URI;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Repository("oAuthResourceRepository")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthResourceRepositoryImpl extends EgonColaRepository<OAuthResourceMapper, OAuthResourcePO>
        implements OAuthResourceRepository {

    private static final String SERVICE_ACTOR = "service:tianquan-shoubing";

    @Getter
    @Qualifier("OAuthResourceMapper")
    private final OAuthResourceMapper baseMapper;
    @Getter(AccessLevel.PROTECTED)
    @Qualifier("egon.cola.component.mybatis-plus-top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties")
    private final EgonColaMybatisPlusProperties properties;
    @Qualifier("OAuthResourceConverterImpl")
    private final OAuthResourceConverter converter;
    @Qualifier("egonColaMdcUserIdProvider")
    private final EgonColaUserIdProvider userIdProvider;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<OAuthResourceBO> listDirectory(TrustedRealmBO platformRealm, int limit) {
        if (limit < 1 || limit > 10_001) {
            throw new IllegalArgumentException("OAUTH_RESOURCE_DIRECTORY_LIMIT_INVALID");
        }
        return withPlatformStoragePartition(platformRealm, tenantId -> baseMapper
                .selectDirectory(tenantId, limit).stream()
                .map(converter::toTarget)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthResourceBO> findByResourceServerId(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotBlank @Size(max = 128) String resourceServerId) {
        String value = required(resourceServerId, "resourceServerId");
        return withPlatformStoragePartition(platformRealm, tenantId ->
                Optional.ofNullable(baseMapper.selectByResourceServerId(tenantId, value))
                        .map(converter::toTarget));
    }

    @Override
    @Transactional
    public Optional<OAuthResourceBO> findByResourceServerIdForUpdate(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotBlank @Size(max = 128) String resourceServerId) {
        String value = required(resourceServerId, "resourceServerId");
        return withPlatformStoragePartition(platformRealm, tenantId ->
                Optional.ofNullable(baseMapper.selectByResourceServerIdForUpdate(tenantId, value))
                        .map(converter::toTarget));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthResourceBO> findByResourceUri(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotNull URI resourceUri) {
        URI value = requiredUri(resourceUri);
        return withPlatformStoragePartition(platformRealm, tenantId ->
                Optional.ofNullable(baseMapper.selectByResourceUri(tenantId, value.toASCIIString()))
                        .map(converter::toTarget));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthResourceBO> findBySource(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotBlank @Size(max = 128) String bizCode,
            @NotBlank @Size(max = 128) String appCode,
            @NotBlank @Size(max = 128) String environment) {
        String domain = required(bizCode, "bizCode");
        String application = required(appCode, "appCode");
        String stage = required(environment, "environment");
        return withPlatformStoragePartition(platformRealm, tenantId ->
                Optional.ofNullable(baseMapper.selectBySource(tenantId, domain, application, stage))
                        .map(converter::toTarget));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OAuthResourceBO> findByManagementClientPk(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @Positive Long managementClientPk) {
        Objects.requireNonNull(managementClientPk, "managementClientPk");
        return withPlatformStoragePartition(platformRealm, tenantId ->
                Optional.ofNullable(baseMapper.selectByManagementClientPk(tenantId, managementClientPk))
                        .map(converter::toTarget));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OAuthResourceBO> findByBizCodeAndEnvironmentAndAppCodeIn(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotBlank @Size(max = 128) String bizCode,
            @NotBlank @Size(max = 128) String environment,
            @NotNull @Size(min = 1, max = 100) Collection<@NotBlank @Size(max = 128) String> appCodes) {
        String domain = required(bizCode, "bizCode");
        String stage = required(environment, "environment");
        List<String> applications = List.copyOf(Objects.requireNonNull(appCodes, "appCodes"));
        if (applications.isEmpty() || applications.size() > 100) {
            throw new IllegalArgumentException("OAUTH_RESOURCE_BATCH_SIZE_INVALID");
        }
        applications.forEach(code -> required(code, "appCode"));
        return withPlatformStoragePartition(platformRealm, tenantId -> baseMapper
                .selectByBizCodeAndEnvironmentAndAppCodeIn(tenantId, domain, stage, applications)
                .stream()
                .map(converter::toTarget)
                .toList());
    }

    @Override
    @Transactional
    public OAuthResourceBO save(
            @NotNull @Valid TrustedRealmBO platformRealm,
            @NotNull @Valid OAuthResourceBO resource,
            @PositiveOrZero long expectedVersion) {
        URI canonicalResourceUri = validateResource(resource);
        return withPlatformStoragePartition(platformRealm, tenantId -> {
            try {
                OAuthResourcePO current = baseMapper.selectByResourceServerIdForUpdate(
                        tenantId, resource.getResourceServerId());
                OAuthResourcePO value = converter.toSource(resource);
                value.setTenantId(tenantId);
                value.setResourceUri(canonicalResourceUri.toASCIIString());
                if (current == null) {
                    if (expectedVersion != 0L || resource.getVersion() != 0L || resource.getId() != null) {
                        throw stale(resource.getResourceServerId(), expectedVersion, -1L);
                    }
                    if (!super.save(value)) {
                        throw new IllegalStateException("OAUTH_RESOURCE_INSERT_NOT_APPLIED");
                    }
                    return converter.toTarget(value);
                }

                long nextVersion = Math.addExact(expectedVersion, 1L);
                if (current.getVersion() != expectedVersion || resource.getVersion() != nextVersion
                        || !Objects.equals(resource.getId(), current.getId())
                        || !Objects.equals(canonicalResourceUri.toASCIIString(), current.getResourceUri())
                        || !Objects.equals(resource.getBizCode(), current.getBizCode())
                        || !Objects.equals(resource.getAppCode(), current.getAppCode())
                        || !Objects.equals(resource.getEnvironment(), current.getEnvironment())
                        || !Objects.equals(resource.getManagementClientId(), current.getManagementClientId())
                        || !Objects.equals(resource.getManagementClientPk(), current.getManagementClientPk())) {
                    throw stale(resource.getResourceServerId(), expectedVersion, current.getVersion());
                }
                value.setId(current.getId());
                value.setVersion(nextVersion);
                value.setUpdateUserId(userIdProvider.currentUserId());
                value.setUpdateTime(clock.instant().truncatedTo(ChronoUnit.MICROS));
                if (baseMapper.updateExpectedVersion(tenantId, value, expectedVersion) != 1) {
                    throw stale(resource.getResourceServerId(), expectedVersion, current.getVersion());
                }
                return converter.toTarget(value);
            } catch (DataIntegrityViolationException failure) {
                throw new IllegalStateException("OAUTH_RESOURCE_CONFLICT", failure);
            }
        });
    }

    private <T> T withPlatformStoragePartition(
            TrustedRealmBO trustedRealm,
            Function<Long, T> operation) {
        Objects.requireNonNull(trustedRealm, "trustedRealm").validateTrustedPartition();
        if (trustedRealm.getRealmType() != RealmTypeEnum.PLATFORM) {
            throw new IllegalArgumentException("OAUTH_RESOURCE_PLATFORM_REALM_REQUIRED");
        }
        Objects.requireNonNull(operation, "operation");
        String tenantKey = properties.getTenantId().getMdcKey();
        String userKey = properties.getAudit().getUserIdMdcKey();
        if (tenantKey.equals(userKey)) {
            throw new IllegalStateException("OAUTH_RESOURCE_MDC_KEYS_INVALID");
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
                throw new IllegalStateException("OAUTH_RESOURCE_TENANT_CONTEXT_MISMATCH");
            }
            return operation.apply(tenantId);
        } finally {
            restoreMdc(tenantKey, previousTenant);
            restoreMdc(userKey, previousUser);
        }
    }

    private static URI validateResource(OAuthResourceBO resource) {
        Objects.requireNonNull(resource, "resource");
        required(resource.getDisplayName(), "displayName");
        required(resource.getManagementClientId(), "managementClientId");
        required(resource.getRbacApplicationCode(), "rbacApplicationCode");
        required(resource.getEntryPermissionCode(), "entryPermissionCode");
        if (resource.getManagementClientPk() == null || resource.getManagementClientPk() <= 0L) {
            throw new IllegalArgumentException("managementClientPk is invalid");
        }
        URI canonicalResourceUri = requiredUri(resource.getResourceUri());
        new ResourceServer(resource.getResourceServerId(), canonicalResourceUri,
                resource.getBizCode(), resource.getAppCode(), resource.getEnvironment(),
                resource.getManagementClientId(), resource.getRbacApplicationCode(),
                resource.getEntryPermissionCode(), resource.getAdmissionTicketTtl(),
                resource.getStatus(), resource.getVersion());
        return canonicalResourceUri;
    }

    private static URI requiredUri(URI value) {
        Objects.requireNonNull(value, "resourceUri");
        if (!value.isAbsolute() || value.getScheme() == null || value.getScheme().isBlank()
                || value.getFragment() != null || value.toASCIIString().length() > 2048) {
            throw new IllegalArgumentException("resourceUri is invalid");
        }
        return value.normalize();
    }

    private static void restoreMdc(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    private static OptimisticLockingFailureException stale(
            String resourceServerId,
            long expectedVersion,
            long actualVersion) {
        return new OptimisticLockingFailureException("resource " + resourceServerId
                + " expected version " + expectedVersion + " but was " + actualVersion);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }
}
