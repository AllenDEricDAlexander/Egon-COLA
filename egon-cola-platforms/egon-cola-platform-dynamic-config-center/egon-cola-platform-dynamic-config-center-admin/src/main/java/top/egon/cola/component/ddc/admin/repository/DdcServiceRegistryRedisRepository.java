package top.egon.cola.component.ddc.admin.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.core.io.ClassPathResource;
import top.egon.cola.component.ddc.admin.common.DdcAdminException;
import top.egon.cola.component.ddc.common.DdcErrorStatus;
import top.egon.cola.component.ddc.common.DdcKeys;
import top.egon.cola.component.ddc.model.dto.DdcServiceLeaseRequest;
import top.egon.cola.component.ddc.model.enums.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.registry.DdcServiceCatalogSnapshot;
import top.egon.cola.component.ddc.model.registry.DdcServiceInstance;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceQuery;
import top.egon.cola.component.ddc.model.registry.DdcServiceSnapshot;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DdcServiceRegistryRedisRepository {

    private static final String REGISTER_SCRIPT = script("redis/ddc_service_register.lua");

    private static final String HEARTBEAT_SCRIPT = script("redis/ddc_service_heartbeat.lua");

    private static final String DEREGISTER_SCRIPT = script("redis/ddc_service_deregister.lua");

    private static final String EXPIRE_SCRIPT = script("redis/ddc_service_expire.lua");

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
        List<?> result = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                REGISTER_SCRIPT,
                RScript.ReturnType.MULTI,
                keys(serviceKey, instance.instanceId()),
                instance.instanceId(),
                instance.leaseId(),
                serviceKey.canonicalValue(),
                instanceJson(instance),
                instance.leaseSeconds(),
                instance.leaseExpireAt().toEpochMilli(),
                json(serviceKey),
                legacyTopic(serviceKey)
        );
        int code = number(result, 0).intValue();
        if (code == 2) {
            throw new DdcAdminException(DdcErrorStatus.INSTANCE_ID_CONFLICT);
        }
        if (code != 1) {
            throw new IllegalStateException("invalid DDC service registration status: " + code);
        }
        knownServiceKeys.add(serviceKey);
    }

    public DdcLeaseOperationResult heartbeat(DdcServiceLeaseRequest request,
                                             Instant heartbeatAt) {
        DdcServiceKey serviceKey = request.getServiceKey();
        List<?> result = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                HEARTBEAT_SCRIPT,
                RScript.ReturnType.MULTI,
                List.of(
                        DdcKeys.v2RegistryInstance(serviceKey, request.getInstanceId()),
                        DdcKeys.v2RegistryService(serviceKey)
                ),
                request.getInstanceId(),
                request.getLeaseId(),
                serviceKey.canonicalValue(),
                heartbeatAt.toEpochMilli()
        );
        int code = number(result, 0).intValue();
        return switch (code) {
            case 1 -> new DdcLeaseOperationResult(
                    DdcLeaseOperationStatus.RENEWED,
                    Instant.ofEpochMilli(number(result, 1).longValue())
            );
            case 2 -> new DdcLeaseOperationResult(DdcLeaseOperationStatus.NOT_FOUND, null);
            case 3 -> new DdcLeaseOperationResult(DdcLeaseOperationStatus.LEASE_MISMATCH, null);
            default -> throw new IllegalStateException(
                    "invalid DDC service heartbeat status: " + code
            );
        };
    }

    public DdcLeaseOperationResult deregister(DdcServiceLeaseRequest request,
                                              Instant deregisteredAt) {
        DdcServiceKey serviceKey = request.getServiceKey();
        List<?> result = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                DEREGISTER_SCRIPT,
                RScript.ReturnType.MULTI,
                keys(serviceKey, request.getInstanceId()),
                request.getInstanceId(),
                request.getLeaseId(),
                serviceKey.canonicalValue(),
                json(serviceKey),
                deregisteredAt.toEpochMilli(),
                legacyTopic(serviceKey)
        );
        int code = number(result, 0).intValue();
        return switch (code) {
            case 1 -> new DdcLeaseOperationResult(DdcLeaseOperationStatus.DELETED, null);
            case 2 -> new DdcLeaseOperationResult(DdcLeaseOperationStatus.NOT_FOUND, null);
            case 3 -> new DdcLeaseOperationResult(DdcLeaseOperationStatus.NOT_DELETED, null);
            default -> throw new IllegalStateException(
                    "invalid DDC service deregistration status: " + code
            );
        };
    }

    public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey, Instant now) {
        knownServiceKeys.add(serviceKey);
        Collection<String> expired = redissonClient.<String>getScoredSortedSet(
                        DdcKeys.v2RegistryService(serviceKey), StringCodec.INSTANCE)
                .valueRange(Double.NEGATIVE_INFINITY, true, now.toEpochMilli(), true);
        expired.forEach(instanceId -> expire(serviceKey, instanceId, now));

        Collection<String> members = redissonClient.<String>getScoredSortedSet(
                        DdcKeys.v2RegistryService(serviceKey), StringCodec.INSTANCE)
                .valueRange(now.toEpochMilli(), false, Double.POSITIVE_INFINITY, true);
        List<DdcServiceInstance> instances = new ArrayList<>();
        for (String instanceId : members) {
            String value = redissonClient.<String>getBucket(
                    DdcKeys.v2RegistryInstance(serviceKey, instanceId), StringCodec.INSTANCE
            ).get();
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
        long revision =
                redissonClient.getAtomicLong(DdcKeys.v2RegistryRevision(serviceKey)).get();
        return new DdcServiceSnapshot(serviceKey, revision, instances, now);
    }

    public DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query,
                                                    Instant now) {
        Set<String> members = redissonClient.<String>getSet(
                DdcKeys.v2RegistryCatalog(
                        query.bizCode(),
                        query.appCode(),
                        query.env(),
                        query.namespace(),
                        query.serviceKind(),
                        query.protocol()
                ),
                StringCodec.INSTANCE
        ).readAll();
        List<DdcServiceKey> serviceKeys = new ArrayList<>();
        for (String member : members) {
            DdcServiceKey serviceKey = DdcServiceKey.parse(member);
            if (!query.matches(serviceKey)) {
                continue;
            }
            if (getInstances(serviceKey, now).instances().isEmpty()) {
                continue;
            }
            serviceKeys.add(serviceKey);
        }
        long revision = redissonClient.getAtomicLong(DdcKeys.v2RegistryCatalogRevision(
                query.bizCode(),
                query.appCode(),
                query.env(),
                query.namespace(),
                query.serviceKind(),
                query.protocol()
        )).get();
        return new DdcServiceCatalogSnapshot(query, revision, serviceKeys, now);
    }

    public int expireKnown(Instant now) {
        int removed = 0;
        for (DdcServiceKey serviceKey : List.copyOf(knownServiceKeys)) {
            int before = redissonClient.<String>getScoredSortedSet(
                    DdcKeys.v2RegistryService(serviceKey), StringCodec.INSTANCE
            ).size();
            getInstances(serviceKey, now);
            int after = redissonClient.<String>getScoredSortedSet(
                    DdcKeys.v2RegistryService(serviceKey), StringCodec.INSTANCE
            ).size();
            removed += Math.max(0, before - after);
            if (after == 0) {
                knownServiceKeys.remove(serviceKey);
            }
        }
        return removed;
    }

    private void expire(DdcServiceKey serviceKey, String instanceId, Instant now) {
        redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                EXPIRE_SCRIPT,
                RScript.ReturnType.MULTI,
                keys(serviceKey, instanceId),
                instanceId,
                serviceKey.canonicalValue(),
                now.toEpochMilli(),
                json(serviceKey),
                legacyTopic(serviceKey)
        );
    }

    private List<Object> keys(DdcServiceKey serviceKey, String instanceId) {
        return List.of(
                DdcKeys.v2RegistryInstance(serviceKey, instanceId),
                DdcKeys.v2RegistryService(serviceKey),
                DdcKeys.v2RegistryRevision(serviceKey),
                DdcKeys.v2RegistryCatalog(
                        serviceKey.bizCode(),
                        serviceKey.appCode(),
                        serviceKey.env(),
                        serviceKey.namespace(),
                        serviceKey.serviceKind(),
                        serviceKey.protocol()
                ),
                DdcKeys.v2RegistryCatalogRevision(
                        serviceKey.bizCode(),
                        serviceKey.appCode(),
                        serviceKey.env(),
                        serviceKey.namespace(),
                        serviceKey.serviceKind(),
                        serviceKey.protocol()
                ),
                DdcKeys.v2RegistryTopic(
                        serviceKey.bizCode(),
                        serviceKey.appCode(),
                        serviceKey.env(),
                        serviceKey.namespace(),
                        serviceKey.serviceKind(),
                        serviceKey.protocol()
                )
        );
    }

    private String legacyTopic(DdcServiceKey serviceKey) {
        return DdcKeys.registryTopic(
                serviceKey.bizCode(),
                serviceKey.appCode(),
                serviceKey.env(),
                serviceKey.namespace(),
                serviceKey.serviceKind(),
                serviceKey.protocol()
        );
    }

    private String instanceJson(DdcServiceInstance instance) {
        ObjectNode value = objectMapper.valueToTree(instance);
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
                    objectNode.put(
                            "lastHeartbeatAt",
                            Instant.ofEpochMilli(lastHeartbeatAt.longValue()).toString()
                    );
                }
                if (leaseExpireAt != null && leaseExpireAt.canConvertToLong()) {
                    objectNode.put(
                            "leaseExpireAt",
                            Instant.ofEpochMilli(leaseExpireAt.longValue()).toString()
                    );
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

    private Number number(List<?> result, int index) {
        if (result == null
                || result.size() <= index
                || !(result.get(index) instanceof Number number)) {
            throw new IllegalStateException("invalid DDC service registry script result");
        }
        return number;
    }

    private static String script(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("load DDC service registry script failed: " + path, exception);
        }
    }
}
