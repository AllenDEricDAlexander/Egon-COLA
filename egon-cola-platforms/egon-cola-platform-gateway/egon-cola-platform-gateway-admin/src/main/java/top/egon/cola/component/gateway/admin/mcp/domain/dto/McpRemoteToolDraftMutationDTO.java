package top.egon.cola.component.gateway.admin.mcp.domain.dto;


/**
 * 中文说明：{@code McpRemoteToolDraftMutationDTO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责草稿Mutation相关的职责与边界。
 * English summary: {@code McpRemoteToolDraftMutationDTO} is an immutable data carrier in the current Gateway module; it owns the draft mutation-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param id 参数 id；parameter id。
 * @param revision 参数 revision；parameter revision。
 */
public record McpRemoteToolDraftMutationDTO(
/**
 * 中文说明：保存 id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolDraftMutationDTO} 在其生命周期内读取或更新。
 * English summary: Holds the state, dependency, or configuration represented by id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolDraftMutationDTO} reads or updates it during its lifecycle.
 *
 * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolDraftMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolDraftMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
 */
String id,
/**
 * 中文说明：保存 revision 对应的状态、依赖或配置值；字段类型为 {@code long}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolDraftMutationDTO} 在其生命周期内读取或更新。
 * English summary: Holds the state, dependency, or configuration represented by revision; its type is {@code long}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolDraftMutationDTO} reads or updates it during its lifecycle.
 *
 * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolDraftMutationDTO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.dto.McpRemoteToolDraftMutationDTO}; do not couple callers to its representation when the owning type exposes an API.
 */
long revision) {
}
