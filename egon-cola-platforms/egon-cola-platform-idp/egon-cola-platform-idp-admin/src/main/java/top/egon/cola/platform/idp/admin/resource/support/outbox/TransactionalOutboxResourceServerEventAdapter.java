package top.egon.cola.platform.idp.admin.resource.support.outbox;

import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.idp.admin.resource.domain.pojo.IdentityResourceServerEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
public final class TransactionalOutboxResourceServerEventAdapter {

    /** Resource 生命周期投递通道；Resource lifecycle delivery channel. */
    public static final String CHANNEL = "identity-resource-runtime";

    /** Resource 停用事件目的地；Resource-disabled event destination. */
    public static final String DISABLED_DESTINATION =
            "identity.resource-server.disabled.v1";

    /** 事务发件箱端口；transactional-outbox port. */
    private final TransactionalOutbox outbox;

    /** 事件时钟；event clock. */
    private final Clock clock;

    /**
     * 创建 Resource Server 事务事件适配器。
     * / Creates a Resource Server transactional-event adapter.
     *
     * @param outbox 事务发件箱 / transactional outbox
     * @param clock 事件时钟 / event clock
     */
    public TransactionalOutboxResourceServerEventAdapter(
            TransactionalOutbox outbox,
            Clock clock) {
        this.outbox = Objects.requireNonNull(outbox, "outbox");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 将已推进版本的停用 Resource 写入当前数据库事务。
     * / Enqueues the disabled Resource with its advanced version in the current database
     * transaction.
     *
     * @param resource 已停用的 Resource 实体 / disabled Resource entity
     * @return 稳定事件标识 / stable event identifier
     */
    public String enqueueDisabled(IdentityResourceServerEntity resource) {
        Objects.requireNonNull(resource, "resource");
        if (resource.getStatus()
                != IdentityResourceServerEntity.Status.DISABLED) {
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
}
