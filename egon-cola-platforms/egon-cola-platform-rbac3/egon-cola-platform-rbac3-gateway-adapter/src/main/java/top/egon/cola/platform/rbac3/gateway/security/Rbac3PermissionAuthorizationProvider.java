package top.egon.cola.platform.rbac3.gateway.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.core.security.AuthorizationDecision;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayAuthorizationProvider;

import java.util.Objects;

/**
 * 类型 `Rbac3PermissionAuthorizationProvider` 位于当前包内，是类型，用于承载 `Rbac3 Permission Authorization Provider` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3PermissionAuthorizationProvider` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Permission Authorization Provider`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Evaluates the versioned API Permission mapping and fails closed on runtime errors.
 */
public final class Rbac3PermissionAuthorizationProvider
        implements GatewayAuthorizationProvider {

    /**
     * 字段 `PROVIDER_ID` 表示 `Rbac3PermissionAuthorizationProvider` 中与 `PROVIDER ID` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `PROVIDER_ID` stores the `PROVIDER ID`-related state, dependency, configuration, or result of `Rbac3PermissionAuthorizationProvider` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `PROVIDER_ID` 时应保持 `Rbac3PermissionAuthorizationProvider` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `PROVIDER_ID`, preserve `Rbac3PermissionAuthorizationProvider`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String PROVIDER_ID = "rbac3-permission";

    /**
     * 字段 `decisionSource` 表示 `Rbac3PermissionAuthorizationProvider` 中与 `decision Source` 相关的状态、依赖、配置或结果（声明类型 `DecisionSource`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `decisionSource` stores the `decision Source`-related state, dependency, configuration, or result of `Rbac3PermissionAuthorizationProvider` (declared type `DecisionSource`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `decisionSource` 时应保持 `Rbac3PermissionAuthorizationProvider` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `decisionSource`, preserve `Rbac3PermissionAuthorizationProvider`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DecisionSource decisionSource;

    /**
     * 构造器 `Rbac3PermissionAuthorizationProvider` 用于创建并初始化 `Rbac3PermissionAuthorizationProvider` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3PermissionAuthorizationProvider` creates and initializes `Rbac3PermissionAuthorizationProvider`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3PermissionAuthorizationProvider` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3PermissionAuthorizationProvider`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param decisionSource 输入参数 `decisionSource`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3PermissionAuthorizationProvider(DecisionSource decisionSource) {
        this.decisionSource = Objects.requireNonNull(decisionSource, "decisionSource");
    }

    /**
     * 方法 `providerId` 按照 `Rbac3PermissionAuthorizationProvider` 的职责处理输入，完成 `provider Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `providerId` processes its inputs according to `Rbac3PermissionAuthorizationProvider`'s responsibility, performs the `provider Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `providerId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `providerId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    /**
     * 方法 `authorize` 按照 `Rbac3PermissionAuthorizationProvider` 的职责处理输入，完成 `authorize` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorize` processes its inputs according to `Rbac3PermissionAuthorizationProvider`'s responsibility, performs the `authorize` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorize` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorize`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public Publisher<AuthorizationDecision> authorize(GatewayAuthContext context) {
        return Mono.fromCallable(() -> decisionSource.authorize(context))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(AuthorizationDecision.error(
                        "RBAC3_AUTHORIZATION_RUNTIME_UNAVAILABLE"));
    }

    /**
     * 类型 `DecisionSource` 位于 `Rbac3PermissionAuthorizationProvider` 内，是接口，用于承载 `Decision Source` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `DecisionSource` is an interface inside `Rbac3PermissionAuthorizationProvider` and carries the responsibility, state, or contract for `Decision Source`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `DecisionSource` 作为 `Rbac3PermissionAuthorizationProvider` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `DecisionSource` as the responsibility boundary of `Rbac3PermissionAuthorizationProvider`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface DecisionSource {
        /**
         * 方法 `authorize` 按照 `DecisionSource` 的职责处理输入，完成 `authorize` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `authorize` processes its inputs according to `DecisionSource`'s responsibility, performs the `authorize` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `authorize` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `authorize`, then continue the business flow using its result, exception, or side effect.
         *
         * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        AuthorizationDecision authorize(GatewayAuthContext context);
    }
}
