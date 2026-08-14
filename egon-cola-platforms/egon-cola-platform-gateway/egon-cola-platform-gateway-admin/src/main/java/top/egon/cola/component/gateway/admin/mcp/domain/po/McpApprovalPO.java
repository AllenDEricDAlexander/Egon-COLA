package top.egon.cola.component.gateway.admin.mcp.domain.po;


import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.McpJdbcJson;

import java.time.Instant;
import java.util.Objects;


/**
 * 中文说明：{@code McpApprovalPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审批相关的职责与边界。
 * English summary: {@code McpApprovalPO} is an immutable data carrier in the current Gateway module; it owns the approval-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param tokenDigest 参数 tokenDigest；parameter token digest。
 * @param subjectId 参数 subjectId；parameter subject id。
 * @param tenantId 参数 tenantId；parameter tenant id。
 * @param clientId 参数 客户端Id；parameter client id。
 * @param serverCode 参数 服务器Code；parameter server code。
 * @param toolName 参数 工具Name；parameter tool name。
 * @param argumentDigest 参数 argumentDigest；parameter argument digest。
 * @param issuedAt 参数 issuedAt；parameter issued at。
 * @param expiresAt 参数 expiresAt；parameter expires at。
 */
public record McpApprovalPO(
        /**
         * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String id,
        /**
         * 中文说明：保存 tokenDigest 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by token digest; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String tokenDigest,
        /**
         * 中文说明：保存 subjectId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by subject id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String subjectId,
        /**
         * 中文说明：保存 tenantId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tenant id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String tenantId,
        /**
         * 中文说明：保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String clientId,
        /**
         * 中文说明：保存 服务器Code 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by server code; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String serverCode,
        /**
         * 中文说明：保存 工具Name 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tool name; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String toolName,
        /**
         * 中文说明：保存 argumentDigest 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by argument digest; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String argumentDigest,
        /**
         * 中文说明：保存 issuedAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by issued at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant issuedAt,
        /**
         * 中文说明：保存 expiresAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expires at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant expiresAt
) {

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpApprovalPO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param id 参数 id；parameter id。
     * @param tokenDigest 参数 tokenDigest；parameter token digest。
     * @param subjectId 参数 subjectId；parameter subject id。
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     * @param serverCode 参数 服务器Code；parameter server code。
     * @param toolName 参数 工具Name；parameter tool name。
     * @param argumentDigest 参数 argumentDigest；parameter argument digest。
     * @param issuedAt 参数 issuedAt；parameter issued at。
     * @param expiresAt 参数 expiresAt；parameter expires at。
     */
    public McpApprovalPO {
        id = McpJdbcJson.required(id, "id");
        tokenDigest = digest(tokenDigest, "tokenDigest");
        subjectId = McpJdbcJson.required(subjectId, "subjectId");
        tenantId = McpJdbcJson.required(tenantId, "tenantId");
        clientId = McpJdbcJson.required(clientId, "clientId");
        serverCode = McpJdbcJson.required(serverCode, "serverCode");
        toolName = McpJdbcJson.required(toolName, "toolName");
        argumentDigest = digest(argumentDigest, "argumentDigest");
        issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after issuedAt"
            );
        }
    }

    private static String digest(String value, String field) {
        String digest = McpJdbcJson.required(value, field);
        if (digest.length() != 64) {
            throw new IllegalArgumentException(
                    field + " must contain 64 characters"
            );
        }
        return digest;
    }
}
