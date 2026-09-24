package top.egon.cola.component.yuheng.admin.routing.repository;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.yuheng.admin.routing.domain.bo.GatewayPolicyDraftBO;
import top.egon.cola.component.yuheng.admin.routing.domain.bo.GatewayRouteDraftBO;

import java.util.List;

/**
 * 中文说明：{@code GatewayDraftRepository} 是接口契约，位于当前 Gateway 模块的相关包中，负责网关草稿存储相关的职责与边界。
 * English summary: {@code GatewayDraftRepository} is an interface contract in the current Gateway module; it owns the gateway draft store-related responsibility and boundary.
 *
 * 用法 / Usage: 由 {@code MpGatewayDraftRepository} 经 MyBatis-Plus 受守卫仓储实现本端口，端口只收发
 * {@code GatewayRouteDraftBO}/{@code GatewayPolicyDraftBO} 业务载体，不外泄行模型或 DAO。
 * {@code MpGatewayDraftRepository} implements this port over the guarded MyBatis-Plus repositories; the port carries only
 * the {@code GatewayRouteDraftBO}/{@code GatewayPolicyDraftBO} business carriers and never leaks a row model or DAO.
 */
@Validated
public interface GatewayDraftRepository {

    /**
     * 中文说明：执行 routes 操作；该方法是 {@code GatewayDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the routes operation; this method is the invocation entry point on {@code GatewayDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftRepository.routes(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 routes 的处理结果；returns the result of the operation.
     */
    List<GatewayRouteDraftBO> routes(@NotBlank String gatewayGroupId);

    /**
     * 中文说明：执行 policies 操作；该方法是 {@code GatewayDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the policies operation; this method is the invocation entry point on {@code GatewayDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftRepository.policies(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @return 返回 policies 的处理结果；returns the result of the operation.
     */
    List<GatewayPolicyDraftBO> policies(@NotBlank String gatewayGroupId);

    /**
     * 中文说明：执行 upsert路由 操作；该方法是 {@code GatewayDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the upsert route operation; this method is the invocation entry point on {@code GatewayDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftRepository.upsertRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param route 参数 路由；parameter route。
     */
    void upsertRoute(@Valid GatewayRouteDraftBO route);

    /**
     * 中文说明：执行 delete路由 操作；该方法是 {@code GatewayDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete route operation; this method is the invocation entry point on {@code GatewayDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftRepository.deleteRoute(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param routeId 参数 路由Id；parameter route id。
     */
    void deleteRoute(@NotBlank String gatewayGroupId, @NotBlank String routeId);

    /**
     * 中文说明：执行 upsert策略 操作；该方法是 {@code GatewayDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the upsert policy operation; this method is the invocation entry point on {@code GatewayDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftRepository.upsertPolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param policy 参数 策略；parameter policy。
     */
    void upsertPolicy(@Valid GatewayPolicyDraftBO policy);

    /**
     * 中文说明：执行 delete策略 操作；该方法是 {@code GatewayDraftRepository} 的调用入口，负责根据输入完成对应的运行时、管理面或协议处理。
     * English summary: Executes the delete policy operation; this method is the invocation entry point on {@code GatewayDraftRepository} and performs the corresponding runtime, management, or protocol work.
     *
     * 用法 / Usage: 调用方式 / Usage: {@code GatewayDraftRepository.deletePolicy(...)}。调用方应准备合法参数并处理返回值或异常；/ Call it with valid arguments and handle the return value or exception according to the owning component's lifecycle.
     * @param gatewayGroupId 参数 网关GroupId；parameter gateway group id。
     * @param policyId 参数 策略Id；parameter policy id。
     */
    void deletePolicy(@NotBlank String gatewayGroupId, @NotBlank String policyId);




}
