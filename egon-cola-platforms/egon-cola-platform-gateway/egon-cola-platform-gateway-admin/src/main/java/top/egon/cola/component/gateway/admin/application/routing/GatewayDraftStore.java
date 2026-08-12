package top.egon.cola.component.gateway.admin.application.routing;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code GatewayDraftStore} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关草稿存储相关的职责与边界。
 * English summary: {@code GatewayDraftStore} is an interface contract in the current Gateway module; it owns the gateway draft store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayDraftStore {

    /**
     * 中文说明：执行 routes 操作；该方法是 {@code GatewayDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the routes operation; this method is the invocation entry point on {@code GatewayDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftStore.routes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 routes 的处理结果；returns the result of the operation.
     */
    List<RouteDraft> routes(String gatewayGroupId);

    /**
     * 中文说明：执行 policies 操作；该方法是 {@code GatewayDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the policies operation; this method is the invocation entry point on {@code GatewayDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftStore.policies(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 policies 的处理结果；returns the result of the operation.
     */
    List<PolicyDraft> policies(String gatewayGroupId);

    /**
     * 中文说明：执行 upsert路由 操作；该方法是 {@code GatewayDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the upsert route operation; this method is the invocation entry point on {@code GatewayDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftStore.upsertRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param route 参数 路由；parameter route。
     */
    void upsertRoute(RouteDraft route);

    /**
     * 中文说明：执行 delete路由 操作；该方法是 {@code GatewayDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete route operation; this method is the invocation entry point on {@code GatewayDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftStore.deleteRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param routeId 参数 路由Id；parameter route id。
     */
    void deleteRoute(String gatewayGroupId, String routeId);

    /**
     * 中文说明：执行 upsert策略 操作；该方法是 {@code GatewayDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the upsert policy operation; this method is the invocation entry point on {@code GatewayDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftStore.upsertPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     */
    void upsertPolicy(PolicyDraft policy);

    /**
     * 中文说明：执行 delete策略 操作；该方法是 {@code GatewayDraftStore} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete policy operation; this method is the invocation entry point on {@code GatewayDraftStore} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftStore.deletePolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param policyId 参数 策略Id；parameter policy id。
     */
    void deletePolicy(String gatewayGroupId, String policyId);

    /**
     * 中文说明：{@code RouteDraft} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责路由草稿相关的职责与边界。
     * English summary: {@code RouteDraft} is an immutable data carrier in the current Gateway module; it owns the route draft-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param routeId 参数 路由Id；parameter route id。
     * @param operationId 参数 操作Id；parameter operation id。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param updatedAt 参数 updatedAt；parameter updated at。
     * @param updatedBy 参数 updatedBy；parameter updated by。
     */
    record RouteDraft(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftStore.RouteDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code GatewayDraftStore.RouteDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.RouteDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.RouteDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 路由Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftStore.RouteDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by route id; its type is {@code String}, and {@code GatewayDraftStore.RouteDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.RouteDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.RouteDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String routeId,
            /**
             * 中文说明：保存 操作Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftStore.RouteDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by operation id; its type is {@code String}, and {@code GatewayDraftStore.RouteDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.RouteDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.RouteDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String operationId,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayDraftStore.RouteDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code GatewayDraftStore.RouteDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.RouteDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.RouteDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayDraftStore.RouteDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code GatewayDraftStore.RouteDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.RouteDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.RouteDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayDraftStore.RouteDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code GatewayDraftStore.RouteDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.RouteDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.RouteDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant updatedAt,
            /**
             * 中文说明：保存 updatedBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftStore.RouteDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by updated by; its type is {@code String}, and {@code GatewayDraftStore.RouteDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.RouteDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.RouteDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String updatedBy
    ) {
    }

    /**
     * 中文说明：{@code PolicyDraft} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责策略草稿相关的职责与边界。
     * English summary: {@code PolicyDraft} is an immutable data carrier in the current Gateway module; it owns the policy draft-related responsibility and boundary.
     *
     * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param policyId 参数 策略Id；parameter policy id。
     * @param policyType 参数 策略Type；parameter policy type。
     * @param policyScope 参数 策略Scope；parameter policy scope。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param updatedAt 参数 updatedAt；parameter updated at。
     * @param updatedBy 参数 updatedBy；parameter updated by。
     */
    record PolicyDraft(
            /**
             * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftStore.PolicyDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code GatewayDraftStore.PolicyDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.PolicyDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.PolicyDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String gatewayGroupId,
            /**
             * 中文说明：保存 策略Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftStore.PolicyDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by policy id; its type is {@code String}, and {@code GatewayDraftStore.PolicyDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.PolicyDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.PolicyDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String policyId,
            /**
             * 中文说明：保存 策略Type 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftStore.PolicyDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by policy type; its type is {@code String}, and {@code GatewayDraftStore.PolicyDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.PolicyDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.PolicyDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String policyType,
            /**
             * 中文说明：保存 策略Scope 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftStore.PolicyDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by policy scope; its type is {@code String}, and {@code GatewayDraftStore.PolicyDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.PolicyDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.PolicyDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String policyScope,
            /**
             * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code GatewayDraftStore.PolicyDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code GatewayDraftStore.PolicyDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.PolicyDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.PolicyDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            Map<String, Object> content,
            /**
             * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code GatewayDraftStore.PolicyDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code GatewayDraftStore.PolicyDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.PolicyDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.PolicyDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            boolean enabled,
            /**
             * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code GatewayDraftStore.PolicyDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code GatewayDraftStore.PolicyDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.PolicyDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.PolicyDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            Instant updatedAt,
            /**
             * 中文说明：保存 updatedBy 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code GatewayDraftStore.PolicyDraft} 在其生命周期内读取或更新。
             * English summary: Holds the state, dependency, or configuration represented by updated by; its type is {@code String}, and {@code GatewayDraftStore.PolicyDraft} reads or updates it during its lifecycle.
             *
             * 用法 / Usage: 该字段通过 {@code GatewayDraftStore.PolicyDraft} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code GatewayDraftStore.PolicyDraft}; do not couple callers to its representation when the owning type exposes an API.
             */
            String updatedBy
    ) {
    }
}
