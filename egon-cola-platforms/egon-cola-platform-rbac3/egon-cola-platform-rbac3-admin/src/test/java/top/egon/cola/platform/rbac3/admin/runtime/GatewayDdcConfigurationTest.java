package top.egon.cola.platform.rbac3.admin.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import top.egon.cola.component.ddc.http.registration.DdcHttpRegistrationRuntime;
import top.egon.cola.component.ddc.http.registration.DdcHttpRegistrationRuntimeProperties;
import top.egon.cola.component.ddc.http.registration.DdcHttpRegistrationState;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.platform.rbac3.admin.runtime.domain.GatewayServiceKey;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.DdcProviderLeaseStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayAdminSnapshotVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayConsistencyObservationVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayDefinitionStatusVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayProviderInstanceVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayProviderObservationVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.GatewayReleaseObservationVO;
import top.egon.cola.platform.rbac3.admin.runtime.domain.vo.ServiceIdentityVO;
import top.egon.cola.platform.rbac3.admin.runtime.repository.ddc.DdcProviderLeaseStatusRepository;
import top.egon.cola.platform.rbac3.admin.runtime.repository.http.GatewayAdminControlPlaneStatusClient;
import top.egon.cola.platform.rbac3.admin.runtime.service.GatewayDdcRuntimeStatusService;

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
    private static final ServiceIdentityVO IDENTITY =
            new ServiceIdentityVO(
                    "rbac3", "rbac3-admin", "prod", "default",
                    "HTTP_PROVIDER", "http",
                    "rbac3-admin", "default", "1.0.0");

    @Test
    void productionProviderRequiresAnExplicitPortAndProductionYamlHasNoLocalFallback()
            throws Exception {
        assertThatThrownBy(() -> new DdcHttpRegistrationRuntimeProperties(
                true, "prod", "default", "instance-1", "rbac3-admin",
                "default", "1.0.0", "http", "rbac3.internal", 0,
                30, 10, true, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("port=0 is only allowed in local/test");

        Path yaml = Path.of(System.getProperty("basedir"))
                .resolve("src/main/resources/application.yml");
        if (Files.exists(yaml)) {
            assertThat(Files.readString(yaml))
                    .doesNotContain("localhost", "127.0.0.1")
                    .contains("RBAC3_RESOURCE_BIZ_CODE", "RBAC3_ADVERTISED_PORT",
                            "ddcRedissonClient",
                            "rbac3RuntimeRedissonClient");
        }
    }

    @Test
    void productionEnablesIndependentDdcConfigAndRegistryLeases() throws Exception {
        PropertySource<?> production = yaml("application.yml");

        assertThat(production.getProperty("egon.cola.component.ddc.enabled")).isEqualTo(true);
        assertThat(production.getProperty("egon.cola.component.ddc.biz-code"))
                .isEqualTo("${RBAC3_RESOURCE_BIZ_CODE:permission}");
        assertThat(production.getProperty("egon.cola.component.ddc.app-code"))
                .isEqualTo("${RBAC3_RESOURCE_APP_CODE:rbac3}");
        assertThat(production.getProperty("egon.cola.component.ddc.env"))
                .isEqualTo("${DEPLOYMENT_ENV}");
        assertThat(production.getProperty("egon.cola.component.ddc.namespace"))
                .isEqualTo("${DEPLOYMENT_NAMESPACE}");
        assertThat(production.getProperty(
                "egon.cola.component.ddc.rpc.target"))
                .isEqualTo("${DDC_RPC_TARGET:dns:///ddc-admin:19080}");
        assertThat(production.getProperty(
                "egon.cola.component.ddc.rpc.load-balancing-policy"))
                .isEqualTo("round_robin");
        assertThat(production.getProperty(
                "egon.cola.component.ddc.admin." + "endpoint"))
                .isNull();
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
                .isEqualTo("${RBAC3_GATEWAY_REPORTING_ENABLED:true}");
        assertThat(production.getProperty(
                "egon.cola.component.ddc.registry.http.enabled"))
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
                "egon.cola.component.ddc.registry.http.enabled"))
                .isEqualTo("${RBAC3_HTTP_PROVIDER_ENABLED:false}");
    }

    @Test
    void ddcLeaseStatesRemainIndependent() {
        DdcHttpRegistrationRuntime runtime = mock(DdcHttpRegistrationRuntime.class);
        when(runtime.instanceId()).thenReturn("instance-1");
        when(runtime.state()).thenReturn(DdcHttpRegistrationState.RECOVERING);
        when(runtime.lease()).thenReturn(Optional.empty());
        var service = new DdcProviderLeaseStatusRepository(runtime, IDENTITY);

        assertThat(service.status().state()).isEqualTo("RECOVERING");
        when(runtime.state()).thenReturn(DdcHttpRegistrationState.REGISTERED);
        when(runtime.lease()).thenReturn(Optional.of(new DdcLeaseSession(
                "instance-1", "lease-1", DdcLeaseRole.HTTP_PROVIDER, 30, 10,
                NOW.minusSeconds(1), NOW.plusSeconds(30))));
        assertThat(service.status().state()).isEqualTo("REGISTERED");
        when(runtime.state()).thenReturn(DdcHttpRegistrationState.STOPPED);
        assertThat(service.status().state()).isEqualTo("STOPPED");
    }

    @Test
    void routeabilityRequiresExactDefinitionLeaseProviderAndReleaseIdentity() {
        var definition = definition("definition-1");
        var lease = new AtomicReference<>(
                new DdcProviderLeaseStatusVO(
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
        lease.set(new DdcProviderLeaseStatusVO(
                "REGISTERED", "instance-1", NOW.minusSeconds(1), IDENTITY));
        assertThat(status.status().gatewayRelease().status()).isEqualTo("NOT_ROUTABLE");
        lease.set(new DdcProviderLeaseStatusVO(
                "REGISTERED", "instance-1", NOW.plusSeconds(30), IDENTITY));

        var wrongIdentity = new ServiceIdentityVO(
                "other-biz", "rbac3-admin", "prod", "default",
                "HTTP_PROVIDER", "http",
                "rbac3-admin", "default", "1.0.0");
        when(client.snapshot()).thenReturn(snapshot(
                "definition-1", "1.0.0", wrongIdentity));
        assertThat(status.status().gatewayRelease().status()).isEqualTo("NOT_ROUTABLE");

        when(client.snapshot()).thenReturn(GatewayAdminSnapshotVO.class.cast(new GatewayAdminSnapshotVO(
                        new GatewayReleaseObservationVO(
                                "UNKNOWN", "release-1", null, null, null,
                                "GATEWAY_STATUS_UNAVAILABLE"),
                        new GatewayProviderObservationVO(
                                "UNKNOWN", List.of(), "GATEWAY_STATUS_UNAVAILABLE"),
                        new GatewayConsistencyObservationVO(
                                "UNKNOWN", null, null, false, null,
                                "GATEWAY_STATUS_UNAVAILABLE"), NOW)));
        assertThat(status.status().gatewayRelease().status()).isEqualTo("UNKNOWN");
    }

    private GatewayAdminSnapshotVO
    snapshotWithReleaseStatus(String releaseStatus) {
        GatewayAdminSnapshotVO routable =
                snapshot("definition-1", "1.0.0", IDENTITY);
        return new GatewayAdminSnapshotVO(
                new GatewayReleaseObservationVO(
                        "SUCCESS", "release-1", releaseStatus,
                        "definition-1", "1.0.0", null),
                routable.providers(), routable.consistency(), NOW);
    }

    private GatewayDefinitionStatusVO definition(String id) {
        return new GatewayDefinitionStatusVO(
                "ACCEPTED", id, List.of(), IDENTITY);
    }

    private GatewayAdminSnapshotVO snapshot(
            String definitionSetId,
            String publishedVersion,
            ServiceIdentityVO identity) {
        var key = new GatewayServiceKey(
                identity.bizCode(), identity.appCode(),
                identity.env(), identity.namespace(), identity.serviceKind(),
                identity.protocol(), identity.serviceName(), identity.group(),
                identity.version());
        return new GatewayAdminSnapshotVO(
                new GatewayReleaseObservationVO(
                        "SUCCESS", "release-1", "SUCCESS", definitionSetId,
                        publishedVersion, null),
                new GatewayProviderObservationVO(
                        "SUCCESS", List.of(new GatewayProviderInstanceVO("instance-1", "UP", key, definitionSetId)), null),
                new GatewayConsistencyObservationVO(
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
