package top.egon.cola.platform.tianquan.shoubing.admin.oauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import top.egon.cola.component.outbox.store.OutboxStore;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthAuthorizationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthRetentionServiceImpl;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthRevocationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthTokenBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthTokenTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthRevocationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository;

class OAuthRetentionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    private static final TrustedRealmBO TENANT = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example/tenants/1001"))
            .realmKey("tenant:1001")
            .realmType(RealmTypeEnum.TENANT)
            .storagePartitionId(1001L)
            .businessTenantId(1001L)
            .trustVersion(0L)
            .build();

    private final OAuthAuditRepository auditRepository = mock(OAuthAuditRepository.class);
    private final OAuthRevocationRepository revocationRepository = mock(OAuthRevocationRepository.class);
    private final OAuthTokenRepository tokenRepository = mock(OAuthTokenRepository.class);
    private final OAuthAuthorizationRepository authorizationRepository = mock(OAuthAuthorizationRepository.class);
    private final OAuthRealmRepository realmRepository = mock(OAuthRealmRepository.class);
    private final OAuthManagementRealmResolver managementRealmResolver = mock(OAuthManagementRealmResolver.class);
    private final OutboxStore outboxStore = mock(OutboxStore.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private OAuthRetentionServiceImpl service;

    @BeforeEach
    void setUp() {
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionStatus.isNewTransaction()).thenReturn(true);
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(outboxStore.countBacklog()).thenReturn(0L);
        service = new OAuthRetentionServiceImpl(auditRepository, revocationRepository, tokenRepository,
                authorizationRepository, realmRepository, managementRealmResolver, outboxStore, clock,
                java.time.Duration.ofSeconds(30), transactionManager);
    }

    @Test
    void deletesOnlyAgedAuditAndAppliedExpiredRevocationsWithVersionGuards() {
        Instant auditCutoff = NOW.minusSeconds(180L * 24 * 60 * 60);
        Instant revocationCutoff = NOW.minusSeconds(30);
        OAuthAuditBO oldAudit = OAuthAuditBO.builder()
                .id(10L).version(4L).occurredAt(auditCutoff.minusSeconds(1)).build();
        OAuthRevocationBO applied = OAuthRevocationBO.builder()
                .id(20L).version(5L).targetType(OAuthSecurityTargetEnum.JTI)
                .expiresAt(revocationCutoff.minusSeconds(1)).projectionStatus("APPLIED").build();
        OAuthRevocationBO pending = OAuthRevocationBO.builder()
                .id(21L).version(1L).targetType(OAuthSecurityTargetEnum.JTI)
                .expiresAt(revocationCutoff.minusSeconds(1)).projectionStatus("PENDING").build();
        OAuthRevocationBO permanentKid = OAuthRevocationBO.builder()
                .id(22L).version(2L).targetType(OAuthSecurityTargetEnum.KID)
                .expiresAt(null).projectionStatus("APPLIED").build();
        when(auditRepository.findExpiredBefore(eq(TENANT), eq(auditCutoff), eq(0L), eq(500)))
                .thenReturn(List.of(oldAudit));
        when(auditRepository.deleteVersionedById(TENANT, 10L, 4L)).thenReturn(true);
        when(revocationRepository.findExpiredBefore(eq(TENANT), eq(revocationCutoff), eq(0L), eq(500)))
                .thenReturn(List.of(applied, pending, permanentKid));
        when(revocationRepository.deleteVersionedById(TENANT, 20L, 5L)).thenReturn(true);

        int deleted = service.cleanExpired(TENANT);

        assertThat(deleted).isEqualTo(2);
        verify(auditRepository).deleteVersionedById(TENANT, 10L, 4L);
        verify(revocationRepository).deleteVersionedById(TENANT, 20L, 5L);
        verify(revocationRepository, never()).deleteVersionedById(TENANT, 21L, 1L);
        verify(revocationRepository, never()).deleteVersionedById(TENANT, 22L, 2L);
        verify(outboxStore).countBacklog();
    }

    @Test
    void activeOutboxBacklogDefersRevocationRetention() {
        when(outboxStore.countBacklog()).thenReturn(1L);
        when(auditRepository.findExpiredBefore(eq(TENANT), any(), eq(0L), eq(500))).thenReturn(List.of());
        when(authorizationRepository.findExpiredBefore(eq(TENANT), any(), eq(0L), eq(500)))
                .thenReturn(List.of());

        int deleted = service.cleanExpired(TENANT);

        assertThat(deleted).isZero();
        verify(revocationRepository, never()).findExpiredBefore(eq(TENANT), any(), anyLong(), anyInt());
        verify(revocationRepository, never()).deleteVersionedById(eq(TENANT), any(), anyLong());
    }

    @Test
    void removesFamilyTokensBeforeDeletingExpiredAuthorization() {
        Instant familyCutoff = NOW.minusSeconds(24L * 60 * 60);
        OAuthAuthorizationBO expiredAuthorization = OAuthAuthorizationBO.builder()
                .id(30L).version(8L).absoluteExpiresAt(familyCutoff.minusSeconds(1)).build();
        OAuthTokenBO refreshToken = OAuthTokenBO.builder()
                .id(31L).version(3L).authorizationPk(30L).tokenType(OAuthTokenTypeEnum.REFRESH)
                .expiresAt(expiredAuthorization.getAbsoluteExpiresAt().minusSeconds(1)).build();
        AtomicBoolean refreshTokenPresent = new AtomicBoolean(true);
        when(auditRepository.findExpiredBefore(eq(TENANT), any(), eq(0L), eq(500))).thenReturn(List.of());
        when(revocationRepository.findExpiredBefore(eq(TENANT), any(), eq(0L), eq(500))).thenReturn(List.of());
        when(authorizationRepository.findExpiredBefore(eq(TENANT), eq(familyCutoff), eq(0L), eq(500)))
                .thenReturn(List.of(expiredAuthorization));
        when(authorizationRepository.findByIdForUpdate(TENANT, 30L)).thenReturn(Optional.of(expiredAuthorization));
        when(tokenRepository.findByAuthorization(eq(TENANT), eq(30L), any(), anyLong(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(2) == OAuthTokenTypeEnum.REFRESH
                        && refreshTokenPresent.get() ? List.of(refreshToken) : List.of());
        when(tokenRepository.deleteVersionedById(TENANT, 31L, 3L)).thenAnswer(invocation -> {
            refreshTokenPresent.set(false);
            return true;
        });
        when(authorizationRepository.deleteVersionedById(TENANT, 30L, 8L)).thenReturn(true);

        int deleted = service.cleanExpired(TENANT);

        assertThat(deleted).isEqualTo(2);
        InOrder order = inOrder(tokenRepository, authorizationRepository);
        order.verify(tokenRepository).deleteVersionedById(TENANT, 31L, 3L);
        order.verify(authorizationRepository).deleteVersionedById(TENANT, 30L, 8L);
    }

    @Test
    void restartContinuesFromRemainingActiveCandidatesAfterCommittedBatch() {
        Instant cutoff = NOW.minusSeconds(180L * 24 * 60 * 60);
        OAuthAuditBO first = OAuthAuditBO.builder().id(40L).version(1L)
                .occurredAt(cutoff.minusSeconds(2)).build();
        OAuthAuditBO second = OAuthAuditBO.builder().id(41L).version(2L)
                .occurredAt(cutoff.minusSeconds(1)).build();
        when(auditRepository.findExpiredBefore(eq(TENANT), eq(cutoff), eq(0L), eq(500)))
                .thenReturn(List.of(first)).thenReturn(List.of(second));
        when(auditRepository.deleteVersionedById(TENANT, 40L, 1L)).thenReturn(true);
        when(auditRepository.deleteVersionedById(TENANT, 41L, 2L)).thenReturn(true);
        when(revocationRepository.findExpiredBefore(eq(TENANT), any(), eq(0L), eq(500)))
                .thenReturn(List.of());
        when(authorizationRepository.findExpiredBefore(eq(TENANT), any(), eq(0L), eq(500)))
                .thenReturn(List.of());

        assertThat(service.cleanExpired(TENANT)).isEqualTo(1);
        assertThat(service.cleanExpired(TENANT)).isEqualTo(1);

        verify(auditRepository).deleteVersionedById(TENANT, 40L, 1L);
        verify(auditRepository).deleteVersionedById(TENANT, 41L, 2L);
        verify(transactionManager, org.mockito.Mockito.times(2)).commit(any());
    }

    @Test
    void hourlyJobEnumeratesOnlyRegistryBackedRealms() {
        TrustedRealmBO platformRealm = TrustedRealmBO.builder()
                .issuer(URI.create("https://idp.example/platform"))
                .realmKey("platform")
                .realmType(RealmTypeEnum.PLATFORM)
                .storagePartitionId(9000L)
                .trustVersion(0L)
                .build();
        OAuthRealmBO registeredTenant = OAuthRealmBO.builder()
                .issuer(TENANT.getIssuer()).realmKey(TENANT.getRealmKey()).realmType(RealmTypeEnum.TENANT)
                .businessTenantId(1001L).storagePartitionId(1001L).trustVersion(0L).status("CLOSED").build();
        when(managementRealmResolver.platformRealm()).thenReturn(platformRealm);
        when(realmRepository.findAllByStoragePartition(platformRealm)).thenReturn(List.of(registeredTenant));
        when(auditRepository.findExpiredBefore(eq(TENANT), any(), eq(0L), eq(500))).thenReturn(List.of());
        when(revocationRepository.findExpiredBefore(eq(TENANT), any(), eq(0L), eq(500))).thenReturn(List.of());
        when(authorizationRepository.findExpiredBefore(eq(TENANT), any(), eq(0L), eq(500)))
                .thenReturn(List.of());

        service.cleanExpiredAllRealms();

        verify(realmRepository).findAllByStoragePartition(platformRealm);
        verify(auditRepository).findExpiredBefore(eq(TENANT), any(), eq(0L), eq(500));
    }
}
