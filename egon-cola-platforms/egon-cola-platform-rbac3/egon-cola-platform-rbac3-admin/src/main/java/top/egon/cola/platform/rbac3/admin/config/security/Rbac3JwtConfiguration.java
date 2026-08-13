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
import top.egon.cola.platform.rbac3.admin.config.security.Rbac3RsaKeyMaterial;
import top.egon.cola.platform.rbac3.admin.auth.domain.vo.KeyDescriptorVO;
import top.egon.cola.platform.rbac3.admin.auth.domain.enums.JwtKeyRingKeyStateEnum;

/**
 * 类型 `Rbac3JwtConfiguration` 位于当前包内，是类型，用于承载 `Rbac3 Jwt Configuration` 相关的职责、状态或契约；调用方通常通过其公开 API、Spring 装配或实现关系使用。
 * Type `Rbac3JwtConfiguration` is a type in its package and carries the responsibility, state, or contract for `Rbac3 Jwt Configuration`; callers normally use it through its public API, Spring assembly, or implementation relationship.
 * Loads deployment-owned RSA material and enforces issuer, Resource URI and RS256.
 */
@Configuration(proxyBeanMethods = false)
public class Rbac3JwtConfiguration {

    /**
     * 方法 `rbac3RsaKeyMaterial` 按照 `Rbac3JwtConfiguration` 的职责处理输入，完成 `rbac3 Rsa Key Material` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3RsaKeyMaterial` processes its inputs according to `Rbac3JwtConfiguration`'s responsibility, performs the `rbac3 Rsa Key Material` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3RsaKeyMaterial` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3RsaKeyMaterial`, then continue the business flow using its result, exception, or side effect.
     *
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    Rbac3RsaKeyMaterial rbac3RsaKeyMaterial(Rbac3SecurityProperties properties) {
        try (InputStream privateInput = new FileSystemResource(
                properties.requirePrivateKeyFile()).getInputStream();
             InputStream publicInput = new FileSystemResource(
                     properties.requirePublicKeyFile()).getInputStream()) {
            return new Rbac3RsaKeyMaterial(
                    (RSAPublicKey) RsaKeyConverters.x509().convert(publicInput),
                    (RSAPrivateKey) RsaKeyConverters.pkcs8().convert(privateInput),
                    properties.requireKid());
        } catch (IOException error) {
            throw new IllegalStateException("cannot read RBAC3 RSA key material", error);
        }
    }

    /**
     * 方法 `jwtEncoder` 按照 `Rbac3JwtConfiguration` 的职责处理输入，完成 `jwt Encoder` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `jwtEncoder` processes its inputs according to `Rbac3JwtConfiguration`'s responsibility, performs the `jwt Encoder` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `jwtEncoder` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `jwtEncoder`, then continue the business flow using its result, exception, or side effect.
     *
     * @param material 输入参数 `material`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    JwtEncoder jwtEncoder(Rbac3RsaKeyMaterial material) {
        RSAKey key = material.rsaKey();
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(
                new com.nimbusds.jose.jwk.JWKSet(key));
        return new NimbusJwtEncoder(source);
    }

    /**
     * 方法 `jwtDecoder` 按照 `Rbac3JwtConfiguration` 的职责处理输入，完成 `jwt Decoder` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `jwtDecoder` processes its inputs according to `Rbac3JwtConfiguration`'s responsibility, performs the `jwt Decoder` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `jwtDecoder` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `jwtDecoder`, then continue the business flow using its result, exception, or side effect.
     *
     * @param material 输入参数 `material`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    JwtDecoder jwtDecoder(
            Rbac3RsaKeyMaterial material,
            Rbac3SecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(material.publicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        OAuth2TokenValidator<Jwt> resource = token -> token.getAudience().stream()
                .anyMatch(properties.requireResourceUris()::contains)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "JWT audience is not accepted", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.requireIssuer()),
                resource));
        return decoder;
    }

    /**
     * 方法 `jwtKeyRingService` 按照 `Rbac3JwtConfiguration` 的职责处理输入，完成 `jwt Key Ring Service` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `jwtKeyRingService` processes its inputs according to `Rbac3JwtConfiguration`'s responsibility, performs the `jwt Key Ring Service` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `jwtKeyRingService` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `jwtKeyRingService`, then continue the business flow using its result, exception, or side effect.
     *
     * @param material 输入参数 `material`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param properties 输入参数 `properties`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @param clock 输入参数 `clock`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    JwtKeyRingService jwtKeyRingService(
            Rbac3RsaKeyMaterial material,
            Rbac3SecurityProperties properties,
            Clock clock) {
        Map<String, Object> publicJwk = material.rsaKey().toPublicJWK().toJSONObject();
        return new JwtKeyRingService(List.of(new KeyDescriptorVO(
                material.kid(), "RS256", publicJwk,
                JwtKeyRingKeyStateEnum.SIGNING, clock.instant(), null)),
                properties.requireVerificationKeyRetention());
    }

    /**
     * 方法 `rbac3JwtAuthenticationConverter` 按照 `Rbac3JwtConfiguration` 的职责处理输入，完成 `rbac3 Jwt Authentication Converter` 操作并返回结果或产生声明的副作用；调用方应遵守参数和异常契约。
     * Method `rbac3JwtAuthenticationConverter` processes its inputs according to `Rbac3JwtConfiguration`'s responsibility, performs the `rbac3 Jwt Authentication Converter` operation, and returns a result or declared side effect; callers must follow its parameter and exception contract.
     *
     * 用法：调用 `rbac3JwtAuthenticationConverter` 前准备符合契约的参数，并根据返回值、异常或副作用继续业务流程。
     * Usage: provide contract-compliant arguments before calling `rbac3JwtAuthenticationConverter`, then continue the business flow using its result, exception, or side effect.
     *
     * @param runtimeStore 输入参数 `runtimeStore`，用于确定本次操作的范围或内容；input value used to determine the operation's scope or content.
     * @return 操作产生的结果，其具体语义由返回类型和所属 API 定义；the result of the operation, whose exact semantics are defined by the return type and owning API.
     */
    @Bean
    Rbac3JwtAuthenticationConverter rbac3JwtAuthenticationConverter(
            RedisAuthorizationRuntimeRepository runtimeStore) {
        return new Rbac3JwtAuthenticationConverter(runtimeStore);
    }

    }
