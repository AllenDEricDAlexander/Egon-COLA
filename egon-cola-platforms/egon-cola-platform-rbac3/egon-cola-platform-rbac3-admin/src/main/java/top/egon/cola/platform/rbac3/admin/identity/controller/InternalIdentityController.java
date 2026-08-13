package top.egon.cola.platform.rbac3.admin.identity.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.shared.repository.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.identity.service.IdentityMappingFacade;
import top.egon.cola.platform.idp.starter.security.RequiresServiceScope;

import java.util.List;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.dto.IdentityResolveRequestDTO;
import top.egon.cola.platform.rbac3.admin.identity.domain.dto.IdentityBindRequestDTO;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.ResolvedMembershipResponseVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.TenantMembershipResponseVO;
import top.egon.cola.platform.rbac3.admin.identity.domain.exception.IdentityMembershipNotFoundException;
import top.egon.cola.platform.rbac3.admin.identity.domain.vo.MappingVO;

/**
 * 类型 `InternalIdentityController` 位于当前包内，是类型，用于承载 `Internal Identity Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `InternalIdentityController` is a type in its package and carries the responsibility, state, or contract for `Internal Identity Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Trusted service endpoints used by the IdP to resolve tenant membership.
 */
@RestController
@RequestMapping("/internal/v1/identity")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "internal-identity",
        name = "统一身份内部映射接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/internal/v1")
public class InternalIdentityController {

    /**
     * 字段 `facade` 表示 `InternalIdentityController` 中与 `facade` 相关的状态、依赖、配置或结果（声明类型 `IdentityMappingFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `facade` stores the `facade`-related state, dependency, configuration, or result of `InternalIdentityController` (declared type `IdentityMappingFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `facade` 时应保持 `InternalIdentityController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `facade`, preserve `InternalIdentityController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final IdentityMappingFacade facade;
    /**
     * 字段 `databaseClock` 表示 `InternalIdentityController` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `InternalIdentityController` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `InternalIdentityController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `InternalIdentityController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;

    /**
     * 构造器 `InternalIdentityController` 用于创建并初始化 `InternalIdentityController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `InternalIdentityController` creates and initializes `InternalIdentityController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `InternalIdentityController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `InternalIdentityController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param facade 输入参数 `facade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public InternalIdentityController(
            IdentityMappingFacade facade, DatabaseClock databaseClock) {
        this.facade = facade;
        this.databaseClock = databaseClock;
    }

    /**
     * 方法 `tenants` 按照 `InternalIdentityController` 的职责处理输入，完成 `tenants` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tenants` processes its inputs according to `InternalIdentityController`'s responsibility, performs the `tenants` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tenants` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tenants`, then continue the business flow using its result, exception, or side effect.
     *
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clientId 输入参数 `clientId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/{identitySub}/tenants")
    @RequiresServiceScope("service:identity:resolve")
    @GatewayOperation(
            name = "rbac3-internal-identity-tenants-v1",
            summary = "查询全局身份可访问的租户",
            externalAccessible = false,
            tags = {"rbac3", "identity", "internal"})
    public ApiEnvelopeVO<List<TenantMembershipResponseVO>> tenants(
            @PathVariable("identitySub") String identitySub,
            @RequestParam("clientId") String clientId) {
        return ApiEnvelopeVO.success(facade.tenants(identitySub, clientId).stream()
                .map(membership -> TenantMembershipResponseVO.from(
                        identitySub, membership))
                .toList());
    }

    /**
     * 方法 `resolve` 按照 `InternalIdentityController` 的职责处理输入，完成 `resolve` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `resolve` processes its inputs according to `InternalIdentityController`'s responsibility, performs the `resolve` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `resolve` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `resolve`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/resolve")
    @RequiresServiceScope("service:identity:resolve")
    @GatewayOperation(
            name = "rbac3-internal-identity-resolve-v1",
            summary = "解析全局身份的租户成员关系",
            externalAccessible = false,
            tags = {"rbac3", "identity", "internal"})
    public ApiEnvelopeVO<ResolvedMembershipResponseVO> resolve(
            @Valid @RequestBody IdentityResolveRequestDTO request) {
        return facade.resolve(
                        request.identitySub(), request.tenantId(), request.clientId())
                .map(ResolvedMembershipResponseVO::from)
                .map(ApiEnvelopeVO::success)
                .orElseThrow(() -> new IdentityMembershipNotFoundException(
                        request.identitySub(), request.tenantId()));
    }

    /**
     * 方法 `bind` 按照 `InternalIdentityController` 的职责处理输入，完成 `bind` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bind` processes its inputs according to `InternalIdentityController`'s responsibility, performs the `bind` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `bind` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `bind`, then continue the business flow using its result, exception, or side effect.
     *
     * @param request 输入参数 `request`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/bindings")
    @RequiresServiceScope("service:identity:bind")
    @GatewayOperation(
            name = "rbac3-internal-identity-bind-v1",
            summary = "绑定全局身份与租户用户",
            externalAccessible = false,
            tags = {"rbac3", "identity", "internal"})
    public ApiEnvelopeVO<MappingVO> bind(
            @Valid @RequestBody IdentityBindRequestDTO request) {
        return ApiEnvelopeVO.success(facade.bind(
                request.tenantId(), request.identitySub(), request.rbac3UserId(),
                request.actorId(), databaseClock.transactionNow()));
    }





    }
