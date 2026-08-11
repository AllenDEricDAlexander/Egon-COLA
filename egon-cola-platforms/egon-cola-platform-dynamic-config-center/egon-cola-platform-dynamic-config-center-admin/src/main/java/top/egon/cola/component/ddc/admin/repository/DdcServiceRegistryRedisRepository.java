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
import top.egon.cola.component.ddc.admin.security.admission.DdcAdmissionClaims;
import top.egon.cola.component.ddc.admin.security.admission.DdcAdmissionException;
import top.egon.cola.component.ddc.error.DdcErrorStatus;
import top.egon.cola.component.ddc.redis.DdcRedisKeys;
import top.egon.cola.component.ddc.model.registry.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.registry.DdcRegistryEvent;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;

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
                    ttl(instance.lastHeartbeatAt(), instance.leaseExpireAt())
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

    public DdcLeaseOperationResult heartbeat(
            DdcServiceLeaseRequest request,
            DdcAdmissionClaims admission,
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
            if (!admission.resourceServerId().equals(instance.resourceServerId())) {
                throw new DdcAdmissionException(
                        DdcErrorStatus.RESOURCE_ADMISSION_BINDING_MISMATCH
                );
            }
            Instant requestedExpireAt = heartbeatAt.plusSeconds(
                    instance.leaseSeconds()
            );
            Instant leaseExpireAt = requestedExpireAt.isBefore(admission.expiresAt())
                    ? requestedExpireAt : admission.expiresAt();
            DdcServiceInstance renewed = new DdcServiceInstance(
                    instance.instanceId(), instance.leaseId(), instance.serviceKey(),
                    instance.host(), instance.port(), instance.secure(), instance.metadata(),
                    instance.leaseSeconds(), instance.heartbeatIntervalSeconds(),
                    instance.registeredAt(), heartbeatAt, leaseExpireAt,
                    instance.status(), instance.revision(),
                    admission.resourceServerId(), admission.resourceVersion(),
                    admission.credentialId(), admission.expiresAt()
            );
            instanceBucket(serviceKey, request.getInstanceId()).set(
                    instanceJson(renewed, renewed.revision()),
                    ttl(heartbeatAt, leaseExpireAt)
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

    /**
     * 撤销精确 Resource Server 三元组下不晚于停用事件版本的 Provider 租约。
     * / Revokes provider leases for the exact Resource Server triple whose version is not newer
     * than the disable event.
     *
     * @param resourceServerId Resource Server 稳定标识 / stable Resource Server identifier
     * @param bizCode 业务域编码 / business-domain code
     * @param env 部署环境 / deployment environment
     * @param appCode 应用编码 / application code
     * @param resourceVersion 停用事件版本 / disable-event version
     * @return 实际撤销的 Provider 租约数 / provider leases actually revoked
     */
    public int revokeResourceAdmission(
            String resourceServerId,
            String bizCode,
            String env,
            String appCode,
            long resourceVersion) {
        int removed = 0;
        Set<String> catalog = redissonClient.<String>getSet(
                DdcRedisKeys.globalRegistryCatalog(),
                StringCodec.INSTANCE
        ).readAll();
        for (String member : catalog) {
            DdcServiceKey serviceKey = DdcServiceKey.parse(member);
            if (bizCode.equals(serviceKey.bizCode())
                    && env.equals(serviceKey.env())
                    && appCode.equals(serviceKey.appCode())) {
                removed += revokeServiceAdmission(
                        serviceKey,
                        resourceServerId,
                        resourceVersion
                );
            }
        }
        return removed;
    }

    /**
     * 在一个服务键锁内撤销匹配实例并发布现有目录变更通知。
     * / Revokes matching instances under one service-key lock and publishes the existing catalog
     * change notification.
     *
     * @param serviceKey 服务键 / service key
     * @param resourceServerId Resource Server 标识 / Resource Server identifier
     * @param resourceVersion 停用事件版本 / disable-event version
     * @return 实际撤销数 / number actually revoked
     */
    private int revokeServiceAdmission(
            DdcServiceKey serviceKey,
            String resourceServerId,
            long resourceVersion) {
        RLock lock = serviceLock(serviceKey);
        lock.lock();
        try {
            int removed = 0;
            Collection<String> instanceIds = serviceInstances(serviceKey)
                    .valueRange(
                            Double.NEGATIVE_INFINITY,
                            true,
                            Double.POSITIVE_INFINITY,
                            true
                    );
            for (String instanceId : List.copyOf(instanceIds)) {
                String current = instanceBucket(
                        serviceKey, instanceId
                ).get();
                if (current == null) {
                    serviceInstances(serviceKey).remove(instanceId);
                    continue;
                }
                DdcServiceInstance instance = instance(current);
                Long leaseVersion = instance.resourceVersion();
                if (!resourceServerId.equals(instance.resourceServerId())
                        || leaseVersion == null
                        || leaseVersion > resourceVersion) {
                    continue;
                }
                instanceBucket(serviceKey, instanceId).delete();
                serviceInstances(serviceKey).remove(instanceId);
                removed++;
            }
            if (removed > 0) {
                long serviceRevision = serviceRevision(
                        serviceKey
                ).incrementAndGet();
                CatalogUpdate catalog = removeServiceCatalogIfEmpty(
                        serviceKey
                );
                publishEvent(
                        serviceKey,
                        serviceRevision,
                        catalog.revision()
                );
                removeGlobalCatalogIfEmpty(serviceKey);
            }
            return removed;
        } finally {
            lock.unlock();
        }
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
                    || !instance.leaseExpireAt().isAfter(now)
                    || !hasActiveAdmission(instance, now)) {
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
                ? DdcRedisKeys.registryCatalog(
                        query.bizCode(), query.env(), query.appCode(),
                        query.serviceKind(), query.protocol())
                : DdcRedisKeys.globalRegistryCatalog();
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
                ? DdcRedisKeys.registryCatalogRevision(
                        query.bizCode(), query.env(), query.appCode(),
                        query.serviceKind(), query.protocol())
                : DdcRedisKeys.globalRegistryCatalogRevision();
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

    private boolean hasActiveAdmission(
            DdcServiceInstance instance,
            Instant now
    ) {
        return instance.resourceServerId() != null
                && !instance.resourceServerId().isBlank()
                && instance.resourceVersion() != null
                && instance.resourceVersion() > 0
                && instance.credentialId() != null
                && !instance.credentialId().isBlank()
                && instance.admissionExpiresAt() != null
                && instance.admissionExpiresAt().isAfter(now);
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
                    DdcRedisKeys.globalRegistryCatalog(), StringCodec.INSTANCE
            ).add(serviceKey.canonicalValue());
            if (added) {
                redissonClient.getAtomicLong(
                        DdcRedisKeys.globalRegistryCatalogRevision()).incrementAndGet();
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
                    DdcRedisKeys.globalRegistryCatalog(), StringCodec.INSTANCE
            ).remove(serviceKey.canonicalValue());
            if (removed) {
                redissonClient.getAtomicLong(
                        DdcRedisKeys.globalRegistryCatalogRevision()).incrementAndGet();
            }
        } finally {
            lock.unlock();
        }
    }

    private RBucket<String> instanceBucket(DdcServiceKey serviceKey, String instanceId) {
        return redissonClient.getBucket(
                DdcRedisKeys.registryInstance(serviceKey, instanceId), StringCodec.INSTANCE);
    }

    private RScoredSortedSet<String> serviceInstances(DdcServiceKey serviceKey) {
        return redissonClient.getScoredSortedSet(
                DdcRedisKeys.registryService(serviceKey), StringCodec.INSTANCE);
    }

    private RSet<String> serviceCatalog(DdcServiceKey serviceKey) {
        return redissonClient.getSet(
                DdcRedisKeys.registryCatalog(
                        serviceKey.bizCode(), serviceKey.env(), serviceKey.appCode(),
                        serviceKey.serviceKind(), serviceKey.protocol()),
                StringCodec.INSTANCE
        );
    }

    private RAtomicLong serviceRevision(DdcServiceKey serviceKey) {
        return redissonClient.getAtomicLong(DdcRedisKeys.registryRevision(serviceKey));
    }

    private RAtomicLong catalogRevision(DdcServiceKey serviceKey) {
        return redissonClient.getAtomicLong(DdcRedisKeys.registryCatalogRevision(
                serviceKey.bizCode(), serviceKey.env(), serviceKey.appCode(),
                serviceKey.serviceKind(), serviceKey.protocol()));
    }

    private RLock serviceLock(DdcServiceKey serviceKey) {
        return redissonClient.getLock(
                DdcRedisKeys.registryInstance(serviceKey, "scope") + ":lock");
    }

    private RLock globalCatalogLock() {
        return redissonClient.getLock(DdcRedisKeys.globalRegistryCatalog() + ":lock");
    }

    private void publishEvent(DdcServiceKey serviceKey,
                              long serviceRevision,
                              long catalogRevision) {
        redissonClient.getTopic(DdcRedisKeys.registryTopic(
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

    private Duration ttl(Instant now, Instant expiresAt) {
        long seconds = Math.max(1L, Duration.between(now, expiresAt).toSeconds());
        return Duration.ofSeconds(seconds);
    }

    private record CatalogUpdate(long revision, boolean changed) {
    }
}
