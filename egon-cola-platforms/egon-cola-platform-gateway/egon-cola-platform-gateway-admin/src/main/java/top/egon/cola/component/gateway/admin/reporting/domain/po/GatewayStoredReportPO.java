package top.egon.cola.component.gateway.admin.reporting.domain.po;


import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.util.List;


/**
 * 中文说明：{@code GatewayStoredReportPO} 是不可变数据载体，位于当前 Gateway 模块的相关包中，负责Stored报告相关的职责与边界。
 * English summary: {@code GatewayStoredReportPO} is an immutable data carrier in the current Gateway module; it owns the stored report-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 * @param created 参数 created；parameter created。
 * @param updated 参数 updated；parameter updated。
 * @param operationRefs 参数 操作Refs；parameter operation refs。
 */
public record GatewayStoredReportPO(
        /**
         * 中文说明：保存 created 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.reporting.domain.po.GatewayStoredReportPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by created; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.reporting.domain.po.GatewayStoredReportPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.domain.po.GatewayStoredReportPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.domain.po.GatewayStoredReportPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int created,
        /**
         * 中文说明：保存 updated 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.reporting.domain.po.GatewayStoredReportPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by updated; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.reporting.domain.po.GatewayStoredReportPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.domain.po.GatewayStoredReportPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.domain.po.GatewayStoredReportPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        int updated,
        /**
         * 中文说明：保存 操作Refs 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayInterfaceDefinitionReportResult.OperationRef>}，由 {@code top.egon.cola.component.gateway.admin.reporting.domain.po.GatewayStoredReportPO} 在其生命周期内读取或更新。
         * English summary: Holds the state, dependency, or configuration represented by operation refs; its type is {@code List<GatewayInterfaceDefinitionReportResult.OperationRef>}, and {@code top.egon.cola.component.gateway.admin.reporting.domain.po.GatewayStoredReportPO} reads or updates it during its lifecycle.
         *
         * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.domain.po.GatewayStoredReportPO} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.domain.po.GatewayStoredReportPO}; do not couple callers to its representation when the owning type exposes an API.
         */
        List<GatewayInterfaceDefinitionReportResult.OperationRef>
                operationRefs
) {
}
