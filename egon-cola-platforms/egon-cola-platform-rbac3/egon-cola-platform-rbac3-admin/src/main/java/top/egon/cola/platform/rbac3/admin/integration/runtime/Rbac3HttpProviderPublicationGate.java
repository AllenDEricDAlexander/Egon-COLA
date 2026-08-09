package top.egon.cola.platform.rbac3.admin.integration.runtime;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import top.egon.cola.component.ddc.model.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.service.lifecycle.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.model.instance.DdcRuntimeState;
import top.egon.cola.component.gateway.provider.GatewayHttpProviderProperties;
import top.egon.cola.component.gateway.provider.HttpProviderLeaseRuntime;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Publishes the HTTP provider only after the configuration client is ready.
 */
public final class Rbac3HttpProviderPublicationGate
        implements ApplicationListener<ApplicationEvent> {

    private final DdcRuntimeCoordinator coordinator;
    private final HttpProviderLeaseRuntime providerRuntime;
    private final GatewayHttpProviderProperties providerProperties;
    private final AtomicBoolean applicationReady = new AtomicBoolean();
    private final AtomicBoolean published = new AtomicBoolean();

    private volatile int serverPort;

    public Rbac3HttpProviderPublicationGate(
            DdcRuntimeCoordinator coordinator,
            HttpProviderLeaseRuntime providerRuntime,
            GatewayHttpProviderProperties providerProperties) {
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.providerRuntime = Objects.requireNonNull(providerRuntime, "providerRuntime");
        this.providerProperties = Objects.requireNonNull(
                providerProperties, "providerProperties");
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof WebServerInitializedEvent initialized) {
            String namespace = initialized.getApplicationContext().getServerNamespace();
            if (namespace == null || namespace.isBlank()) {
                serverPort = initialized.getWebServer().getPort();
                tryPublish();
            }
            return;
        }
        if (event instanceof ApplicationReadyEvent) {
            requireConfigClientReady();
            applicationReady.set(true);
            tryPublish();
        }
    }

    private void tryPublish() {
        if (!applicationReady.get() || serverPort <= 0 || published.get()) {
            return;
        }
        requireConfigClientReady();
        int configuredPort = providerProperties.getPort();
        if (configuredPort > 0 && configuredPort != serverPort) {
            throw new IllegalStateException(
                    "RBAC3 HTTP provider port does not match the root web server");
        }
        if (published.compareAndSet(false, true)) {
            providerRuntime.onHttpServerReady(serverPort);
        }
    }

    private void requireConfigClientReady() {
        if (coordinator.state() != DdcRuntimeState.READY) {
            throw new IllegalStateException("DDC config client is not ready");
        }
        boolean sessionPresent = coordinator.currentSession()
                .filter(session -> session.role() == DdcLeaseRole.CONFIG_CLIENT)
                .isPresent();
        if (!sessionPresent) {
            throw new IllegalStateException("DDC config client session is missing");
        }
    }
}
