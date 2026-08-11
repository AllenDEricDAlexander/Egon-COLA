package top.egon.cola.component.ddc.admin.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.component.ddc.redis.DdcRedisKeys;
import top.egon.cola.component.ddc.model.config.DdcHeartbeatRequest;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.instance.DdcInstanceIdentity;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static top.egon.cola.component.ddc.admin.security.admission.DdcAdmissionTestFixture.claims;

class DdcConfigLeaseRedisRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    @Test
    void registerHeartbeatAndDeregisterUseRedissonLeaseObjects() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        RBucket<String> bucket = bucket();
        RSet<String> instances = set();
        AtomicReference<String> stored = new AtomicReference<>();
        Set<String> members = new HashSet<>();
        when(redisson.getLock(anyString())).thenReturn(lock);
        when(redisson.<String>getBucket(
                DdcRedisKeys.configLeaseInstance("retail", "dev", "demo", "instance-1"),
                StringCodec.INSTANCE
        )).thenReturn(bucket);
        when(redisson.<String>getSet(
                DdcRedisKeys.configLeaseInstances("retail", "dev", "demo"),
                StringCodec.INSTANCE
        )).thenReturn(instances);
        when(bucket.get()).thenAnswer(invocation -> stored.get());
        doAnswer(invocation -> {
            stored.set(invocation.getArgument(0));
            return null;
        }).when(bucket).set(anyString(), eq(Duration.ofSeconds(30)));
        when(bucket.delete()).thenAnswer(invocation -> stored.getAndSet(null) != null);
        when(instances.add(anyString())).thenAnswer(
                invocation -> members.add(invocation.getArgument(0)));
        when(instances.remove(anyString())).thenAnswer(
                invocation -> members.remove(invocation.getArgument(0)));
        DdcConfigLeaseRedisRepository repository =
                new DdcConfigLeaseRedisRepository(redisson, new ObjectMapper());
        DdcInstanceIdentity identity = new DdcInstanceIdentity(
                "instance-1", "retail", "demo", "dev",
                "127.0.0.1", 8080, "100", "5.3.3");
        DdcLeaseSession session = new DdcLeaseSession(
                "instance-1", "lease-1", DdcLeaseRole.CONFIG_CLIENT,
                30, 10, NOW, NOW.plusSeconds(30));

        repository.register(identity, session, NOW, claims(NOW.plusSeconds(60)));
        var registered = new ObjectMapper().readTree(stored.get());
        assertThat(registered.path("resourceServerId").asText())
                .isEqualTo("resource-1");
        assertThat(registered.path("resourceVersion").asLong()).isEqualTo(7L);
        assertThat(registered.path("credentialId").asText())
                .isEqualTo("credential-1");
        assertThat(registered.path("admissionExpiresAt").asLong())
                .isEqualTo(NOW.plusSeconds(60).toEpochMilli());
        assertThat(stored.get())
                .doesNotContain("admissionTicket")
                .doesNotContain("test-admission-ticket");
        DdcHeartbeatRequest request = request("lease-1");
        var heartbeat = repository.heartbeat(
                request,
                claims(NOW.plusSeconds(20)),
                NOW.plusSeconds(5)
        );
        var deregister = repository.deregister(request);

        assertThat(heartbeat.status()).isEqualTo(DdcLeaseOperationStatus.RENEWED);
        assertThat(heartbeat.leaseExpireAt()).isEqualTo(NOW.plusSeconds(20));
        assertThat(deregister.status()).isEqualTo(DdcLeaseOperationStatus.DELETED);
        assertThat(stored).hasNullValue();
        assertThat(members).isEmpty();
        verify(lock, org.mockito.Mockito.times(3)).unlock();
    }

    @Test
    void heartbeatRejectsAnotherLeaseWithoutMutatingProjection() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        RBucket<String> bucket = bucket();
        when(redisson.getLock(anyString())).thenReturn(lock);
        when(redisson.<String>getBucket(
                DdcRedisKeys.configLeaseInstance("retail", "dev", "demo", "instance-1"),
                StringCodec.INSTANCE
        )).thenReturn(bucket);
        when(bucket.get()).thenReturn(new ObjectMapper().writeValueAsString(
                java.util.Map.of(
                        "instanceId", "instance-1",
                        "leaseId", "lease-1",
                        "leaseSeconds", 30
                )
        ));
        DdcConfigLeaseRedisRepository repository =
                new DdcConfigLeaseRedisRepository(redisson, new ObjectMapper());

        assertThat(repository.heartbeat(
                request("lease-2"),
                claims(NOW.plusSeconds(60)),
                NOW
        ).status())
                .isEqualTo(DdcLeaseOperationStatus.LEASE_MISMATCH);
        verify(lock).unlock();
    }

    private DdcHeartbeatRequest request(String leaseId) {
        DdcHeartbeatRequest request = new DdcHeartbeatRequest();
        request.setInstanceId("instance-1");
        request.setLeaseId(leaseId);
        request.setBizCode("retail");
        request.setEnv("dev");
        request.setAppCode("demo");
        return request;
    }

    @SuppressWarnings("unchecked")
    private <T> RBucket<T> bucket() {
        return mock(RBucket.class);
    }

    @SuppressWarnings("unchecked")
    private <T> RSet<T> set() {
        return mock(RSet.class);
    }
}
