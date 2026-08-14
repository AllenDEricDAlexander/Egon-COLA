package top.egon.cola.component.gateway.admin.reporting.repository.jdbc;


import top.egon.cola.component.gateway.admin.reporting.domain.po.GatewayStoredReportPO;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;

import java.util.ArrayList;
import java.util.List;


/**
 * 中文说明：{@code GatewayMutableStoredReport} 是类型，位于当前 Gateway 模块的相关包中，负责MutableStored相关的职责与边界。
 * English summary: {@code GatewayMutableStoredReport} is a type in the current Gateway module; it owns the mutable stored-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public final class GatewayMutableStoredReport {

    /**
     * 中文说明：保存 created 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by created; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport}; do not couple callers to its representation when the owning type exposes an API.
     */
    int created;

    /**
     * 中文说明：保存 updated 对应的状态、依赖或配置值；字段类型为 {@code int}，由 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by updated; its type is {@code int}, and {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport}; do not couple callers to its representation when the owning type exposes an API.
     */
    int updated;

    /**
     * 中文说明：保存 refs 对应的状态、依赖或配置值；字段类型为 {@code List<GatewayInterfaceDefinitionReportResult.OperationRef>}，由 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport} 在其生命周期内读取或更新。
     * English summary: Holds the state, dependency, or configuration represented by refs; its type is {@code List<GatewayInterfaceDefinitionReportResult.OperationRef>}, and {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport} reads or updates it during its lifecycle.
     *
     * 用法 / Usage: 该字段通过 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport} 的构造、初始化或业务方法使用；/ Access it through the construction, initialization, or business methods of {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport}; do not couple callers to its representation when the owning type exposes an API.
     */
    final List<
            GatewayInterfaceDefinitionReportResult.OperationRef> refs =
            new ArrayList<>();

    /**
     * 中文说明：执行 freeze 操作；该方法是 {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the freeze operation; this method is the invocation entry point on {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code top.egon.cola.component.gateway.admin.reporting.repository.jdbc.GatewayMutableStoredReport.freeze(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @return 返回 freeze 的处理结果；returns the result of the operation.
     */
    GatewayStoredReportPO freeze() {
        return new GatewayStoredReportPO(created, updated, List.copyOf(refs));
    }
}
