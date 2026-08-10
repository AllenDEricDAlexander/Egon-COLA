package top.egon.cola.platform.idp.admin.resource.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.idp.admin.oauth.domain.pojo.IdentityClientEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityClientResourceGrantEntity;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 将 IdP Resource Server 权威数据投影到 Redis 运行态索引。
 *
 * <p>Projects authoritative IdP Resource Server data into Redis runtime indexes.</p>
 *
 * <p>每次管理变更使用一个 Redis 原子写批次；序列化或 Redis 写入失败会向上抛出，使外层
 * 数据库事务回滚，不返回陈旧 ACTIVE 状态。</p>
 *
 * <p>Each management mutation uses one atomic Redis write batch. Serialization or Redis failures
 * propagate so the surrounding database transaction rolls back and never returns a stale ACTIVE
 * state.</p>
 */
public class ResourceServerProjectionService {

    /** Resource 主索引前缀；Resource primary-index prefix. */
    private static final String RESOURCE_PREFIX = "identity:resource-server:";

    /** Resource URI 反向索引前缀；Resource URI reverse-index prefix. */
    private static final String URI_PREFIX = "identity:resource-uri:";

    /** 业务域、应用、环境反向索引前缀；business/application/environment reverse-index prefix. */
    private static final String SCOPE_PREFIX = "identity:resource-scope:";

    /** OAuth Client 运行态索引前缀；OAuth Client runtime-index prefix. */
    private static final String CLIENT_PREFIX = "identity:oauth-client:";

    /** Service Grant 运行态索引前缀；service-grant runtime-index prefix. */
    private static final String SERVICE_GRANT_PREFIX =
            "identity:service-resource-grant:";

    /** Redis 客户端；Redis client. */
    private final RedissonClient redisson;

    /** JSON 编解码器；JSON codec. */
    private final ObjectMapper objectMapper;

    /**
     * 创建运行态投影服务。
     *
     * <p>Creates the runtime projection service.</p>
     *
     * @param redisson Redis 客户端；Redis client
     * @param objectMapper JSON 编解码器；JSON codec
     */
    public ResourceServerProjectionService(
            RedissonClient redisson,
            ObjectMapper objectMapper
    ) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
    }

    /**
     * 原子写入 Resource、URI、三元组和绑定 Client 四个索引。
     *
     * <p>Atomically writes the Resource, URI, triple, and bound-Client indexes.</p>
     *
     * @param resource Resource Server；Resource Server
     * @param client 绑定管理 Client；bound management Client
     */
    public void projectResource(
            IdentityResourceServerEntity resource,
            IdentityClientEntity client
    ) {
        projectResources(List.of(new ResourceProjection(resource, client)));
    }

    /**
     * 在一个原子批次中写入全部 Resource 及其绑定 Client 索引。
     *
     * <p>Writes every Resource and bound-Client index in one atomic batch.</p>
     *
     * @param projections 待写入投影；projections to write
     */
    public void projectResources(List<ResourceProjection> projections) {
        Objects.requireNonNull(projections, "projections");
        if (projections.isEmpty()) {
            throw new IllegalArgumentException("projections must not be empty");
        }
        RBatch batch = batch();
        projections.forEach(projection -> queueResource(batch, projection));
        batch.execute();
    }

    /**
     * 将一个 Resource 投影的全部索引加入既有批次。
     *
     * <p>Queues all indexes for one Resource projection in an existing batch.</p>
     *
     * @param batch Redis 批次；Redis batch
     * @param projection Resource 与 Client 对；Resource and Client pair
     */
    private void queueResource(RBatch batch, ResourceProjection projection) {
        Objects.requireNonNull(projection, "projection");
        IdentityResourceServerEntity resource = projection.resource();
        IdentityClientEntity client = projection.client();
        set(batch, RESOURCE_PREFIX + resource.getResourceServerId(), json(
                resourcePayload(resource)
        ));
        set(batch, URI_PREFIX + sha256(resource.getResourceUri()),
                resource.getResourceServerId());
        set(batch, SCOPE_PREFIX + sha256(scope(resource)),
                resource.getResourceServerId());
        set(batch, CLIENT_PREFIX + client.getClientId(), json(Map.of(
                "clientId", client.getClientId(),
                "clientType", client.getClientType().name(),
                "status", client.getStatus().name(),
                "boundSourceResourceServerId",
                resource.getResourceServerId(),
                "version", client.getVersion()
        )));
    }

    /**
     * 原子写入一个服务访问授权。
     *
     * <p>Atomically writes one service-access grant.</p>
     *
     * @param grant CLIENT_CREDENTIALS Grant；CLIENT_CREDENTIALS Grant
     */
    public void projectServiceGrant(
            IdentityClientResourceGrantEntity grant
    ) {
        projectServiceGrants(List.of(grant));
    }

    /**
     * 在一个原子批次中写入全部服务访问授权。
     *
     * <p>Writes all service-access grants in one atomic batch.</p>
     *
     * @param grants CLIENT_CREDENTIALS Grant 集合；CLIENT_CREDENTIALS grants
     */
    public void projectServiceGrants(
            List<IdentityClientResourceGrantEntity> grants
    ) {
        requireServiceGrants(grants);
        RBatch batch = batch();
        grants.forEach(grant -> set(
                batch,
                serviceGrantKey(grant),
                serviceGrantPayload(grant)
        ));
        batch.execute();
    }

    /**
     * 原子删除一个服务访问授权投影。
     *
     * <p>Atomically deletes one service-access grant projection.</p>
     *
     * @param grant CLIENT_CREDENTIALS Grant；CLIENT_CREDENTIALS Grant
     */
    public void deleteServiceGrant(
            IdentityClientResourceGrantEntity grant
    ) {
        deleteServiceGrants(List.of(grant));
    }

    /**
     * 在一个原子批次中删除全部服务访问授权投影。
     *
     * <p>Deletes all service-access grant projections in one atomic batch.</p>
     *
     * @param grants CLIENT_CREDENTIALS Grant 集合；CLIENT_CREDENTIALS grants
     */
    public void deleteServiceGrants(
            List<IdentityClientResourceGrantEntity> grants
    ) {
        requireServiceGrants(grants);
        RBatch batch = batch();
        grants.forEach(grant -> batch.getBucket(
                        serviceGrantKey(grant),
                        StringCodec.INSTANCE
                ).deleteAsync());
        batch.execute();
    }

    /**
     * 构造服务授权投影 JSON。
     *
     * <p>Builds service-grant projection JSON.</p>
     *
     * @param grant 服务授权；service grant
     * @return JSON 文本；JSON text
     */
    private String serviceGrantPayload(
            IdentityClientResourceGrantEntity grant
    ) {
        return json(Map.of(
                "clientId", grant.getClientId(),
                "resourceServerId", grant.getResourceServerId(),
                "tenantId", grant.getTenantId(),
                "allowedScopes", jsonTree(grant.getAllowedScopes()),
                "status", grant.getStatus().name(),
                "version", grant.getVersion()
        ));
    }

    /**
     * 创建 Redis 原子写批次。
     *
     * <p>Creates an atomic Redis write batch.</p>
     *
     * @return 原子批次；atomic batch
     */
    private RBatch batch() {
        return redisson.createBatch(BatchOptions.defaults().executionMode(
                BatchOptions.ExecutionMode.REDIS_WRITE_ATOMIC
        ));
    }

    /**
     * 将字符串写入批次。
     *
     * <p>Queues a String write in the batch.</p>
     *
     * @param batch Redis 批次；Redis batch
     * @param key 键；key
     * @param value 值；value
     */
    private void set(RBatch batch, String key, String value) {
        batch.<String>getBucket(key, StringCodec.INSTANCE).setAsync(value);
    }

    /**
     * 构造 Resource 投影内容。
     *
     * <p>Builds the Resource projection payload.</p>
     *
     * @param resource Resource Server；Resource Server
     * @return 有序字段映射；ordered field map
     */
    private Map<String, Object> resourcePayload(
            IdentityResourceServerEntity resource
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resourceServerId", resource.getResourceServerId());
        payload.put("resourceUri", resource.getResourceUri());
        payload.put("bizCode", resource.getBizCode());
        payload.put("appCode", resource.getAppCode());
        payload.put("environment", resource.getEnvironment());
        payload.put("status", resource.getStatus().name());
        payload.put("version", resource.getVersion());
        payload.put("managementClientId", resource.getManagementClientId());
        payload.put("rbacApplicationCode",
                resource.getRbacApplicationCode());
        payload.put("entryPermissionCode",
                resource.getEntryPermissionCode());
        payload.put("admissionTicketTtlSeconds",
                resource.getAdmissionTicketTtlSeconds());
        return payload;
    }

    /**
     * 构造三元组索引原文。
     *
     * <p>Builds the source text for the triple index.</p>
     *
     * @param resource Resource Server；Resource Server
     * @return biz:app:env；biz:app:env
     */
    private String scope(IdentityResourceServerEntity resource) {
        return resource.getBizCode() + ":" + resource.getAppCode()
                + ":" + resource.getEnvironment();
    }

    /**
     * 构造 Service Grant 键。
     *
     * <p>Builds a service-grant key.</p>
     *
     * @param grant 服务授权；service grant
     * @return Redis 键；Redis key
     */
    private String serviceGrantKey(
            IdentityClientResourceGrantEntity grant
    ) {
        return SERVICE_GRANT_PREFIX + grant.getClientId() + ":"
                + grant.getResourceServerId() + ":" + grant.getTenantId();
    }

    /**
     * 校验传入授权属于服务链路。
     *
     * <p>Checks that the supplied grant belongs to the service flow.</p>
     *
     * @param grant 待校验授权；grant to validate
     */
    private void requireServiceGrant(
            IdentityClientResourceGrantEntity grant
    ) {
        Objects.requireNonNull(grant, "grant");
        if (grant.getGrantType()
                != IdentityClientResourceGrantEntity.GrantType
                .CLIENT_CREDENTIALS) {
            throw new IllegalArgumentException(
                    "only CLIENT_CREDENTIALS grants are projected"
            );
        }
    }

    /**
     * 校验非空服务授权批次。
     *
     * <p>Validates a non-empty batch of service grants.</p>
     *
     * @param grants 服务授权；service grants
     */
    private void requireServiceGrants(
            List<IdentityClientResourceGrantEntity> grants
    ) {
        Objects.requireNonNull(grants, "grants");
        if (grants.isEmpty()) {
            throw new IllegalArgumentException("grants must not be empty");
        }
        grants.forEach(this::requireServiceGrant);
    }

    /**
     * 序列化 JSON。
     *
     * <p>Serializes JSON.</p>
     *
     * @param value 待序列化值；value to serialize
     * @return JSON 文本；JSON text
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Resource projection serialization failed",
                    exception
            );
        }
    }

    /**
     * 将已验证的 JSON 文本读取为树节点。
     *
     * <p>Reads validated JSON text as a tree node.</p>
     *
     * @param value JSON 文本；JSON text
     * @return JSON 树；JSON tree
     */
    private Object jsonTree(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Grant scope JSON is invalid",
                    exception
            );
        }
    }

    /**
     * 计算小写十六进制 SHA-256。
     *
     * <p>Computes a lowercase hexadecimal SHA-256 digest.</p>
     *
     * @param value 原文；source text
     * @return 摘要；digest
     */
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }

    /**
     * 一个 Resource Server 与其绑定管理 Client 的运行态投影输入。
     *
     * <p>Runtime projection input pairing one Resource Server with its bound
     * management Client.</p>
     *
     * @param resource Resource Server；Resource Server
     * @param client 绑定管理 Client；bound management Client
     */
    public record ResourceProjection(
            IdentityResourceServerEntity resource,
            IdentityClientEntity client
    ) {
        /**
         * 校验投影输入完整。
         *
         * <p>Validates that the projection input is complete.</p>
         */
        public ResourceProjection {
            Objects.requireNonNull(resource, "resource");
            Objects.requireNonNull(client, "client");
        }
    }
}
