package top.egon.cola.platform.rbac3.admin.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.admin.shared.domain.DatabaseClock;
import top.egon.cola.platform.rbac3.admin.bootstrap.service.BootstrapQueryService;
import top.egon.cola.platform.rbac3.admin.config.security.CurrentRbac3Principal;
import top.egon.cola.platform.rbac3.admin.session.service.SessionFacade;
import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;
import top.egon.cola.platform.rbac3.admin.shared.domain.vo.ApiEnvelopeVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.LogoutVO;

/**
 * 类型 `AuthController` 位于当前包内，是类型，用于承载 `Auth Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuthController` is a type in its package and carries the responsibility, state, or contract for `Auth Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * 语义与用法：将 `AuthController` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `AuthController` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
@RestController
@RequestMapping("/api/rbac3/v1/auth")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "rbac3",
        entityDomainName = "RBAC3权限实体域",
        code = "auth",
        name = "认证与激活引导接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/rbac3/v1")
public class AuthController {

    /**
     * 字段 `bootstrapQueryService` 表示 `AuthController` 中与 `bootstrap Query Service` 相关的状态、依赖、配置或结果（声明类型 `BootstrapQueryService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `bootstrapQueryService` stores the `bootstrap Query Service`-related state, dependency, configuration, or result of `AuthController` (declared type `BootstrapQueryService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `bootstrapQueryService` 时应保持 `AuthController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `bootstrapQueryService`, preserve `AuthController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final BootstrapQueryService bootstrapQueryService;
    /**
     * 字段 `sessionFacade` 表示 `AuthController` 中与 `session Facade` 相关的状态、依赖、配置或结果（声明类型 `SessionFacade`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionFacade` stores the `session Facade`-related state, dependency, configuration, or result of `AuthController` (declared type `SessionFacade`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionFacade` 时应保持 `AuthController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionFacade`, preserve `AuthController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionFacade sessionFacade;
    /**
     * 字段 `databaseClock` 表示 `AuthController` 中与 `database Clock` 相关的状态、依赖、配置或结果（声明类型 `DatabaseClock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `databaseClock` stores the `database Clock`-related state, dependency, configuration, or result of `AuthController` (declared type `DatabaseClock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `databaseClock` 时应保持 `AuthController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `databaseClock`, preserve `AuthController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DatabaseClock databaseClock;

    /**
     * 构造器 `AuthController` 用于创建并初始化 `AuthController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuthController` creates and initializes `AuthController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuthController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuthController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param bootstrapQueryService 输入参数 `bootstrapQueryService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionFacade 输入参数 `sessionFacade`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param databaseClock 输入参数 `databaseClock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AuthController(
            BootstrapQueryService bootstrapQueryService,
            SessionFacade sessionFacade,
            DatabaseClock databaseClock) {
        this.bootstrapQueryService = bootstrapQueryService;
        this.sessionFacade = sessionFacade;
        this.databaseClock = databaseClock;
    }

    /**
     * 方法 `logout` 按照 `AuthController` 的职责处理输入，完成 `logout` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `logout` processes its inputs according to `AuthController`'s responsibility, performs the `logout` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `logout` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `logout`, then continue the business flow using its result, exception, or side effect.
     *
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @PostMapping("/logout")
    @GatewayOperation(
            name = "rbac3-auth-logout-v1",
            summary = "幂等注销当前会话",
            externalAccessible = true,
            tags = {"rbac3", "session"})
    public ResponseEntity<ApiEnvelopeVO<LogoutVO>> logout(
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        boolean changed = sessionFacade.logout(
                principal.tenantId(),
                principal.userId(),
                principal.sessionId(),
                databaseClock.transactionNow());
        return ResponseEntity.ok(ApiEnvelopeVO.success(new LogoutVO(true, changed)));
    }

    /**
     * 方法 `bootstrap` 按照 `AuthController` 的职责处理输入，完成 `bootstrap` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bootstrap` processes its inputs according to `AuthController`'s responsibility, performs the `bootstrap` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `bootstrap` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `bootstrap`, then continue the business flow using its result, exception, or side effect.
     *
     * @param principal 输入参数 `principal`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/bootstrap")
    @GatewayOperation(
            name = "rbac3-auth-bootstrap-v1",
            summary = "读取当前激活角色的业务启动视图",
            externalAccessible = true,
            tags = {"rbac3", "bootstrap"})
    public ApiEnvelopeVO<BootstrapView> bootstrap(
            @AuthenticationPrincipal CurrentRbac3Principal principal) {
        return ApiEnvelopeVO.success(bootstrapQueryService.query(
                principal.tenantId(), principal.userId(), principal.sessionId()));
    }

    }
