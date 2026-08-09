package top.egon.cola.platform.idp.admin.integration.runtime;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import top.egon.cola.component.ddc.lease.DdcLeaseRole;
import top.egon.cola.component.ddc.configuration.runtime.DdcRuntimeCoordinator;
import top.egon.cola.component.ddc.configuration.runtime.DdcRuntimeState;
import top.egon.cola.component.gateway.provider.GatewayHttpProviderProperties;
import top.egon.cola.component.gateway.provider.HttpProviderLeaseRuntime;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Publishes IdP HTTP operations only after identity runtimes are ready.
 */
public final class IdpHttpProviderPublicationGate
        implements ApplicationListener<ApplicationEvent> {

    private final DdcRuntimeCoordinator coordinator;
    private final HttpProviderLeaseRuntime providerRuntime;
    private final GatewayHttpProviderProperties providerProperties;
    private final IdpRuntimeReadiness readiness;
    private final AtomicBoolean applicationReady = new AtomicBoolean();
    private final AtomicBoolean published = new AtomicBoolean();

    private volatile int serverPort;

    public IdpHttpProviderPublicationGate(
            DdcRuntimeCoordinator coordinator,
            HttpProviderLeaseRuntime providerRuntime,
            GatewayHttpProviderProperties providerProperties,
            IdpRuntimeReadiness readiness
    ) {
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
        this.providerRuntime = Objects.requireNonNull(
                providerRuntime,
                "providerRuntime"
        );
        this.providerProperties = Objects.requireNonNull(
                providerProperties,
                "providerProperties"
        );
        this.readiness = Objects.requireNonNull(readiness, "readiness");
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof WebServerInitializedEvent initialized) {
            String namespace = initialized.getApplicationContext()
                    .getServerNamespace();
            if (namespace == null || namespace.isBlank()) {
                serverPort = initialized.getWebServer().getPort();
                tryPublish();
            }
            return;
        }
        if (event instanceof ApplicationReadyEvent) {
            requireReady();
            applicationReady.set(true);
            tryPublish();
        }
    }

    public boolean mayPublish(ReadinessStatus status) {
        Objects.requireNonNull(status, "status");
        return status.ddcConfigClientReady()
                && status.oauthRuntimeReady()
                && status.outboxRuntimeReady();
    }

    private void tryPublish() {
        if (!applicationReady.get() || serverPort <= 0 || published.get()) {
            return;
        }
        requireReady();
        int configuredPort = providerProperties.getPort();
        if (configuredPort > 0 && configuredPort != serverPort) {
            throw new IllegalStateException(
                    "IdP HTTP provider port does not match the root web server"
            );
        }
        if (published.compareAndSet(false, true)) {
            providerRuntime.onHttpServerReady(serverPort);
        }
    }

    private void requireReady() {
        requireConfigClientReady();
        ReadinessStatus status = Objects.requireNonNull(
                readiness.status(),
                "IdP readiness status"
        );
        if (!status.ddcConfigClientReady()) {
            throw new IllegalStateException("DDC config client is not ready");
        }
        if (!status.oauthRuntimeReady()) {
            throw new IllegalStateException("OAuth runtime is not ready");
        }
        if (!status.outboxRuntimeReady()) {
            throw new IllegalStateException("Outbox runtime is not ready");
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
            throw new IllegalStateException(
                    "DDC config client session is missing"
            );
        }
    }

    public record ReadinessStatus(
            boolean ddcConfigClientReady,
            boolean oauthRuntimeReady,
            boolean outboxRuntimeReady
    ) {
    }
}
