package top.egon.cola.platform.idp.admin.resource.support.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.model.management.DdcResourceAdmissionRevocationRequest;
import top.egon.cola.component.ddc.model.management.DdcResourceAdmissionRevocationResult;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;

import java.util.Objects;

/**
 * 将 Resource Server 停用事件投递为 DDC 精确三元组撤销命令。
 * / Delivers Resource Server disable events as exact-triple DDC revocation commands.
 */
public final class DdcResourceServerLifecycleDeliveryHandler
        implements DeliveryHandler {

    /** DDC 撤销端口；DDC revocation port. */
    private final RevocationClient client;

    /** JSON 编解码器；JSON codec. */
    private final ObjectMapper objectMapper;

    /**
     * 使用类型化 DDC 管理客户端创建投递器。
     * / Creates the handler with the typed DDC management client.
     *
     * @param client DDC 管理客户端 / DDC management client
     */
    public DdcResourceServerLifecycleDeliveryHandler(
            DdcManagementClient client) {
        this(
                client::revokeResourceAdmission,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    /**
     * 使用可延迟创建的撤销端口和共享 JSON 配置创建投递器。
     * / Creates the handler with a lazily creatable revocation port and shared JSON configuration.
     *
     * @param client DDC 撤销端口 / DDC revocation port
     * @param objectMapper JSON 编解码器 / JSON codec
     */
    public DdcResourceServerLifecycleDeliveryHandler(
            RevocationClient client,
            ObjectMapper objectMapper) {
        this.client = Objects.requireNonNull(client, "client");
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper"
        );
    }

    /** {@inheritDoc} */
    @Override
    public String channel() {
        return TransactionalOutboxResourceServerEventAdapter.CHANNEL;
    }

    /** {@inheritDoc} */
    @Override
    public void validateDestination(String destination) {
        if (!TransactionalOutboxResourceServerEventAdapter
                .DISABLED_DESTINATION.equals(destination)) {
            throw new IllegalArgumentException(
                    "unsupported identity Resource destination: "
                            + destination
            );
        }
    }

    /**
     * 校验事件信封并投递 DDC 撤销；协议错误永久失败，DDC 暂时不可用则保留重试。
     * / Validates the event envelope and delivers DDC revocation. Protocol errors fail permanently,
     * while temporary DDC unavailability remains retryable.
     *
     * @param context 发件箱投递上下文 / outbox delivery context
     * @return 投递分类结果 / classified delivery result
     */
    @Override
    public DeliveryResult deliver(DeliveryContext context) {
        try {
            validateDestination(context.destination());
            client.revoke(parse(context));
            return DeliveryResult.success();
        } catch (IllegalArgumentException invalid) {
            return DeliveryResult.permanentFailure(
                    "IDENTITY_RESOURCE_EVENT_INVALID",
                    safeMessage(invalid)
            );
        } catch (RuntimeException unavailable) {
            return DeliveryResult.retryableFailure(
                    "DDC_RESOURCE_REVOCATION_UNAVAILABLE",
                    "DDC Resource admission revocation is unavailable"
            );
        }
    }

    /**
     * 解析并交叉校验事件信封与业务载荷版本。
     * / Parses and cross-validates the event envelope and business-payload version.
     *
     * @param context 发件箱投递上下文 / outbox delivery context
     * @return DDC 精确撤销命令 / exact DDC revocation command
     */
    private DdcResourceAdmissionRevocationRequest parse(
            DeliveryContext context) {
        try {
            JsonNode envelope = objectMapper.readTree(context.payload());
            if (!context.destination().equals(
                    required(envelope, "eventType"))) {
                throw new IllegalArgumentException(
                        "event type does not match destination"
                );
            }
            if (envelope.path("schemaVersion").asInt(-1) != 1) {
                throw new IllegalArgumentException(
                        "unsupported identity Resource event version"
                );
            }
            JsonNode payload = envelope.path("payload");
            long aggregateVersion = envelope.path(
                    "aggregateVersion"
            ).asLong(-1L);
            long resourceVersion = payload.path(
                    "resourceVersion"
            ).asLong(-1L);
            String resourceServerId = required(
                    payload,
                    "resourceServerId"
            );
            if (aggregateVersion < 0L
                    || aggregateVersion != resourceVersion
                    || !resourceServerId.equals(
                            required(envelope, "aggregateId"))) {
                throw new IllegalArgumentException(
                        "identity Resource event aggregate mismatch"
                );
            }
            return new DdcResourceAdmissionRevocationRequest(
                    resourceServerId,
                    required(payload, "bizCode"),
                    required(payload, "appCode"),
                    required(payload, "env"),
                    resourceVersion
            );
        } catch (IllegalArgumentException invalid) {
            throw invalid;
        } catch (Exception invalid) {
            throw new IllegalArgumentException(
                    "invalid identity Resource event envelope",
                    invalid
            );
        }
    }

    /**
     * 读取非空文本字段。
     * / Reads a non-blank text field.
     */
    private String required(JsonNode source, String field) {
        String value = source.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    /**
     * 截断可公开的协议错误信息。
     * / Truncates a safe protocol-error message.
     */
    private String safeMessage(IllegalArgumentException invalid) {
        String message = invalid.getMessage();
        if (message == null || message.isBlank()) {
            return "invalid identity Resource event envelope";
        }
        return message.substring(0, Math.min(256, message.length()));
    }

    /**
     * DDC 撤销调用边界，允许生产装配按投递创建并关闭 Direct RPC 客户端。
     * / DDC revocation boundary allowing production wiring to create and close a Direct RPC client
     * per delivery.
     */
    @FunctionalInterface
    public interface RevocationClient {

        /**
         * 执行幂等精确三元组撤销。
         * / Executes an idempotent exact-triple revocation.
         *
         * @param request 撤销命令 / revocation command
         * @return 撤销统计 / revocation counts
         */
        DdcResourceAdmissionRevocationResult revoke(
                DdcResourceAdmissionRevocationRequest request);
    }
}
