package top.egon.cola.platform.rbac3.admin.auth.service;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.platform.rbac3.admin.application.port.Rbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.AccessTokenSubjectVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.IssuedAccessTokenVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.KeyDescriptorVO;

/**
 * 类型 `JwtTokenService` 位于当前包内，是类型，用于承载 `Jwt Token Service` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `JwtTokenService` is a type in its package and carries the responsibility, state, or contract for `Jwt Token Service`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Issues small reference JWTs. Authorization facts remain in the runtime projection.
 */
public final class JwtTokenService {

    /**
     * 字段 `jwtEncoder` 表示 `JwtTokenService` 中与 `jwt Encoder` 相关的状态、依赖、配置或结果（声明类型 `JwtEncoder`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `jwtEncoder` stores the `jwt Encoder`-related state, dependency, configuration, or result of `JwtTokenService` (declared type `JwtEncoder`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `jwtEncoder` 时应保持 `JwtTokenService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `jwtEncoder`, preserve `JwtTokenService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final JwtEncoder jwtEncoder;
    /**
     * 字段 `keyRing` 表示 `JwtTokenService` 中与 `key Ring` 相关的状态、依赖、配置或结果（声明类型 `JwtKeyRingService`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `keyRing` stores the `key Ring`-related state, dependency, configuration, or result of `JwtTokenService` (declared type `JwtKeyRingService`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `keyRing` 时应保持 `JwtTokenService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `keyRing`, preserve `JwtTokenService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final JwtKeyRingService keyRing;
    /**
     * 字段 `idGenerator` 表示 `JwtTokenService` 中与 `id Generator` 相关的状态、依赖、配置或结果（声明类型 `LongIdGenerator`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `idGenerator` stores the `id Generator`-related state, dependency, configuration, or result of `JwtTokenService` (declared type `LongIdGenerator`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `idGenerator` 时应保持 `JwtTokenService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `idGenerator`, preserve `JwtTokenService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final LongIdGenerator idGenerator;
    /**
     * 字段 `issuer` 表示 `JwtTokenService` 中与 `issuer` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `issuer` stores the `issuer`-related state, dependency, configuration, or result of `JwtTokenService` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `issuer` 时应保持 `JwtTokenService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `issuer`, preserve `JwtTokenService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final String issuer;
    /**
     * 字段 `audiences` 表示 `JwtTokenService` 中与 `audiences` 相关的状态、依赖、配置或结果（声明类型 `List&lt;String&gt;`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `audiences` stores the `audiences`-related state, dependency, configuration, or result of `JwtTokenService` (declared type `List&lt;String&gt;`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `audiences` 时应保持 `JwtTokenService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `audiences`, preserve `JwtTokenService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final List<String> audiences;
    /**
     * 字段 `runtimePolicy` 表示 `JwtTokenService` 中与 `runtime Policy` 相关的状态、依赖、配置或结果（声明类型 `Rbac3RuntimePolicy`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `runtimePolicy` stores the `runtime Policy`-related state, dependency, configuration, or result of `JwtTokenService` (declared type `Rbac3RuntimePolicy`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `runtimePolicy` 时应保持 `JwtTokenService` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `runtimePolicy`, preserve `JwtTokenService`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final Rbac3RuntimePolicy runtimePolicy;

    /**
     * 构造器 `JwtTokenService` 用于创建并初始化 `JwtTokenService` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `JwtTokenService` creates and initializes `JwtTokenService`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `JwtTokenService` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `JwtTokenService`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param jwtEncoder 输入参数 `jwtEncoder`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param keyRing 输入参数 `keyRing`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param idGenerator 输入参数 `idGenerator`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param issuer 输入参数 `issuer`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param audiences 输入参数 `audiences`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param runtimePolicy 输入参数 `runtimePolicy`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public JwtTokenService(
            JwtEncoder jwtEncoder,
            JwtKeyRingService keyRing,
            LongIdGenerator idGenerator,
            String issuer,
            List<String> audiences,
            Rbac3RuntimePolicy runtimePolicy) {
        this.jwtEncoder = Objects.requireNonNull(jwtEncoder, "jwtEncoder");
        this.keyRing = Objects.requireNonNull(keyRing, "keyRing");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.issuer = required(issuer, "issuer");
        this.audiences = List.copyOf(audiences);
        if (this.audiences.isEmpty()) {
            throw new IllegalArgumentException("at least one audience is required");
        }
        this.runtimePolicy = Objects.requireNonNull(runtimePolicy, "runtimePolicy");
    }

    /**
     * 方法 `issue` 按照 `JwtTokenService` 的职责处理输入，完成 `issue` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `issue` processes its inputs according to `JwtTokenService`'s responsibility, performs the `issue` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `issue` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `issue`, then continue the business flow using its result, exception, or side effect.
     *
     * @param subject 输入参数 `subject`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param now 输入参数 `now`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    public IssuedAccessTokenVO issue(AccessTokenSubjectVO subject, Instant now) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(now, "now");
        Rbac3RuntimePolicy.Snapshot policySnapshot = runtimePolicy.current();
        KeyDescriptorVO signingKey = keyRing.signingKey();
        Instant expiresAt = now.plus(policySnapshot.accessTokenTtl());
        Rbac3TokenClaims claims = new Rbac3TokenClaims(
                issuer,
                audiences,
                subject.userId(),
                subject.tenantId(),
                subject.sessionId(),
                subject.authVersion(),
                subject.sessionVersion(),
                subject.policyVersion(),
                idGenerator.nextId(),
                now,
                now,
                expiresAt,
                signingKey.kid());
        JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(signingKey.kid())
                .build();
        JwtClaimsSet claimSet = JwtClaimsSet.builder()
                .issuer(claims.iss())
                .audience(claims.aud())
                .subject(claims.sub())
                .claim("tid", claims.tid())
                .claim("sid", claims.sid())
                .claim("av", claims.av())
                .claim("sv", claims.sv())
                .claim("pv", claims.pv())
                .id(claims.jti())
                .issuedAt(claims.iat())
                .notBefore(claims.nbf())
                .expiresAt(claims.exp())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(headers, claimSet))
                .getTokenValue();
        return new IssuedAccessTokenVO(token, expiresAt, claims);
    }



    /**
     * 方法 `required` 按照 `JwtTokenService` 的职责处理输入，完成 `required` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `required` processes its inputs according to `JwtTokenService`'s responsibility, performs the `required` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
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
