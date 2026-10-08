package top.egon.cola.platform.tianquan.shoubing.admin.resource.repository;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.TenantIssuerService;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthResourceRepository;
import top.egon.cola.platform.tianquan.shoubing.core.port.ResourceServerStore;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServer;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;

@Slf4j
@RequiredArgsConstructor
public class MpResourceServerStore implements ResourceServerStore {

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
    @Qualifier("tenantIssuerService")
    private final TenantIssuerService issuerService;
    @NonNull
    @Qualifier("oauthManagementRealmResolver")
    private final OAuthManagementRealmResolver realmResolver;

    @Override
    @Transactional(readOnly = true)
    public Optional<ResourceServer> findById(String resourceServerId) {
        return resources.findByResourceServerId(platformRealm(), required(resourceServerId, "resourceServerId"))
                .map(MpResourceServerStore::resource);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResourceServer> findByUri(URI resourceUri) {
        return resources.findByResourceUri(platformRealm(), Objects.requireNonNull(resourceUri, "resourceUri"))
                .map(MpResourceServerStore::resource);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResourceServer> findByScope(String bizCode, String appCode, String environment) {
        return resources.findBySource(platformRealm(), required(bizCode, "bizCode"),
                        required(appCode, "appCode"), required(environment, "environment"))
                .map(MpResourceServerStore::resource);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResourceServer> findByManagementClientId(String clientId) {
        OAuthClientBO client = clients.findByClientId(platformRealm(), required(clientId, "clientId")).orElse(null);
        if (client == null) {
            return Optional.empty();
        }
        return resources.findByManagementClientPk(platformRealm(), client.getId())
                .map(MpResourceServerStore::resource);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientResourceGrant> findGrant(
            String clientId,
            String resourceServerId,
            ResourceGrantType grantType,
            String tenantId) {
        TrustedRealmBO realm = issuerService.resolveCurrentIssuer();
        ResourceGrantType type = Objects.requireNonNull(grantType, "grantType");
        String expectedTenant = type == ResourceGrantType.CLIENT_CREDENTIALS
                ? realm.getBusinessTenantId() == null ? null : realm.getBusinessTenantId().toString()
                : null;
        if (!Objects.equals(expectedTenant, tenantId)) {
            return Optional.empty();
        }
        OAuthClientBO client = clients.findByClientId(realm, required(clientId, "clientId")).orElse(null);
        if (client == null) {
            return Optional.empty();
        }
        return grants.findGrant(realm, client.getId(), required(resourceServerId, "resourceServerId"), type)
                .map(value -> grant(realm, client, value));
    }

    private TrustedRealmBO platformRealm() {
        return realmResolver.platformRealm();
    }

    private static ResourceServer resource(OAuthResourceBO value) {
        return new ResourceServer(value.getResourceServerId(), value.getResourceUri(), value.getBizCode(),
                value.getAppCode(), value.getEnvironment(), value.getManagementClientId(),
                value.getRbacApplicationCode(), value.getEntryPermissionCode(), value.getAdmissionTicketTtl(),
                value.getStatus(), value.getVersion());
    }

    private static ClientResourceGrant grant(TrustedRealmBO realm, OAuthClientBO client, OAuthGrantBO value) {
        String tenantId = realm.getBusinessTenantId() == null ? null : realm.getBusinessTenantId().toString();
        ServiceTokenContext context = value.getGrantType() == ResourceGrantType.CLIENT_CREDENTIALS
                ? realm.getBusinessTenantId() == null ? ServiceTokenContext.PLATFORM : ServiceTokenContext.TENANT
                : null;
        return new ClientResourceGrant(client.getClientId(), value.getResourceServerId(), value.getGrantType(),
                tenantId, Set.copyOf(value.getAllowedScopes()), value.getStatus(), value.getVersion(), context);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }
}
