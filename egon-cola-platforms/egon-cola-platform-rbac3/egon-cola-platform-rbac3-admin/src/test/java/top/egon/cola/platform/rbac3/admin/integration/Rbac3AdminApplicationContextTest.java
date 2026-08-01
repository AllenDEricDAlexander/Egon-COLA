package top.egon.cola.platform.rbac3.admin.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import top.egon.cola.component.ddc.service.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.service.DefaultDdcConfigApplierRegistry;
import top.egon.cola.component.gateway.provider.GatewayHttpProviderProperties;
import top.egon.cola.component.gateway.provider.HttpProviderLeaseRuntime;
import top.egon.cola.platform.rbac3.admin.config.Rbac3AdminProperties;
import top.egon.cola.platform.rbac3.admin.integration.ddc.AtomicRbac3RuntimePolicy;
import top.egon.cola.platform.rbac3.admin.integration.ddc.Rbac3DdcPolicyApplier;
import top.egon.cola.platform.rbac3.admin.integration.ddc.Rbac3DdcPolicyConfiguration;
import top.egon.cola.platform.rbac3.admin.integration.ddc.Rbac3DdcValueDeclarations;
import top.egon.cola.platform.rbac3.admin.integration.flyway.Rbac3FlywayConfiguration;
import top.egon.cola.platform.rbac3.admin.integration.runtime.Rbac3PlatformIntegrationConfiguration;
import top.egon.cola.platform.rbac3.admin.integration.runtime.Rbac3ReadinessIndicator;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class Rbac3AdminApplicationContextTest {

    @Test
    void usesIndependentFlywayHistoriesOnTheSameDataSource() {
        DataSource dataSource = mock(DataSource.class);
        Flyway rbac3 = Rbac3FlywayConfiguration.buildRbac3Flyway(dataSource);
        Flyway outbox = Rbac3FlywayConfiguration.buildOutboxFlyway(dataSource);

        assertThat(rbac3.getConfiguration().getDataSource()).isSameAs(dataSource);
        assertThat(outbox.getConfiguration().getDataSource()).isSameAs(dataSource);
        assertThat(rbac3.getConfiguration().getTable())
                .isEqualTo("flyway_schema_history_rbac3");
        assertThat(outbox.getConfiguration().getTable())
                .isEqualTo("flyway_schema_history_outbox");
        assertThat(rbac3.getConfiguration().getLocations())
                .extracting(Object::toString)
                .containsExactly("classpath:db/migration");
        assertThat(outbox.getConfiguration().getLocations())
                .extracting(Object::toString)
                .containsExactly("classpath:db/transactional-outbox/postgresql");
    }

    @Test
    void readinessFailsClosedWhenAnyApplicationPrerequisiteFails() {
        var indicator = new Rbac3ReadinessIndicator(
                List.of(
                        new Rbac3ReadinessIndicator.ReadinessCheck(
                                "rbac3Flyway", () -> true),
                        new Rbac3ReadinessIndicator.ReadinessCheck(
                                "outboxFlyway", () -> false)),
                () -> "UNKNOWN");

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(indicator.health().getDetails())
                .containsEntry("failedCheck", "outboxFlyway")
                .containsEntry("gatewayRouteability", "UNKNOWN");
    }

    @Test
    void routeabilityIsReportedButDoesNotCreateAStartupDeadlock() {
        var indicator = new Rbac3ReadinessIndicator(
                List.of(new Rbac3ReadinessIndicator.ReadinessCheck(
                        "application", () -> true)),
                () -> "NOT_ROUTABLE");

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(indicator.health().getDetails())
                .containsEntry("gatewayRouteability", "NOT_ROUTABLE");

        indicator.stopAcceptingTraffic();
        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(indicator.health().getDetails())
                .containsEntry("failedCheck", "trafficAcceptance");
    }

    @Test
    void runtimePolicyExistsWithoutDdcAndExactAppliersExistOnlyWhenEnabled() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
                .withUserConfiguration(Rbac3DdcPolicyConfiguration.class)
                .withBean(Rbac3AdminProperties.class, Rbac3AdminProperties::new)
                .withBean(AtomicRbac3RuntimePolicy.class,
                        () -> new AtomicRbac3RuntimePolicy(new Rbac3AdminProperties()))
                .withBean(DefaultDdcConfigApplierRegistry.class,
                        () -> new DefaultDdcConfigApplierRegistry((key, value, version) -> {
                        }));

        runner.run(context -> {
            assertThat(context).hasSingleBean(AtomicRbac3RuntimePolicy.class);
            assertThat(context).doesNotHaveBean(Rbac3DdcValueDeclarations.class);
            assertThat(context).doesNotHaveBean("rbac3DdcPolicyRegistrar");
        });

        runner.withPropertyValues("egon.cola.component.ddc.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(AtomicRbac3RuntimePolicy.class);
                    assertThat(context).hasSingleBean(Rbac3DdcValueDeclarations.class);
                    DefaultDdcConfigApplierRegistry registry = context.getBean(
                            DefaultDdcConfigApplierRegistry.class);
                    for (String key : AtomicRbac3RuntimePolicy.CONFIG_KEYS) {
                        assertThat(registry.resolve(key))
                                .isInstanceOf(Rbac3DdcPolicyApplier.class);
                    }
                });
    }

    @Test
    void replacesTheDefaultProviderListenerOnlyWhenDdcIsEnabled()
            throws NoSuchMethodException {
        var method = Rbac3PlatformIntegrationConfiguration.class.getDeclaredMethod(
                "gatewayHttpProviderServerReadyListener",
                DdcRuntimeCoordinator.class,
                HttpProviderLeaseRuntime.class,
                GatewayHttpProviderProperties.class);

        assertThat(method.getAnnotation(Bean.class).name())
                .containsExactly("gatewayHttpProviderServerReadyListener");
        ConditionalOnProperty condition = method.getAnnotation(
                ConditionalOnProperty.class);
        assertThat(condition.prefix()).isEqualTo("egon.cola.component.ddc");
        assertThat(condition.name()).containsExactly("enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isFalse();
    }
}
