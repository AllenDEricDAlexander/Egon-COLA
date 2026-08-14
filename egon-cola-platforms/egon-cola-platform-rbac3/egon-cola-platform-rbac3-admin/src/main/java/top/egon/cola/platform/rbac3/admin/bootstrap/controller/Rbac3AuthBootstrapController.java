package top.egon.cola.platform.rbac3.admin.bootstrap.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.gateway.starter.annotation.EgonHttpService;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.platform.rbac3.contract.auth.BootstrapView;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationBootstrapService;
import top.egon.cola.platform.rbac3.starter.security.RequiresPermission;

import java.util.Objects;

/**
 * 类型 `Rbac3AuthBootstrapController` 位于当前包内，是类型，用于承载 `Rbac3 Auth Bootstrap Controller` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3AuthBootstrapController` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Auth Bootstrap Controller`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Unified SSO bootstrap endpoint for the RBAC3 administration web application.
 */
@RestController
@RequestMapping("/api/v1/auth")
@GatewayInterfaceGroup(
        businessDomainCode = "platform",
        businessDomainName = "平台治理域",
        entityDomainCode = "identity",
        entityDomainName = "统一身份域",
        code = "rbac3-auth-bootstrap",
        name = "RBAC3统一身份启动接口组")
@EgonHttpService(
        serviceName = "rbac3-admin",
        group = "default",
        version = "1.0.0",
        basePath = "/api/v1")
public class Rbac3AuthBootstrapController {

    /**
     * 字段 `bootstrap` 表示 `Rbac3AuthBootstrapController` 中与 `bootstrap` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationBootstrapService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `bootstrap` stores the `bootstrap`-related state, dependency, configuration, or result of `Rbac3AuthBootstrapController` (declared type `AuthorizationBootstrapService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `bootstrap` 时应保持 `Rbac3AuthBootstrapController` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `bootstrap`, preserve `Rbac3AuthBootstrapController`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationBootstrapService bootstrap;

    /**
     * 构造器 `Rbac3AuthBootstrapController` 用于创建并初始化 `Rbac3AuthBootstrapController` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3AuthBootstrapController` creates and initializes `Rbac3AuthBootstrapController`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3AuthBootstrapController` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3AuthBootstrapController`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param bootstrap 输入参数 `bootstrap`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3AuthBootstrapController(AuthorizationBootstrapService bootstrap) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
    }

    /**
     * 方法 `bootstrap` 按照 `Rbac3AuthBootstrapController` 的职责处理输入，完成 `bootstrap` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `bootstrap` processes its inputs according to `Rbac3AuthBootstrapController`'s responsibility, performs the `bootstrap` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `bootstrap` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `bootstrap`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @GetMapping("/bootstrap")
    @RequiresPermission("system:bootstrap:read")
    @GatewayOperation(name = "rbac3-unified-auth-bootstrap-v1",
            summary = "查询RBAC3管理端统一身份启动上下文",
            externalAccessible = false, tags = {"rbac3", "identity"})
    public BootstrapView bootstrap() {
        return bootstrap.current();
    }
}
