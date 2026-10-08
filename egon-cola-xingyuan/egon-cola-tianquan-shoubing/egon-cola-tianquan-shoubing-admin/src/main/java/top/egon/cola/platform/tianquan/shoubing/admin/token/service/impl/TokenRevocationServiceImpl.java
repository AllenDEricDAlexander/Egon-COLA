package top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthAuthorizationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthRevocationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSecurityStateBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.OAuthClientRevokeCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.OAuthSubjectRevokeCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.command.RevokeOAuthTokenCommand;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthRevocationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSecurityStateRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.TokenRevocationService;
import top.egon.cola.platform.tianquan.shoubing.core.audit.OAuthSecurityChangedEvent;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;

@Service("tokenRevocationService")
@Slf4j
@RequiredArgsConstructor
@Validated
public class TokenRevocationServiceImpl implements TokenRevocationService {

    private static final int TOKEN_PAGE_SIZE = 500;

    @Qualifier("oAuthRealmRepository")
    private final OAuthRealmRepository realmRepository;
    @Qualifier("oAuthClientRepository")
    private final OAuthClientRepository clientRepository;
    @Qualifier("oAuthAuthorizationRepository")
    private final OAuthAuthorizationRepository authorizationRepository;
    @Qualifier("oAuthTokenRepository")
    private final OAuthTokenRepository tokenRepository;
    @Qualifier("oAuthSecurityStateRepository")
    private final OAuthSecurityStateRepository stateRepository;
    @Qualifier("oAuthRevocationRepository")
    private final OAuthRevocationRepository revocationRepository;
    @Qualifier("oAuthAuditRepository")
    private final OAuthAuditRepository auditRepository;
    @Qualifier("transactionalOutbox")
    private final TransactionalOutbox outbox;
    @Qualifier("oauthSecurityProjectionComponent")
    private final OAuthSecurityProjectionComponent projectionComponent;
    @Qualifier("oauthManagementRealmResolver")
    private final OAuthManagementRealmResolver managementRealmResolver;
    @Qualifier("egonColaMybatisPlusClock")
    private final Clock clock;
    @Value("${egon.tianquan-shoubing.clock-skew:30s}")
    private final Duration clockSkew;
    @Qualifier("transactionManager")
    private final PlatformTransactionManager transactionManager;

    @PostConstruct
    void validateClockSkew() {
        if (clockSkew.isNegative() || clockSkew.compareTo(Duration.ofSeconds(60)) > 0) {
            throw new IllegalStateException("OAUTH_CLOCK_SKEW_INVALID");
        }
    }

    @Override
    @Transactional(transactionManager = "transactionManager")
    public void revoke(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid RevokeOAuthTokenCommand command) {
        TrustedRealmBO realm = requireRealm(trustedRealm);
        RevokeOAuthTokenCommand input = Objects.requireNonNull(command, "command");
        realmRepository.findByRealmKeyForUpdate(realm)
                .orElseThrow(() -> unavailable("OAUTH_REVOCATION_REALM_MISSING"));
        OAuthSecurityStateBO realmState = requiredState(realm, OAuthSecurityTargetEnum.REALM, "self");
        OAuthTokenBO tokenCandidate = tokenRepository.findByTokenDigest(realm, input.getTokenDigest())
                .orElse(null);
        if (tokenCandidate == null) {
            return;
        }
        OAuthAuthorizationBO authorizationCandidate = authorizationRepository.findById(
                        realm, tokenCandidate.getAuthorizationPk())
                .orElseThrow(() -> unavailable("OAUTH_REVOCATION_AUTHORIZATION_MISSING"));
        if (!Objects.equals(authorizationCandidate.getClientPk(), input.getClientPk())) {
            throw invalidClient();
        }
        OAuthClientBO clientCandidate = clientRepository.findById(realm, authorizationCandidate.getClientPk())
                .orElseThrow(() -> unavailable("OAUTH_REVOCATION_CLIENT_MISSING"));
        OAuthClientBO client = clientRepository.findByClientIdForUpdate(realm, clientCandidate.getClientId())
                .filter(value -> Objects.equals(value.getId(), input.getClientPk()))
                .orElseThrow(TokenRevocationServiceImpl::invalidClient);
        OAuthSecurityStateBO clientState = requiredState(
                realm, OAuthSecurityTargetEnum.CLIENT, Long.toString(client.getId()));
        OAuthSecurityStateBO subjectState = "USER".equals(authorizationCandidate.getPrincipalType())
                ? requiredState(realm, OAuthSecurityTargetEnum.SUBJECT, authorizationCandidate.getPrincipalName())
                : null;
        OAuthAuthorizationBO authorization = authorizationRepository.findByIdForUpdate(
                        realm, tokenCandidate.getAuthorizationPk())
                .orElseThrow(() -> unavailable("OAUTH_REVOCATION_AUTHORIZATION_MISSING"));
        OAuthTokenBO token = tokenRepository.findByTokenDigestForUpdate(realm, input.getTokenDigest())
                .orElse(null);
        if (token == null) {
            return;
        }
        if (!Objects.equals(authorization.getClientPk(), input.getClientPk())
                || !Objects.equals(token.getAuthorizationPk(), authorization.getId())) {
            throw invalidClient();
        }

        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        boolean family = token.getTokenType() == OAuthTokenTypeEnum.REFRESH
                || token.getTokenType() == OAuthTokenTypeEnum.CODE;
        OAuthSecurityTargetEnum targetType = family || token.getJti() == null
                ? OAuthSecurityTargetEnum.AUTHORIZATION : OAuthSecurityTargetEnum.JTI;
        String targetKey = targetType == OAuthSecurityTargetEnum.JTI
                ? token.getJti() : Long.toString(authorization.getId());
        Instant expiry = targetType == OAuthSecurityTargetEnum.JTI
                ? token.getExpiresAt() : authorization.getAbsoluteExpiresAt();
        Instant expiresAt = safeExpiry(expiry, now);

        if (family) {
            revokeAuthorizationFamily(realm, authorization, now);
        } else if (token.getRevokedAt() == null) {
            tokenRepository.save(realm, revoked(token, now), token.getVersion());
        }

        OAuthRevocationBO current = revocationRepository.findByTargetForUpdate(realm, targetType, targetKey)
                .orElse(null);
        OAuthSecurityChangedEvent event;
        boolean enqueue = current == null || "PENDING".equals(current.getProjectionStatus());
        if (current == null) {
            long revision = nextRevision(realmState, clientState, subjectState);
            saveRealmRevision(realm, realmState, revision);
            OAuthRevocationBO created = OAuthRevocationBO.builder()
                    .version(0L)
                    .targetType(targetType)
                    .targetKey(targetKey)
                    .reasonCode(input.getReasonCode())
                    .revokedAt(now)
                    .expiresAt(expiresAt)
                    .revision(revision)
                    .projectionStatus("PENDING")
                    .build();
            current = revocationRepository.save(realm, created, 0L);
            event = event(realm, current.getRevision(), targetType, targetKey, null,
                    current.getExpiresAt(), current.getReasonCode(), current.getRevokedAt());
            saveAuditIfAbsent(realm, event, "TOKEN_REVOKED");
        } else {
            if (current.getExpiresAt() != null && expiresAt.isAfter(current.getExpiresAt())) {
                throw unavailable("OAUTH_REVOCATION_EXPIRY_CONFLICT");
            }
            event = event(realm, current.getRevision(), targetType, targetKey, null,
                    current.getExpiresAt(), current.getReasonCode(), current.getRevokedAt());
        }
        if (enqueue) {
            enqueue(event);
        }
        registerProjectionAfterCommit(List.of(new ProjectionChange(realm, event, targetType)));
    }

    @Override
    @Transactional(transactionManager = "transactionManager")
    public void revokeSubject(@NotNull @Valid OAuthSubjectRevokeCommand command) {
        OAuthSubjectRevokeCommand input = Objects.requireNonNull(command, "command");
        TrustedRealmBO platformRealm = managementRealmResolver.platformRealm();
        List<OAuthRealmBO> registered = realmRepository.findAllByStoragePartition(platformRealm);
        if (registered.isEmpty()) {
            throw unavailable("OAUTH_REVOCATION_REALM_DIRECTORY_EMPTY");
        }
        List<ProjectionChange> projections = new ArrayList<>(registered.size());
        for (OAuthRealmBO realmRow : registered) {
            TrustedRealmBO realm = trustedRealm(realmRow);
            realmRepository.findByRealmKeyForUpdate(realm)
                    .orElseThrow(() -> unavailable("OAUTH_REVOCATION_REALM_MISSING"));
            OAuthSecurityStateBO realmState = requiredState(realm, OAuthSecurityTargetEnum.REALM, "self");
            OAuthSecurityStateBO subjectState = stateRepository.findByTargetForUpdate(
                    realm, OAuthSecurityTargetEnum.SUBJECT, input.getSubject()).orElse(null);
            if (subjectState == null && hasAuthorizationHistory(realm, input.getSubject())) {
                throw unavailable("OAUTH_SECURITY_SUBJECT_STATE_MISSING_WITH_AUTHORIZATION");
            }
            Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
            long revision = nextRevision(realmState, null, subjectState);
            long generation = subjectState == null ? 0L : Math.addExact(subjectState.getGeneration(), 1L);
            boolean enabled = switch (input.getReasonCode()) {
                case "USER_DISABLED" -> false;
                case "USER_ENABLED", "USER_CREATED" -> true;
                default -> subjectState == null || subjectState.isEnabled();
            };
            saveRealmRevision(realm, realmState, revision);
            OAuthSecurityStateBO changed = securityState(
                    subjectState, OAuthSecurityTargetEnum.SUBJECT, input.getSubject(),
                    generation, enabled, revision, input.getReasonCode());
            OAuthSecurityStateBO saved = stateRepository.save(
                    realm, changed, subjectState == null ? 0L : subjectState.getVersion());
            OAuthSecurityChangedEvent event = event(realm, revision, OAuthSecurityTargetEnum.SUBJECT,
                    input.getSubject(), saved.getGeneration(), null, input.getReasonCode(), now);
            saveAuditIfAbsent(realm, event, "SUBJECT_REVOKED");
            enqueue(event);
            projections.add(new ProjectionChange(realm, event, null));
        }
        registerProjectionAfterCommit(projections);
    }

    @Override
    @Transactional(transactionManager = "transactionManager")
    public void revokeClient(
            @NotNull @Valid TrustedRealmBO trustedRealm,
            @NotNull @Valid OAuthClientRevokeCommand command) {
        TrustedRealmBO realm = requireRealm(trustedRealm);
        OAuthClientRevokeCommand input = Objects.requireNonNull(command, "command");
        realmRepository.findByRealmKeyForUpdate(realm)
                .orElseThrow(() -> unavailable("OAUTH_REVOCATION_REALM_MISSING"));
        OAuthSecurityStateBO realmState = requiredState(realm, OAuthSecurityTargetEnum.REALM, "self");
        OAuthClientBO clientCandidate = clientRepository.findById(realm, input.getClientPk())
                .orElseThrow(() -> unavailable("OAUTH_REVOCATION_CLIENT_MISSING"));
        OAuthClientBO client = clientRepository.findByClientIdForUpdate(realm, clientCandidate.getClientId())
                .filter(value -> Objects.equals(value.getId(), input.getClientPk()))
                .orElseThrow(TokenRevocationServiceImpl::invalidClient);
        OAuthSecurityStateBO clientState = stateRepository.findByTargetForUpdate(
                realm, OAuthSecurityTargetEnum.CLIENT, Long.toString(client.getId())).orElse(null);
        boolean created = clientState == null;
        if (created && !"CLIENT_CREATED".equals(input.getReasonCode())) {
            throw unavailable("OAUTH_SECURITY_CLIENT_STATE_MISSING");
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        long revision = nextRevision(realmState, clientState, null);
        long generation = created ? 0L : Math.addExact(clientState.getGeneration(), 1L);
        boolean enabled = client.getStatus() == OAuthClient.Status.ACTIVE;
        saveRealmRevision(realm, realmState, revision);
        OAuthSecurityStateBO changed = securityState(
                clientState, OAuthSecurityTargetEnum.CLIENT, Long.toString(client.getId()),
                generation, enabled, revision, input.getReasonCode());
        OAuthSecurityStateBO saved = stateRepository.save(
                realm, changed, clientState == null ? 0L : clientState.getVersion());
        OAuthSecurityChangedEvent event = event(realm, revision, OAuthSecurityTargetEnum.CLIENT,
                Long.toString(client.getId()), saved.getGeneration(), null, input.getReasonCode(), now);
        saveAuditIfAbsent(realm, event, "CLIENT_SECURITY_CHANGED");
        enqueue(event);
        registerProjectionAfterCommit(List.of(new ProjectionChange(realm, event, null)));
    }

    private void revokeAuthorizationFamily(TrustedRealmBO realm, OAuthAuthorizationBO authorization, Instant now) {
        if (!"REVOKED".equals(authorization.getStatus())) {
            OAuthAuthorizationBO changed = OAuthAuthorizationBO.builder()
                    .id(authorization.getId())
                    .version(Math.addExact(authorization.getVersion(), 1L))
                    .clientPk(authorization.getClientPk())
                    .principalName(authorization.getPrincipalName())
                    .principalType(authorization.getPrincipalType())
                    .grantType(authorization.getGrantType())
                    .authorizedScopes(authorization.getAuthorizedScopes())
                    .audiences(authorization.getAudiences())
                    .status("REVOKED")
                    .authTime(authorization.getAuthTime())
                    .acr(authorization.getAcr())
                    .attributes(authorization.getAttributes())
                    .absoluteExpiresAt(authorization.getAbsoluteExpiresAt())
                    .revokedAt(now)
                    .build();
            authorizationRepository.save(realm, changed, authorization.getVersion());
        }
        for (OAuthTokenTypeEnum tokenType : OAuthTokenTypeEnum.values()) {
            long afterId = 0L;
            while (true) {
                List<OAuthTokenBO> page = tokenRepository.findByAuthorization(
                        realm, authorization.getId(), tokenType, afterId, TOKEN_PAGE_SIZE);
                if (page.isEmpty()) {
                    break;
                }
                for (OAuthTokenBO candidate : page) {
                    afterId = Objects.requireNonNull(candidate.getId(), "tokenId");
                    OAuthTokenBO current = tokenRepository.findByTokenDigestForUpdate(
                                    realm, candidate.getTokenDigest())
                            .orElse(null);
                    if (current != null && current.getRevokedAt() == null) {
                        tokenRepository.save(realm, revoked(current, now), current.getVersion());
                    }
                }
                if (page.size() < TOKEN_PAGE_SIZE) {
                    break;
                }
            }
        }
    }

    private void registerProjectionAfterCommit(List<ProjectionChange> changes) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("OAUTH_REVOCATION_TRANSACTION_REQUIRED");
        }
        List<ProjectionChange> pending = List.copyOf(changes);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                RuntimeException failure = null;
                for (ProjectionChange change : pending) {
                    try {
                        projectionComponent.applyProjection(change.realm(), change.event());
                        if (change.revocationTargetType() != null) {
                            markRevocationApplied(change);
                        }
                    } catch (RuntimeException projectionFailure) {
                        if (failure == null) {
                            failure = projectionFailure;
                        } else {
                            failure.addSuppressed(projectionFailure);
                        }
                    }
                }
                if (failure != null) {
                    throw unavailable("OAUTH_SECURITY_PROJECTION_UNAVAILABLE", failure);
                }
            }
        });
    }

    private void markRevocationApplied(ProjectionChange change) {
        TransactionTemplate afterCommitTransaction = new TransactionTemplate(transactionManager);
        afterCommitTransaction.setName("oauthRevocationProjectionStatus");
        afterCommitTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        afterCommitTransaction.executeWithoutResult(status -> {
            OAuthSecurityChangedEvent event = change.event();
            OAuthSecurityTargetEnum targetType = change.revocationTargetType();
            OAuthRevocationBO current = revocationRepository.findByTargetForUpdate(
                    change.realm(), targetType, event.targetKey()).orElse(null);
            if (current == null || "APPLIED".equals(current.getProjectionStatus())) {
                return;
            }
            revocationRepository.save(change.realm(), applied(current), current.getVersion());
        });
    }

    private void saveAuditIfAbsent(TrustedRealmBO realm, OAuthSecurityChangedEvent event, String eventType) {
        if (auditRepository.findByEventId(realm, event.eventId()).isPresent()) {
            return;
        }
        OAuthAuditBO audit = OAuthAuditBO.builder()
                .version(0L)
                .eventId(event.eventId())
                .eventType(eventType)
                .result("SUCCESS")
                .reasonCode(event.reasonCode())
                .occurredAt(event.occurredAt())
                .auditKind("OAUTH")
                .publicId(event.eventId())
                .payload(JsonNodeFactory.instance.objectNode())
                .build();
        auditRepository.save(realm, audit, 0L);
    }

    private void enqueue(OAuthSecurityChangedEvent event) {
        String idempotencyKey = "oauth-security:" + event.realmKey() + ":" + event.targetType()
                + ":" + event.targetKey() + ":" + event.revision();
        outbox.enqueue(OutboxMessage.builder()
                .messageId(event.eventId())
                .idempotencyKey(idempotencyKey)
                .channel(OAuthSecurityChangedEvent.OUTBOX_CHANNEL)
                .destination(OAuthSecurityChangedEvent.OUTBOX_DESTINATION)
                .payload(event)
                .schemaVersion("1")
                .build());
    }

    private OAuthSecurityChangedEvent event(
            TrustedRealmBO realm,
            long revision,
            OAuthSecurityTargetEnum targetType,
            String targetKey,
            Long generation,
            Instant expiresAt,
            String reasonCode,
            Instant occurredAt) {
        String idempotencyKey = "oauth-security:" + realm.getRealmKey() + ":" + targetType.getCode()
                + ":" + targetKey + ":" + revision;
        return new OAuthSecurityChangedEvent(
                Digests.sha256Hex(idempotencyKey),
                1,
                realm.getIssuer(),
                realm.getRealmKey(),
                revision,
                targetType.getCode(),
                targetKey,
                generation,
                expiresAt,
                reasonCode,
                occurredAt,
                null);
    }

    private OAuthSecurityStateBO requiredState(
            TrustedRealmBO realm,
            OAuthSecurityTargetEnum targetType,
            String targetKey) {
        return stateRepository.findByTargetForUpdate(realm, targetType, targetKey)
                .orElseThrow(() -> unavailable("OAUTH_SECURITY_STATE_MISSING"));
    }

    private void saveRealmRevision(TrustedRealmBO realm, OAuthSecurityStateBO current, long revision) {
        stateRepository.save(realm, securityState(
                current, OAuthSecurityTargetEnum.REALM, "self", current.getGeneration(),
                current.isEnabled(), revision, current.getReasonCode()), current.getVersion());
    }

    private OAuthSecurityStateBO securityState(
            OAuthSecurityStateBO current,
            OAuthSecurityTargetEnum targetType,
            String targetKey,
            long generation,
            boolean enabled,
            long revision,
            String reasonCode) {
        return OAuthSecurityStateBO.builder()
                .id(current == null ? null : current.getId())
                .version(current == null ? 0L : Math.addExact(current.getVersion(), 1L))
                .targetType(targetType)
                .targetKey(targetKey)
                .generation(generation)
                .enabled(enabled)
                .revision(revision)
                .reasonCode(reasonCode)
                .build();
    }

    private OAuthRevocationBO applied(OAuthRevocationBO current) {
        return OAuthRevocationBO.builder()
                .id(current.getId())
                .version(Math.addExact(current.getVersion(), 1L))
                .targetType(current.getTargetType())
                .targetKey(current.getTargetKey())
                .reasonCode(current.getReasonCode())
                .revokedAt(current.getRevokedAt())
                .expiresAt(current.getExpiresAt())
                .revision(current.getRevision())
                .projectionStatus("APPLIED")
                .build();
    }

    private OAuthTokenBO revoked(OAuthTokenBO current, Instant now) {
        return OAuthTokenBO.builder()
                .id(current.getId())
                .version(Math.addExact(current.getVersion(), 1L))
                .authorizationPk(current.getAuthorizationPk())
                .tokenType(current.getTokenType())
                .tokenDigest(current.getTokenDigest())
                .tokenValue(current.getTokenValue())
                .jti(current.getJti())
                .issuedAt(current.getIssuedAt())
                .expiresAt(current.getExpiresAt())
                .consumedAt(current.getConsumedAt())
                .revokedAt(now)
                .metadata(current.getMetadata())
                .build();
    }

    private long nextRevision(
            OAuthSecurityStateBO realmState,
            OAuthSecurityStateBO clientState,
            OAuthSecurityStateBO subjectState) {
        long current = realmState.getRevision();
        if (clientState != null) {
            current = Math.max(current, clientState.getRevision());
        }
        if (subjectState != null) {
            current = Math.max(current, subjectState.getRevision());
        }
        return Math.addExact(current, 1L);
    }

    private boolean hasAuthorizationHistory(TrustedRealmBO realm, String subject) {
        return !authorizationRepository.findByPrincipalName(realm, subject, "ACTIVE", 0L, 1).isEmpty()
                || !authorizationRepository.findByPrincipalName(realm, subject, "REVOKED", 0L, 1).isEmpty()
                || !authorizationRepository.findByPrincipalName(realm, subject, "EXPIRED", 0L, 1).isEmpty();
    }

    private Instant safeExpiry(Instant expiry, Instant now) {
        Instant value = Objects.requireNonNull(expiry, "tokenExpiry").plus(clockSkew);
        return value.isAfter(now) ? value : now.plusSeconds(1);
    }

    private static TrustedRealmBO requireRealm(TrustedRealmBO trustedRealm) {
        TrustedRealmBO realm = Objects.requireNonNull(trustedRealm, "trustedRealm");
        realm.validateTrustedPartition();
        return realm;
    }

    private static TrustedRealmBO trustedRealm(OAuthRealmBO realm) {
        OAuthRealmBO registered = Objects.requireNonNull(realm, "registeredRealm");
        TrustedRealmBO trusted = TrustedRealmBO.builder()
                .issuer(registered.getIssuer())
                .realmKey(registered.getRealmKey())
                .realmType(registered.getRealmType())
                .storagePartitionId(registered.getStoragePartitionId())
                .businessTenantId(registered.getBusinessTenantId())
                .trustVersion(registered.getTrustVersion())
                .build();
        trusted.validateTrustedPartition();
        return trusted;
    }

    private static OAuth2AuthenticationException invalidClient() {
        return new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Request rejected", null));
    }

    private static OAuth2AuthenticationException unavailable(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error("temporarily_unavailable", code, null));
    }

    private static OAuth2AuthenticationException unavailable(String code, Throwable cause) {
        return new OAuth2AuthenticationException(new OAuth2Error("temporarily_unavailable", code, null), cause);
    }

    private record ProjectionChange(
            TrustedRealmBO realm,
            OAuthSecurityChangedEvent event,
            OAuthSecurityTargetEnum revocationTargetType) {
    }
}
