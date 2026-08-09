package top.egon.cola.platform.idp.admin.integration.runtime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServer;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.model.instance.DdcRuntimeState;
import top.egon.cola.component.gateway.provider.GatewayHttpProviderProperties;
import top.egon.cola.component.gateway.provider.HttpProviderLeaseRuntime;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdpHttpProviderPublicationGateTest {

    @Test
    void providerRequiresDdcOauthAndOutboxReadiness() {
        IdpHttpProviderPublicationGate gate = new IdpHttpProviderPublicationGate(
                readyCoordinator(),
                mock(HttpProviderLeaseRuntime.class),
                properties(0),
                () -> status(true, true, true)
        );
        assertFalse(gate.mayPublish(status(false, true, true)));
        assertFalse(gate.mayPublish(status(true, false, true)));
        assertFalse(gate.mayPublish(status(true, true, false)));
        assertTrue(gate.mayPublish(status(true, true, true)));
    }

    @Test
    void publishesOnceOnlyAfterRootPortApplicationAndAllRuntimesAreReady() {
        DdcRuntimeCoordinator coordinator = readyCoordinator();
        HttpProviderLeaseRuntime provider = mock(HttpProviderLeaseRuntime.class);
        IdpHttpProviderPublicationGate gate = new IdpHttpProviderPublicationGate(
                coordinator,
                provider,
                properties(0),
                () -> status(true, true, true)
        );

        gate.onApplicationEvent(webServerEvent(18111, null));
        verify(provider, never()).onHttpServerReady(18111);
        gate.onApplicationEvent(mock(ApplicationReadyEvent.class));
        gate.onApplicationEvent(mock(ApplicationReadyEvent.class));
        gate.onApplicationEvent(webServerEvent(18111, null));

        verify(provider, times(1)).onHttpServerReady(18111);
    }

    @Test
    void ignoresManagementServerAndFailsClosedWhenOAuthIsNotReady() {
        HttpProviderLeaseRuntime provider = mock(HttpProviderLeaseRuntime.class);
        IdpHttpProviderPublicationGate gate = new IdpHttpProviderPublicationGate(
                readyCoordinator(),
                provider,
                properties(0),
                () -> status(true, false, true)
        );

        gate.onApplicationEvent(webServerEvent(19091, "management"));
        gate.onApplicationEvent(webServerEvent(18111, null));
        assertThatThrownBy(() -> gate.onApplicationEvent(
                mock(ApplicationReadyEvent.class)
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OAuth runtime");
        verify(provider, never()).onHttpServerReady(18111);
        verify(provider, never()).onHttpServerReady(19091);
    }

    @Test
    void failsClosedWithoutConfigClientOrWhenConfiguredPortDiffers() {
        DdcRuntimeCoordinator coordinator = mock(DdcRuntimeCoordinator.class);
        when(coordinator.state()).thenReturn(DdcRuntimeState.READY);
        when(coordinator.currentSession()).thenReturn(Optional.empty());
        HttpProviderLeaseRuntime provider = mock(HttpProviderLeaseRuntime.class);
        IdpHttpProviderPublicationGate missingSession =
                new IdpHttpProviderPublicationGate(
                        coordinator,
                        provider,
                        properties(0),
                        () -> status(true, true, true)
                );
        missingSession.onApplicationEvent(webServerEvent(18111, null));

        assertThatThrownBy(() -> missingSession.onApplicationEvent(
                mock(ApplicationReadyEvent.class)
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("session is missing");

        IdpHttpProviderPublicationGate wrongPort =
                new IdpHttpProviderPublicationGate(
                        readyCoordinator(),
                        provider,
                        properties(18112),
                        () -> status(true, true, true)
                );
        wrongPort.onApplicationEvent(webServerEvent(18111, null));
        assertThatThrownBy(() -> wrongPort.onApplicationEvent(
                mock(ApplicationReadyEvent.class)
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("port");
        verify(provider, never()).onHttpServerReady(18111);
    }

    private IdpHttpProviderPublicationGate.ReadinessStatus status(
            boolean ddcReady,
            boolean oauthReady,
            boolean outboxReady
    ) {
        return new IdpHttpProviderPublicationGate.ReadinessStatus(
                ddcReady,
                oauthReady,
                outboxReady
        );
    }

    private DdcRuntimeCoordinator readyCoordinator() {
        DdcRuntimeCoordinator coordinator = mock(DdcRuntimeCoordinator.class);
        when(coordinator.state()).thenReturn(DdcRuntimeState.READY);
        when(coordinator.currentSession()).thenReturn(Optional.of(
                new DdcLeaseSession(
                        "idp-1",
                        "lease-secret",
                        DdcLeaseRole.CONFIG_CLIENT,
                        30,
                        10,
                        Instant.parse("2026-08-02T00:00:00Z"),
                        Instant.parse("2026-08-02T00:00:30Z")
                )
        ));
        return coordinator;
    }

    private GatewayHttpProviderProperties properties(int port) {
        GatewayHttpProviderProperties properties =
                new GatewayHttpProviderProperties();
        properties.setPort(port);
        return properties;
    }

    private WebServerInitializedEvent webServerEvent(
            int port,
            String namespace
    ) {
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
        WebServerApplicationContext context =
                mock(WebServerApplicationContext.class);
        when(context.getServerNamespace()).thenReturn(namespace);
        return new WebServerInitializedEvent(server) {
            @Override
            public WebServerApplicationContext getApplicationContext() {
                return context;
            }
        };
    }
}
