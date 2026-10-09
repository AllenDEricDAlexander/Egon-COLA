package top.egon.cola.platform.tianquan.shoubing.admin.resource.service.impl;

import jakarta.validation.Validator;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.component.common.core.validation.ValidationUtils;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.ClientResourceGrantViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.converter.ResourceServerViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.BatchClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.BatchResourceServerActionDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.CreateResourceServerDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.DeleteClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.ResourceVersionDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.dto.UpsertClientResourceGrantDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ClientResourceGrantVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.vo.ResourceServerVO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthResourceRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.service.ResourceServerService;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox.TransactionalOutboxResourceServerEventAdapter;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;

@Service("resourceServerServiceImpl")
@Slf4j
@RequiredArgsConstructor
public class ResourceServerServiceImpl implements ResourceServerService {

    private static final int RESOURCE_DIRECTORY_LIMIT = 10_000;
    private static final int RESOURCE_DIRECTORY_FETCH_LIMIT = RESOURCE_DIRECTORY_LIMIT + 1;
    private static final int RESOURCE_BATCH_LIMIT = 100;
    private static final int GRANT_LIST_LIMIT = 65;

    @NonNull
    @Qualifier("oAuthResourceRepository")
    private final OAuthResourceRepository resources;
    @NonNull
    @Qualifier("oAuthGrantRepository")
    private final OAuthGrantRepository grants;
    @NonNull
    @Qualifier("oAuthClientRepository")
    private final OAuthClientRepository clients;
    @NonNull
    @Qualifier("resourceServerViewConverterImpl")
    private final ResourceServerViewConverter resourceViews;
    @NonNull
    @Qualifier("clientResourceGrantViewConverterImpl")
    private final ClientResourceGrantViewConverter grantViews;
    @NonNull
    @Qualifier("idpClock")
    private final Clock clock;
    @NonNull
    @Qualifier("resourceServerEventAdapter")
    private final TransactionalOutboxResourceServerEventAdapter events;
    @NonNull
    @Qualifier("defaultValidator")
    private final Validator validator;

    @Override
    @Transactional(readOnly = true)
    public List<ResourceServerVO> list(TrustedRealmBO platformRealm) {
        TrustedRealmBO realm = requirePlatform(platformRealm);
        List<OAuthResourceBO> found = resources.listDirectory(realm, RESOURCE_DIRECTORY_FETCH_LIMIT);
        if (found.size() > RESOURCE_DIRECTORY_LIMIT) {
            throw new IllegalStateException("OAUTH_RESOURCE_DIRECTORY_LIMIT_EXCEEDED");
        }
        return found.stream()
                .sorted(Comparator.comparing(OAuthResourceBO::getResourceServerId))
                .map(resourceViews::toTarget)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceServerVO detail(TrustedRealmBO platformRealm, String resourceServerId) {
        TrustedRealmBO realm = requirePlatform(platformRealm);
        return resourceViews.toTarget(resource(realm, resourceServerId));
    }

    @Override
    @Transactional
    public ResourceServerVO create(TrustedRealmBO platformRealm, CreateResourceServerDTO command) {
        TrustedRealmBO realm = requirePlatform(platformRealm);
        CreateResourceServerDTO input = Objects.requireNonNull(command, "command");
        String resourceId = exact(input.resourceServerId(), "resourceServerId");
        URI resourceUri = exactHttpsUri(input.resourceUri(), "resourceUri");
        String bizCode = requiredText(input.bizCode(), "bizCode", 128);
        String appCode = requiredText(input.appCode(), "appCode", 128);
        String environment = requiredText(input.environment(), "environment", 128);
        if (resources.findByResourceServerId(realm, resourceId).isPresent()
                || resources.findByResourceUri(realm, resourceUri).isPresent()
                || resources.findBySource(realm, bizCode, appCode, environment).isPresent()) {
            throw new IllegalStateException("OAUTH_RESOURCE_ALREADY_EXISTS");
        }
        OAuthClientBO managementClient = managementClient(realm, input.managementClientId());
        if (resources.findByManagementClientPk(realm, managementClient.getId()).isPresent()) {
            throw new IllegalStateException("OAUTH_RESOURCE_MANAGEMENT_CLIENT_ALREADY_BOUND");
        }

        OAuthResourceBO created = OAuthResourceBO.builder()
                .version(0L)
                .resourceServerId(resourceId)
                .resourceUri(resourceUri)
                .bizCode(bizCode)
                .appCode(appCode)
                .environment(environment)
                .displayName(requiredText(input.displayName(), "displayName", 200))
                .managementClientId(managementClient.getClientId())
                .managementClientPk(managementClient.getId())
                .rbacApplicationCode(requiredText(input.rbacApplicationCode(), "rbacApplicationCode", 128))
                .entryPermissionCode(requiredText(input.entryPermissionCode(), "entryPermissionCode", 256))
                .status(ResourceServerStatus.DISABLED)
                .admissionTicketTtl(Duration.ofMinutes(5))
                .build();
        OAuthResourceBO saved = resources.save(realm, created, 0L);
        events.enqueueResourceProjection(saved);
        return resourceViews.toTarget(saved);
    }

    @Override
    @Transactional
    public ResourceServerVO enable(TrustedRealmBO platformRealm, String resourceServerId, ResourceVersionDTO command) {
        Objects.requireNonNull(command, "command");
        TrustedRealmBO realm = requirePlatform(platformRealm);
        OAuthResourceBO current = resourceForUpdate(realm, resourceServerId);
        if (current.getVersion() != command.expectedVersion()) {
            throw new OptimisticLockingFailureException("OAUTH_RESOURCE_VERSION_CONFLICT");
        }
        if (current.getStatus() == ResourceServerStatus.ACTIVE) {
            return resourceViews.toTarget(current);
        }
        OAuthResourceBO changed = copy(current)
                .version(Math.addExact(current.getVersion(), 1L))
                .status(ResourceServerStatus.ACTIVE)
                .updatedAt(clock.instant())
                .build();
        OAuthResourceBO saved = resources.save(realm, changed, current.getVersion());
        events.enqueueResourceProjection(saved);
        return resourceViews.toTarget(saved);
    }

    @Override
    @Transactional
    public ResourceServerVO disable(TrustedRealmBO platformRealm, String resourceServerId, ResourceVersionDTO command) {
        Objects.requireNonNull(command, "command");
        TrustedRealmBO realm = requirePlatform(platformRealm);
        OAuthResourceBO current = resourceForUpdate(realm, resourceServerId);
        if (current.getVersion() != command.expectedVersion()) {
            throw new OptimisticLockingFailureException("OAUTH_RESOURCE_VERSION_CONFLICT");
        }
        if (current.getStatus() == ResourceServerStatus.DISABLED) {
            return resourceViews.toTarget(current);
        }
        OAuthResourceBO changed = copy(current)
                .version(Math.addExact(current.getVersion(), 1L))
                .status(ResourceServerStatus.DISABLED)
                .updatedAt(clock.instant())
                .build();
        OAuthResourceBO saved = resources.save(realm, changed, current.getVersion());
        events.enqueueResourceProjection(saved);
        events.enqueueDisabled(saved);
        return resourceViews.toTarget(saved);
    }

    @Override
    @Transactional
    public ClientResourceGrantVO putGrant(
            TrustedRealmBO clientRealm,
            TrustedRealmBO platformRealm,
            String clientId,
            String resourceServerId,
            UpsertClientResourceGrantDTO command) {
        OAuthClientBO client = clientForUpdate(clientRealm, clientId);
        OAuthResourceBO resource = resourceForUpdate(requirePlatform(platformRealm), resourceServerId);
        OAuthGrantBO saved = upsertGrant(clientRealm, client, resource, Objects.requireNonNull(command, "command"));
        enqueueServiceGrantProjection(clientRealm, client, saved);
        return grantViews.toTarget(saved, clientRealm, client);
    }

    @Override
    @Transactional
    public void deleteGrant(
            TrustedRealmBO clientRealm,
            TrustedRealmBO platformRealm,
            String clientId,
            String resourceServerId,
            DeleteClientResourceGrantDTO command) {
        Objects.requireNonNull(command, "command");
        TrustedRealmBO realm = requireRealm(clientRealm);
        OAuthClientBO client = clientForUpdate(realm, clientId);
        OAuthResourceBO resource = resourceForUpdate(requirePlatform(platformRealm), resourceServerId);
        if (resource.getVersion() != command.getExpectedResourceVersion()) {
            throw new OptimisticLockingFailureException("OAUTH_RESOURCE_VERSION_CONFLICT");
        }
        ResourceGrantType grantType = Objects.requireNonNull(command.getGrantType(), "grantType");
        validateClientGrantContext(realm, client, grantType, command.getTenantId(), Set.of());
        OAuthGrantBO grant = grants.findGrantForUpdate(realm, client.getId(), resource.getResourceServerId(), grantType)
                .orElseThrow(() -> new NoSuchElementException("OAUTH_GRANT_NOT_FOUND"));
        if (grant.getVersion() != command.getExpectedGrantVersion()) {
            throw new OptimisticLockingFailureException("OAUTH_GRANT_VERSION_CONFLICT");
        }
        OAuthGrantBO disabled = copy(grant)
                .version(Math.addExact(grant.getVersion(), 1L))
                .status(ClientResourceGrant.Status.DISABLED)
                .resourceVersion(resource.getVersion())
                .build();
        OAuthGrantBO saved = grants.save(realm, disabled, grant.getVersion());
        if (grantType == ResourceGrantType.CLIENT_CREDENTIALS) {
            enqueueServiceGrantProjection(realm, client, saved);
        }
    }

    @Override
    @Transactional
    public List<ResourceServerVO> batch(TrustedRealmBO platformRealm, BatchResourceServerActionDTO command) {
        Objects.requireNonNull(command, "command");
        TrustedRealmBO realm = requirePlatform(platformRealm);
        List<String> appCodes = explicitApps(command.appCodes(), command.expectedVersions());
        List<OAuthResourceBO> selected = selectResources(realm, command.bizCode(), command.environment(), appCodes);
        List<OAuthResourceBO> saved = new ArrayList<>();
        for (OAuthResourceBO current : selected) {
            long expected = expected(command.expectedVersions(), current.getAppCode());
            if (current.getVersion() != expected) {
                throw new OptimisticLockingFailureException("OAUTH_RESOURCE_VERSION_CONFLICT");
            }
            ResourceServerStatus status = command.action() == BatchResourceServerActionDTO.Action.ENABLE
                    ? ResourceServerStatus.ACTIVE : ResourceServerStatus.DISABLED;
            if (current.getStatus() == status) {
                saved.add(current);
                continue;
            }
            OAuthResourceBO changed = copy(current)
                    .version(Math.addExact(current.getVersion(), 1L))
                    .status(status)
                    .updatedAt(clock.instant())
                    .build();
            OAuthResourceBO value = resources.save(realm, changed, current.getVersion());
            saved.add(value);
            if (status == ResourceServerStatus.DISABLED) {
                events.enqueueDisabled(value);
            }
        }
        saved.forEach(events::enqueueResourceProjection);
        return saved.stream().map(resourceViews::toTarget).toList();
    }

    @Override
    @Transactional
    public List<ClientResourceGrantVO> batchGrants(
            TrustedRealmBO clientRealm,
            TrustedRealmBO platformRealm,
            String clientId,
            BatchClientResourceGrantDTO command) {
        Objects.requireNonNull(command, "command");
        TrustedRealmBO realm = requireRealm(clientRealm);
        TrustedRealmBO platform = requirePlatform(platformRealm);
        OAuthClientBO client = clientForUpdate(realm, clientId);
        List<String> appCodes = explicitApps(command.getAppCodes(), command.getExpectedResourceVersions());
        validateGrantVersions(appCodes, command.getExpectedGrantVersions(), command.getAction());
        List<OAuthResourceBO> selected = selectResources(platform, command.getBizCode(), command.getEnvironment(), appCodes);
        List<ClientResourceGrantVO> results = new ArrayList<>();
        for (OAuthResourceBO resource : selected) {
            long expectedResourceVersion = expected(command.getExpectedResourceVersions(), resource.getAppCode());
            if (resource.getVersion() != expectedResourceVersion) {
                throw new OptimisticLockingFailureException("OAUTH_RESOURCE_VERSION_CONFLICT");
            }
            Long expectedGrantVersion = command.getExpectedGrantVersions().get(resource.getAppCode());
            if (command.getAction() == BatchClientResourceGrantDTO.Action.UPSERT) {
                OAuthGrantBO grant = upsertGrant(realm, client, resource, UpsertClientResourceGrantDTO.builder()
                        .grantType(command.getGrantType())
                        .tenantId(command.getTenantId())
                        .allowedScopes(command.getAllowedScopes())
                        .expectedResourceVersion(expectedResourceVersion)
                        .expectedGrantVersion(expectedGrantVersion)
                        .build());
                results.add(grantViews.toTarget(grant, realm, client));
                enqueueServiceGrantProjection(realm, client, grant);
            } else {
                if (expectedGrantVersion == null) {
                    throw new IllegalArgumentException("OAUTH_GRANT_EXPECTED_VERSION_REQUIRED");
                }
                OAuthGrantBO grant = disableGrant(realm, client, resource, command.getGrantType(), command.getTenantId(),
                        expectedResourceVersion, expectedGrantVersion);
                results.add(grantViews.toTarget(grant, realm, client));
                enqueueServiceGrantProjection(realm, client, grant);
            }
        }
        return List.copyOf(results);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResourceGrantVO> listGrants(
            TrustedRealmBO clientRealm,
            TrustedRealmBO platformRealm,
            String clientId) {
        TrustedRealmBO realm = requireRealm(clientRealm);
        requirePlatform(platformRealm);
        OAuthClientBO client = clients.findByClientId(realm, exact(clientId, "clientId"))
                .orElseThrow(() -> new NoSuchElementException("OAUTH_CLIENT_NOT_FOUND"));
        return grants.listByClient(realm, client.getId(), GRANT_LIST_LIMIT).stream()
                .sorted(Comparator.comparing(OAuthGrantBO::getResourceServerId)
                        .thenComparing(value -> value.getGrantType().name())
                        .thenComparing(OAuthGrantBO::getId))
                .map(grant -> grantViews.toTarget(grant, realm, client))
                .toList();
    }

    private OAuthGrantBO upsertGrant(
            TrustedRealmBO realm,
            OAuthClientBO client,
            OAuthResourceBO resource,
            UpsertClientResourceGrantDTO command) {
        new ValidationUtils(validator).validate(command);
        ResourceGrantType type = Objects.requireNonNull(command.getGrantType(), "grantType");
        Set<String> scopes = command.getAllowedScopes() == null ? Set.of() : canonicalScopes(command.getAllowedScopes());
        validateClientGrantContext(realm, client, type, command.getTenantId(), scopes);
        if (resource.getVersion() != command.getExpectedResourceVersion()) {
            throw new OptimisticLockingFailureException("OAUTH_RESOURCE_VERSION_CONFLICT");
        }
        OAuthGrantBO current = grants.findGrantForUpdate(realm, client.getId(), resource.getResourceServerId(), type)
                .orElse(null);
        if (current == null) {
            if (command.getExpectedGrantVersion() != null) {
                throw new OptimisticLockingFailureException("OAUTH_GRANT_VERSION_CONFLICT");
            }
            return grants.save(realm, OAuthGrantBO.builder()
                    .version(0L)
                    .clientPk(client.getId())
                    .resourceServerId(resource.getResourceServerId())
                    .grantType(type)
                    .allowedScopes(type == ResourceGrantType.USER_DELEGATION
                            ? scopes.stream().sorted().toList() : List.copyOf(scopes))
                    .status(ClientResourceGrant.Status.ACTIVE)
                    .resourceVersion(resource.getVersion())
                    .build(), 0L);
        }
        if (command.getExpectedGrantVersion() == null || current.getVersion() != command.getExpectedGrantVersion()) {
            throw new OptimisticLockingFailureException("OAUTH_GRANT_VERSION_CONFLICT");
        }
        OAuthGrantBO changed = copy(current)
                .version(Math.addExact(current.getVersion(), 1L))
                .allowedScopes(type == ResourceGrantType.USER_DELEGATION
                        ? scopes.stream().sorted().toList() : List.copyOf(scopes))
                .status(ClientResourceGrant.Status.ACTIVE)
                .resourceVersion(resource.getVersion())
                .build();
        return grants.save(realm, changed, current.getVersion());
    }

    private OAuthGrantBO disableGrant(
            TrustedRealmBO realm,
            OAuthClientBO client,
            OAuthResourceBO resource,
            ResourceGrantType type,
            String tenantId,
            long expectedResourceVersion,
            long expectedGrantVersion) {
        if (resource.getVersion() != expectedResourceVersion) {
            throw new OptimisticLockingFailureException("OAUTH_RESOURCE_VERSION_CONFLICT");
        }
        validateClientGrantContext(realm, client, type, tenantId, Set.of());
        OAuthGrantBO current = grants.findGrantForUpdate(realm, client.getId(), resource.getResourceServerId(), type)
                .orElseThrow(() -> new NoSuchElementException("OAUTH_GRANT_NOT_FOUND"));
        if (current.getVersion() != expectedGrantVersion) {
            throw new OptimisticLockingFailureException("OAUTH_GRANT_VERSION_CONFLICT");
        }
        OAuthGrantBO changed = copy(current)
                .version(Math.addExact(current.getVersion(), 1L))
                .status(ClientResourceGrant.Status.DISABLED)
                .resourceVersion(resource.getVersion())
                .build();
        return grants.save(realm, changed, current.getVersion());
    }

    private void validateClientGrantContext(
            TrustedRealmBO realm,
            OAuthClientBO client,
            ResourceGrantType type,
            String tenantId,
            Set<String> scopes) {
        if (type == ResourceGrantType.USER_DELEGATION) {
            if (client.getPurpose() != OAuthClientPurposeEnum.USER || tenantId != null
                    || !client.getAllowedScopes().containsAll(scopes)) {
                throw new IllegalArgumentException("OAUTH_USER_DELEGATION_GRANT_INVALID");
            }
            new ClientResourceGrant(client.getClientId(), "grant-target", type, null, scopes,
                    ClientResourceGrant.Status.ACTIVE, 0L);
            return;
        }
        if (client.getPurpose() != OAuthClientPurposeEnum.SERVICE || scopes.isEmpty()) {
            throw new IllegalArgumentException("OAUTH_SERVICE_GRANT_INVALID");
        }
        String realmTenant = realm.getBusinessTenantId() == null ? null : realm.getBusinessTenantId().toString();
        if (!Objects.equals(realmTenant, tenantId)) {
            throw new IllegalArgumentException("OAUTH_SERVICE_GRANT_TENANT_MISMATCH");
        }
        new ClientResourceGrant(client.getClientId(), "grant-target", type, tenantId, scopes,
                ClientResourceGrant.Status.ACTIVE, 0L,
                realmTenant == null ? ServiceTokenContext.PLATFORM : ServiceTokenContext.TENANT);
    }

    private OAuthClientBO managementClient(TrustedRealmBO platformRealm, String clientId) {
        OAuthClientBO client = clients.findByClientId(platformRealm, exact(clientId, "managementClientId"))
                .orElseThrow(() -> new NoSuchElementException("OAUTH_MANAGEMENT_CLIENT_NOT_FOUND"));
        if (client.getPurpose() != OAuthClientPurposeEnum.SERVICE
                || client.getClientType() != OAuthClient.ClientType.CONFIDENTIAL
                || client.getStatus() != OAuthClient.Status.ACTIVE
                || client.getSecretHash() == null || client.getSecretHash().isBlank()) {
            throw new IllegalArgumentException("OAUTH_MANAGEMENT_CLIENT_INVALID");
        }
        return client;
    }

    private OAuthClientBO clientForUpdate(TrustedRealmBO realm, String clientId) {
        return clients.findByClientIdForUpdate(realm, exact(clientId, "clientId"))
                .orElseThrow(() -> new NoSuchElementException("OAUTH_CLIENT_NOT_FOUND"));
    }

    private OAuthResourceBO resourceForUpdate(TrustedRealmBO realm, String resourceServerId) {
        return resources.findByResourceServerIdForUpdate(realm, exact(resourceServerId, "resourceServerId"))
                .orElseThrow(() -> new NoSuchElementException("OAUTH_RESOURCE_NOT_FOUND"));
    }

    private OAuthResourceBO resource(TrustedRealmBO realm, String resourceServerId) {
        return resources.findByResourceServerId(realm, exact(resourceServerId, "resourceServerId"))
                .orElseThrow(() -> new NoSuchElementException("OAUTH_RESOURCE_NOT_FOUND"));
    }

    private List<OAuthResourceBO> selectResources(
            TrustedRealmBO platformRealm,
            String bizCode,
            String environment,
            List<String> appCodes) {
        List<OAuthResourceBO> selected = resources.findByBizCodeAndEnvironmentAndAppCodeIn(
                platformRealm, requiredText(bizCode, "bizCode", 128),
                requiredText(environment, "environment", 128), appCodes);
        Set<String> found = selected.stream().map(OAuthResourceBO::getAppCode).collect(java.util.stream.Collectors.toSet());
        if (!found.equals(Set.copyOf(appCodes))) {
            throw new NoSuchElementException("OAUTH_RESOURCE_SELECTION_NOT_FOUND");
        }
        return selected;
    }

    private OAuthClientBO resolveResourceManagementClient(TrustedRealmBO platformRealm, OAuthResourceBO resource) {
        OAuthClientBO client = clients.findById(platformRealm, resource.getManagementClientPk())
                .orElseThrow(() -> new IllegalStateException("OAUTH_RESOURCE_MANAGEMENT_CLIENT_NOT_FOUND"));
        if (!client.getClientId().equals(resource.getManagementClientId())) {
            throw new IllegalStateException("OAUTH_RESOURCE_MANAGEMENT_CLIENT_MISMATCH");
        }
        return client;
    }

    private void enqueueServiceGrantProjection(
            TrustedRealmBO clientRealm,
            OAuthClientBO client,
            OAuthGrantBO grant) {
        if (grant.getGrantType() != ResourceGrantType.CLIENT_CREDENTIALS) {
            return;
        }
        events.enqueueServiceGrantProjection(clientRealm, client, grant);
    }

    private List<String> explicitApps(List<String> appCodes, Map<String, Long> versions) {
        Objects.requireNonNull(appCodes, "appCodes");
        Objects.requireNonNull(versions, "expectedVersions");
        List<String> exactApps = appCodes.stream().map(value -> requiredText(value, "appCode", 128)).toList();
        if (exactApps.isEmpty() || exactApps.size() > RESOURCE_BATCH_LIMIT
                || new HashSet<>(exactApps).size() != exactApps.size()
                || exactApps.stream().anyMatch("*"::equals)
                || !versions.keySet().equals(Set.copyOf(exactApps))) {
            throw new IllegalArgumentException("OAUTH_RESOURCE_BATCH_SELECTION_INVALID");
        }
        versions.values().forEach(version -> {
            if (version == null || version < 0L) {
                throw new IllegalArgumentException("OAUTH_RESOURCE_BATCH_VERSION_INVALID");
            }
        });
        return List.copyOf(exactApps);
    }

    private void validateGrantVersions(
            List<String> appCodes,
            Map<String, Long> versions,
            BatchClientResourceGrantDTO.Action action) {
        Objects.requireNonNull(versions, "expectedGrantVersions");
        Set<String> exactApps = Set.copyOf(appCodes);
        if (!exactApps.containsAll(versions.keySet())
                || action == BatchClientResourceGrantDTO.Action.DELETE && !versions.keySet().equals(exactApps)
                || versions.values().stream().anyMatch(version -> version == null || version < 0L)) {
            throw new IllegalArgumentException("OAUTH_GRANT_BATCH_VERSION_INVALID");
        }
    }

    private long expected(Map<String, Long> versions, String appCode) {
        Long value = versions.get(appCode);
        if (value == null || value < 0L) {
            throw new IllegalArgumentException("OAUTH_RESOURCE_EXPECTED_VERSION_REQUIRED");
        }
        return value;
    }

    private static TrustedRealmBO requirePlatform(TrustedRealmBO realm) {
        TrustedRealmBO value = requireRealm(realm);
        if (value.getRealmType() != RealmTypeEnum.PLATFORM) {
            throw new IllegalArgumentException("OAUTH_PLATFORM_REALM_REQUIRED");
        }
        return value;
    }

    private static TrustedRealmBO requireRealm(TrustedRealmBO realm) {
        TrustedRealmBO value = Objects.requireNonNull(realm, "trustedRealm");
        value.validateTrustedPartition();
        return value;
    }

    private static OAuthResourceBO.OAuthResourceBOBuilder copy(OAuthResourceBO value) {
        return OAuthResourceBO.builder()
                .id(value.getId())
                .version(value.getVersion())
                .resourceServerId(value.getResourceServerId())
                .resourceUri(value.getResourceUri())
                .bizCode(value.getBizCode())
                .appCode(value.getAppCode())
                .environment(value.getEnvironment())
                .displayName(value.getDisplayName())
                .managementClientId(value.getManagementClientId())
                .managementClientPk(value.getManagementClientPk())
                .rbacApplicationCode(value.getRbacApplicationCode())
                .entryPermissionCode(value.getEntryPermissionCode())
                .status(value.getStatus())
                .admissionTicketTtl(value.getAdmissionTicketTtl())
                .createdAt(value.getCreatedAt())
                .updatedAt(value.getUpdatedAt());
    }

    private static OAuthGrantBO.OAuthGrantBOBuilder copy(OAuthGrantBO value) {
        return OAuthGrantBO.builder()
                .id(value.getId())
                .version(value.getVersion())
                .clientPk(value.getClientPk())
                .resourceServerId(value.getResourceServerId())
                .grantType(value.getGrantType())
                .allowedScopes(value.getAllowedScopes())
                .status(value.getStatus())
                .resourceVersion(value.getResourceVersion());
    }

    private static String exact(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }

    private static String requiredText(String value, String field, int maxLength) {
        String exact = exact(value, field);
        if (exact.length() > maxLength) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return exact;
    }

    private static URI exactHttpsUri(String value, String field) {
        String exact = requiredText(value, field, 2048);
        try {
            URI uri = URI.create(exact);
            if (!uri.isAbsolute() || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getFragment() != null || uri.getQuery() != null
                    || !uri.normalize().toASCIIString().equals(uri.toASCIIString())) {
                throw new IllegalArgumentException(field + " is not a canonical HTTPS URI");
            }
            return uri;
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(field + " is invalid", failure);
        }
    }

    private static Set<String> canonicalScopes(Set<String> scopes) {
        Objects.requireNonNull(scopes, "allowedScopes");
        if (scopes.size() > 64 || scopes.stream().anyMatch(scope -> scope == null
                || !scope.matches("[A-Za-z0-9._:-]{1,128}"))) {
            throw new IllegalArgumentException("OAUTH_GRANT_SCOPES_INVALID");
        }
        return Set.copyOf(new java.util.TreeSet<>(scopes));
    }
}
