package top.egon.cola.component.gateway.engine.http.logging;

import java.util.Objects;

/**
 * 中文说明：{@code GatewayBodyLogEvent} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关BodyLog事件相关的职责与边界。
 * English summary: {@code GatewayBodyLogEvent} is an immutable data carrier in the current Gateway module; it owns the gateway body log event-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param direction 参数 direction；parameter direction。
 * @param contentType 参数 contentType；parameter content type。
 * @param totalBytes 参数 totalBytes；parameter total bytes。
 * @param metadataOnly 参数 元数据Only；parameter metadata only。
 * @param sample 参数 sample；parameter sample。
 */
public record GatewayBodyLogEvent(
        /**
         * 中文说明：保存 direction 对应的状态、依赖或配置值；字段类型为 {@code GatewayBodyLogDirection}，由 {@code GatewayBodyLogEvent} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by direction; its type is {@code GatewayBodyLogDirection}, and {@code GatewayBodyLogEvent} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayBodyLogEvent} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayBodyLogEvent}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayBodyLogDirection direction,
        /**
         * 中文说明：保存 contentType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayBodyLogEvent} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by content type; its type is {@code String}, and {@code GatewayBodyLogEvent} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayBodyLogEvent} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayBodyLogEvent}; do not couple callers to its representation when the owning type exposes an API.
         */
        String contentType,
        /**
         * 中文说明：保存 totalBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayBodyLogEvent} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by total bytes; its type is {@code long}, and {@code GatewayBodyLogEvent} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayBodyLogEvent} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayBodyLogEvent}; do not couple callers to its representation when the owning type exposes an API.
         */
        long totalBytes,
        /**
         * 中文说明：保存 元数据Only 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayBodyLogEvent} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by metadata only; its type is {@code boolean}, and {@code GatewayBodyLogEvent} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayBodyLogEvent} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayBodyLogEvent}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean metadataOnly,
        /**
         * 中文说明：保存 sample 对应的状态、依赖或配置值；字段类型为 {@code byte[]}，由 {@code GatewayBodyLogEvent} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by sample; its type is {@code byte[]}, and {@code GatewayBodyLogEvent} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayBodyLogEvent} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayBodyLogEvent}; do not couple callers to its representation when the owning type exposes an API.
         */
        byte[] sample
) {

    /**
     * 中文说明：创建 {@code GatewayBodyLogEvent} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayBodyLogEvent} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param direction 参数 direction；parameter direction。
     * @param contentType 参数 contentType；parameter content type。
     * @param totalBytes 参数 totalBytes；parameter total bytes。
     * @param metadataOnly 参数 元数据Only；parameter metadata only。
     * @param sample 参数 sample；parameter sample。
     */
    public GatewayBodyLogEvent {
        direction = Objects.requireNonNull(direction, "direction");
        contentType = contentType == null ? "" : contentType;
        if (totalBytes < 0) {
            throw new IllegalArgumentException(
                    "totalBytes must be non-negative"
            );
        }
        sample = sample == null ? new byte[0] : sample.clone();
    }

    /**
     * 中文说明：执行 sample 操作；该方法是 {@code GatewayBodyLogEvent} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the sample operation; this method is the invocation entry point on {@code GatewayBodyLogEvent} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayBodyLogEvent.sample(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 sample 的处理结果；returns the result of the operation.
     */
    @Override
    public byte[] sample() {
        return sample.clone();
    }
}
