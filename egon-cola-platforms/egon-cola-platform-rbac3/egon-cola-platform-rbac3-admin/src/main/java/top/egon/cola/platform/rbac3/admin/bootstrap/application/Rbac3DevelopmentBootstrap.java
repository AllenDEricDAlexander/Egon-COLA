package top.egon.cola.platform.rbac3.admin.bootstrap.application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 类型 `Rbac3DevelopmentBootstrap` 位于当前包内，是类型，用于承载 `Rbac3 Development Bootstrap` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3DevelopmentBootstrap` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Development Bootstrap`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 *
 * Idempotently completes the local unified-identity tenant after platform bootstrap.
 */
@Component
@Profile("local")
@ConditionalOnProperty(
        prefix = "egon.rbac3.development-bootstrap",
        name = "enabled",
        havingValue = "true")
public class Rbac3DevelopmentBootstrap implements ApplicationRunner {

    /**
     * 字段 `bootstrap` 表示 `Rbac3DevelopmentBootstrap` 中与 `bootstrap` 相关的状态、依赖、配置或结果（声明类型 `BootstrapPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `bootstrap` stores the `bootstrap`-related state, dependency, configuration, or result of `Rbac3DevelopmentBootstrap` (declared type `BootstrapPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `bootstrap` 时应保持 `Rbac3DevelopmentBootstrap` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `bootstrap`, preserve `Rbac3DevelopmentBootstrap`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final BootstrapPort bootstrap;
    /**
     * 字段 `tenantCodes` 表示 `Rbac3DevelopmentBootstrap` 中与 `tenant Codes` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tenantCodes` stores the `tenant Codes`-related state, dependency, configuration, or result of `Rbac3DevelopmentBootstrap` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tenantCodes` 时应保持 `Rbac3DevelopmentBootstrap` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tenantCodes`, preserve `Rbac3DevelopmentBootstrap`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final List<String> tenantCodes;
    /**
     * 字段 `username` 表示 `Rbac3DevelopmentBootstrap` 中与 `username` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `username` stores the `username`-related state, dependency, configuration, or result of `Rbac3DevelopmentBootstrap` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `username` 时应保持 `Rbac3DevelopmentBootstrap` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `username`, preserve `Rbac3DevelopmentBootstrap`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final String username;
    /**
     * 字段 `identitySub` 表示 `Rbac3DevelopmentBootstrap` 中与 `identity Sub` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `identitySub` stores the `identity Sub`-related state, dependency, configuration, or result of `Rbac3DevelopmentBootstrap` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `identitySub` 时应保持 `Rbac3DevelopmentBootstrap` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `identitySub`, preserve `Rbac3DevelopmentBootstrap`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final String identitySub;

    /**
     * 构造器 `Rbac3DevelopmentBootstrap` 用于创建并初始化 `Rbac3DevelopmentBootstrap` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3DevelopmentBootstrap` creates and initializes `Rbac3DevelopmentBootstrap`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3DevelopmentBootstrap` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3DevelopmentBootstrap`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param bootstrap 输入参数 `bootstrap`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param tenantCodes 输入参数 `tenantCodes`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param username 输入参数 `username`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3DevelopmentBootstrap(
            BootstrapPort bootstrap,
            @Value("${egon.rbac3.development-bootstrap.tenant-codes:${egon.rbac3.development-bootstrap.tenant-code:default}}")
            String tenantCodes,
            @Value("${egon.rbac3.development-bootstrap.username:alice}")
            String username,
            @Value("${egon.rbac3.development-bootstrap.identity-sub:}")
            String identitySub) {
        this.bootstrap = Objects.requireNonNull(bootstrap, "bootstrap");
        this.tenantCodes = Arrays.stream(requireText(tenantCodes, "tenantCodes").split(","))
                .map(value -> requireText(value, "tenantCode"))
                .distinct()
                .toList();
        this.username = requireText(username, "username");
        this.identitySub = requireText(identitySub, "identitySub");
    }

    /**
     * 方法 `run` 按照 `Rbac3DevelopmentBootstrap` 的职责处理输入，完成 `run` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `run` processes its inputs according to `Rbac3DevelopmentBootstrap`'s responsibility, performs the `run` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `run` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `run`, then continue the business flow using its result, exception, or side effect.
     *
     * @param args 输入参数 `args`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    public void run(ApplicationArguments args) {
        tenantCodes.forEach(tenantCode ->
                bootstrap.bootstrap(tenantCode, username, identitySub));
    }

    /**
     * 方法 `requireText` 按照 `Rbac3DevelopmentBootstrap` 的职责处理输入，完成 `require Text` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireText` processes its inputs according to `Rbac3DevelopmentBootstrap`'s responsibility, performs the `require Text` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireText` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireText`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    /**
     * 类型 `BootstrapPort` 位于 `Rbac3DevelopmentBootstrap` 内，是接口，用于承载 `Bootstrap Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `BootstrapPort` is an interface inside `Rbac3DevelopmentBootstrap` and carries the responsibility, state, or contract for `Bootstrap Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `BootstrapPort` 作为 `Rbac3DevelopmentBootstrap` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `BootstrapPort` as the responsibility boundary of `Rbac3DevelopmentBootstrap`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface BootstrapPort {

        /**
         * 方法 `bootstrap` 按照 `BootstrapPort` 的职责处理输入，完成 `bootstrap` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `bootstrap` processes its inputs according to `BootstrapPort`'s responsibility, performs the `bootstrap` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `bootstrap` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `bootstrap`, then continue the business flow using its result, exception, or side effect.
         *
         * @param tenantCode 输入参数 `tenantCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param username 输入参数 `username`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param identitySub 输入参数 `identitySub`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void bootstrap(String tenantCode, String username, String identitySub);
    }
}
