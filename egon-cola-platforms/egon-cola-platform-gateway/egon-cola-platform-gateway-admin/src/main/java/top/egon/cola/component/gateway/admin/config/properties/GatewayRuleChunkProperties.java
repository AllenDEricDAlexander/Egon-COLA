package top.egon.cola.component.gateway.admin.config.properties;


import java.time.Duration;


/**
 * 中文说明：{@code GatewayRuleChunkProperties} 是类型，位于当前 Gateway 模块的相关包中，负责规则Chunk相关的职责与边界。
 * English summary: {@code GatewayRuleChunkProperties} is a type in the current Gateway module; it owns the rule chunk-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public class GatewayRuleChunkProperties {

    /**
     * 中文说明：保存 retention 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code config.properties.GatewayRuleChunkProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by retention; its type is {@code Duration}, and {@code config.properties.GatewayRuleChunkProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code config.properties.GatewayRuleChunkProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code config.properties.GatewayRuleChunkProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private Duration retention = Duration.ofHours(24);

    /**
     * 中文说明：执行 getRetention 操作；该方法是 {@code config.properties.GatewayRuleChunkProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get retention operation; this method is the invocation entry point on {@code config.properties.GatewayRuleChunkProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code config.properties.GatewayRuleChunkProperties.getRetention(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getRetention 的处理结果；returns the result of the operation.
     */
    public Duration getRetention() {
        return retention;
    }

    /**
     * 中文说明：执行 setRetention 操作；该方法是 {@code config.properties.GatewayRuleChunkProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set retention operation; this method is the invocation entry point on {@code config.properties.GatewayRuleChunkProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code config.properties.GatewayRuleChunkProperties.setRetention(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param retention 参数 retention；parameter retention。
     */
    public void setRetention(Duration retention) {
        this.retention = retention;
    }
}
