package top.egon.cola.platform.rbac3.admin.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import top.egon.cola.component.ddc.model.enums.DdcLeaseRole;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReportResult;
import top.egon.cola.component.gateway.provider.HttpProviderLeaseRuntime;
import top.egon.cola.component.gateway.provider.HttpProviderRuntimeProperties;
import top.egon.cola.component.gateway.provider.HttpProviderRuntimeState;
import top.egon.cola.platform.rbac3.admin.integration.ddc.DdcProviderLeaseStatusService;
import top.egon.cola.platform.rbac3.admin.integration.gateway.GatewayAdminControlPlaneStatusClient;
import top.egon.cola.platform.rbac3.admin.integration.gateway.GatewayDefinitionStatusService;
import top.egon.cola.platform.rbac3.admin.integration.runtime.GatewayDdcRuntimeStatusService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayDdcConfigurationTest {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");
    private static final GatewayDdcRuntimeStatusService.ServiceIdentity IDENTITY =
            new GatewayDdcRuntimeStatusService.ServiceIdentity(
                    "rbac3", "rbac3-admin", "prod", "default",
                    "HTTP_PROVIDER", "http",
                    "rbac3-admin", "default", "1.0.0");

    @Test
    void productionProviderRequiresAnExplicitPortAndProductionYamlHasNoLocalFallback()
            throws Exception {
        assertThatThrownBy(() -> new HttpProviderRuntimeProperties(
                true, "prod", "default", "instance-1", "rbac3-admin",
                "default", "1.0.0", "http", "rbac3.internal", 0,
                30, 10, true, Map.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port=0 is only allowed in local/test");

        Path yaml = Path.of(System.getProperty("basedir"))
                .resolve("src/main/resources/application.yml");
        if (Files.exists(yaml)) {
            assertThat(Files.readString(yaml))
                    .doesNotContain("localhost", "127.0.0.1")
                    .contains("DDC_BIZ_CODE", "RBAC3_ADVERTISED_PORT",
                            "ddcRegistryRedissonClient",
                            "rbac3RuntimeRedissonClient");
        }
    }

    @Test
    void productionEnablesIndependentDdcConfigAndRegistryLeases() throws Exception {
        PropertySource<?> production = yaml("application.yml");

        assertThat(production.getProperty("egon.cola.component.ddc.enabled")).isEqualTo(true);
        assertThat(production.getProperty("egon.cola.component.ddc.biz-code"))
                .isEqualTo("${DDC_BIZ_CODE:rbac3}");
        assertThat(production.getProperty("egon.cola.component.ddc.app-code"))
                .isEqualTo("rbac3-admin");
        assertThat(production.getProperty("egon.cola.component.ddc.env"))
                .isEqualTo("${DEPLOYMENT_ENV}");
        assertThat(production.getProperty("egon.cola.component.ddc.namespace"))
                .isEqualTo("${DEPLOYMENT_NAMESPACE}");
        assertThat(production.getProperty("egon.cola.component.ddc.instance.id"))
                .isEqualTo("${RBAC3_INSTANCE_ID}");
        assertThat(production.getProperty("egon.cola.component.ddc.instance.lease-seconds"))
                .isEqualTo(30);
        assertThat(production.getProperty(
                "egon.cola.component.ddc.instance.heartbeat-interval-seconds"))
                .isEqualTo(10);
        assertThat(production.getProperty("egon.cola.component.ddc.consistency.fail-fast"))
                .isEqualTo(true);
        assertThat(production.getProperty(
                "egon.cola.component.ddc.consistency.reconcile-enabled"))
                .isEqualTo(true);
        assertThat(production.getProperty(
                "egon.cola.component.ddc.consistency.reconcile-interval-seconds"))
                .isEqualTo(30);
        assertThat(production.getProperty("egon.cola.component.ddc.registry.enabled"))
                .isEqualTo(true);
        assertThat(production.getProperty(
                "egon.cola.component.gateway.reporting.enabled"))
                .isEqualTo(true);
        assertThat(production.getProperty(
                "egon.cola.component.gateway.provider.http.enabled"))
                .isEqualTo(true);
    }

    @Test
    void localProfileAllowsExplicitDdcAndProviderEnablement() throws Exception {
        PropertySource<?> local = yaml("application-local.yml");

        assertThat(local.getProperty("egon.cola.component.ddc.enabled"))
                .isEqualTo("${RBAC3_DDC_ENABLED:false}");
        assertThat(local.getProperty("egon.cola.component.ddc.registry.enabled"))
                .isEqualTo("${RBAC3_DDC_ENABLED:false}");
        assertThat(local.getProperty("egon.cola.component.gateway.reporting.enabled"))
                .isEqualTo(false);
        assertThat(local.getProperty(
                "egon.cola.component.gateway.provider.http.enabled"))
                .isEqualTo("${RBAC3_HTTP_PROVIDER_ENABLED:false}");
    }

    @Test
    void ddcLeaseStatesRemainIndependent() {
        HttpProviderLeaseRuntime runtime = mock(HttpProviderLeaseRuntime.class);
        when(runtime.instanceId()).thenReturn("instance-1");
        when(runtime.state()).thenReturn(HttpProviderRuntimeState.RECOVERING);
        when(runtime.lease()).thenReturn(Optional.empty());
        var service = new DdcProviderLeaseStatusService(runtime, IDENTITY);

        assertThat(service.status().state()).isEqualTo("RECOVERING");
        when(runtime.state()).thenReturn(HttpProviderRuntimeState.REGISTERED);
        when(runtime.lease()).thenReturn(Optional.of(new DdcLeaseSession(
                "instance-1", "lease-1", DdcLeaseRole.HTTP_PROVIDER, 30, 10,
                NOW.minusSeconds(1), NOW.plusSeconds(30))));
        assertThat(service.status().state()).isEqualTo("REGISTERED");
        when(runtime.state()).thenReturn(HttpProviderRuntimeState.STOPPED);
        assertThat(service.status().state()).isEqualTo("STOPPED");
    }

    @Test
    void routeabilityRequiresExactDefinitionLeaseProviderAndReleaseIdentity() {
        var definition = definition("definition-1");
        var lease = new AtomicReference<>(
                new DdcProviderLeaseStatusService.ProviderLeaseStatus(
                        "REGISTERED", "instance-1", NOW.plusSeconds(30), IDENTITY));
        var client = mock(GatewayAdminControlPlaneStatusClient.class);
        when(client.snapshot()).thenReturn(snapshot("definition-1", "1.0.0", IDENTITY));
        var status = new GatewayDdcRuntimeStatusService(
                () -> definition, lease::get, client, IDENTITY,
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(status.status().gatewayRelease().status()).isEqualTo("ROUTABLE");

        when(client.snapshot()).thenReturn(snapshotWithReleaseStatus("ACTIVATING"));
        assertThat(status.status().gatewayRelease().status()).isEqualTo("NOT_ROUTABLE");

        when(client.snapshot()).thenReturn(snapshot("definition-1", "1.0.0", IDENTITY));
        lease.set(new DdcProviderLeaseStatusService.ProviderLeaseStatus(
                "REGISTERED", "instance-1", NOW.minusSeconds(1), IDENTITY));
        assertThat(status.status().gatewayRelease().status()).isEqualTo("NOT_ROUTABLE");
        lease.set(new DdcProviderLeaseStatusService.ProviderLeaseStatus(
                "REGISTERED", "instance-1", NOW.plusSeconds(30), IDENTITY));

        var wrongIdentity = new GatewayDdcRuntimeStatusService.ServiceIdentity(
                "other-biz", "rbac3-admin", "prod", "default",
                "HTTP_PROVIDER", "http",
                "rbac3-admin", "default", "1.0.0");
        when(client.snapshot()).thenReturn(snapshot(
                "definition-1", "1.0.0", wrongIdentity));
        assertThat(status.status().gatewayRelease().status()).isEqualTo("NOT_ROUTABLE");

        when(client.snapshot()).thenReturn(GatewayAdminControlPlaneStatusClient
                .GatewayAdminSnapshot.class.cast(new GatewayAdminControlPlaneStatusClient
                .GatewayAdminSnapshot(
                        new GatewayAdminControlPlaneStatusClient.ReleaseObservation(
                                "UNKNOWN", "release-1", null, null, null,
                                "GATEWAY_STATUS_UNAVAILABLE"),
                        new GatewayAdminControlPlaneStatusClient.ProviderObservation(
                                "UNKNOWN", List.of(), "GATEWAY_STATUS_UNAVAILABLE"),
                        new GatewayAdminControlPlaneStatusClient.ConsistencyObservation(
                                "UNKNOWN", null, null, false, null,
                                "GATEWAY_STATUS_UNAVAILABLE"), NOW)));
        assertThat(status.status().gatewayRelease().status()).isEqualTo("UNKNOWN");
    }

    private GatewayAdminControlPlaneStatusClient.GatewayAdminSnapshot
    snapshotWithReleaseStatus(String releaseStatus) {
        GatewayAdminControlPlaneStatusClient.GatewayAdminSnapshot routable =
                snapshot("definition-1", "1.0.0", IDENTITY);
        return new GatewayAdminControlPlaneStatusClient.GatewayAdminSnapshot(
                new GatewayAdminControlPlaneStatusClient.ReleaseObservation(
                        "SUCCESS", "release-1", releaseStatus,
                        "definition-1", "1.0.0", null),
                routable.providers(), routable.consistency(), NOW);
    }

    private GatewayDefinitionStatusService.DefinitionStatus definition(String id) {
        return new GatewayDefinitionStatusService.DefinitionStatus(
                "ACCEPTED", id, List.of(), IDENTITY);
    }

    private GatewayAdminControlPlaneStatusClient.GatewayAdminSnapshot snapshot(
            String definitionSetId,
            String publishedVersion,
            GatewayDdcRuntimeStatusService.ServiceIdentity identity) {
        var key = new GatewayAdminControlPlaneStatusClient.ServiceKey(
                identity.bizCode(), identity.appCode(),
                identity.env(), identity.namespace(), identity.serviceKind(),
                identity.protocol(), identity.serviceName(), identity.group(),
                identity.version());
        return new GatewayAdminControlPlaneStatusClient.GatewayAdminSnapshot(
                new GatewayAdminControlPlaneStatusClient.ReleaseObservation(
                        "SUCCESS", "release-1", "SUCCESS", definitionSetId,
                        publishedVersion, null),
                new GatewayAdminControlPlaneStatusClient.ProviderObservation(
                        "SUCCESS", List.of(new GatewayAdminControlPlaneStatusClient
                        .ProviderInstance("instance-1", "UP", key, definitionSetId)), null),
                new GatewayAdminControlPlaneStatusClient.ConsistencyObservation(
                        "SUCCESS", "release-1", "SUCCESS", true, "1", null), NOW);
    }

    private PropertySource<?> yaml(String fileName) throws Exception {
        Path yaml = Path.of(System.getProperty("basedir"))
                .resolve("src/main/resources")
                .resolve(fileName);
        return new YamlPropertySourceLoader()
                .load(fileName, new FileSystemResource(yaml))
                .getFirst();
    }
}
