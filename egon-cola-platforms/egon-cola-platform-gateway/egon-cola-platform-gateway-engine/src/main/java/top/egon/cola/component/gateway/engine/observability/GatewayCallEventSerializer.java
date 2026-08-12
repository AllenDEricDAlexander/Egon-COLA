package top.egon.cola.component.gateway.engine.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.io.IOException;

/**
 * 中文说明：{@code GatewayCallEventSerializer} 是类型，位于当前 Gateway 模块的相关包中，负责网关调用事件Serializer相关的职责与边界。
 * English summary: {@code GatewayCallEventSerializer} is a type in the current Gateway module; it owns the gateway call event serializer-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayCallEventSerializer {

    /**
     * 中文说明：表示 MAXPAYLOADBYTES 这一固定值；它属于 {@code GatewayCallEventSerializer} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value max payload bytes; it is a state, type, or protocol value of {@code GatewayCallEventSerializer} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventSerializer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventSerializer}; do not couple callers to its representation when the owning type exposes an API.
     */
    public static final int MAX_PAYLOAD_BYTES = 64 * 1024;

    /**
     * 中文说明：保存 映射器 对应的状态、依赖或配置值；字段类型为 {@code ObjectMapper}，由 {@code GatewayCallEventSerializer} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by mapper; its type is {@code ObjectMapper}, and {@code GatewayCallEventSerializer} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayCallEventSerializer} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayCallEventSerializer}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ObjectMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .build();

    /**
     * 中文说明：执行 serialize 操作；该方法是 {@code GatewayCallEventSerializer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the serialize operation; this method is the invocation entry point on {@code GatewayCallEventSerializer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventSerializer.serialize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     * @return 返回 serialize 的处理结果；returns the result of the operation.
     */
    public byte[] serialize(GatewayCallEventV1 event) {
        try {
            byte[] payload = mapper.writeValueAsBytes(event);
            if (payload.length > MAX_PAYLOAD_BYTES) {
                throw new PayloadTooLargeException(payload.length);
            }
            return payload;
        } catch (JsonProcessingException failure) {
            throw new IllegalArgumentException(
                    "gateway call event serialization failed",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 deserialize 操作；该方法是 {@code GatewayCallEventSerializer} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the deserialize operation; this method is the invocation entry point on {@code GatewayCallEventSerializer} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayCallEventSerializer.deserialize(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param payload 参数 payload；parameter payload。
     * @return 返回 deserialize 的处理结果；returns the result of the operation.
     */
    public GatewayCallEventV1 deserialize(byte[] payload) {
        if (payload == null || payload.length > MAX_PAYLOAD_BYTES) {
            throw new PayloadTooLargeException(
                    payload == null ? 0 : payload.length
            );
        }
        try {
            return mapper.readValue(payload, GatewayCallEventV1.class);
        } catch (IOException failure) {
            throw new IllegalArgumentException(
                    "invalid gateway call event",
                    failure
            );
        }
    }

    /**
     * 中文说明：{@code PayloadTooLargeException} 是异常类型，位于当前 Gateway 模块的相关包中，负责PayloadTooLargeException相关的职责与边界。
     * English summary: {@code PayloadTooLargeException} is a payload too large exception exception in the current Gateway module; it owns the payload too large exception-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static final class PayloadTooLargeException
            extends IllegalArgumentException {

        /**
         * 中文说明：创建 {@code GatewayCallEventSerializer.PayloadTooLargeException} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
         * English summary: Creates an instance of {@code GatewayCallEventSerializer.PayloadTooLargeException} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
         *
         * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
         * @param bytes 参数 bytes；parameter bytes。
         */
        public PayloadTooLargeException(int bytes) {
            super("gateway call event exceeds "
                    + MAX_PAYLOAD_BYTES
                    + " bytes: "
                    + bytes);
        }
    }
}
