package top.egon.cola.platform.rbac3.admin.snapshot.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import top.egon.cola.platform.rbac3.admin.activation.application.RoleActivationFacade;
import top.egon.cola.platform.rbac3.admin.authorization.application.AuthorizationDecisionService;
import top.egon.cola.platform.rbac3.admin.snapshot.application.SessionSnapshotProjector;
import top.egon.cola.platform.rbac3.contract.authorization.SessionAuthorizationSnapshot;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 类型 `RedisAuthorizationRuntimeStore` 位于当前包内，是类型，用于承载 `Redis Authorization Runtime Store` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `RedisAuthorizationRuntimeStore` is a type in its package and carries the responsibility, state, or contract for `Redis Authorization Runtime Store`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Publishes the session pointer and its immutable snapshot through one Redis script.
 */
@Repository
public class RedisAuthorizationRuntimeStore implements
        RoleActivationFacade.RuntimeStore,
        AuthorizationDecisionService.SnapshotSource,
        AuthorizationDecisionService.FenceVerifier {

    /**
     * 字段 `PUBLISH_SCRIPT` 表示 `RedisAuthorizationRuntimeStore` 中与 `PUBLISH SCRIPT` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `PUBLISH_SCRIPT` stores the `PUBLISH SCRIPT`-related state, dependency, configuration, or result of `RedisAuthorizationRuntimeStore` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `PUBLISH_SCRIPT` 时应保持 `RedisAuthorizationRuntimeStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `PUBLISH_SCRIPT`, preserve `RedisAuthorizationRuntimeStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String PUBLISH_SCRIPT = script(
            "redis/publish-session-snapshot.lua");
    /**
     * 字段 `VERIFY_FENCE_SCRIPT` 表示 `RedisAuthorizationRuntimeStore` 中与 `VERIFY FENCE SCRIPT` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `VERIFY_FENCE_SCRIPT` stores the `VERIFY FENCE SCRIPT`-related state, dependency, configuration, or result of `RedisAuthorizationRuntimeStore` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `VERIFY_FENCE_SCRIPT` 时应保持 `RedisAuthorizationRuntimeStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `VERIFY_FENCE_SCRIPT`, preserve `RedisAuthorizationRuntimeStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private static final String VERIFY_FENCE_SCRIPT = script(
            "redis/verify-authorization-fence.lua");

    /**
     * 字段 `redisson` 表示 `RedisAuthorizationRuntimeStore` 中与 `redisson` 相关的状态、依赖、配置或结果（声明类型 `RedissonClient`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `redisson` stores the `redisson`-related state, dependency, configuration, or result of `RedisAuthorizationRuntimeStore` (declared type `RedissonClient`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `redisson` 时应保持 `RedisAuthorizationRuntimeStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `redisson`, preserve `RedisAuthorizationRuntimeStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RedissonClient redisson;
    /**
     * 字段 `objectMapper` 表示 `RedisAuthorizationRuntimeStore` 中与 `object Mapper` 相关的状态、依赖、配置或结果（声明类型 `ObjectMapper`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `objectMapper` stores the `object Mapper`-related state, dependency, configuration, or result of `RedisAuthorizationRuntimeStore` (declared type `ObjectMapper`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `objectMapper` 时应保持 `RedisAuthorizationRuntimeStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `objectMapper`, preserve `RedisAuthorizationRuntimeStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ObjectMapper objectMapper;
    /**
     * 字段 `keyFactory` 表示 `RedisAuthorizationRuntimeStore` 中与 `key Factory` 相关的状态、依赖、配置或结果（声明类型 `Rbac3RuntimeKeyFactory`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `keyFactory` stores the `key Factory`-related state, dependency, configuration, or result of `RedisAuthorizationRuntimeStore` (declared type `Rbac3RuntimeKeyFactory`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `keyFactory` 时应保持 `RedisAuthorizationRuntimeStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `keyFactory`, preserve `RedisAuthorizationRuntimeStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Rbac3RuntimeKeyFactory keyFactory;
    /**
     * 字段 `clock` 表示 `RedisAuthorizationRuntimeStore` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `RedisAuthorizationRuntimeStore` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `RedisAuthorizationRuntimeStore` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `RedisAuthorizationRuntimeStore`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;

    /**
     * 构造器 `RedisAuthorizationRuntimeStore` 用于创建并初始化 `RedisAuthorizationRuntimeStore` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RedisAuthorizationRuntimeStore` creates and initializes `RedisAuthorizationRuntimeStore`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RedisAuthorizationRuntimeStore` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RedisAuthorizationRuntimeStore`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param redisson 输入参数 `redisson`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param keyFactory 输入参数 `keyFactory`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public RedisAuthorizationRuntimeStore(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory
    ) {
        this(redisson, objectMapper, keyFactory, Clock.systemUTC());
    }

    /**
     * 构造器 `RedisAuthorizationRuntimeStore` 用于创建并初始化 `RedisAuthorizationRuntimeStore` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `RedisAuthorizationRuntimeStore` creates and initializes `RedisAuthorizationRuntimeStore`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `RedisAuthorizationRuntimeStore` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `RedisAuthorizationRuntimeStore`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param redisson 输入参数 `redisson`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param keyFactory 输入参数 `keyFactory`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Autowired
    public RedisAuthorizationRuntimeStore(
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
     * 方法 `publish` 按照 `RedisAuthorizationRuntimeStore` 的职责处理输入，完成 `publish` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `publish` processes its inputs according to `RedisAuthorizationRuntimeStore`'s responsibility, performs the `publish` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `publish` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `publish`, then continue the business flow using its result, exception, or side effect.
     *
     * @param command 输入参数 `command`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public PublishResult publish(PublishCommand command) {
        var session = command.projection().session();
        var snapshot = command.projection().snapshot();
        List<Object> keys = List.of(
                keyFactory.session(command.tenantId(), command.sessionId()),
                keyFactory.authVersion(command.tenantId(), command.userId()),
                keyFactory.policyVersion(command.tenantId()),
                keyFactory.snapshot(
                        command.tenantId(), command.sessionId(), command.sessionVersion()),
                keyFactory.sessionFence(command.tenantId(), command.sessionId()));
        Number result = redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                PUBLISH_SCRIPT,
                RScript.ReturnType.INTEGER,
                keys,
                json(session),
                Long.toString(command.authVersion()),
                Long.toString(command.policyVersion()),
                json(snapshot),
                Long.toString(command.sessionVersion()),
                Long.toString(session.expiresAt().toEpochMilli()));
        if (result == null || result.intValue() < 0) {
            throw new IllegalStateException("RBAC3_RUNTIME_VERSION_CONFLICT");
        }
        return new PublishResult(result.intValue() == 1, snapshot.checksum());
    }

    /**
     * 方法 `publish` 按照 `RedisAuthorizationRuntimeStore` 的职责处理输入，完成 `publish` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `publish` processes its inputs according to `RedisAuthorizationRuntimeStore`'s responsibility, performs the `publish` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `publish` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `publish`, then continue the business flow using its result, exception, or side effect.
     *
     * @param publication 输入参数 `publication`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    public void publish(RoleActivationFacade.RuntimePublication publication) {
        publish(new PublishCommand(
                publication.tenantId(), publication.userId(), publication.sessionId(),
                publication.authVersion(), publication.sessionVersion(),
                publication.policyVersion(), publication.projection()));
    }

    /**
     * 方法 `createSessionFence` 按照 `RedisAuthorizationRuntimeStore` 的职责处理输入，完成 `create Session Fence` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `createSessionFence` processes its inputs according to `RedisAuthorizationRuntimeStore`'s responsibility, performs the `create Session Fence` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `createSessionFence` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `createSessionFence`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ttl 输入参数 `ttl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void createSessionFence(
            String tenantId,
            String sessionId,
            String mutationId,
            Duration ttl
    ) {
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("fence ttl must be positive");
        }
        RBucket<String> bucket = redisson.getBucket(
                keyFactory.sessionFence(tenantId, sessionId), StringCodec.INSTANCE);
        bucket.set(mutationId, ttl);
    }

    /**
     * 方法 `createFence` 按照 `RedisAuthorizationRuntimeStore` 的职责处理输入，完成 `create Fence` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `createFence` processes its inputs according to `RedisAuthorizationRuntimeStore`'s responsibility, performs the `create Fence` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `createFence` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `createFence`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param mutationId 输入参数 `mutationId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param ttl 输入参数 `ttl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    @Override
    public void createFence(
            String tenantId,
            String sessionId,
            String mutationId,
            Duration ttl
    ) {
        createSessionFence(tenantId, sessionId, mutationId, ttl);
    }

    /**
     * 方法 `isSessionFenced` 按照 `RedisAuthorizationRuntimeStore` 的职责处理输入，完成 `is Session Fenced` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isSessionFenced` processes its inputs according to `RedisAuthorizationRuntimeStore`'s responsibility, performs the `is Session Fenced` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `isSessionFenced` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `isSessionFenced`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public boolean isSessionFenced(String tenantId, String sessionId) {
        Number result = redisson.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_ONLY,
                VERIFY_FENCE_SCRIPT,
                RScript.ReturnType.INTEGER,
                List.of(keyFactory.sessionFence(tenantId, sessionId)));
        return result != null && result.intValue() == 1;
    }

    /**
     * 方法 `removeSessionFence` 按照 `RedisAuthorizationRuntimeStore` 的职责处理输入，完成 `remove Session Fence` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `removeSessionFence` processes its inputs according to `RedisAuthorizationRuntimeStore`'s responsibility, performs the `remove Session Fence` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `removeSessionFence` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `removeSessionFence`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public void removeSessionFence(String tenantId, String sessionId) {
        redisson.getBucket(
                keyFactory.sessionFence(tenantId, sessionId), StringCodec.INSTANCE)
                .delete();
    }

    /**
     * 方法 `load` 按照 `RedisAuthorizationRuntimeStore` 的职责处理输入，完成 `load` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `load` processes its inputs according to `RedisAuthorizationRuntimeStore`'s responsibility, performs the `load` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `load` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `load`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public AuthorizationDecisionService.SnapshotRecord load(
            String tenantId,
            String sessionId) {
        try {
            RBucket<String> sessionBucket = redisson.getBucket(
                    keyFactory.session(tenantId, sessionId), StringCodec.INSTANCE);
            String sessionJson = sessionBucket.get();
            if (sessionJson == null) {
                throw new Rbac3RuleViolation("AUTH_SNAPSHOT_NOT_READY");
            }
            SessionSnapshotProjector.RuntimeSession session = objectMapper.readValue(
                    sessionJson, SessionSnapshotProjector.RuntimeSession.class);
            if (!tenantId.equals(session.tenantId())
                    || !sessionId.equals(session.sessionId())
                    || !"ACTIVE".equals(session.status())
                    || !session.expiresAt().isAfter(clock.instant())) {
                throw new Rbac3RuleViolation("SESSION_INVALIDATED");
            }
            if (version(redisson.getBucket(
                    keyFactory.authVersion(tenantId, session.userId()),
                    StringCodec.INSTANCE).get()) != session.authVersion()) {
                throw new Rbac3RuleViolation("AUTH_VERSION_MISMATCH");
            }
            if (version(redisson.getBucket(
                    keyFactory.policyVersion(tenantId),
                    StringCodec.INSTANCE).get()) != session.policyVersion()) {
                throw new Rbac3RuleViolation("POLICY_VERSION_MISMATCH");
            }
            if (redisson.getBucket(
                    keyFactory.sessionFence(tenantId, sessionId),
                    StringCodec.INSTANCE).isExists()) {
                throw new Rbac3RuleViolation("AUTH_PROPAGATION_PENDING");
            }
            RBucket<String> snapshotBucket = redisson.getBucket(
                    keyFactory.snapshot(tenantId, sessionId, session.sessionVersion()),
                    StringCodec.INSTANCE);
            String snapshotJson = snapshotBucket.get();
            if (snapshotJson == null) {
                throw new Rbac3RuleViolation("AUTH_SNAPSHOT_NOT_READY");
            }
            SessionAuthorizationSnapshot snapshot = objectMapper.readValue(
                    snapshotJson, SessionAuthorizationSnapshot.class);
            if (!sessionId.equals(snapshot.sessionId())
                    || snapshot.authVersion() != session.authVersion()
                    || snapshot.sessionVersion() != session.sessionVersion()
                    || snapshot.policyVersion() != session.policyVersion()) {
                throw new Rbac3RuleViolation("SESSION_VERSION_MISMATCH");
            }
            return new AuthorizationDecisionService.SnapshotRecord(
                    tenantId, session.identitySub(), session.userId(), snapshot);
        } catch (Rbac3RuleViolation error) {
            throw error;
        } catch (RuntimeException | JsonProcessingException error) {
            throw new Rbac3RuleViolation("AUTH_RUNTIME_UNAVAILABLE");
        }
    }

    /**
     * 方法 `isFenced` 按照 `RedisAuthorizationRuntimeStore` 的职责处理输入，完成 `is Fenced` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `isFenced` processes its inputs according to `RedisAuthorizationRuntimeStore`'s responsibility, performs the `is Fenced` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `isFenced` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `isFenced`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionId 输入参数 `sessionId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public boolean isFenced(String tenantId, String sessionId) {
        return isSessionFenced(tenantId, sessionId);
    }

    /**
     * 方法 `json` 按照 `RedisAuthorizationRuntimeStore` 的职责处理输入，完成 `json` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `json` processes its inputs according to `RedisAuthorizationRuntimeStore`'s responsibility, performs the `json` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `json` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `json`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("cannot encode RBAC3 runtime projection", exception);
        }
    }

    /**
     * 方法 `version` 按照 `RedisAuthorizationRuntimeStore` 的职责处理输入，完成 `version` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `version` processes its inputs according to `RedisAuthorizationRuntimeStore`'s responsibility, performs the `version` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
        if (value instanceof java.util.Map<?, ?> map && map.get("value") != null) {
            return version(map.get("value"));
        }
        throw new IllegalArgumentException("runtime version is missing");
    }

    /**
     * 方法 `script` 按照 `RedisAuthorizationRuntimeStore` 的职责处理输入，完成 `script` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `script` processes its inputs according to `RedisAuthorizationRuntimeStore`'s responsibility, performs the `script` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `script` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `script`, then continue the business flow using its result, exception, or side effect.
     *
     * @param location 输入参数 `location`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private static String script(String location) {
        try (var input = new ClassPathResource(location).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot load Redis script: " + location, exception);
        }
    }

    /**
     * 类型 `PublishCommand` 位于 `RedisAuthorizationRuntimeStore` 内，是记录类型，用于承载 `Publish Command` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PublishCommand` is a record inside `RedisAuthorizationRuntimeStore` and carries the responsibility, state, or contract for `Publish Command`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PublishCommand` 作为 `RedisAuthorizationRuntimeStore` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PublishCommand` as the responsibility boundary of `RedisAuthorizationRuntimeStore`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     * @param projection 记录组件 `projection` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `projection` carries constructor data whose meaning is defined by the record contract.
     */
    public record PublishCommand(
            /**
             * 字段 `tenantId` 表示 `PublishCommand` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `PublishCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `PublishCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `PublishCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `PublishCommand` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `PublishCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `PublishCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `PublishCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `PublishCommand` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `PublishCommand` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `PublishCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `PublishCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `authVersion` 表示 `PublishCommand` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `PublishCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `PublishCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `PublishCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `sessionVersion` 表示 `PublishCommand` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `PublishCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `PublishCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `PublishCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `PublishCommand` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `PublishCommand` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `PublishCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `PublishCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion,
            /**
             * 字段 `projection` 表示 `PublishCommand` 中与 `projection` 相关的状态、依赖、配置或结果（声明类型 `SessionSnapshotProjector.Projection`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `projection` stores the `projection`-related state, dependency, configuration, or result of `PublishCommand` (declared type `SessionSnapshotProjector.Projection`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `projection` 时应保持 `PublishCommand` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `projection`, preserve `PublishCommand`'s lifecycle, immutability, and thread-safety constraints.
             */
            SessionSnapshotProjector.Projection projection
    ) {
    }

    /**
     * 类型 `PublishResult` 位于 `RedisAuthorizationRuntimeStore` 内，是记录类型，用于承载 `Publish Result` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `PublishResult` is a record inside `RedisAuthorizationRuntimeStore` and carries the responsibility, state, or contract for `Publish Result`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `PublishResult` 作为 `RedisAuthorizationRuntimeStore` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `PublishResult` as the responsibility boundary of `RedisAuthorizationRuntimeStore`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param changed 记录组件 `changed` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `changed` carries constructor data whose meaning is defined by the record contract.
     * @param checksum 记录组件 `checksum` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `checksum` carries constructor data whose meaning is defined by the record contract.
     */
    public record PublishResult(/**
 * 字段 `changed` 表示 `PublishResult` 中与 `changed` 相关的状态、依赖、配置或结果（声明类型 `boolean`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `changed` stores the `changed`-related state, dependency, configuration, or result of `PublishResult` (declared type `boolean`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `changed` 时应保持 `PublishResult` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `changed`, preserve `PublishResult`'s lifecycle, immutability, and thread-safety constraints.
 */ boolean changed, /**
 * 字段 `checksum` 表示 `PublishResult` 中与 `checksum` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `checksum` stores the `checksum`-related state, dependency, configuration, or result of `PublishResult` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `checksum` 时应保持 `PublishResult` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `checksum`, preserve `PublishResult`'s lifecycle, immutability, and thread-safety constraints.
 */ String checksum) {
    }
}
