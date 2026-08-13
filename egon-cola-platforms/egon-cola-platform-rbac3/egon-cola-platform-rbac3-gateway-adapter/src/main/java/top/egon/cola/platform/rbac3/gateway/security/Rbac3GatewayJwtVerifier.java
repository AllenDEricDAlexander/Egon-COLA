package top.egon.cola.platform.rbac3.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;
import top.egon.cola.platform.rbac3.core.runtime.Rbac3RuntimeKeyFactory;

import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类型 `Rbac3GatewayJwtVerifier` 位于当前包内，是类型，用于承载 `Rbac3 Gateway Jwt Verifier` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3GatewayJwtVerifier` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Gateway Jwt Verifier`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * RS256 verifier backed by the Redis public Key Ring with a bounded-time in-memory LKG.
 */
public final class Rbac3GatewayJwtVerifier
        implements Rbac3JwtSessionAuthenticationProvider.TokenVerifier {

    /**
     * 字段 `redisson` 表示 `Rbac3GatewayJwtVerifier` 中与 `redisson` 相关的状态、依赖、配置或结果（声明类型 `RedissonClient`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `redisson` stores the `redisson`-related state, dependency, configuration, or result of `Rbac3GatewayJwtVerifier` (declared type `RedissonClient`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `redisson` 时应保持 `Rbac3GatewayJwtVerifier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `redisson`, preserve `Rbac3GatewayJwtVerifier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final RedissonClient redisson;
    /**
     * 字段 `objectMapper` 表示 `Rbac3GatewayJwtVerifier` 中与 `object Mapper` 相关的状态、依赖、配置或结果（声明类型 `ObjectMapper`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `objectMapper` stores the `object Mapper`-related state, dependency, configuration, or result of `Rbac3GatewayJwtVerifier` (declared type `ObjectMapper`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `objectMapper` 时应保持 `Rbac3GatewayJwtVerifier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `objectMapper`, preserve `Rbac3GatewayJwtVerifier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final ObjectMapper objectMapper;
    /**
     * 字段 `keyFactory` 表示 `Rbac3GatewayJwtVerifier` 中与 `key Factory` 相关的状态、依赖、配置或结果（声明类型 `Rbac3RuntimeKeyFactory`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `keyFactory` stores the `key Factory`-related state, dependency, configuration, or result of `Rbac3GatewayJwtVerifier` (declared type `Rbac3RuntimeKeyFactory`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `keyFactory` 时应保持 `Rbac3GatewayJwtVerifier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `keyFactory`, preserve `Rbac3GatewayJwtVerifier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Rbac3RuntimeKeyFactory keyFactory;
    /**
     * 字段 `clock` 表示 `Rbac3GatewayJwtVerifier` 中与 `clock` 相关的状态、依赖、配置或结果（声明类型 `Clock`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clock` stores the `clock`-related state, dependency, configuration, or result of `Rbac3GatewayJwtVerifier` (declared type `Clock`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clock` 时应保持 `Rbac3GatewayJwtVerifier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clock`, preserve `Rbac3GatewayJwtVerifier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Clock clock;
    /**
     * 字段 `issuer` 表示 `Rbac3GatewayJwtVerifier` 中与 `issuer` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `issuer` stores the `issuer`-related state, dependency, configuration, or result of `Rbac3GatewayJwtVerifier` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `issuer` 时应保持 `Rbac3GatewayJwtVerifier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `issuer`, preserve `Rbac3GatewayJwtVerifier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final String issuer;
    /**
     * 字段 `audience` 表示 `Rbac3GatewayJwtVerifier` 中与 `audience` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `audience` stores the `audience`-related state, dependency, configuration, or result of `Rbac3GatewayJwtVerifier` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `audience` 时应保持 `Rbac3GatewayJwtVerifier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `audience`, preserve `Rbac3GatewayJwtVerifier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final String audience;
    /**
     * 字段 `clockSkew` 表示 `Rbac3GatewayJwtVerifier` 中与 `clock Skew` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `clockSkew` stores the `clock Skew`-related state, dependency, configuration, or result of `Rbac3GatewayJwtVerifier` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `clockSkew` 时应保持 `Rbac3GatewayJwtVerifier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `clockSkew`, preserve `Rbac3GatewayJwtVerifier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Duration clockSkew;
    /**
     * 字段 `lkgTtl` 表示 `Rbac3GatewayJwtVerifier` 中与 `lkg Ttl` 相关的状态、依赖、配置或结果（声明类型 `Duration`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `lkgTtl` stores the `lkg Ttl`-related state, dependency, configuration, or result of `Rbac3GatewayJwtVerifier` (declared type `Duration`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `lkgTtl` 时应保持 `Rbac3GatewayJwtVerifier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `lkgTtl`, preserve `Rbac3GatewayJwtVerifier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Duration lkgTtl;
    /**
     * 字段 `cache` 表示 `Rbac3GatewayJwtVerifier` 中与 `cache` 相关的状态、依赖、配置或结果（声明类型 `Map&lt;KeyId, CachedKey&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `cache` stores the `cache`-related state, dependency, configuration, or result of `Rbac3GatewayJwtVerifier` (declared type `Map&lt;KeyId, CachedKey&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `cache` 时应保持 `Rbac3GatewayJwtVerifier` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `cache`, preserve `Rbac3GatewayJwtVerifier`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Map<KeyId, CachedKey> cache = new ConcurrentHashMap<>();

    /**
     * 构造器 `Rbac3GatewayJwtVerifier` 用于创建并初始化 `Rbac3GatewayJwtVerifier` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3GatewayJwtVerifier` creates and initializes `Rbac3GatewayJwtVerifier`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3GatewayJwtVerifier` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3GatewayJwtVerifier`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param redisson 输入参数 `redisson`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param objectMapper 输入参数 `objectMapper`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param keyFactory 输入参数 `keyFactory`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param issuer 输入参数 `issuer`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param audience 输入参数 `audience`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clockSkew 输入参数 `clockSkew`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param lkgTtl 输入参数 `lkgTtl`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3GatewayJwtVerifier(
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            Rbac3RuntimeKeyFactory keyFactory,
            Clock clock,
            String issuer,
            String audience,
            Duration clockSkew,
            Duration lkgTtl
    ) {
        this.redisson = Objects.requireNonNull(redisson, "redisson");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.keyFactory = Objects.requireNonNull(keyFactory, "keyFactory");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.issuer = required(issuer, "issuer");
        this.audience = required(audience, "audience");
        this.clockSkew = positive(clockSkew, "clockSkew");
        this.lkgTtl = positive(lkgTtl, "lkgTtl");
    }

    /**
     * 方法 `verify` 按照 `Rbac3GatewayJwtVerifier` 的职责处理输入，完成 `verify` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `verify` processes its inputs according to `Rbac3GatewayJwtVerifier`'s responsibility, performs the `verify` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `verify` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `verify`, then continue the business flow using its result, exception, or side effect.
     *
     * @param token 输入参数 `token`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public Rbac3TokenClaims verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(required(token, "token"));
            if (!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm())) {
                throw invalid("RBAC3_JWT_ALGORITHM_INVALID");
            }
            String kid = required(jwt.getHeader().getKeyID(), "kid");
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            String tenantId = required(claims.getStringClaim("tid"), "tid");
            RSAKey publicKey = key(tenantId, kid);
            if (!jwt.verify(new RSASSAVerifier(publicKey.toRSAPublicKey()))) {
                throw invalid("RBAC3_JWT_SIGNATURE_INVALID");
            }
            validateStandardClaims(claims);
            return new Rbac3TokenClaims(
                    claims.getIssuer(), claims.getAudience(), claims.getSubject(),
                    tenantId, required(claims.getStringClaim("sid"), "sid"),
                    nonNegative(claims.getLongClaim("av"), "av"),
                    nonNegative(claims.getLongClaim("sv"), "sv"),
                    nonNegative(claims.getLongClaim("pv"), "pv"),
                    required(claims.getJWTID(), "jti"),
                    instant(claims.getIssueTime(), "iat"),
                    instant(claims.getNotBeforeTime(), "nbf"),
                    instant(claims.getExpirationTime(), "exp"), kid);
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidTokenException("RBAC3_JWT_INVALID", exception);
        }
    }

    /**
     * 方法 `validateStandardClaims` 按照 `Rbac3GatewayJwtVerifier` 的职责处理输入，完成 `validate Standard Claims` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `validateStandardClaims` processes its inputs according to `Rbac3GatewayJwtVerifier`'s responsibility, performs the `validate Standard Claims` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `validateStandardClaims` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `validateStandardClaims`, then continue the business flow using its result, exception, or side effect.
     *
     * @param claims 输入参数 `claims`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @throws ParseException 当输入违反契约或依赖不可用时抛出；thrown when the contract is violated or a dependency is unavailable.
     */
    private void validateStandardClaims(JWTClaimsSet claims) throws ParseException {
        if (!issuer.equals(claims.getIssuer())) {
            throw invalid("RBAC3_JWT_ISSUER_INVALID");
        }
        if (claims.getAudience() == null || !claims.getAudience().contains(audience)) {
            throw invalid("RBAC3_JWT_AUDIENCE_INVALID");
        }
        Instant now = clock.instant();
        Instant issuedAt = instant(claims.getIssueTime(), "iat");
        Instant notBefore = instant(claims.getNotBeforeTime(), "nbf");
        Instant expiresAt = instant(claims.getExpirationTime(), "exp");
        if (issuedAt.isAfter(now.plus(clockSkew))
                || notBefore.isAfter(now.plus(clockSkew))
                || !expiresAt.isAfter(now.minus(clockSkew))) {
            throw invalid("RBAC3_JWT_TIME_INVALID");
        }
    }

    /**
     * 方法 `key` 按照 `Rbac3GatewayJwtVerifier` 的职责处理输入，完成 `key` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `key` processes its inputs according to `Rbac3GatewayJwtVerifier`'s responsibility, performs the `key` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `key` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `key`, then continue the business flow using its result, exception, or side effect.
     *
     * @param tenantId 输入参数 `tenantId`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param kid 输入参数 `kid`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private RSAKey key(String tenantId, String kid) {
        KeyId id = new KeyId(tenantId, kid);
        try {
            Object value = redisson.getBucket(keyFactory.keyRing(tenantId)).get();
            Map<?, ?> ring = objectMapper.convertValue(value, Map.class);
            Object values = ring.get("keys");
            if (!(values instanceof Collection<?> keys)) {
                throw new IllegalArgumentException("public Key Ring is missing keys");
            }
            Instant expiresAt = clock.instant().plus(lkgTtl);
            Map<KeyId, CachedKey> refreshed = new HashMap<>();
            for (Object entry : keys) {
                @SuppressWarnings("unchecked")
                Map<String, Object> jwk = objectMapper.convertValue(entry, Map.class);
                RSAKey key = RSAKey.parse(jwk).toPublicJWK();
                if (JWSAlgorithm.RS256.equals(key.getAlgorithm())
                        && key.getKeyID() != null) {
                    refreshed.put(new KeyId(tenantId, key.getKeyID()),
                            new CachedKey(key, expiresAt));
                }
            }
            cache.keySet().removeIf(keyId -> tenantId.equals(keyId.tenantId()));
            cache.putAll(refreshed);
            CachedKey selected = refreshed.get(id);
            if (selected == null) {
                throw invalid("RBAC3_JWT_KID_UNKNOWN");
            }
            return selected.key();
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (RuntimeException | java.text.ParseException exception) {
            CachedKey selected = cache.get(id);
            if (selected != null && selected.expiresAt().isAfter(clock.instant())) {
                return selected.key();
            }
            throw new InvalidTokenException("RBAC3_KEY_RING_UNAVAILABLE", exception);
        }
    }

    /**
     * 方法 `instant` 按照 `Rbac3GatewayJwtVerifier` 的职责处理输入，完成 `instant` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `instant` processes its inputs according to `Rbac3GatewayJwtVerifier`'s responsibility, performs the `instant` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `instant` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `instant`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Instant instant(java.util.Date value, String field) {
        if (value == null) {
            throw invalid("RBAC3_JWT_CLAIM_INVALID_" + field.toUpperCase());
        }
        return value.toInstant();
    }

    /**
     * 方法 `nonNegative` 按照 `Rbac3GatewayJwtVerifier` 的职责处理输入，完成 `non Negative` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `nonNegative` processes its inputs according to `Rbac3GatewayJwtVerifier`'s responsibility, performs the `non Negative` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `nonNegative` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `nonNegative`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private long nonNegative(Long value, String field) {
        if (value == null || value < 0) {
            throw invalid("RBAC3_JWT_CLAIM_INVALID_" + field.toUpperCase());
        }
        return value;
    }

    /**
     * 方法 `required` 按照 `Rbac3GatewayJwtVerifier` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `Rbac3GatewayJwtVerifier`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `required` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `required`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid("RBAC3_JWT_CLAIM_INVALID_" + field.toUpperCase());
        }
        return value.trim();
    }

    /**
     * 方法 `positive` 按照 `Rbac3GatewayJwtVerifier` 的职责处理输入，完成 `positive` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `positive` processes its inputs according to `Rbac3GatewayJwtVerifier`'s responsibility, performs the `positive` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `positive` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `positive`, then continue the business flow using its result, exception, or side effect.
     *
     * @param value 输入参数 `value`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param field 输入参数 `field`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private Duration positive(Duration value, String field) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    /**
     * 方法 `invalid` 按照 `Rbac3GatewayJwtVerifier` 的职责处理输入，完成 `invalid` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `invalid` processes its inputs according to `Rbac3GatewayJwtVerifier`'s responsibility, performs the `invalid` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `invalid` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `invalid`, then continue the business flow using its result, exception, or side effect.
     *
     * @param code 输入参数 `code`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    private InvalidTokenException invalid(String code) {
        return new InvalidTokenException(code, null);
    }

    /**
     * 类型 `KeyId` 位于 `Rbac3GatewayJwtVerifier` 内，是记录类型，用于承载 `Key Id` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `KeyId` is a record inside `Rbac3GatewayJwtVerifier` and carries the responsibility, state, or contract for `Key Id`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `KeyId` 作为 `Rbac3GatewayJwtVerifier` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `KeyId` as the responsibility boundary of `Rbac3GatewayJwtVerifier`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param kid 记录组件 `kid` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `kid` carries constructor data whose meaning is defined by the record contract.
     */
    private record KeyId(/**
 * 字段 `tenantId` 表示 `KeyId` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `KeyId` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `KeyId` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `KeyId`'s lifecycle, immutability, and thread-safety constraints.
 */ String tenantId, /**
 * 字段 `kid` 表示 `KeyId` 中与 `kid` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `kid` stores the `kid`-related state, dependency, configuration, or result of `KeyId` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `kid` 时应保持 `KeyId` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `kid`, preserve `KeyId`'s lifecycle, immutability, and thread-safety constraints.
 */ String kid) {
    }

    /**
     * 类型 `CachedKey` 位于 `Rbac3GatewayJwtVerifier` 内，是记录类型，用于承载 `Cached Key` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `CachedKey` is a record inside `Rbac3GatewayJwtVerifier` and carries the responsibility, state, or contract for `Cached Key`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `CachedKey` 作为 `Rbac3GatewayJwtVerifier` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `CachedKey` as the responsibility boundary of `Rbac3GatewayJwtVerifier`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param key 记录组件 `key` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `key` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     */
    private record CachedKey(/**
 * 字段 `key` 表示 `CachedKey` 中与 `key` 相关的状态、依赖、配置或结果（声明类型 `RSAKey`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `key` stores the `key`-related state, dependency, configuration, or result of `CachedKey` (declared type `RSAKey`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `key` 时应保持 `CachedKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `key`, preserve `CachedKey`'s lifecycle, immutability, and thread-safety constraints.
 */ RSAKey key, /**
 * 字段 `expiresAt` 表示 `CachedKey` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
 * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `CachedKey` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
 *
 * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `CachedKey` 的生命周期、不可变性和线程安全约束。
 * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `CachedKey`'s lifecycle, immutability, and thread-safety constraints.
 */ Instant expiresAt) {
    }

    /**
     * 类型 `InvalidTokenException` 位于 `Rbac3GatewayJwtVerifier` 内，是类型，用于承载 `Invalid Token Exception` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `InvalidTokenException` is a type inside `Rbac3GatewayJwtVerifier` and carries the responsibility, state, or contract for `Invalid Token Exception`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `InvalidTokenException` 作为 `Rbac3GatewayJwtVerifier` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `InvalidTokenException` as the responsibility boundary of `Rbac3GatewayJwtVerifier`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    public static final class InvalidTokenException extends RuntimeException {
        /**
         * 构造器 `InvalidTokenException` 用于创建并初始化 `InvalidTokenException` 实例，建立该类型后续方法所依赖的状态和不变量。
         * Constructor `InvalidTokenException` creates and initializes `InvalidTokenException`, establishing the state and invariants required by subsequent operations.
         *
         * 用法：通过 `InvalidTokenException` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
         * Usage: create the instance through `InvalidTokenException`'s constructor entry point and do not bypass the validation and initialization constraints established there.
         *
         * @param message 输入参数 `message`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @param cause 输入参数 `cause`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        public InvalidTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
