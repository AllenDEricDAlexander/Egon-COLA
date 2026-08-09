package top.egon.cola.platform.rbac3.admin.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.configuration.runtime.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.model.instance.DdcRuntimeState;
import top.egon.cola.component.gateway.provider.GatewayHttpProviderProperties;
import top.egon.cola.component.gateway.provider.HttpProviderLeaseRuntime;
import top.egon.cola.platform.rbac3.admin.integration.runtime.Rbac3HttpProviderPublicationGate;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Rbac3HttpProviderPublicationGateTest {

    @Test
    void publishesOnceOnlyAfterRootPortApplicationReadyAndDdcReady() {
        DdcRuntimeCoordinator coordinator = readyCoordinator();
        HttpProviderLeaseRuntime provider = mock(HttpProviderLeaseRuntime.class);
        Rbac3HttpProviderPublicationGate gate = new Rbac3HttpProviderPublicationGate(
                coordinator, provider, properties(0));

        gate.onApplicationEvent(webServerEvent(18101, null));
        verify(provider, never()).onHttpServerReady(18101);
        gate.onApplicationEvent(mock(ApplicationReadyEvent.class));
        gate.onApplicationEvent(mock(ApplicationReadyEvent.class));
        gate.onApplicationEvent(webServerEvent(18101, null));

        verify(provider, times(1)).onHttpServerReady(18101);
    }

    @Test
    void ignoresManagementServerAndSupportsReversedEventOrder() {
        DdcRuntimeCoordinator coordinator = readyCoordinator();
        HttpProviderLeaseRuntime provider = mock(HttpProviderLeaseRuntime.class);
        Rbac3HttpProviderPublicationGate gate = new Rbac3HttpProviderPublicationGate(
                coordinator, provider, properties(0));

        gate.onApplicationEvent(mock(ApplicationReadyEvent.class));
        gate.onApplicationEvent(webServerEvent(19090, "management"));
        verify(provider, never()).onHttpServerReady(19090);
        gate.onApplicationEvent(webServerEvent(18101, null));

        verify(provider).onHttpServerReady(18101);
    }

    @Test
    void failsClosedWhenDdcIsNotReadyOrHasNoConfigClientSession() {
        DdcRuntimeCoordinator coordinator = mock(DdcRuntimeCoordinator.class);
        when(coordinator.state()).thenReturn(DdcRuntimeState.RECOVERING);
        when(coordinator.currentSession()).thenReturn(Optional.empty());
        HttpProviderLeaseRuntime provider = mock(HttpProviderLeaseRuntime.class);
        Rbac3HttpProviderPublicationGate gate = new Rbac3HttpProviderPublicationGate(
                coordinator, provider, properties(0));
        gate.onApplicationEvent(webServerEvent(18101, null));

        assertThatThrownBy(() -> gate.onApplicationEvent(mock(ApplicationReadyEvent.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DDC config client is not ready");
        verify(provider, never()).onHttpServerReady(18101);

        when(coordinator.state()).thenReturn(DdcRuntimeState.READY);
        assertThatThrownBy(() -> gate.onApplicationEvent(mock(ApplicationReadyEvent.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DDC config client session is missing");
    }

    @Test
    void rejectsAConfiguredProviderPortThatDiffersFromTheRootServer() {
        HttpProviderLeaseRuntime provider = mock(HttpProviderLeaseRuntime.class);
        Rbac3HttpProviderPublicationGate gate = new Rbac3HttpProviderPublicationGate(
                readyCoordinator(), provider, properties(18102));
        gate.onApplicationEvent(webServerEvent(18101, null));

        assertThatThrownBy(() -> gate.onApplicationEvent(mock(ApplicationReadyEvent.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("port");
        verify(provider, never()).onHttpServerReady(18101);
    }

    private DdcRuntimeCoordinator readyCoordinator() {
        DdcRuntimeCoordinator coordinator = mock(DdcRuntimeCoordinator.class);
        when(coordinator.state()).thenReturn(DdcRuntimeState.READY);
        when(coordinator.currentSession()).thenReturn(Optional.of(new DdcLeaseSession(
                "rbac3-1", "config-lease-secret", DdcLeaseRole.CONFIG_CLIENT,
                30, 10, Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:30Z"))));
        return coordinator;
    }

    private GatewayHttpProviderProperties properties(int port) {
        GatewayHttpProviderProperties properties = new GatewayHttpProviderProperties();
        properties.setPort(port);
        return properties;
    }

    private WebServerInitializedEvent webServerEvent(int port, String namespace) {
        WebServer server = new WebServer() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public int getPort() {
                return port;
            }
        };
        WebServerApplicationContext context = mock(WebServerApplicationContext.class);
        when(context.getServerNamespace()).thenReturn(namespace);
        return new WebServerInitializedEvent(server) {
            @Override
            public WebServerApplicationContext getApplicationContext() {
                return context;
            }
        };
    }
}
