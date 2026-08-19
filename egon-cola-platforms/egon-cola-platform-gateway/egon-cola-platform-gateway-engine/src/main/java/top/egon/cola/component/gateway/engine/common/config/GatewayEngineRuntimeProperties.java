package top.egon.cola.component.gateway.engine.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 中文说明：{@code GatewayEngineRuntimeProperties} 是配置属性模型，位于当前 Gateway 模块的相关包中，负责网关引擎运行时Properties相关的职责与边界。
 * English summary: {@code GatewayEngineRuntimeProperties} is a gateway engine runtime properties properties in the current Gateway module; it owns the gateway engine runtime properties-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
@ConfigurationProperties(prefix = "egon.cola.component.gateway.engine")
public class GatewayEngineRuntimeProperties {

    /**
     * 中文说明：保存 网关GroupCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by gateway group code; its type is {@code String}, and {@code GatewayEngineRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private String gatewayGroupCode = "default";

    /**
     * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code GatewayEngineRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private String env = "dev";

    /**
     * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code GatewayEngineRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private String namespace = "default";

    /**
     * 中文说明：保存 nodeId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by node id; its type is {@code String}, and {@code GatewayEngineRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private String nodeId = "gateway-engine";

    /**
     * 中文说明：保存 instanceId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by instance id; its type is {@code String}, and {@code GatewayEngineRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private String instanceId = "gateway-engine";

    /**
     * 中文说明：保存 dataDirectory 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by data directory; its type is {@code String}, and {@code GatewayEngineRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private String dataDirectory = "./data/gateway-engine";

    /**
     * 中文说明：保存 http 对应的状态、依赖或配置值；字段类型为 {@code Http}，由 {@code GatewayEngineRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by http; its type is {@code Http}, and {@code GatewayEngineRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private Http http = new Http();

    /**
     * 中文说明：保存 rpc 对应的状态、依赖或配置值；字段类型为 {@code Rpc}，由 {@code GatewayEngineRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by rpc; its type is {@code Rpc}, and {@code GatewayEngineRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private Rpc rpc = new Rpc();

    /**
     * 中文说明：保存 kafka 对应的状态、依赖或配置值；字段类型为 {@code Kafka}，由 {@code GatewayEngineRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by kafka; its type is {@code Kafka}, and {@code GatewayEngineRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private Kafka kafka = new Kafka();

    /**
     * 中文说明：保存 安全 对应的状态、依赖或配置值；字段类型为 {@code Security}，由 {@code GatewayEngineRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by security; its type is {@code Security}, and {@code GatewayEngineRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private Security security = new Security();

    /**
     * 中文说明：保存 active健康 对应的状态、依赖或配置值；字段类型为 {@code ActiveHealth}，由 {@code GatewayEngineRuntimeProperties} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by active health; its type is {@code ActiveHealth}, and {@code GatewayEngineRuntimeProperties} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties}; do not couple callers to its representation when the owning type exposes an API.
     */
    private ActiveHealth activeHealth = new ActiveHealth();

    /**
     * 中文说明：执行 get网关GroupCode 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get gateway group code operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.getGatewayGroupCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get网关GroupCode 的处理结果；returns the result of the operation.
     */
    public String getGatewayGroupCode() {
        return gatewayGroupCode;
    }

    /**
     * 中文说明：执行 set网关GroupCode 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set gateway group code operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.setGatewayGroupCode(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupCode 参数 网关GroupCode；parameter gateway group code。
     */
    public void setGatewayGroupCode(String gatewayGroupCode) {
        this.gatewayGroupCode = gatewayGroupCode;
    }

    /**
     * 中文说明：执行 getEnv 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get env operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.getEnv(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getEnv 的处理结果；returns the result of the operation.
     */
    public String getEnv() {
        return env;
    }

    /**
     * 中文说明：执行 setEnv 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set env operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.setEnv(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param env 参数 env；parameter env。
     */
    public void setEnv(String env) {
        this.env = env;
    }

    /**
     * 中文说明：执行 get命名空间 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get namespace operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.getNamespace(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get命名空间 的处理结果；returns the result of the operation.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * 中文说明：执行 set命名空间 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set namespace operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.setNamespace(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param namespace 参数 命名空间；parameter namespace。
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * 中文说明：执行 getNodeId 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get node id operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.getNodeId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getNodeId 的处理结果；returns the result of the operation.
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * 中文说明：执行 setNodeId 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set node id operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.setNodeId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param nodeId 参数 nodeId；parameter node id。
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * 中文说明：执行 getInstanceId 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get instance id operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.getInstanceId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getInstanceId 的处理结果；returns the result of the operation.
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * 中文说明：执行 setInstanceId 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set instance id operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.setInstanceId(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param instanceId 参数 instanceId；parameter instance id。
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * 中文说明：执行 getDataDirectory 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get data directory operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.getDataDirectory(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getDataDirectory 的处理结果；returns the result of the operation.
     */
    public String getDataDirectory() {
        return dataDirectory;
    }

    /**
     * 中文说明：执行 setDataDirectory 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set data directory operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.setDataDirectory(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param dataDirectory 参数 dataDirectory；parameter data directory。
     */
    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    /**
     * 中文说明：执行 getHttp 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get http operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.getHttp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getHttp 的处理结果；returns the result of the operation.
     */
    public Http getHttp() {
        return http;
    }

    /**
     * 中文说明：执行 setHttp 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set http operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.setHttp(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param http 参数 http；parameter http。
     */
    public void setHttp(Http http) {
        this.http = http;
    }

    /**
     * 中文说明：执行 getRpc 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get rpc operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.getRpc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getRpc 的处理结果；returns the result of the operation.
     */
    public Rpc getRpc() {
        return rpc;
    }

    /**
     * 中文说明：执行 setRpc 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set rpc operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.setRpc(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param rpc 参数 rpc；parameter rpc。
     */
    public void setRpc(Rpc rpc) {
        this.rpc = rpc;
    }

    /**
     * 中文说明：执行 getKafka 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get kafka operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.getKafka(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getKafka 的处理结果；returns the result of the operation.
     */
    public Kafka getKafka() {
        return kafka;
    }

    /**
     * 中文说明：执行 setKafka 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set kafka operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.setKafka(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param kafka 参数 kafka；parameter kafka。
     */
    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    /**
     * 中文说明：执行 get安全 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get security operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.getSecurity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 get安全 的处理结果；returns the result of the operation.
     */
    public Security getSecurity() {
        return security;
    }

    /**
     * 中文说明：执行 set安全 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set security operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.setSecurity(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param security 参数 安全；parameter security。
     */
    public void setSecurity(Security security) {
        this.security = security;
    }

    /**
     * 中文说明：执行 getActive健康 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the get active health operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.getActiveHealth(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 getActive健康 的处理结果；returns the result of the operation.
     */
    public ActiveHealth getActiveHealth() {
        return activeHealth;
    }

    /**
     * 中文说明：执行 setActive健康 操作；该方法是 {@code GatewayEngineRuntimeProperties} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the set active health operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.setActiveHealth(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param activeHealth 参数 active健康；parameter active health。
     */
    public void setActiveHealth(ActiveHealth activeHealth) {
        this.activeHealth = activeHealth;
    }

    /**
     * 中文说明：{@code Http} 是类型，位于当前 Gateway 模块的相关包中，负责Http相关的职责与边界。
     * English summary: {@code Http} is a type in the current Gateway module; it owns the http-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static class Http {

        /**
         * 中文说明：保存 publicEnabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by public enabled; its type is {@code boolean}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean publicEnabled = true;

        /**
         * 中文说明：保存 publicHost 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by public host; its type is {@code String}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String publicHost = "0.0.0.0";

        /**
         * 中文说明：保存 publicPort 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by public port; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int publicPort = 18081;

        /**
         * 中文说明：保存 internalEnabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by internal enabled; its type is {@code boolean}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean internalEnabled = true;

        /**
         * 中文说明：保存 internalHost 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by internal host; its type is {@code String}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String internalHost = "0.0.0.0";

        /**
         * 中文说明：保存 internalPort 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by internal port; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int internalPort = 18082;

        /**
         * 中文说明：保存 maxHeaderCount 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max header count; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int maxHeaderCount = 128;

        /**
         * 中文说明：保存 maxHeaderBytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max header bytes; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int maxHeaderBytes = 64 * 1024;

        /**
         * 中文说明：保存 maxBodyBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max body bytes; its type is {@code long}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private long maxBodyBytes = 2L * 1024 * 1024;

        /**
         * 中文说明：保存 absoluteMax请求BodyBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by absolute max request body bytes; its type is {@code long}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private long absoluteMaxRequestBodyBytes = 1024L * 1024 * 1024;

        /**
         * 中文说明：保存 bodyLogSampleBytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by body log sample bytes; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int bodyLogSampleBytes = 8 * 1024;

        /**
         * 中文说明：保存 absoluteMaxBodyLogSampleBytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by absolute max body log sample bytes; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int absoluteMaxBodyLogSampleBytes = 64 * 1024;

        /**
         * 中文说明：保存 idle超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by idle timeout; its type is {@code Duration}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration idleTimeout = Duration.ofSeconds(30);

        /**
         * 中文说明：保存 upstream超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by upstream timeout; its type is {@code Duration}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration upstreamTimeout = Duration.ofSeconds(5);

        /**
         * 中文说明：保存 maxConnect超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max connect timeout; its type is {@code Duration}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration maxConnectTimeout = Duration.ofSeconds(60);

        /**
         * 中文说明：保存 max响应Header超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max response header timeout; its type is {@code Duration}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration maxResponseHeaderTimeout = Duration.ofMinutes(10);

        /**
         * 中文说明：保存 maxStreamIdle超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max stream idle timeout; its type is {@code Duration}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration maxStreamIdleTimeout = Duration.ofMinutes(30);

        /**
         * 中文说明：保存 maxTotal超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max total timeout; its type is {@code Duration}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration maxTotalTimeout = Duration.ofHours(2);

        /**
         * 中文说明：保存 maxWebSocketIdle超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max websocket idle timeout; its type is {@code Duration}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration maxWebsocketIdleTimeout = Duration.ofHours(2);

        /**
         * 中文说明：保存 maxWebSocketFrameBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max websocket frame bytes; its type is {@code long}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private long maxWebsocketFrameBytes = 64L * 1024 * 1024;

        /**
         * 中文说明：保存 drain超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by drain timeout; its type is {@code Duration}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration drainTimeout = Duration.ofSeconds(10);

        /**
         * 中文说明：保存 upstreamMaxConnections 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by upstream max connections; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int upstreamMaxConnections = 512;

        /**
         * 中文说明：保存 upstreamPendingAcquireMaxCount 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by upstream pending acquire max count; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int upstreamPendingAcquireMaxCount = 1024;

        /**
         * 中文说明：保存 publicTls 对应的状态、依赖或配置值；字段类型为 {@code Tls}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by public tls; its type is {@code Tls}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Tls publicTls = new Tls();

        /**
         * 中文说明：保存 internalTls 对应的状态、依赖或配置值；字段类型为 {@code Tls}，由 {@code GatewayEngineRuntimeProperties.Http} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by internal tls; its type is {@code Tls}, and {@code GatewayEngineRuntimeProperties.Http} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Http} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Http}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Tls internalTls = new Tls();

        /**
         * 中文说明：执行 isPublicEnabled 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is public enabled operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.isPublicEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 isPublicEnabled 的处理结果；returns the result of the operation.
         */
        public boolean isPublicEnabled() {
            return publicEnabled;
        }

        /**
         * 中文说明：执行 setPublicEnabled 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set public enabled operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setPublicEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param publicEnabled 参数 publicEnabled；parameter public enabled。
         */
        public void setPublicEnabled(boolean publicEnabled) {
            this.publicEnabled = publicEnabled;
        }

        /**
         * 中文说明：执行 getPublicHost 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get public host operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getPublicHost(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getPublicHost 的处理结果；returns the result of the operation.
         */
        public String getPublicHost() {
            return publicHost;
        }

        /**
         * 中文说明：执行 setPublicHost 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set public host operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setPublicHost(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param publicHost 参数 publicHost；parameter public host。
         */
        public void setPublicHost(String publicHost) {
            this.publicHost = publicHost;
        }

        /**
         * 中文说明：执行 getPublicPort 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get public port operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getPublicPort(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getPublicPort 的处理结果；returns the result of the operation.
         */
        public int getPublicPort() {
            return publicPort;
        }

        /**
         * 中文说明：执行 setPublicPort 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set public port operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setPublicPort(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param publicPort 参数 publicPort；parameter public port。
         */
        public void setPublicPort(int publicPort) {
            this.publicPort = publicPort;
        }

        /**
         * 中文说明：执行 isInternalEnabled 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is internal enabled operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.isInternalEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 isInternalEnabled 的处理结果；returns the result of the operation.
         */
        public boolean isInternalEnabled() {
            return internalEnabled;
        }

        /**
         * 中文说明：执行 setInternalEnabled 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set internal enabled operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setInternalEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param internalEnabled 参数 internalEnabled；parameter internal enabled。
         */
        public void setInternalEnabled(boolean internalEnabled) {
            this.internalEnabled = internalEnabled;
        }

        /**
         * 中文说明：执行 getInternalHost 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get internal host operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getInternalHost(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getInternalHost 的处理结果；returns the result of the operation.
         */
        public String getInternalHost() {
            return internalHost;
        }

        /**
         * 中文说明：执行 setInternalHost 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set internal host operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setInternalHost(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param internalHost 参数 internalHost；parameter internal host。
         */
        public void setInternalHost(String internalHost) {
            this.internalHost = internalHost;
        }

        /**
         * 中文说明：执行 getInternalPort 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get internal port operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getInternalPort(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getInternalPort 的处理结果；returns the result of the operation.
         */
        public int getInternalPort() {
            return internalPort;
        }

        /**
         * 中文说明：执行 setInternalPort 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set internal port operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setInternalPort(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param internalPort 参数 internalPort；parameter internal port。
         */
        public void setInternalPort(int internalPort) {
            this.internalPort = internalPort;
        }

        /**
         * 中文说明：执行 getMaxHeaderCount 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get max header count operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getMaxHeaderCount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMaxHeaderCount 的处理结果；returns the result of the operation.
         */
        public int getMaxHeaderCount() {
            return maxHeaderCount;
        }

        /**
         * 中文说明：执行 setMaxHeaderCount 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set max header count operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setMaxHeaderCount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maxHeaderCount 参数 maxHeaderCount；parameter max header count。
         */
        public void setMaxHeaderCount(int maxHeaderCount) {
            this.maxHeaderCount = maxHeaderCount;
        }

        /**
         * 中文说明：执行 getMaxHeaderBytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get max header bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getMaxHeaderBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMaxHeaderBytes 的处理结果；returns the result of the operation.
         */
        public int getMaxHeaderBytes() {
            return maxHeaderBytes;
        }

        /**
         * 中文说明：执行 setMaxHeaderBytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set max header bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setMaxHeaderBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maxHeaderBytes 参数 maxHeaderBytes；parameter max header bytes。
         */
        public void setMaxHeaderBytes(int maxHeaderBytes) {
            this.maxHeaderBytes = maxHeaderBytes;
        }

        /**
         * 中文说明：执行 getMaxBodyBytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get max body bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getMaxBodyBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMaxBodyBytes 的处理结果；returns the result of the operation.
         */
        public long getMaxBodyBytes() {
            return maxBodyBytes;
        }

        /**
         * 中文说明：执行 setMaxBodyBytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set max body bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setMaxBodyBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maxBodyBytes 参数 maxBodyBytes；parameter max body bytes。
         */
        public void setMaxBodyBytes(long maxBodyBytes) {
            this.maxBodyBytes = maxBodyBytes;
        }

        /**
         * 中文说明：执行 getAbsoluteMax请求BodyBytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get absolute max request body bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getAbsoluteMaxRequestBodyBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getAbsoluteMax请求BodyBytes 的处理结果；returns the result of the operation.
         */
        public long getAbsoluteMaxRequestBodyBytes() {
            return absoluteMaxRequestBodyBytes;
        }

        /**
         * 中文说明：执行 setAbsoluteMax请求BodyBytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set absolute max request body bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setAbsoluteMaxRequestBodyBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param absoluteMaxRequestBodyBytes 参数 absoluteMax请求BodyBytes；parameter absolute max request body bytes。
         */
        public void setAbsoluteMaxRequestBodyBytes(
                long absoluteMaxRequestBodyBytes) {
            this.absoluteMaxRequestBodyBytes = absoluteMaxRequestBodyBytes;
        }

        /**
         * 中文说明：执行 getBodyLogSampleBytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get body log sample bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getBodyLogSampleBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getBodyLogSampleBytes 的处理结果；returns the result of the operation.
         */
        public int getBodyLogSampleBytes() {
            return bodyLogSampleBytes;
        }

        /**
         * 中文说明：执行 setBodyLogSampleBytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set body log sample bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setBodyLogSampleBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param bodyLogSampleBytes 参数 bodyLogSampleBytes；parameter body log sample bytes。
         */
        public void setBodyLogSampleBytes(int bodyLogSampleBytes) {
            this.bodyLogSampleBytes = bodyLogSampleBytes;
        }

        /**
         * 中文说明：执行 getAbsoluteMaxBodyLogSampleBytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get absolute max body log sample bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getAbsoluteMaxBodyLogSampleBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getAbsoluteMaxBodyLogSampleBytes 的处理结果；returns the result of the operation.
         */
        public int getAbsoluteMaxBodyLogSampleBytes() {
            return absoluteMaxBodyLogSampleBytes;
        }

        /**
         * 中文说明：执行 setAbsoluteMaxBodyLogSampleBytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set absolute max body log sample bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setAbsoluteMaxBodyLogSampleBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param absoluteMaxBodyLogSampleBytes 参数 absoluteMaxBodyLogSampleBytes；parameter absolute max body log sample bytes。
         */
        public void setAbsoluteMaxBodyLogSampleBytes(
                int absoluteMaxBodyLogSampleBytes) {
            this.absoluteMaxBodyLogSampleBytes =
                    absoluteMaxBodyLogSampleBytes;
        }

        /**
         * 中文说明：执行 getIdle超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get idle timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getIdleTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getIdle超时 的处理结果；returns the result of the operation.
         */
        public Duration getIdleTimeout() {
            return idleTimeout;
        }

        /**
         * 中文说明：执行 setIdle超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set idle timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setIdleTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param idleTimeout 参数 idle超时；parameter idle timeout。
         */
        public void setIdleTimeout(Duration idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        /**
         * 中文说明：执行 getUpstream超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get upstream timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getUpstreamTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getUpstream超时 的处理结果；returns the result of the operation.
         */
        public Duration getUpstreamTimeout() {
            return upstreamTimeout;
        }

        /**
         * 中文说明：执行 setUpstream超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set upstream timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setUpstreamTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param upstreamTimeout 参数 upstream超时；parameter upstream timeout。
         */
        public void setUpstreamTimeout(Duration upstreamTimeout) {
            this.upstreamTimeout = upstreamTimeout;
        }

        /**
         * 中文说明：执行 getMaxConnect超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get max connect timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getMaxConnectTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMaxConnect超时 的处理结果；returns the result of the operation.
         */
        public Duration getMaxConnectTimeout() {
            return maxConnectTimeout;
        }

        /**
         * 中文说明：执行 setMaxConnect超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set max connect timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setMaxConnectTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maxConnectTimeout 参数 maxConnect超时；parameter max connect timeout。
         */
        public void setMaxConnectTimeout(Duration maxConnectTimeout) {
            this.maxConnectTimeout = maxConnectTimeout;
        }

        /**
         * 中文说明：执行 getMax响应Header超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get max response header timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getMaxResponseHeaderTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMax响应Header超时 的处理结果；returns the result of the operation.
         */
        public Duration getMaxResponseHeaderTimeout() {
            return maxResponseHeaderTimeout;
        }

        /**
         * 中文说明：执行 setMax响应Header超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set max response header timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setMaxResponseHeaderTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maxResponseHeaderTimeout 参数 max响应Header超时；parameter max response header timeout。
         */
        public void setMaxResponseHeaderTimeout(
                Duration maxResponseHeaderTimeout) {
            this.maxResponseHeaderTimeout = maxResponseHeaderTimeout;
        }

        /**
         * 中文说明：执行 getMaxStreamIdle超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get max stream idle timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getMaxStreamIdleTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMaxStreamIdle超时 的处理结果；returns the result of the operation.
         */
        public Duration getMaxStreamIdleTimeout() {
            return maxStreamIdleTimeout;
        }

        /**
         * 中文说明：执行 setMaxStreamIdle超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set max stream idle timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setMaxStreamIdleTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maxStreamIdleTimeout 参数 maxStreamIdle超时；parameter max stream idle timeout。
         */
        public void setMaxStreamIdleTimeout(Duration maxStreamIdleTimeout) {
            this.maxStreamIdleTimeout = maxStreamIdleTimeout;
        }

        /**
         * 中文说明：执行 getMaxTotal超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get max total timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getMaxTotalTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMaxTotal超时 的处理结果；returns the result of the operation.
         */
        public Duration getMaxTotalTimeout() {
            return maxTotalTimeout;
        }

        /**
         * 中文说明：执行 setMaxTotal超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set max total timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setMaxTotalTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maxTotalTimeout 参数 maxTotal超时；parameter max total timeout。
         */
        public void setMaxTotalTimeout(Duration maxTotalTimeout) {
            this.maxTotalTimeout = maxTotalTimeout;
        }

        /**
         * 中文说明：执行 getMaxWebSocketIdle超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get max websocket idle timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getMaxWebsocketIdleTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMaxWebSocketIdle超时 的处理结果；returns the result of the operation.
         */
        public Duration getMaxWebsocketIdleTimeout() {
            return maxWebsocketIdleTimeout;
        }

        /**
         * 中文说明：执行 setMaxWebSocketIdle超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set max websocket idle timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setMaxWebsocketIdleTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maxWebsocketIdleTimeout 参数 maxWebSocketIdle超时；parameter max websocket idle timeout。
         */
        public void setMaxWebsocketIdleTimeout(
                Duration maxWebsocketIdleTimeout) {
            this.maxWebsocketIdleTimeout = maxWebsocketIdleTimeout;
        }

        /**
         * 中文说明：执行 getMaxWebSocketFrameBytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get max websocket frame bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getMaxWebsocketFrameBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMaxWebSocketFrameBytes 的处理结果；returns the result of the operation.
         */
        public long getMaxWebsocketFrameBytes() {
            return maxWebsocketFrameBytes;
        }

        /**
         * 中文说明：执行 setMaxWebSocketFrameBytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set max websocket frame bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setMaxWebsocketFrameBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maxWebsocketFrameBytes 参数 maxWebSocketFrameBytes；parameter max websocket frame bytes。
         */
        public void setMaxWebsocketFrameBytes(long maxWebsocketFrameBytes) {
            this.maxWebsocketFrameBytes = maxWebsocketFrameBytes;
        }

        /**
         * 中文说明：执行 getDrain超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get drain timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getDrainTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getDrain超时 的处理结果；returns the result of the operation.
         */
        public Duration getDrainTimeout() {
            return drainTimeout;
        }

        /**
         * 中文说明：执行 setDrain超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set drain timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setDrainTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param drainTimeout 参数 drain超时；parameter drain timeout。
         */
        public void setDrainTimeout(Duration drainTimeout) {
            this.drainTimeout = drainTimeout;
        }

        /**
         * 中文说明：执行 getUpstreamMaxConnections 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get upstream max connections operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getUpstreamMaxConnections(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getUpstreamMaxConnections 的处理结果；returns the result of the operation.
         */
        public int getUpstreamMaxConnections() {
            return upstreamMaxConnections;
        }

        /**
         * 中文说明：执行 setUpstreamMaxConnections 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set upstream max connections operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setUpstreamMaxConnections(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param upstreamMaxConnections 参数 upstreamMaxConnections；parameter upstream max connections。
         */
        public void setUpstreamMaxConnections(int upstreamMaxConnections) {
            this.upstreamMaxConnections = upstreamMaxConnections;
        }

        /**
         * 中文说明：执行 getUpstreamPendingAcquireMaxCount 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get upstream pending acquire max count operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getUpstreamPendingAcquireMaxCount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getUpstreamPendingAcquireMaxCount 的处理结果；returns the result of the operation.
         */
        public int getUpstreamPendingAcquireMaxCount() {
            return upstreamPendingAcquireMaxCount;
        }

        /**
         * 中文说明：执行 setUpstreamPendingAcquireMaxCount 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set upstream pending acquire max count operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setUpstreamPendingAcquireMaxCount(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param upstreamPendingAcquireMaxCount 参数 upstreamPendingAcquireMaxCount；parameter upstream pending acquire max count。
         */
        public void setUpstreamPendingAcquireMaxCount(
                int upstreamPendingAcquireMaxCount) {
            this.upstreamPendingAcquireMaxCount =
                    upstreamPendingAcquireMaxCount;
        }

        /**
         * 中文说明：执行 getPublicTls 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get public tls operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getPublicTls(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getPublicTls 的处理结果；returns the result of the operation.
         */
        public Tls getPublicTls() {
            return publicTls;
        }

        /**
         * 中文说明：执行 setPublicTls 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set public tls operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setPublicTls(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param publicTls 参数 publicTls；parameter public tls。
         */
        public void setPublicTls(Tls publicTls) {
            this.publicTls = publicTls;
        }

        /**
         * 中文说明：执行 getInternalTls 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get internal tls operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.getInternalTls(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getInternalTls 的处理结果；returns the result of the operation.
         */
        public Tls getInternalTls() {
            return internalTls;
        }

        /**
         * 中文说明：执行 setInternalTls 操作；该方法是 {@code GatewayEngineRuntimeProperties.Http} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set internal tls operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Http} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Http.setInternalTls(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param internalTls 参数 internalTls；parameter internal tls。
         */
        public void setInternalTls(Tls internalTls) {
            this.internalTls = internalTls;
        }
    }

    /**
     * 中文说明：{@code Rpc} 是类型，位于当前 Gateway 模块的相关包中，负责Rpc相关的职责与边界。
     * English summary: {@code Rpc} is a type in the current Gateway module; it owns the rpc-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static class Rpc {

        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayEngineRuntimeProperties.Rpc} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code GatewayEngineRuntimeProperties.Rpc} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Rpc} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Rpc}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean enabled = true;

        /**
         * 中文说明：保存 port 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.Rpc} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by port; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.Rpc} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Rpc} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Rpc}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int port = 19090;

        /**
         * 中文说明：保存 advertisedHost 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties.Rpc} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by advertised host; its type is {@code String}, and {@code GatewayEngineRuntimeProperties.Rpc} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Rpc} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Rpc}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String advertisedHost = "127.0.0.1";

        /**
         * 中文说明：保存 服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties.Rpc} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by service name; its type is {@code String}, and {@code GatewayEngineRuntimeProperties.Rpc} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Rpc} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Rpc}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String serviceName = "egon-gateway-rpc";

        /**
         * 中文说明：保存 group 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties.Rpc} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by group; its type is {@code String}, and {@code GatewayEngineRuntimeProperties.Rpc} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Rpc} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Rpc}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String group = "default";

        /**
         * 中文说明：保存 version 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties.Rpc} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by version; its type is {@code String}, and {@code GatewayEngineRuntimeProperties.Rpc} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Rpc} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Rpc}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String version = "1.0.0";

        /**
         * 中文说明：保存 maxInbound消息Bytes 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.Rpc} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max inbound message bytes; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.Rpc} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Rpc} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Rpc}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int maxInboundMessageBytes = 4 * 1024 * 1024;

        /**
         * 中文说明：保存 maximum超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayEngineRuntimeProperties.Rpc} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by maximum timeout; its type is {@code Duration}, and {@code GatewayEngineRuntimeProperties.Rpc} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Rpc} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Rpc}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration maximumTimeout = Duration.ofSeconds(10);

        /**
         * 中文说明：保存 通道Drain超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayEngineRuntimeProperties.Rpc} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by channel drain timeout; its type is {@code Duration}, and {@code GatewayEngineRuntimeProperties.Rpc} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Rpc} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Rpc}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration channelDrainTimeout = Duration.ofSeconds(5);

        /**
         * 中文说明：保存 租约Seconds 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.Rpc} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by lease seconds; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.Rpc} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Rpc} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Rpc}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int leaseSeconds = 30;

        /**
         * 中文说明：保存 heartbeatIntervalSeconds 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.Rpc} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by heartbeat interval seconds; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.Rpc} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Rpc} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Rpc}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int heartbeatIntervalSeconds = 10;

        /**
         * 中文说明：保存 tls 对应的状态、依赖或配置值；字段类型为 {@code Tls}，由 {@code GatewayEngineRuntimeProperties.Rpc} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tls; its type is {@code Tls}, and {@code GatewayEngineRuntimeProperties.Rpc} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Rpc} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Rpc}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Tls tls = new Tls();

        /**
         * 中文说明：执行 isEnabled 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is enabled operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.isEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 isEnabled 的处理结果；returns the result of the operation.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 中文说明：执行 setEnabled 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set enabled operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.setEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param enabled 参数 enabled；parameter enabled。
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 中文说明：执行 getPort 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get port operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.getPort(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getPort 的处理结果；returns the result of the operation.
         */
        public int getPort() {
            return port;
        }

        /**
         * 中文说明：执行 setPort 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set port operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.setPort(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param port 参数 port；parameter port。
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * 中文说明：执行 getAdvertisedHost 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get advertised host operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.getAdvertisedHost(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getAdvertisedHost 的处理结果；returns the result of the operation.
         */
        public String getAdvertisedHost() {
            return advertisedHost;
        }

        /**
         * 中文说明：执行 setAdvertisedHost 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set advertised host operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.setAdvertisedHost(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param advertisedHost 参数 advertisedHost；parameter advertised host。
         */
        public void setAdvertisedHost(String advertisedHost) {
            this.advertisedHost = advertisedHost;
        }

        /**
         * 中文说明：执行 get服务Name 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get service name operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.getServiceName(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 get服务Name 的处理结果；returns the result of the operation.
         */
        public String getServiceName() {
            return serviceName;
        }

        /**
         * 中文说明：执行 set服务Name 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set service name operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.setServiceName(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param serviceName 参数 服务Name；parameter service name。
         */
        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        /**
         * 中文说明：执行 getGroup 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get group operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.getGroup(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getGroup 的处理结果；returns the result of the operation.
         */
        public String getGroup() {
            return group;
        }

        /**
         * 中文说明：执行 setGroup 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set group operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.setGroup(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param group 参数 group；parameter group。
         */
        public void setGroup(String group) {
            this.group = group;
        }

        /**
         * 中文说明：执行 getVersion 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get version operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.getVersion(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getVersion 的处理结果；returns the result of the operation.
         */
        public String getVersion() {
            return version;
        }

        /**
         * 中文说明：执行 setVersion 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set version operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.setVersion(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param version 参数 version；parameter version。
         */
        public void setVersion(String version) {
            this.version = version;
        }

        /**
         * 中文说明：执行 getMaxInbound消息Bytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get max inbound message bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.getMaxInboundMessageBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMaxInbound消息Bytes 的处理结果；returns the result of the operation.
         */
        public int getMaxInboundMessageBytes() {
            return maxInboundMessageBytes;
        }

        /**
         * 中文说明：执行 setMaxInbound消息Bytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set max inbound message bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.setMaxInboundMessageBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maxInboundMessageBytes 参数 maxInbound消息Bytes；parameter max inbound message bytes。
         */
        public void setMaxInboundMessageBytes(int maxInboundMessageBytes) {
            this.maxInboundMessageBytes = maxInboundMessageBytes;
        }

        /**
         * 中文说明：执行 getMaximum超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get maximum timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.getMaximumTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMaximum超时 的处理结果；returns the result of the operation.
         */
        public Duration getMaximumTimeout() {
            return maximumTimeout;
        }

        /**
         * 中文说明：执行 setMaximum超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set maximum timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.setMaximumTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maximumTimeout 参数 maximum超时；parameter maximum timeout。
         */
        public void setMaximumTimeout(Duration maximumTimeout) {
            this.maximumTimeout = maximumTimeout;
        }

        /**
         * 中文说明：执行 get通道Drain超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get channel drain timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.getChannelDrainTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 get通道Drain超时 的处理结果；returns the result of the operation.
         */
        public Duration getChannelDrainTimeout() {
            return channelDrainTimeout;
        }

        /**
         * 中文说明：执行 set通道Drain超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set channel drain timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.setChannelDrainTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param channelDrainTimeout 参数 通道Drain超时；parameter channel drain timeout。
         */
        public void setChannelDrainTimeout(Duration channelDrainTimeout) {
            this.channelDrainTimeout = channelDrainTimeout;
        }

        /**
         * 中文说明：执行 get租约Seconds 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get lease seconds operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.getLeaseSeconds(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 get租约Seconds 的处理结果；returns the result of the operation.
         */
        public int getLeaseSeconds() {
            return leaseSeconds;
        }

        /**
         * 中文说明：执行 set租约Seconds 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set lease seconds operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.setLeaseSeconds(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param leaseSeconds 参数 租约Seconds；parameter lease seconds。
         */
        public void setLeaseSeconds(int leaseSeconds) {
            this.leaseSeconds = leaseSeconds;
        }

        /**
         * 中文说明：执行 getHeartbeatIntervalSeconds 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get heartbeat interval seconds operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.getHeartbeatIntervalSeconds(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getHeartbeatIntervalSeconds 的处理结果；returns the result of the operation.
         */
        public int getHeartbeatIntervalSeconds() {
            return heartbeatIntervalSeconds;
        }

        /**
         * 中文说明：执行 setHeartbeatIntervalSeconds 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set heartbeat interval seconds operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.setHeartbeatIntervalSeconds(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param heartbeatIntervalSeconds 参数 heartbeatIntervalSeconds；parameter heartbeat interval seconds。
         */
        public void setHeartbeatIntervalSeconds(
                int heartbeatIntervalSeconds) {
            this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        }

        /**
         * 中文说明：执行 getTls 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get tls operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.getTls(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getTls 的处理结果；returns the result of the operation.
         */
        public Tls getTls() {
            return tls;
        }

        /**
         * 中文说明：执行 setTls 操作；该方法是 {@code GatewayEngineRuntimeProperties.Rpc} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set tls operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Rpc} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Rpc.setTls(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param tls 参数 tls；parameter tls。
         */
        public void setTls(Tls tls) {
            this.tls = tls;
        }
    }

    /**
     * 中文说明：{@code Tls} 是类型，位于当前 Gateway 模块的相关包中，负责Tls相关的职责与边界。
     * English summary: {@code Tls} is a type in the current Gateway module; it owns the tls-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static class Tls {

        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayEngineRuntimeProperties.Tls} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code GatewayEngineRuntimeProperties.Tls} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Tls} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Tls}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean enabled;

        /**
         * 中文说明：保存 developmentPlaintext 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayEngineRuntimeProperties.Tls} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by development plaintext; its type is {@code boolean}, and {@code GatewayEngineRuntimeProperties.Tls} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Tls} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Tls}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean developmentPlaintext;

        /**
         * 中文说明：保存 certificateChainPath 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties.Tls} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by certificate chain path; its type is {@code String}, and {@code GatewayEngineRuntimeProperties.Tls} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Tls} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Tls}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String certificateChainPath;

        /**
         * 中文说明：保存 private键Path 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties.Tls} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by private key path; its type is {@code String}, and {@code GatewayEngineRuntimeProperties.Tls} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Tls} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Tls}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String privateKeyPath;

        /**
         * 中文说明：保存 trustCertificateCollectionPath 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties.Tls} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by trust certificate collection path; its type is {@code String}, and {@code GatewayEngineRuntimeProperties.Tls} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Tls} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Tls}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String trustCertificateCollectionPath;

        /**
         * 中文说明：保存 客户端CertificateRequired 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayEngineRuntimeProperties.Tls} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by client certificate required; its type is {@code boolean}, and {@code GatewayEngineRuntimeProperties.Tls} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Tls} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Tls}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean clientCertificateRequired;

        /**
         * 中文说明：执行 isEnabled 操作；该方法是 {@code GatewayEngineRuntimeProperties.Tls} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is enabled operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Tls} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Tls.isEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 isEnabled 的处理结果；returns the result of the operation.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 中文说明：执行 setEnabled 操作；该方法是 {@code GatewayEngineRuntimeProperties.Tls} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set enabled operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Tls} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Tls.setEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param enabled 参数 enabled；parameter enabled。
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 中文说明：执行 isDevelopmentPlaintext 操作；该方法是 {@code GatewayEngineRuntimeProperties.Tls} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is development plaintext operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Tls} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Tls.isDevelopmentPlaintext(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 isDevelopmentPlaintext 的处理结果；returns the result of the operation.
         */
        public boolean isDevelopmentPlaintext() {
            return developmentPlaintext;
        }

        /**
         * 中文说明：执行 setDevelopmentPlaintext 操作；该方法是 {@code GatewayEngineRuntimeProperties.Tls} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set development plaintext operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Tls} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Tls.setDevelopmentPlaintext(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param developmentPlaintext 参数 developmentPlaintext；parameter development plaintext。
         */
        public void setDevelopmentPlaintext(boolean developmentPlaintext) {
            this.developmentPlaintext = developmentPlaintext;
        }

        /**
         * 中文说明：执行 getCertificateChainPath 操作；该方法是 {@code GatewayEngineRuntimeProperties.Tls} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get certificate chain path operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Tls} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Tls.getCertificateChainPath(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getCertificateChainPath 的处理结果；returns the result of the operation.
         */
        public String getCertificateChainPath() {
            return certificateChainPath;
        }

        /**
         * 中文说明：执行 setCertificateChainPath 操作；该方法是 {@code GatewayEngineRuntimeProperties.Tls} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set certificate chain path operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Tls} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Tls.setCertificateChainPath(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param certificateChainPath 参数 certificateChainPath；parameter certificate chain path。
         */
        public void setCertificateChainPath(String certificateChainPath) {
            this.certificateChainPath = certificateChainPath;
        }

        /**
         * 中文说明：执行 getPrivate键Path 操作；该方法是 {@code GatewayEngineRuntimeProperties.Tls} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get private key path operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Tls} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Tls.getPrivateKeyPath(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getPrivate键Path 的处理结果；returns the result of the operation.
         */
        public String getPrivateKeyPath() {
            return privateKeyPath;
        }

        /**
         * 中文说明：执行 setPrivate键Path 操作；该方法是 {@code GatewayEngineRuntimeProperties.Tls} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set private key path operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Tls} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Tls.setPrivateKeyPath(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param privateKeyPath 参数 private键Path；parameter private key path。
         */
        public void setPrivateKeyPath(String privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
        }

        /**
         * 中文说明：执行 getTrustCertificateCollectionPath 操作；该方法是 {@code GatewayEngineRuntimeProperties.Tls} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get trust certificate collection path operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Tls} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Tls.getTrustCertificateCollectionPath(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getTrustCertificateCollectionPath 的处理结果；returns the result of the operation.
         */
        public String getTrustCertificateCollectionPath() {
            return trustCertificateCollectionPath;
        }

        /**
         * 中文说明：执行 setTrustCertificateCollectionPath 操作；该方法是 {@code GatewayEngineRuntimeProperties.Tls} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set trust certificate collection path operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Tls} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Tls.setTrustCertificateCollectionPath(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param trustCertificateCollectionPath 参数 trustCertificateCollectionPath；parameter trust certificate collection path。
         */
        public void setTrustCertificateCollectionPath(
                String trustCertificateCollectionPath) {
            this.trustCertificateCollectionPath =
                    trustCertificateCollectionPath;
        }

        /**
         * 中文说明：执行 is客户端CertificateRequired 操作；该方法是 {@code GatewayEngineRuntimeProperties.Tls} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is client certificate required operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Tls} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Tls.isClientCertificateRequired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 is客户端CertificateRequired 的处理结果；returns the result of the operation.
         */
        public boolean isClientCertificateRequired() {
            return clientCertificateRequired;
        }

        /**
         * 中文说明：执行 set客户端CertificateRequired 操作；该方法是 {@code GatewayEngineRuntimeProperties.Tls} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set client certificate required operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Tls} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Tls.setClientCertificateRequired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param clientCertificateRequired 参数 客户端CertificateRequired；parameter client certificate required。
         */
        public void setClientCertificateRequired(
                boolean clientCertificateRequired) {
            this.clientCertificateRequired = clientCertificateRequired;
        }
    }

    /**
     * 中文说明：{@code ActiveHealth} 是类型，位于当前 Gateway 模块的相关包中，负责Active健康相关的职责与边界。
     * English summary: {@code ActiveHealth} is a type in the current Gateway module; it owns the active health-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static class ActiveHealth {

        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayEngineRuntimeProperties.ActiveHealth} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code GatewayEngineRuntimeProperties.ActiveHealth} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.ActiveHealth}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean enabled;

        /**
         * 中文说明：保存 interval 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayEngineRuntimeProperties.ActiveHealth} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by interval; its type is {@code Duration}, and {@code GatewayEngineRuntimeProperties.ActiveHealth} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.ActiveHealth}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration interval = Duration.ofSeconds(10);

        /**
         * 中文说明：保存 jitterRatio 对应的状态、依赖或配置值；字段类型为 {@code double}，由 {@code GatewayEngineRuntimeProperties.ActiveHealth} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by jitter ratio; its type is {@code double}, and {@code GatewayEngineRuntimeProperties.ActiveHealth} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.ActiveHealth}; do not couple callers to its representation when the owning type exposes an API.
         */
        private double jitterRatio = 0.2;

        /**
         * 中文说明：保存 超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayEngineRuntimeProperties.ActiveHealth} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by timeout; its type is {@code Duration}, and {@code GatewayEngineRuntimeProperties.ActiveHealth} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.ActiveHealth}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration timeout = Duration.ofSeconds(2);

        /**
         * 中文说明：保存 maximumConcurrency 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.ActiveHealth} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by maximum concurrency; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.ActiveHealth} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.ActiveHealth}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int maximumConcurrency = 16;

        /**
         * 中文说明：保存 failureThreshold 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.ActiveHealth} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by failure threshold; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.ActiveHealth} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.ActiveHealth}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int failureThreshold = 2;

        /**
         * 中文说明：保存 successThreshold 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.ActiveHealth} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by success threshold; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.ActiveHealth} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.ActiveHealth}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int successThreshold = 2;

        /**
         * 中文说明：保存 http方法 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties.ActiveHealth} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by http method; its type is {@code String}, and {@code GatewayEngineRuntimeProperties.ActiveHealth} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.ActiveHealth}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String httpMethod = "GET";

        /**
         * 中文说明：保存 httpPath 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties.ActiveHealth} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by http path; its type is {@code String}, and {@code GatewayEngineRuntimeProperties.ActiveHealth} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.ActiveHealth}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String httpPath = "/actuator/health";

        /**
         * 中文说明：保存 httpSuccessStatuses 对应的状态、依赖或配置值；字段类型为 {@code List<Integer>}，由 {@code GatewayEngineRuntimeProperties.ActiveHealth} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by http success statuses; its type is {@code List<Integer>}, and {@code GatewayEngineRuntimeProperties.ActiveHealth} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.ActiveHealth}; do not couple callers to its representation when the owning type exposes an API.
         */
        private List<Integer> httpSuccessStatuses = List.of(200);

        /**
         * 中文说明：保存 rpc服务Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties.ActiveHealth} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by rpc service name; its type is {@code String}, and {@code GatewayEngineRuntimeProperties.ActiveHealth} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.ActiveHealth}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String rpcServiceName = "";

        /**
         * 中文说明：保存 rpcConnectFallback 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayEngineRuntimeProperties.ActiveHealth} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by rpc connect fallback; its type is {@code boolean}, and {@code GatewayEngineRuntimeProperties.ActiveHealth} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.ActiveHealth}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean rpcConnectFallback = true;

        /**
         * 中文说明：执行 isEnabled 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is enabled operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.isEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 isEnabled 的处理结果；returns the result of the operation.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 中文说明：执行 setEnabled 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set enabled operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.setEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param enabled 参数 enabled；parameter enabled。
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 中文说明：执行 getInterval 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get interval operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.getInterval(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getInterval 的处理结果；returns the result of the operation.
         */
        public Duration getInterval() {
            return interval;
        }

        /**
         * 中文说明：执行 setInterval 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set interval operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.setInterval(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param interval 参数 interval；parameter interval。
         */
        public void setInterval(Duration interval) {
            this.interval = interval;
        }

        /**
         * 中文说明：执行 getJitterRatio 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get jitter ratio operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.getJitterRatio(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getJitterRatio 的处理结果；returns the result of the operation.
         */
        public double getJitterRatio() {
            return jitterRatio;
        }

        /**
         * 中文说明：执行 setJitterRatio 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set jitter ratio operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.setJitterRatio(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param jitterRatio 参数 jitterRatio；parameter jitter ratio。
         */
        public void setJitterRatio(double jitterRatio) {
            this.jitterRatio = jitterRatio;
        }

        /**
         * 中文说明：执行 get超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.getTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 get超时 的处理结果；returns the result of the operation.
         */
        public Duration getTimeout() {
            return timeout;
        }

        /**
         * 中文说明：执行 set超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.setTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param timeout 参数 超时；parameter timeout。
         */
        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        /**
         * 中文说明：执行 getMaximumConcurrency 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get maximum concurrency operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.getMaximumConcurrency(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMaximumConcurrency 的处理结果；returns the result of the operation.
         */
        public int getMaximumConcurrency() {
            return maximumConcurrency;
        }

        /**
         * 中文说明：执行 setMaximumConcurrency 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set maximum concurrency operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.setMaximumConcurrency(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maximumConcurrency 参数 maximumConcurrency；parameter maximum concurrency。
         */
        public void setMaximumConcurrency(int maximumConcurrency) {
            this.maximumConcurrency = maximumConcurrency;
        }

        /**
         * 中文说明：执行 getFailureThreshold 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get failure threshold operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.getFailureThreshold(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getFailureThreshold 的处理结果；returns the result of the operation.
         */
        public int getFailureThreshold() {
            return failureThreshold;
        }

        /**
         * 中文说明：执行 setFailureThreshold 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set failure threshold operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.setFailureThreshold(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param failureThreshold 参数 failureThreshold；parameter failure threshold。
         */
        public void setFailureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        /**
         * 中文说明：执行 getSuccessThreshold 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get success threshold operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.getSuccessThreshold(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getSuccessThreshold 的处理结果；returns the result of the operation.
         */
        public int getSuccessThreshold() {
            return successThreshold;
        }

        /**
         * 中文说明：执行 setSuccessThreshold 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set success threshold operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.setSuccessThreshold(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param successThreshold 参数 successThreshold；parameter success threshold。
         */
        public void setSuccessThreshold(int successThreshold) {
            this.successThreshold = successThreshold;
        }

        /**
         * 中文说明：执行 getHttp方法 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get http method operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.getHttpMethod(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getHttp方法 的处理结果；returns the result of the operation.
         */
        public String getHttpMethod() {
            return httpMethod;
        }

        /**
         * 中文说明：执行 setHttp方法 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set http method operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.setHttpMethod(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param httpMethod 参数 http方法；parameter http method。
         */
        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }

        /**
         * 中文说明：执行 getHttpPath 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get http path operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.getHttpPath(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getHttpPath 的处理结果；returns the result of the operation.
         */
        public String getHttpPath() {
            return httpPath;
        }

        /**
         * 中文说明：执行 setHttpPath 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set http path operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.setHttpPath(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param httpPath 参数 httpPath；parameter http path。
         */
        public void setHttpPath(String httpPath) {
            this.httpPath = httpPath;
        }

        /**
         * 中文说明：执行 getHttpSuccessStatuses 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get http success statuses operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.getHttpSuccessStatuses(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getHttpSuccessStatuses 的处理结果；returns the result of the operation.
         */
        public List<Integer> getHttpSuccessStatuses() {
            return httpSuccessStatuses;
        }

        /**
         * 中文说明：执行 setHttpSuccessStatuses 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set http success statuses operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.setHttpSuccessStatuses(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param httpSuccessStatuses 参数 httpSuccessStatuses；parameter http success statuses。
         */
        public void setHttpSuccessStatuses(
                List<Integer> httpSuccessStatuses) {
            this.httpSuccessStatuses = httpSuccessStatuses;
        }

        /**
         * 中文说明：执行 getRpc服务Name 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get rpc service name operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.getRpcServiceName(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getRpc服务Name 的处理结果；returns the result of the operation.
         */
        public String getRpcServiceName() {
            return rpcServiceName;
        }

        /**
         * 中文说明：执行 setRpc服务Name 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set rpc service name operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.setRpcServiceName(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param rpcServiceName 参数 rpc服务Name；parameter rpc service name。
         */
        public void setRpcServiceName(String rpcServiceName) {
            this.rpcServiceName = rpcServiceName;
        }

        /**
         * 中文说明：执行 isRpcConnectFallback 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is rpc connect fallback operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.isRpcConnectFallback(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 isRpcConnectFallback 的处理结果；returns the result of the operation.
         */
        public boolean isRpcConnectFallback() {
            return rpcConnectFallback;
        }

        /**
         * 中文说明：执行 setRpcConnectFallback 操作；该方法是 {@code GatewayEngineRuntimeProperties.ActiveHealth} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set rpc connect fallback operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.ActiveHealth} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.ActiveHealth.setRpcConnectFallback(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param rpcConnectFallback 参数 rpcConnectFallback；parameter rpc connect fallback。
         */
        public void setRpcConnectFallback(
                boolean rpcConnectFallback) {
            this.rpcConnectFallback = rpcConnectFallback;
        }
    }

    /**
     * 中文说明：{@code Kafka} 是类型，位于当前 Gateway 模块的相关包中，负责Kafka相关的职责与边界。
     * English summary: {@code Kafka} is a type in the current Gateway module; it owns the kafka-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static class Kafka {

        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayEngineRuntimeProperties.Kafka} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code GatewayEngineRuntimeProperties.Kafka} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Kafka} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Kafka}; do not couple callers to its representation when the owning type exposes an API.
         */
        private boolean enabled;

        /**
         * 中文说明：保存 bootstrapServers 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties.Kafka} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by bootstrap servers; its type is {@code String}, and {@code GatewayEngineRuntimeProperties.Kafka} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Kafka} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Kafka}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String bootstrapServers = "127.0.0.1:9092";

        /**
         * 中文说明：保存 topic 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayEngineRuntimeProperties.Kafka} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by topic; its type is {@code String}, and {@code GatewayEngineRuntimeProperties.Kafka} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Kafka} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Kafka}; do not couple callers to its representation when the owning type exposes an API.
         */
        private String topic = "egon.gateway.call.v1";

        /**
         * 中文说明：保存 maxQueuedEvents 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code GatewayEngineRuntimeProperties.Kafka} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max queued events; its type is {@code int}, and {@code GatewayEngineRuntimeProperties.Kafka} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Kafka} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Kafka}; do not couple callers to its representation when the owning type exposes an API.
         */
        private int maxQueuedEvents = 8192;

        /**
         * 中文说明：保存 maxQueuedBytes 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code GatewayEngineRuntimeProperties.Kafka} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max queued bytes; its type is {@code long}, and {@code GatewayEngineRuntimeProperties.Kafka} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Kafka} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Kafka}; do not couple callers to its representation when the owning type exposes an API.
         */
        private long maxQueuedBytes = 32L * 1024 * 1024;

        /**
         * 中文说明：保存 delivery超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayEngineRuntimeProperties.Kafka} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by delivery timeout; its type is {@code Duration}, and {@code GatewayEngineRuntimeProperties.Kafka} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Kafka} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Kafka}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration deliveryTimeout = Duration.ofSeconds(10);

        /**
         * 中文说明：保存 shutdownDrain 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code GatewayEngineRuntimeProperties.Kafka} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by shutdown drain; its type is {@code Duration}, and {@code GatewayEngineRuntimeProperties.Kafka} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Kafka} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Kafka}; do not couple callers to its representation when the owning type exposes an API.
         */
        private Duration shutdownDrain = Duration.ofSeconds(5);

        /**
         * 中文说明：执行 isEnabled 操作；该方法是 {@code GatewayEngineRuntimeProperties.Kafka} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the is enabled operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Kafka} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Kafka.isEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 isEnabled 的处理结果；returns the result of the operation.
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 中文说明：执行 setEnabled 操作；该方法是 {@code GatewayEngineRuntimeProperties.Kafka} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set enabled operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Kafka} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Kafka.setEnabled(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param enabled 参数 enabled；parameter enabled。
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 中文说明：执行 getBootstrapServers 操作；该方法是 {@code GatewayEngineRuntimeProperties.Kafka} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get bootstrap servers operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Kafka} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Kafka.getBootstrapServers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getBootstrapServers 的处理结果；returns the result of the operation.
         */
        public String getBootstrapServers() {
            return bootstrapServers;
        }

        /**
         * 中文说明：执行 setBootstrapServers 操作；该方法是 {@code GatewayEngineRuntimeProperties.Kafka} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set bootstrap servers operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Kafka} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Kafka.setBootstrapServers(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param bootstrapServers 参数 bootstrapServers；parameter bootstrap servers。
         */
        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        /**
         * 中文说明：执行 getTopic 操作；该方法是 {@code GatewayEngineRuntimeProperties.Kafka} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get topic operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Kafka} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Kafka.getTopic(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getTopic 的处理结果；returns the result of the operation.
         */
        public String getTopic() {
            return topic;
        }

        /**
         * 中文说明：执行 setTopic 操作；该方法是 {@code GatewayEngineRuntimeProperties.Kafka} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set topic operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Kafka} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Kafka.setTopic(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param topic 参数 topic；parameter topic。
         */
        public void setTopic(String topic) {
            this.topic = topic;
        }

        /**
         * 中文说明：执行 getMaxQueuedEvents 操作；该方法是 {@code GatewayEngineRuntimeProperties.Kafka} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get max queued events operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Kafka} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Kafka.getMaxQueuedEvents(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMaxQueuedEvents 的处理结果；returns the result of the operation.
         */
        public int getMaxQueuedEvents() {
            return maxQueuedEvents;
        }

        /**
         * 中文说明：执行 setMaxQueuedEvents 操作；该方法是 {@code GatewayEngineRuntimeProperties.Kafka} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set max queued events operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Kafka} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Kafka.setMaxQueuedEvents(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maxQueuedEvents 参数 maxQueuedEvents；parameter max queued events。
         */
        public void setMaxQueuedEvents(int maxQueuedEvents) {
            this.maxQueuedEvents = maxQueuedEvents;
        }

        /**
         * 中文说明：执行 getMaxQueuedBytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Kafka} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get max queued bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Kafka} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Kafka.getMaxQueuedBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getMaxQueuedBytes 的处理结果；returns the result of the operation.
         */
        public long getMaxQueuedBytes() {
            return maxQueuedBytes;
        }

        /**
         * 中文说明：执行 setMaxQueuedBytes 操作；该方法是 {@code GatewayEngineRuntimeProperties.Kafka} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set max queued bytes operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Kafka} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Kafka.setMaxQueuedBytes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param maxQueuedBytes 参数 maxQueuedBytes；parameter max queued bytes。
         */
        public void setMaxQueuedBytes(long maxQueuedBytes) {
            this.maxQueuedBytes = maxQueuedBytes;
        }

        /**
         * 中文说明：执行 getDelivery超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Kafka} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get delivery timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Kafka} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Kafka.getDeliveryTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getDelivery超时 的处理结果；returns the result of the operation.
         */
        public Duration getDeliveryTimeout() {
            return deliveryTimeout;
        }

        /**
         * 中文说明：执行 setDelivery超时 操作；该方法是 {@code GatewayEngineRuntimeProperties.Kafka} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set delivery timeout operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Kafka} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Kafka.setDeliveryTimeout(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param deliveryTimeout 参数 delivery超时；parameter delivery timeout。
         */
        public void setDeliveryTimeout(Duration deliveryTimeout) {
            this.deliveryTimeout = deliveryTimeout;
        }

        /**
         * 中文说明：执行 getShutdownDrain 操作；该方法是 {@code GatewayEngineRuntimeProperties.Kafka} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get shutdown drain operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Kafka} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Kafka.getShutdownDrain(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getShutdownDrain 的处理结果；returns the result of the operation.
         */
        public Duration getShutdownDrain() {
            return shutdownDrain;
        }

        /**
         * 中文说明：执行 setShutdownDrain 操作；该方法是 {@code GatewayEngineRuntimeProperties.Kafka} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set shutdown drain operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Kafka} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Kafka.setShutdownDrain(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param shutdownDrain 参数 shutdownDrain；parameter shutdown drain。
         */
        public void setShutdownDrain(Duration shutdownDrain) {
            this.shutdownDrain = shutdownDrain;
        }
    }

    /**
     * 中文说明：{@code Security} 是类型，位于当前 Gateway 模块的相关包中，负责安全相关的职责与边界。
     * English summary: {@code Security} is a type in the current Gateway module; it owns the security-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     */
    public static class Security {

        /**
         * 中文说明：保存 trusted代理Cidrs 对应的状态、依赖或配置值；字段类型为 {@code List<String>}，由 {@code GatewayEngineRuntimeProperties.Security} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by trusted proxy cidrs; its type is {@code List<String>}, and {@code GatewayEngineRuntimeProperties.Security} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayEngineRuntimeProperties.Security} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayEngineRuntimeProperties.Security}; do not couple callers to its representation when the owning type exposes an API.
         */
        private List<String> trustedProxyCidrs = new ArrayList<>();

        /**
         * 中文说明：执行 getTrusted代理Cidrs 操作；该方法是 {@code GatewayEngineRuntimeProperties.Security} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the get trusted proxy cidrs operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Security} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Security.getTrustedProxyCidrs(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @return 返回 getTrusted代理Cidrs 的处理结果；returns the result of the operation.
         */
        public List<String> getTrustedProxyCidrs() {
            return trustedProxyCidrs;
        }

        /**
         * 中文说明：执行 setTrusted代理Cidrs 操作；该方法是 {@code GatewayEngineRuntimeProperties.Security} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
         * English summary: Executes the set trusted proxy cidrs operation; this method is the invocation entry point on {@code GatewayEngineRuntimeProperties.Security} and performs the corresponding runtime, management, or protocol work.
         *
         * 用法 / Usage: 调用方式 / Usage: {@code GatewayEngineRuntimeProperties.Security.setTrustedProxyCidrs(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
         * @param trustedProxyCidrs 参数 trusted代理Cidrs；parameter trusted proxy cidrs。
         */
        public void setTrustedProxyCidrs(List<String> trustedProxyCidrs) {
            this.trustedProxyCidrs = trustedProxyCidrs;
        }
    }
}
