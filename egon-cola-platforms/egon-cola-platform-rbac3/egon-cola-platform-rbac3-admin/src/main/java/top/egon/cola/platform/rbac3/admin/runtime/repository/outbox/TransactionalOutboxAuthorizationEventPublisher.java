package top.egon.cola.platform.rbac3.admin.runtime.repository.outbox;

import top.egon.cola.component.outbox.api.OutboxMessage;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.rbac3.admin.runtime.domain.Rbac3RuntimeEventCatalog;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.AuthorizationEventVO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.AuthorizationEventPublisher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 类型 `TransactionalOutboxAuthorizationEventPublisher` 位于当前包内，是类型，用于承载 `Transactional Outbox Authorization Event Adapter` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `TransactionalOutboxAuthorizationEventPublisher` is a type in its package and carries the responsibility, state, or contract for `Transactional Outbox Authorization Event Adapter`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Adapts RBAC3 logical authorization events to the public transactional-outbox API.
 */
public final class TransactionalOutboxAuthorizationEventPublisher
        implements AuthorizationEventPublisher {

    /**
     * 字段 `CHANNEL` 表示 `TransactionalOutboxAuthorizationEventPublisher` 中与 `CHANNEL` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `CHANNEL` stores the `CHANNEL`-related state, dependency, configuration, or result of `TransactionalOutboxAuthorizationEventPublisher` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `CHANNEL` 时应保持 `TransactionalOutboxAuthorizationEventPublisher` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `CHANNEL`, preserve `TransactionalOutboxAuthorizationEventPublisher`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String CHANNEL = Rbac3RuntimeEventCatalog.CHANNEL;

    /**
     * 字段 `DESTINATIONS` 表示 `TransactionalOutboxAuthorizationEventPublisher` 中与 `DESTINATIONS` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `DESTINATIONS` stores the `DESTINATIONS`-related state, dependency, configuration, or result of `TransactionalOutboxAuthorizationEventPublisher` (declared type `Map&lt;String, String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `DESTINATIONS` 时应保持 `TransactionalOutboxAuthorizationEventPublisher` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `DESTINATIONS`, preserve `TransactionalOutboxAuthorizationEventPublisher`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Map<String, String> DESTINATIONS = Map.ofEntries(
            Map.entry("RBAC3_USER_ACTIVE_ROLES_REPLACED",
                    "rbac3.role-activation.changed.v1"),
            Map.entry("ASSIGNMENT_CHANGED", "rbac3.assignment.changed.v1"),
            Map.entry("RESOURCE_CATALOG_UPDATED", "rbac3.resource.catalog.updated.v1"),
            Map.entry("RESOURCE_ARCHIVED", "rbac3.role.policy-changed.v1"),
            Map.entry("ROLE_CREATED", "rbac3.role.policy-changed.v1"),
            Map.entry("ROLE_UPDATED", "rbac3.role.policy-changed.v1"),
            Map.entry("ROLE_INHERITANCE_CHANGED", "rbac3.role.policy-changed.v1"),
            Map.entry("ROLE_PERMISSION_CHANGED", "rbac3.role.policy-changed.v1"),
            Map.entry("SOD_SET_CHANGED", "rbac3.role.policy-changed.v1"),
            Map.entry("ROLE_PREREQUISITE_CHANGED", "rbac3.role.policy-changed.v1"),
            Map.entry("ROLE_CARDINALITY_CHANGED", "rbac3.role.policy-changed.v1"),
            Map.entry("DATA_RULE_CHANGED", "rbac3.role.policy-changed.v1"),
            Map.entry("FIELD_RULE_CHANGED", "rbac3.role.policy-changed.v1"),
            Map.entry("OPERATION_SOD_RULE_CHANGED", "rbac3.role.policy-changed.v1"));

    /**
     * 字段 `outbox` 表示 `TransactionalOutboxAuthorizationEventPublisher` 中与 `outbox` 相关的状态、依赖、配置或结果（声明类型 `TransactionalOutbox`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `outbox` stores the `outbox`-related state, dependency, configuration, or result of `TransactionalOutboxAuthorizationEventPublisher` (declared type `TransactionalOutbox`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `outbox` 时应保持 `TransactionalOutboxAuthorizationEventPublisher` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `outbox`, preserve `TransactionalOutboxAuthorizationEventPublisher`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final TransactionalOutbox outbox;
    /**
     * 字段 `clock` 表示 `TransactionalOutboxAuthorizationEventPublisher` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `TransactionalOutboxAuthorizationEventPublisher` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `TransactionalOutboxAuthorizationEventPublisher` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `TransactionalOutboxAuthorizationEventPublisher`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /**
     * 构造器 `TransactionalOutboxAuthorizationEventPublisher` 用于创建并初始化 `TransactionalOutboxAuthorizationEventPublisher` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `TransactionalOutboxAuthorizationEventPublisher` creates and initializes `TransactionalOutboxAuthorizationEventPublisher`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `TransactionalOutboxAuthorizationEventPublisher` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `TransactionalOutboxAuthorizationEventPublisher`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param outbox 输入参数 `outbox`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public TransactionalOutboxAuthorizationEventPublisher(
            TransactionalOutbox outbox,
            Clock clock) {
        this.outbox = Objects.requireNonNull(outbox, "outbox");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 方法 `enqueue` 按照 `TransactionalOutboxAuthorizationEventPublisher` 的职责处理输入，完成 `enqueue` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `enqueue` processes its inputs according to `TransactionalOutboxAuthorizationEventPublisher`'s responsibility, performs the `enqueue` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `enqueue` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `enqueue`, then continue the business flow using its result, exception, or side effect.
     *
     * @param event 输入参数 `event`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public String enqueue(AuthorizationEventVO event) {
        Objects.requireNonNull(event, "event");
        String destination = destination(event.eventType());
        long aggregateVersion = aggregateVersion(event.safePayload());
        String idempotencyKey = event.tenantId() + ':' + destination + ':'
                + event.aggregateId() + ':' + aggregateVersion;
        String eventId = sha256(idempotencyKey);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", destination);
        envelope.put("schemaVersion", 1);
        envelope.put("occurredAt", clock.instant());
        envelope.put("tenantId", event.tenantId());
        envelope.put("aggregateType", event.aggregateType());
        envelope.put("aggregateId", event.aggregateId());
        envelope.put("aggregateVersion", aggregateVersion);
        envelope.put("traceId", event.traceId());
        envelope.put("payload", event.safePayload());
        return outbox.enqueue(OutboxMessage.builder()
                        .messageId(eventId)
                        .idempotencyKey(idempotencyKey)
                        .channel(CHANNEL)
                        .destination(destination)
                        .payload(envelope)
                        .schemaVersion("1")
                        .traceId(event.traceId())
                        .build())
                .messageId();
    }

    /**
     * 方法 `destination` 按照 `TransactionalOutboxAuthorizationEventPublisher` 的职责处理输入，完成 `destination` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `destination` processes its inputs according to `TransactionalOutboxAuthorizationEventPublisher`'s responsibility, performs the `destination` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `destination` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `destination`, then continue the business flow using its result, exception, or side effect.
     *
     * @param internalEventType 输入参数 `internalEventType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    static String destination(String internalEventType) {
        String normalized = required(internalEventType, "eventType")
                .toUpperCase(Locale.ROOT);
        String destination = DESTINATIONS.get(normalized);
        if (destination == null && normalized.endsWith("_CHANGED")) {
            destination = "rbac3.role.policy-changed.v1";
        }
        if (destination == null) {
            throw new IllegalArgumentException(
                    "unsupported RBAC3 authorization event type: " + normalized);
        }
        return destination;
    }

    /**
     * 方法 `aggregateVersion` 按照 `TransactionalOutboxAuthorizationEventPublisher` 的职责处理输入，完成 `aggregate Version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `aggregateVersion` processes its inputs according to `TransactionalOutboxAuthorizationEventPublisher`'s responsibility, performs the `aggregate Version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `aggregateVersion` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `aggregateVersion`, then continue the business flow using its result, exception, or side effect.
     *
     * @param payload 输入参数 `payload`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private long aggregateVersion(Map<String, String> payload) {
        for (String key : new String[]{
                "aggregateVersion", "contextVersion", "authVersion",
                "policyVersion", "manifestVersion"}) {
            String value = payload.get(key);
            if (value != null) {
                try {
                    long parsed = Long.parseLong(value);
                    if (parsed >= 0L) {
                        return parsed;
                    }
                } catch (NumberFormatException ignored) {
                    // Continue to the stable validation error below.
                }
            }
        }
        throw new IllegalArgumentException(
                "RBAC3 authorization event requires a non-negative aggregate version");
    }

    /**
     * 方法 `sha256` 按照 `TransactionalOutboxAuthorizationEventPublisher` 的职责处理输入，完成 `sha256` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `sha256` processes its inputs according to `TransactionalOutboxAuthorizationEventPublisher`'s responsibility, performs the `sha256` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `sha256` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `sha256`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is unavailable", unavailable);
        }
    }

    /**
     * 方法 `required` 按照 `TransactionalOutboxAuthorizationEventPublisher` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `TransactionalOutboxAuthorizationEventPublisher`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
