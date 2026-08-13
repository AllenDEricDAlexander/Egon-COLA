package top.egon.cola.platform.rbac3.admin.runtime.controller;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ReadinessCheckVO;

/**
 * 类型 `Rbac3ReadinessIndicator` 位于当前包内，是类型，用于承载 `Rbac3 Readiness Indicator` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3ReadinessIndicator` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Readiness Indicator`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Separates application readiness from Gateway release routeability.
 */
public final class Rbac3ReadinessIndicator implements HealthIndicator {

    /**
     * 字段 `applicationChecks` 表示 `Rbac3ReadinessIndicator` 中与 `application Checks` 相关的状态、依赖、配置或结果（声明类型 `List&lt;ReadinessCheckVO&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationChecks` stores the `application Checks`-related state, dependency, configuration, or result of `Rbac3ReadinessIndicator` (declared type `List&lt;ReadinessCheckVO&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationChecks` 时应保持 `Rbac3ReadinessIndicator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationChecks`, preserve `Rbac3ReadinessIndicator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final List<ReadinessCheckVO> applicationChecks;
    /**
     * 字段 `gatewayRouteability` 表示 `Rbac3ReadinessIndicator` 中与 `gateway Routeability` 相关的状态、依赖、配置或结果（声明类型 `Supplier&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `gatewayRouteability` stores the `gateway Routeability`-related state, dependency, configuration, or result of `Rbac3ReadinessIndicator` (declared type `Supplier&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `gatewayRouteability` 时应保持 `Rbac3ReadinessIndicator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `gatewayRouteability`, preserve `Rbac3ReadinessIndicator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Supplier<String> gatewayRouteability;
    /**
     * 字段 `acceptingTraffic` 表示 `Rbac3ReadinessIndicator` 中与 `accepting Traffic` 相关的状态、依赖、配置或结果（声明类型 `AtomicBoolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `acceptingTraffic` stores the `accepting Traffic`-related state, dependency, configuration, or result of `Rbac3ReadinessIndicator` (declared type `AtomicBoolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `acceptingTraffic` 时应保持 `Rbac3ReadinessIndicator` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `acceptingTraffic`, preserve `Rbac3ReadinessIndicator`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AtomicBoolean acceptingTraffic = new AtomicBoolean(true);

    /**
     * 构造器 `Rbac3ReadinessIndicator` 用于创建并初始化 `Rbac3ReadinessIndicator` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3ReadinessIndicator` creates and initializes `Rbac3ReadinessIndicator`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3ReadinessIndicator` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3ReadinessIndicator`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param applicationChecks 输入参数 `applicationChecks`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param gatewayRouteability 输入参数 `gatewayRouteability`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3ReadinessIndicator(
            List<ReadinessCheckVO> applicationChecks,
            Supplier<String> gatewayRouteability) {
        this.applicationChecks = List.copyOf(applicationChecks);
        this.gatewayRouteability = Objects.requireNonNull(
                gatewayRouteability, "gatewayRouteability");
        if (this.applicationChecks.isEmpty()) {
            throw new IllegalArgumentException(
                    "at least one application readiness check is required");
        }
    }

    /**
     * 方法 `health` 按照 `Rbac3ReadinessIndicator` 的职责处理输入，完成 `health` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `health` processes its inputs according to `Rbac3ReadinessIndicator`'s responsibility, performs the `health` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `health` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `health`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public Health health() {
        String routeability = safeRouteability();
        if (!acceptingTraffic.get()) {
            return Health.down()
                    .withDetail("failedCheck", "trafficAcceptance")
                    .withDetail("gatewayRouteability", routeability)
                    .build();
        }
        for (ReadinessCheckVO check : applicationChecks) {
            if (!safeReady(check)) {
                return Health.down()
                        .withDetail("failedCheck", check.name())
                        .withDetail("gatewayRouteability", routeability)
                        .build();
            }
        }
        return Health.up()
                .withDetail("gatewayRouteability", routeability)
                .build();
    }

    /**
     * 方法 `stopAcceptingTraffic` 按照 `Rbac3ReadinessIndicator` 的职责处理输入，完成 `stop Accepting Traffic` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `stopAcceptingTraffic` processes its inputs according to `Rbac3ReadinessIndicator`'s responsibility, performs the `stop Accepting Traffic` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `stopAcceptingTraffic` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `stopAcceptingTraffic`, then continue the business flow using its result, exception, or side effect.
     */
    public void stopAcceptingTraffic() {
        acceptingTraffic.set(false);
    }

    /**
     * 方法 `safeReady` 按照 `Rbac3ReadinessIndicator` 的职责处理输入，完成 `safe Ready` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `safeReady` processes its inputs according to `Rbac3ReadinessIndicator`'s responsibility, performs the `safe Ready` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `safeReady` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `safeReady`, then continue the business flow using its result, exception, or side effect.
     *
     * @param check 输入参数 `check`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private boolean safeReady(ReadinessCheckVO check) {
        try {
            return check.ready().getAsBoolean();
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    /**
     * 方法 `safeRouteability` 按照 `Rbac3ReadinessIndicator` 的职责处理输入，完成 `safe Routeability` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `safeRouteability` processes its inputs according to `Rbac3ReadinessIndicator`'s responsibility, performs the `safe Routeability` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `safeRouteability` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `safeRouteability`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String safeRouteability() {
        try {
            String value = gatewayRouteability.get();
            return value == null || value.isBlank() ? "UNKNOWN" : value;
        } catch (RuntimeException unavailable) {
            return "UNKNOWN";
        }
    }

    }
