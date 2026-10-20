package top.egon.cola.platform.tianquan.shoubing.admin.support.runtime;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.service.impl.OAuthManagementRealmResolver;
import top.egon.cola.platform.tianquan.shoubing.admin.oauth.repository.OAuthRealmRepository;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import top.egon.cola.platform.tianquan.shoubing.admin.token.service.impl.OAuthSecurityProjectionComponent;
import top.egon.cola.component.outbox.store.OutboxStore;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@Profile("!oauth-migration")
public class IdpPlatformConfiguration {

    /** Selects the host connection for Common cache when identity and Tianshu clients coexist. */
    @Bean(name = "redissonClient", destroyMethod = "")
    @ConditionalOnMissingBean(name = "redissonClient")
    @ConditionalOnProperty(prefix = "egon.cola.component.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    RedissonClient applicationCacheRedissonClient(@Qualifier("rbac3RuntimeRedissonClient") RedissonClient client) {
        return client;
    }

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
            @Qualifier("oauthManagementRealmResolver") OAuthManagementRealmResolver realms,
            @Qualifier("oAuthRealmRepository") OAuthRealmRepository realmRepository,
            @Qualifier("tenantJwkSourceComponent") JWKSource<SecurityContext> jwkSource,
            @Qualifier("authorizationServerSettings") AuthorizationServerSettings settings,
            @Qualifier("tianquanSecurityAuditReady") ObjectProvider<java.util.function.BooleanSupplier> auditReady
    ) {
        return () -> {
            DdcRuntimeCoordinator coordinator = coordinators.getIfAvailable();
            boolean ddcReady = coordinator != null
                    && coordinator.state() == DdcRuntimeState.READY
                    && coordinator.currentSession()
                    .filter(session -> session.role()
                            == DdcLeaseRole.CONFIG_CLIENT)
                    .isPresent();
            boolean oauthReady = false;
            AuthorizationServerContext previous = AuthorizationServerContextHolder.getContext();
            try {
                var realm = realms.platformRealm();
                var active = realmRepository.findByIssuer(realm)
                        .filter(row -> "ACTIVE".equals(row.getStatus()) && row.getActiveKid() != null);
                if (active.isPresent()) {
                    AuthorizationServerContextHolder.setContext(new AuthorizationServerContext() {
                        @Override
                        public String getIssuer() {
                            return realm.getIssuer().toASCIIString();
                        }
                        @Override
                        public AuthorizationServerSettings getAuthorizationServerSettings() {
                            return settings;
                        }
                    });
                    oauthReady = !jwkSource.get(new JWKSelector(new JWKMatcher.Builder()
                            .keyType(KeyType.RSA).privateOnly(true)
                            .keyID(active.get().getActiveKid()).build()), null).isEmpty();
                }
            } catch (OAuth2AuthenticationException | KeySourceException notReady) {
                oauthReady = false;
            } finally {
                AuthorizationServerContextHolder.setContext(previous);
            }
            boolean outboxReady = outbox != null
                    && !redisson.isShutdown()
                    && !redisson.isShuttingDown()
                    && auditReady.getIfAvailable(() -> () -> true).getAsBoolean();
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
