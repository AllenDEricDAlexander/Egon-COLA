package top.egon.cola.platform.rbac3.admin.bootstrap.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 类型 `Rbac3PlatformAdminBootstrapCli` 位于当前包内，是类型，用于承载 `Rbac3 Platform Admin Bootstrap Cli` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3PlatformAdminBootstrapCli` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Platform Admin Bootstrap Cli`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * One-shot platform administrator bootstrap command.
 */
public final class Rbac3PlatformAdminBootstrapCli {

    /**
     * 字段 `COMMAND` 表示 `Rbac3PlatformAdminBootstrapCli` 中与 `COMMAND` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `COMMAND` stores the `COMMAND`-related state, dependency, configuration, or result of `Rbac3PlatformAdminBootstrapCli` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `COMMAND` 时应保持 `Rbac3PlatformAdminBootstrapCli` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `COMMAND`, preserve `Rbac3PlatformAdminBootstrapCli`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String COMMAND = "bootstrap-platform-admin";
    /**
     * 字段 `ALLOWED_OPTIONS` 表示 `Rbac3PlatformAdminBootstrapCli` 中与 `ALLOWED OPTIONS` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `ALLOWED_OPTIONS` stores the `ALLOWED OPTIONS`-related state, dependency, configuration, or result of `Rbac3PlatformAdminBootstrapCli` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `ALLOWED_OPTIONS` 时应保持 `Rbac3PlatformAdminBootstrapCli` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `ALLOWED_OPTIONS`, preserve `Rbac3PlatformAdminBootstrapCli`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Set<String> ALLOWED_OPTIONS = Set.of(
            "--tenant-code", "--username");

    /**
     * 字段 `bootstrapPort` 表示 `Rbac3PlatformAdminBootstrapCli` 中与 `bootstrap Port` 相关的状态、依赖、配置或结果（声明类型 `BootstrapPort`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `bootstrapPort` stores the `bootstrap Port`-related state, dependency, configuration, or result of `Rbac3PlatformAdminBootstrapCli` (declared type `BootstrapPort`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `bootstrapPort` 时应保持 `Rbac3PlatformAdminBootstrapCli` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `bootstrapPort`, preserve `Rbac3PlatformAdminBootstrapCli`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final BootstrapPort bootstrapPort;

    /**
     * 构造器 `Rbac3PlatformAdminBootstrapCli` 用于创建并初始化 `Rbac3PlatformAdminBootstrapCli` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3PlatformAdminBootstrapCli` creates and initializes `Rbac3PlatformAdminBootstrapCli`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3PlatformAdminBootstrapCli` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3PlatformAdminBootstrapCli`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param bootstrapPort 输入参数 `bootstrapPort`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3PlatformAdminBootstrapCli(BootstrapPort bootstrapPort) {
        this.bootstrapPort = Objects.requireNonNull(bootstrapPort, "bootstrapPort");
    }

    /**
     * 方法 `run` 按照 `Rbac3PlatformAdminBootstrapCli` 的职责处理输入，完成 `run` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `run` processes its inputs according to `Rbac3PlatformAdminBootstrapCli`'s responsibility, performs the `run` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `run` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `run`, then continue the business flow using its result, exception, or side effect.
     *
     * @param arguments 输入参数 `arguments`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param passwordInput 输入参数 `passwordInput`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public int run(String[] arguments, InputStream passwordInput) {
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(passwordInput, "passwordInput");
        Map<String, String> options = parse(arguments);
        char[] password = readPassword(passwordInput);
        try {
            bootstrapPort.bootstrap(
                    required(options, "--tenant-code"),
                    required(options, "--username"),
                    password);
            return 0;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * 方法 `parse` 按照 `Rbac3PlatformAdminBootstrapCli` 的职责处理输入，完成 `parse` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `parse` processes its inputs according to `Rbac3PlatformAdminBootstrapCli`'s responsibility, performs the `parse` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `parse` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `parse`, then continue the business flow using its result, exception, or side effect.
     *
     * @param arguments 输入参数 `arguments`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static Map<String, String> parse(String[] arguments) {
        if (arguments.length == 0 || !COMMAND.equals(arguments[0])) {
            throw new IllegalArgumentException("expected command " + COMMAND);
        }
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 1; index < arguments.length; index += 2) {
            String option = arguments[index];
            if ("--password".equals(option) || option.startsWith("--password=")) {
                throw new IllegalArgumentException("password must not be supplied as an argument");
            }
            if (!ALLOWED_OPTIONS.contains(option)) {
                throw new IllegalArgumentException("unsupported option " + option);
            }
            if (index + 1 >= arguments.length || !option.startsWith("--")) {
                throw new IllegalArgumentException("invalid bootstrap argument list");
            }
            if (options.putIfAbsent(option, arguments[index + 1]) != null) {
                throw new IllegalArgumentException("duplicate option " + option);
            }
        }
        return options;
    }

    /**
     * 方法 `readPassword` 按照 `Rbac3PlatformAdminBootstrapCli` 的职责处理输入，完成 `read Password` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `readPassword` processes its inputs according to `Rbac3PlatformAdminBootstrapCli`'s responsibility, performs the `read Password` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `readPassword` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `readPassword`, then continue the business flow using its result, exception, or side effect.
     *
     * @param input 输入参数 `input`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static char[] readPassword(InputStream input) {
        try {
            InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            char[] buffer = new char[65];
            int length = 0;
            int value;
            while ((value = reader.read()) >= 0 && value != '\n' && value != '\r') {
                if (length == buffer.length) {
                    Arrays.fill(buffer, '\0');
                    throw new IllegalArgumentException("password must not exceed 64 characters");
                }
                buffer[length++] = (char) value;
            }
            if (length < 12) {
                Arrays.fill(buffer, '\0');
                throw new IllegalArgumentException(
                        "password must contain 12 to 64 characters");
            }
            char[] password = Arrays.copyOf(buffer, length);
            Arrays.fill(buffer, '\0');
            return password;
        } catch (IOException exception) {
            throw new IllegalStateException("unable to read bootstrap password", exception);
        }
    }

    /**
     * 方法 `required` 按照 `Rbac3PlatformAdminBootstrapCli` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `Rbac3PlatformAdminBootstrapCli`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param options 输入参数 `options`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    /**
     * 类型 `BootstrapPort` 位于 `Rbac3PlatformAdminBootstrapCli` 内，是接口，用于承载 `Bootstrap Port` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `BootstrapPort` is an interface inside `Rbac3PlatformAdminBootstrapCli` and carries the responsibility, state, or contract for `Bootstrap Port`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `BootstrapPort` 作为 `Rbac3PlatformAdminBootstrapCli` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `BootstrapPort` as the responsibility boundary of `Rbac3PlatformAdminBootstrapCli`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
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
         * @param password 输入参数 `password`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void bootstrap(String tenantCode, String username, char[] password);
    }
}
