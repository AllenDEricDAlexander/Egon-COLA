package top.egon.cola.platform.rbac3.admin.audit.application;

import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaField;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 类型 `AuditQueryService` 位于当前包内，是类型，用于承载 `Audit Query Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuditQueryService` is a type in its package and carries the responsibility, state, or contract for `Audit Query Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Tenant-scoped audit write/query facade with mandatory redaction and read audit.
 */
public final class AuditQueryService {

    /**
     * 字段 `MAX_QUERY_WINDOW` 表示 `AuditQueryService` 中与 `MAX QUERY WINDOW` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `MAX_QUERY_WINDOW` stores the `MAX QUERY WINDOW`-related state, dependency, configuration, or result of `AuditQueryService` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `MAX_QUERY_WINDOW` 时应保持 `AuditQueryService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `MAX_QUERY_WINDOW`, preserve `AuditQueryService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final Duration MAX_QUERY_WINDOW = Duration.ofDays(31);
    /**
     * 字段 `REDACTED` 表示 `AuditQueryService` 中与 `REDACTED` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `REDACTED` stores the `REDACTED`-related state, dependency, configuration, or result of `AuditQueryService` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `REDACTED` 时应保持 `AuditQueryService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `REDACTED`, preserve `AuditQueryService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String REDACTED = "<redacted>";
    /**
     * 字段 `SECRET_KEY_PARTS` 表示 `AuditQueryService` 中与 `SECRET KEY PARTS` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `SECRET_KEY_PARTS` stores the `SECRET KEY PARTS`-related state, dependency, configuration, or result of `AuditQueryService` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `SECRET_KEY_PARTS` 时应保持 `AuditQueryService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `SECRET_KEY_PARTS`, preserve `AuditQueryService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final List<String> SECRET_KEY_PARTS = List.of(
            "password", "passwd", "secret", "token", "authorization",
            "credential", "privatekey", "private_key", "refresh");

    /**
     * 字段 `store` 表示 `AuditQueryService` 中与 `store` 相关的状态、依赖、配置或结果（声明类型 `AuditStore`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `store` stores the `store`-related state, dependency, configuration, or result of `AuditQueryService` (declared type `AuditStore`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `store` 时应保持 `AuditQueryService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `store`, preserve `AuditQueryService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuditStore store;
    /**
     * 字段 `clock` 表示 `AuditQueryService` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `AuditQueryService` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `AuditQueryService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `AuditQueryService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /**
     * 构造器 `AuditQueryService` 用于创建并初始化 `AuditQueryService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `AuditQueryService` creates and initializes `AuditQueryService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `AuditQueryService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `AuditQueryService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param store 输入参数 `store`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public AuditQueryService(AuditStore store, Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 方法 `record` 按照 `AuditQueryService` 的职责处理输入，完成 `record` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `record` processes its inputs according to `AuditQueryService`'s responsibility, performs the `record` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `record` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `record`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public AuditView record(AuditCommand command) {
        Objects.requireNonNull(command, "command");
        Map<String, Object> before = sanitize(command.beforeSnapshot());
        Map<String, Object> after = sanitize(command.afterSnapshot());
        String checksum = checksum(command, before, after);
        AuditView view = new AuditView(
                null, command.tenantId(), command.eventType(),
                command.outcome(), command.severity(), command.actorType(), command.actorId(),
                command.targetType(), command.targetId(), command.managementPolicyId(),
                command.reasonCode(), command.requestId(), command.traceId(),
                before, after, checksum, command.occurredAt());
        return store.append(view);
    }

    /**
     * 方法 `query` 按照 `AuditQueryService` 的职责处理输入，完成 `query` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `query` processes its inputs according to `AuditQueryService`'s responsibility, performs the `query` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `query` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `query`, then continue the business flow using its result, exception, or side effect.
     *
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param readerId 输入参数 `readerId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param requestId 输入参数 `requestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public Page query(
            Query query,
            String readerId,
            String requestId,
            String traceId) {
        validate(query);
        Page page = store.query(query);
        record(new AuditCommand(
                query.tenantId(), "AUDIT_LOGS_READ", "SUCCESS", "INFO", "USER",
                required(readerId, "readerId"), "AUDIT_QUERY", null, null, "ALLOW",
                required(requestId, "requestId"), required(traceId, "traceId"),
                Map.of(), Map.of(
                        "returned", page.items().size(),
                        "from", query.from().toString(),
                        "to", query.to().toString()), clock.instant()));
        return page;
    }

    /**
     * 方法 `validate` 按照 `AuditQueryService` 的职责处理输入，完成 `validate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validate` processes its inputs according to `AuditQueryService`'s responsibility, performs the `validate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    private void validate(Query query) {
        Objects.requireNonNull(query, "query");
        if (query.to().isBefore(query.from())) {
            throw new IllegalArgumentException("audit query end must not precede start");
        }
        if (Duration.between(query.from(), query.to()).compareTo(MAX_QUERY_WINDOW) > 0) {
            throw new IllegalArgumentException("audit query window must not exceed 31 days");
        }
        if (query.pageSize() < 1 || query.pageSize() > 200) {
            throw new IllegalArgumentException("pageSize must be between 1 and 200");
        }
    }

    /**
     * 方法 `sanitize` 按照 `AuditQueryService` 的职责处理输入，完成 `sanitize` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `sanitize` processes its inputs according to `AuditQueryService`'s responsibility, performs the `sanitize` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `sanitize` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `sanitize`, then continue the business flow using its result, exception, or side effect.
     *
     * @param source 输入参数 `source`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Map<String, Object> sanitize(Map<String, ?> source) {
        var result = new TreeMap<String, Object>();
        Objects.requireNonNull(source, "snapshot").forEach((key, value) -> {
            String normalized = key.toLowerCase(Locale.ROOT).replace("-", "");
            boolean secret = SECRET_KEY_PARTS.stream().anyMatch(normalized::contains);
            result.put(key, secret ? REDACTED : sanitizeValue(value));
        });
        return Map.copyOf(result);
    }

    /**
     * 方法 `sanitizeValue` 按照 `AuditQueryService` 的职责处理输入，完成 `sanitize Value` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `sanitizeValue` processes its inputs according to `AuditQueryService`'s responsibility, performs the `sanitize Value` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `sanitizeValue` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `sanitizeValue`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Object sanitizeValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            var normalized = new LinkedHashMap<String, Object>();
            map.forEach((key, nested) -> normalized.put(String.valueOf(key), nested));
            return sanitize(normalized);
        }
        if (value instanceof Iterable<?> values) {
            var sanitized = new ArrayList<>();
            values.forEach(valueItem -> sanitized.add(sanitizeValue(valueItem)));
            return List.copyOf(sanitized);
        }
        return value;
    }

    /**
     * 方法 `checksum` 按照 `AuditQueryService` 的职责处理输入，完成 `checksum` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `checksum` processes its inputs according to `AuditQueryService`'s responsibility, performs the `checksum` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `checksum` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `checksum`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param before 输入参数 `before`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param after 输入参数 `after`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String checksum(
            AuditCommand command,
            Map<String, Object> before,
            Map<String, Object> after) {
        String canonical = String.join("\u001f",
                command.tenantId(), command.eventType(), command.outcome(),
                command.actorType(), command.actorId(), command.requestId(),
                command.traceId(), before.toString(), after.toString(),
                command.occurredAt().toString());
        try {
            return "sha256:" + HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    /**
     * 方法 `required` 按照 `AuditQueryService` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `AuditQueryService`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param fieldName 输入参数 `fieldName`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 类型 `AuditStore` 位于 `AuditQueryService` 内，是接口，用于承载 `Audit Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuditStore` is an interface inside `AuditQueryService` and carries the responsibility, state, or contract for `Audit Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuditStore` 作为 `AuditQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuditStore` as the responsibility boundary of `AuditQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public interface AuditStore {
        /**
         * 方法 `append` 按照 `AuditStore` 的职责处理输入，完成 `append` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `append` processes its inputs according to `AuditStore`'s responsibility, performs the `append` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `append` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `append`, then continue the business flow using its result, exception, or side effect.
         *
         * @param record 输入参数 `record`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        AuditView append(AuditView record);

        /**
         * 方法 `query` 按照 `AuditStore` 的职责处理输入，完成 `query` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `query` processes its inputs according to `AuditStore`'s responsibility, performs the `query` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `query` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `query`, then continue the business flow using its result, exception, or side effect.
         *
         * @param query 输入参数 `query`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Page query(Query query);
    }

    /**
     * 类型 `AuditCommand` 位于 `AuditQueryService` 内，是记录类型，用于承载 `Audit Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuditCommand` is a record inside `AuditQueryService` and carries the responsibility, state, or contract for `Audit Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuditCommand` 作为 `AuditQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuditCommand` as the responsibility boundary of `AuditQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param eventType 记录组件 `eventType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `eventType` carries constructor data whose meaning is defined by the record contract.
     * @param outcome 记录组件 `outcome` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `outcome` carries constructor data whose meaning is defined by the record contract.
     * @param severity 记录组件 `severity` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `severity` carries constructor data whose meaning is defined by the record contract.
     * @param actorType 记录组件 `actorType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorType` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     * @param targetType 记录组件 `targetType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetType` carries constructor data whose meaning is defined by the record contract.
     * @param targetId 记录组件 `targetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetId` carries constructor data whose meaning is defined by the record contract.
     * @param managementPolicyId 记录组件 `managementPolicyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `managementPolicyId` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     * @param requestId 记录组件 `requestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requestId` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     * @param beforeSnapshot 记录组件 `beforeSnapshot` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `beforeSnapshot` carries constructor data whose meaning is defined by the record contract.
     * @param afterSnapshot 记录组件 `afterSnapshot` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `afterSnapshot` carries constructor data whose meaning is defined by the record contract.
     * @param occurredAt 记录组件 `occurredAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `occurredAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record AuditCommand(
            /**
             * 字段 `tenantId` 表示 `AuditCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `AuditCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `eventType` 表示 `AuditCommand` 中与 `event Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `eventType` stores the `event Type`-related state, dependency, configuration, or result of `AuditCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `eventType` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `eventType`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String eventType,
            /**
             * 字段 `outcome` 表示 `AuditCommand` 中与 `outcome` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `outcome` stores the `outcome`-related state, dependency, configuration, or result of `AuditCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `outcome` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `outcome`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String outcome,
            /**
             * 字段 `severity` 表示 `AuditCommand` 中与 `severity` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `severity` stores the `severity`-related state, dependency, configuration, or result of `AuditCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `severity` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `severity`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String severity,
            /**
             * 字段 `actorType` 表示 `AuditCommand` 中与 `actor Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorType` stores the `actor Type`-related state, dependency, configuration, or result of `AuditCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorType` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorType`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorType,
            /**
             * 字段 `actorId` 表示 `AuditCommand` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `AuditCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `targetType` 表示 `AuditCommand` 中与 `target Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetType` stores the `target Type`-related state, dependency, configuration, or result of `AuditCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetType` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetType`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetType,
            /**
             * 字段 `targetId` 表示 `AuditCommand` 中与 `target Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetId` stores the `target Id`-related state, dependency, configuration, or result of `AuditCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetId` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetId`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetId,
            /**
             * 字段 `managementPolicyId` 表示 `AuditCommand` 中与 `management Policy Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `managementPolicyId` stores the `management Policy Id`-related state, dependency, configuration, or result of `AuditCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `managementPolicyId` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `managementPolicyId`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String managementPolicyId,
            /**
             * 字段 `reasonCode` 表示 `AuditCommand` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `AuditCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode,
            /**
             * 字段 `requestId` 表示 `AuditCommand` 中与 `request Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestId` stores the `request Id`-related state, dependency, configuration, or result of `AuditCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestId` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestId`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requestId,
            /**
             * 字段 `traceId` 表示 `AuditCommand` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `AuditCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId,
            /**
             * 字段 `beforeSnapshot` 表示 `AuditCommand` 中与 `before Snapshot` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, ?&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `beforeSnapshot` stores the `before Snapshot`-related state, dependency, configuration, or result of `AuditCommand` (declared type `Map&lt;String, ?&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `beforeSnapshot` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `beforeSnapshot`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, ?> beforeSnapshot,
            /**
             * 字段 `afterSnapshot` 表示 `AuditCommand` 中与 `after Snapshot` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, ?&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `afterSnapshot` stores the `after Snapshot`-related state, dependency, configuration, or result of `AuditCommand` (declared type `Map&lt;String, ?&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `afterSnapshot` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `afterSnapshot`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Map<String, ?> afterSnapshot,
            /**
             * 字段 `occurredAt` 表示 `AuditCommand` 中与 `occurred At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `occurredAt` stores the `occurred At`-related state, dependency, configuration, or result of `AuditCommand` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `occurredAt` 时应保持 `AuditCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `occurredAt`, preserve `AuditCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant occurredAt) {
        /**
         * 构造器 `AuditCommand` 用于创建并初始化 `AuditCommand` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `AuditCommand` creates and initializes `AuditCommand`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `AuditCommand` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `AuditCommand`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param eventType 输入参数 `eventType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param outcome 输入参数 `outcome`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param severity 输入参数 `severity`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorType 输入参数 `actorType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param targetType 输入参数 `targetType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param targetId 输入参数 `targetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param managementPolicyId 输入参数 `managementPolicyId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requestId 输入参数 `requestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param beforeSnapshot 输入参数 `beforeSnapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param afterSnapshot 输入参数 `afterSnapshot`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param occurredAt 输入参数 `occurredAt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public AuditCommand {
            tenantId = required(tenantId, "tenantId");
            eventType = required(eventType, "eventType");
            outcome = required(outcome, "outcome");
            severity = required(severity, "severity");
            actorType = required(actorType, "actorType");
            actorId = required(actorId, "actorId");
            requestId = required(requestId, "requestId");
            traceId = required(traceId, "traceId");
            beforeSnapshot = Map.copyOf(Objects.requireNonNull(
                    beforeSnapshot, "beforeSnapshot"));
            afterSnapshot = Map.copyOf(Objects.requireNonNull(
                    afterSnapshot, "afterSnapshot"));
            occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        }
    }

    /**
     * 类型 `Query` 位于 `AuditQueryService` 内，是记录类型，用于承载 `Query` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Query` is a record inside `AuditQueryService` and carries the responsibility, state, or contract for `Query`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Query` 作为 `AuditQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Query` as the responsibility boundary of `AuditQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param from 记录组件 `from` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `from` carries constructor data whose meaning is defined by the record contract.
     * @param to 记录组件 `to` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `to` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     * @param targetId 记录组件 `targetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetId` carries constructor data whose meaning is defined by the record contract.
     * @param eventType 记录组件 `eventType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `eventType` carries constructor data whose meaning is defined by the record contract.
     * @param outcome 记录组件 `outcome` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `outcome` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     * @param requestId 记录组件 `requestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requestId` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     * @param targetType 记录组件 `targetType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetType` carries constructor data whose meaning is defined by the record contract.
     * @param pageSize 记录组件 `pageSize` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `pageSize` carries constructor data whose meaning is defined by the record contract.
     * @param cursor 记录组件 `cursor` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `cursor` carries constructor data whose meaning is defined by the record contract.
     */
    public record Query(
            /**
             * 字段 `tenantId` 表示 `Query` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `Query` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `Query` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `Query`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `from` 表示 `Query` 中与 `from` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `from` stores the `from`-related state, dependency, configuration, or result of `Query` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `from` 时应保持 `Query` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `from`, preserve `Query`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant from,
            /**
             * 字段 `to` 表示 `Query` 中与 `to` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `to` stores the `to`-related state, dependency, configuration, or result of `Query` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `to` 时应保持 `Query` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `to`, preserve `Query`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant to,
            /**
             * 字段 `actorId` 表示 `Query` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `Query` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `Query` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `Query`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `targetId` 表示 `Query` 中与 `target Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetId` stores the `target Id`-related state, dependency, configuration, or result of `Query` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetId` 时应保持 `Query` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetId`, preserve `Query`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetId,
            /**
             * 字段 `eventType` 表示 `Query` 中与 `event Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `eventType` stores the `event Type`-related state, dependency, configuration, or result of `Query` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `eventType` 时应保持 `Query` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `eventType`, preserve `Query`'s lifecycle, immutability, and thread-safety constraints.
             */
            String eventType,
            /**
             * 字段 `outcome` 表示 `Query` 中与 `outcome` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `outcome` stores the `outcome`-related state, dependency, configuration, or result of `Query` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `outcome` 时应保持 `Query` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `outcome`, preserve `Query`'s lifecycle, immutability, and thread-safety constraints.
             */
            String outcome,
            /**
             * 字段 `reasonCode` 表示 `Query` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `Query` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `Query` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `Query`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode,
            /**
             * 字段 `requestId` 表示 `Query` 中与 `request Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestId` stores the `request Id`-related state, dependency, configuration, or result of `Query` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestId` 时应保持 `Query` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestId`, preserve `Query`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requestId,
            /**
             * 字段 `traceId` 表示 `Query` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `Query` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `Query` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `Query`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId,
            /**
             * 字段 `targetType` 表示 `Query` 中与 `target Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetType` stores the `target Type`-related state, dependency, configuration, or result of `Query` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetType` 时应保持 `Query` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetType`, preserve `Query`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetType,
            /**
             * 字段 `pageSize` 表示 `Query` 中与 `page Size` 相关的状态、依赖、配置或结果（声明类型 `int`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `pageSize` stores the `page Size`-related state, dependency, configuration, or result of `Query` (declared type `int`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `pageSize` 时应保持 `Query` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `pageSize`, preserve `Query`'s lifecycle, immutability, and thread-safety constraints.
             */
            int pageSize,
            /**
             * 字段 `cursor` 表示 `Query` 中与 `cursor` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `cursor` stores the `cursor`-related state, dependency, configuration, or result of `Query` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `cursor` 时应保持 `Query` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `cursor`, preserve `Query`'s lifecycle, immutability, and thread-safety constraints.
             */
            String cursor) {
        /**
         * 构造器 `Query` 用于创建并初始化 `Query` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `Query` creates and initializes `Query`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `Query` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `Query`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param from 输入参数 `from`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param to 输入参数 `to`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param actorId 输入参数 `actorId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param targetId 输入参数 `targetId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param eventType 输入参数 `eventType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param outcome 输入参数 `outcome`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param reasonCode 输入参数 `reasonCode`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param requestId 输入参数 `requestId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param traceId 输入参数 `traceId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param targetType 输入参数 `targetType`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param pageSize 输入参数 `pageSize`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param cursor 输入参数 `cursor`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public Query {
            tenantId = required(tenantId, "tenantId");
            from = Objects.requireNonNull(from, "from");
            to = Objects.requireNonNull(to, "to");
        }
    }

    /**
     * 类型 `AuditView` 位于 `AuditQueryService` 内，是记录类型，用于承载 `Audit View` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AuditView` is a record inside `AuditQueryService` and carries the responsibility, state, or contract for `Audit View`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AuditView` 作为 `AuditQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AuditView` as the responsibility boundary of `AuditQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param id 记录组件 `id` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `id` carries constructor data whose meaning is defined by the record contract.
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param eventType 记录组件 `eventType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `eventType` carries constructor data whose meaning is defined by the record contract.
     * @param outcome 记录组件 `outcome` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `outcome` carries constructor data whose meaning is defined by the record contract.
     * @param severity 记录组件 `severity` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `severity` carries constructor data whose meaning is defined by the record contract.
     * @param actorType 记录组件 `actorType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorType` carries constructor data whose meaning is defined by the record contract.
     * @param actorId 记录组件 `actorId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `actorId` carries constructor data whose meaning is defined by the record contract.
     * @param targetType 记录组件 `targetType` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetType` carries constructor data whose meaning is defined by the record contract.
     * @param targetId 记录组件 `targetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `targetId` carries constructor data whose meaning is defined by the record contract.
     * @param managementPolicyId 记录组件 `managementPolicyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `managementPolicyId` carries constructor data whose meaning is defined by the record contract.
     * @param reasonCode 记录组件 `reasonCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `reasonCode` carries constructor data whose meaning is defined by the record contract.
     * @param requestId 记录组件 `requestId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `requestId` carries constructor data whose meaning is defined by the record contract.
     * @param traceId 记录组件 `traceId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `traceId` carries constructor data whose meaning is defined by the record contract.
     * @param beforeSnapshot 记录组件 `beforeSnapshot` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `beforeSnapshot` carries constructor data whose meaning is defined by the record contract.
     * @param afterSnapshot 记录组件 `afterSnapshot` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `afterSnapshot` carries constructor data whose meaning is defined by the record contract.
     * @param payloadChecksum 记录组件 `payloadChecksum` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `payloadChecksum` carries constructor data whose meaning is defined by the record contract.
     * @param createdAt 记录组件 `createdAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `createdAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record AuditView(
            /**
             * 字段 `id` 表示 `AuditView` 中与 `id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `id` stores the `id`-related state, dependency, configuration, or result of `AuditView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `id` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `id`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String id,
            /**
             * 字段 `tenantId` 表示 `AuditView` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `AuditView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `eventType` 表示 `AuditView` 中与 `event Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `eventType` stores the `event Type`-related state, dependency, configuration, or result of `AuditView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `eventType` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `eventType`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String eventType,
            /**
             * 字段 `outcome` 表示 `AuditView` 中与 `outcome` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `outcome` stores the `outcome`-related state, dependency, configuration, or result of `AuditView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `outcome` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `outcome`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String outcome,
            /**
             * 字段 `severity` 表示 `AuditView` 中与 `severity` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `severity` stores the `severity`-related state, dependency, configuration, or result of `AuditView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `severity` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `severity`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String severity,
            /**
             * 字段 `actorType` 表示 `AuditView` 中与 `actor Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorType` stores the `actor Type`-related state, dependency, configuration, or result of `AuditView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorType` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorType`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorType,
            /**
             * 字段 `actorId` 表示 `AuditView` 中与 `actor Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `actorId` stores the `actor Id`-related state, dependency, configuration, or result of `AuditView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `actorId` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `actorId`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String actorId,
            /**
             * 字段 `targetType` 表示 `AuditView` 中与 `target Type` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetType` stores the `target Type`-related state, dependency, configuration, or result of `AuditView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetType` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetType`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetType,
            /**
             * 字段 `targetId` 表示 `AuditView` 中与 `target Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `targetId` stores the `target Id`-related state, dependency, configuration, or result of `AuditView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `targetId` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `targetId`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String targetId,
            /**
             * 字段 `managementPolicyId` 表示 `AuditView` 中与 `management Policy Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `managementPolicyId` stores the `management Policy Id`-related state, dependency, configuration, or result of `AuditView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `managementPolicyId` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `managementPolicyId`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String managementPolicyId,
            /**
             * 字段 `reasonCode` 表示 `AuditView` 中与 `reason Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `reasonCode` stores the `reason Code`-related state, dependency, configuration, or result of `AuditView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `reasonCode` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `reasonCode`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String reasonCode,
            /**
             * 字段 `requestId` 表示 `AuditView` 中与 `request Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `requestId` stores the `request Id`-related state, dependency, configuration, or result of `AuditView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `requestId` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `requestId`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String requestId,
            /**
             * 字段 `traceId` 表示 `AuditView` 中与 `trace Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `traceId` stores the `trace Id`-related state, dependency, configuration, or result of `AuditView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `traceId` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `traceId`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String traceId,
            /**
             * 字段 `beforeSnapshot` 表示 `AuditView` 中与 `before Snapshot` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `beforeSnapshot` stores the `before Snapshot`-related state, dependency, configuration, or result of `AuditView` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `beforeSnapshot` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `beforeSnapshot`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            @GatewaySchemaField(allowArbitraryJson = true)
            Map<String, Object> beforeSnapshot,
            /**
             * 字段 `afterSnapshot` 表示 `AuditView` 中与 `after Snapshot` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;String, Object&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `afterSnapshot` stores the `after Snapshot`-related state, dependency, configuration, or result of `AuditView` (declared type `Map&lt;String, Object&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `afterSnapshot` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `afterSnapshot`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            @GatewaySchemaField(allowArbitraryJson = true)
            Map<String, Object> afterSnapshot,
            /**
             * 字段 `payloadChecksum` 表示 `AuditView` 中与 `payload Checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `payloadChecksum` stores the `payload Checksum`-related state, dependency, configuration, or result of `AuditView` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `payloadChecksum` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `payloadChecksum`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            String payloadChecksum,
            /**
             * 字段 `createdAt` 表示 `AuditView` 中与 `created At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `createdAt` stores the `created At`-related state, dependency, configuration, or result of `AuditView` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `createdAt` 时应保持 `AuditView` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `createdAt`, preserve `AuditView`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant createdAt) {
    }

    /**
     * 类型 `Page` 位于 `AuditQueryService` 内，是记录类型，用于承载 `Page` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Page` is a record inside `AuditQueryService` and carries the responsibility, state, or contract for `Page`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Page` 作为 `AuditQueryService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Page` as the responsibility boundary of `AuditQueryService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param items 记录组件 `items` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `items` carries constructor data whose meaning is defined by the record contract.
     * @param nextCursor 记录组件 `nextCursor` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `nextCursor` carries constructor data whose meaning is defined by the record contract.
     */
    public record Page(/**
 * 字段 `items` 表示 `Page` 中与 `items` 相关的状态、依赖、配置或结果（声明类型 `List&lt;AuditView&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `items` stores the `items`-related state, dependency, configuration, or result of `Page` (declared type `List&lt;AuditView&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `items` 时应保持 `Page` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `items`, preserve `Page`'s lifecycle, immutability, and thread-safety constraints.
 */ List<AuditView> items, /**
 * 字段 `nextCursor` 表示 `Page` 中与 `next Cursor` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `nextCursor` stores the `next Cursor`-related state, dependency, configuration, or result of `Page` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `nextCursor` 时应保持 `Page` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `nextCursor`, preserve `Page`'s lifecycle, immutability, and thread-safety constraints.
 */ String nextCursor) {
        /**
         * 构造器 `Page` 用于创建并初始化 `Page` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `Page` creates and initializes `Page`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `Page` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `Page`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param items 输入参数 `items`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param nextCursor 输入参数 `nextCursor`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public Page {
            items = List.copyOf(items);
        }
    }
}
