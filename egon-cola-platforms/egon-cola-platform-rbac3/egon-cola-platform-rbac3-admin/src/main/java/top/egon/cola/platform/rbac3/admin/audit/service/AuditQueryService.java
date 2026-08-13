package top.egon.cola.platform.rbac3.admin.audit.service;

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
import top.egon.cola.platform.rbac3.admin.audit.repository.AuditRepository;
import top.egon.cola.platform.rbac3.admin.audit.repository.AuditPort;
import top.egon.cola.platform.rbac3.admin.audit.domain.dto.AuditCommandDTO;
import top.egon.cola.platform.rbac3.admin.audit.domain.dto.QueryDTO;
import top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditVO;
import top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditQueryPageVO;
import top.egon.cola.platform.rbac3.admin.audit.domain.vo.AuditEventVO;

/**
 * 类型 `AuditQueryService` 位于当前包内，是类型，用于承载 `Audit QueryDTO Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `AuditQueryService` is a type in its package and carries the responsibility, state, or contract for `Audit QueryDTO Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Tenant-scoped audit write/query facade with mandatory redaction and read audit.
 */
public final class AuditQueryService implements AuditPort {

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
     * 字段 `store` 表示 `AuditQueryService` 中与 `store` 相关的状态、依赖、配置或结果（声明类型 `AuditRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `store` stores the `store`-related state, dependency, configuration, or result of `AuditQueryService` (declared type `AuditRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `store` 时应保持 `AuditQueryService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `store`, preserve `AuditQueryService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final AuditRepository store;
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
    public AuditQueryService(AuditRepository store, Clock clock) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 将通用审计事件转换为脱敏后的审计记录。
     * Converts a generic audit event into a sanitized audit record.
     *
     * @param event 审计事件；audit event
     */
    @Override
    public void append(AuditEventVO event) {
        record(new AuditCommandDTO(
                event.tenantId(), event.eventType(), event.outcome(),
                event.severity(), "USER", event.actorId(), event.targetType(),
                event.targetId(), null, event.reasonCode(), event.requestId(),
                event.traceId(), Map.of(), event.safeEvidence(),
                event.occurredAt()));
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
    public AuditVO record(AuditCommandDTO command) {
        Objects.requireNonNull(command, "command");
        Map<String, Object> before = sanitize(command.beforeSnapshot());
        Map<String, Object> after = sanitize(command.afterSnapshot());
        String checksum = checksum(command, before, after);
        AuditVO view = new AuditVO(
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
    public AuditQueryPageVO query(
            QueryDTO query,
            String readerId,
            String requestId,
            String traceId) {
        validate(query);
        AuditQueryPageVO page = store.query(query);
        record(new AuditCommandDTO(
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
    private void validate(QueryDTO query) {
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
            AuditCommandDTO command,
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





    }
