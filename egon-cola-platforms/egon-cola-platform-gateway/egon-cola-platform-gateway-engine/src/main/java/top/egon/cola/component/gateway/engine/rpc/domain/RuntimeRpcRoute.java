package top.egon.cola.component.gateway.engine.rpc.domain;

import top.egon.cola.component.gateway.core.provider.ProviderProtocolType;
import top.egon.cola.component.gateway.core.provider.ProviderServiceKey;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * 中文说明：{@code RuntimeRpcRoute} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责运行时Rpc路由相关的职责与边界。
 * English summary: {@code RuntimeRpcRoute} is an immutable data carrier in the current Gateway module; it owns the runtime rpc route-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param routeId 参数 路由Id；parameter route id。
 * @param operationId 参数 操作Id；parameter operation id。
 * @param fullMethodName 参数 full方法Name；parameter full method name。
 * @param targetService 参数 target服务；parameter target service。
 * @param requestType 参数 请求Type；parameter request type。
 * @param responseType 参数 响应Type；parameter response type。
 * @param descriptorSha256 参数 descriptorSha256；parameter descriptor sha256。
 * @param policyRefs 参数 策略Refs；parameter policy refs。
 * @param responseMode 参数 响应Mode；parameter response mode。
 * @param idempotent 参数 idempotent；parameter idempotent。
 * @param timeout 参数 超时；parameter timeout。
 */
public record RuntimeRpcRoute(
        /**
         * 中文说明：保存 路由Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuntimeRpcRoute} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by route id; its type is {@code String}, and {@code RuntimeRpcRoute} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeRpcRoute} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeRpcRoute}; do not couple callers to its representation when the owning type exposes an API.
         */
        String routeId,
        /**
         * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuntimeRpcRoute} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code RuntimeRpcRoute} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeRpcRoute} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeRpcRoute}; do not couple callers to its representation when the owning type exposes an API.
         */
        String operationId,
        /**
         * 中文说明：保存 full方法Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuntimeRpcRoute} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by full method name; its type is {@code String}, and {@code RuntimeRpcRoute} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeRpcRoute} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeRpcRoute}; do not couple callers to its representation when the owning type exposes an API.
         */
        String fullMethodName,
        /**
         * 中文说明：保存 target服务 对应的状态、依赖或配置值；字段类型为 {@code ProviderServiceKey}，由 {@code RuntimeRpcRoute} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by target service; its type is {@code ProviderServiceKey}, and {@code RuntimeRpcRoute} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeRpcRoute} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeRpcRoute}; do not couple callers to its representation when the owning type exposes an API.
         */
        ProviderServiceKey targetService,
        /**
         * 中文说明：保存 请求Type 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuntimeRpcRoute} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by request type; its type is {@code String}, and {@code RuntimeRpcRoute} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeRpcRoute} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeRpcRoute}; do not couple callers to its representation when the owning type exposes an API.
         */
        String requestType,
        /**
         * 中文说明：保存 响应Type 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuntimeRpcRoute} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by response type; its type is {@code String}, and {@code RuntimeRpcRoute} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeRpcRoute} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeRpcRoute}; do not couple callers to its representation when the owning type exposes an API.
         */
        String responseType,
        /**
         * 中文说明：保存 descriptorSha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code RuntimeRpcRoute} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by descriptor sha256; its type is {@code String}, and {@code RuntimeRpcRoute} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeRpcRoute} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeRpcRoute}; do not couple callers to its representation when the owning type exposes an API.
         */
        String descriptorSha256,
        /**
         * 中文说明：保存 策略Refs 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code RuntimeRpcRoute} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by policy refs; its type is {@code Set<String>}, and {@code RuntimeRpcRoute} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeRpcRoute} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeRpcRoute}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> policyRefs,
        /**
         * 中文说明：保存 响应Mode 对应的状态、依赖或配置值；字段类型为 {@code GatewayResponseMode}，由 {@code RuntimeRpcRoute} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by response mode; its type is {@code GatewayResponseMode}, and {@code RuntimeRpcRoute} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeRpcRoute} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeRpcRoute}; do not couple callers to its representation when the owning type exposes an API.
         */
        GatewayResponseMode responseMode,
        /**
         * 中文说明：保存 idempotent 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code RuntimeRpcRoute} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by idempotent; its type is {@code boolean}, and {@code RuntimeRpcRoute} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeRpcRoute} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeRpcRoute}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean idempotent,
        /**
         * 中文说明：保存 超时 对应的状态、依赖或配置值；字段类型为 {@code Duration}，由 {@code RuntimeRpcRoute} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by timeout; its type is {@code Duration}, and {@code RuntimeRpcRoute} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code RuntimeRpcRoute} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code RuntimeRpcRoute}; do not couple callers to its representation when the owning type exposes an API.
         */
        Duration timeout
) {

    /**
     * 中文说明：创建 {@code RuntimeRpcRoute} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code RuntimeRpcRoute} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param routeId 参数 路由Id；parameter route id。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param fullMethodName 参数 full方法Name；parameter full method name。
     * @param targetService 参数 target服务；parameter target service。
     * @param requestType 参数 请求Type；parameter request type。
     * @param responseType 参数 响应Type；parameter response type。
     * @param descriptorSha256 参数 descriptorSha256；parameter descriptor sha256。
     * @param policyRefs 参数 策略Refs；parameter policy refs。
     * @param responseMode 参数 响应Mode；parameter response mode。
     * @param idempotent 参数 idempotent；parameter idempotent。
     * @param timeout 参数 超时；parameter timeout。
     */
    public RuntimeRpcRoute {
        routeId = required(routeId, "routeId");
        operationId = required(operationId, "operationId");
        fullMethodName = required(fullMethodName, "fullMethodName");
        if (!fullMethodName.contains("/")) {
            throw new IllegalArgumentException(
                    "fullMethodName must be service/method"
            );
        }
        targetService = Objects.requireNonNull(
                targetService,
                "targetService"
        );
        if (targetService.protocolType() != ProviderProtocolType.RPC) {
            throw new IllegalArgumentException(
                    "RPC route target must use RPC provider protocol"
            );
        }
        requestType = required(requestType, "requestType");
        responseType = required(responseType, "responseType");
        descriptorSha256 = required(descriptorSha256, "descriptorSha256");
        policyRefs = Set.copyOf(Objects.requireNonNull(policyRefs, "policyRefs"));
        responseMode = Objects.requireNonNull(responseMode, "responseMode");
        timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code RuntimeRpcRoute} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code RuntimeRpcRoute} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code RuntimeRpcRoute.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
