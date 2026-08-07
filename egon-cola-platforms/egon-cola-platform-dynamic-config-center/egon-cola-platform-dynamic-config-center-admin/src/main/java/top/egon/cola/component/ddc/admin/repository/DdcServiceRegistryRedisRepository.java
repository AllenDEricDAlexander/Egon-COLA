package top.egon.cola.component.ddc.admin.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.common.DdcErrorStatus;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.model.dto.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.registry.DdcRegistryEvent;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DdcServiceRegistryRedisRepository {

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    private final Set<DdcServiceKey> knownServiceKeys = ConcurrentHashMap.newKeySet();

    public DdcServiceRegistryRedisRepository(RedissonClient redissonClient,
                                             ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    public void register(DdcServiceInstance instance) {
        DdcServiceKey serviceKey = instance.serviceKey();
        RLock lock = serviceLock(serviceKey);
        lock.lock();
        try {
            String current = instanceBucket(serviceKey, instance.instanceId()).get();
            if (current != null
                    && !serviceKey.equals(instance(current).serviceKey())) {
                throw new DdcAdminException(DdcErrorStatus.INSTANCE_ID_CONFLICT);
            }

            long serviceRevision = serviceRevision(serviceKey).incrementAndGet();
            long catalogRevision = addServiceCatalog(serviceKey);
            instanceBucket(serviceKey, instance.instanceId()).set(
                    instanceJson(instance, serviceRevision),
                    Duration.ofSeconds(instance.leaseSeconds())
            );
            serviceInstances(serviceKey).add(
                    instance.leaseExpireAt().toEpochMilli(), instance.instanceId());
            publishEvent(serviceKey, serviceRevision, catalogRevision);
            addGlobalCatalog(serviceKey);
        } finally {
            lock.unlock();
        }
        knownServiceKeys.add(serviceKey);
    }

    public DdcLeaseOperationResult heartbeat(DdcServiceLeaseRequest request,
                                             Instant heartbeatAt) {
        DdcServiceKey serviceKey = request.getServiceKey();
        RLock lock = serviceLock(serviceKey);
        lock.lock();
        try {
            String current = instanceBucket(serviceKey, request.getInstanceId()).get();
            if (current == null) {
                return new DdcLeaseOperationResult(DdcLeaseOperationStatus.NOT_FOUND, null);
            }
            DdcServiceInstance instance = instance(current);
            if (!sameIdentity(instance, request)) {
                return new DdcLeaseOperationResult(DdcLeaseOperationStatus.LEASE_MISMATCH, null);
            }
            Instant leaseExpireAt = heartbeatAt.plusSeconds(instance.leaseSeconds());
            DdcServiceInstance renewed = new DdcServiceInstance(
                    instance.instanceId(), instance.leaseId(), instance.serviceKey(),
                    instance.host(), instance.port(), instance.secure(), instance.metadata(),
                    instance.leaseSeconds(), instance.heartbeatIntervalSeconds(),
                    instance.registeredAt(), heartbeatAt, leaseExpireAt,
                    instance.status(), instance.revision()
            );
            instanceBucket(serviceKey, request.getInstanceId()).set(
                    instanceJson(renewed, renewed.revision()),
                    Duration.ofSeconds(renewed.leaseSeconds())
            );
            serviceInstances(serviceKey).add(
                    leaseExpireAt.toEpochMilli(), request.getInstanceId());
            addGlobalCatalog(serviceKey);
            return new DdcLeaseOperationResult(DdcLeaseOperationStatus.RENEWED, leaseExpireAt);
        } finally {
            lock.unlock();
        }
    }

    public DdcLeaseOperationResult deregister(DdcServiceLeaseRequest request,
                                              Instant deregisteredAt) {
        DdcServiceKey serviceKey = request.getServiceKey();
        DdcLeaseOperationResult result;
        RLock lock = serviceLock(serviceKey);
        lock.lock();
        try {
            String current = instanceBucket(serviceKey, request.getInstanceId()).get();
            if (current == null) {
                return new DdcLeaseOperationResult(DdcLeaseOperationStatus.NOT_FOUND, null);
            }
            if (!sameIdentity(instance(current), request)) {
                return new DdcLeaseOperationResult(DdcLeaseOperationStatus.NOT_DELETED, null);
            }
            instanceBucket(serviceKey, request.getInstanceId()).delete();
            serviceInstances(serviceKey).remove(request.getInstanceId());
            long serviceRevision = serviceRevision(serviceKey).incrementAndGet();
            CatalogUpdate catalog = removeServiceCatalogIfEmpty(serviceKey);
            publishEvent(serviceKey, serviceRevision, catalog.revision());
            removeGlobalCatalogIfEmpty(serviceKey);
            result = new DdcLeaseOperationResult(DdcLeaseOperationStatus.DELETED, null);
        } finally {
            lock.unlock();
        }
        return result;
    }

    public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey, Instant now) {
        knownServiceKeys.add(serviceKey);
        Collection<String> expired = serviceInstances(serviceKey)
                .valueRange(Double.NEGATIVE_INFINITY, true, now.toEpochMilli(), true);
        expired.forEach(instanceId -> expire(serviceKey, instanceId, now));

        Collection<String> members = serviceInstances(serviceKey)
                .valueRange(now.toEpochMilli(), false, Double.POSITIVE_INFINITY, true);
        List<DdcServiceInstance> instances = new ArrayList<>();
        for (String instanceId : members) {
            String value = instanceBucket(serviceKey, instanceId).get();
            if (value == null) {
                expire(serviceKey, instanceId, now);
                continue;
            }
            DdcServiceInstance instance = instance(value);
            if (!serviceKey.equals(instance.serviceKey())
                    || instance.leaseExpireAt() == null
                    || !instance.leaseExpireAt().isAfter(now)) {
                expire(serviceKey, instanceId, now);
                continue;
            }
            instances.add(instance);
        }
        if (instances.isEmpty()) {
            expire(serviceKey, "", now);
        }
        return new DdcServiceSnapshot(
                serviceKey, serviceRevision(serviceKey).get(), instances, now);
    }

    public DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query,
                                                    Instant now) {
        String catalogKey = query.hasExactCatalogScope()
                ? DdcKeys.v3RegistryCatalog(
                        query.bizCode(), query.env(), query.appCode(),
                        query.serviceKind(), query.protocol())
                : DdcKeys.v3GlobalRegistryCatalog();
        Set<String> members = redissonClient.<String>getSet(
                catalogKey, StringCodec.INSTANCE).readAll();
        List<DdcServiceKey> serviceKeys = new ArrayList<>();
        for (String member : members) {
            DdcServiceKey serviceKey = DdcServiceKey.parse(member);
            if (query.matches(serviceKey)
                    && !getInstances(serviceKey, now).instances().isEmpty()) {
                serviceKeys.add(serviceKey);
            }
        }
        serviceKeys.sort(DdcServiceKey::compareTo);
        String revisionKey = query.hasExactCatalogScope()
                ? DdcKeys.v3RegistryCatalogRevision(
                        query.bizCode(), query.env(), query.appCode(),
                        query.serviceKind(), query.protocol())
                : DdcKeys.v3GlobalRegistryCatalogRevision();
        return new DdcServiceCatalogSnapshot(
                query, redissonClient.getAtomicLong(revisionKey).get(), serviceKeys, now);
    }

    public int expireKnown(Instant now) {
        int removed = 0;
        for (DdcServiceKey serviceKey : List.copyOf(knownServiceKeys)) {
            int before = serviceInstances(serviceKey).size();
            getInstances(serviceKey, now);
            int after = serviceInstances(serviceKey).size();
            removed += Math.max(0, before - after);
            if (after == 0) {
                knownServiceKeys.remove(serviceKey);
            }
        }
        return removed;
    }

    private void expire(DdcServiceKey serviceKey, String instanceId, Instant now) {
        boolean changed = false;
        RLock lock = serviceLock(serviceKey);
        lock.lock();
        try {
            if (instanceId.isEmpty()) {
                if (!serviceInstances(serviceKey).isEmpty()) {
                    return;
                }
            } else {
                Double score = serviceInstances(serviceKey).getScore(instanceId);
                if (score == null || score > now.toEpochMilli()) {
                    return;
                }
                String current = instanceBucket(serviceKey, instanceId).get();
                if (current != null) {
                    DdcServiceInstance instance = instance(current);
                    if (serviceKey.equals(instance.serviceKey())
                            && instance.leaseExpireAt().isAfter(now)) {
                        serviceInstances(serviceKey).add(
                                instance.leaseExpireAt().toEpochMilli(), instanceId);
                        return;
                    }
                    if (serviceKey.equals(instance.serviceKey())) {
                        instanceBucket(serviceKey, instanceId).delete();
                    }
                }
                serviceInstances(serviceKey).remove(instanceId);
                changed = true;
            }

            long serviceRevision = changed
                    ? serviceRevision(serviceKey).incrementAndGet()
                    : serviceRevision(serviceKey).get();
            CatalogUpdate catalog = removeServiceCatalogIfEmpty(serviceKey);
            if (changed || catalog.changed()) {
                publishEvent(serviceKey, serviceRevision, catalog.revision());
            }
            removeGlobalCatalogIfEmpty(serviceKey);
        } finally {
            lock.unlock();
        }
    }

    private boolean sameIdentity(DdcServiceInstance instance,
                                 DdcServiceLeaseRequest request) {
        return request.getInstanceId().equals(instance.instanceId())
                && request.getLeaseId().equals(instance.leaseId())
                && request.getServiceKey().equals(instance.serviceKey());
    }

    private long addServiceCatalog(DdcServiceKey serviceKey) {
        boolean added = serviceCatalog(serviceKey).add(serviceKey.canonicalValue());
        return added
                ? catalogRevision(serviceKey).incrementAndGet()
                : catalogRevision(serviceKey).get();
    }

    private CatalogUpdate removeServiceCatalogIfEmpty(DdcServiceKey serviceKey) {
        if (!serviceInstances(serviceKey).isEmpty()) {
            return new CatalogUpdate(catalogRevision(serviceKey).get(), false);
        }
        boolean removed = serviceCatalog(serviceKey).remove(serviceKey.canonicalValue());
        long revision = removed
                ? catalogRevision(serviceKey).incrementAndGet()
                : catalogRevision(serviceKey).get();
        return new CatalogUpdate(revision, removed);
    }

    private void addGlobalCatalog(DdcServiceKey serviceKey) {
        RLock lock = globalCatalogLock();
        lock.lock();
        try {
            boolean added = redissonClient.<String>getSet(
                    DdcKeys.v3GlobalRegistryCatalog(), StringCodec.INSTANCE
            ).add(serviceKey.canonicalValue());
            if (added) {
                redissonClient.getAtomicLong(
                        DdcKeys.v3GlobalRegistryCatalogRevision()).incrementAndGet();
            }
        } finally {
            lock.unlock();
        }
    }

    private void removeGlobalCatalogIfEmpty(DdcServiceKey serviceKey) {
        RLock lock = globalCatalogLock();
        lock.lock();
        try {
            if (!serviceInstances(serviceKey).isEmpty()) {
                return;
            }
            boolean removed = redissonClient.<String>getSet(
                    DdcKeys.v3GlobalRegistryCatalog(), StringCodec.INSTANCE
            ).remove(serviceKey.canonicalValue());
            if (removed) {
                redissonClient.getAtomicLong(
                        DdcKeys.v3GlobalRegistryCatalogRevision()).incrementAndGet();
            }
        } finally {
            lock.unlock();
        }
    }

    private RBucket<String> instanceBucket(DdcServiceKey serviceKey, String instanceId) {
        return redissonClient.getBucket(
                DdcKeys.v3RegistryInstance(serviceKey, instanceId), StringCodec.INSTANCE);
    }

    private RScoredSortedSet<String> serviceInstances(DdcServiceKey serviceKey) {
        return redissonClient.getScoredSortedSet(
                DdcKeys.v3RegistryService(serviceKey), StringCodec.INSTANCE);
    }

    private RSet<String> serviceCatalog(DdcServiceKey serviceKey) {
        return redissonClient.getSet(
                DdcKeys.v3RegistryCatalog(
                        serviceKey.bizCode(), serviceKey.env(), serviceKey.appCode(),
                        serviceKey.serviceKind(), serviceKey.protocol()),
                StringCodec.INSTANCE
        );
    }

    private RAtomicLong serviceRevision(DdcServiceKey serviceKey) {
        return redissonClient.getAtomicLong(DdcKeys.v3RegistryRevision(serviceKey));
    }

    private RAtomicLong catalogRevision(DdcServiceKey serviceKey) {
        return redissonClient.getAtomicLong(DdcKeys.v3RegistryCatalogRevision(
                serviceKey.bizCode(), serviceKey.env(), serviceKey.appCode(),
                serviceKey.serviceKind(), serviceKey.protocol()));
    }

    private RLock serviceLock(DdcServiceKey serviceKey) {
        return redissonClient.getLock(
                DdcKeys.v3RegistryInstance(serviceKey, "scope") + ":lock");
    }

    private RLock globalCatalogLock() {
        return redissonClient.getLock(DdcKeys.v3GlobalRegistryCatalog() + ":lock");
    }

    private void publishEvent(DdcServiceKey serviceKey,
                              long serviceRevision,
                              long catalogRevision) {
        redissonClient.getTopic(DdcKeys.v3RegistryTopic(
                serviceKey.bizCode(), serviceKey.env(), serviceKey.appCode(),
                serviceKey.serviceKind(), serviceKey.protocol()), StringCodec.INSTANCE
        ).publish(json(new DdcRegistryEvent(
                serviceKey, serviceRevision, catalogRevision)));
    }

    private String instanceJson(DdcServiceInstance instance, long revision) {
        ObjectNode value = objectMapper.valueToTree(instance);
        value.put("revision", revision);
        value.put("serviceKeyCanonical", instance.serviceKey().canonicalValue());
        value.put("lastHeartbeatAtEpochMillis", instance.lastHeartbeatAt().toEpochMilli());
        value.put("leaseExpireAtEpochMillis", instance.leaseExpireAt().toEpochMilli());
        return json(value);
    }

    private DdcServiceInstance instance(String value) {
        try {
            JsonNode node = objectMapper.readTree(value);
            if (node instanceof ObjectNode objectNode) {
                JsonNode lastHeartbeatAt = objectNode.remove("lastHeartbeatAtEpochMillis");
                JsonNode leaseExpireAt = objectNode.remove("leaseExpireAtEpochMillis");
                objectNode.remove("serviceKeyCanonical");
                if (lastHeartbeatAt != null && lastHeartbeatAt.canConvertToLong()) {
                    objectNode.put("lastHeartbeatAt",
                            Instant.ofEpochMilli(lastHeartbeatAt.longValue()).toString());
                }
                if (leaseExpireAt != null && leaseExpireAt.canConvertToLong()) {
                    objectNode.put("leaseExpireAt",
                            Instant.ofEpochMilli(leaseExpireAt.longValue()).toString());
                }
            }
            return objectMapper.treeToValue(node, DdcServiceInstance.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("deserialize DDC service instance failed", exception);
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("serialize DDC service registry value failed", exception);
        }
    }

    private record CatalogUpdate(long revision, boolean changed) {
    }
}
