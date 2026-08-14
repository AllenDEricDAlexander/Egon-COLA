package top.egon.cola.component.gateway.admin.catalog.domain.vo;


import java.util.List;

/**
 * 中文说明：{@code GatewayInterfaceGroupNodeVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责接口GroupNode相关的职责与边界。
 * English summary: {@code GatewayInterfaceGroupNodeVO} is an immutable data carrier in the current Gateway module; it owns the interface group node-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param code 参数 code；parameter code。
 * @param displayName 参数 displayName；parameter display name。
 * @param sourceType 参数 sourceType；parameter source type。
 * @param className 参数 className；parameter class name。
 * @param operations 参数 operations；parameter operations。
 */
public record GatewayInterfaceGroupNodeVO(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String code,
        /**
         * 中文说明：保存 displayName 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by display name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String displayName,
        /**
         * 中文说明：保存 sourceType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by source type; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String sourceType,
        /**
         * 中文说明：保存 className 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by class name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String className,
        /**
         * 中文说明：保存 operations 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayOperationNodeVO>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operations; its type is {@code List<GatewayOperationNodeVO>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.vo.GatewayInterfaceGroupNodeVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<GatewayOperationNodeVO> operations
) {
}
