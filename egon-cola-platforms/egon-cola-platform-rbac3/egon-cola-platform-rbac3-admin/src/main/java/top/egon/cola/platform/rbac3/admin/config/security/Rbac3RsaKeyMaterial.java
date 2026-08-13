package top.egon.cola.platform.rbac3.admin.config.security;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import top.egon.cola.platform.rbac3.admin.auth.service.JwtKeyRingService;
import top.egon.cola.platform.rbac3.admin.config.properties.Rbac3SecurityProperties;
import top.egon.cola.platform.rbac3.admin.runtime.repository.redis.RedisAuthorizationRuntimeRepository;
import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
     * 类型 `Rbac3RsaKeyMaterial` 位于 `Rbac3JwtConfiguration` 内，是记录类型，用于承载 `Rbac3 Rsa Key Material` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
     * Type `Rbac3RsaKeyMaterial` is a record inside `Rbac3JwtConfiguration` and carries the responsibility, state, or contract for `Rbac3 Rsa Key Material`; callers normally use it through its public API, Spring assembly, or implementation relationship.
     *
     * 语义与用法：将 `Rbac3RsaKeyMaterial` 作为 `Rbac3JwtConfiguration` 的职责边界使用，优先依赖其已有构造、接口或 Spring 装配方式。
     * Semantics and usage: use `Rbac3RsaKeyMaterial` as the responsibility boundary of `Rbac3JwtConfiguration`, following its existing construction, interface, or Spring-assembly mechanism.
     *
     * @param publicKey 记录组件 `publicKey` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `publicKey` carries constructor data whose meaning is defined by the record contract.
     * @param privateKey 记录组件 `privateKey` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `privateKey` carries constructor data whose meaning is defined by the record contract.
     * @param kid 记录组件 `kid` 表示构造该记录时传入的业务数据，其取值含义由所属记录的契约定义；record component `kid` carries constructor data whose meaning is defined by the record contract.
     */
    public record Rbac3RsaKeyMaterial(
            /**
             * 字段 `publicKey` 表示 `Rbac3RsaKeyMaterial` 中与 `public Key` 相关的状态、依赖、配置或结果（声明类型 `RSAPublicKey`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `publicKey` stores the `public Key`-related state, dependency, configuration, or result of `Rbac3RsaKeyMaterial` (declared type `RSAPublicKey`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `publicKey` 时应保持 `Rbac3RsaKeyMaterial` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `publicKey`, preserve `Rbac3RsaKeyMaterial`'s lifecycle, immutability, and thread-safety constraints.
             */
            RSAPublicKey publicKey,
            /**
             * 字段 `privateKey` 表示 `Rbac3RsaKeyMaterial` 中与 `private Key` 相关的状态、依赖、配置或结果（声明类型 `RSAPrivateKey`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `privateKey` stores the `private Key`-related state, dependency, configuration, or result of `Rbac3RsaKeyMaterial` (declared type `RSAPrivateKey`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `privateKey` 时应保持 `Rbac3RsaKeyMaterial` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `privateKey`, preserve `Rbac3RsaKeyMaterial`'s lifecycle, immutability, and thread-safety constraints.
             */
            RSAPrivateKey privateKey,
            /**
             * 字段 `kid` 表示 `Rbac3RsaKeyMaterial` 中与 `kid` 相关的状态、依赖、配置或结果（声明类型 `String`）；其生命周期和取值含义由声明类型及所属对象共同确定。
             * Field `kid` stores the `kid`-related state, dependency, configuration, or result of `Rbac3RsaKeyMaterial` (declared type `String`); its lifecycle and value semantics are defined by its declared type and owning object.
             *
             * 含义与用法：读取、传递或更新 `kid` 时应保持 `Rbac3RsaKeyMaterial` 的生命周期、不可变性和线程安全约束。
             * Meaning and usage: when reading, passing, or updating `kid`, preserve `Rbac3RsaKeyMaterial`'s lifecycle, immutability, and thread-safety constraints.
             */
            String kid) {

        /**
         * 方法 `rsaKey` 按照 `Rbac3RsaKeyMaterial` 的职责处理输入，完成 `rsa Key` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
         * Method `rsaKey` processes its inputs according to `Rbac3RsaKeyMaterial`'s responsibility, performs the `rsa Key` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
         *
         * 用法：调用 `rsaKey` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
         * Usage: provide contract-compliant arguments before calling `rsaKey`, then continue the business flow using its result, exception, or side effect.
         *
         * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
         */
        RSAKey rsaKey() {
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(kid)
                    .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                    .build();
        }
    }
