package top.egon.cola.platform.idp.admin.support.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.idp.admin.audit.domain.IdentityAuditLogEntity;
import top.egon.cola.platform.idp.admin.audit.infrastructure.IdentityAuditLogRepository;
import top.egon.cola.platform.idp.admin.support.outbox.domain.pojo.IdentityOutboxEventEntity;
import top.egon.cola.platform.idp.admin.support.outbox.repo.IdentityOutboxEventRepository;
import top.egon.cola.platform.idp.contract.IdentityUserState;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEvent;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IdentityOutboxPublisherTest {

    private static final Instant NOW =
            Instant.parse("2026-08-02T00:00:00Z");

    private final IdentityOutboxEventRepository outbox =
            mock(IdentityOutboxEventRepository.class);
    private final IdentityAuditLogRepository audits =
            mock(IdentityAuditLogRepository.class);
    private final RefreshTokenStore refreshTokens =
            mock(RefreshTokenStore.class);
    private final RedissonClient redisson = mock(RedissonClient.class);
    private final RBucket<String> bucket = mock(RBucket.class);
    private final AtomicLong ids = new AtomicLong(100L);

    private IdentityOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        when(redisson.<String>getBucket(anyString(), eq(StringCodec.INSTANCE)))
                .thenReturn(bucket);
        publisher = new IdentityOutboxPublisher(
                outbox,
                audits,
                refreshTokens,
                redisson,
                new ObjectMapper().findAndRegisterModules(),
                () -> Long.toString(ids.incrementAndGet()),
                "identity:v1:user-state:"
        );
    }

    @Test
    void publishesSafeRedisStateAndDurableStateEvent() {
        publisher.publish(new IdentityUserState(
                "alice-sub",
                IdentityUserState.Status.ACTIVE,
                4L,
                NOW
        ));

        verify(redisson).getBucket(
                "identity:v1:user-state:alice-sub",
                StringCodec.INSTANCE
        );
        verify(bucket).set(anyString());
        IdentityOutboxEventEntity event = captureOutbox();
        assertThat(event.getEventType()).isEqualTo("IDENTITY_USER_STATE_CHANGED");
        assertThat(event.getPayload())
                .contains("alice-sub", "tokenVersion", "4")
                .doesNotContain("password", "refreshToken");
    }

    @Test
    void startupProjectionRestoresRedisWithoutCreatingDomainEvents() {
        publisher.project(new IdentityUserState(
                "alice-sub",
                IdentityUserState.Status.ACTIVE,
                4L,
                NOW
        ));

        verify(bucket).set(anyString());
        verifyNoInteractions(outbox, audits, refreshTokens);
    }

    @Test
    void revocationRevokesRefreshFamiliesAndAppendsAuditAndOutbox() {
        publisher.revokeFamilies(
                "alice-sub",
                5L,
                "PASSWORD_CHANGED"
        );
        publisher.append(new IdentitySecurityEvent(
                "IDENTITY_TOKEN_REVOKED",
                "alice-sub",
                "PASSWORD_CHANGED",
                "SELF_SERVICE",
                5L,
                NOW
        ));

        verify(refreshTokens).revokeSubject(
                eq("alice-sub"),
                eq("PASSWORD_CHANGED"),
                any(Instant.class)
        );
        verify(audits).save(any(IdentityAuditLogEntity.class));
        IdentityOutboxEventEntity event = captureOutbox();
        assertThat(event.getAggregateId()).isEqualTo("alice-sub");
        assertThat(event.getPayload())
                .contains("PASSWORD_CHANGED")
                .doesNotContain("passwordHash", "refreshToken");
    }

    private IdentityOutboxEventEntity captureOutbox() {
        org.mockito.ArgumentCaptor<IdentityOutboxEventEntity> captor =
                org.mockito.ArgumentCaptor.forClass(
                        IdentityOutboxEventEntity.class
                );
        verify(outbox, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}
