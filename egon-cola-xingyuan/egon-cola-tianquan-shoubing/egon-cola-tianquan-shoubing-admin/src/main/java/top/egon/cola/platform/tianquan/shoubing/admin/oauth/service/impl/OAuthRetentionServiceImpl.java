package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.outbox.store.OutboxStore;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthAuthorizationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.OAuthRetentionService;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthRevocationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthRevocationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository;

@Service("oauthRetentionService")
@Slf4j
@RequiredArgsConstructor
@Validated
public class OAuthRetentionServiceImpl implements OAuthRetentionService {

    private static final int BATCH_SIZE = 500;
    private static final Duration AUDIT_RETENTION = Duration.ofDays(180);
    private static final Duration TOKEN_FAMILY_GRACE = Duration.ofHours(24);
    private static final Duration MAX_REALM_RUN = Duration.ofSeconds(30);

    @Qualifier("oAuthAuditRepository")
    private final OAuthAuditRepository auditRepository;
    @Qualifier("oAuthRevocationRepository")
    private final OAuthRevocationRepository revocationRepository;
    @Qualifier("oAuthTokenRepository")
    private final OAuthTokenRepository tokenRepository;
    @Qualifier("oAuthAuthorizationRepository")
    private final OAuthAuthorizationRepository authorizationRepository;
    @Qualifier("oAuthRealmRepository")
    private final OAuthRealmRepository realmRepository;
    @Qualifier("oauthManagementRealmResolver")
    private final OAuthManagementRealmResolver managementRealmResolver;
    @Qualifier("outboxStore")
    private final OutboxStore outboxStore;
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
    public int cleanExpired(@NotNull @Valid TrustedRealmBO trustedRealm) {
        TrustedRealmBO realm = Objects.requireNonNull(trustedRealm, "trustedRealm");
        realm.validateTrustedPartition();
        TransactionTemplate batchTransaction = new TransactionTemplate(transactionManager);
        batchTransaction.setName("oauthRetentionBatch");
        Integer deleted = batchTransaction.execute(status -> cleanExpiredBatch(realm));
        return deleted == null ? 0 : deleted;
    }

    @Scheduled(cron = "0 0 * * * *", zone = "UTC")
    public void cleanExpiredAllRealms() {
        TrustedRealmBO platformRealm = managementRealmResolver.platformRealm();
        List<OAuthRealmBO> registeredRealms = realmRepository.findAllByStoragePartition(platformRealm);
        RuntimeException failure = null;
        for (OAuthRealmBO registeredRealm : registeredRealms) {
            TrustedRealmBO trustedRealm = trustedRealm(registeredRealm);
            try {
                int deleted = cleanExpired(trustedRealm);
                if (deleted > 0) {
                    log.info("OAuth retention batch completed realmKey={} deletedRows={}",
                            trustedRealm.getRealmKey(), deleted);
                }
            } catch (RuntimeException realmFailure) {
                log.error("OAuth retention batch failed realmKey={} failureType={}",
                        trustedRealm.getRealmKey(), realmFailure.getClass().getSimpleName(), realmFailure);
                if (failure == null) {
                    failure = realmFailure;
                } else {
                    failure.addSuppressed(realmFailure);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private int cleanExpiredBatch(TrustedRealmBO realm) {
        long deadline = System.nanoTime() + MAX_REALM_RUN.toNanos();
        Instant now = clock.instant();
        int deleted = purgeAudits(realm, now.minus(AUDIT_RETENTION), deadline);
        if (!deadlineReached(deadline)) {
            deleted += purgeRevocations(realm, now.minus(clockSkew), deadline);
        }
        if (!deadlineReached(deadline)) {
            deleted += purgeAuthorizationFamilies(realm, now.minus(TOKEN_FAMILY_GRACE), deadline);
        }
        return deleted;
    }

    private int purgeAudits(TrustedRealmBO realm, Instant cutoff, long deadline) {
        List<OAuthAuditBO> expired = auditRepository.findExpiredBefore(realm, cutoff, 0L, BATCH_SIZE);
        int deleted = 0;
        for (OAuthAuditBO audit : expired) {
            if (deadlineReached(deadline)) {
                break;
            }
            if (audit.getOccurredAt().isAfter(cutoff)) {
                continue;
            }
            if (auditRepository.deleteVersionedById(realm, audit.getId(), audit.getVersion())) {
                deleted++;
            }
        }
        return deleted;
    }

    private int purgeRevocations(TrustedRealmBO realm, Instant cutoff, long deadline) {
        if (outboxStore.countBacklog() != 0L) {
            return 0;
        }
        List<OAuthRevocationBO> expired = revocationRepository.findExpiredBefore(realm, cutoff, 0L, BATCH_SIZE);
        int deleted = 0;
        for (OAuthRevocationBO revocation : expired) {
            if (deadlineReached(deadline)) {
                break;
            }
            if (!"APPLIED".equals(revocation.getProjectionStatus())
                    || revocation.getTargetType() == OAuthSecurityTargetEnum.KID
                    || revocation.getExpiresAt() == null || revocation.getExpiresAt().isAfter(cutoff)) {
                continue;
            }
            if (revocationRepository.deleteVersionedById(realm, revocation.getId(), revocation.getVersion())) {
                deleted++;
            }
        }
        return deleted;
    }

    private int purgeAuthorizationFamilies(TrustedRealmBO realm, Instant cutoff, long deadline) {
        List<OAuthAuthorizationBO> expired =
                authorizationRepository.findExpiredBefore(realm, cutoff, 0L, BATCH_SIZE);
        int deleted = 0;
        for (OAuthAuthorizationBO candidate : expired) {
            if (deadlineReached(deadline)) {
                break;
            }
            if (candidate.getId() == null) {
                throw new IllegalStateException("OAUTH_RETENTION_AUTHORIZATION_ID_MISSING");
            }
            OAuthAuthorizationBO current = authorizationRepository.findByIdForUpdate(realm, candidate.getId())
                    .orElse(null);
            if (current == null || current.getAbsoluteExpiresAt().isAfter(cutoff)) {
                continue;
            }
            boolean familyComplete = true;
            for (OAuthTokenTypeEnum tokenType : OAuthTokenTypeEnum.values()) {
                long afterId = 0L;
                while (!deadlineReached(deadline)) {
                    List<OAuthTokenBO> tokens = tokenRepository.findByAuthorization(
                            realm, current.getId(), tokenType, afterId, BATCH_SIZE);
                    if (tokens.isEmpty()) {
                        break;
                    }
                    for (OAuthTokenBO token : tokens) {
                        if (deadlineReached(deadline)) {
                            familyComplete = false;
                            break;
                        }
                        if (token.getId() == null || token.getExpiresAt().isAfter(current.getAbsoluteExpiresAt())) {
                            throw new IllegalStateException("OAUTH_RETENTION_TOKEN_FAMILY_INVALID");
                        }
                        afterId = token.getId();
                        if (!tokenRepository.deleteVersionedById(realm, token.getId(), token.getVersion())) {
                            familyComplete = false;
                        } else {
                            deleted++;
                        }
                    }
                    if (tokens.size() < BATCH_SIZE || deadlineReached(deadline)) {
                        if (deadlineReached(deadline)) {
                            familyComplete = false;
                        }
                        break;
                    }
                }
                if (deadlineReached(deadline)) {
                    familyComplete = false;
                    break;
                }
            }
            if (!familyComplete || deadlineReached(deadline)
                    || hasActiveFamilyTokens(realm, current.getId())) {
                continue;
            }
            if (authorizationRepository.deleteVersionedById(realm, current.getId(), current.getVersion())) {
                deleted++;
            }
        }
        return deleted;
    }

    private boolean hasActiveFamilyTokens(TrustedRealmBO realm, Long authorizationId) {
        for (OAuthTokenTypeEnum tokenType : OAuthTokenTypeEnum.values()) {
            if (!tokenRepository.findByAuthorization(realm, authorizationId, tokenType, 0L, 1).isEmpty()) {
                return true;
            }
        }
        return false;
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

    private static boolean deadlineReached(long deadline) {
        return System.nanoTime() >= deadline;
    }
}
