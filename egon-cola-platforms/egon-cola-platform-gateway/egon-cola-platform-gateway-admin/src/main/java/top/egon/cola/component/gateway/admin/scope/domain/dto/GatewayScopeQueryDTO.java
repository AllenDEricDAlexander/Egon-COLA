package top.egon.cola.component.gateway.admin.scope.domain.dto;


import java.util.stream.Stream;

/**
 * 中文说明：{@code GatewayScopeQueryDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责ScopeQuery相关的职责与边界。
 * English summary: {@code GatewayScopeQueryDTO} is an immutable data carrier in the current Gateway module; it owns the scope query-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param bizCode 参数 bizCode；parameter biz code。
 * @param namespace 参数 命名空间；parameter namespace。
 * @param env 参数 env；parameter env。
 * @param appCode 参数 appCode；parameter app code。
 */
public record GatewayScopeQueryDTO(
        /**
         * 中文说明：保存 bizCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by biz code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String bizCode,
        /**
         * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String namespace,
        /**
         * 中文说明：保存 env 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by env; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String env,
        /**
         * 中文说明：保存 appCode 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by app code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String appCode
) {
    /**
     * 中文说明：执行 empty 操作；该方法是 {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the empty operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.scope.domain.dto.GatewayScopeQueryDTO.empty(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 empty 的处理结果；returns the result of the operation.
     */
    public boolean empty() {
        return Stream.of(bizCode, namespace, env, appCode)
                .allMatch(value -> value == null || value.isBlank());
    }
}
