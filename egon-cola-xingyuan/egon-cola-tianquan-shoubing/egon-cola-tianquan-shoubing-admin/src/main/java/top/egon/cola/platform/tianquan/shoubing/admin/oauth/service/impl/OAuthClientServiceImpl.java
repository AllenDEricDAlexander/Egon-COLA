package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.converter.OAuthClientViewConverter;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.CreateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.RotateClientSecretDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.dto.UpdateOAuthClientDTO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.CreatedOAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.OAuthClientVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.vo.RotatedClientSecretVO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthResourceRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthClientService;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.OAuthClientRevokeCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService;
import top.egon.cola.platform.tianquan.shoubing.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.tianquan.shoubing.core.audit.IdentitySecurityEventPort;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;

@Service("OAuthClientServiceImpl")
@Slf4j
@RequiredArgsConstructor
public class OAuthClientServiceImpl implements OAuthClientService {

    private static final int BCrypt_COST = 12;
    private static final int SECRET_BYTES = 32;

    @NonNull
    @Qualifier("oAuthClientRepository")
    private final OAuthClientRepository clients;
    @NonNull
    @Qualifier("oAuthRealmRepository")
    private final OAuthRealmRepository realms;
    @NonNull
    @Qualifier("oAuthResourceRepository")
    private final OAuthResourceRepository resources;
    @NonNull
    @Qualifier("oAuthGrantRepository")
    private final OAuthGrantRepository grants;
    @NonNull
    @Qualifier("OAuthClientViewConverterImpl")
    private final OAuthClientViewConverter views;
    @NonNull
    @Qualifier("identityOutboxPublisher")
    private final IdentitySecurityEventPort securityEvents;
    @NonNull
    @Qualifier("tokenRevocationService")
    private final TokenRevocationService tokenRevocationService;
    @NonNull
    @Qualifier("idpSecureRandom")
    private final SecureRandom secureRandom;
    @NonNull
    @Qualifier("idpClock")
    private final Clock clock;
    @Value("${spring.profiles.active:}")
    private final String activeProfiles;
    private final BCryptPasswordEncoder secretEncoder = new BCryptPasswordEncoder(BCrypt_COST);

    @Override
    @Transactional(readOnly = true)
    public List<OAuthClientVO> list(TrustedRealmBO trustedRealm) {
        TrustedRealmBO realm = requireRealm(trustedRealm);
        return clients.findAllByRealm(realm).stream()
                .sorted(Comparator.comparing(OAuthClientBO::getClientId))
                .map(client -> view(client, realm))
                .toList();
    }

    @Override
    @Transactional
    public CreatedOAuthClientVO create(
            TrustedRealmBO trustedRealm,
            TrustedRealmBO platformRealm,
            CreateOAuthClientDTO command,
            String operatorSub) {
        TrustedRealmBO realm = requireRealm(trustedRealm);
        TrustedRealmBO trustedPlatform = requirePlatformRealm(platformRealm);
        lockRealm(realm);
        CreateOAuthClientDTO input = Objects.requireNonNull(command, "command");
        String operator = operator(operatorSub);
        validateCreate(realm, input);
        if (clients.findByClientId(realm, input.getClientId()).isPresent()) {
            throw new IllegalStateException("OAUTH_CLIENT_ALREADY_EXISTS");
        }
        if (input.getAppId() != null && clients.findAllByRealm(realm).stream()
                .anyMatch(existing -> input.getAppId().equals(existing.getAppId()))) {
            throw new IllegalStateException("OAUTH_CLIENT_APP_ID_ALREADY_EXISTS");
        }

        List<String> redirectUris = exactRedirectUris(input.getRedirectUris());
        List<String> resourceUris = exactUris(input.getResourceUris(), input.getPurpose() == OAuthClientPurposeEnum.USER);
        List<String> allowedScopes = exactScopes(input.getAllowedScopes());
        List<OAuthResourceBO> userResources = input.getPurpose() == OAuthClientPurposeEnum.USER
                ? resolveActiveResources(trustedPlatform, resourceUris)
                : List.of();

        Instant now = clock.instant();
        ClientSecretMaterialVO secret = input.getClientType() == OAuthClient.ClientType.CONFIDENTIAL
                ? generateSecret()
                : null;
        OAuthClientBO created = OAuthClientBO.builder()
                .version(0L)
                .clientId(exact(input.getClientId(), "clientId"))
                .appId(input.getAppId())
                .clientName(requiredText(input.getClientName(), "clientName", 200))
                .clientType(input.getClientType())
                .purpose(input.getPurpose())
                .status(OAuthClient.Status.ACTIVE)
                .secretHash(secret == null ? null : secret.hash())
                .secretHint(secret == null ? null : secret.hint())
                .credentialVersion(secret == null ? 0L : 1L)
                .redirectUris(redirectUris)
                .resourceUris(resourceUris)
                .allowedScopes(allowedScopes)
                .accessTtlSeconds(input.getAccessTokenTtlSeconds())
                .refreshTtlSeconds(input.getRefreshTokenTtlSeconds())
                .build();
        OAuthClientBO saved = clients.save(realm, created, 0L);
        tokenRevocationService.revokeClient(realm, clientRevokeCommand(saved, "CLIENT_CREATED"));
        if (!userResources.isEmpty()) {
            for (OAuthResourceBO resource : userResources) {
                saveUserDelegationGrant(realm, saved, resource);
            }
        }
        appendAudit("IDENTITY_OAUTH_CLIENT_CREATED", operator, "CLIENT_CREATED:" + saved.getClientId(), now);

        return views.toCreatedTarget(saved, realm.getIssuer(), secret == null ? null : secret.plaintext());
    }

    @Override
    @Transactional
    public RotatedClientSecretVO rotateSecret(
            TrustedRealmBO trustedRealm,
            String clientId,
            RotateClientSecretDTO command,
            String operatorSub) {
        TrustedRealmBO realm = requireRealm(trustedRealm);
        lockRealm(realm);
        String exactClientId = exact(clientId, "clientId");
        RotateClientSecretDTO input = Objects.requireNonNull(command, "command");
        String operator = operator(operatorSub);
        OAuthClientBO client = clients.findByClientIdForUpdate(realm, exactClientId)
                .orElseThrow(() -> new IllegalArgumentException("OAUTH_CLIENT_NOT_FOUND"));
        if (client.getClientType() != OAuthClient.ClientType.CONFIDENTIAL
                || client.getStatus() != OAuthClient.Status.ACTIVE) {
            throw new IllegalStateException("OAUTH_CLIENT_SECRET_ROTATION_NOT_ALLOWED");
        }
        if (client.getVersion() != input.getExpectedVersion()) {
            throw new OptimisticLockingFailureException("OAUTH_CLIENT_VERSION_CONFLICT");
        }
        ClientSecretMaterialVO secret = generateSecret();
        long nextVersion = Math.addExact(client.getVersion(), 1L);
        OAuthClientBO changed = copy(client)
                .version(nextVersion)
                .credentialVersion(Math.addExact(client.getCredentialVersion(), 1L))
                .secretHash(secret.hash())
                .secretHint(secret.hint())
                .updatedAt(clock.instant())
                .build();
        OAuthClientBO saved = clients.save(realm, changed, client.getVersion());
        tokenRevocationService.revokeClient(realm, clientRevokeCommand(saved, "CLIENT_SECRET_ROTATED"));
        Instant now = clock.instant();
        appendAudit("IDENTITY_OAUTH_CLIENT_SECRET_ROTATED", operator,
                "SECRET_ROTATED:" + saved.getClientId(), now);
        return views.toRotatedTarget(saved, realm.getIssuer(), secret.plaintext(), now);
    }

    @Override
    @Transactional
    public OAuthClientVO update(TrustedRealmBO trustedRealm, String clientId, UpdateOAuthClientDTO command) {
        TrustedRealmBO realm = requireRealm(trustedRealm);
        lockRealm(realm);
        UpdateOAuthClientDTO input = Objects.requireNonNull(command, "command");
        OAuthClientBO client = lockClient(realm, clientId);
        if (client.getVersion() != input.getExpectedVersion()) {
            throw new OptimisticLockingFailureException("OAUTH_CLIENT_VERSION_CONFLICT");
        }
        if (input.getAccessTokenTtlSeconds() < 300 || input.getAccessTokenTtlSeconds() > 600
                || input.getRefreshTokenTtlSeconds() < 0 || input.getRefreshTokenTtlSeconds() > 604_800) {
            throw new IllegalArgumentException("OAUTH_CLIENT_TTL_INVALID");
        }
        validateRefreshTtl(client, input.getRefreshTokenTtlSeconds());
        if (input.getAllowedScopes() != null && input.getAllowedScopes().isEmpty()) {
            throw new IllegalArgumentException("OAUTH_CLIENT_SCOPES_EMPTY");
        }
        List<String> scopes = input.getAllowedScopes() == null
                ? client.getAllowedScopes()
                : exactScopes(input.getAllowedScopes());
        OAuthClientBO changed = copy(client)
                .version(Math.addExact(client.getVersion(), 1L))
                .clientName(requiredText(input.getClientName(), "clientName", 200))
                .status(Objects.requireNonNull(input.getStatus(), "status"))
                .accessTtlSeconds(input.getAccessTokenTtlSeconds())
                .refreshTtlSeconds(input.getRefreshTokenTtlSeconds())
                .allowedScopes(scopes)
                .updatedAt(clock.instant())
                .build();
        OAuthClientBO saved = clients.save(realm, changed, client.getVersion());
        recordSecurityChange(realm, client, saved, "CLIENT_CONFIGURATION_CHANGED");
        return view(saved, realm);
    }

    private static void validateRefreshTtl(OAuthClientBO client, int refreshTtlSeconds) {
        if (client.getPurpose() == OAuthClientPurposeEnum.USER
                && client.getClientType() == OAuthClient.ClientType.CONFIDENTIAL) {
            if (refreshTtlSeconds < 3_600 || refreshTtlSeconds > 604_800) {
                throw new IllegalArgumentException("OAUTH_USER_CLIENT_REFRESH_TTL_INVALID");
            }
            return;
        }
        if (refreshTtlSeconds != 0) {
            throw new IllegalArgumentException("OAUTH_CLIENT_REFRESH_TTL_INVALID");
        }
    }

    @Override
    @Transactional
    public OAuthClientVO putRedirectUri(TrustedRealmBO trustedRealm, String clientId, String redirectUri) {
        TrustedRealmBO realm = requireRealm(trustedRealm);
        lockRealm(realm);
        OAuthClientBO client = lockUserClient(realm, clientId);
        List<String> redirects = new ArrayList<>(client.getRedirectUris());
        String value = exactRedirectUri(redirectUri);
        if (!redirects.contains(value)) {
            redirects.add(value);
        }
        return saveClient(realm, client, copy(client).redirectUris(sorted(redirects)).build());
    }

    @Override
    @Transactional
    public OAuthClientVO deleteRedirectUri(TrustedRealmBO trustedRealm, String clientId, String redirectUri) {
        TrustedRealmBO realm = requireRealm(trustedRealm);
        lockRealm(realm);
        OAuthClientBO client = lockUserClient(realm, clientId);
        List<String> redirects = new ArrayList<>(client.getRedirectUris());
        redirects.remove(exactRedirectUri(redirectUri));
        if (redirects.isEmpty()) {
            throw new IllegalArgumentException("OAUTH_USER_CLIENT_REDIRECT_URI_REQUIRED");
        }
        return saveClient(realm, client, copy(client).redirectUris(sorted(redirects)).build());
    }

    @Override
    @Transactional
    public OAuthClientVO putResourceUri(
            TrustedRealmBO trustedRealm,
            TrustedRealmBO platformRealm,
            String clientId,
            String resourceUri) {
        TrustedRealmBO realm = requireRealm(trustedRealm);
        lockRealm(realm);
        OAuthClientBO client = lockUserClient(realm, clientId);
        String exactUri = exactHttpsUri(resourceUri, "resourceUri");
        OAuthResourceBO resource = activeResource(platformRealm, exactUri);
        List<String> resourceUris = new ArrayList<>(client.getResourceUris());
        if (!resourceUris.contains(exactUri)) {
            resourceUris.add(exactUri);
        }
        OAuthClientBO saved = clients.save(realm, copy(client)
                .version(Math.addExact(client.getVersion(), 1L))
                .resourceUris(sorted(resourceUris))
                .updatedAt(clock.instant())
                .build(), client.getVersion());
        recordSecurityChange(realm, client, saved, "CLIENT_RESOURCE_CHANGED");
        saveUserDelegationGrant(realm, saved, resource);
        return view(saved, realm);
    }

    @Override
    @Transactional
    public OAuthClientVO deleteResourceUri(
            TrustedRealmBO trustedRealm,
            TrustedRealmBO platformRealm,
            String clientId,
            String resourceUri) {
        TrustedRealmBO realm = requireRealm(trustedRealm);
        lockRealm(realm);
        OAuthClientBO client = lockUserClient(realm, clientId);
        String exactUri = exactHttpsUri(resourceUri, "resourceUri");
        OAuthResourceBO resource = activeResource(platformRealm, exactUri);
        List<String> resourceUris = new ArrayList<>(client.getResourceUris());
        resourceUris.remove(exactUri);
        OAuthClientBO saved = clients.save(realm, copy(client)
                .version(Math.addExact(client.getVersion(), 1L))
                .resourceUris(sorted(resourceUris))
                .updatedAt(clock.instant())
                .build(), client.getVersion());
        recordSecurityChange(realm, client, saved, "CLIENT_RESOURCE_CHANGED");
        grants.findGrantForUpdate(realm, saved.getId(), resource.getResourceServerId(), ResourceGrantType.USER_DELEGATION)
                .ifPresent(grant -> grants.save(realm, copyGrant(grant)
                        .version(Math.addExact(grant.getVersion(), 1L))
                        .status(ClientResourceGrant.Status.DISABLED)
                        .resourceVersion(resource.getVersion())
                        .build(), grant.getVersion()));
        return view(saved, realm);
    }

    private OAuthClientVO saveClient(TrustedRealmBO realm, OAuthClientBO current, OAuthClientBO proposed) {
        OAuthClientBO changed = copy(proposed)
                .version(Math.addExact(current.getVersion(), 1L))
                .updatedAt(clock.instant())
                .build();
        OAuthClientBO saved = clients.save(realm, changed, current.getVersion());
        recordSecurityChange(realm, current, saved, "CLIENT_REDIRECT_CHANGED");
        return view(saved, realm);
    }

    private void recordSecurityChange(
            TrustedRealmBO realm,
            OAuthClientBO previous,
            OAuthClientBO current,
            String reasonCode) {
        if (securityMaterialChanged(previous, current)) {
            tokenRevocationService.revokeClient(realm, clientRevokeCommand(current, reasonCode));
        }
    }

    private static boolean securityMaterialChanged(OAuthClientBO previous, OAuthClientBO current) {
        return previous.getStatus() != current.getStatus()
                || !Objects.equals(previous.getCredentialVersion(), current.getCredentialVersion())
                || !Objects.equals(previous.getRedirectUris(), current.getRedirectUris())
                || !Objects.equals(previous.getResourceUris(), current.getResourceUris())
                || !Objects.equals(previous.getAllowedScopes(), current.getAllowedScopes())
                || !Objects.equals(previous.getAccessTtlSeconds(), current.getAccessTtlSeconds())
                || !Objects.equals(previous.getRefreshTtlSeconds(), current.getRefreshTtlSeconds());
    }

    private static OAuthClientRevokeCommand clientRevokeCommand(OAuthClientBO client, String reasonCode) {
        if (client.getId() == null || client.getId() <= 0L) {
            throw new IllegalStateException("OAUTH_CLIENT_SECURITY_ID_INVALID");
        }
        return OAuthClientRevokeCommand.builder().clientPk(client.getId()).reasonCode(reasonCode).build();
    }

    private OAuthClientBO lockUserClient(TrustedRealmBO realm, String clientId) {
        OAuthClientBO client = lockClient(realm, clientId);
        if (client.getPurpose() != OAuthClientPurposeEnum.USER) {
            throw new IllegalStateException("OAUTH_USER_CLIENT_REQUIRED");
        }
        return client;
    }

    private OAuthClientBO lockClient(TrustedRealmBO realm, String clientId) {
        return clients.findByClientIdForUpdate(realm, exact(clientId, "clientId"))
                .orElseThrow(() -> new IllegalArgumentException("OAUTH_CLIENT_NOT_FOUND"));
    }

    private void lockRealm(TrustedRealmBO realm) {
        realms.findByRealmKeyForUpdate(realm)
                .filter(value -> "ACTIVE".equals(value.getStatus()))
                .orElseThrow(() -> new IllegalStateException("OAUTH_CLIENT_REALM_NOT_ACTIVE"));
    }

    private OAuthClientVO view(OAuthClientBO client, TrustedRealmBO realm) {
        return views.toTarget(client, realm.getIssuer());
    }

    private void saveUserDelegationGrant(TrustedRealmBO realm, OAuthClientBO client, OAuthResourceBO resource) {
        OAuthGrantBO current = grants.findGrantForUpdate(
                realm, client.getId(), resource.getResourceServerId(), ResourceGrantType.USER_DELEGATION).orElse(null);
        if (current == null) {
            grants.save(realm, OAuthGrantBO.builder()
                    .version(0L)
                    .clientPk(client.getId())
                    .resourceServerId(resource.getResourceServerId())
                    .grantType(ResourceGrantType.USER_DELEGATION)
                    .allowedScopes(List.of())
                    .status(ClientResourceGrant.Status.ACTIVE)
                    .resourceVersion(resource.getVersion())
                    .build(), 0L);
        } else if (current.getStatus() != ClientResourceGrant.Status.ACTIVE
                || !Objects.equals(current.getResourceVersion(), resource.getVersion())) {
            grants.save(realm, copyGrant(current)
                    .version(Math.addExact(current.getVersion(), 1L))
                    .status(ClientResourceGrant.Status.ACTIVE)
                    .resourceVersion(resource.getVersion())
                    .build(), current.getVersion());
        }
    }

    private List<OAuthResourceBO> resolveActiveResources(TrustedRealmBO platformRealm, List<String> resourceUris) {
        return resourceUris.stream().map(uri -> activeResource(platformRealm, uri)).toList();
    }

    private OAuthResourceBO activeResource(TrustedRealmBO platformRealm, String resourceUri) {
        TrustedRealmBO trustedPlatform = requirePlatformRealm(platformRealm);
        OAuthResourceBO resource = resources.findByResourceUri(trustedPlatform, URI.create(resourceUri))
                .orElseThrow(() -> new IllegalArgumentException("OAUTH_RESOURCE_NOT_FOUND"));
        if (resource.getStatus() != ResourceServerStatus.ACTIVE) {
            throw new IllegalStateException("OAUTH_RESOURCE_NOT_ACTIVE");
        }
        return resource;
    }

    private void validateCreate(TrustedRealmBO realm, CreateOAuthClientDTO input) {
        Objects.requireNonNull(input.getClientType(), "clientType");
        Objects.requireNonNull(input.getPurpose(), "purpose");
        if (input.getClientId() == null || !input.getClientId().matches("[A-Za-z0-9._-]{1,128}")) {
            throw new IllegalArgumentException("OAUTH_CLIENT_ID_INVALID");
        }
        if (input.getClientName() == null || input.getClientName().isBlank() || input.getClientName().length() > 200) {
            throw new IllegalArgumentException("OAUTH_CLIENT_NAME_INVALID");
        }
        if (input.getAccessTokenTtlSeconds() < 300 || input.getAccessTokenTtlSeconds() > 600
                || input.getRefreshTokenTtlSeconds() < 0 || input.getRefreshTokenTtlSeconds() > 604_800) {
            throw new IllegalArgumentException("OAUTH_CLIENT_TTL_INVALID");
        }
        if (realm.getRealmType() == top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum.PLATFORM
                && input.getPurpose() != OAuthClientPurposeEnum.SERVICE) {
            throw new IllegalArgumentException("OAUTH_PLATFORM_CLIENT_MUST_BE_SERVICE");
        }
        if (input.getClientType() == OAuthClient.ClientType.PUBLIC) {
            if (input.getPurpose() != OAuthClientPurposeEnum.USER || input.getAppId() != null
                    || input.getRefreshTokenTtlSeconds() != 0) {
                throw new IllegalArgumentException("OAUTH_PUBLIC_CLIENT_FIELDS_INVALID");
            }
        } else if (input.getAppId() == null || input.getAppId().isBlank()) {
            throw new IllegalArgumentException("OAUTH_CONFIDENTIAL_CLIENT_APP_ID_REQUIRED");
        } else if (!input.getAppId().matches("[a-z][a-z0-9-]{2,127}")) {
            throw new IllegalArgumentException("OAUTH_CLIENT_APP_ID_INVALID");
        }
        if (input.getPurpose() == OAuthClientPurposeEnum.SERVICE
                && input.getAllowedScopes() != null
                && input.getAllowedScopes().stream().anyMatch(scope -> scope == null
                        || !scope.matches("[A-Za-z0-9._:-]{1,128}"))) {
            throw new IllegalArgumentException("OAUTH_CLIENT_SCOPE_INVALID");
        }
        if (input.getPurpose() == OAuthClientPurposeEnum.USER) {
            if (input.getRedirectUris() == null || input.getRedirectUris().isEmpty()
                    || input.getResourceUris() == null || input.getResourceUris().isEmpty()
                    || input.getAllowedScopes() == null || input.getAllowedScopes().isEmpty()) {
                throw new IllegalArgumentException("OAUTH_USER_CLIENT_GRANT_FIELDS_REQUIRED");
            }
            if (input.getClientType() == OAuthClient.ClientType.CONFIDENTIAL
                    && (input.getRefreshTokenTtlSeconds() < 3600
                            || input.getRefreshTokenTtlSeconds() > 604800)) {
                throw new IllegalArgumentException("OAUTH_USER_CLIENT_REFRESH_TTL_INVALID");
            }
        } else if (input.getClientType() != OAuthClient.ClientType.CONFIDENTIAL
                || input.getRedirectUris() == null || !input.getRedirectUris().isEmpty()
                || input.getResourceUris() == null || !input.getResourceUris().isEmpty()
                || input.getRefreshTokenTtlSeconds() != 0) {
            throw new IllegalArgumentException("OAUTH_SERVICE_CLIENT_FIELDS_INVALID");
        }
    }

    private ClientSecretMaterialVO generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        String plaintext = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        java.util.Arrays.fill(bytes, (byte) 0);
        return new ClientSecretMaterialVO(
                plaintext, secretEncoder.encode(plaintext), plaintext.substring(plaintext.length() - 4));
    }

    private void appendAudit(String eventType, String operator, String target, Instant occurredAt) {
        securityEvents.append(new IdentitySecurityEvent(eventType, operator, target, "ADMIN_API", occurredAt));
    }

    private static OAuthClientBO.OAuthClientBOBuilder copy(OAuthClientBO value) {
        return OAuthClientBO.builder()
                .id(value.getId())
                .version(value.getVersion())
                .clientId(value.getClientId())
                .appId(value.getAppId())
                .clientName(value.getClientName())
                .clientType(value.getClientType())
                .purpose(value.getPurpose())
                .status(value.getStatus())
                .secretHash(value.getSecretHash())
                .secretHint(value.getSecretHint())
                .credentialVersion(value.getCredentialVersion())
                .redirectUris(value.getRedirectUris())
                .resourceUris(value.getResourceUris())
                .allowedScopes(value.getAllowedScopes())
                .accessTtlSeconds(value.getAccessTtlSeconds())
                .refreshTtlSeconds(value.getRefreshTtlSeconds())
                .createdAt(value.getCreatedAt())
                .updatedAt(value.getUpdatedAt());
    }

    private static OAuthGrantBO.OAuthGrantBOBuilder copyGrant(OAuthGrantBO value) {
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

    private static TrustedRealmBO requireRealm(TrustedRealmBO realm) {
        TrustedRealmBO value = Objects.requireNonNull(realm, "trustedRealm");
        value.validateTrustedPartition();
        return value;
    }

    private static TrustedRealmBO requirePlatformRealm(TrustedRealmBO realm) {
        TrustedRealmBO value = requireRealm(realm);
        if (value.getRealmType() != top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum.PLATFORM) {
            throw new IllegalArgumentException("OAUTH_PLATFORM_REALM_REQUIRED");
        }
        return value;
    }

    private static List<String> exactUris(List<String> values, boolean required) {
        List<String> result = exactValues(values, "resourceUris", 64, 2048).stream()
                .map(value -> exactHttpsUri(value, "resourceUri"))
                .sorted()
                .toList();
        if (required && result.isEmpty()) {
            throw new IllegalArgumentException("OAUTH_RESOURCE_URI_REQUIRED");
        }
        return result;
    }

    private List<String> exactRedirectUris(List<String> values) {
        return exactValues(values, "redirectUris", 16, 2048).stream()
                .map(this::exactRedirectUri)
                .toList();
    }

    private String exactRedirectUri(String value) {
        String exact = requiredText(value, "redirectUri", 2048);
        try {
            URI uri = URI.create(exact);
            boolean secureHttps = "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
            boolean localLoopback = isLocalProfile()
                    && "http".equalsIgnoreCase(uri.getScheme())
                    && "127.0.0.1".equals(uri.getHost())
                    && uri.getPort() > 0 && uri.getPort() <= 65_535;
            if ((!secureHttps && !localLoopback) || uri.getUserInfo() != null || uri.getQuery() != null
                    || uri.getFragment() != null || exact.contains("*")
                    || !uri.normalize().toASCIIString().equals(uri.toASCIIString())) {
                throw new IllegalArgumentException("OAUTH_REDIRECT_URI_INVALID");
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("OAUTH_REDIRECT_URI_INVALID", failure);
        }
    }

    private boolean isLocalProfile() {
        return activeProfiles != null && java.util.Arrays.stream(activeProfiles.split("[,;]"))
                .map(String::trim)
                .anyMatch("local"::equals);
    }

    private static List<String> exactScopes(List<String> values) {
        List<String> scopes = exactValues(values, "allowedScopes", 64, 128);
        if (scopes.stream().anyMatch(scope -> !scope.matches("[A-Za-z0-9._:-]{1,128}"))) {
            throw new IllegalArgumentException("OAUTH_CLIENT_SCOPE_INVALID");
        }
        return scopes;
    }

    private static List<String> exactValues(List<String> values, String field, int maximum, int maxLength) {
        Objects.requireNonNull(values, field);
        if (values.size() > maximum) {
            throw new IllegalArgumentException(field + " exceeds its maximum size");
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String exact = exact(value, field);
            if (exact.length() > maxLength || !unique.add(exact)) {
                throw new IllegalArgumentException(field + " contains invalid or duplicate values");
            }
        }
        return unique.stream().sorted().toList();
    }

    private static String exactHttpsUri(String value, String field) {
        String exact = exact(value, field);
        try {
            URI uri = URI.create(exact);
            URI normalized = uri.normalize();
            if (!uri.isAbsolute() || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                    || !uri.toASCIIString().equals(normalized.toASCIIString())) {
                throw new IllegalArgumentException(field + " is not canonical HTTPS URI");
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException(field + " is invalid", failure);
        }
    }

    private static String requiredText(String value, String field, int maximum) {
        String exact = exact(value, field);
        if (exact.length() > maximum) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return exact;
    }

    private static String exact(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }

    private static List<String> sorted(List<String> values) {
        return values.stream().sorted().toList();
    }

    private static String operator(String value) {
        String exact = requiredText(value, "operatorSub", 64);
        if (!exact.matches("[A-Za-z0-9._~-]{1,64}")) {
            throw new IllegalArgumentException("operatorSub has invalid format");
        }
        return exact;
    }

    private record ClientSecretMaterialVO(String plaintext, String hash, String hint) {
    }
}
