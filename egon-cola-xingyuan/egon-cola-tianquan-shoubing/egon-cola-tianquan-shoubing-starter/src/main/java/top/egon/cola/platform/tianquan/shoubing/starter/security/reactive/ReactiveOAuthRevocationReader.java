package top.egon.cola.platform.tianquan.shoubing.starter.security.reactive;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.api.RScript;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Mono;
import top.egon.cola.platform.tianquan.shoubing.starter.autoconfigure.IdpStarterProperties;
import top.egon.cola.platform.tianquan.shoubing.starter.security.OAuthRevocationReader;
import top.egon.cola.platform.tianquan.shoubing.starter.security.TrustedIssuerRegistry;
import top.egon.cola.platform.tianquan.shoubing.starter.security.domain.VerifiedJwtBO;
import top.egon.cola.platform.tianquan.shoubing.starter.security.impl.RedisOAuthRevocationReader;

/** Non-blocking same-slot OAuth revocation lookup for WebFlux and Reactor security chains. */
@Slf4j
@RequiredArgsConstructor
public final class ReactiveOAuthRevocationReader {

    @Qualifier("rbac3RuntimeRedissonClient")
    private final RedissonReactiveClient redisson;
    @Qualifier("trustedIssuerRegistry")
    private final TrustedIssuerRegistry trustedIssuerRegistry;
    @Qualifier("idpStarterProperties")
    private final IdpStarterProperties properties;

    public Mono<OAuthRevocationReader.Result> check(VerifiedJwtBO verifiedJwt) {
        VerifiedJwtBO token = Objects.requireNonNull(verifiedJwt, "verifiedJwt");
        try {
            token.validate();
        } catch (RuntimeException invalid) {
            return Mono.just(OAuthRevocationReader.Result.revoked("JWT_SECURITY_CONTEXT_INVALID"));
        }
        TrustedIssuerRegistry.TrustedIssuerVO issuer = token.getTrustedIssuer();
        TrustedIssuerRegistry.TrustedIssuerVO current = trustedIssuerRegistry
                .findByIssuer(issuer.issuer().toASCIIString())
                .orElse(null);
        if (current == null || current.trustVersion() != issuer.trustVersion()) {
            return Mono.just(OAuthRevocationReader.Result.revoked("JWT_ISSUER_NOT_ACTIVE"));
        }
        String issuerHash = TrustedIssuerRegistry.issuerHash(issuer.issuer());
        List<Object> keys = RedisOAuthRevocationReader.redisKeys(token, issuerHash);
        List<Object> arguments = RedisOAuthRevocationReader.redisArguments(
                token, properties.getRevocation().getEpoch());
        return redisson.getScript(StringCodec.INSTANCE)
                .<List<Object>>eval(
                        RScript.Mode.READ_ONLY,
                        RedisOAuthRevocationReader.READ_SECURITY_SNAPSHOT_SCRIPT,
                        RScript.ReturnType.MULTI,
                        keys,
                        arguments.toArray())
                .timeout(properties.getRevocation().getTimeout())
                .map(RedisOAuthRevocationReader::interpret)
                .onErrorResume(failure -> {
                    log.warn("Reactive OAuth security read is unavailable; issuerHash={}", issuerHash);
                    return Mono.just(OAuthRevocationReader.Result.unavailable(
                            "OAUTH_SECURITY_STATE_UNAVAILABLE"));
                });
    }
}
