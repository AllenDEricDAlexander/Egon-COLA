package top.egon.cola.platform.rbac3.gateway.security;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import top.egon.cola.component.gateway.core.context.GatewayPrincipal;
import top.egon.cola.component.gateway.core.security.AuthenticationDecision;
import top.egon.cola.component.gateway.core.security.GatewayAuthContext;
import top.egon.cola.component.gateway.core.security.GatewayAuthenticationProvider;
import top.egon.cola.component.gateway.core.security.GatewayCredential;
import top.egon.cola.platform.rbac3.contract.auth.Rbac3TokenClaims;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 类型 `Rbac3JwtSessionAuthenticationProvider` 位于当前包内，是类型，用于承载 `Rbac3 Jwt Session Authentication Provider` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3JwtSessionAuthenticationProvider` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Jwt Session Authentication Provider`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Verifies the JWT and all three runtime versions away from Reactor event loops.
 */
public final class Rbac3JwtSessionAuthenticationProvider
        implements GatewayAuthenticationProvider {

    /**
     * 字段 `PROVIDER_ID` 表示 `Rbac3JwtSessionAuthenticationProvider` 中与 `PROVIDER ID` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `PROVIDER_ID` stores the `PROVIDER ID`-related state, dependency, configuration, or result of `Rbac3JwtSessionAuthenticationProvider` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `PROVIDER_ID` 时应保持 `Rbac3JwtSessionAuthenticationProvider` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `PROVIDER_ID`, preserve `Rbac3JwtSessionAuthenticationProvider`'s lifecycle, immutability, and thread-safety constraints.
     */
    public static final String PROVIDER_ID = "rbac3-jwt-session";

    /**
     * 字段 `tokenVerifier` 表示 `Rbac3JwtSessionAuthenticationProvider` 中与 `token Verifier` 相关的状态、依赖、配置或结果（声明类型 `TokenVerifier`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `tokenVerifier` stores the `token Verifier`-related state, dependency, configuration, or result of `Rbac3JwtSessionAuthenticationProvider` (declared type `TokenVerifier`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `tokenVerifier` 时应保持 `Rbac3JwtSessionAuthenticationProvider` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `tokenVerifier`, preserve `Rbac3JwtSessionAuthenticationProvider`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final TokenVerifier tokenVerifier;
    /**
     * 字段 `sessionVerifier` 表示 `Rbac3JwtSessionAuthenticationProvider` 中与 `session Verifier` 相关的状态、依赖、配置或结果（声明类型 `SessionVerifier`）；其生命周期和取值含义由声明类型及所属对象共同确定。
     * Field `sessionVerifier` stores the `session Verifier`-related state, dependency, configuration, or result of `Rbac3JwtSessionAuthenticationProvider` (declared type `SessionVerifier`); its lifecycle and value semantics are defined by its declared type and owning object.
     *
     * 含义与用法：读取、传递或更新 `sessionVerifier` 时应保持 `Rbac3JwtSessionAuthenticationProvider` 的生命周期、不可变性和线程安全约束。
     * Meaning and usage: when reading, passing, or updating `sessionVerifier`, preserve `Rbac3JwtSessionAuthenticationProvider`'s lifecycle, immutability, and thread-safety constraints.
     */
    private final SessionVerifier sessionVerifier;

    /**
     * 构造器 `Rbac3JwtSessionAuthenticationProvider` 用于创建并初始化 `Rbac3JwtSessionAuthenticationProvider` 实例，建立该类型后续方法所依赖的状态和不变量。
     * Constructor `Rbac3JwtSessionAuthenticationProvider` creates and initializes `Rbac3JwtSessionAuthenticationProvider`, establishing the state and invariants required by subsequent operations.
     *
     * 用法：通过 `Rbac3JwtSessionAuthenticationProvider` 的构造入口创建实例，不绕过构造器建立的校验和初始化约束。
     * Usage: create the instance through `Rbac3JwtSessionAuthenticationProvider`'s constructor entry point and do not bypass the validation and initialization constraints established there.
     *
     * @param tokenVerifier 输入参数 `tokenVerifier`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param sessionVerifier 输入参数 `sessionVerifier`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     */
    public Rbac3JwtSessionAuthenticationProvider(
            TokenVerifier tokenVerifier,
            SessionVerifier sessionVerifier
    ) {
        this.tokenVerifier = Objects.requireNonNull(tokenVerifier, "tokenVerifier");
        this.sessionVerifier = Objects.requireNonNull(sessionVerifier, "sessionVerifier");
    }

    /**
     * 方法 `providerId` 按照 `Rbac3JwtSessionAuthenticationProvider` 的职责处理输入，完成 `provider Id` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `providerId` processes its inputs according to `Rbac3JwtSessionAuthenticationProvider`'s responsibility, performs the `provider Id` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `providerId` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `providerId`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    /**
     * 方法 `supportedCredentialTypes` 按照 `Rbac3JwtSessionAuthenticationProvider` 的职责处理输入，完成 `supported Credential Types` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `supportedCredentialTypes` processes its inputs according to `Rbac3JwtSessionAuthenticationProvider`'s responsibility, performs the `supported Credential Types` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `supportedCredentialTypes` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `supportedCredentialTypes`, then continue the business flow using its result, exception, or side effect.
     *
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public Set<String> supportedCredentialTypes() {
        return Set.of("bearer");
    }

    /**
     * 方法 `authenticate` 按照 `Rbac3JwtSessionAuthenticationProvider` 的职责处理输入，完成 `authenticate` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `authenticate` processes its inputs according to `Rbac3JwtSessionAuthenticationProvider`'s responsibility, performs the `authenticate` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `authenticate` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `authenticate`, then continue the business flow using its result, exception, or side effect.
     *
     * @param context 输入参数 `context`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param credential 输入参数 `credential`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Override
    public Publisher<AuthenticationDecision> authenticate(
            GatewayAuthContext context,
            GatewayCredential credential
    ) {
        if (!"bearer".equalsIgnoreCase(credential.type())) {
            return Mono.just(AuthenticationDecision.deny("RBAC3_CREDENTIAL_TYPE_INVALID"));
        }
        return Mono.fromCallable(() -> {
                    Rbac3TokenClaims claims = tokenVerifier.verify(
                            credential.tokenReference());
                    sessionVerifier.verify(claims);
                    return AuthenticationDecision.allow(new GatewayPrincipal(
                            claims.sub(), "USER", claims.tid(), null, true,
                            Map.of(
                                    "rbac3.session-id", claims.sid(),
                                    "rbac3.auth-version", Long.toString(claims.av()),
                                    "rbac3.session-version", Long.toString(claims.sv()),
                                    "rbac3.policy-version", Long.toString(claims.pv())
                            )));
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(AuthenticationDecision.deny("RBAC3_AUTHENTICATION_FAILED"));
    }

    /**
     * 类型 `TokenVerifier` 位于 `Rbac3JwtSessionAuthenticationProvider` 内，是接口，用于承载 `Token Verifier` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `TokenVerifier` is an interface inside `Rbac3JwtSessionAuthenticationProvider` and carries the responsibility, state, or contract for `Token Verifier`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `TokenVerifier` 作为 `Rbac3JwtSessionAuthenticationProvider` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `TokenVerifier` as the responsibility boundary of `Rbac3JwtSessionAuthenticationProvider`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface TokenVerifier {
        /**
         * 方法 `verify` 按照 `TokenVerifier` 的职责处理输入，完成 `verify` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `verify` processes its inputs according to `TokenVerifier`'s responsibility, performs the `verify` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `verify` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `verify`, then continue the business flow using its result, exception, or side effect.
         *
         * @param token 输入参数 `token`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        Rbac3TokenClaims verify(String token);
    }

    /**
     * 类型 `SessionVerifier` 位于 `Rbac3JwtSessionAuthenticationProvider` 内，是接口，用于承载 `Session Verifier` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `SessionVerifier` is an interface inside `Rbac3JwtSessionAuthenticationProvider` and carries the responsibility, state, or contract for `Session Verifier`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `SessionVerifier` 作为 `Rbac3JwtSessionAuthenticationProvider` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `SessionVerifier` as the responsibility boundary of `Rbac3JwtSessionAuthenticationProvider`, following its existing construction, interface, or Spring-assembly mechanism.
     */
    @FunctionalInterface
    public interface SessionVerifier {
        /**
         * 方法 `verify` 按照 `SessionVerifier` 的职责处理输入，完成 `verify` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `verify` processes its inputs according to `SessionVerifier`'s responsibility, performs the `verify` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `verify` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `verify`, then continue the business flow using its result, exception, or side effect.
         *
         * @param claims 输入参数 `claims`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
         */
        void verify(Rbac3TokenClaims claims);
    }
}
