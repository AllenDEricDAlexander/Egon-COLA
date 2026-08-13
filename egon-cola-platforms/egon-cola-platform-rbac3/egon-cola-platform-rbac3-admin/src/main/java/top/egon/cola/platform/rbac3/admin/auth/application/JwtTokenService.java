package top.egon.cola.platform.rbac3.admin.auth.application;

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
    public IssuedAccessToken issue(AccessTokenSubject subject, Instant now) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(now, "now");
        Rbac3RuntimePolicy.Snapshot policySnapshot = runtimePolicy.current();
        JwtKeyRingService.KeyDescriptor signingKey = keyRing.signingKey();
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
        return new IssuedAccessToken(token, expiresAt, claims);
    }

    /**
     * 类型 `AccessTokenSubject` 位于 `JwtTokenService` 内，是记录类型，用于承载 `Access Token Subject` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `AccessTokenSubject` is a record inside `JwtTokenService` and carries the responsibility, state, or contract for `Access Token Subject`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `AccessTokenSubject` 作为 `JwtTokenService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `AccessTokenSubject` as the responsibility boundary of `JwtTokenService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param tenantId 记录组件 `tenantId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `tenantId` carries constructor data whose meaning is defined by the record contract.
     * @param userId 记录组件 `userId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `userId` carries constructor data whose meaning is defined by the record contract.
     * @param sessionId 记录组件 `sessionId` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionId` carries constructor data whose meaning is defined by the record contract.
     * @param authVersion 记录组件 `authVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `authVersion` carries constructor data whose meaning is defined by the record contract.
     * @param sessionVersion 记录组件 `sessionVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `sessionVersion` carries constructor data whose meaning is defined by the record contract.
     * @param policyVersion 记录组件 `policyVersion` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `policyVersion` carries constructor data whose meaning is defined by the record contract.
     */
    public record AccessTokenSubject(
            /**
             * 字段 `tenantId` 表示 `AccessTokenSubject` 中与 `tenant Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `tenantId` stores the `tenant Id`-related state, dependency, configuration, or result of `AccessTokenSubject` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `tenantId` 时应保持 `AccessTokenSubject` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `tenantId`, preserve `AccessTokenSubject`'s lifecycle, immutability, and thread-safety constraints.
             */
            String tenantId,
            /**
             * 字段 `userId` 表示 `AccessTokenSubject` 中与 `user Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `userId` stores the `user Id`-related state, dependency, configuration, or result of `AccessTokenSubject` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `userId` 时应保持 `AccessTokenSubject` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `userId`, preserve `AccessTokenSubject`'s lifecycle, immutability, and thread-safety constraints.
             */
            String userId,
            /**
             * 字段 `sessionId` 表示 `AccessTokenSubject` 中与 `session Id` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionId` stores the `session Id`-related state, dependency, configuration, or result of `AccessTokenSubject` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionId` 时应保持 `AccessTokenSubject` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionId`, preserve `AccessTokenSubject`'s lifecycle, immutability, and thread-safety constraints.
             */
            String sessionId,
            /**
             * 字段 `authVersion` 表示 `AccessTokenSubject` 中与 `auth Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `authVersion` stores the `auth Version`-related state, dependency, configuration, or result of `AccessTokenSubject` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `authVersion` 时应保持 `AccessTokenSubject` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `authVersion`, preserve `AccessTokenSubject`'s lifecycle, immutability, and thread-safety constraints.
             */
            long authVersion,
            /**
             * 字段 `sessionVersion` 表示 `AccessTokenSubject` 中与 `session Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `sessionVersion` stores the `session Version`-related state, dependency, configuration, or result of `AccessTokenSubject` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `sessionVersion` 时应保持 `AccessTokenSubject` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `sessionVersion`, preserve `AccessTokenSubject`'s lifecycle, immutability, and thread-safety constraints.
             */
            long sessionVersion,
            /**
             * 字段 `policyVersion` 表示 `AccessTokenSubject` 中与 `policy Version` 相关的状态、依赖、配置或结果（声明类型 `long`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `policyVersion` stores the `policy Version`-related state, dependency, configuration, or result of `AccessTokenSubject` (declared type `long`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `policyVersion` 时应保持 `AccessTokenSubject` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `policyVersion`, preserve `AccessTokenSubject`'s lifecycle, immutability, and thread-safety constraints.
             */
            long policyVersion
    ) {
    }

    /**
     * 类型 `IssuedAccessToken` 位于 `JwtTokenService` 内，是记录类型，用于承载 `Issued Access Token` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `IssuedAccessToken` is a record inside `JwtTokenService` and carries the responsibility, state, or contract for `Issued Access Token`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `IssuedAccessToken` 作为 `JwtTokenService` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `IssuedAccessToken` as the responsibility boundary of `JwtTokenService`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param token 记录组件 `token` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `token` carries constructor data whose meaning is defined by the record contract.
     * @param expiresAt 记录组件 `expiresAt` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `expiresAt` carries constructor data whose meaning is defined by the record contract.
     * @param claims 记录组件 `claims` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `claims` carries constructor data whose meaning is defined by the record contract.
     */
    public record IssuedAccessToken(
            /**
             * 字段 `token` 表示 `IssuedAccessToken` 中与 `token` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `token` stores the `token`-related state, dependency, configuration, or result of `IssuedAccessToken` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `token` 时应保持 `IssuedAccessToken` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `token`, preserve `IssuedAccessToken`'s lifecycle, immutability, and thread-safety constraints.
             */
            String token,
            /**
             * 字段 `expiresAt` 表示 `IssuedAccessToken` 中与 `expires At` 相关的状态、依赖、配置或结果（声明类型 `Instant`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `expiresAt` stores the `expires At`-related state, dependency, configuration, or result of `IssuedAccessToken` (declared type `Instant`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `expiresAt` 时应保持 `IssuedAccessToken` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `expiresAt`, preserve `IssuedAccessToken`'s lifecycle, immutability, and thread-safety constraints.
             */
            Instant expiresAt,
            /**
             * 字段 `claims` 表示 `IssuedAccessToken` 中与 `claims` 相关的状态、依赖、配置或结果（声明类型 `Rbac3TokenClaims`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `claims` stores the `claims`-related state, dependency, configuration, or result of `IssuedAccessToken` (declared type `Rbac3TokenClaims`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `claims` 时应保持 `IssuedAccessToken` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `claims`, preserve `IssuedAccessToken`'s lifecycle, immutability, and thread-safety constraints.
             */
            Rbac3TokenClaims claims
    ) {

        /**
         * 方法 `toString` 按照 `IssuedAccessToken` 的职责处理输入，完成 `to String` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `toString` processes its inputs according to `IssuedAccessToken`'s responsibility, performs the `to String` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `toString` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `toString`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        @Override
        public String toString() {
            return "IssuedAccessToken[token=<redacted>, expiresAt=" + expiresAt
                    + ", claims=" + claims + ']';
        }
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
