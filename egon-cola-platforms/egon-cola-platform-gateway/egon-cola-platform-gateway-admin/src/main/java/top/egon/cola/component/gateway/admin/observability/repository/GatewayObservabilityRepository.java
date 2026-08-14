package top.egon.cola.component.gateway.admin.observability.repository;


import top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayAuditQueryDTO;
import top.egon.cola.component.gateway.admin.observability.domain.dto.GatewayTraceQueryDTO;
import top.egon.cola.component.gateway.admin.observability.domain.po.GatewayConsumeFailurePO;
import top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayAuditVO;
import top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayDashboardVO;
import top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayPageVO;
import top.egon.cola.component.gateway.admin.observability.domain.vo.GatewayTraceVO;
import top.egon.cola.component.gateway.contract.observability.GatewayCallEventV1;

import java.time.Instant;

/**
 * 中文说明：{@code GatewayObservabilityRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关可观测性存储相关的职责与边界。
 * English summary: {@code GatewayObservabilityRepository} is an interface contract in the current Gateway module; it owns the gateway observability store-related responsibility and boundary.
 *
 * 用法 / Usage: 通过 Spring 容器或上层组件使用该类型；/ Use this type through the Spring container or an enclosing component; its public contract is the supported extension and invocation boundary.
 */
public interface GatewayObservabilityRepository {

    /**
     * 中文说明：执行 project 操作；该方法是 {@code GatewayObservabilityRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the project operation; this method is the invocation entry point on {@code GatewayObservabilityRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityRepository.project(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param event 参数 事件；parameter event。
     * @param expiresAt 参数 expiresAt；parameter expires at。
     * @return 返回 project 的处理结果；returns the result of the operation.
     */
    boolean project(GatewayCallEventV1 event, Instant expiresAt);

    /**
     * 中文说明：执行 recordFailure 操作；该方法是 {@code GatewayObservabilityRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the record failure operation; this method is the invocation entry point on {@code GatewayObservabilityRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityRepository.recordFailure(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param failure 参数 failure；parameter failure。
     */
    void recordFailure(GatewayConsumeFailurePO failure);

    /**
     * 中文说明：执行 traces 操作；该方法是 {@code GatewayObservabilityRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the traces operation; this method is the invocation entry point on {@code GatewayObservabilityRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityRepository.traces(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 traces 的处理结果；returns the result of the operation.
     */
    GatewayPageVO<GatewayTraceVO> traces(GatewayTraceQueryDTO query);

    /**
     * 中文说明：执行 dashboard 操作；该方法是 {@code GatewayObservabilityRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the dashboard operation; this method is the invocation entry point on {@code GatewayObservabilityRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityRepository.dashboard(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param env 参数 env；parameter env。
     * @param namespace 参数 命名空间；parameter namespace。
     * @param since 参数 since；parameter since。
     * @return 返回 dashboard 的处理结果；returns the result of the operation.
     */
    GatewayDashboardVO dashboard(String env, String namespace, Instant since);

    /**
     * 中文说明：执行 audits 操作；该方法是 {@code GatewayObservabilityRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the audits operation; this method is the invocation entry point on {@code GatewayObservabilityRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityRepository.audits(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param query 参数 query；parameter query。
     * @return 返回 audits 的处理结果；returns the result of the operation.
     */
    GatewayPageVO<GatewayAuditVO> audits(GatewayAuditQueryDTO query);

    /**
     * 中文说明：执行 deleteExpired 操作；该方法是 {@code GatewayObservabilityRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete expired operation; this method is the invocation entry point on {@code GatewayObservabilityRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayObservabilityRepository.deleteExpired(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param now 参数 now；parameter now。
     * @return 返回 deleteExpired 的处理结果；returns the result of the operation.
     */
    int deleteExpired(Instant now);


















}
