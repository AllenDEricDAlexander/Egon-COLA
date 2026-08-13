package top.egon.cola.platform.rbac3.starter.manifest;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import top.egon.cola.platform.rbac3.contract.manifest.ResourceManifest;

import java.util.List;
import java.util.Objects;

/**
 * 类型 `Rbac3ManifestReporter` 位于当前包内，是类型，用于承载 `Rbac3 Manifest Reporter` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3ManifestReporter` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Manifest Reporter`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Reports startup manifests using service identity supplied by the application.
 */
public final class Rbac3ManifestReporter implements ApplicationRunner {

    /**
     * 字段 `contributors` 表示 `Rbac3ManifestReporter` 中与 `contributors` 相关的状态、依赖、配置或结果（声明类型 `List&lt;Rbac3ManifestContributor&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `contributors` stores the `contributors`-related state, dependency, configuration, or result of `Rbac3ManifestReporter` (declared type `List&lt;Rbac3ManifestContributor&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `contributors` 时应保持 `Rbac3ManifestReporter` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `contributors`, preserve `Rbac3ManifestReporter`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final List<Rbac3ManifestContributor> contributors;
    /**
     * 字段 `transport` 表示 `Rbac3ManifestReporter` 中与 `transport` 相关的状态、依赖、配置或结果（声明类型 `ManifestTransport`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `transport` stores the `transport`-related state, dependency, configuration, or result of `Rbac3ManifestReporter` (declared type `ManifestTransport`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `transport` 时应保持 `Rbac3ManifestReporter` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `transport`, preserve `Rbac3ManifestReporter`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ManifestTransport transport;
    /**
     * 字段 `credentialSupplier` 表示 `Rbac3ManifestReporter` 中与 `credential Supplier` 相关的状态、依赖、配置或结果（声明类型 `ServiceCredentialSupplier`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `credentialSupplier` stores the `credential Supplier`-related state, dependency, configuration, or result of `Rbac3ManifestReporter` (declared type `ServiceCredentialSupplier`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `credentialSupplier` 时应保持 `Rbac3ManifestReporter` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `credentialSupplier`, preserve `Rbac3ManifestReporter`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ServiceCredentialSupplier credentialSupplier;

    /**
     * 构造器 `Rbac3ManifestReporter` 用于创建并初始化 `Rbac3ManifestReporter` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3ManifestReporter` creates and initializes `Rbac3ManifestReporter`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3ManifestReporter` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3ManifestReporter`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param contributors 输入参数 `contributors`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param transport 输入参数 `transport`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param credentialSupplier 输入参数 `credentialSupplier`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3ManifestReporter(
            List<Rbac3ManifestContributor> contributors,
            ManifestTransport transport,
            ServiceCredentialSupplier credentialSupplier
    ) {
        this.contributors = List.copyOf(contributors);
        this.transport = Objects.requireNonNull(transport, "transport");
        this.credentialSupplier = Objects.requireNonNull(
                credentialSupplier, "credentialSupplier");
    }

    /**
     * 方法 `run` 按照 `Rbac3ManifestReporter` 的职责处理输入，完成 `run` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `run` processes its inputs according to `Rbac3ManifestReporter`'s responsibility, performs the `run` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `run` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `run`, then continue the business flow using its result, exception, or side effect.
     *
     * @param args 输入参数 `args`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    public void run(ApplicationArguments args) {
        ServiceCredential credential = credentialSupplier.get();
        contributors.stream()
                .map(Rbac3ManifestContributor::contribute)
                .forEach(manifest -> transport.report(manifest, credential));
    }

    /**
     * 类型 `ManifestTransport` 位于 `Rbac3ManifestReporter` 内，是接口，用于承载 `Manifest Transport` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ManifestTransport` is an interface inside `Rbac3ManifestReporter` and carries the responsibility, state, or contract for `Manifest Transport`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ManifestTransport` 作为 `Rbac3ManifestReporter` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ManifestTransport` as the responsibility boundary of `Rbac3ManifestReporter`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface ManifestTransport {
        /**
         * 方法 `report` 按照 `ManifestTransport` 的职责处理输入，完成 `report` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `report` processes its inputs according to `ManifestTransport`'s responsibility, performs the `report` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `report` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `report`, then continue the business flow using its result, exception, or side effect.
         *
         * @param manifest 输入参数 `manifest`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param credential 输入参数 `credential`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void report(ResourceManifest manifest, ServiceCredential credential);
    }

    /**
     * 类型 `ServiceCredentialSupplier` 位于 `Rbac3ManifestReporter` 内，是接口，用于承载 `Service Credential Supplier` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ServiceCredentialSupplier` is an interface inside `Rbac3ManifestReporter` and carries the responsibility, state, or contract for `Service Credential Supplier`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ServiceCredentialSupplier` 作为 `Rbac3ManifestReporter` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ServiceCredentialSupplier` as the responsibility boundary of `Rbac3ManifestReporter`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface ServiceCredentialSupplier {
        /**
         * 方法 `get` 按照 `ServiceCredentialSupplier` 的职责处理输入，完成 `get` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `get` processes its inputs according to `ServiceCredentialSupplier`'s responsibility, performs the `get` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `get` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `get`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        ServiceCredential get();
    }

    /**
     * 类型 `ServiceCredential` 位于 `Rbac3ManifestReporter` 内，是记录类型，用于承载 `Service Credential` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ServiceCredential` is a record inside `Rbac3ManifestReporter` and carries the responsibility, state, or contract for `Service Credential`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ServiceCredential` 作为 `Rbac3ManifestReporter` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ServiceCredential` as the responsibility boundary of `Rbac3ManifestReporter`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param clientId 记录组件 `clientId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `clientId` carries constructor data whose meaning is defined by the record contract.
     * @param secret 记录组件 `secret` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `secret` carries constructor data whose meaning is defined by the record contract.
     */
    public record ServiceCredential(/**
 * 字段 `clientId` 表示 `ServiceCredential` 中与 `client Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `clientId` stores the `client Id`-related state, dependency, configuration, or result of `ServiceCredential` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `clientId` 时应保持 `ServiceCredential` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `clientId`, preserve `ServiceCredential`'s lifecycle, immutability, and thread-safety constraints.
 */ String clientId, /**
 * 字段 `secret` 表示 `ServiceCredential` 中与 `secret` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `secret` stores the `secret`-related state, dependency, configuration, or result of `ServiceCredential` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `secret` 时应保持 `ServiceCredential` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `secret`, preserve `ServiceCredential`'s lifecycle, immutability, and thread-safety constraints.
 */ String secret) {
        /**
         * 构造器 `ServiceCredential` 用于创建并初始化 `ServiceCredential` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `ServiceCredential` creates and initializes `ServiceCredential`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `ServiceCredential` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `ServiceCredential`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param clientId 输入参数 `clientId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param secret 输入参数 `secret`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public ServiceCredential {
            clientId = required(clientId, "clientId");
            secret = required(secret, "secret");
        }

        /**
         * 方法 `required` 按照 `ServiceCredential` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `required` processes its inputs according to `ServiceCredential`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
         *
         * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        private static String required(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(name + " is required");
            }
            return value.trim();
        }
    }
}
