package top.egon.cola.platform.rbac3.admin.integration.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import top.egon.cola.component.outbox.delivery.DeliveryContext;
import top.egon.cola.component.outbox.delivery.DeliveryHandler;
import top.egon.cola.component.outbox.delivery.DeliveryResult;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 类型 `Rbac3RuntimeProjectionDeliveryHandler` 位于当前包内，是类型，用于承载 `Rbac3 Runtime Projection Delivery Handler` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3RuntimeProjectionDeliveryHandler` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Runtime Projection Delivery Handler`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Dispatches the fixed RBAC3 logical event catalog to the runtime projector.
 */
public final class Rbac3RuntimeProjectionDeliveryHandler implements DeliveryHandler {

    /**
     * 字段 `DESTINATIONS` 表示 `Rbac3RuntimeProjectionDeliveryHandler` 中与 `DESTINATIONS` 相关的状态、依赖、配置或结果（声明类型 `Set&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `DESTINATIONS` stores the `DESTINATIONS`-related state, dependency, configuration, or result of `Rbac3RuntimeProjectionDeliveryHandler` (declared type `Set&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `DESTINATIONS` 时应保持 `Rbac3RuntimeProjectionDeliveryHandler` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `DESTINATIONS`, preserve `Rbac3RuntimeProjectionDeliveryHandler`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Set<String> DESTINATIONS = Set.of(
            "rbac3.directory.snapshot-activated.v1",
            "rbac3.user.status-changed.v1",
            "rbac3.assignment.changed.v1",
            "rbac3.role.policy-changed.v1",
            "rbac3.management-policy.changed.v1",
            "rbac3.role-activation.changed.v1",
            "rbac3.manifest.activated.v1",
            "rbac3.session.revoked.v1",
            "rbac3.authorization.mutation-committed.v1",
            "rbac3.participation.recorded.v1");

    /**
     * 字段 `sink` 表示 `Rbac3RuntimeProjectionDeliveryHandler` 中与 `sink` 相关的状态、依赖、配置或结果（声明类型 `ProjectionSink`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sink` stores the `sink`-related state, dependency, configuration, or result of `Rbac3RuntimeProjectionDeliveryHandler` (declared type `ProjectionSink`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sink` 时应保持 `Rbac3RuntimeProjectionDeliveryHandler` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sink`, preserve `Rbac3RuntimeProjectionDeliveryHandler`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ProjectionSink sink;
    /**
     * 字段 `objectMapper` 表示 `Rbac3RuntimeProjectionDeliveryHandler` 中与 `object Mapper` 相关的状态、依赖、配置或结果（声明类型 `ObjectMapper`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `objectMapper` stores the `object Mapper`-related state, dependency, configuration, or result of `Rbac3RuntimeProjectionDeliveryHandler` (declared type `ObjectMapper`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `objectMapper` 时应保持 `Rbac3RuntimeProjectionDeliveryHandler` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `objectMapper`, preserve `Rbac3RuntimeProjectionDeliveryHandler`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ObjectMapper objectMapper;

    /**
     * 构造器 `Rbac3RuntimeProjectionDeliveryHandler` 用于创建并初始化 `Rbac3RuntimeProjectionDeliveryHandler` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3RuntimeProjectionDeliveryHandler` creates and initializes `Rbac3RuntimeProjectionDeliveryHandler`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3RuntimeProjectionDeliveryHandler` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3RuntimeProjectionDeliveryHandler`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param sink 输入参数 `sink`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3RuntimeProjectionDeliveryHandler(ProjectionSink sink) {
        this(sink, new ObjectMapper().findAndRegisterModules());
    }

    /**
     * 构造器 `Rbac3RuntimeProjectionDeliveryHandler` 用于创建并初始化 `Rbac3RuntimeProjectionDeliveryHandler` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3RuntimeProjectionDeliveryHandler` creates and initializes `Rbac3RuntimeProjectionDeliveryHandler`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3RuntimeProjectionDeliveryHandler` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3RuntimeProjectionDeliveryHandler`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param sink 输入参数 `sink`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3RuntimeProjectionDeliveryHandler(
            ProjectionSink sink,
            ObjectMapper objectMapper) {
        this.sink = Objects.requireNonNull(sink, "sink");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    /**
     * 方法 `channel` 按照 `Rbac3RuntimeProjectionDeliveryHandler` 的职责处理输入，完成 `channel` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `channel` processes its inputs according to `Rbac3RuntimeProjectionDeliveryHandler`'s responsibility, performs the `channel` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `channel` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `channel`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public String channel() {
        return TransactionalOutboxAuthorizationEventAdapter.CHANNEL;
    }

    /**
     * 方法 `validateDestination` 按照 `Rbac3RuntimeProjectionDeliveryHandler` 的职责处理输入，完成 `validate Destination` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validateDestination` processes its inputs according to `Rbac3RuntimeProjectionDeliveryHandler`'s responsibility, performs the `validate Destination` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validateDestination` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validateDestination`, then continue the business flow using its result, exception, or side effect.
     *
     * @param destination 输入参数 `destination`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    public void validateDestination(String destination) {
        if (!DESTINATIONS.contains(destination)) {
            throw new IllegalArgumentException(
                    "unsupported RBAC3 runtime destination: " + destination);
        }
    }

    /**
     * 方法 `deliver` 按照 `Rbac3RuntimeProjectionDeliveryHandler` 的职责处理输入，完成 `deliver` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `deliver` processes its inputs according to `Rbac3RuntimeProjectionDeliveryHandler`'s responsibility, performs the `deliver` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `deliver` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `deliver`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public DeliveryResult deliver(DeliveryContext context) {
        validateDestination(context.destination());
        try {
            EventEnvelope envelope = parse(context.payload());
            if (!context.destination().equals(envelope.eventType())) {
                return DeliveryResult.permanentFailure(
                        "RBAC3_EVENT_DESTINATION_MISMATCH",
                        "event type does not match the outbox destination");
            }
            ProjectionOutcome outcome = sink.project(envelope);
            return switch (outcome) {
                case APPLIED, ALREADY_APPLIED -> DeliveryResult.success();
                case RETRYABLE_FAILURE -> DeliveryResult.retryableFailure(
                        "RBAC3_RUNTIME_PROJECTION_RETRYABLE",
                        "runtime projection has not converged");
                case PERMANENT_FAILURE -> DeliveryResult.permanentFailure(
                        "RBAC3_RUNTIME_PROJECTION_REJECTED",
                        "runtime projection rejected the event");
            };
        } catch (IllegalArgumentException invalid) {
            return DeliveryResult.permanentFailure(
                    "RBAC3_EVENT_INVALID", safeMessage(invalid));
        } catch (RuntimeException unavailable) {
            return DeliveryResult.retryableFailure(
                    "RBAC3_RUNTIME_UNAVAILABLE", "runtime projection is unavailable");
        }
    }

    /**
     * 方法 `parse` 按照 `Rbac3RuntimeProjectionDeliveryHandler` 的职责处理输入，完成 `parse` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `parse` processes its inputs according to `Rbac3RuntimeProjectionDeliveryHandler`'s responsibility, performs the `parse` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `parse` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `parse`, then continue the business flow using its result, exception, or side effect.
     *
     * @param payload 输入参数 `payload`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private EventEnvelope parse(String payload) {
        try {
            JsonNode value = objectMapper.readTree(payload);
            String eventId = required(value, "eventId");
            String eventType = required(value, "eventType");
            int schemaVersion = value.path("schemaVersion").asInt(-1);
            long aggregateVersion = value.path("aggregateVersion").asLong(-1L);
            if (schemaVersion != 1 || aggregateVersion < 0L) {
                throw new IllegalArgumentException("unsupported RBAC3 event version");
            }
            @SuppressWarnings("unchecked")
            Map<String, String> safePayload = objectMapper.convertValue(
                    value.path("payload"), Map.class);
            return new EventEnvelope(
                    eventId, eventType, schemaVersion,
                    Instant.parse(required(value, "occurredAt")),
                    required(value, "tenantId"),
                    required(value, "aggregateType"),
                    required(value, "aggregateId"),
                    aggregateVersion,
                    optional(value, "traceId"),
                    safePayload);
        } catch (IllegalArgumentException invalid) {
            throw invalid;
        } catch (Exception invalid) {
            throw new IllegalArgumentException("invalid RBAC3 event envelope", invalid);
        }
    }

    /**
     * 方法 `required` 按照 `Rbac3RuntimeProjectionDeliveryHandler` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `Rbac3RuntimeProjectionDeliveryHandler`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param source 输入参数 `source`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String required(JsonNode source, String field) {
        String value = optional(source, field);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /**
     * 方法 `optional` 按照 `Rbac3RuntimeProjectionDeliveryHandler` 的职责处理输入，完成 `optional` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `optional` processes its inputs according to `Rbac3RuntimeProjectionDeliveryHandler`'s responsibility, performs the `optional` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `optional` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `optional`, then continue the business flow using its result, exception, or side effect.
     *
     * @param source 输入参数 `source`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String optional(JsonNode source, String field) {
        JsonNode value = source.get(field);
        return value == null || value.isNull() || value.asText().isBlank()
                ? null : value.asText().trim();
    }

    /**
     * 方法 `safeMessage` 按照 `Rbac3RuntimeProjectionDeliveryHandler` 的职责处理输入，完成 `safe Message` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `safeMessage` processes its inputs according to `Rbac3RuntimeProjectionDeliveryHandler`'s responsibility, performs the `safe Message` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `safeMessage` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `safeMessage`, then continue the business flow using its result, exception, or side effect.
     *
     * @param invalid 输入参数 `invalid`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String safeMessage(IllegalArgumentException invalid) {
        String message = invalid.getMessage();
        return message == null || message.isBlank()
                ? "invalid RBAC3 event envelope"
                : message.substring(0, Math.min(256, message.length()));
    }

    /**
     * 类型 `ProjectionSink` 位于 `Rbac3RuntimeProjectionDeliveryHandler` 内，是接口，用于承载 `Projection Sink` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ProjectionSink` is an interface inside `Rbac3RuntimeProjectionDeliveryHandler` and carries the responsibility, state, or contract for `Projection Sink`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ProjectionSink` 作为 `Rbac3RuntimeProjectionDeliveryHandler` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ProjectionSink` as the responsibility boundary of `Rbac3RuntimeProjectionDeliveryHandler`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface ProjectionSink {

        /**
         * 方法 `project` 按照 `ProjectionSink` 的职责处理输入，完成 `project` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `project` processes its inputs according to `ProjectionSink`'s responsibility, performs the `project` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `project` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `project`, then continue the business flow using its result, exception, or side effect.
         *
         * @param envelope 输入参数 `envelope`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        ProjectionOutcome project(EventEnvelope envelope);
    }

    /**
     * 类型 `ProjectionOutcome` 位于 `Rbac3RuntimeProjectionDeliveryHandler` 内，是枚举，用于承载 `Projection Outcome` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `ProjectionOutcome` is an enum inside `Rbac3RuntimeProjectionDeliveryHandler` and carries the responsibility, state, or contract for `Projection Outcome`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `ProjectionOutcome` 作为 `Rbac3RuntimeProjectionDeliveryHandler` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `ProjectionOutcome` as the responsibility boundary of `Rbac3RuntimeProjectionDeliveryHandler`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public enum ProjectionOutcome {
        /**
         * 字段 `APPLIED` 表示 `ProjectionOutcome` 中与 `APPLIED` 相关的状态、依赖、配置或结果（声明类型 `ProjectionOutcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `APPLIED` stores the `APPLIED`-related state, dependency, configuration, or result of `ProjectionOutcome` (declared type `ProjectionOutcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `APPLIED` 时应保持 `ProjectionOutcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `APPLIED`, preserve `ProjectionOutcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        APPLIED,
        /**
         * 字段 `ALREADY_APPLIED` 表示 `ProjectionOutcome` 中与 `ALREADY APPLIED` 相关的状态、依赖、配置或结果（声明类型 `ProjectionOutcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `ALREADY_APPLIED` stores the `ALREADY APPLIED`-related state, dependency, configuration, or result of `ProjectionOutcome` (declared type `ProjectionOutcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `ALREADY_APPLIED` 时应保持 `ProjectionOutcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `ALREADY_APPLIED`, preserve `ProjectionOutcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        ALREADY_APPLIED,
        /**
         * 字段 `RETRYABLE_FAILURE` 表示 `ProjectionOutcome` 中与 `RETRYABLE FAILURE` 相关的状态、依赖、配置或结果（声明类型 `ProjectionOutcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `RETRYABLE_FAILURE` stores the `RETRYABLE FAILURE`-related state, dependency, configuration, or result of `ProjectionOutcome` (declared type `ProjectionOutcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `RETRYABLE_FAILURE` 时应保持 `ProjectionOutcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `RETRYABLE_FAILURE`, preserve `ProjectionOutcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        RETRYABLE_FAILURE,
        /**
         * 字段 `PERMANENT_FAILURE` 表示 `ProjectionOutcome` 中与 `PERMANENT FAILURE` 相关的状态、依赖、配置或结果（声明类型 `ProjectionOutcome`）；其生命周期和取值含义由声明类型及所属对象共同确定。
         * Field `PERMANENT_FAILURE` stores the `PERMANENT FAILURE`-related state, dependency, configuration, or result of `ProjectionOutcome` (declared type `ProjectionOutcome`); its lifecycle and value semantics are defined by its declared type and owning object.
         *
         * 含义与用法：读取、传递或更新 `PERMANENT_FAILURE` 时应保持 `ProjectionOutcome` 的生命周期、不可变性和线程安全约束。
         * Meaning and usage: when reading, passing, or updating `PERMANENT_FAILURE`, preserve `ProjectionOutcome`'s lifecycle, immutability, and thread-safety constraints.
         */
        PERMANENT_FAILURE
    }

    /**
     * 类型 `EventEnvelope` 位于 `Rbac3RuntimeProjectionDeliveryHandler` 内，是记录类型，用于承载 `Event Envelope` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `EventEnvelope` is a record inside `Rbac3RuntimeProjectionDeliveryHandler` and carries the responsibility, state, or contract for `Event Envelope`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `EventEnvelope` 作为 `Rbac3RuntimeProjectionDeliveryHandler` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `EventEnvelope` as the responsibility boundary of `Rbac3RuntimeProjectionDeliveryHandler`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param eventId 记录组件 `eventId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `eventId` carries constructor data whose meaning is defined by the record contract.
     * @param eventType 记录组件 `eventType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `eventType` carries constructor data whose meaning is defined by the record contract.
     * @param schemaVersion 记录组件 `schemaVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `schemaVersion` carries constructor data whose meaning is defined by the record contract.
     * @param occurredAt 记录组件 `occurredAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `occurredAt` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param aggregateType 记录组件 `aggregateType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `aggregateType` carries constructor data whose meaning is defined by the record contract.
     * @param aggregateId 记录组件 `aggregateId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `aggregateId` carries constructor data whose meaning is defined by the record contract.
     * @param aggregateVersion 记录组件 `aggregateVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `aggregateVersion` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     * @param payload 记录组件 `payload` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `payload` carries constructor data whose meaning is defined by the record contract.
     */
    public record EventEnvelope(
            /**
             * 字段 `eventId` 表示 `EventEnvelope` 中与 `event Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `eventId` stores the `event Id`-related state, dependency, configuration, or result of `EventEnvelope` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `eventId` 时应保持 `EventEnvelope` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `eventId`, preserve `EventEnvelope`'s lifecycle, immutability, and thread-safety constraints.
             */
            String eventId,
            /**
             * 字段 `eventType` 表示 `EventEnvelope` 中与 `event Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `eventType` stores the `event Type`-related state, dependency, configuration, or result of `EventEnvelope` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `eventType` 时应保持 `EventEnvelope` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `eventType`, preserve `EventEnvelope`'s lifecycle, immutability, and thread-safety constraints.
             */
            String eventType,
            /**
             * 字段 `schemaVersion` 表示 `EventEnvelope` 中与 `schema Version` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `schemaVersion` stores the `schema Version`-related state, dependency, configuration, or result of `EventEnvelope` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `schemaVersion` 时应保持 `EventEnvelope` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `schemaVersion`, preserve `EventEnvelope`'s lifecycle, immutability, and thread-safety constraints.
             */
            int schemaVersion,
            /**
             * 字段 `occurredAt` 表示 `EventEnvelope` 中与 `occurred At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `occurredAt` stores the `occurred At`-related state, dependency, configuration, or result of `EventEnvelope` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `occurredAt` 时应保持 `EventEnvelope` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `occurredAt`, preserve `EventEnvelope`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant occurredAt,
            /**
             * 字段 `tenantId` 表示 `EventEnvelope` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `EventEnvelope` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `EventEnvelope` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `EventEnvelope`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `aggregateType` 表示 `EventEnvelope` 中与 `aggregate Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `aggregateType` stores the `aggregate Type`-related state, dependency, configuration, or result of `EventEnvelope` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `aggregateType` 时应保持 `EventEnvelope` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `aggregateType`, preserve `EventEnvelope`'s lifecycle, immutability, and thread-safety constraints.
             */
            String aggregateType,
            /**
             * 字段 `aggregateId` 表示 `EventEnvelope` 中与 `aggregate Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `aggregateId` stores the `aggregate Id`-related state, dependency, configuration, or result of `EventEnvelope` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `aggregateId` 时应保持 `EventEnvelope` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `aggregateId`, preserve `EventEnvelope`'s lifecycle, immutability, and thread-safety constraints.
             */
            String aggregateId,
            /**
             * 字段 `aggregateVersion` 表示 `EventEnvelope` 中与 `aggregate Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `aggregateVersion` stores the `aggregate Version`-related state, dependency, configuration, or result of `EventEnvelope` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `aggregateVersion` 时应保持 `EventEnvelope` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `aggregateVersion`, preserve `EventEnvelope`'s lifecycle, immutability, and thread-safety constraints.
             */
            long aggregateVersion,
            /**
             * 字段 `traceId` 表示 `EventEnvelope` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `EventEnvelope` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `EventEnvelope` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `EventEnvelope`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId,
            /**
             * 字段 `payload` 表示 `EventEnvelope` 中与 `payload` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `payload` stores the `payload`-related state, dependency, configuration, or result of `EventEnvelope` (declared type `Map&lt;String, String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `payload` 时应保持 `EventEnvelope` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `payload`, preserve `EventEnvelope`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, String> payload) {

        /**
         * 构造器 `EventEnvelope` 用于创建并初始化 `EventEnvelope` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `EventEnvelope` creates and initializes `EventEnvelope`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `EventEnvelope` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `EventEnvelope`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param eventId 输入参数 `eventId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param eventType 输入参数 `eventType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param schemaVersion 输入参数 `schemaVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param occurredAt 输入参数 `occurredAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param aggregateType 输入参数 `aggregateType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param aggregateId 输入参数 `aggregateId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param aggregateVersion 输入参数 `aggregateVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param payload 输入参数 `payload`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public EventEnvelope {
            payload = Map.copyOf(payload);
        }
    }
}
