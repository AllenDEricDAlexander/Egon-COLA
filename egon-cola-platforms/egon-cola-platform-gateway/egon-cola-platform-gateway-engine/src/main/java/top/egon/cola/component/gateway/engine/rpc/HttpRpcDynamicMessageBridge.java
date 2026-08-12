package top.egon.cola.component.gateway.engine.rpc;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;

/**
 * 中文说明：{@code HttpRpcDynamicMessageBridge} 是类型，位于当前 Gateway 模块的相关包中，负责HttpRpcDynamic消息Bridge相关的职责与边界。
 * English summary: {@code HttpRpcDynamicMessageBridge} is a type in the current Gateway module; it owns the http rpc dynamic message bridge-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class HttpRpcDynamicMessageBridge {

    /**
     * 中文说明：保存 descriptors 对应的状态、依赖或配置值；字段类型为 {@code ProtobufDescriptorRegistry}，由 {@code HttpRpcDynamicMessageBridge} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by descriptors; its type is {@code ProtobufDescriptorRegistry}, and {@code HttpRpcDynamicMessageBridge} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code HttpRpcDynamicMessageBridge} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code HttpRpcDynamicMessageBridge}; do not couple callers to its representation when the owning type exposes an API.
     */
    private final ProtobufDescriptorRegistry descriptors;

    /**
     * 中文说明：创建 {@code HttpRpcDynamicMessageBridge} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code HttpRpcDynamicMessageBridge} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param descriptors 参数 descriptors；parameter descriptors。
     */
    public HttpRpcDynamicMessageBridge(
            ProtobufDescriptorRegistry descriptors) {
        this.descriptors = descriptors;
    }

    /**
     * 中文说明：执行 请求Bytes 操作；该方法是 {@code HttpRpcDynamicMessageBridge} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the request bytes operation; this method is the invocation entry point on {@code HttpRpcDynamicMessageBridge} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpRpcDynamicMessageBridge.requestBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param messageType 参数 消息Type；parameter message type。
     * @param json 参数 json；parameter json。
     * @return 返回 请求Bytes 的处理结果；returns the result of the operation.
     */
    public byte[] requestBytes(String messageType, String json) {
        try {
            DynamicMessage.Builder builder = DynamicMessage.newBuilder(
                    descriptors.message(messageType)
            );
            JsonFormat.parser().merge(json, builder);
            return builder.build().toByteArray();
        } catch (com.google.protobuf.InvalidProtocolBufferException failure) {
            throw new IllegalArgumentException(
                    "HTTP request does not match protobuf schema",
                    failure
            );
        }
    }

    /**
     * 中文说明：执行 响应Json 操作；该方法是 {@code HttpRpcDynamicMessageBridge} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the response json operation; this method is the invocation entry point on {@code HttpRpcDynamicMessageBridge} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code HttpRpcDynamicMessageBridge.responseJson(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param messageType 参数 消息Type；parameter message type。
     * @param bytes 参数 bytes；parameter bytes。
     * @return 返回 响应Json 的处理结果；returns the result of the operation.
     */
    public String responseJson(String messageType, byte[] bytes) {
        try {
            DynamicMessage message = DynamicMessage.parseFrom(
                    descriptors.message(messageType),
                    bytes
            );
            return JsonFormat.printer().print(message);
        } catch (com.google.protobuf.InvalidProtocolBufferException failure) {
            throw new IllegalArgumentException(
                    "RPC response does not match protobuf schema",
                    failure
            );
        }
    }
}
