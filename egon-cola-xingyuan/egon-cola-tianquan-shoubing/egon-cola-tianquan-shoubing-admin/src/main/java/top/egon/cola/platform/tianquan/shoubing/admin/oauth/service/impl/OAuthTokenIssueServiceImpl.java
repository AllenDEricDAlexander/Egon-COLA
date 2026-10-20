package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.common.id.snowflake.SnowflakeIdGenerator;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthConsentBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthSecuritySnapshotBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.command.ConsumeOAuthTokenCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthAuthorizationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthConsentRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthTokenIssueService;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.repository.OAuthGrantRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSecurityProjectionComponent;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.port.ResourceServerStore;
import top.egon.cola.platform.tianquan.shoubing.core.port.TenantMembershipPort;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ClientCredentialsAccessPolicy;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServer;

@Service("oauthTokenIssueService")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OAuthTokenIssueServiceImpl implements OAuthTokenIssueService {

    private static final Set<String> IDENTITY_SCOPES = Set.of("openid", "profile", "email", "address", "phone", "offline_access");

    private static final String INVALID_GRANT = "invalid_grant";
    private static final String TOKEN_ISSUED = "TOKEN_ISSUED";
    private static final String AUDIT_OUTBOX_CHANNEL = "oauth-audit";
    private static final String AUDIT_OUTBOX_DESTINATION = "oauth.audit.token-issued.v1";

    @Qualifier("oAuthTokenRepository")
    private final OAuthTokenRepository tokenRepository;
    @Qualifier("oAuthAuthorizationRepository")
    private final OAuthAuthorizationRepository authorizationRepository;
    @Qualifier("oAuthClientRepository")
    private final OAuthClientRepository clientRepository;
    @Qualifier("oAuthGrantRepository")
    private final OAuthGrantRepository grantRepository;
    @Qualifier("oAuthConsentRepository")
    private final OAuthConsentRepository consentRepository;
    @Qualifier("oAuthRealmRepository")
    private final OAuthRealmRepository realmRepository;
    @Qualifier("oAuthAuditRepository")
    private final OAuthAuditRepository auditRepository;
    @Qualifier("transactionalOutbox")
    private final TransactionalOutbox outbox;
    @Qualifier("oauthSecurityProjectionComponent")
    private final OAuthSecurityProjectionComponent securityProjection;
    @Qualifier("clientCredentialsAccessPolicy")
    private final ClientCredentialsAccessPolicy clientCredentialsAccessPolicy;
    @Qualifier("resourceServerStore")
    private final ResourceServerStore resourceServerStore;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;
    @Qualifier("tenantMembershipPort")
    private final TenantMembershipPort tenantMemberships;
    @Value("${egon.tianquan-shoubing.oauth.platform-management-tenant-id}")
    private final long platformManagementTenantId;

    @PostConstruct
    void validatePlatformManagementTenant() {
        if (platformManagementTenantId <= 0L) {
            throw new IllegalStateException("OAUTH_PLATFORM_MANAGEMENT_TENANT_ID_INVALID");
        }
    }

    @Override
    public Authentication consumeAndIssue(
            TrustedRealmBO trustedRealm,
            ConsumeOAuthTokenCommand command,
            Supplier<Authentication> delegate) {
        TrustedRealmBO realm = Objects.requireNonNull(trustedRealm, "trustedRealm");
        ConsumeOAuthTokenCommand input = Objects.requireNonNull(command, "command");
        Supplier<Authentication> tokenExchange = Objects.requireNonNull(delegate, "delegate");
        OAuthTokenBO tokenCandidate = tokenRepository.findByTokenDigest(realm, input.getTokenDigest())
                .filter(value -> value.getTokenType() == input.getTokenType())
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        OAuthAuthorizationBO authorizationCandidate = authorizationRepository
                .findById(realm, tokenCandidate.getAuthorizationPk())
                .filter(value -> "ACTIVE".equals(value.getStatus()))
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        if (!Objects.equals(authorizationCandidate.getClientPk(), input.getClientPk())
                || !"USER".equals(authorizationCandidate.getPrincipalType())
                || authorizationCandidate.getAbsoluteExpiresAt() == null
                || !authorizationCandidate.getAbsoluteExpiresAt().isAfter(clock.instant())
                || authorizationCandidate.getAttributes() == null
                || authorizationCandidate.getAttributes().getPendingConsent() != null) {
            throw invalidGrant();
        }
        realmRepository.findByRealmKeyForUpdate(realm)
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        OAuthClientBO clientCandidate = clientRepository.findById(realm, input.getClientPk())
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        OAuthClientBO lockedClient = clientRepository.findByClientIdForUpdate(realm, clientCandidate.getClientId())
                .filter(value -> value.getStatus() == OAuthClient.Status.ACTIVE)
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        if (!Objects.equals(clientCandidate.getId(), lockedClient.getId())) {
            throw invalidGrant();
        }
        if (authorizationCandidate.getAttributes() == null
                || authorizationCandidate.getAttributes().getSecuritySnapshot() == null) {
            throw invalidGrant();
        }
        securityProjection.requireCurrentSnapshot(realm, input.getClientPk(), "USER",
                authorizationCandidate.getPrincipalName(), authorizationCandidate.getAttributes().getSecuritySnapshot());
        OAuthConsentBO lockedConsent = consentRepository.findConsentForUpdate(
                        realm, input.getClientPk(), authorizationCandidate.getPrincipalName())
                .filter(value -> "ACTIVE".equals(value.getStatus()))
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        if (!consentedScopes(lockedConsent).containsAll(authorizationCandidate.getAuthorizedScopes())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_scope", "Request rejected", null));
        }
        lockUserGrants(realm, lockedClient, authorizationCandidate.getAudiences(), authorizationCandidate.getAuthorizedScopes());

        OAuthAuthorizationBO authorization = authorizationRepository
                .findByIdForUpdate(realm, tokenCandidate.getAuthorizationPk())
                .filter(value -> "ACTIVE".equals(value.getStatus()))
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        OAuthTokenBO token = tokenRepository.findByTokenDigestForUpdate(realm, input.getTokenDigest())
                .filter(value -> value.getTokenType() == input.getTokenType())
                .filter(value -> value.getAuthorizationPk().equals(authorization.getId()))
                .filter(value -> value.getExpiresAt() != null && value.getExpiresAt().isAfter(clock.instant()))
                .filter(value -> value.getMetadata() != null && !value.getMetadata().isInvalidated())
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        if (!Objects.equals(authorization.getClientPk(), input.getClientPk())
                || !Objects.equals(authorization.getPrincipalName(), authorizationCandidate.getPrincipalName())
                || input.getTokenType() == OAuthTokenTypeEnum.CODE
                        && !matchesAuthorizationCode(token, authorization, input)
                || input.getTokenType() == OAuthTokenTypeEnum.REFRESH
                        && !"USER".equals(authorization.getPrincipalType())) {
            throw invalidGrant();
        }
        if (input.getScopes() != null && !input.getScopes().isEmpty()
                && !authorization.getAuthorizedScopes().containsAll(input.getScopes())) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "invalid_scope", "Request rejected", null));
        }

        Authentication issued = tokenExchange.get();
        recordIssued(realm, authorization.getClientPk(), authorization.getPrincipalName(),
                authorization.getAudiences() == null || authorization.getAudiences().isEmpty()
                        ? "urn:egon:oauth:token-issuance"
                        : String.join(",", authorization.getAudiences()),
                input.getTokenType() == OAuthTokenTypeEnum.CODE
                        ? "AUTHORIZATION_CODE_TOKEN_ISSUED" : "REFRESH_TOKEN_ROTATED");
        return issued;
    }

    @Override
    public ClientCredentialsIssueContext authorizeClientCredentials(
            TrustedRealmBO trustedRealm,
            Long clientPk,
            URI resource,
            Set<String> scopes) {
        TrustedRealmBO realm = Objects.requireNonNull(trustedRealm, "trustedRealm");
        OAuthClientBO client = clientRepository.findById(realm, Objects.requireNonNull(clientPk, "clientPk"))
                .filter(value -> value.getStatus() == OAuthClient.Status.ACTIVE)
                .filter(value -> "SERVICE".equals(value.getPurpose().name()))
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        realmRepository.findByRealmKeyForUpdate(realm)
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        client = clientRepository.findByClientIdForUpdate(realm, client.getClientId())
                .filter(value -> value.getStatus() == OAuthClient.Status.ACTIVE)
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        OAuthSecuritySnapshotBO snapshot = securityProjection.snapshotForAuthorization(
                realm, clientPk, "SERVICE", null);
        URI targetUri = Objects.requireNonNull(resource, "resource").normalize();
        ResourceServer target = resourceServerStore.findByUri(targetUri)
                .filter(ResourceServer::active)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_target", "Request rejected", null)));
        OAuthClient policyClient = new OAuthClient(
                client.getClientId(), client.getAppId(), client.getClientType(), client.getStatus(), false,
                List.of(), java.time.Duration.ofSeconds(client.getAccessTtlSeconds()),
                java.time.Duration.ofDays(1));
        String businessTenant = realm.getBusinessTenantId() == null
                ? null : Long.toString(realm.getBusinessTenantId());
        ClientCredentialsAccessPolicy.ServiceResourceAccess access =
                clientCredentialsAccessPolicy.authorize(policyClient, target, businessTenant, scopes);
        grantRepository.findGrantForUpdate(realm, clientPk, target.resourceServerId(),
                        ResourceGrantType.CLIENT_CREDENTIALS)
                .filter(grant -> grant.getStatus()
                        == top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant.Status.ACTIVE)
                .filter(grant -> Objects.equals(grant.getVersion(), access.grantVersion()))
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_target", "Request rejected", null)));
        if (realm.getRealmType() == top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum.TENANT
                && access.scopeContext() != top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.TENANT
                || realm.getRealmType() == top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum.PLATFORM
                && access.scopeContext() != top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext.PLATFORM) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_target", "Request rejected", null));
        }
        long authorizationId = SnowflakeIdGenerator.nextLongId();
        if (authorizationId <= 0L) {
            throw new IllegalStateException("OAUTH_AUTHORIZATION_ID_INVALID");
        }
        String appId = client.getAppId();
        if (appId == null || appId.isBlank() || !appId.equals(appId.trim())) {
            throw invalidGrant();
        }
        return new ClientCredentialsIssueContext(
                authorizationId,
                snapshot,
                access,
                appId,
                versionedCredentialId(client));
    }

    @Override
    public UserAuthorizationIssueContext authorizeUserToken(
            TrustedRealmBO trustedRealm,
            OAuthAuthorizationBO authorization) {
        TrustedRealmBO realm = Objects.requireNonNull(trustedRealm, "trustedRealm");
        OAuthAuthorizationBO value = Objects.requireNonNull(authorization, "authorization");
        if (!"USER".equals(value.getPrincipalType()) || value.getId() == null
                || value.getAttributes() == null || value.getAttributes().getSecuritySnapshot() == null
                || value.getAttributes().getPendingConsent() != null
                || value.getAbsoluteExpiresAt() == null || !value.getAbsoluteExpiresAt().isAfter(clock.instant())
                || value.getAudiences() == null || value.getAudiences().isEmpty()) {
            throw invalidGrant();
        }
        OAuthClientBO client = clientRepository.findById(realm, value.getClientPk())
                .filter(candidate -> candidate.getStatus() == OAuthClient.Status.ACTIVE)
                .filter(candidate -> "USER".equals(candidate.getPurpose().name()))
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        realmRepository.findByRealmKeyForUpdate(realm)
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        client = clientRepository.findByClientIdForUpdate(realm, client.getClientId())
                .filter(candidate -> candidate.getStatus() == OAuthClient.Status.ACTIVE)
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        OAuthSecuritySnapshotBO snapshot = securityProjection.snapshotForAuthorization(
                realm, value.getClientPk(), "USER", value.getPrincipalName());
        securityProjection.requireCurrentSnapshot(
                realm, value.getClientPk(), "USER", value.getPrincipalName(),
                value.getAttributes().getSecuritySnapshot());
        String membershipTenantId = realm.getBusinessTenantId() == null
                ? Long.toString(platformManagementTenantId)
                : Long.toString(realm.getBusinessTenantId());
        TenantMembershipPort.TenantMembership membership;
        try {
            membership = tenantMemberships.resolve(value.getPrincipalName(), membershipTenantId);
        } catch (TenantMembershipPort.TenantMembershipException unavailable) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    "temporarily_unavailable", "Request temporarily unavailable", null), unavailable);
        }
        if (membership == null || membership.status() != TenantMembershipPort.MembershipStatus.ACTIVE) {
            throw new OAuth2AuthenticationException(new OAuth2Error("access_denied", "Request rejected", null));
        }
        if (!client.getAllowedScopes().containsAll(value.getAuthorizedScopes())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_scope", "Request rejected", null));
        }
        OAuthConsentBO consent = consentRepository.findConsentForUpdate(
                        realm, value.getClientPk(), value.getPrincipalName())
                .filter(candidate -> "ACTIVE".equals(candidate.getStatus()))
                .orElseThrow(OAuthTokenIssueServiceImpl::invalidGrant);
        Set<String> consented = consentedScopes(consent);
        if (!consented.containsAll(value.getAuthorizedScopes())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_scope", "Request rejected", null));
        }

        Set<URI> audiences = value.getAudiences().stream().map(URI::create)
                .collect(java.util.stream.Collectors.toSet());
        for (URI audience : audiences) {
            if (!client.getResourceUris().contains(audience.toASCIIString())) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_target", "Request rejected", null));
            }
            ResourceServer target = resourceServerStore.findByUri(audience)
                    .filter(ResourceServer::active)
                    .orElseThrow(() -> new OAuth2AuthenticationException(
                            new OAuth2Error("invalid_target", "Request rejected", null)));
            OAuthGrantBO grant = grantRepository.findGrantForUpdate(realm, value.getClientPk(), target.resourceServerId(),
                            ResourceGrantType.USER_DELEGATION)
                    .filter(candidate -> candidate.getStatus()
                            == top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant.Status.ACTIVE)
                    .orElseThrow(() -> new OAuth2AuthenticationException(
                            new OAuth2Error("invalid_target", "Request rejected", null)));
            requireCurrentUserGrant(grant, target, value.getAuthorizedScopes());
        }
        return new UserAuthorizationIssueContext(value.getId(), snapshot, value.getPrincipalName(), audiences);
    }

    @Override
    public void recordClientCredentialsIssued(TrustedRealmBO trustedRealm, Long clientPk, URI resource) {
        Objects.requireNonNull(trustedRealm, "trustedRealm").validateTrustedPartition();
        Objects.requireNonNull(clientRepository.findById(
                trustedRealm, Objects.requireNonNull(clientPk, "clientPk")).orElseThrow(
                        OAuthTokenIssueServiceImpl::invalidGrant));
        recordIssued(trustedRealm, clientPk, null,
                Objects.requireNonNull(resource, "resource").normalize().toASCIIString(),
                "CLIENT_CREDENTIALS_TOKEN_ISSUED");
    }

    @Override
    public void recordAuthorizationCodeCreated(
            TrustedRealmBO trustedRealm,
            OAuthAuthorizationBO authorization) {
        TrustedRealmBO realm = Objects.requireNonNull(trustedRealm, "trustedRealm");
        OAuthAuthorizationBO value = Objects.requireNonNull(authorization, "authorization");
        if (!"authorization_code".equals(value.getGrantType()) || value.getClientPk() == null
                || value.getPrincipalName() == null || value.getAudiences() == null
                || value.getAudiences().isEmpty()) {
            throw invalidGrant();
        }
        recordIssued(realm, value.getClientPk(), value.getPrincipalName(),
                String.join(",", value.getAudiences()), "AUTHORIZATION_CODE_CREATED");
    }

    private boolean matchesAuthorizationCode(
            OAuthTokenBO token,
            OAuthAuthorizationBO authorization,
            ConsumeOAuthTokenCommand command) {
        if (token.getMetadata().isCodeConsumed() || command.getRedirectUri() == null
                || command.getVerifier() == null || authorization.getAttributes() == null
                || !Objects.equals(command.getRedirectUri(), authorization.getAttributes().getRedirectUri())
                || !"S256".equals(authorization.getAttributes().getCodeChallengeMethod())) {
            return false;
        }
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                sha256(command.getVerifier().getBytes(StandardCharsets.US_ASCII)));
        return MessageDigest.isEqual(challenge.getBytes(StandardCharsets.US_ASCII),
                authorization.getAttributes().getCodeChallenge().getBytes(StandardCharsets.US_ASCII));
    }

    private void lockUserGrants(TrustedRealmBO realm, OAuthClientBO client, List<String> audiences, List<String> scopes) {
        if (audiences == null || audiences.isEmpty()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_target", "Request rejected", null));
        }
        for (String audience : audiences) {
            URI uri = URI.create(audience);
            ResourceServer target = resourceServerStore.findByUri(uri)
                    .filter(ResourceServer::active)
                    .orElseThrow(() -> new OAuth2AuthenticationException(
                            new OAuth2Error("invalid_target", "Request rejected", null)));
            OAuthGrantBO grant = grantRepository.findGrantForUpdate(realm, client.getId(), target.resourceServerId(),
                            ResourceGrantType.USER_DELEGATION)
                    .filter(candidate -> candidate.getStatus()
                            == top.egon.cola.platform.tianquan.shoubing.core.resource.ClientResourceGrant.Status.ACTIVE)
                    .orElseThrow(() -> new OAuth2AuthenticationException(
                            new OAuth2Error("invalid_target", "Request rejected", null)));
            requireCurrentUserGrant(grant, target, scopes);
        }
    }

    private static void requireCurrentUserGrant(OAuthGrantBO grant, ResourceServer target, List<String> scopes) {
        if (!Objects.equals(grant.getResourceVersion(), target.version())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_target", "Request rejected", null));
        }
        Set<String> businessScopes = scopes.stream().filter(scope -> !IDENTITY_SCOPES.contains(scope))
                .collect(java.util.stream.Collectors.toSet());
        if (grant.getAllowedScopes() == null || !grant.getAllowedScopes().containsAll(businessScopes)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_scope", "Request rejected", null));
        }
    }

    private static Set<String> consentedScopes(OAuthConsentBO consent) {
        return consent.getAuthorities().stream()
                .filter(authority -> authority.startsWith("SCOPE_"))
                .map(authority -> authority.substring("SCOPE_".length()))
                .collect(java.util.stream.Collectors.toSet());
    }

    private void recordIssued(
            TrustedRealmBO realm,
            Long clientPk,
            String subject,
            String resource,
            String eventType) {
        String eventId = SnowflakeIdGenerator.nextId();
        String clientRef = Digests.sha256Hex(Long.toString(clientPk));
        String subjectRef = subject == null ? null : Digests.sha256Hex(subject);
        var occurredAt = clock.instant();
        auditRepository.save(realm, OAuthAuditBO.builder()
                .eventId(eventId)
                .eventType(eventType)
                .clientRef(clientRef)
                .subjectRef(subjectRef)
                .resourceRef(resource)
                .result("SUCCESS")
                .reasonCode(eventType)
                .occurredAt(occurredAt)
                .auditKind("OAUTH")
                .publicId(eventId)
                .build(), 0L);
        Map<String, Object> auditEvent = new LinkedHashMap<>();
        auditEvent.put("eventId", eventId);
        auditEvent.put("schemaVersion", 1);
        auditEvent.put("issuer", realm.getIssuer().toASCIIString());
        auditEvent.put("resource", resource);
        if (subjectRef != null) {
            auditEvent.put("subjectRef", subjectRef);
        }
        auditEvent.put("clientRef", clientRef);
        auditEvent.put("result", "SUCCESS");
        auditEvent.put("reasonCode", eventType);
        auditEvent.put("occurredAt", occurredAt);
        outbox.enqueue(OutboxMessage.builder()
                .messageId(eventId)
                .idempotencyKey("oauth-audit:" + eventId)
                .channel(AUDIT_OUTBOX_CHANNEL)
                .destination(AUDIT_OUTBOX_DESTINATION)
                .payload(auditEvent)
                .schemaVersion("1")
                .build());
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String versionedCredentialId(OAuthClientBO client) {
        Long clientPk = client.getId();
        Long credentialVersion = client.getCredentialVersion();
        if (clientPk == null || clientPk <= 0L
                || credentialVersion == null || credentialVersion < 0L) {
            throw invalidGrant();
        }
        return clientPk + ":" + credentialVersion;
    }

    private static OAuth2AuthenticationException invalidGrant() {
        return new OAuth2AuthenticationException(new OAuth2Error(INVALID_GRANT, "Request rejected", null));
    }
}
