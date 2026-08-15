package top.egon.cola.platform.rbac3.admin.participation.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.idp.contract.ServiceIdentityPrincipal;
import top.egon.cola.platform.idp.starter.security.RequiresServiceScope;
import top.egon.cola.platform.rbac3.admin.participation.service.ParticipationFacade;
import top.egon.cola.platform.rbac3.admin.iam.tenant.domain.TenantContext;
import top.egon.cola.platform.rbac3.contract.participation.BusinessParticipationCommand;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.RecordResultVO;
import top.egon.cola.platform.rbac3.admin.participation.domain.dto.ConflictQueryDTO;
import top.egon.cola.platform.rbac3.admin.participation.domain.vo.ConflictDecisionVO;

/**
 * 类型 `ParticipationController` 位于当前包内，是类型，用于承载 `Participation Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ParticipationController` is a type in its package and carries the responsibility, state, or contract for `Participation Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ParticipationController` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ParticipationController` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@RestController
@RequestMapping("/api/rbac3/v1/internal/business-participations")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "business-participation",
        name = "业务参与事实接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class ParticipationController {

    /**
     * 字段 `facade` 表示 `ParticipationController` 中与 `facade` 相关的状态、依赖、配置或结果（声明类型 `ParticipationFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `facade` stores the `facade`-related state, dependency, configuration, or result of `ParticipationController` (declared type `ParticipationFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `facade` 时应保持 `ParticipationController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `facade`, preserve `ParticipationController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ParticipationFacade facade;

    /**
     * 构造器 `ParticipationController` 用于创建并初始化 `ParticipationController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ParticipationController` creates and initializes `ParticipationController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ParticipationController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ParticipationController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param facade 输入参数 `facade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ParticipationController(ParticipationFacade facade) {
        this.facade = facade;
    }

    /**
     * 方法 `record` 按照 `ParticipationController` 的职责处理输入，完成 `record` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `record` processes its inputs according to `ParticipationController`'s responsibility, performs the `record` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `record` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `record`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping
    @RequiresServiceScope("service:participation:write")
    @GatewayOperation(name = "rbac3-business-participation-record-v1",
            summary = "幂等追加业务对象参与事实",
            externalAccessible = false, tags = {"rbac3", "internal", "participation"})
    public ApiEnvelopeVO<RecordResultVO> record(
            @Valid @RequestBody BusinessParticipationCommand command,
            @AuthenticationPrincipal ServiceIdentityPrincipal principal) {
        return ApiEnvelopeVO.success(facade.record(principal, tenantId(), command));
    }

    /**
     * 方法 `conflicts` 按照 `ParticipationController` 的职责处理输入，完成 `conflicts` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `conflicts` processes its inputs according to `ParticipationController`'s responsibility, performs the `conflicts` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `conflicts` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `conflicts`, then continue the business flow using its result, exception, or side effect.
     *
     * @param applicationCode 输入参数 `applicationCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param businessResource 输入参数 `businessResource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param businessId 输入参数 `businessId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actorUserId 输入参数 `actorUserId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param actionCode 输入参数 `actionCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/conflicts")
    @RequiresServiceScope("service:participation:read")
    @GatewayOperation(name = "rbac3-business-participation-conflicts-v1",
            summary = "查询同一业务对象的职责冲突证据",
            externalAccessible = false, tags = {"rbac3", "internal", "participation"})
    public ApiEnvelopeVO<ConflictDecisionVO> conflicts(
            @RequestParam @NotBlank String applicationCode,
            @RequestParam @NotBlank String businessResource,
            @RequestParam @NotBlank String businessId,
            @RequestParam @NotBlank String actorUserId,
            @RequestParam @NotBlank String actionCode,
            @AuthenticationPrincipal ServiceIdentityPrincipal principal) {
        return ApiEnvelopeVO.success(facade.conflicts(
                principal, tenantId(), new ConflictQueryDTO(
                        applicationCode, businessResource, businessId,
                        actorUserId, actionCode)));
    }

    /**
     * 方法 `tenantId` 按照 `ParticipationController` 的职责处理输入，完成 `tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantId` processes its inputs according to `ParticipationController`'s responsibility, performs the `tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenantId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenantId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String tenantId() {
        return TenantContext.requireCurrent().effectiveTenantId();
    }
}
