package top.egon.cola.platform.tianquan.shoubing.admin.support.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.domain.bo.OAuthAuditBO;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repository.OAuthAuditRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.RealmTypeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.contract.IdentityUserState;
import top.egon.cola.platform.tianquan.shoubing.core.audit.IdentitySecurityEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IdentityOutboxPublisherTest {

    private static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    private static final TrustedRealmBO PLATFORM = TrustedRealmBO.builder()
            .issuer(URI.create("https://idp.example/platform"))
            .realmKey("platform")
            .realmType(RealmTypeEnum.PLATFORM)
            .storagePartitionId(9000L)
            .trustVersion(0L)
            .build();

    private final OAuthAuditRepository audits = mock(OAuthAuditRepository.class);
    private final TransactionalOutbox outbox = mock(TransactionalOutbox.class);
    private final RedissonClient redisson = mock(RedissonClient.class);
    private final RBucket<String> bucket = mock(RBucket.class);
    private final OAuthManagementRealmResolver realmResolver = mock(OAuthManagementRealmResolver.class);
    private IdentityOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        doReturn(bucket).when(redisson).getBucket(anyString(), eq(StringCodec.INSTANCE));
        when(realmResolver.platformRealm()).thenReturn(PLATFORM);
        when(audits.save(eq(PLATFORM), any(OAuthAuditBO.class), eq(0L)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        publisher = new IdentityOutboxPublisher(audits, outbox, redisson,
                new ObjectMapper().findAndRegisterModules(), realmResolver, "identity:v1:user-state:",
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void commitsIdentityAuditAndComponentOutboxWithCompatibleSafeFields() {
        publisher.append(new IdentitySecurityEvent(
                "IDENTITY_TOKEN_REVOKED", "alice-sub", "SECRET_ROTATED", "SELF_SERVICE", NOW));

        var audit = org.mockito.ArgumentCaptor.forClass(OAuthAuditBO.class);
        verify(audits).save(eq(PLATFORM), audit.capture(), eq(0L));
        assertThat(audit.getValue().getAuditKind()).isEqualTo("IDENTITY");
        assertThat(audit.getValue().getPublicId()).isEqualTo(audit.getValue().getEventId());
        assertThat(audit.getValue().getActorSub()).isEqualTo("alice-sub");
        assertThat(audit.getValue().getTargetSub()).isEqualTo("alice-sub");
        assertThat(audit.getValue().getPayload().path("sourceBucket").asText()).isEqualTo("SELF_SERVICE");

        var outboxMessage = org.mockito.ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outbox).enqueue(outboxMessage.capture());
        assertThat(outboxMessage.getValue().channel()).isEqualTo("oauth-audit");
        assertThat(outboxMessage.getValue().destination()).isEqualTo("oauth.audit.identity.v1");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) outboxMessage.getValue().payload();
        assertThat(payload).containsEntry("eventType", "IDENTITY_TOKEN_REVOKED")
                .containsEntry("actorSub", "alice-sub")
                .containsEntry("targetSub", "alice-sub")
                .doesNotContainKey("passwordHash")
                .doesNotContainKey("token");
        verifyNoInteractions(redisson);
    }

    @Test
    void businessStateProjectionWaitsUntilAfterTheDatabaseCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            publisher.publish(new IdentityUserState("alice-sub", IdentityUserState.Status.DISABLED, NOW));
            verifyNoInteractions(redisson, bucket, outbox, audits);
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
            verify(redisson).getBucket("identity:v1:user-state:alice-sub", StringCodec.INSTANCE);
            verify(bucket).set(anyString());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void startupProjectionRestoresRedisWithoutCreatingDomainEvents() {
        publisher.project(new IdentityUserState("alice-sub", IdentityUserState.Status.ACTIVE, NOW));

        verify(bucket).set(anyString());
        verifyNoInteractions(outbox, audits);
    }
}
