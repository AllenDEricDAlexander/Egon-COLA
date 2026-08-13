package top.egon.cola.platform.rbac3.starter.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import top.egon.cola.platform.rbac3.contract.authorization.Decision;
import top.egon.cola.platform.rbac3.contract.authorization.PermissionRequest;
import top.egon.cola.platform.rbac3.starter.authorization.AuthorizationService;
import top.egon.cola.platform.rbac3.starter.authorization.DefaultAuthorizationService.AuthorizationDeniedException;

import java.util.Objects;

/**
 * 类型 `Rbac3MethodAuthorizationAspect` 位于当前包内，是类型，用于承载 `Rbac3 Method Authorization Aspect` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3MethodAuthorizationAspect` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Method Authorization Aspect`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Thin method-level PEP delegating all decisions to AuthorizationService.
 */
@Aspect
public final class Rbac3MethodAuthorizationAspect {

    /**
     * 字段 `authorizationService` 表示 `Rbac3MethodAuthorizationAspect` 中与 `authorization Service` 相关的状态、依赖、配置或结果（声明类型 `AuthorizationService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `authorizationService` stores the `authorization Service`-related state, dependency, configuration, or result of `Rbac3MethodAuthorizationAspect` (declared type `AuthorizationService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `authorizationService` 时应保持 `Rbac3MethodAuthorizationAspect` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `authorizationService`, preserve `Rbac3MethodAuthorizationAspect`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuthorizationService authorizationService;

    /**
     * 构造器 `Rbac3MethodAuthorizationAspect` 用于创建并初始化 `Rbac3MethodAuthorizationAspect` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3MethodAuthorizationAspect` creates and initializes `Rbac3MethodAuthorizationAspect`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3MethodAuthorizationAspect` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3MethodAuthorizationAspect`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param authorizationService 输入参数 `authorizationService`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3MethodAuthorizationAspect(AuthorizationService authorizationService) {
        this.authorizationService = Objects.requireNonNull(
                authorizationService, "authorizationService");
    }

    /**
     * 方法 `authorize` 按照 `Rbac3MethodAuthorizationAspect` 的职责处理输入，完成 `authorize` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorize` processes its inputs according to `Rbac3MethodAuthorizationAspect`'s responsibility, performs the `authorize` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorize` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorize`, then continue the business flow using its result, exception, or side effect.
     *
     * @param joinPoint 输入参数 `joinPoint`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requiresPermission 输入参数 `requiresPermission`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     * @throws Throwable 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
     */
    @Around("@annotation(requiresPermission)")
    public Object authorize(
            ProceedingJoinPoint joinPoint,
            RequiresPermission requiresPermission
    ) throws Throwable {
        var decision = authorizationService.requirePermission(
                PermissionRequest.of(requiresPermission.value()));
        if (decision.decision() != Decision.ALLOW) {
            throw new AuthorizationDeniedException(
                    decision.reasonCode(), requiresPermission.value());
        }
        return joinPoint.proceed();
    }
}
