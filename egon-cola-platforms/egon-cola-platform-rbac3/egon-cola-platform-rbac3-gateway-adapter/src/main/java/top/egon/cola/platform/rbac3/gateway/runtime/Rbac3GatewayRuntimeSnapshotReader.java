package top.egon.cola.platform.rbac3.gateway.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.component.gateway.contract.protocol.AccessZone;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.AuthorizationDecision;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;
import top.egon.cola.platform.rbac3.contract.authorization.AppAuthorizationContext;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 类型 `Rbac3GatewayRuntimeSnapshotReader` 位于当前包内，是类型，用于承载 `Rbac3 Gateway Runtime Snapshot Reader` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3GatewayRuntimeSnapshotReader` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Gateway Runtime Snapshot Reader`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Reads only Redis runtime projections and requires one version-consistent fact set.
 */
public final class Rbac3GatewayRuntimeSnapshotReader {

    /**
     * 字段 `redisson` 表示 `Rbac3GatewayRuntimeSnapshotReader` 中与 `redisson` 相关的状态、依赖、配置或结果（声明类型 `RedissonClient`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `redisson` stores the `redisson`-related state, dependency, configuration, or result of `Rbac3GatewayRuntimeSnapshotReader` (declared type `RedissonClient`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `redisson` 时应保持 `Rbac3GatewayRuntimeSnapshotReader` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `redisson`, preserve `Rbac3GatewayRuntimeSnapshotReader`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RedissonClient redisson;
    /**
     * 字段 `objectMapper` 表示 `Rbac3GatewayRuntimeSnapshotReader` 中与 `object Mapper` 相关的状态、依赖、配置或结果（声明类型 `ObjectMapper`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `objectMapper` stores the `object Mapper`-related state, dependency, configuration, or result of `Rbac3GatewayRuntimeSnapshotReader` (declared type `ObjectMapper`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `objectMapper` 时应保持 `Rbac3GatewayRuntimeSnapshotReader` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `objectMapper`, preserve `Rbac3GatewayRuntimeSnapshotReader`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ObjectMapper objectMapper;
    /**
     * 字段 `keyFactory` 表示 `Rbac3GatewayRuntimeSnapshotReader` 中与 `key Factory` 相关的状态、依赖、配置或结果（声明类型 `Rbac3RuntimeKeyFactory`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `keyFactory` stores the `key Factory`-related state, dependency, configuration, or result of `Rbac3GatewayRuntimeSnapshotReader` (declared type `Rbac3RuntimeKeyFactory`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `keyFactory` 时应保持 `Rbac3GatewayRuntimeSnapshotReader` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `keyFactory`, preserve `Rbac3GatewayRuntimeSnapshotReader`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Rbac3RuntimeKeyFactory keyFactory;
    /**
     * 字段 `clock` 表示 `Rbac3GatewayRuntimeSnapshotReader` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `Rbac3GatewayRuntimeSnapshotReader` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `Rbac3GatewayRuntimeSnapshotReader` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `Rbac3GatewayRuntimeSnapshotReader`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /**
     * 构造器 `Rbac3GatewayRuntimeSnapshotReader` 用于创建并初始化 `Rbac3GatewayRuntimeSnapshotReader` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3GatewayRuntimeSnapshotReader` creates and initializes `Rbac3GatewayRuntimeSnapshotReader`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3GatewayRuntimeSnapshotReader` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3GatewayRuntimeSnapshotReader`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param redisson 输入参数 `redisson`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param keyFactory 输入参数 `keyFactory`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3GatewayRuntimeSnapshotReader(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory,
            Clock clock
    ) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.keyFactory = Objects.requireNonNull(keyFactory, "keyFactory");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 方法 `verifySession` 按照 `Rbac3GatewayRuntimeSnapshotReader` 的职责处理输入，完成 `verify Session` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `verifySession` processes its inputs according to `Rbac3GatewayRuntimeSnapshotReader`'s responsibility, performs the `verify Session` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `verifySession` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `verifySession`, then continue the business flow using its result, exception, or side effect.
     *
     * @param claims 输入参数 `claims`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void verifySession(Rbac3TokenClaims claims) {
        runtime(claims.tid(), claims.sub(), claims.sid(),
                claims.av(), claims.sv(), claims.pv());
    }

    /**
     * 方法 `authorize` 按照 `Rbac3GatewayRuntimeSnapshotReader` 的职责处理输入，完成 `authorize` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authorize` processes its inputs according to `Rbac3GatewayRuntimeSnapshotReader`'s responsibility, performs the `authorize` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authorize` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authorize`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public AuthorizationDecision authorize(GatewayAuthContext context) {
        if (!context.principal().authenticated()) {
            return AuthorizationDecision.deny("RBAC3_PRINCIPAL_REQUIRED");
        }
        GatewayPrincipal principal = context.principal();
        Map<String, String> claims = principal.attributes();
        String sessionId = claims.get("rbac3.session-id");
        Long authVersion = number(claims.get("rbac3.auth-version"));
        Long sessionVersion = number(claims.get("rbac3.session-version"));
        Long policyVersion = number(claims.get("rbac3.policy-version"));
        if (principal.tenantId() == null || sessionId == null
                || authVersion == null || sessionVersion == null
                || policyVersion == null) {
            return AuthorizationDecision.deny("RBAC3_PRINCIPAL_CLAIMS_INVALID");
        }
        Map<String, String> route = context.attributes();
        String definitionSetId = route.get("rbac3.definition-set-id");
        Long mappingVersion = number(route.get("rbac3.mapping-version"));
        if (definitionSetId == null || mappingVersion == null) {
            return AuthorizationDecision.deny("RBAC3_OPERATION_MAPPING_MISSING");
        }
        SessionAuthorizationSnapshot snapshot = runtime(
                principal.tenantId(), principal.principalId(), sessionId,
                authVersion, sessionVersion, policyVersion);
        List<OperationPermissionMapping> mappings = mappings(redisson.getBucket(
                keyFactory.operationMapping(
                        principal.tenantId(), definitionSetId,
                        context.operationId(), mappingVersion)).get());
        List<OperationPermissionMapping> active = mappings.stream()
                .filter(OperationPermissionMapping::active)
                .filter(mapping -> principal.tenantId().equals(mapping.tenantId()))
                .filter(mapping -> definitionSetId.equals(mapping.definitionSetId()))
                .filter(mapping -> context.operationId().equals(
                        mapping.gatewayOperationId()))
                .filter(mapping -> mappingVersion == mapping.mappingVersion())
                .toList();
        if (active.size() != 1) {
            return AuthorizationDecision.deny(active.isEmpty()
                    ? "RBAC3_OPERATION_MAPPING_MISSING"
                    : "RBAC3_OPERATION_MAPPING_CONFLICT");
        }
        OperationPermissionMapping mapping = active.getFirst();
        if (!context.policyId().equals(mapping.securityPolicyId())) {
            return AuthorizationDecision.deny("RBAC3_SECURITY_POLICY_MISMATCH");
        }
        if (context.accessZone() == AccessZone.PUBLIC
                && !mapping.externalAccessible()) {
            return AuthorizationDecision.deny("RBAC3_OPERATION_NOT_EXTERNAL");
        }
        List<AppAuthorizationContext> applications = snapshot.appContexts().stream()
                .filter(application -> mapping.applicationCode().equals(
                        application.applicationCode()))
                .toList();
        if (applications.size() != 1) {
            return AuthorizationDecision.deny("RBAC3_APPLICATION_CONTEXT_INVALID");
        }
        return applications.getFirst().permissions().contains(mapping.permissionCode())
                ? AuthorizationDecision.allow()
                : AuthorizationDecision.deny("RBAC3_PERMISSION_DENIED");
    }

    /**
     * 方法 `runtime` 按照 `Rbac3GatewayRuntimeSnapshotReader` 的职责处理输入，完成 `runtime` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `runtime` processes its inputs according to `Rbac3GatewayRuntimeSnapshotReader`'s responsibility, performs the `runtime` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `runtime` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `runtime`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param userId 输入参数 `userId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param authVersion 输入参数 `authVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionVersion 输入参数 `sessionVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param policyVersion 输入参数 `policyVersion`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private SessionAuthorizationSnapshot runtime(
            String tenantId,
            String userId,
            String sessionId,
            long authVersion,
            long sessionVersion,
            long policyVersion
    ) {
        try {
            RuntimeSession session = convert(redisson.getBucket(
                    keyFactory.session(tenantId, sessionId)).get(), RuntimeSession.class);
            if (!tenantId.equals(session.tenantId())
                    || !userId.equals(session.userId())
                    || !sessionId.equals(session.sessionId())
                    || !"ACTIVE".equals(session.status())
                    || !session.expiresAt().isAfter(clock.instant())
                    || authVersion != session.authVersion()
                    || sessionVersion != session.sessionVersion()
                    || policyVersion != session.policyVersion()) {
                throw unavailable("RBAC3_SESSION_INVALID");
            }
            if (version(redisson.getBucket(
                    keyFactory.authVersion(tenantId, userId)).get()) != authVersion
                    || version(redisson.getBucket(
                    keyFactory.policyVersion(tenantId)).get()) != policyVersion) {
                throw unavailable("RBAC3_RUNTIME_VERSION_MISMATCH");
            }
            if (redisson.getBucket(keyFactory.sessionFence(
                    tenantId, sessionId)).isExists()) {
                throw unavailable("RBAC3_SESSION_FENCED");
            }
            SessionAuthorizationSnapshot snapshot = convert(redisson.getBucket(
                    keyFactory.snapshot(tenantId, sessionId, sessionVersion)).get(),
                    SessionAuthorizationSnapshot.class);
            if (!sessionId.equals(snapshot.sessionId())
                    || snapshot.authVersion() != authVersion
                    || snapshot.sessionVersion() != sessionVersion
                    || snapshot.policyVersion() != policyVersion) {
                throw unavailable("RBAC3_SNAPSHOT_VERSION_MISMATCH");
            }
            return snapshot;
        } catch (RuntimeUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RuntimeUnavailableException(
                    "RBAC3_AUTHORIZATION_RUNTIME_UNAVAILABLE", exception);
        }
    }

    /**
     * 方法 `mappings` 按照 `Rbac3GatewayRuntimeSnapshotReader` 的职责处理输入，完成 `mappings` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `mappings` processes its inputs according to `Rbac3GatewayRuntimeSnapshotReader`'s responsibility, performs the `mappings` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `mappings` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `mappings`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private List<OperationPermissionMapping> mappings(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Map<?, ?> map && map.get("mappings") != null) {
            return objectMapper.convertValue(
                    map.get("mappings"), new TypeReference<>() { });
        }
        if (value instanceof Collection<?>) {
            return objectMapper.convertValue(value, new TypeReference<>() { });
        }
        return List.of(convert(value, OperationPermissionMapping.class));
    }

    /**
     * 方法 `convert` 按照 `Rbac3GatewayRuntimeSnapshotReader` 的职责处理输入，完成 `convert` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `convert` processes its inputs according to `Rbac3GatewayRuntimeSnapshotReader`'s responsibility, performs the `convert` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `convert` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `convert`, then continue the business flow using its result, exception, or side effect.
     *
     * @param <T> 类型参数表示转换结果的具体类型；type parameter representing the converted result type.
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param type 输入参数 `type`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private <T> T convert(Object value, Class<T> type) {
        if (value == null) {
            throw new IllegalArgumentException("runtime value is missing");
        }
        return objectMapper.convertValue(value, type);
    }

    /**
     * 方法 `version` 按照 `Rbac3GatewayRuntimeSnapshotReader` 的职责处理输入，完成 `version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `version` processes its inputs according to `Rbac3GatewayRuntimeSnapshotReader`'s responsibility, performs the `version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `version` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `version`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private long version(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        if (value instanceof Map<?, ?> map && map.get("value") != null) {
            return version(map.get("value"));
        }
        throw new IllegalArgumentException("runtime version is missing");
    }

    /**
     * 方法 `number` 按照 `Rbac3GatewayRuntimeSnapshotReader` 的职责处理输入，完成 `number` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `number` processes its inputs according to `Rbac3GatewayRuntimeSnapshotReader`'s responsibility, performs the `number` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `number` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `number`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Long number(String value) {
        try {
            long number = Long.parseLong(value);
            return number < 0 ? null : number;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    /**
     * 方法 `unavailable` 按照 `Rbac3GatewayRuntimeSnapshotReader` 的职责处理输入，完成 `unavailable` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `unavailable` processes its inputs according to `Rbac3GatewayRuntimeSnapshotReader`'s responsibility, performs the `unavailable` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `unavailable` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `unavailable`, then continue the business flow using its result, exception, or side effect.
     *
     * @param code 输入参数 `code`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private RuntimeUnavailableException unavailable(String code) {
        return new RuntimeUnavailableException(code, null);
    }

    /**
     * 类型 `RuntimeSession` 位于 `Rbac3GatewayRuntimeSnapshotReader` 内，是记录类型，用于承载 `Runtime Session` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuntimeSession` is a record inside `Rbac3GatewayRuntimeSnapshotReader` and carries the responsibility, state, or contract for `Runtime Session`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuntimeSession` 作为 `Rbac3GatewayRuntimeSnapshotReader` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuntimeSession` as the responsibility boundary of `Rbac3GatewayRuntimeSnapshotReader`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param status 记录组件 `status` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `status` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    public record RuntimeSession(
            /**
             * 字段 `tenantId` 表示 `RuntimeSession` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `RuntimeSession` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `RuntimeSession` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `status` 表示 `RuntimeSession` 中与 `status` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `status` stores the `status`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `status` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `status`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            String status,
            /**
             * 字段 `authVersion` 表示 `RuntimeSession` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `sessionVersion` 表示 `RuntimeSession` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `RuntimeSession` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `expiresAt` 表示 `RuntimeSession` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `RuntimeSession` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `RuntimeSession` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `RuntimeSession`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant expiresAt
    ) {
    }

    /**
     * 类型 `OperationPermissionMapping` 位于 `Rbac3GatewayRuntimeSnapshotReader` 内，是记录类型，用于承载 `Operation Permission Mapping` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `OperationPermissionMapping` is a record inside `Rbac3GatewayRuntimeSnapshotReader` and carries the responsibility, state, or contract for `Operation Permission Mapping`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `OperationPermissionMapping` 作为 `Rbac3GatewayRuntimeSnapshotReader` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `OperationPermissionMapping` as the responsibility boundary of `Rbac3GatewayRuntimeSnapshotReader`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param applicationCode 记录组件 `applicationCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `applicationCode` carries constructor data whose meaning is defined by the record contract.
     * @param definitionSetId 记录组件 `definitionSetId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `definitionSetId` carries constructor data whose meaning is defined by the record contract.
     * @param gatewayOperationId 记录组件 `gatewayOperationId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `gatewayOperationId` carries constructor data whose meaning is defined by the record contract.
     * @param mappingVersion 记录组件 `mappingVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `mappingVersion` carries constructor data whose meaning is defined by the record contract.
     * @param permissionCode 记录组件 `permissionCode` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `permissionCode` carries constructor data whose meaning is defined by the record contract.
     * @param externalAccessible 记录组件 `externalAccessible` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `externalAccessible` carries constructor data whose meaning is defined by the record contract.
     * @param securityPolicyId 记录组件 `securityPolicyId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `securityPolicyId` carries constructor data whose meaning is defined by the record contract.
     * @param active 记录组件 `active` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `active` carries constructor data whose meaning is defined by the record contract.
     */
    public record OperationPermissionMapping(
            /**
             * 字段 `tenantId` 表示 `OperationPermissionMapping` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `OperationPermissionMapping` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `OperationPermissionMapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `OperationPermissionMapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `applicationCode` 表示 `OperationPermissionMapping` 中与 `application Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `applicationCode` stores the `application Code`-related state, dependency, configuration, or result of `OperationPermissionMapping` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `applicationCode` 时应保持 `OperationPermissionMapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `applicationCode`, preserve `OperationPermissionMapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            String applicationCode,
            /**
             * 字段 `definitionSetId` 表示 `OperationPermissionMapping` 中与 `definition Set Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `definitionSetId` stores the `definition Set Id`-related state, dependency, configuration, or result of `OperationPermissionMapping` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `definitionSetId` 时应保持 `OperationPermissionMapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `definitionSetId`, preserve `OperationPermissionMapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            String definitionSetId,
            /**
             * 字段 `gatewayOperationId` 表示 `OperationPermissionMapping` 中与 `gateway Operation Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `gatewayOperationId` stores the `gateway Operation Id`-related state, dependency, configuration, or result of `OperationPermissionMapping` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `gatewayOperationId` 时应保持 `OperationPermissionMapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `gatewayOperationId`, preserve `OperationPermissionMapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            String gatewayOperationId,
            /**
             * 字段 `mappingVersion` 表示 `OperationPermissionMapping` 中与 `mapping Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `mappingVersion` stores the `mapping Version`-related state, dependency, configuration, or result of `OperationPermissionMapping` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `mappingVersion` 时应保持 `OperationPermissionMapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `mappingVersion`, preserve `OperationPermissionMapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            long mappingVersion,
            /**
             * 字段 `permissionCode` 表示 `OperationPermissionMapping` 中与 `permission Code` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `permissionCode` stores the `permission Code`-related state, dependency, configuration, or result of `OperationPermissionMapping` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `permissionCode` 时应保持 `OperationPermissionMapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `permissionCode`, preserve `OperationPermissionMapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            String permissionCode,
            /**
             * 字段 `externalAccessible` 表示 `OperationPermissionMapping` 中与 `external Accessible` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `externalAccessible` stores the `external Accessible`-related state, dependency, configuration, or result of `OperationPermissionMapping` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `externalAccessible` 时应保持 `OperationPermissionMapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `externalAccessible`, preserve `OperationPermissionMapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean externalAccessible,
            /**
             * 字段 `securityPolicyId` 表示 `OperationPermissionMapping` 中与 `security Policy Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `securityPolicyId` stores the `security Policy Id`-related state, dependency, configuration, or result of `OperationPermissionMapping` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `securityPolicyId` 时应保持 `OperationPermissionMapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `securityPolicyId`, preserve `OperationPermissionMapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            String securityPolicyId,
            /**
             * 字段 `active` 表示 `OperationPermissionMapping` 中与 `active` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `active` stores the `active`-related state, dependency, configuration, or result of `OperationPermissionMapping` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `active` 时应保持 `OperationPermissionMapping` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `active`, preserve `OperationPermissionMapping`'s lifecycle, immutability, and thread-safety constraints.
             */
            boolean active
    ) {
    }

    /**
     * 类型 `RuntimeUnavailableException` 位于 `Rbac3GatewayRuntimeSnapshotReader` 内，是类型，用于承载 `Runtime Unavailable Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `RuntimeUnavailableException` is a type inside `Rbac3GatewayRuntimeSnapshotReader` and carries the responsibility, state, or contract for `Runtime Unavailable Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `RuntimeUnavailableException` 作为 `Rbac3GatewayRuntimeSnapshotReader` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `RuntimeUnavailableException` as the responsibility boundary of `Rbac3GatewayRuntimeSnapshotReader`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public static final class RuntimeUnavailableException extends RuntimeException {
        /**
         * 构造器 `RuntimeUnavailableException` 用于创建并初始化 `RuntimeUnavailableException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `RuntimeUnavailableException` creates and initializes `RuntimeUnavailableException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `RuntimeUnavailableException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `RuntimeUnavailableException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param message 输入参数 `message`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param cause 输入参数 `cause`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public RuntimeUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
