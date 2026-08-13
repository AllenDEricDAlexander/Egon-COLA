package top.egon.cola.platform.rbac3.admin.config.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import top.egon.cola.platform.rbac3.admin.runtime.repository.redis.RedisAuthorizationRuntimeRepository;
import top.egon.cola.platform.rbac3.core.rule.Rbac3RuleViolation;

import java.util.LinkedHashSet;
import java.util.Set;
import top.egon.cola.platform.rbac3.admin.authorization.domain.vo.SnapshotRecordVO;

/**
 * 将已验证的用户 Bearer Claim 转换为 RBAC3 用户主体。
 * Converts validated user bearer claims into the RBAC3 user principal.
 * 语义与用法：将 `Rbac3JwtAuthenticationConverter` 作为 `当前包` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
 * Semantics and usage: use `Rbac3JwtAuthenticationConverter` as the responsibility boundary of `the current package`, following its existing construction, interface, or Spring-assembly mechanism.
 */
public final class Rbac3JwtAuthenticationConverter
        implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    /**
     * 字段 `runtimeStore` 表示 `Rbac3JwtAuthenticationConverter` 中与 `runtime Store` 相关的状态、依赖、配置或结果（声明类型 `RedisAuthorizationRuntimeRepository`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `runtimeStore` stores the `runtime Store`-related state, dependency, configuration, or result of `Rbac3JwtAuthenticationConverter` (declared type `RedisAuthorizationRuntimeRepository`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `runtimeStore` 时应保持 `Rbac3JwtAuthenticationConverter` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `runtimeStore`, preserve `Rbac3JwtAuthenticationConverter`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RedisAuthorizationRuntimeRepository runtimeStore;

    /**
     * 构造器 `Rbac3JwtAuthenticationConverter` 用于创建并初始化 `Rbac3JwtAuthenticationConverter` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3JwtAuthenticationConverter` creates and initializes `Rbac3JwtAuthenticationConverter`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3JwtAuthenticationConverter` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3JwtAuthenticationConverter`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param runtimeStore 输入参数 `runtimeStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3JwtAuthenticationConverter(
            RedisAuthorizationRuntimeRepository runtimeStore) {
        this.runtimeStore = runtimeStore;
    }

    /**
     * 方法 `convert` 按照 `Rbac3JwtAuthenticationConverter` 的职责处理输入，完成 `convert` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `convert` processes its inputs according to `Rbac3JwtAuthenticationConverter`'s responsibility, performs the `convert` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `convert` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `convert`, then continue the business flow using its result, exception, or side effect.
     *
     * @param jwt 输入参数 `jwt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        CurrentRbac3Principal principal = user(jwt);
        return UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.authorities());
    }

    /**
     * 方法 `user` 按照 `Rbac3JwtAuthenticationConverter` 的职责处理输入，完成 `user` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `user` processes its inputs according to `Rbac3JwtAuthenticationConverter`'s responsibility, performs the `user` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `user` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `user`, then continue the business flow using its result, exception, or side effect.
     *
     * @param jwt 输入参数 `jwt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private CurrentRbac3Principal user(Jwt jwt) {
        String tenantId = required(jwt.getClaimAsString("tid"), "tid");
        String identitySub = required(jwt.getSubject(), "sub");
        String sessionId = required(jwt.getClaimAsString("sid"), "sid");
        long authVersion = number(jwt, "av");
        long sessionVersion = number(jwt, "sv");
        long policyVersion = number(jwt, "pv");
        SnapshotRecordVO record = runtimeStore.load(
                tenantId, sessionId);
        if (!record.tenantId().equals(tenantId)
                || !record.identitySub().equals(identitySub)
                || record.snapshot().authVersion() != authVersion
                || record.snapshot().sessionVersion() != sessionVersion
                || record.snapshot().policyVersion() != policyVersion) {
            throw new Rbac3RuleViolation("AUTHORIZATION_VERSION_MISMATCH");
        }
        Set<String> permissions = new LinkedHashSet<>();
        record.snapshot().appContexts().forEach(
                context -> permissions.addAll(context.permissions()));
        return new CurrentRbac3Principal(
                tenantId, identitySub, record.userId(), sessionId,
                authVersion, sessionVersion,
                policyVersion, permissions,
                Boolean.TRUE.equals(jwt.getClaim("platform_administrator")));
    }

    /**
     * 方法 `number` 按照 `Rbac3JwtAuthenticationConverter` 的职责处理输入，完成 `number` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `number` processes its inputs according to `Rbac3JwtAuthenticationConverter`'s responsibility, performs the `number` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `number` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `number`, then continue the business flow using its result, exception, or side effect.
     *
     * @param jwt 输入参数 `jwt`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private long number(Jwt jwt, String name) {
        Object value = jwt.getClaim(name);
        if (!(value instanceof Number number) || number.longValue() < 0L) {
            throw new IllegalArgumentException(name + " must be a non-negative number");
        }
        return number.longValue();
    }

    /**
     * 方法 `required` 按照 `Rbac3JwtAuthenticationConverter` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `Rbac3JwtAuthenticationConverter`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param name 输入参数 `name`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}
