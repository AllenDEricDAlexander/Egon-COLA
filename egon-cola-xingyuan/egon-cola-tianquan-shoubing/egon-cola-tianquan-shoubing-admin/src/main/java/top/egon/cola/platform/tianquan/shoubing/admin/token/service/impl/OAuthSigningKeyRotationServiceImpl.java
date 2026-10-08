package top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.egon.cola.component.common.crypto.digest.Digests;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.support.outbox.OAuthAuditOutboxDeliveryHandler;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.token.config.OAuthKeyRotationProperties;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSigningKeyRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.SigningKeyService;

@Service("oauthSigningKeyRotationService")
@Slf4j
@RequiredArgsConstructor
public class OAuthSigningKeyRotationServiceImpl {

    private static final int RSA_KEY_SIZE = 3072;
    private static final int KEY_PAGE_SIZE = 8;
    private static final String CHANNEL = OAuthAuditOutboxDeliveryHandler.CHANNEL;
    private static final String DESTINATION = OAuthAuditOutboxDeliveryHandler.DESTINATION;

    @Qualifier("oAuthRealmRepository")
    private final OAuthRealmRepository realms;
    @Qualifier("oAuthSigningKeyRepository")
    private final OAuthSigningKeyRepository keys;
    @Qualifier("signingKeyServiceImpl")
    private final SigningKeyService signingKeys;
    @Qualifier("oauthKeyCipherComponent")
    private final OAuthKeyCipherComponent keyCipher;
    @Qualifier("oauthSecurityProjectionComponent")
    private final OAuthSecurityProjectionComponent projection;
    @Qualifier("oAuthAuditRepository")
    private final OAuthAuditRepository audits;
    @Qualifier("transactionalOutbox")
    private final TransactionalOutbox outbox;
    @Qualifier("oauthManagementRealmResolver")
    private final OAuthManagementRealmResolver realmResolver;
    @Qualifier("oauthKeyRotationProperties")
    private final OAuthKeyRotationProperties properties;
    @Qualifier("idpClock")
    private final Clock clock;
    @Qualifier("transactionManager")
    private final PlatformTransactionManager transactionManager;

    @PostConstruct
    void validateProperties() {
        properties.validate();
    }

    @Scheduled(cron = "${egon.tianquan-shoubing.oauth.key-rotation.schedule:0 0 2 * * *}", zone = "UTC")
    public void rotateDueRealms() {
        TrustedRealmBO platformRealm = realmResolver.platformRealm();
        List<OAuthRealmBO> registeredRealms = realms.findAllByStoragePartition(platformRealm);
        List<TrustedRealmBO> activeRealms = registeredRealms.stream()
                .filter(realm -> "ACTIVE".equals(realm.getStatus()))
                .map(OAuthSigningKeyRotationServiceImpl::trustedRealm)
                .toList();
        if (activeRealms.stream().anyMatch(realm -> !projection.isIssuerReady(realm))) {
            log.warn("OAuth key rotation deferred because an issuer projection is not ready; issuerCount={}",
                    activeRealms.size());
            return;
        }
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setName("oauthSigningKeyRotation");
        for (TrustedRealmBO realm : activeRealms) {
            try {
                transaction.executeWithoutResult(status -> rotateRealm(realm));
            } catch (RuntimeException failure) {
                log.error("OAuth key rotation failed; issuerHash={} failureType={}",
                        Digests.sha256Hex(realm.getIssuer().toASCIIString()),
                        failure.getClass().getSimpleName());
            }
        }
    }

    private void rotateRealm(TrustedRealmBO realm) {
        OAuthRealmBO currentRealm = realms.findByRealmKeyForUpdate(realm).orElse(null);
        if (currentRealm == null || !"ACTIVE".equals(currentRealm.getStatus())
                || currentRealm.getActiveKid() == null || currentRealm.getActiveKid().isBlank()) {
            return;
        }
        List<OAuthSigningKeyBO> candidates = keys.findByStatuses(realm,
                EnumSet.of(OAuthKeyStatusEnum.ACTIVE, OAuthKeyStatusEnum.PUBLISHED), 0L, KEY_PAGE_SIZE);
        OAuthSigningKeyBO active = candidates.stream()
                .filter(key -> key.getStatus() == OAuthKeyStatusEnum.ACTIVE)
                .filter(key -> currentRealm.getActiveKid().equals(key.getKid()))
                .findFirst()
                .orElse(null);
        if (active == null) {
            log.warn("OAuth key rotation deferred because the active key is unavailable; issuerHash={}",
                    Digests.sha256Hex(realm.getIssuer().toASCIIString()));
            return;
        }
        OAuthSigningKeyBO published = candidates.stream()
                .filter(key -> key.getStatus() == OAuthKeyStatusEnum.PUBLISHED)
                .min(java.util.Comparator.comparing(OAuthSigningKeyBO::getPublishedAt))
                .orElse(null);
        Instant now = clock.instant();
        if (published != null) {
            Instant activationTime = published.getPublishedAt().plus(properties.getPrepublish());
            if (!activationTime.isAfter(now)) {
                signingKeys.activate(realm, published.getKid(), published.getVersion());
                recordRotation(realm, "SIGNING_KEY_ACTIVATED", now);
            }
            return;
        }
        Instant activeSince = active.getActivatedAt() == null ? active.getPublishedAt() : active.getActivatedAt();
        Instant publishAfter = activeSince.plus(properties.getPeriod()).minus(properties.getPrepublish());
        if (!publishAfter.isAfter(now)) {
            signingKeys.publish(realm, generateKey(realm));
            recordRotation(realm, "SIGNING_KEY_PUBLISHED", now);
        }
    }

    private top.egon.cola.platform.tianquan.shoubing.admin.token.domain.dto.PublishSigningKeyDTO generateKey(
            TrustedRealmBO realm) {
        String kid = nextId();
        byte[] privateEncoded = null;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(RSA_KEY_SIZE);
            KeyPair pair = generator.generateKeyPair();
            privateEncoded = pair.getPrivate().getEncoded();
            RSAKey publicKey = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .keyID(kid)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyUse(KeyUse.SIGNATURE)
                    .build();
            String privatePem = "-----BEGIN PRIVATE KEY-----\n"
                    + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(privateEncoded)
                    + "\n-----END PRIVATE KEY-----";
            String encrypted = keyCipher.encryptPrivateKey(realm, kid, privatePem);
            return top.egon.cola.platform.tianquan.shoubing.admin.token.domain.dto.PublishSigningKeyDTO.builder()
                    .kid(kid)
                    .publicJwk(publicKey.toPublicJWK().toJSONString())
                    .encryptedPrivateKey(encrypted)
                    .build();
        } catch (java.security.GeneralSecurityException failure) {
            throw new IllegalStateException("OAUTH_SIGNING_KEY_GENERATION_FAILED", failure);
        } finally {
            if (privateEncoded != null) {
                Arrays.fill(privateEncoded, (byte) 0);
            }
        }
    }

    private void recordRotation(TrustedRealmBO realm, String eventType, Instant occurredAt) {
        String eventId = nextId();
        OAuthAuditBO audit = OAuthAuditBO.builder()
                .version(0L)
                .eventId(eventId)
                .eventType(eventType)
                .result("SUCCESS")
                .reasonCode("AUTOMATED_KEY_ROTATION")
                .resourceRef(realm.getIssuer().toASCIIString())
                .occurredAt(occurredAt)
                .payload(JsonNodeFactory.instance.objectNode())
                .auditKind("OAUTH")
                .publicId(eventId)
                .build();
        audits.save(realm, audit, 0L);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", eventId);
        event.put("schemaVersion", "1");
        event.put("issuer", realm.getIssuer().toASCIIString());
        event.put("auditKind", "OAUTH");
        event.put("eventType", eventType);
        event.put("resource", realm.getIssuer().toASCIIString());
        event.put("result", "SUCCESS");
        event.put("reasonCode", "AUTOMATED_KEY_ROTATION");
        event.put("occurredAt", occurredAt.toString());
        outbox.enqueue(OutboxMessage.builder()
                .messageId(eventId)
                .idempotencyKey("oauth-audit:" + eventId)
                .channel(CHANNEL)
                .destination(DESTINATION)
                .payload(event)
                .schemaVersion("1")
                .build());
    }

    private static TrustedRealmBO trustedRealm(OAuthRealmBO realm) {
        return TrustedRealmBO.builder()
                .issuer(realm.getIssuer())
                .realmKey(realm.getRealmKey())
                .realmType(realm.getRealmType())
                .storagePartitionId(realm.getStoragePartitionId())
                .businessTenantId(realm.getBusinessTenantId())
                .trustVersion(realm.getTrustVersion())
                .build();
    }

    private String nextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
