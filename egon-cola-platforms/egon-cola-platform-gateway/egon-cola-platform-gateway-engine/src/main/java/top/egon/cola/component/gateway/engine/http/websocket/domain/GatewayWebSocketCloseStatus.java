package top.egon.cola.component.gateway.engine.http.websocket.domain;

import java.nio.charset.StandardCharsets;

/**
 * WebSocket close fact that distinguishes sendable wire codes from 1006.
 * 补充说明 / Supplementary summary: {@code GatewayWebSocketCloseStatus} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关WebSocketCloseStatus相关的职责与边界。
 * English supplement: {@code GatewayWebSocketCloseStatus} is an immutable data carrier in the current Gateway module; it owns the gateway web socket close status-related responsibility and boundary.
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param code 参数 code；parameter code。
 * @param reason 参数 reason；parameter reason。
 */
public record GatewayWebSocketCloseStatus(
    /**
     * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayWebSocketCloseStatus} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code int}, and {@code GatewayWebSocketCloseStatus} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketCloseStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketCloseStatus}; do not couple callers to its representation when the owning type exposes an API.
     */
    int code,
    /**
     * 中文说明：保存 reason 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayWebSocketCloseStatus} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by reason; its type is {@code String}, and {@code GatewayWebSocketCloseStatus} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketCloseStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketCloseStatus}; do not couple callers to its representation when the owning type exposes an API.
     */
    String reason) {

    /**
     * 中文说明：表示 MAXREASONBYTES 这一固定值；它属于 {@code GatewayWebSocketCloseStatus} 的状态、类型或协议取值，用于保持调用方与所属类型之间的语义一致。
     * English summary: Represents the fixed value max reason bytes; it is a state, type, or protocol value of {@code GatewayWebSocketCloseStatus} and keeps callers aligned with the owning type.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayWebSocketCloseStatus} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayWebSocketCloseStatus}; do not couple callers to its representation when the owning type exposes an API.
     */
    private static final int MAX_REASON_BYTES = 123;

    /**
     * 中文说明：创建 {@code GatewayWebSocketCloseStatus} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayWebSocketCloseStatus} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param code 参数 code；parameter code。
     * @param reason 参数 reason；parameter reason。
     */
    public GatewayWebSocketCloseStatus {
        reason = reason == null ? "" : reason;
        if (!valid(code)) {
            throw new IllegalArgumentException(
                    "invalid WebSocket close code: " + code
            );
        }
        if (reason.getBytes(StandardCharsets.UTF_8).length
                > MAX_REASON_BYTES) {
            throw new IllegalArgumentException(
                    "WebSocket close reason exceeds 123 UTF-8 bytes"
            );
        }
    }

    /**
     * 中文说明：执行 abnormal 操作；该方法是 {@code GatewayWebSocketCloseStatus} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the abnormal operation; this method is the invocation entry point on {@code GatewayWebSocketCloseStatus} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketCloseStatus.abnormal(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 abnormal 的处理结果；returns the result of the operation.
     */
    public static GatewayWebSocketCloseStatus abnormal() {
        return new GatewayWebSocketCloseStatus(1006, "abnormal closure");
    }

    /**
     * 中文说明：执行 goingAway 操作；该方法是 {@code GatewayWebSocketCloseStatus} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the going away operation; this method is the invocation entry point on {@code GatewayWebSocketCloseStatus} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketCloseStatus.goingAway(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 goingAway 的处理结果；returns the result of the operation.
     */
    public static GatewayWebSocketCloseStatus goingAway() {
        return new GatewayWebSocketCloseStatus(1001, "going away");
    }

    /**
     * 中文说明：执行 frameTooLarge 操作；该方法是 {@code GatewayWebSocketCloseStatus} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the frame too large operation; this method is the invocation entry point on {@code GatewayWebSocketCloseStatus} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketCloseStatus.frameTooLarge(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 frameTooLarge 的处理结果；returns the result of the operation.
     */
    public static GatewayWebSocketCloseStatus frameTooLarge() {
        return new GatewayWebSocketCloseStatus(1009, "frame too large");
    }

    /**
     * 中文说明：执行 sendable 操作；该方法是 {@code GatewayWebSocketCloseStatus} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sendable operation; this method is the invocation entry point on {@code GatewayWebSocketCloseStatus} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketCloseStatus.sendable(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 sendable 的处理结果；returns the result of the operation.
     */
    public boolean sendable() {
        return code != 1006;
    }

    /**
     * 中文说明：执行 valid 操作；该方法是 {@code GatewayWebSocketCloseStatus} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the valid operation; this method is the invocation entry point on {@code GatewayWebSocketCloseStatus} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayWebSocketCloseStatus.valid(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param code 参数 code；parameter code。
     * @return 返回 valid 的处理结果；returns the result of the operation.
     */
    private static boolean valid(int code) {
        return code == 1006
                || (code >= 1000
                && code <= 1014
                && code != 1004
                && code != 1005)
                || (code >= 3000 && code <= 4999);
    }
}
