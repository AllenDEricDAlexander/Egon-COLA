package top.egon.cola.component.gateway.engine.traffic;

import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code GatewayTrafficContext} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责网关流量Context相关的职责与边界。
 * English summary: {@code GatewayTrafficContext} is an immutable data carrier in the current Gateway module; it owns the gateway traffic context-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param operationId 参数 操作Id；parameter operation id。
 * @param routeId 参数 路由Id；parameter route id。
 * @param applicationCode 参数 applicationCode；parameter application code。
 * @param callerId 参数 callerId；parameter caller id。
 * @param clientIp 参数 客户端Ip；parameter client ip。
 * @param providerService 参数 提供方服务；parameter provider service。
 * @param providerInstance 参数 提供方Instance；parameter provider instance。
 * @param approvedHeaders 参数 approvedHeaders；parameter approved headers。
 * @param pathVariables 参数 pathVariables；parameter path variables。
 * @param queryParameters 参数 queryParameters；parameter query parameters。
 */
public record GatewayTrafficContext(
        /**
         * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTrafficContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code GatewayTrafficContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        String operationId,
        /**
         * 中文说明：保存 路由Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTrafficContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by route id; its type is {@code String}, and {@code GatewayTrafficContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        String routeId,
        /**
         * 中文说明：保存 applicationCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTrafficContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by application code; its type is {@code String}, and {@code GatewayTrafficContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        String applicationCode,
        /**
         * 中文说明：保存 callerId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTrafficContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by caller id; its type is {@code String}, and {@code GatewayTrafficContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        String callerId,
        /**
         * 中文说明：保存 客户端Ip 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTrafficContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by client ip; its type is {@code String}, and {@code GatewayTrafficContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        String clientIp,
        /**
         * 中文说明：保存 提供方服务 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTrafficContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider service; its type is {@code String}, and {@code GatewayTrafficContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        String providerService,
        /**
         * 中文说明：保存 提供方Instance 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayTrafficContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider instance; its type is {@code String}, and {@code GatewayTrafficContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        String providerInstance,
        /**
         * 中文说明：保存 approvedHeaders 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code GatewayTrafficContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by approved headers; its type is {@code Map<String, String>}, and {@code GatewayTrafficContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, String> approvedHeaders,
        /**
         * 中文说明：保存 pathVariables 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code GatewayTrafficContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by path variables; its type is {@code Map<String, String>}, and {@code GatewayTrafficContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, String> pathVariables,
        /**
         * 中文说明：保存 queryParameters 对应的状态、依赖或配置值；字段类型为 {@code Map<String, String>}，由 {@code GatewayTrafficContext} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by query parameters; its type is {@code Map<String, String>}, and {@code GatewayTrafficContext} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code GatewayTrafficContext} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayTrafficContext}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, String> queryParameters
) {

    /**
     * 中文说明：创建 {@code GatewayTrafficContext} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code GatewayTrafficContext} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param operationId 参数 操作Id；parameter operation id。
     * @param routeId 参数 路由Id；parameter route id。
     * @param applicationCode 参数 applicationCode；parameter application code。
     * @param callerId 参数 callerId；parameter caller id。
     * @param clientIp 参数 客户端Ip；parameter client ip。
     * @param providerService 参数 提供方服务；parameter provider service。
     * @param providerInstance 参数 提供方Instance；parameter provider instance。
     * @param approvedHeaders 参数 approvedHeaders；parameter approved headers。
     * @param pathVariables 参数 pathVariables；parameter path variables。
     * @param queryParameters 参数 queryParameters；parameter query parameters。
     */
    public GatewayTrafficContext {
        approvedHeaders = Map.copyOf(Objects.requireNonNull(
                approvedHeaders,
                "approvedHeaders"
        ));
        pathVariables = Map.copyOf(Objects.requireNonNull(
                pathVariables,
                "pathVariables"
        ));
        queryParameters = Map.copyOf(Objects.requireNonNull(
                queryParameters,
                "queryParameters"
        ));
    }

    /**
     * 中文说明：执行 值 操作；该方法是 {@code GatewayTrafficContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the value operation; this method is the invocation entry point on {@code GatewayTrafficContext} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficContext.value(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param field 参数 field；parameter field。
     * @return 返回 值 的处理结果；returns the result of the operation.
     */
    String value(String field) {
        return switch (field) {
            case "operationId" -> operationId;
            case "routeId" -> routeId;
            case "applicationCode" -> applicationCode;
            case "callerId" -> callerId;
            case "clientIp" -> clientIp;
            case "providerService" -> providerService;
            case "providerInstance" -> providerInstance;
            default -> dynamic(field);
        };
    }

    /**
     * 中文说明：执行 dynamic 操作；该方法是 {@code GatewayTrafficContext} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dynamic operation; this method is the invocation entry point on {@code GatewayTrafficContext} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayTrafficContext.dynamic(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param field 参数 field；parameter field。
     * @return 返回 dynamic 的处理结果；returns the result of the operation.
     */
    private String dynamic(String field) {
        int separator = field.indexOf('.');
        if (separator < 1 || separator == field.length() - 1) {
            throw new IllegalArgumentException(
                    "unsupported traffic key field " + field
            );
        }
        String namespace = field.substring(0, separator);
        String name = field.substring(separator + 1);
        return switch (namespace) {
            case "header" -> approvedHeaders.get(name);
            case "path" -> pathVariables.get(name);
            case "query" -> queryParameters.get(name);
            default -> throw new IllegalArgumentException(
                    "unsupported traffic key namespace " + namespace
            );
        };
    }
}
