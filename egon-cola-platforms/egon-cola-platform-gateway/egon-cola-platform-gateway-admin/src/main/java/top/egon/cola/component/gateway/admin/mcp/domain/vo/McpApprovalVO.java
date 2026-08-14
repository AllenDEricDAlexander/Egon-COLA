package top.egon.cola.component.gateway.admin.mcp.domain.vo;


import java.time.Instant;

/**
 * 中文说明：{@code McpApprovalVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审批响应相关的职责与边界。
 * English summary: {@code McpApprovalVO} is an immutable data carrier in the current Gateway module; it owns the approval response-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param approvalId 参数 审批Id；parameter approval id。
 * @param approvalToken 参数 审批Token；parameter approval token。
 * @param expiresAt 参数 expiresAt；parameter expires at。
 */
public record McpApprovalVO(
        /**
         * 中文说明：保存 审批Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by approval id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String approvalId,
        /**
         * 中文说明：保存 审批Token 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by approval token; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String approvalToken,
        /**
         * 中文说明：保存 expiresAt 对应的状态、依赖或配置值；字段类型为 {@code Instant}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by expires at; its type is {@code Instant}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Instant expiresAt
) {

    /**
     * 中文说明：执行 toString 操作；该方法是 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the to string operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalVO.toString(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 toString 的处理结果；returns the result of the operation.
     */
    @Override
    public String toString() {
        return "McpApprovalVO[approvalId=" + approvalId
                + ", approvalToken=<redacted>, expiresAt="
                + expiresAt + ']';
    }
}
