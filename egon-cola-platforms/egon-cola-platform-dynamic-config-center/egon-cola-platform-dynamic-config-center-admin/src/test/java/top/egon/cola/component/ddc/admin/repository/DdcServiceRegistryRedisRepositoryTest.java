package top.egon.cola.component.ddc.admin.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSet;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.model.dto.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DdcServiceRegistryRedisRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void registrationUsesRedissonObjectsAndStoresEpochProjection() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock scopeLock = mock(RLock.class);
        RLock globalLock = mock(RLock.class);
        RBucket<String> bucket = bucket();
        RScoredSortedSet<String> instances = scoredSet();
        RAtomicLong serviceRevision = mock(RAtomicLong.class);
        RSet<String> catalog = set();
        RAtomicLong catalogRevision = mock(RAtomicLong.class);
        RTopic topic = mock(RTopic.class);
        RSet<String> globalCatalog = set();
        RAtomicLong globalRevision = mock(RAtomicLong.class);
        when(redisson.getLock(DdcKeys.registryInstance(SERVICE_KEY, "scope") + ":lock"))
                .thenReturn(scopeLock);
        when(redisson.getLock(DdcKeys.globalRegistryCatalog() + ":lock"))
                .thenReturn(globalLock);
        when(redisson.<String>getBucket(
                DdcKeys.registryInstance(SERVICE_KEY, "provider-1"), StringCodec.INSTANCE
        )).thenReturn(bucket);
        when(redisson.<String>getScoredSortedSet(
                DdcKeys.registryService(SERVICE_KEY), StringCodec.INSTANCE
        )).thenReturn(instances);
        when(redisson.getAtomicLong(DdcKeys.registryRevision(SERVICE_KEY)))
                .thenReturn(serviceRevision);
        when(serviceRevision.incrementAndGet()).thenReturn(7L);
        when(redisson.<String>getSet(catalogKey(), StringCodec.INSTANCE)).thenReturn(catalog);
        when(catalog.add(SERVICE_KEY.canonicalValue())).thenReturn(true);
        when(redisson.getAtomicLong(catalogRevisionKey())).thenReturn(catalogRevision);
        when(catalogRevision.incrementAndGet()).thenReturn(3L);
        when(redisson.getTopic(topicKey(), StringCodec.INSTANCE)).thenReturn(topic);
        when(redisson.<String>getSet(
                DdcKeys.globalRegistryCatalog(), StringCodec.INSTANCE
        )).thenReturn(globalCatalog);
        when(globalCatalog.add(SERVICE_KEY.canonicalValue())).thenReturn(true);
        when(redisson.getAtomicLong(DdcKeys.globalRegistryCatalogRevision()))
                .thenReturn(globalRevision);
        DdcServiceRegistryRedisRepository repository =
                new DdcServiceRegistryRedisRepository(redisson, objectMapper);

        repository.register(instance());

        ArgumentCaptor<String> storedJson = ArgumentCaptor.forClass(String.class);
        verify(bucket).set(storedJson.capture(), eq(Duration.ofSeconds(30)));
        JsonNode stored = objectMapper.readTree(storedJson.getValue());
        assertThat(stored.path("serviceKeyCanonical").asText())
                .isEqualTo(SERVICE_KEY.canonicalValue());
        assertThat(stored.path("lastHeartbeatAtEpochMillis").asLong())
                .isEqualTo(NOW.toEpochMilli());
        assertThat(stored.path("leaseExpireAtEpochMillis").asLong())
                .isEqualTo(NOW.plusSeconds(30).toEpochMilli());
        assertThat(stored.path("revision").asLong()).isEqualTo(7L);
        verify(instances).add(NOW.plusSeconds(30).toEpochMilli(), "provider-1");
        verify(topic).publish(anyString());
        verify(globalRevision).incrementAndGet();
        verify(scopeLock).unlock();
        verify(globalLock).unlock();
    }

    @Test
    void registrationRejectsAnInstanceOwnedByAnotherService() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        RBucket<String> bucket = bucket();
        DdcServiceKey otherService = new DdcServiceKey(
                "pay-biz", "dev", "orders-app", DdcServiceKind.RPC_PROVIDER,
                "order.v1.OtherService", "default", "1.0.0", "grpc"
        );
        when(redisson.getLock(DdcKeys.registryInstance(SERVICE_KEY, "scope") + ":lock"))
                .thenReturn(lock);
        when(redisson.<String>getBucket(
                DdcKeys.registryInstance(SERVICE_KEY, "provider-1"), StringCodec.INSTANCE
        )).thenReturn(bucket);
        when(bucket.get()).thenReturn(objectMapper.writeValueAsString(
                instance(otherService, "lease-1")));

        assertThatThrownBy(() -> new DdcServiceRegistryRedisRepository(
                redisson, objectMapper).register(instance()))
                .isInstanceOf(DdcAdminException.class)
                .hasMessageContaining("instance id conflict");
        verify(lock).unlock();
    }

    @Test
    void heartbeatAndDeregisterPreserveStrictLeaseOutcomes() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        RBucket<String> bucket = bucket();
        when(redisson.getLock(DdcKeys.registryInstance(SERVICE_KEY, "scope") + ":lock"))
                .thenReturn(lock);
        when(redisson.<String>getBucket(
                DdcKeys.registryInstance(SERVICE_KEY, "provider-1"), StringCodec.INSTANCE
        )).thenReturn(bucket);
        when(bucket.get()).thenReturn(objectMapper.writeValueAsString(instance()));
        DdcServiceRegistryRedisRepository repository =
                new DdcServiceRegistryRedisRepository(redisson, objectMapper);
        DdcServiceLeaseRequest request = leaseRequest("different-lease");

        assertThat(repository.heartbeat(request, NOW).status())
                .isEqualTo(DdcLeaseOperationStatus.LEASE_MISMATCH);
        assertThat(repository.deregister(request, NOW).status())
                .isEqualTo(DdcLeaseOperationStatus.NOT_DELETED);
        verify(lock, org.mockito.Mockito.times(2)).unlock();
    }

    private DdcServiceInstance instance() {
        return instance(SERVICE_KEY, "lease-1");
    }

    private DdcServiceInstance instance(DdcServiceKey serviceKey, String leaseId) {
        return new DdcServiceInstance(
                "provider-1", leaseId, serviceKey, "127.0.0.1", 19090, false,
                Map.of("zone", "east"), 30, 10, NOW, NOW,
                NOW.plusSeconds(30), "ONLINE", 0L
        );
    }

    private DdcServiceLeaseRequest leaseRequest(String leaseId) {
        DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
        request.setServiceKey(SERVICE_KEY);
        request.setInstanceId("provider-1");
        request.setLeaseId(leaseId);
        return request;
    }

    private String catalogKey() {
        return DdcKeys.registryCatalog(
                SERVICE_KEY.bizCode(), SERVICE_KEY.env(), SERVICE_KEY.appCode(),
                SERVICE_KEY.serviceKind(), SERVICE_KEY.protocol());
    }

    private String catalogRevisionKey() {
        return DdcKeys.registryCatalogRevision(
                SERVICE_KEY.bizCode(), SERVICE_KEY.env(), SERVICE_KEY.appCode(),
                SERVICE_KEY.serviceKind(), SERVICE_KEY.protocol());
    }

    private String topicKey() {
        return DdcKeys.registryTopic(
                SERVICE_KEY.bizCode(), SERVICE_KEY.env(), SERVICE_KEY.appCode(),
                SERVICE_KEY.serviceKind(), SERVICE_KEY.protocol());
    }

    @SuppressWarnings("unchecked")
    private <T> RBucket<T> bucket() {
        return mock(RBucket.class);
    }

    @SuppressWarnings("unchecked")
    private <T> RSet<T> set() {
        return mock(RSet.class);
    }

    @SuppressWarnings("unchecked")
    private <T> RScoredSortedSet<T> scoredSet() {
        return mock(RScoredSortedSet.class);
    }

    private static final DdcServiceKey SERVICE_KEY = new DdcServiceKey(
            "pay-biz", "dev", "orders-app", DdcServiceKind.RPC_PROVIDER,
            "order.v1.OrderQueryService", "default", "1.0.0", "grpc"
    );
}
