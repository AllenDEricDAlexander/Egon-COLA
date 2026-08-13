package top.egon.cola.platform.rbac3.admin.runtime.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.model.instance.DdcRuntimeState;
import top.egon.cola.component.ddc.http.registration.DdcHttpRegistrationProperties;
import top.egon.cola.component.ddc.http.registration.DdcHttpRegistrationRuntime;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 类型 `Rbac3HttpProviderPublicationGate` 位于当前包内，是类型，用于承载 `Rbac3 Http Provider Publication Gate` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3HttpProviderPublicationGate` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Http Provider Publication Gate`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Publishes the HTTP provider only after the configuration client is ready.
 */
public final class Rbac3HttpProviderPublicationGate
        implements ApplicationListener<ApplicationEvent> {

    /**
     * 字段 `coordinator` 表示 `Rbac3HttpProviderPublicationGate` 中与 `coordinator` 相关的状态、依赖、配置或结果（声明类型 `DdcRuntimeCoordinator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `coordinator` stores the `coordinator`-related state, dependency, configuration, or result of `Rbac3HttpProviderPublicationGate` (declared type `DdcRuntimeCoordinator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `coordinator` 时应保持 `Rbac3HttpProviderPublicationGate` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `coordinator`, preserve `Rbac3HttpProviderPublicationGate`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DdcRuntimeCoordinator coordinator;
    /**
     * 字段 `providerRuntime` 表示 `Rbac3HttpProviderPublicationGate` 中与 `provider Runtime` 相关的状态、依赖、配置或结果（声明类型 `DdcHttpRegistrationRuntime`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `providerRuntime` stores the `provider Runtime`-related state, dependency, configuration, or result of `Rbac3HttpProviderPublicationGate` (declared type `DdcHttpRegistrationRuntime`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `providerRuntime` 时应保持 `Rbac3HttpProviderPublicationGate` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `providerRuntime`, preserve `Rbac3HttpProviderPublicationGate`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DdcHttpRegistrationRuntime providerRuntime;
    /**
     * 字段 `providerProperties` 表示 `Rbac3HttpProviderPublicationGate` 中与 `provider Properties` 相关的状态、依赖、配置或结果（声明类型 `DdcHttpRegistrationProperties`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `providerProperties` stores the `provider Properties`-related state, dependency, configuration, or result of `Rbac3HttpProviderPublicationGate` (declared type `DdcHttpRegistrationProperties`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `providerProperties` 时应保持 `Rbac3HttpProviderPublicationGate` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `providerProperties`, preserve `Rbac3HttpProviderPublicationGate`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final DdcHttpRegistrationProperties providerProperties;
    /**
     * 字段 `applicationReady` 表示 `Rbac3HttpProviderPublicationGate` 中与 `application Ready` 相关的状态、依赖、配置或结果（声明类型 `AtomicBoolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `applicationReady` stores the `application Ready`-related state, dependency, configuration, or result of `Rbac3HttpProviderPublicationGate` (declared type `AtomicBoolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `applicationReady` 时应保持 `Rbac3HttpProviderPublicationGate` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `applicationReady`, preserve `Rbac3HttpProviderPublicationGate`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AtomicBoolean applicationReady = new AtomicBoolean();
    /**
     * 字段 `published` 表示 `Rbac3HttpProviderPublicationGate` 中与 `published` 相关的状态、依赖、配置或结果（声明类型 `AtomicBoolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `published` stores the `published`-related state, dependency, configuration, or result of `Rbac3HttpProviderPublicationGate` (declared type `AtomicBoolean`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `published` 时应保持 `Rbac3HttpProviderPublicationGate` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `published`, preserve `Rbac3HttpProviderPublicationGate`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AtomicBoolean published = new AtomicBoolean();

    /**
     * 字段 `serverPort` 表示 `Rbac3HttpProviderPublicationGate` 中与 `server Port` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `serverPort` stores the `server Port`-related state, dependency, configuration, or result of `Rbac3HttpProviderPublicationGate` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `serverPort` 时应保持 `Rbac3HttpProviderPublicationGate` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `serverPort`, preserve `Rbac3HttpProviderPublicationGate`'s lifecycle, immutability, and thread-safety constraints.
     */
    private volatile int serverPort;

    /**
     * 构造器 `Rbac3HttpProviderPublicationGate` 用于创建并初始化 `Rbac3HttpProviderPublicationGate` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3HttpProviderPublicationGate` creates and initializes `Rbac3HttpProviderPublicationGate`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3HttpProviderPublicationGate` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3HttpProviderPublicationGate`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param coordinator 输入参数 `coordinator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param providerRuntime 输入参数 `providerRuntime`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param providerProperties 输入参数 `providerProperties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3HttpProviderPublicationGate(
            DdcRuntimeCoordinator coordinator,
            DdcHttpRegistrationRuntime providerRuntime,
            DdcHttpRegistrationProperties providerProperties) {
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.providerRuntime = Objects.requireNonNull(providerRuntime, "providerRuntime");
        this.providerProperties = Objects.requireNonNull(
                providerProperties, "providerProperties");
    }

    /**
     * 方法 `onApplicationEvent` 按照 `Rbac3HttpProviderPublicationGate` 的职责处理输入，完成 `on Application Event` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `onApplicationEvent` processes its inputs according to `Rbac3HttpProviderPublicationGate`'s responsibility, performs the `on Application Event` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `onApplicationEvent` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `onApplicationEvent`, then continue the business flow using its result, exception, or side effect.
     *
     * @param event 输入参数 `event`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof WebServerInitializedEvent initialized) {
            String namespace = initialized.getApplicationContext().getServerNamespace();
            if (namespace == null || namespace.isBlank()) {
                serverPort = initialized.getWebServer().getPort();
                tryPublish();
            }
            return;
        }
        if (event instanceof ApplicationReadyEvent) {
            requireConfigClientReady();
            applicationReady.set(true);
            tryPublish();
        }
    }

    /**
     * 方法 `tryPublish` 按照 `Rbac3HttpProviderPublicationGate` 的职责处理输入，完成 `try Publish` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `tryPublish` processes its inputs according to `Rbac3HttpProviderPublicationGate`'s responsibility, performs the `try Publish` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `tryPublish` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `tryPublish`, then continue the business flow using its result, exception, or side effect.
     */
    private void tryPublish() {
        if (!applicationReady.get() || serverPort <= 0 || published.get()) {
            return;
        }
        requireConfigClientReady();
        int configuredPort = providerProperties.getPort();
        if (configuredPort > 0 && configuredPort != serverPort) {
            throw new IllegalStateException(
                    "RBAC3 HTTP provider port does not match the root web server");
        }
        if (published.compareAndSet(false, true)) {
            providerRuntime.onHttpServerReady(serverPort);
        }
    }

    /**
     * 方法 `requireConfigClientReady` 按照 `Rbac3HttpProviderPublicationGate` 的职责处理输入，完成 `require Config Client Ready` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `requireConfigClientReady` processes its inputs according to `Rbac3HttpProviderPublicationGate`'s responsibility, performs the `require Config Client Ready` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `requireConfigClientReady` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `requireConfigClientReady`, then continue the business flow using its result, exception, or side effect.
     */
    private void requireConfigClientReady() {
        if (coordinator.state() != DdcRuntimeState.READY) {
            throw new IllegalStateException("DDC config client is not ready");
        }
        boolean sessionPresent = coordinator.currentSession()
                .filter(session -> session.role() == DdcLeaseRole.CONFIG_CLIENT)
                .isPresent();
        if (!sessionPresent) {
            throw new IllegalStateException("DDC config client session is missing");
        }
    }
}
