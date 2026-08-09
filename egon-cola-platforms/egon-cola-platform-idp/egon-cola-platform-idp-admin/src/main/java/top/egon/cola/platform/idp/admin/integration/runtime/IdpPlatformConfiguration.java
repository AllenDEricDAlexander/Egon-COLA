package top.egon.cola.platform.idp.admin.integration.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.ddc.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.configuration.runtime.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.configuration.runtime.DdcRuntimeState;
import top.egon.cola.component.gateway.provider.GatewayHttpProviderProperties;
import top.egon.cola.component.gateway.provider.HttpProviderLeaseRuntime;
import top.egon.cola.platform.idp.admin.audit.infrastructure.IdentityAuditLogRepository;
import top.egon.cola.platform.idp.admin.bootstrap.IdpBootstrapRunner;
import top.egon.cola.platform.idp.admin.bootstrap.IdpBootstrapService;
import top.egon.cola.platform.idp.admin.integration.ddc.IdpRuntimePolicy;
import top.egon.cola.platform.idp.admin.integration.outbox.IdentityOutboxPublisher;
import top.egon.cola.platform.idp.admin.identity.application.IdentityUserStateReconciler;
import top.egon.cola.platform.idp.admin.outbox.infrastructure.IdentityOutboxEventRepository;
import top.egon.cola.platform.idp.admin.token.application.SigningKeyRuntime;
import top.egon.cola.platform.idp.admin.token.infrastructure.ExternalPemSigningKeyRuntime;
import top.egon.cola.platform.idp.admin.token.infrastructure.Rs256TokenService;
import top.egon.cola.platform.idp.core.identity.IdentityFacade;
import top.egon.cola.platform.idp.core.identity.UsernameNormalizer;
import top.egon.cola.platform.idp.core.port.IdentityUserStatePort;
import top.egon.cola.platform.idp.core.port.IdentityUserStore;
import top.egon.cola.platform.idp.core.port.PasswordCredentialStore;
import top.egon.cola.platform.idp.core.port.PasswordHashPort;
import top.egon.cola.platform.idp.core.port.RefreshTokenStore;
import top.egon.cola.platform.idp.core.audit.IdentitySecurityEventPort;

import java.time.Clock;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class IdpPlatformConfiguration {

    @Bean
    IdentityOutboxPublisher identityOutboxPublisher(
            IdentityOutboxEventRepository outbox,
            IdentityAuditLogRepository audits,
            RefreshTokenStore refreshTokens,
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            ObjectMapper objectMapper,
            LongIdGenerator ids,
            @Qualifier("idpClock") Clock idpClock,
            @Value("${egon.idp.identity-state-key-prefix:identity:v1:user:}")
            String stateKeyPrefix
    ) {
        return new IdentityOutboxPublisher(
                outbox,
                audits,
                refreshTokens,
                redisson,
                objectMapper,
                ids::nextId,
                stateKeyPrefix,
                idpClock
        );
    }

    @Bean
    IdentityFacade identityFacade(
            IdentityUserStore users,
            PasswordCredentialStore credentials,
            PasswordHashPort passwordHashes,
            IdentityUserStatePort states,
            IdentitySecurityEventPort securityEvents,
            IdpRuntimePolicy runtimePolicy
    ) {
        return IdentityFacade.dynamicPolicy(
                users,
                credentials,
                passwordHashes,
                states,
                securityEvents,
                new UsernameNormalizer(),
                () -> runtimePolicy.current().maximumLoginFailures(),
                () -> runtimePolicy.current().loginLockDuration()
        );
    }

    @Bean
    JwtDecoder idpJwtDecoder(Rs256TokenService tokens) {
        return tokens.jwtDecoder();
    }

    @Bean
    SigningKeyRuntime signingKeyRuntime(
            @Value("${egon.idp.oauth.signing-key.kid}") String configuredKid
    ) {
        return new ExternalPemSigningKeyRuntime(configuredKid);
    }

    @Bean
    ApplicationRunner idpBootstrapApplicationRunner(
            IdpBootstrapService bootstrap,
            IdentityUserStateReconciler stateReconciler
    ) {
        IdpBootstrapRunner runner = new IdpBootstrapRunner(bootstrap);
        return (ApplicationArguments arguments) -> {
            runner.run(
                    arguments.getSourceArgs(),
                    Map.copyOf(System.getenv())
            );
            stateReconciler.reconcile();
        };
    }

    @Bean
    IdpRuntimeReadiness idpRuntimeReadiness(
            ObjectProvider<DdcRuntimeCoordinator> coordinators,
            IdentityOutboxEventRepository outbox,
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient redisson,
            Rs256TokenService tokenService
    ) {
        return () -> {
            DdcRuntimeCoordinator coordinator = coordinators.getIfAvailable();
            boolean ddcReady = coordinator != null
                    && coordinator.state() == DdcRuntimeState.READY
                    && coordinator.currentSession()
                    .filter(session -> session.role()
                            == DdcLeaseRole.CONFIG_CLIENT)
                    .isPresent();
            boolean oauthReady = !tokenService.jwkSet().isEmpty();
            boolean outboxReady = outbox != null
                    && !redisson.isShutdown()
                    && !redisson.isShuttingDown();
            return new IdpHttpProviderPublicationGate.ReadinessStatus(
                    ddcReady,
                    oauthReady,
                    outboxReady
            );
        };
    }

    @Bean(name = "gatewayHttpProviderServerReadyListener")
    @ConditionalOnProperty(
            prefix = "egon.cola.component.gateway.provider.http",
            name = "enabled",
            havingValue = "true"
    )
    IdpHttpProviderPublicationGate idpHttpProviderPublicationGate(
            DdcRuntimeCoordinator coordinator,
            HttpProviderLeaseRuntime providerRuntime,
            GatewayHttpProviderProperties providerProperties,
            IdpRuntimeReadiness readiness
    ) {
        return new IdpHttpProviderPublicationGate(
                coordinator,
                providerRuntime,
                providerProperties,
                readiness
        );
    }
}
