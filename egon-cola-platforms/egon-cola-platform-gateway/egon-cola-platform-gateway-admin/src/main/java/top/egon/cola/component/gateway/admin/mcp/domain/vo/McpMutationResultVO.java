package top.egon.cola.component.gateway.admin.mcp.domain.vo;


/**
 * 中文说明：{@code McpMutationResultVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MutationResult相关的职责与边界。
 * English summary: {@code McpMutationResultVO} is an immutable data carrier in the current Gateway module; it owns the mutation result-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param draftRevision 参数 草稿Revision；parameter draft revision。
 * @param resourceId 参数 资源Id；parameter resource id。
 * @param resourceRevision 参数 资源Revision；parameter resource revision。
 * @param replayed 参数 replayed；parameter replayed。
 */
public record McpMutationResultVO(
        /**
         * 中文说明：保存 草稿Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by draft revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long draftRevision,
        /**
         * 中文说明：保存 资源Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by resource id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String resourceId,
        /**
         * 中文说明：保存 资源Revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by resource revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        long resourceRevision,
        /**
         * 中文说明：保存 replayed 对应的状态、依赖或配置值；字段类型为 {@code boolean}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by replayed; its type is {@code boolean}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpMutationResultVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        boolean replayed
) {
}
