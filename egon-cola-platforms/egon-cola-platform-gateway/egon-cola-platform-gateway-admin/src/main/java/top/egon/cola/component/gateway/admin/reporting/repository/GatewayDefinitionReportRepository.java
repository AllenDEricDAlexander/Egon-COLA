package top.egon.cola.component.gateway.admin.reporting.repository;


import top.egon.cola.component.gateway.admin.reporting.domain.po.GatewayStoredReportPO;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;

import java.time.Instant;
import java.util.Optional;

/**
 * 中文说明：{@code GatewayDefinitionReportRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关定义报告存储相关的职责与边界。
 * English summary: {@code GatewayDefinitionReportRepository} is an interface contract in the current Gateway module; it owns the gateway definition report store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayDefinitionReportRepository {

    /**
     * 中文说明：执行 findBuildFingerprint 操作；该方法是 {@code GatewayDefinitionReportRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the find build fingerprint operation; this method is the invocation entry point on {@code GatewayDefinitionReportRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportRepository.findBuildFingerprint(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param buildId 参数 buildId；parameter build id。
     * @return 返回 findBuildFingerprint 的处理结果；returns the result of the operation.
     */
    Optional<String> findBuildFingerprint(
            String applicationId,
            String buildId);

    /**
     * 中文说明：执行 定义SetExists 操作；该方法是 {@code GatewayDefinitionReportRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the definition set exists operation; this method is the invocation entry point on {@code GatewayDefinitionReportRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportRepository.definitionSetExists(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param definitionSetId 参数 定义SetId；parameter definition set id。
     * @return 返回 定义SetExists 的处理结果；returns the result of the operation.
     */
    boolean definitionSetExists(
            String applicationId,
            String definitionSetId);

    /**
     * 中文说明：执行 countStarterOperations 操作；该方法是 {@code GatewayDefinitionReportRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the count starter operations operation; this method is the invocation entry point on {@code GatewayDefinitionReportRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportRepository.countStarterOperations(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @return 返回 countStarterOperations 的处理结果；returns the result of the operation.
     */
    int countStarterOperations(String applicationId);

    /**
     * 中文说明：执行 ingest 操作；该方法是 {@code GatewayDefinitionReportRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the ingest operation; this method is the invocation entry point on {@code GatewayDefinitionReportRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDefinitionReportRepository.ingest(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param applicationId 参数 applicationId；parameter application id。
     * @param report 参数 报告；parameter report。
     * @param now 参数 now；parameter now。
     * @return 返回 ingest 的处理结果；returns the result of the operation.
     */
    GatewayStoredReportPO ingest(
            String applicationId,
            GatewayInterfaceDefinitionReport report,
            Instant now);


}
