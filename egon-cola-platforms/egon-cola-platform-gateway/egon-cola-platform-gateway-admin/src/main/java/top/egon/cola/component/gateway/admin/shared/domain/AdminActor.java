package top.egon.cola.component.gateway.admin.shared.domain;


import top.egon.cola.component.gateway.admin.shared.domain.enums.AdminActorTypeEnum;

import java.util.Set;

/**
 * 中文说明：{@code AdminActor} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责管理端Actor相关的职责与边界。
 * English summary: {@code AdminActor} is an immutable data carrier in the current Gateway module; it owns the admin actor-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param actorId 参数 actorId；parameter actor id。
 * @param actorType 参数 actorType；parameter actor type。
 * @param scopes 参数 scopes；parameter scopes。
 * @param roles 参数 roles；parameter roles。
 */
public record AdminActor(
        /**
         * 中文说明：保存 actorId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code AdminActor} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by actor id; its type is {@code String}, and {@code AdminActor} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code AdminActor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AdminActor}; do not couple callers to its representation when the owning type exposes an API.
         */
        String actorId,
        /**
         * 中文说明：保存 actorType 对应的状态、依赖或配置值；字段类型为 {@code AdminActorTypeEnum}，由 {@code AdminActor} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by actor type; its type is {@code AdminActorTypeEnum}, and {@code AdminActor} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code AdminActor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AdminActor}; do not couple callers to its representation when the owning type exposes an API.
         */
        AdminActorTypeEnum actorType,
        /**
         * 中文说明：保存 scopes 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code AdminActor} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by scopes; its type is {@code Set<String>}, and {@code AdminActor} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code AdminActor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AdminActor}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> scopes,
        /**
         * 中文说明：保存 roles 对应的状态、依赖或配置值；字段类型为 {@code Set<String>}，由 {@code AdminActor} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by roles; its type is {@code Set<String>}, and {@code AdminActor} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code AdminActor} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code AdminActor}; do not couple callers to its representation when the owning type exposes an API.
         */
        Set<String> roles
) {

    /**
     * 中文说明：创建 {@code AdminActor} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code AdminActor} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param actorId 参数 actorId；parameter actor id。
     * @param actorType 参数 actorType；parameter actor type。
     * @param scopes 参数 scopes；parameter scopes。
     * @param roles 参数 roles；parameter roles。
     */
    public AdminActor {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actorId is required");
        }
        actorId = actorId.trim();
        actorType = java.util.Objects.requireNonNull(
                actorType,
                "actorType"
        );
        scopes = Set.copyOf(scopes == null ? Set.of() : scopes);
        roles = Set.copyOf(roles == null ? Set.of() : roles);
    }


}
