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
import top.egon.cola.component.ddc.redis.DdcRedisKeys;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.registry.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static top.egon.cola.component.ddc.admin.security.admission.DdcAdmissionTestFixture.claims;

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
        when(redisson.getLock(DdcRedisKeys.registryInstance(SERVICE_KEY, "scope") + ":lock"))
                .thenReturn(scopeLock);
        when(redisson.getLock(DdcRedisKeys.globalRegistryCatalog() + ":lock"))
                .thenReturn(globalLock);
        when(redisson.<String>getBucket(
                DdcRedisKeys.registryInstance(SERVICE_KEY, "provider-1"), StringCodec.INSTANCE
        )).thenReturn(bucket);
        when(redisson.<String>getScoredSortedSet(
                DdcRedisKeys.registryService(SERVICE_KEY), StringCodec.INSTANCE
        )).thenReturn(instances);
        when(redisson.getAtomicLong(DdcRedisKeys.registryRevision(SERVICE_KEY)))
                .thenReturn(serviceRevision);
        when(serviceRevision.incrementAndGet()).thenReturn(7L);
        when(redisson.<String>getSet(catalogKey(), StringCodec.INSTANCE)).thenReturn(catalog);
        when(catalog.add(SERVICE_KEY.canonicalValue())).thenReturn(true);
        when(redisson.getAtomicLong(catalogRevisionKey())).thenReturn(catalogRevision);
        when(catalogRevision.incrementAndGet()).thenReturn(3L);
        when(redisson.getTopic(topicKey(), StringCodec.INSTANCE)).thenReturn(topic);
        when(redisson.<String>getSet(
                DdcRedisKeys.globalRegistryCatalog(), StringCodec.INSTANCE
        )).thenReturn(globalCatalog);
        when(globalCatalog.add(SERVICE_KEY.canonicalValue())).thenReturn(true);
        when(redisson.getAtomicLong(DdcRedisKeys.globalRegistryCatalogRevision()))
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
    void initialResourceVersionZeroRemainsDiscoverable() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        RScoredSortedSet<String> instances = scoredSet();
        RBucket<String> bucket = bucket();
        RAtomicLong revision = mock(RAtomicLong.class);
        when(redisson.<String>getScoredSortedSet(
                DdcRedisKeys.registryService(SERVICE_KEY), StringCodec.INSTANCE
        )).thenReturn(instances);
        when(instances.valueRange(
                Double.NEGATIVE_INFINITY, true, NOW.toEpochMilli(), true
        )).thenReturn(Set.of());
        when(instances.valueRange(
                NOW.toEpochMilli(), false, Double.POSITIVE_INFINITY, true
        )).thenReturn(Set.of("provider-1"));
        when(redisson.<String>getBucket(
                DdcRedisKeys.registryInstance(SERVICE_KEY, "provider-1"),
                StringCodec.INSTANCE
        )).thenReturn(bucket);
        when(bucket.get()).thenReturn(objectMapper.writeValueAsString(
                instance(SERVICE_KEY, "lease-1", 0L)
        ));
        when(redisson.getAtomicLong(
                DdcRedisKeys.registryRevision(SERVICE_KEY)
        )).thenReturn(revision);

        DdcServiceSnapshot snapshot = new DdcServiceRegistryRedisRepository(
                redisson,
                objectMapper
        ).getInstances(SERVICE_KEY, NOW);

        assertThat(snapshot.instances()).hasSize(1);
        assertThat(snapshot.instances().getFirst().resourceVersion()).isZero();
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
        when(redisson.getLock(DdcRedisKeys.registryInstance(SERVICE_KEY, "scope") + ":lock"))
                .thenReturn(lock);
        when(redisson.<String>getBucket(
                DdcRedisKeys.registryInstance(SERVICE_KEY, "provider-1"), StringCodec.INSTANCE
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
        when(redisson.getLock(DdcRedisKeys.registryInstance(SERVICE_KEY, "scope") + ":lock"))
                .thenReturn(lock);
        when(redisson.<String>getBucket(
                DdcRedisKeys.registryInstance(SERVICE_KEY, "provider-1"), StringCodec.INSTANCE
        )).thenReturn(bucket);
        when(bucket.get()).thenReturn(objectMapper.writeValueAsString(instance()));
        DdcServiceRegistryRedisRepository repository =
                new DdcServiceRegistryRedisRepository(redisson, objectMapper);
        DdcServiceLeaseRequest request = leaseRequest("different-lease");

        assertThat(repository.heartbeat(
                request,
                claims(NOW.plusSeconds(60)),
                NOW
        ).status())
                .isEqualTo(DdcLeaseOperationStatus.LEASE_MISMATCH);
        assertThat(repository.deregister(request, NOW).status())
                .isEqualTo(DdcLeaseOperationStatus.NOT_DELETED);
        verify(lock, org.mockito.Mockito.times(2)).unlock();
    }

    @Test
    void heartbeatCapsProviderLeaseAtTheNewAdmissionExpiry() throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        RLock scopeLock = mock(RLock.class);
        RLock globalLock = mock(RLock.class);
        RBucket<String> bucket = bucket();
        RScoredSortedSet<String> instances = scoredSet();
        RSet<String> globalCatalog = set();
        RAtomicLong globalRevision = mock(RAtomicLong.class);
        when(redisson.getLock(
                DdcRedisKeys.registryInstance(SERVICE_KEY, "scope") + ":lock"
        )).thenReturn(scopeLock);
        when(redisson.getLock(
                DdcRedisKeys.globalRegistryCatalog() + ":lock"
        )).thenReturn(globalLock);
        when(redisson.<String>getBucket(
                DdcRedisKeys.registryInstance(SERVICE_KEY, "provider-1"),
                StringCodec.INSTANCE
        )).thenReturn(bucket);
        when(bucket.get()).thenReturn(objectMapper.writeValueAsString(instance()));
        when(redisson.<String>getScoredSortedSet(
                DdcRedisKeys.registryService(SERVICE_KEY),
                StringCodec.INSTANCE
        )).thenReturn(instances);
        when(redisson.<String>getSet(
                DdcRedisKeys.globalRegistryCatalog(),
                StringCodec.INSTANCE
        )).thenReturn(globalCatalog);
        when(redisson.getAtomicLong(
                DdcRedisKeys.globalRegistryCatalogRevision()
        )).thenReturn(globalRevision);
        DdcServiceRegistryRedisRepository repository =
                new DdcServiceRegistryRedisRepository(redisson, objectMapper);

        var result = repository.heartbeat(
                leaseRequest("lease-1"),
                claims(NOW.plusSeconds(12)),
                NOW.plusSeconds(5)
        );

        assertThat(result.leaseExpireAt()).isEqualTo(NOW.plusSeconds(12));
        ArgumentCaptor<String> storedJson = ArgumentCaptor.forClass(String.class);
        verify(bucket).set(storedJson.capture(), eq(Duration.ofSeconds(7)));
        JsonNode stored = objectMapper.readTree(storedJson.getValue());
        assertThat(stored.path("resourceServerId").asText())
                .isEqualTo("resource-1");
        assertThat(stored.path("resourceVersion").asLong()).isEqualTo(7L);
        assertThat(stored.path("credentialId").asText())
                .isEqualTo("credential-1");
        assertThat(stored.path("admissionExpiresAt").asLong())
                .isEqualTo(NOW.plusSeconds(12).getEpochSecond());
        assertThat(storedJson.getValue())
                .doesNotContain("admissionTicket")
                .doesNotContain("test-admission-ticket");
    }

    @Test
    void revocationKeepsOtherApplicationAndNewerProviderLease()
            throws Exception {
        RedissonClient redisson = mock(RedissonClient.class);
        RSet<String> globalCatalog = set();
        RLock serviceLock = mock(RLock.class);
        RLock globalLock = mock(RLock.class);
        RScoredSortedSet<String> instances = scoredSet();
        RBucket<String> covered = bucket();
        RBucket<String> newer = bucket();
        RAtomicLong serviceRevision = mock(RAtomicLong.class);
        RSet<String> serviceCatalog = set();
        RAtomicLong catalogRevision = mock(RAtomicLong.class);
        RTopic topic = mock(RTopic.class);
        RAtomicLong globalRevision = mock(RAtomicLong.class);
        DdcServiceKey otherApp = new DdcServiceKey(
                "pay-biz", "dev", "billing-app",
                DdcServiceKind.RPC_PROVIDER,
                "billing.v1.QueryService", "default", "1.0.0", "grpc"
        );
        when(redisson.<String>getSet(
                DdcRedisKeys.globalRegistryCatalog(),
                StringCodec.INSTANCE
        )).thenReturn(globalCatalog);
        when(globalCatalog.readAll()).thenReturn(Set.of(
                SERVICE_KEY.canonicalValue(),
                otherApp.canonicalValue()
        ));
        when(redisson.getLock(
                DdcRedisKeys.registryInstance(SERVICE_KEY, "scope") + ":lock"
        )).thenReturn(serviceLock);
        when(redisson.getLock(
                DdcRedisKeys.globalRegistryCatalog() + ":lock"
        )).thenReturn(globalLock);
        when(redisson.<String>getScoredSortedSet(
                DdcRedisKeys.registryService(SERVICE_KEY),
                StringCodec.INSTANCE
        )).thenReturn(instances);
        when(instances.valueRange(
                Double.NEGATIVE_INFINITY,
                true,
                Double.POSITIVE_INFINITY,
                true
        )).thenReturn(Set.of("covered", "newer"));
        when(instances.isEmpty()).thenReturn(false);
        when(redisson.<String>getBucket(
                DdcRedisKeys.registryInstance(SERVICE_KEY, "covered"),
                StringCodec.INSTANCE
        )).thenReturn(covered);
        when(redisson.<String>getBucket(
                DdcRedisKeys.registryInstance(SERVICE_KEY, "newer"),
                StringCodec.INSTANCE
        )).thenReturn(newer);
        when(covered.get()).thenReturn(objectMapper.writeValueAsString(
                instance(SERVICE_KEY, "lease-covered")
        ));
        when(newer.get()).thenReturn(objectMapper.writeValueAsString(
                instance(SERVICE_KEY, "lease-newer", 8L)
        ));
        when(redisson.getAtomicLong(
                DdcRedisKeys.registryRevision(SERVICE_KEY)
        )).thenReturn(serviceRevision);
        when(serviceRevision.incrementAndGet()).thenReturn(8L);
        when(redisson.<String>getSet(
                catalogKey(),
                StringCodec.INSTANCE
        )).thenReturn(serviceCatalog);
        when(redisson.getAtomicLong(
                catalogRevisionKey()
        )).thenReturn(catalogRevision);
        when(catalogRevision.get()).thenReturn(3L);
        when(redisson.getTopic(
                topicKey(),
                StringCodec.INSTANCE
        )).thenReturn(topic);
        when(redisson.getAtomicLong(
                DdcRedisKeys.globalRegistryCatalogRevision()
        )).thenReturn(globalRevision);
        DdcServiceRegistryRedisRepository repository =
                new DdcServiceRegistryRedisRepository(
                        redisson,
                        objectMapper
                );

        int removed = repository.revokeResourceAdmission(
                "resource-1", "pay-biz", "dev", "orders-app", 7L
        );

        assertThat(removed).isEqualTo(1);
        verify(covered).delete();
        verify(newer, org.mockito.Mockito.never()).delete();
        verify(instances).remove("covered");
        verify(redisson, org.mockito.Mockito.never()).getBucket(
                DdcRedisKeys.registryInstance(otherApp, "covered"),
                StringCodec.INSTANCE
        );
    }

    private DdcServiceInstance instance() {
        return instance(SERVICE_KEY, "lease-1");
    }

    private DdcServiceInstance instance(DdcServiceKey serviceKey, String leaseId) {
        return instance(serviceKey, leaseId, 7L);
    }

    private DdcServiceInstance instance(
            DdcServiceKey serviceKey,
            String leaseId,
            long resourceVersion) {
        return new DdcServiceInstance(
                "provider-1", leaseId, serviceKey, "127.0.0.1", 19090, false,
                Map.of("zone", "east"), 30, 10, NOW, NOW,
                NOW.plusSeconds(30), "ONLINE", 0L,
                "resource-1", resourceVersion, "credential-1", NOW.plusSeconds(60)
        );
    }

    private DdcServiceLeaseRequest leaseRequest(String leaseId) {
        DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
        request.setServiceKey(SERVICE_KEY);
        request.setInstanceId("provider-1");
        request.setLeaseId(leaseId);
        request.setAdmissionTicket("test-admission-ticket");
        return request;
    }

    private String catalogKey() {
        return DdcRedisKeys.registryCatalog(
                SERVICE_KEY.bizCode(), SERVICE_KEY.env(), SERVICE_KEY.appCode(),
                SERVICE_KEY.serviceKind(), SERVICE_KEY.protocol());
    }

    private String catalogRevisionKey() {
        return DdcRedisKeys.registryCatalogRevision(
                SERVICE_KEY.bizCode(), SERVICE_KEY.env(), SERVICE_KEY.appCode(),
                SERVICE_KEY.serviceKind(), SERVICE_KEY.protocol());
    }

    private String topicKey() {
        return DdcRedisKeys.registryTopic(
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
