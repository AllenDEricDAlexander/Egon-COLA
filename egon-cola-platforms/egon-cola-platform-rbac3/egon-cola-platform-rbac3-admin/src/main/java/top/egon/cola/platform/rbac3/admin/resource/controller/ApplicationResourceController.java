package top.egon.cola.platform.rbac3.admin.resource.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.resource.service.ApplicationResourceFacade;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.config.security.RequiresRbac3Permission;
import top.egon.cola.platform.rbac3.admin.tenant.domain.TenantContext;

import java.util.List;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.dto.ArchiveResourceRequestDTO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ApplicationVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ResourceVO;
import top.egon.cola.platform.rbac3.admin.resource.domain.vo.ArchiveResultVO;

/**
 * 类型 `ApplicationResourceController` 位于当前包内，是类型，用于承载 `Application Resource Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `ApplicationResourceController` is a type in its package and carries the responsibility, state, or contract for `Application Resource Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `ApplicationResourceController` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `ApplicationResourceController` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@RestController
@RequestMapping("/api/rbac3/v1")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "application-resource",
        name = "应用与资源接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class ApplicationResourceController {

    /**
     * 字段 `facade` 表示 `ApplicationResourceController` 中与 `facade` 相关的状态、依赖、配置或结果（声明类型 `ApplicationResourceFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `facade` stores the `facade`-related state, dependency, configuration, or result of `ApplicationResourceController` (declared type `ApplicationResourceFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `facade` 时应保持 `ApplicationResourceController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `facade`, preserve `ApplicationResourceController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ApplicationResourceFacade facade;
    /**
     * 字段 `databaseClock` 表示 `ApplicationResourceController` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `ApplicationResourceController` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `ApplicationResourceController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `ApplicationResourceController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;

    /**
     * 构造器 `ApplicationResourceController` 用于创建并初始化 `ApplicationResourceController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `ApplicationResourceController` creates and initializes `ApplicationResourceController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `ApplicationResourceController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `ApplicationResourceController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param facade 输入参数 `facade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public ApplicationResourceController(
            ApplicationResourceFacade facade,
            DatabaseClock databaseClock) {
        this.facade = facade;
        this.databaseClock = databaseClock;
    }

    /**
     * 方法 `applications` 按照 `ApplicationResourceController` 的职责处理输入，完成 `applications` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `applications` processes its inputs according to `ApplicationResourceController`'s responsibility, performs the `applications` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `applications` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `applications`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/applications")
    @RequiresRbac3Permission(permission = "system:application:read")
    @GatewayOperation(
            name = "rbac3-application-list-v1",
            summary = "查询租户应用",
            externalAccessible = true,
            tags = {"rbac3", "application"})
    public ApiEnvelopeVO<List<ApplicationVO>> applications() {
        return ApiEnvelopeVO.success(facade.applications(tenantId()));
    }

    /**
     * 方法 `resources` 按照 `ApplicationResourceController` 的职责处理输入，完成 `resources` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `resources` processes its inputs according to `ApplicationResourceController`'s responsibility, performs the `resources` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `resources` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resources`, then continue the business flow using its result, exception, or side effect.
     *
     * @param applicationId 输入参数 `applicationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/applications/{applicationId}/resources")
    @RequiresRbac3Permission(permission = "system:resource:read")
    @GatewayOperation(
            name = "rbac3-application-resource-list-v1",
            summary = "查询应用资源",
            externalAccessible = true,
            tags = {"rbac3", "resource"})
    public ApiEnvelopeVO<List<ResourceVO>> resources(
            @PathVariable String applicationId) {
        return ApiEnvelopeVO.success(facade.resources(tenantId(), applicationId));
    }

    /**
     * 方法 `archive` 按照 `ApplicationResourceController` 的职责处理输入，完成 `archive` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `archive` processes its inputs according to `ApplicationResourceController`'s responsibility, performs the `archive` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `archive` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `archive`, then continue the business flow using its result, exception, or side effect.
     *
     * @param resourceId 输入参数 `resourceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/resources/{resourceId}/archive")
    @RequiresRbac3Permission(permission = "system:resource:archive")
    @GatewayOperation(
            name = "rbac3-resource-archive-v1",
            summary = "归档已失效资源",
            externalAccessible = true,
            tags = {"rbac3", "resource"})
    public ApiEnvelopeVO<ArchiveResultVO> archive(
            @PathVariable String resourceId,
            @Valid @RequestBody ArchiveResourceRequestDTO request,
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(facade.archive(
                tenantId(),
                resourceId,
                request.expectedVersion(),
                principal.userId(),
                databaseClock.transactionNow()));
    }

    /**
     * 方法 `tenantId` 按照 `ApplicationResourceController` 的职责处理输入，完成 `tenant Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenantId` processes its inputs according to `ApplicationResourceController`'s responsibility, performs the `tenant Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
