package top.egon.cola.component.gateway.admin.mcp.domain.po;


import top.egon.cola.component.gateway.admin.mcp.domain.enums.McpCapabilityKindEnum;
import top.egon.cola.component.gateway.admin.mcp.repository.jdbc.McpJdbcJson;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 中文说明：{@code McpCapabilityDraftPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责MCPCapability草稿相关的职责与边界。
 * English summary: {@code McpCapabilityDraftPO} is an immutable data carrier in the current Gateway module; it owns the mcp capability draft-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
 * @param capabilities 参数 capabilities；parameter capabilities。
 */
public record McpCapabilityDraftPO(
        /**
         * 中文说明：保存 网关GroupId 对应的状态、依赖或配置值；字段类型为 {@code String}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by gateway group id; its type is {@code String}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        String gatewayGroupId,
        /**
         * 中文说明：保存 capabilities 对应的状态、依赖或配置值；字段类型为 {@code Map<McpCapabilityKindEnum, List<McpCapabilityRecordPO>>}，由 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityDraftPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by capabilities; its type is {@code Map<McpCapabilityKindEnum, List<McpCapabilityRecordPO>>}, and {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityDraftPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityDraftPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityDraftPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        Map<McpCapabilityKindEnum, List<McpCapabilityRecordPO>> capabilities
) {

    /**
     * 中文说明：创建 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityDraftPO} 实例，并接收构建该实例所需的依赖或初始数据；构造器参数定义了实例建立时必须满足的输入契约。
     * English summary: Creates an instance of {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityDraftPO} from the dependencies or initial data required at construction time; its parameters define the initialization contract.
     *
     * 用法 / Usage: 由 Spring 容器、工厂或上层组件调用；/ Call it from the Spring container, a factory, or an enclosing component after validating the supplied dependencies.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param capabilities 参数 capabilities；parameter capabilities。
     */
    public McpCapabilityDraftPO {
        gatewayGroupId = McpJdbcJson.required(
                gatewayGroupId,
                "gatewayGroupId"
        );
        EnumMap<McpCapabilityKindEnum, List<McpCapabilityRecordPO>> copy =
                new EnumMap<>(McpCapabilityKindEnum.class);
        capabilities.forEach((kind, drafts) -> copy.put(
                kind,
                List.copyOf(drafts)
        ));
        capabilities = Map.copyOf(copy);
    }

    /**
     * 中文说明：执行 capabilities 操作；该方法是 {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityDraftPO} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the capabilities operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityDraftPO} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.mcp.domain.po.McpCapabilityDraftPO.capabilities(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param kind 参数 kind；parameter kind。
     * @return 返回 capabilities 的处理结果；returns the result of the operation.
     */
    public List<McpCapabilityRecordPO> capabilities(McpCapabilityKindEnum kind) {
        return capabilities.getOrDefault(kind, List.of());
    }
}
