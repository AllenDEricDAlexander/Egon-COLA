package top.egon.cola.component.gateway.admin.mcp.domain.vo;


/**
 * 中文说明：{@code McpApprovalOwnerVO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责审批Owner相关的职责与边界。
 * English summary: {@code McpApprovalOwnerVO} is an immutable data carrier in the current Gateway module; it owns the approval owner-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param subjectId 参数 subjectId；parameter subject id。
 * @param tenantId 参数 tenantId；parameter tenant id。
 * @param clientId 参数 客户端Id；parameter client id。
 */
public record McpApprovalOwnerVO(
        /**
         * 中文说明：保存 subjectId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by subject id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String subjectId,
        /**
         * 中文说明：保存 tenantId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by tenant id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String tenantId,
        /**
         * 中文说明：保存 客户端Id 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by client id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String clientId
) {

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param subjectId 参数 subjectId；parameter subject id。
     * @param tenantId 参数 tenantId；parameter tenant id。
     * @param clientId 参数 客户端Id；parameter client id。
     */
    public McpApprovalOwnerVO {
        subjectId = required(subjectId, "subjectId");
        tenantId = required(tenantId, "tenantId");
        clientId = required(clientId, "clientId");
    }

    /**
     * 中文说明：执行 required 操作；该方法是 {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the required operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.mcp.domain.vo.McpApprovalOwnerVO.required(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param value 参数 值；parameter value。
     * @param field 参数 field；parameter field。
     * @return 返回 required 的处理结果；returns the result of the operation.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
