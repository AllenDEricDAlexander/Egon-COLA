package top.egon.cola.platform.rbac3.admin.interfaces.http;

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
import top.egon.cola.platform.rbac3.admin.application.port.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.identity.application.IdentityMappingFacade;
import top.egon.cola.platform.idp.starter.security.RequiresServiceScope;

import java.util.List;

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
    public ApiEnvelope<List<TenantMembershipResponse>> tenants(
            @PathVariable("identitySub") String identitySub,
            @RequestParam("clientId") String clientId) {
        return ApiEnvelope.success(facade.tenants(identitySub, clientId).stream()
                .map(membership -> TenantMembershipResponse.from(
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
    public ApiEnvelope<ResolvedMembershipResponse> resolve(
            @Valid @RequestBody ResolveRequest request) {
        return facade.resolve(
                        request.identitySub(), request.tenantId(), request.clientId())
                .map(ResolvedMembershipResponse::from)
                .map(ApiEnvelope::success)
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
    public ApiEnvelope<IdentityMappingFacade.Mapping> bind(
            @Valid @RequestBody BindRequest request) {
        return ApiEnvelope.success(facade.bind(
                request.tenantId(), request.identitySub(), request.rbac3UserId(),
                request.actorId(), databaseClock.transactionNow()));
    }

    /**
     * 类型 `ResolveRequest` 位于 `InternalIdentityController` 内，是记录类型，用于承载 `Resolve Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ResolveRequest` is a record inside `InternalIdentityController` and carries the responsibility, state, or contract for `Resolve Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ResolveRequest` 作为 `InternalIdentityController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ResolveRequest` as the responsibility boundary of `InternalIdentityController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param identitySub 记录组件 `identitySub` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identitySub` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param clientId 记录组件 `clientId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `clientId` carries constructor data whose meaning is defined by the record contract.
     */
    public record ResolveRequest(
            /**
             * 字段 `identitySub` 表示 `ResolveRequest` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `ResolveRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `ResolveRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `ResolveRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String identitySub,
            /**
             * 字段 `tenantId` 表示 `ResolveRequest` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ResolveRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ResolveRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ResolveRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String tenantId,
            /**
             * 字段 `clientId` 表示 `ResolveRequest` 中与 `client Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `clientId` stores the `client Id`-related state, dependency, configuration, or result of `ResolveRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `clientId` 时应保持 `ResolveRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `clientId`, preserve `ResolveRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String clientId
    ) {
    }

    /**
     * 类型 `BindRequest` 位于 `InternalIdentityController` 内，是记录类型，用于承载 `Bind Request` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `BindRequest` is a record inside `InternalIdentityController` and carries the responsibility, state, or contract for `Bind Request`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `BindRequest` 作为 `InternalIdentityController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `BindRequest` as the responsibility boundary of `InternalIdentityController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param identitySub 记录组件 `identitySub` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identitySub` carries constructor data whose meaning is defined by the record contract.
     * @param rbac3UserId 记录组件 `rbac3UserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rbac3UserId` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     */
    public record BindRequest(
            /**
             * 字段 `tenantId` 表示 `BindRequest` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `BindRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `BindRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `BindRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String tenantId,
            /**
             * 字段 `identitySub` 表示 `BindRequest` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `BindRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `BindRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `BindRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String identitySub,
            /**
             * 字段 `rbac3UserId` 表示 `BindRequest` 中与 `rbac3 User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rbac3UserId` stores the `rbac3 User Id`-related state, dependency, configuration, or result of `BindRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rbac3UserId` 时应保持 `BindRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rbac3UserId`, preserve `BindRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String rbac3UserId,
            /**
             * 字段 `actorId` 表示 `BindRequest` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `BindRequest` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `BindRequest` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `BindRequest`'s lifecycle, immutability, and thread-safety constraints.
             */
            @NotBlank String actorId
    ) {
    }

    /**
     * 类型 `ResolvedMembershipResponse` 位于 `InternalIdentityController` 内，是记录类型，用于承载 `Resolved Membership Response` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ResolvedMembershipResponse` is a record inside `InternalIdentityController` and carries the responsibility, state, or contract for `Resolved Membership Response`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ResolvedMembershipResponse` 作为 `InternalIdentityController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ResolvedMembershipResponse` as the responsibility boundary of `InternalIdentityController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param identitySub 记录组件 `identitySub` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identitySub` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param rbac3UserId 记录组件 `rbac3UserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rbac3UserId` carries constructor data whose meaning is defined by the record contract.
     * @param tenantDisplayName 记录组件 `tenantDisplayName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantDisplayName` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param authorizationContextRequired 记录组件 `authorizationContextRequired` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authorizationContextRequired` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record ResolvedMembershipResponse(
            /**
             * 字段 `identitySub` 表示 `ResolvedMembershipResponse` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `ResolvedMembershipResponse` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `ResolvedMembershipResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `ResolvedMembershipResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            String identitySub,
            /**
             * 字段 `tenantId` 表示 `ResolvedMembershipResponse` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `ResolvedMembershipResponse` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `ResolvedMembershipResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `ResolvedMembershipResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `rbac3UserId` 表示 `ResolvedMembershipResponse` 中与 `rbac3 User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rbac3UserId` stores the `rbac3 User Id`-related state, dependency, configuration, or result of `ResolvedMembershipResponse` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rbac3UserId` 时应保持 `ResolvedMembershipResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rbac3UserId`, preserve `ResolvedMembershipResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            String rbac3UserId,
            /**
             * 字段 `tenantDisplayName` 表示 `ResolvedMembershipResponse` 中与 `tenant Display Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantDisplayName` stores the `tenant Display Name`-related state, dependency, configuration, or result of `ResolvedMembershipResponse` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantDisplayName` 时应保持 `ResolvedMembershipResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantDisplayName`, preserve `ResolvedMembershipResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantDisplayName,
            /**
             * 字段 `status` 表示 `ResolvedMembershipResponse` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `ResolvedMembershipResponse` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `ResolvedMembershipResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `ResolvedMembershipResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `authorizationContextRequired` 表示 `ResolvedMembershipResponse` 中与 `authorization Context Required` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authorizationContextRequired` stores the `authorization Context Required`-related state, dependency, configuration, or result of `ResolvedMembershipResponse` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authorizationContextRequired` 时应保持 `ResolvedMembershipResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authorizationContextRequired`, preserve `ResolvedMembershipResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean authorizationContextRequired,
            /**
             * 字段 `authVersion` 表示 `ResolvedMembershipResponse` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `ResolvedMembershipResponse` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `ResolvedMembershipResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `ResolvedMembershipResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `policyVersion` 表示 `ResolvedMembershipResponse` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `ResolvedMembershipResponse` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `ResolvedMembershipResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `ResolvedMembershipResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion
    ) {
        /**
         * 方法 `from` 按照 `ResolvedMembershipResponse` 的职责处理输入，完成 `from` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `from` processes its inputs according to `ResolvedMembershipResponse`'s responsibility, performs the `from` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `from` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `from`, then continue the business flow using its result, exception, or side effect.
         *
         * @param membership 输入参数 `membership`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        private static ResolvedMembershipResponse from(
                IdentityMappingFacade.ResolvedMembership membership) {
            return new ResolvedMembershipResponse(
                    membership.identitySub(),
                    membership.tenantId(),
                    membership.rbac3UserId(),
                    membership.tenantName(),
                    "ACTIVE",
                    membership.authorizationContextRequired(),
                    membership.authVersion(),
                    membership.policyVersion());
        }
    }

    /**
     * 类型 `TenantMembershipResponse` 位于 `InternalIdentityController` 内，是记录类型，用于承载 `Tenant Membership Response` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `TenantMembershipResponse` is a record inside `InternalIdentityController` and carries the responsibility, state, or contract for `Tenant Membership Response`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `TenantMembershipResponse` 作为 `InternalIdentityController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TenantMembershipResponse` as the responsibility boundary of `InternalIdentityController`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param identitySub 记录组件 `identitySub` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `identitySub` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param rbac3UserId 记录组件 `rbac3UserId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `rbac3UserId` carries constructor data whose meaning is defined by the record contract.
     * @param tenantDisplayName 记录组件 `tenantDisplayName` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantDisplayName` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     */
    public record TenantMembershipResponse(
            /**
             * 字段 `identitySub` 表示 `TenantMembershipResponse` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `TenantMembershipResponse` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `TenantMembershipResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `TenantMembershipResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            String identitySub,
            /**
             * 字段 `tenantId` 表示 `TenantMembershipResponse` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `TenantMembershipResponse` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `TenantMembershipResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `TenantMembershipResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `rbac3UserId` 表示 `TenantMembershipResponse` 中与 `rbac3 User Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `rbac3UserId` stores the `rbac3 User Id`-related state, dependency, configuration, or result of `TenantMembershipResponse` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `rbac3UserId` 时应保持 `TenantMembershipResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `rbac3UserId`, preserve `TenantMembershipResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            String rbac3UserId,
            /**
             * 字段 `tenantDisplayName` 表示 `TenantMembershipResponse` 中与 `tenant Display Name` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantDisplayName` stores the `tenant Display Name`-related state, dependency, configuration, or result of `TenantMembershipResponse` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantDisplayName` 时应保持 `TenantMembershipResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantDisplayName`, preserve `TenantMembershipResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantDisplayName,
            /**
             * 字段 `status` 表示 `TenantMembershipResponse` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `TenantMembershipResponse` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `TenantMembershipResponse` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `TenantMembershipResponse`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status
    ) {
        /**
         * 方法 `from` 按照 `TenantMembershipResponse` 的职责处理输入，完成 `from` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `from` processes its inputs according to `TenantMembershipResponse`'s responsibility, performs the `from` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `from` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `from`, then continue the business flow using its result, exception, or side effect.
         *
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param membership 输入参数 `membership`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        private static TenantMembershipResponse from(
                String identitySub,
                IdentityMappingFacade.TenantMembership membership) {
            return new TenantMembershipResponse(
                    identitySub,
                    membership.tenantId(),
                    membership.rbac3UserId(),
                    membership.tenantName(),
                    "ACTIVE");
        }
    }

    /**
     * 类型 `IdentityMembershipNotFoundException` 位于 `InternalIdentityController` 内，是类型，用于承载 `Identity Membership Not Found Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `IdentityMembershipNotFoundException` is a type inside `InternalIdentityController` and carries the responsibility, state, or contract for `Identity Membership Not Found Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `IdentityMembershipNotFoundException` 作为 `InternalIdentityController` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `IdentityMembershipNotFoundException` as the responsibility boundary of `InternalIdentityController`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public static final class IdentityMembershipNotFoundException
            extends IllegalStateException {

        /**
         * 构造器 `IdentityMembershipNotFoundException` 用于创建并初始化 `IdentityMembershipNotFoundException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `IdentityMembershipNotFoundException` creates and initializes `IdentityMembershipNotFoundException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `IdentityMembershipNotFoundException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `IdentityMembershipNotFoundException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        IdentityMembershipNotFoundException(String identitySub, String tenantId) {
            super("active identity membership not found: identitySub="
                    + identitySub + ", tenantId=" + tenantId);
        }
    }
}
