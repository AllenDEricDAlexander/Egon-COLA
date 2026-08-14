package top.egon.cola.component.gateway.admin.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import top.egon.cola.component.gateway.admin.config.properties.GatewayAdminDdcProperties;
import top.egon.cola.component.gateway.admin.config.properties.GatewayRuleChunkProperties;

/**
 * 中文说明：{@code GatewayAdminProperties} 是配置属性模型，位于当前 Gateway 模块的相关包中，负责网关管理端Properties相关的职责与边界。
 * English summary: {@code GatewayAdminProperties} is a gateway admin properties properties in the current Gateway module; it owns the gateway admin properties-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@ConfigurationProperties(prefix = "gateway.admin")
public class GatewayAdminProperties {

    /**
     * 中文说明：保存 规则Chunk 对应的状态、依赖或配置值；字段类型为 {@code GatewayRuleChunkProperties}，由 {@code GatewayAdminProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rule chunk; its type is {@code GatewayRuleChunkProperties}, and {@code GatewayAdminProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAdminProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private GatewayRuleChunkProperties ruleChunk = new GatewayRuleChunkProperties();

    /**
     * 中文说明：保存 ddc 对应的状态、依赖或配置值；字段类型为 {@code GatewayAdminDdcProperties}，由 {@code GatewayAdminProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by ddc; its type is {@code GatewayAdminDdcProperties}, and {@code GatewayAdminProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayAdminProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayAdminProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private GatewayAdminDdcProperties ddc = new GatewayAdminDdcProperties();

    /**
     * 中文说明：执行 get规则Chunk 操作；该方法是 {@code GatewayAdminProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get rule chunk operation; this method is the invocation entry point on {@code GatewayAdminProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminProperties.getRuleChunk(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get规则Chunk 的处理结果；returns the result of the operation.
     */
    public GatewayRuleChunkProperties getRuleChunk() {
        return ruleChunk;
    }

    /**
     * 中文说明：执行 set规则Chunk 操作；该方法是 {@code GatewayAdminProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set rule chunk operation; this method is the invocation entry point on {@code GatewayAdminProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminProperties.setRuleChunk(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param ruleChunk 参数 规则Chunk；parameter rule chunk。
     */
    public void setRuleChunk(GatewayRuleChunkProperties ruleChunk) {
        this.ruleChunk = ruleChunk;
    }

    /**
     * 中文说明：执行 getDdc 操作；该方法是 {@code GatewayAdminProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get ddc operation; this method is the invocation entry point on {@code GatewayAdminProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminProperties.getDdc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getDdc 的处理结果；returns the result of the operation.
     */
    public GatewayAdminDdcProperties getDdc() {
        return ddc;
    }

    /**
     * 中文说明：执行 setDdc 操作；该方法是 {@code GatewayAdminProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set ddc operation; this method is the invocation entry point on {@code GatewayAdminProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayAdminProperties.setDdc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param ddc 参数 ddc；parameter ddc。
     */
    public void setDdc(GatewayAdminDdcProperties ddc) {
        this.ddc = ddc;
    }




}
