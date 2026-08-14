package top.egon.cola.component.gateway.admin.mcp.domain.po;


import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.McpJdbcJson;

import java.util.Map;
import java.util.Objects;

/**
 * 中文说明：{@code McpRemoteMountDraftPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责远程Mount草稿相关的职责与边界。
 * English summary: {@code McpRemoteMountDraftPO} is an immutable data carrier in the current Gateway module; it owns the remote mount draft-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
 * @param serverId 参数 服务器Id；parameter server id。
 * @param providerId 参数 提供方Id；parameter provider id。
 * @param namespace 参数 命名空间；parameter namespace。
 * @param capabilityFingerprint 参数 capabilityFingerprint；parameter capability fingerprint。
 * @param content 参数 content；parameter content。
 * @param enabled 参数 enabled；parameter enabled。
 * @param revision 参数 revision；parameter revision。
 */
public record McpRemoteMountDraftPO(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 服务器Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serverId,
        /**
         * 中文说明：保存 提供方Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by provider id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String providerId,
        /**
         * 中文说明：保存 命名空间 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by namespace; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String namespace,
        /**
         * 中文说明：保存 capabilityFingerprint 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by capability fingerprint; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String capabilityFingerprint,
        /**
         * 中文说明：保存 content 对应的状态、依赖或配置值；字段类型为 {@code Map<String, Object>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by content; its type is {@code Map<String, Object>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<String, Object> content,
        /**
         * 中文说明：保存 enabled 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by enabled; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean enabled,
        /**
         * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long revision
) {

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpRemoteMountDraftPO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param id 参数 id；parameter id。
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param serverId 参数 服务器Id；parameter server id。
     * @param providerId 参数 提供方Id；parameter provider id。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param capabilityFingerprint 参数 capabilityFingerprint；parameter capability fingerprint。
     * @param content 参数 content；parameter content。
     * @param enabled 参数 enabled；parameter enabled。
     * @param revision 参数 revision；parameter revision。
     */
    public McpRemoteMountDraftPO {
        id = McpJdbcJson.required(id, "id");
        gatewayGroupId = McpJdbcJson.required(
                gatewayGroupId,
                "gatewayGroupId"
        );
        serverId = McpJdbcJson.required(serverId, "serverId");
        providerId = McpJdbcJson.required(providerId, "providerId");
        namespace = McpJdbcJson.required(namespace, "namespace");
        capabilityFingerprint = McpJdbcJson.required(
                capabilityFingerprint,
                "capabilityFingerprint"
        );
        content = Map.copyOf(Objects.requireNonNull(content, "content"));
    }
}
