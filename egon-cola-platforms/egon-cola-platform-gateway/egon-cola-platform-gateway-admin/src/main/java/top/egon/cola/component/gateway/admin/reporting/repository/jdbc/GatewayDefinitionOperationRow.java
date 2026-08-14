package top.egon.cola.component.gateway.admin.reporting.repository.jdbc;


/**
 * 中文说明：{@code GatewayDefinitionOperationRow} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责操作Row相关的职责与边界。
 * English summary: {@code GatewayDefinitionOperationRow} is an immutable data carrier in the current Gateway module; it owns the operation row-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param interfaceGroupId 参数 接口GroupId；parameter interface group id。
 * @param sourceType 参数 sourceType；parameter source type。
 * @param currentDefinitionId 参数 current定义Id；parameter current definition id。
 * @param definitionSha256 参数 定义Sha256；parameter definition sha256。
 * @param maxVersion 参数 maxVersion；parameter max version。
 */
public record GatewayDefinitionOperationRow(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 接口GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by interface group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow}; do not couple callers to its representation when the owning type exposes an API.
         */
        String interfaceGroupId,
        /**
         * 中文说明：保存 sourceType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by source type; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow}; do not couple callers to its representation when the owning type exposes an API.
         */
        String sourceType,
        /**
         * 中文说明：保存 current定义Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by current definition id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow}; do not couple callers to its representation when the owning type exposes an API.
         */
        String currentDefinitionId,
        /**
         * 中文说明：保存 定义Sha256 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by definition sha256; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow}; do not couple callers to its representation when the owning type exposes an API.
         */
        String definitionSha256,
        /**
         * 中文说明：保存 maxVersion 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by max version; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayDefinitionOperationRow}; do not couple callers to its representation when the owning type exposes an API.
         */
        long maxVersion
) {
}
