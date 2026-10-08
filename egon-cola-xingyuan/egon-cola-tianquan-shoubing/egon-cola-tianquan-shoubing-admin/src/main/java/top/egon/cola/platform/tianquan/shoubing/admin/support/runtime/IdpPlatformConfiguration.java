package top.egon.cola.platform.tianquan.shoubing.admin.support.runtime;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import top.egon.cola.component.tianshu.model.lease.DdcLeaseRole;
import top.egon.cola.component.tianshu.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.tianshu.model.instance.DdcRuntimeState;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationProperties;
import top.egon.cola.component.tianshu.http.registration.DdcHttpRegistrationRuntime;
import top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap.IdpBootstrapRunner;
import top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap.IdpDevelopmentTenantBootstrap;
import top.egon.cola.platform.tianquan.shoubing.admin.support.bootstrap.IdpBootstrapService;
import top.egon.cola.platform.tianquan.shoubing.admin.identity.service.IdentityUserStateService;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.Rs256TokenService;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSecurityProjectionComponent;
import top.egon.cola.component.outbox.store.OutboxStore;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class IdpPlatformConfiguration {

    @Bean
    ApplicationRunner idpBootstrapApplicationRunner(
            IdpBootstrapService bootstrap,
            IdentityUserStateService stateService,
            OAuthSecurityProjectionComponent securityProjection,
            ObjectProvider<IdpDevelopmentTenantBootstrap> developmentTenants
    ) {
        IdpBootstrapRunner runner = new IdpBootstrapRunner(bootstrap);
        return (ApplicationArguments arguments) -> {
            securityProjection.recoverRegisteredRealms();
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
            @Qualifier("outboxStore") OutboxStore outbox,
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
            prefix = "egon.cola.component.tianshu.registry.http",
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
