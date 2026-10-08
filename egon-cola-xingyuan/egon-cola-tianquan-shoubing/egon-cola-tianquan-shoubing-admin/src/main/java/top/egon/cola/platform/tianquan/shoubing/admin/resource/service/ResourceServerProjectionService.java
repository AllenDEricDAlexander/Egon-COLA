package top.egon.cola.platform.tianquan.shoubing.admin.resource.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.redisson.api.BatchOptions;
import org.redisson.api.RBatch;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.contract.ServiceTokenContext;

/** Projects committed Resource and service-grant facts into the Redis runtime indexes. */
@Slf4j
@RequiredArgsConstructor
public class ResourceServerProjectionService {

    private static final String RESOURCE_PREFIX = "identity:resource-server:";
    private static final String URI_PREFIX = "identity:resource-uri:";
    private static final String SCOPE_PREFIX = "identity:resource-scope:";
    private static final String CLIENT_PREFIX = "identity:oauth-client:";
    private static final String SERVICE_GRANT_PREFIX = "identity:service-resource-grant:";

    @NonNull
    @Qualifier("rbac3RuntimeRedissonClient")
    private final RedissonClient redisson;
    @NonNull
    @Qualifier("jacksonObjectMapper")
    private final ObjectMapper objectMapper;

    public void projectResource(OAuthResourceBO resource, OAuthClientBO managementClient) {
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(managementClient, "managementClient");
        RBatch batch = batch();
        queueResource(batch, resource, managementClient);
        batch.execute();
    }

    public void projectServiceGrant(
            TrustedRealmBO clientRealm,
            OAuthClientBO client,
            OAuthResourceBO resource,
            OAuthGrantBO grant) {
        requireServiceGrant(clientRealm, client, resource, grant);
        RBatch batch = batch();
        set(batch, serviceGrantKey(clientRealm, client, resource),
                serviceGrantPayload(clientRealm, client, resource, grant));
        batch.execute();
    }

    public void deleteServiceGrant(
            TrustedRealmBO clientRealm,
            OAuthClientBO client,
            OAuthResourceBO resource,
            OAuthGrantBO grant) {
        requireServiceGrant(clientRealm, client, resource, grant);
        RBatch batch = batch();
        batch.getBucket(serviceGrantKey(clientRealm, client, resource), StringCodec.INSTANCE).deleteAsync();
        batch.execute();
    }

    private void queueResource(RBatch batch, OAuthResourceBO resource, OAuthClientBO client) {
        set(batch, RESOURCE_PREFIX + resource.getResourceServerId(), json(resourcePayload(resource)));
        set(batch, URI_PREFIX + sha256(resource.getResourceUri().toASCIIString()), resource.getResourceServerId());
        set(batch, SCOPE_PREFIX + sha256(scope(resource)), resource.getResourceServerId());
        set(batch, CLIENT_PREFIX + client.getClientId(), json(Map.of(
                "clientId", client.getClientId(),
                "clientType", client.getClientType().name(),
                "status", client.getStatus().name(),
                "boundSourceResourceServerId", resource.getResourceServerId(),
                "version", client.getVersion())));
    }

    private void requireServiceGrant(
            TrustedRealmBO clientRealm,
            OAuthClientBO client,
            OAuthResourceBO resource,
            OAuthGrantBO grant) {
        Objects.requireNonNull(clientRealm, "clientRealm").validateTrustedPartition();
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(grant, "grant");
        if (grant.getGrantType() != ResourceGrantType.CLIENT_CREDENTIALS
                || client.getPurpose() != OAuthClientPurposeEnum.SERVICE) {
            throw new IllegalArgumentException("ONLY_SERVICE_GRANTS_ARE_PROJECTED");
        }
    }

    private String serviceGrantKey(TrustedRealmBO clientRealm, OAuthClientBO client, OAuthResourceBO resource) {
        String contextKey = clientRealm.getBusinessTenantId() == null
                ? "platform" : clientRealm.getBusinessTenantId().toString();
        return SERVICE_GRANT_PREFIX + client.getClientId() + ":"
                + resource.getResourceServerId() + ":" + contextKey;
    }

    private String serviceGrantPayload(
            TrustedRealmBO clientRealm,
            OAuthClientBO client,
            OAuthResourceBO resource,
            OAuthGrantBO grant) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("clientId", client.getClientId());
        payload.put("resourceServerId", grant.getResourceServerId());
        payload.put("grantContext", clientRealm.getBusinessTenantId() == null
                ? ServiceTokenContext.PLATFORM.name() : ServiceTokenContext.TENANT.name());
        payload.put("tenantId", clientRealm.getBusinessTenantId() == null
                ? null : clientRealm.getBusinessTenantId().toString());
        payload.put("allowedScopes", grant.getAllowedScopes());
        payload.put("status", grant.getStatus().name());
        payload.put("version", grant.getVersion());
        return json(payload);
    }

    private Map<String, Object> resourcePayload(OAuthResourceBO resource) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resourceServerId", resource.getResourceServerId());
        payload.put("resourceUri", resource.getResourceUri().toASCIIString());
        payload.put("bizCode", resource.getBizCode());
        payload.put("appCode", resource.getAppCode());
        payload.put("environment", resource.getEnvironment());
        payload.put("status", resource.getStatus().name());
        payload.put("version", resource.getVersion());
        payload.put("managementClientId", resource.getManagementClientId());
        payload.put("rbacApplicationCode", resource.getRbacApplicationCode());
        payload.put("entryPermissionCode", resource.getEntryPermissionCode());
        return payload;
    }

    private static String scope(OAuthResourceBO resource) {
        return resource.getBizCode() + ":" + resource.getAppCode() + ":" + resource.getEnvironment();
    }

    private RBatch batch() {
        return redisson.createBatch(BatchOptions.defaults().executionMode(BatchOptions.ExecutionMode.REDIS_WRITE_ATOMIC));
    }

    private void set(RBatch batch, String key, String value) {
        batch.<String>getBucket(key, StringCodec.INSTANCE).setAsync(value);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("RESOURCE_PROJECTION_SERIALIZATION_FAILED", failure);
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA_256_UNAVAILABLE", failure);
        }
    }

}
