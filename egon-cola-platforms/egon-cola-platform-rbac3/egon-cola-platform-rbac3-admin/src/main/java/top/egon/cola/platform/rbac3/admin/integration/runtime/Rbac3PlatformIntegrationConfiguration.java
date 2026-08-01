package top.egon.cola.platform.rbac3.admin.integration.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import top.egon.cola.component.gateway.provider.HttpProviderLeaseRuntime;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.reporting.GatewayReportingState;
import top.egon.cola.component.outbox.api.TransactionalOutbox;
import top.egon.cola.platform.rbac3.admin.application.port.AuthorizationEventPort;
import top.egon.cola.platform.rbac3.admin.integration.ddc.DdcProviderLeaseStatusService;
import top.egon.cola.platform.rbac3.admin.integration.flyway.Rbac3FlywayConfiguration;
import top.egon.cola.platform.rbac3.admin.integration.gateway.GatewayAdminControlPlaneStatusClient;
import top.egon.cola.platform.rbac3.admin.integration.gateway.GatewayAdminStatusCredentialProvider;
import top.egon.cola.platform.rbac3.admin.integration.gateway.GatewayDefinitionStatusService;
import top.egon.cola.platform.rbac3.admin.integration.outbox.TransactionalOutboxAuthorizationEventAdapter;
import top.egon.cola.platform.rbac3.admin.runtime.application.ControlPlaneRuntimeStatusPort;

import java.time.Clock;
import java.util.List;

/**
 * Wires component-owned Gateway, DDC and Outbox runtimes into RBAC3 ports.
 */
@Configuration(proxyBeanMethods = false)
public class Rbac3PlatformIntegrationConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock rbac3Clock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean(AuthorizationEventPort.class)
    AuthorizationEventPort authorizationEventPort(
            TransactionalOutbox outbox,
            Clock clock) {
        return new TransactionalOutboxAuthorizationEventAdapter(outbox, clock);
    }

    @Bean
    @ConditionalOnProperty(prefix = "egon.cola.component.gateway.reporting",
            name = "enabled", havingValue = "true")
    GatewayDdcRuntimeStatusService.ServiceIdentity rbac3ServiceIdentity(
            GatewayReportingProperties properties) {
        return new GatewayDdcRuntimeStatusService.ServiceIdentity(
                properties.getBizCode(), properties.getApplicationCode(),
                properties.getEnv(), properties.getNamespace(),
                "HTTP_PROVIDER", "http", properties.getApplicationCode(),
                "default", properties.getArtifactVersion());
    }

    @Bean
    @ConditionalOnProperty(prefix = "egon.cola.component.gateway.reporting",
            name = "enabled", havingValue = "true")
    GatewayAdminStatusCredentialProvider gatewayAdminStatusCredentialProvider(
            Rbac3GatewayStatusProperties properties,
            ObjectMapper objectMapper,
            Clock clock) {
        return GatewayAdminStatusCredentialProvider.rotatingFile(
                properties.requireOauthTokenFile(), objectMapper, clock);
    }

    @Bean
    @ConditionalOnProperty(prefix = "egon.cola.component.gateway.reporting",
            name = "enabled", havingValue = "true")
    GatewayAdminControlPlaneStatusClient gatewayAdminControlPlaneStatusClient(
            Rbac3GatewayStatusProperties properties,
            GatewayDdcRuntimeStatusService.ServiceIdentity identity,
            GatewayAdminStatusCredentialProvider credentials,
            ObjectMapper objectMapper,
            Clock clock) {
        return new GatewayAdminControlPlaneStatusClient(
                properties.requireAdminBaseUrl(),
                properties.requireGatewayGroupId(),
                properties.requireReleaseId(),
                new GatewayAdminControlPlaneStatusClient.ServiceKey(
                        identity.bizCode(), identity.appCode(),
                        identity.env(), identity.namespace(), identity.serviceKind(),
                        identity.protocol(), identity.serviceName(), identity.group(),
                        identity.version()),
                credentials, objectMapper, clock, properties.requireTimeout());
    }

    @Bean
    @ConditionalOnProperty(prefix = "egon.cola.component.gateway.reporting",
            name = "enabled", havingValue = "true")
    GatewayDefinitionStatusService gatewayDefinitionStatusService(
            GatewayReportingState state,
            GatewayReportingProperties properties) {
        return new GatewayDefinitionStatusService(state, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "egon.cola.component.gateway.provider.http",
            name = "enabled", havingValue = "true")
    DdcProviderLeaseStatusService ddcProviderLeaseStatusService(
            HttpProviderLeaseRuntime runtime,
            GatewayDdcRuntimeStatusService.ServiceIdentity identity) {
        return new DdcProviderLeaseStatusService(runtime, identity);
    }

    @Bean
    @ConditionalOnProperty(prefix = "egon.cola.component.gateway.reporting",
            name = "enabled", havingValue = "true")
    GatewayDdcRuntimeStatusService gatewayDdcRuntimeStatusService(
            GatewayDefinitionStatusService definition,
            DdcProviderLeaseStatusService lease,
            GatewayAdminControlPlaneStatusClient gatewayAdmin,
            GatewayDdcRuntimeStatusService.ServiceIdentity identity,
            Clock clock) {
        return new GatewayDdcRuntimeStatusService(
                definition, lease, gatewayAdmin, identity, clock);
    }

    @Bean
    @Primary
    ControlPlaneRuntimeStatusPort rbac3ControlPlaneRuntimeStatusPort(
            ObjectProvider<GatewayDdcRuntimeStatusService> runtimeStatus,
            Rbac3OperationalRuntimeStatusService operationalStatus,
            Clock clock) {
        return () -> {
            GatewayDdcRuntimeStatusService available = runtimeStatus.getIfAvailable();
            ControlPlaneRuntimeStatusPort.RuntimeStatus controlPlane = available != null
                    ? available.status()
                    : new ControlPlaneRuntimeStatusPort.RuntimeStatus(
                    new ControlPlaneRuntimeStatusPort.DefinitionStatus(
                            "UNKNOWN", null, List.of("CONTROL_PLANE_DISABLED")),
                    new ControlPlaneRuntimeStatusPort.ProviderLeaseStatus(
                            "STOPPED", null, null),
                    new ControlPlaneRuntimeStatusPort.GatewayReleaseStatus(
                            null, "UNKNOWN", null),
                    clock.instant());
            var operational = operationalStatus.status();
            return new ControlPlaneRuntimeStatusPort.RuntimeStatus(
                    controlPlane.definition(), controlPlane.providerLease(),
                    controlPlane.gatewayRelease(), operational.flyway(),
                    operational.redisProjection(), operational.fence(),
                    operational.outbox(), clock.instant());
        };
    }

    @Bean("rbac3Readiness")
    @ConditionalOnProperty(prefix = "egon.cola.component.gateway.reporting",
            name = "enabled", havingValue = "true")
    Rbac3ReadinessIndicator rbac3ReadinessIndicator(
            @Qualifier(Rbac3FlywayConfiguration.RBAC3_FLYWAY) Flyway rbac3Flyway,
            @Qualifier(Rbac3FlywayConfiguration.OUTBOX_FLYWAY) Flyway outboxFlyway,
            EntityManagerFactory entityManagerFactory,
            TransactionalOutbox outbox,
            @Qualifier("rbac3RuntimeRedissonClient") RedissonClient runtimeRedis,
            GatewayDefinitionStatusService definition,
            DdcProviderLeaseStatusService lease,
            GatewayDdcRuntimeStatusService runtimeStatus,
            GatewayReportingProperties reportingProperties) {
        boolean production = !List.of("local", "test").contains(
                reportingProperties.getEnv().toLowerCase(java.util.Locale.ROOT));
        return new Rbac3ReadinessIndicator(List.of(
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "rbac3Flyway", () -> migrated(rbac3Flyway)),
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "outboxFlyway", () -> migrated(outboxFlyway)),
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "jpa", entityManagerFactory::isOpen),
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "outboxSchema", () -> outbox != null),
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "rbac3RuntimeRedis", () -> redisAvailable(runtimeRedis)),
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "gatewayDefinition", () -> definition.status().accepted()),
                new Rbac3ReadinessIndicator.ReadinessCheck(
                        "ddcProviderLease", () -> !production
                                || "REGISTERED".equals(lease.status().state()))),
                () -> runtimeStatus.status().gatewayRelease().status());
    }

    @Bean
    @ConditionalOnBean(name = "rbac3Readiness")
    ApplicationListener<ContextClosedEvent> rbac3TrafficDrainListener(
            @Qualifier("rbac3Readiness") Rbac3ReadinessIndicator readiness) {
        return ignored -> readiness.stopAcceptingTraffic();
    }

    private static boolean migrated(Flyway flyway) {
        return flyway.info().pending().length == 0
                && flyway.info().current() != null;
    }

    private static boolean redisAvailable(RedissonClient redisson) {
        redisson.getKeys().count();
        return true;
    }
}
