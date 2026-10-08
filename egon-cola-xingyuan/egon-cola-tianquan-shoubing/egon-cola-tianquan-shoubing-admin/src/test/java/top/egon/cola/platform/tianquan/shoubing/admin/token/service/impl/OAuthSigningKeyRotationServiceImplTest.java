package top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.token.config.OAuthKeyRotationProperties;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSigningKeyBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthKeyStatusEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSigningKeyRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.SigningKeyService;

class OAuthSigningKeyRotationServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    private static final TrustedRealmBO PLATFORM = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example/platform"))
            .realmKey("platform")
            .realmType(RealmTypeEnum.PLATFORM)
            .storagePartitionId(9000L)
            .trustVersion(0L)
            .build();
    private static final TrustedRealmBO TENANT = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example/tenants/1001"))
            .realmKey("tenant:1001")
            .realmType(RealmTypeEnum.TENANT)
            .storagePartitionId(1001L)
            .businessTenantId(1001L)
            .trustVersion(0L)
            .build();

    private final OAuthRealmRepository realms = mock(OAuthRealmRepository.class);
    private final OAuthSigningKeyRepository keys = mock(OAuthSigningKeyRepository.class);
    private final SigningKeyService signingKeys = mock(SigningKeyService.class);
    private final OAuthKeyCipherComponent keyCipher = mock(OAuthKeyCipherComponent.class);
    private final OAuthSecurityProjectionComponent projection = mock(OAuthSecurityProjectionComponent.class);
    private final OAuthAuditRepository audits = mock(OAuthAuditRepository.class);
    private final TransactionalOutbox outbox = mock(TransactionalOutbox.class);
    private final OAuthManagementRealmResolver realmResolver = mock(OAuthManagementRealmResolver.class);
    private final OAuthKeyRotationProperties properties = new OAuthKeyRotationProperties();
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private OAuthSigningKeyRotationServiceImpl service;

    @BeforeEach
    void setUp() {
        TransactionStatus status = mock(TransactionStatus.class);
        when(status.isNewTransaction()).thenReturn(true);
        when(transactionManager.getTransaction(any())).thenReturn(status);
        when(realmResolver.platformRealm()).thenReturn(PLATFORM);
        OAuthRealmBO row = realmRow();
        when(realms.findAllByStoragePartition(PLATFORM)).thenReturn(List.of(row));
        when(realms.findByRealmKeyForUpdate(any(TrustedRealmBO.class))).thenReturn(Optional.of(row));
        when(projection.isIssuerReady(any(TrustedRealmBO.class))).thenReturn(true);
        when(audits.save(eq(TENANT), any(OAuthAuditBO.class), eq(0L)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        service = new OAuthSigningKeyRotationServiceImpl(realms, keys, signingKeys, keyCipher,
                projection, audits, outbox, realmResolver, properties, Clock.fixed(NOW, ZoneOffset.UTC),
                transactionManager);
    }

    @Test
    void lateInstanceDefersActivationUntilTheIssuerProjectionIsReady() {
        OAuthSigningKeyBO active = key("old-kid", OAuthKeyStatusEnum.ACTIVE, NOW.minusSeconds(100));
        OAuthSigningKeyBO published = key("new-kid", OAuthKeyStatusEnum.PUBLISHED, NOW.minusSeconds(25 * 60 * 60));
        when(projection.isIssuerReady(TENANT)).thenReturn(false);
        when(keys.findByStatuses(any(TrustedRealmBO.class), anyCollection(), eq(0L), eq(8)))
                .thenReturn(List.of(active, published));

        service.rotateDueRealms();

        verify(signingKeys, never()).activate(any(), anyString(), anyLong());
        verify(signingKeys, never()).retire(any(), anyString(), anyLong());
        verify(transactionManager, never()).getTransaction(any());
    }

    @Test
    void activatesOnlyAnAgedPublishedKeyAndRecordsTheRotationInTheSameTransaction() {
        OAuthSigningKeyBO active = key("old-kid", OAuthKeyStatusEnum.ACTIVE, NOW.minusSeconds(100));
        OAuthSigningKeyBO published = key("new-kid", OAuthKeyStatusEnum.PUBLISHED, NOW.minusSeconds(25 * 60 * 60));
        when(keys.findByStatuses(any(TrustedRealmBO.class), anyCollection(), eq(0L), eq(8)))
                .thenReturn(List.of(active, published));

        service.rotateDueRealms();

        verify(signingKeys).activate(TENANT, "new-kid", published.getVersion());
        verify(signingKeys, never()).retire(any(), anyString(), anyLong());
        verify(audits).save(eq(TENANT), any(OAuthAuditBO.class), eq(0L));
        verify(outbox).enqueue(any(OutboxMessage.class));
        verify(transactionManager).commit(any());
    }

    @Test
    void publishesAnEncryptedRsaKeyAtThePrepublicationBoundary() throws Exception {
        OAuthSigningKeyBO active = key("old-kid", OAuthKeyStatusEnum.ACTIVE,
                NOW.minus(properties.getPeriod()).plus(properties.getPrepublish()).minusSeconds(1));
        when(keys.findByStatuses(any(TrustedRealmBO.class), anyCollection(), eq(0L), eq(8)))
                .thenReturn(List.of(active));
        when(keyCipher.encryptPrivateKey(eq(TENANT), anyString(), anyString()))
                .thenReturn("ENC:v1:kek:abcdefghijklmnop:abcdefghijklmnopqrstuv");

        service.rotateDueRealms();

        var published = org.mockito.ArgumentCaptor.forClass(
                top.egon.cola.platform.tianquan.shoubing.admin.token.domain.dto.PublishSigningKeyDTO.class);
        verify(signingKeys).publish(eq(TENANT), published.capture());
        assertThat(published.getValue().getKid()).matches("[0-9a-f]{32}");
        assertThat(published.getValue().getEncryptedPrivateKey()).startsWith("ENC:v1:");
        assertThat(published.getValue().getPublicJwk()).contains("\"kty\":\"RSA\"")
                .contains("\"alg\":\"RS256\"");
        verify(signingKeys, never()).activate(any(), anyString(), anyLong());
    }

    private static OAuthSigningKeyBO key(String kid, OAuthKeyStatusEnum status, Instant activatedAt) {
        return OAuthSigningKeyBO.builder()
                .id(10L)
                .version(2L)
                .kid(kid)
                .algorithm("RS256")
                .publicJwk(JsonNodeFactory.instance.objectNode())
                .privateKeyCiphertext("ENC:v1:kek:abcdefghijklmnop:abcdefghijklmnopqrstuv")
                .status(status)
                .publishedAt(activatedAt)
                .activatedAt(activatedAt)
                .build();
    }

    private static OAuthRealmBO realmRow() {
        return OAuthRealmBO.builder()
                .id(1001L)
                .version(1L)
                .issuer(TENANT.getIssuer())
                .realmKey(TENANT.getRealmKey())
                .realmType(RealmTypeEnum.TENANT)
                .businessTenantId(TENANT.getBusinessTenantId())
                .storagePartitionId(TENANT.getStoragePartitionId())
                .status("ACTIVE")
                .activeKid("old-kid")
                .trustVersion(0L)
                .build();
    }
}
