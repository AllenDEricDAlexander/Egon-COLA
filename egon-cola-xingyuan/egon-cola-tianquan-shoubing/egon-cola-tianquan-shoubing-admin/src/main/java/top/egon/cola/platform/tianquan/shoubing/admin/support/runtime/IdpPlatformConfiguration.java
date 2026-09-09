package top.egon.cola.platform.tianquan.shoubing.admin.support.runtime;

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
import top.egon.cola.component.common.id.generator.LongIdGenerator;
import top.egon.cola.component.tianshu.model.lease.DdcLeaseRole;
import top.egon.cola.component.tianshu.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.tianshu.model.instance.DdcRuntimeState;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationProperties;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationRuntime;
import top.egon.cola.platform.tianquan.shoubing.admin.audit.repo.IdentityAuditLogRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap.IdpBootstrapRunner;
import top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap.IdpDevelopmentTenantBootstrap;
import top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap.IdpBootstrapService;
import top.egon.cola.platform.tianquan.shoubing.admin.support.outbox.service.IdentityOutboxPublisher;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.service.IdentityUserStateService;
import top.egon.cola.platform.tianquan.shoubing.admin.support.outbox.repo.IdentityOutboxEventRepository;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.Rs256TokenService;
import top.egon.cola.platform.tianquan.shoubing.core.port.RefreshTokenStore;

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
    ApplicationRunner idpBootstrapApplicationRunner(
            IdpBootstrapService bootstrap,
            IdentityUserStateService stateService,
            ObjectProvider<IdpDevelopmentTenantBootstrap> developmentTenants
    ) {
        IdpBootstrapRunner runner = new IdpBootstrapRunner(bootstrap);
        return (ApplicationArguments arguments) -> {
            runner.run(
                    arguments.getSourceArgs(),
                    Map.copyOf(System.getenv())
            );
            developmentTenants.ifAvailable(IdpDevelopmentTenantBootstrap::initializeAdministratorMemberships);
            stateService.reconcile();
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

    @Bean(name = "ddcHttpRegistrationServerReadyListener")
    @ConditionalOnProperty(
            prefix = "egon.cola.component.ddc.registry.http",
            name = "enabled",
            havingValue = "true"
    )
    IdpHttpProviderPublicationGate idpHttpProviderPublicationGate(
            DdcRuntimeCoordinator coordinator,
            DdcHttpRegistrationRuntime providerRuntime,
            DdcHttpRegistrationProperties providerProperties,
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
