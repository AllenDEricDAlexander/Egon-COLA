package top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RKeys;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthSecuritySnapshotBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthAuthorizationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthAuthorizationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthClientRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.token.config.OAuthRevocationProperties;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthSecurityStateBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.bo.OAuthRevocationBO;
import top.egon.cola.platform.tianquan.shoubing.admin.token.domain.enums.OAuthSecurityTargetEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthRevocationRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthSecurityStateRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.repository.OAuthTokenRepository;
import top.egon.cola.platform.tianquan.shoubing.core.audit.OAuthSecurityChangedEvent;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;

class OAuthSecurityProjectionComponentTest {

    private static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    private static final TrustedRealmBO REALM = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example/tenants/1001"))
            .realmKey("tenant:1001")
            .realmType(RealmTypeEnum.TENANT)
            .storagePartitionId(1001L)
            .businessTenantId(1001L)
            .trustVersion(0L)
            .build();

    private final RedissonClient redisson = mock(RedissonClient.class);
    private final RScript script = mock(RScript.class);
    private final OAuthSecurityStateRepository stateRepository = mock(OAuthSecurityStateRepository.class);
    private final OAuthRevocationRepository revocationRepository = mock(OAuthRevocationRepository.class);
    private final OAuthTokenRepository tokenRepository = mock(OAuthTokenRepository.class);
    private final OAuthAuthorizationRepository authorizationRepository = mock(OAuthAuthorizationRepository.class);
    private final OAuthClientRepository clientRepository = mock(OAuthClientRepository.class);
    private final OAuthRealmRepository realmRepository = mock(OAuthRealmRepository.class);
    private final OAuthManagementRealmResolver managementRealmResolver = mock(OAuthManagementRealmResolver.class);
    private final OAuthRevocationProperties properties = OAuthRevocationProperties.builder()
            .primaryEndpoint(URI.create("rediss://redis.example:6379"))
            .epoch("recovery-epoch-7")
            .build();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private OAuthSecurityProjectionComponent component;

    @BeforeEach
    void setUp() {
        when(redisson.getScript(StringCodec.INSTANCE)).thenReturn(script);
        component = new OAuthSecurityProjectionComponent(
                redisson, stateRepository, revocationRepository, tokenRepository, authorizationRepository,
                clientRepository,
                realmRepository, managementRealmResolver, properties, clock);
    }

    @Test
    void appliesLatestDatabaseStateUnderItsIssuerHashSlot() {
        OAuthSecurityStateBO currentState = OAuthSecurityStateBO.builder()
                .id(11L).version(3L).targetType(OAuthSecurityTargetEnum.REALM).targetKey("self")
                .generation(7L).enabled(false).revision(8L).reasonCode("ADMIN_REVOKE").build();
        when(stateRepository.findByTarget(REALM, OAuthSecurityTargetEnum.REALM, "self"))
                .thenReturn(Optional.of(currentState));
        when(script.eval(any(RScript.Mode.class), anyString(), any(RScript.ReturnType.class),
                anyList(), any(Object[].class))).thenReturn(2L);

        component.applyProjection(REALM, event("REALM", "self", 7L, 6L, null));

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object[]> argumentsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(script).eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER),
                keysCaptor.capture(), argumentsCaptor.capture());
        String readyKey = (String) keysCaptor.getValue().get(0);
        String generationKey = (String) keysCaptor.getValue().get(1);
        assertThat(readyKey).isEqualTo("egon:oauth:security:v2:{"
                + top.egon.cola.component.common.crypto.digest.Digests.sha256Hex(
                        REALM.getIssuer().normalize().toASCIIString()) + "}:ready");
        assertThat(generationKey).isEqualTo(readyKey.replace(":ready", ":generation:realm"));
        assertThat(argumentsCaptor.getValue()).contains("recovery-epoch-7", "GENERATION", "8", "7", "0");
    }

    @Test
    void failsClosedWhenRedisProjectionIsNotReady() {
        OAuthSecurityStateBO state = OAuthSecurityStateBO.builder()
                .id(11L).version(3L).targetType(OAuthSecurityTargetEnum.REALM).targetKey("self")
                .generation(7L).enabled(true).revision(8L).reasonCode("ADMIN_REVOKE").build();
        when(stateRepository.findByTarget(REALM, OAuthSecurityTargetEnum.REALM, "self"))
                .thenReturn(Optional.of(state));
        when(script.eval(any(RScript.Mode.class), anyString(), any(RScript.ReturnType.class),
                anyList(), any(Object[].class))).thenReturn(-1L);

        assertThatThrownBy(() -> component.applyProjection(REALM, event("REALM", "self", 8L, 7L, null)))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .extracting(failure -> ((OAuth2AuthenticationException) failure).getError().getErrorCode())
                .isEqualTo("temporarily_unavailable");
    }

    @Test
    @SuppressWarnings("unchecked")
    void initializesFirstSubjectStateOnlyWhenNoPriorAuthorizationExists() {
        OAuthSecurityStateBO realmState = OAuthSecurityStateBO.builder()
                .id(1L).version(0L).targetType(OAuthSecurityTargetEnum.REALM).targetKey("self")
                .generation(1L).enabled(true).revision(7L).reasonCode("INITIALIZED").build();
        OAuthSecurityStateBO clientState = OAuthSecurityStateBO.builder()
                .id(2L).version(0L).targetType(OAuthSecurityTargetEnum.CLIENT).targetKey("501")
                .generation(2L).enabled(true).revision(7L).reasonCode("INITIALIZED").build();
        AtomicReference<OAuthSecurityStateBO> subjectState = new AtomicReference<>();
        when(stateRepository.findByTarget(REALM, OAuthSecurityTargetEnum.REALM, "self"))
                .thenReturn(Optional.of(realmState));
        when(stateRepository.findByTarget(REALM, OAuthSecurityTargetEnum.CLIENT, "501"))
                .thenReturn(Optional.of(clientState));
        when(stateRepository.findByTarget(REALM, OAuthSecurityTargetEnum.SUBJECT, "subject-1"))
                .thenAnswer(invocation -> Optional.ofNullable(subjectState.get()));
        when(authorizationRepository.findByPrincipalName(eq(REALM), eq("subject-1"), anyString(), eq(0L), eq(1)))
                .thenReturn(List.of());
        when(stateRepository.save(eq(REALM), any(OAuthSecurityStateBO.class), eq(0L)))
                .thenAnswer(invocation -> {
                    OAuthSecurityStateBO saved = invocation.getArgument(1);
                    saved.setId(3L);
                    subjectState.set(saved);
                    return saved;
                });
        when(script.eval(eq(RScript.Mode.READ_WRITE), anyString(), eq(RScript.ReturnType.INTEGER),
                anyList(), any(Object[].class))).thenReturn(2L);
        org.mockito.Mockito.doReturn(List.of("recovery-epoch-7", "7", "READY",
                "1", "true", "7", "2", "true", "7", "0", "true", "7"))
                .when(script).<List<Object>>eval(eq(RScript.Mode.READ_ONLY), anyString(),
                        eq(RScript.ReturnType.MULTI), anyList());

        var snapshot = component.snapshotForAuthorization(REALM, 501L, "USER", "subject-1");

        assertThat(snapshot.getSubjectGeneration()).isZero();
        assertThat(subjectState.get().getReasonCode()).isEqualTo("INITIALIZED");
        verify(stateRepository).save(eq(REALM), any(OAuthSecurityStateBO.class), eq(0L));
        verify(authorizationRepository).findByPrincipalName(REALM, "subject-1", "ACTIVE", 0L, 1);
        verify(authorizationRepository).findByPrincipalName(REALM, "subject-1", "REVOKED", 0L, 1);
        verify(authorizationRepository).findByPrincipalName(REALM, "subject-1", "EXPIRED", 0L, 1);
    }

    @Test
    void doesNotReinitializeMissingSubjectStateWhenAuthorizationHistoryExists() {
        OAuthSecurityStateBO realmState = OAuthSecurityStateBO.builder()
                .id(1L).version(0L).targetType(OAuthSecurityTargetEnum.REALM).targetKey("self")
                .generation(1L).enabled(true).revision(7L).reasonCode("INITIALIZED").build();
        OAuthSecurityStateBO clientState = OAuthSecurityStateBO.builder()
                .id(2L).version(0L).targetType(OAuthSecurityTargetEnum.CLIENT).targetKey("501")
                .generation(2L).enabled(true).revision(7L).reasonCode("INITIALIZED").build();
        when(stateRepository.findByTarget(REALM, OAuthSecurityTargetEnum.REALM, "self"))
                .thenReturn(Optional.of(realmState));
        when(stateRepository.findByTarget(REALM, OAuthSecurityTargetEnum.CLIENT, "501"))
                .thenReturn(Optional.of(clientState));
        when(stateRepository.findByTarget(REALM, OAuthSecurityTargetEnum.SUBJECT, "subject-1"))
                .thenReturn(Optional.empty());
        when(authorizationRepository.findByPrincipalName(REALM, "subject-1", "ACTIVE", 0L, 1))
                .thenReturn(List.of(OAuthAuthorizationBO.builder().id(9L).build()));

        assertThatThrownBy(() -> component.snapshotForAuthorization(REALM, 501L, "USER", "subject-1"))
                .isInstanceOf(OAuth2AuthenticationException.class);
        verify(stateRepository, org.mockito.Mockito.never()).save(any(), any(), anyLong());
    }

    @Test
    void validatesTheConfiguredRedissonPrimaryEndpoint() {
        Config config = mock(Config.class);
        SingleServerConfig singleServer = mock(SingleServerConfig.class);
        when(redisson.getConfig()).thenReturn(config);
        when(config.isClusterConfig()).thenReturn(false);
        when(config.isSentinelConfig()).thenReturn(false);
        when(config.useSingleServer()).thenReturn(singleServer);
        when(singleServer.getAddress()).thenReturn("rediss://redis.example:6379");

        component.validatePrimaryConfiguration();

        when(singleServer.getAddress()).thenReturn("rediss://other.example:6379");
        assertThatThrownBy(component::validatePrimaryConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OAUTH_REVOCATION_PRIMARY_ENDPOINT_MISMATCH");
    }

    @Test
    @SuppressWarnings("unchecked")
    void issuerIsReadyOnlyWhenTheSharedEpochCoversCurrentDurableRevisions() {
        String issuerHash = top.egon.cola.component.common.crypto.digest.Digests.sha256Hex(
                REALM.getIssuer().normalize().toASCIIString());
        String readyKey = "egon:oauth:security:v2:{" + issuerHash + "}:ready";
        RMap<String, String> ready = mock(RMap.class);
        org.mockito.Mockito.doReturn(ready).when(redisson).getMap(eq(readyKey), eq(StringCodec.INSTANCE));
        when(ready.getAll(Set.of("epoch", "lastRevision", "status")))
                .thenReturn(Map.of("epoch", "recovery-epoch-7", "lastRevision", "9", "status", "READY"));
        when(stateRepository.findByRevisionAfter(REALM, 9L, 0L, 1)).thenReturn(List.of());
        when(revocationRepository.findByProjectionStatus(REALM, "PENDING", 9L, 0L, 1)).thenReturn(List.of());
        when(revocationRepository.findByProjectionStatus(REALM, "APPLIED", 9L, 0L, 1)).thenReturn(List.of());

        assertThat(component.isIssuerReady(REALM)).isTrue();

        when(stateRepository.findByRevisionAfter(REALM, 9L, 0L, 1)).thenReturn(List.of(
                OAuthSecurityStateBO.builder().id(12L).version(1L)
                        .targetType(OAuthSecurityTargetEnum.REALM).targetKey("self")
                        .generation(2L).enabled(true).revision(10L).reasonCode("ADMIN_REVOKE").build()));
        assertThat(component.isIssuerReady(REALM)).isFalse();
    }

    @Test
    void recoveryFencesThenRebuildsAndMarksReadyAtDatabaseRevision() {
        RLock recoveryLock = mock(RLock.class);
        RKeys keys = mock(RKeys.class);
        OAuthSecurityStateBO state = OAuthSecurityStateBO.builder()
                .id(11L).version(3L).targetType(OAuthSecurityTargetEnum.REALM).targetKey("self")
                .generation(7L).enabled(true).revision(8L).reasonCode("ADMIN_REVOKE").build();
        when(redisson.getLock(anyString())).thenReturn(recoveryLock);
        when(recoveryLock.tryLock()).thenReturn(true);
        when(redisson.getKeys()).thenReturn(keys);
        when(keys.deleteByPattern(anyString())).thenReturn(3L);
        when(stateRepository.findByTarget(REALM, OAuthSecurityTargetEnum.REALM, "self"))
                .thenReturn(Optional.of(state));
        when(realmRepository.findByRealmKey(REALM)).thenReturn(Optional.of(registeredRealm()));
        when(clientRepository.findAllByRealm(REALM)).thenReturn(List.of());
        when(stateRepository.findByRevisionAfter(REALM, 0L, 0L, 500)).thenReturn(List.of(state));
        when(revocationRepository.findByProjectionStatus(REALM, "PENDING", 0L, 0L, 500))
                .thenReturn(List.of());
        when(revocationRepository.findByProjectionStatus(REALM, "APPLIED", 0L, 0L, 500))
                .thenReturn(List.of());
        when(script.eval(any(RScript.Mode.class), anyString(), any(RScript.ReturnType.class),
                anyList(), any(Object[].class))).thenReturn(1L, 1L, 2L, 1L);

        assertThat(component.recover(REALM)).isEqualTo(8L);

        verify(keys).deleteByPattern("egon:oauth:security:v2:{"
                + top.egon.cola.component.common.crypto.digest.Digests.sha256Hex(
                        REALM.getIssuer().normalize().toASCIIString()) + "}:*");
        verify(revocationRepository).findByProjectionStatus(REALM, "PENDING", 0L, 0L, 500);
        verify(revocationRepository).findByProjectionStatus(REALM, "APPLIED", 0L, 0L, 500);
        verify(recoveryLock).unlock();
    }

    @Test
    void recoveryProjectsPendingRevocationBeforeMarkingItApplied() {
        RLock recoveryLock = mock(RLock.class);
        RKeys keys = mock(RKeys.class);
        OAuthRevocationBO pending = OAuthRevocationBO.builder()
                .id(61L).version(4L).targetType(OAuthSecurityTargetEnum.JTI).targetKey("123456")
                .reasonCode("ADMIN_REVOKE").revokedAt(NOW.minusSeconds(5))
                .expiresAt(NOW.plusSeconds(60)).revision(9L).projectionStatus("PENDING").build();
        OAuthSecurityStateBO realmState = OAuthSecurityStateBO.builder()
                .id(12L).version(1L).targetType(OAuthSecurityTargetEnum.REALM).targetKey("self")
                .generation(1L).enabled(true).revision(7L).reasonCode("INITIALIZED").build();
        when(redisson.getLock(anyString())).thenReturn(recoveryLock);
        when(recoveryLock.tryLock()).thenReturn(true);
        when(redisson.getKeys()).thenReturn(keys);
        when(keys.deleteByPattern(anyString())).thenReturn(1L);
        when(stateRepository.findByRevisionAfter(REALM, 0L, 0L, 1)).thenReturn(List.of(realmState));
        when(stateRepository.findByRevisionAfter(REALM, 0L, 0L, 500)).thenReturn(List.of(realmState));
        when(stateRepository.findByTarget(REALM, OAuthSecurityTargetEnum.REALM, "self"))
                .thenReturn(Optional.of(realmState));
        when(realmRepository.findByRealmKey(REALM)).thenReturn(Optional.of(registeredRealm()));
        when(clientRepository.findAllByRealm(REALM)).thenReturn(List.of());
        when(revocationRepository.findByProjectionStatus(REALM, "PENDING", 0L, 0L, 500))
                .thenReturn(List.of(pending));
        when(revocationRepository.findByProjectionStatus(REALM, "APPLIED", 0L, 0L, 500))
                .thenReturn(List.of());
        when(revocationRepository.save(eq(REALM), any(OAuthRevocationBO.class), eq(4L)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(script.eval(any(RScript.Mode.class), anyString(), any(RScript.ReturnType.class),
                anyList(), any(Object[].class))).thenReturn(1L, 1L, 2L, 2L, 1L);

        assertThat(component.recover(REALM)).isEqualTo(9L);

        ArgumentCaptor<OAuthRevocationBO> saved = ArgumentCaptor.forClass(OAuthRevocationBO.class);
        verify(revocationRepository).save(eq(REALM), saved.capture(), eq(4L));
        assertThat(saved.getValue().getProjectionStatus()).isEqualTo("APPLIED");
        assertThat(saved.getValue().getVersion()).isEqualTo(5L);
        verify(recoveryLock).unlock();
    }

    @Test
    void recoverySeedsRealmAndClientStatesOnlyWhenNoAuthorizationHistoryExists() {
        RLock recoveryLock = mock(RLock.class);
        RKeys keys = mock(RKeys.class);
        OAuthClientBO activeClient = OAuthClientBO.builder()
                .id(501L).clientId("client-1").status(OAuthClient.Status.ACTIVE).build();
        List<OAuthSecurityStateBO> seeded = new java.util.ArrayList<>();
        when(redisson.getLock(anyString())).thenReturn(recoveryLock);
        when(recoveryLock.tryLock()).thenReturn(true);
        when(redisson.getKeys()).thenReturn(keys);
        when(keys.deleteByPattern(anyString())).thenReturn(0L);
        when(realmRepository.findByRealmKey(REALM)).thenReturn(Optional.of(registeredRealm()));
        when(clientRepository.findAllByRealm(REALM)).thenReturn(List.of(activeClient));
        when(stateRepository.findByRevisionAfter(REALM, 0L, 0L, 1)).thenAnswer(invocation ->
                seeded.isEmpty() ? List.of() : List.of(seeded.getFirst()));
        when(stateRepository.findByRevisionAfter(REALM, 0L, 0L, 500)).thenAnswer(invocation -> List.copyOf(seeded));
        when(stateRepository.findByTarget(REALM, OAuthSecurityTargetEnum.REALM, "self"))
                .thenAnswer(invocation -> seeded.stream()
                        .filter(state -> state.getTargetType() == OAuthSecurityTargetEnum.REALM)
                        .findFirst());
        when(stateRepository.findByTarget(REALM, OAuthSecurityTargetEnum.CLIENT, "501"))
                .thenAnswer(invocation -> seeded.stream()
                        .filter(state -> state.getTargetType() == OAuthSecurityTargetEnum.CLIENT)
                        .findFirst());
        when(stateRepository.save(eq(REALM), any(OAuthSecurityStateBO.class), eq(0L)))
                .thenAnswer(invocation -> {
                    OAuthSecurityStateBO saved = invocation.getArgument(1);
                    saved.setId((long) seeded.size() + 1L);
                    seeded.add(saved);
                    return saved;
                });
        when(authorizationRepository.findExpiredBefore(eq(REALM), any(), eq(0L), eq(1)))
                .thenReturn(List.of());
        when(tokenRepository.findExpiredBefore(eq(REALM), any(), eq(0L), eq(1)))
                .thenReturn(List.of());
        when(revocationRepository.findByProjectionStatus(REALM, "PENDING", 0L, 0L, 1))
                .thenReturn(List.of());
        when(revocationRepository.findByProjectionStatus(REALM, "APPLIED", 0L, 0L, 1))
                .thenReturn(List.of());
        when(revocationRepository.findByProjectionStatus(REALM, "PENDING", 0L, 0L, 500))
                .thenReturn(List.of());
        when(revocationRepository.findByProjectionStatus(REALM, "APPLIED", 0L, 0L, 500))
                .thenReturn(List.of());
        when(script.eval(any(RScript.Mode.class), anyString(), any(RScript.ReturnType.class),
                anyList(), any(Object[].class))).thenReturn(1L, 1L, 2L, 2L, 1L);

        assertThat(component.recover(REALM)).isZero();

        assertThat(seeded).hasSize(2);
        assertThat(seeded).extracting(OAuthSecurityStateBO::getTargetType)
                .containsExactly(OAuthSecurityTargetEnum.REALM, OAuthSecurityTargetEnum.CLIENT);
        assertThat(seeded).allSatisfy(state -> {
            assertThat(state.getGeneration()).isZero();
            assertThat(state.isEnabled()).isTrue();
        });
    }

    private static OAuthSecurityChangedEvent event(
            String targetType,
            String targetKey,
            long revision,
            long generation,
            Instant expiresAt) {
        return new OAuthSecurityChangedEvent(
                "event-" + revision, 1, REALM.getIssuer(), REALM.getRealmKey(), revision,
                targetType, targetKey, generation, expiresAt, "ADMIN_REVOKE", NOW, null);
    }

    private static OAuthRealmBO registeredRealm() {
        return OAuthRealmBO.builder()
                .id(1001L).version(0L).issuer(REALM.getIssuer()).realmKey(REALM.getRealmKey())
                .realmType(RealmTypeEnum.TENANT).businessTenantId(1001L).storagePartitionId(1001L)
                .status("ACTIVE").trustVersion(0L).build();
    }
}
