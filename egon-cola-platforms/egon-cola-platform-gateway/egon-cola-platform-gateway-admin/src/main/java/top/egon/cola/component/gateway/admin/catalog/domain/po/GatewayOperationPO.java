package top.egon.cola.component.gateway.admin.catalog.domain.po;


import java.time.Instant;
import java.util.Map;

/**
 * 中文说明：{@code GatewayOperationPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责操作Record相关的职责与边界。
 * English summary: {@code GatewayOperationPO} is an immutable data carrier in the current Gateway module; it owns the operation record-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param applicationId 参数 applicationId；parameter application id。
 * @param interfaceGroupId 参数 接口GroupId；parameter interface group id。
 * @param operationKey 参数 操作键；parameter operation key。
 * @param protocol 参数 protocol；parameter protocol。
 * @param methodIdentity 参数 方法身份；parameter method identity。
 * @param externalAccessible 参数 externalAccessible；parameter external accessible。
 * @param providerServiceIdentity 参数 提供方服务身份；parameter provider service identity。
 * @param sourceType 参数 sourceType；parameter source type。
 * @param lifecycleStatus 参数 生命周期Status；parameter lifecycle status。
 * @param currentDefinitionId 参数 current定义Id；parameter current definition id。
 * @param revision 参数 revision；parameter revision。
 * @param createdAt 参数 createdAt；parameter created at。
 * @param updatedAt 参数 updatedAt；parameter updated at。
 */
public record GatewayOperationPO(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 applicationId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by application id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String applicationId,
        /**
         * 中文说明：保存 接口GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by interface group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String interfaceGroupId,
        /**
         * 中文说明：保存 操作键 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation key; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String operationKey,
        /**
         * 中文说明：保存 protocol 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by protocol; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String protocol,
        /**
         * 中文说明：保存 方法身份 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by method identity; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String methodIdentity,
        /**
         * 中文说明：保存 externalAccessible 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by external accessible; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean externalAccessible,
        /**
         * 中文说明：保存 提供方服务身份 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider service identity; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> providerServiceIdentity,
        /**
         * 中文说明：保存 sourceType 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by source type; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String sourceType,
        /**
         * 中文说明：保存 生命周期Status 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by lifecycle status; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String lifecycleStatus,
        /**
         * 中文说明：保存 current定义Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by current definition id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String currentDefinitionId,
        /**
         * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long revision,
        /**
         * 中文说明：保存 createdAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by created at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant createdAt,
        /**
         * 中文说明：保存 updatedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by updated at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.catalog.domain.po.GatewayOperationPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant updatedAt
) {
}
