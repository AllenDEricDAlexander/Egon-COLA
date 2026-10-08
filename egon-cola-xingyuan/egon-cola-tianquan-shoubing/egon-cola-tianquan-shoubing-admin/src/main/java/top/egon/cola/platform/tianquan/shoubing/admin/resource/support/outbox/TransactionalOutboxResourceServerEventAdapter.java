package top.egon.cola.platform.tianquan.shoubing.admin.resource.support.outbox;

import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.OAuthClientBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.bo.TrustedRealmBO;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.domain.enums.OAuthClientPurposeEnum;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthGrantBO;
import top.egon.cola.platform.tianquan.shoubing.admin.resource.domain.bo.OAuthResourceBO;
import top.egon.cola.platform.tianquan.shoubing.core.oauth.OAuthClient;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceGrantType;
import top.egon.cola.platform.tianquan.shoubing.core.resource.ResourceServerStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * 将 Resource Server 停用事实适配为仓库标准事务发件箱消息。
 * / Adapts Resource Server disable facts to repository-standard transactional-outbox messages.
 *
 * <p>事件仅携带公开的 Resource 标识、精确业务三元组和版本，不携带 JWK、私钥、Secret
 * 或原始 Admission Ticket。</p>
 *
 * <p>The event carries only the public Resource identity, exact business triple, and version. It
 * never carries JWKs, private keys, secrets, or raw Admission Tickets.</p>
 */
@Slf4j
@RequiredArgsConstructor
public final class TransactionalOutboxResourceServerEventAdapter {

    /** Resource 生命周期投递通道；Resource lifecycle delivery channel. */
    public static final String CHANNEL = "identity-resource-runtime";

    /** Resource 停用事件目的地；Resource-disabled event destination. */
    public static final String DISABLED_DESTINATION =
            "identity.resource-server.disabled.v1";

    /** MP已提交 Resource 的Redis投影目的地；committed Resource Redis projection destination. */
    public static final String RESOURCE_PROJECTION_DESTINATION =
            "identity.resource-server.project.v1";

    /** MP已提交 service Grant 的Redis投影目的地；committed service-Grant Redis projection destination. */
    public static final String SERVICE_GRANT_PROJECTION_DESTINATION =
            "identity.service-grant.project.v1";

    /** Redis projection delivery channel. */
    public static final String PROJECTION_CHANNEL = "identity-resource-projection";

    /** 事务发件箱端口；transactional-outbox port. */
    @NonNull
    @Qualifier("transactionalOutbox")
    private final TransactionalOutbox outbox;

    /** 事件时钟；event clock. */
    @NonNull
    @Qualifier("idpClock")
    private final Clock clock;

    /**
     * 将已推进版本的停用 Resource 写入当前数据库事务。
     * / Enqueues the disabled Resource with its advanced version in the current database
     * transaction.
     *
     * @param resource 已停用的 Resource 实体 / disabled Resource entity
     * @return 稳定事件标识 / stable event identifier
     */
    public String enqueueDisabled(OAuthResourceBO resource) {
        Objects.requireNonNull(resource, "resource");
        if (resource.getStatus() != ResourceServerStatus.DISABLED) {
            throw new IllegalArgumentException(
                    "Resource Server must be disabled before enqueue"
            );
        }
        String idempotencyKey = resource.getResourceServerId()
                + ":disabled:" + resource.getVersion();
        String eventId = sha256(idempotencyKey);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resourceServerId", resource.getResourceServerId());
        payload.put("bizCode", resource.getBizCode());
        payload.put("appCode", resource.getAppCode());
        payload.put("env", resource.getEnvironment());
        payload.put("resourceVersion", resource.getVersion());

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", DISABLED_DESTINATION);
        envelope.put("schemaVersion", 1);
        envelope.put("occurredAt", clock.instant());
        envelope.put("aggregateType", "IDENTITY_RESOURCE_SERVER");
        envelope.put("aggregateId", resource.getResourceServerId());
        envelope.put("aggregateVersion", resource.getVersion());
        envelope.put("payload", payload);

        return outbox.enqueue(OutboxMessage.builder()
                        .messageId(eventId)
                        .idempotencyKey(idempotencyKey)
                        .channel(CHANNEL)
                        .destination(DISABLED_DESTINATION)
                        .payload(envelope)
                        .schemaVersion("1")
                        .build())
                .messageId();
    }

    /**
     * 将Resource投影意图与已提交的Resource事实绑定在同一事务Outbox中。
     * / Enqueues a Resource projection intent atomically with its MP fact.
     *
     * @param resource 已写入当前MP事务的Resource / Resource written in the current MP transaction
     * @return 稳定事件标识 / stable event identifier
     */
    public String enqueueResourceProjection(OAuthResourceBO resource) {
        Objects.requireNonNull(resource, "resource");
        requireVersion(resource.getVersion());
        String aggregateId = required(resource.getResourceServerId(), "resourceServerId");
        String idempotencyKey = aggregateId + ":projection:" + resource.getVersion();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resourceServerId", aggregateId);
        payload.put("resourceVersion", resource.getVersion());
        return enqueue(PROJECTION_CHANNEL, RESOURCE_PROJECTION_DESTINATION, idempotencyKey,
                "IDENTITY_RESOURCE_SERVER", aggregateId, resource.getVersion(), payload);
    }

    /**
     * 将同Realm service Grant投影意图与Grant事实绑定在同一事务Outbox中。
     * / Enqueues a same-realm service Grant projection intent atomically with its fact.
     *
     * @param clientRealm 经验证的Client realm / verified Client realm
     * @param client Grant所属Client / grant-owning Client
     * @param grant 已写入当前MP事务的Grant / Grant written in the current MP transaction
     * @return 稳定事件标识 / stable event identifier
     */
    public String enqueueServiceGrantProjection(
            TrustedRealmBO clientRealm,
            OAuthClientBO client,
            OAuthGrantBO grant) {
        TrustedRealmBO realm = Objects.requireNonNull(clientRealm, "clientRealm");
        realm.validateTrustedPartition();
        OAuthClientBO targetClient = Objects.requireNonNull(client, "client");
        OAuthGrantBO targetGrant = Objects.requireNonNull(grant, "grant");
        if (targetGrant.getGrantType() != ResourceGrantType.CLIENT_CREDENTIALS
                || targetClient.getPurpose() != OAuthClientPurposeEnum.SERVICE
                || targetClient.getClientType() != OAuthClient.ClientType.CONFIDENTIAL
                || targetGrant.getClientPk() == null || targetGrant.getClientPk() <= 0L
                || targetClient.getId() == null || !targetClient.getId().equals(targetGrant.getClientPk())) {
            throw new IllegalArgumentException("OAUTH_SERVICE_GRANT_PROJECTION_INVALID");
        }
        requireVersion(targetGrant.getVersion());
        String resourceId = required(targetGrant.getResourceServerId(), "resourceServerId");
        String clientId = required(targetClient.getClientId(), "clientId");
        String aggregateId = realm.getRealmKey() + ":" + clientId + ":" + resourceId + ":CLIENT_CREDENTIALS";
        String idempotencyKey = aggregateId + ":projection:" + targetGrant.getVersion();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("realmKey", realm.getRealmKey());
        payload.put("tenantId", realm.getBusinessTenantId() == null
                ? null : realm.getBusinessTenantId().toString());
        payload.put("clientId", clientId);
        payload.put("resourceServerId", resourceId);
        payload.put("grantType", ResourceGrantType.CLIENT_CREDENTIALS.name());
        payload.put("grantVersion", targetGrant.getVersion());
        return enqueue(PROJECTION_CHANNEL, SERVICE_GRANT_PROJECTION_DESTINATION, idempotencyKey,
                "IDENTITY_SERVICE_RESOURCE_GRANT", aggregateId, targetGrant.getVersion(), payload);
    }

    private String enqueue(String channel, String destination, String idempotencyKey, String aggregateType,
            String aggregateId, long aggregateVersion, Map<String, Object> payload) {
        String eventId = sha256(idempotencyKey);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", destination);
        envelope.put("schemaVersion", 1);
        envelope.put("occurredAt", clock.instant());
        envelope.put("aggregateType", aggregateType);
        envelope.put("aggregateId", aggregateId);
        envelope.put("aggregateVersion", aggregateVersion);
        envelope.put("payload", payload);
        return outbox.enqueue(OutboxMessage.builder()
                        .messageId(eventId)
                        .idempotencyKey(idempotencyKey)
                        .channel(channel)
                        .destination(destination)
                        .payload(envelope)
                        .schemaVersion("1")
                        .build())
                .messageId();
    }

    /**
     * 从幂等键生成不暴露业务内容的稳定事件标识。
     * / Generates a stable event identifier that does not expose business content.
     *
     * @param value 幂等键 / idempotency key
     * @return 小写十六进制 SHA-256 / lowercase hexadecimal SHA-256
     */
    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    unavailable
            );
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return value;
    }

    private static void requireVersion(long version) {
        if (version < 0L) {
            throw new IllegalArgumentException("RESOURCE_PROJECTION_VERSION_INVALID");
        }
    }
}
