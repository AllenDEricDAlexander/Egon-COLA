package top.egon.cola.component.rpc.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RpcProviderLeaseManager {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RpcProviderLeaseManager.class);

    private final RpcProviderRegistry registry;

    private final RpcProviderAvailabilityRegistry availability;

    private final EgonRpcProperties.Provider properties;

    private final boolean secure;

    private final RpcProcessIdentity processIdentity;

    private final String runtimeVersion;

    private final RpcProviderMetadataMerger metadataMerger;

    private final Map<RpcServiceIdentity, RpcProviderRegistration> registrations =
            new ConcurrentHashMap<>();

    private final Map<RpcServiceIdentity, RpcProviderLease> leases =
            new ConcurrentHashMap<>();

    private boolean recoveryEnabled;

    public RpcProviderLeaseManager(
            RpcProviderRegistry registry,
            RpcProviderAvailabilityRegistry availability,
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity,
            String runtimeVersion) {
        this(
                registry,
                availability,
                properties,
                processIdentity,
                runtimeVersion,
                new RpcProviderMetadataMerger(List.of())
        );
    }

    public RpcProviderLeaseManager(
            RpcProviderRegistry registry,
            RpcProviderAvailabilityRegistry availability,
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity,
            String runtimeVersion,
            RpcProviderMetadataMerger metadataMerger) {
        this.registry = registry;
        this.availability = availability;
        this.properties = properties.getProvider();
        this.secure = properties.getTls().isEnabled();
        this.processIdentity = processIdentity;
        this.runtimeVersion = runtimeVersion;
        this.metadataMerger = metadataMerger;
    }

    public void prepare(Iterable<RpcProviderBinding> providers,
                        String advertisedHost,
                        int advertisedPort) {
        Map<RpcServiceIdentity, RpcProviderRegistration> prepared =
                new LinkedHashMap<>();
        for (RpcProviderBinding provider : providers) {
            RpcServiceIdentity service = provider.serviceIdentity();
            prepared.put(service, new RpcProviderRegistration(
                    service,
                    processIdentity,
                    advertisedHost,
                    advertisedPort,
                    secure,
                    registrationMetadata(service),
                    properties.getLeaseSeconds(),
                    properties.getHeartbeatIntervalSeconds()
            ));
        }
        registrations.clear();
        registrations.putAll(prepared);
        prepared.keySet().forEach(availability::unavailable);
    }

    public synchronized void enableRecovery() {
        recoveryEnabled = true;
    }

    public synchronized void disableRecovery() {
        recoveryEnabled = false;
    }

    public synchronized void registerAll() {
        if (!recoveryEnabled) {
            return;
        }
        try {
            for (RpcServiceIdentity service : registrations.keySet()) {
                register(service);
            }
        } catch (RuntimeException exception) {
            deregisterAll();
            throw exception;
        }
    }

    public synchronized void heartbeatAndRecover() {
        if (!recoveryEnabled) {
            return;
        }
        registrations.keySet().forEach(this::heartbeatAndRecover);
    }

    public synchronized void deregisterAll() {
        registrations.keySet().forEach(availability::unavailable);
        leases.forEach((service, session) -> {
            try {
                registry.deregister(leaseIdentity(service, session));
            } catch (RuntimeException exception) {
                LOGGER.warn(
                        "RPC Provider deregistration failed for {}",
                        service,
                        exception
                );
            }
        });
        leases.clear();
    }

    public Map<RpcServiceIdentity, RpcProviderLease> currentLeases() {
        return Map.copyOf(leases);
    }

    private void heartbeatAndRecover(RpcServiceIdentity service) {
        RpcProviderLease session = leases.get(service);
        if (session == null) {
            recover(service);
            return;
        }
        try {
            RpcLeaseOperationResult result = registry.heartbeat(
                    leaseIdentity(service, session)
            );
            if (!result.renewed()) {
                leases.remove(service, session);
                availability.unavailable(service);
                recover(service);
            }
        } catch (RuntimeException exception) {
            leases.remove(service, session);
            availability.unavailable(service);
            LOGGER.warn("RPC Provider heartbeat failed for {}", service, exception);
            recover(service);
        }
    }

    private void recover(RpcServiceIdentity service) {
        if (!recoveryEnabled) {
            return;
        }
        try {
            register(service);
        } catch (RuntimeException exception) {
            availability.unavailable(service);
            LOGGER.warn("RPC Provider lease recovery failed for {}", service, exception);
        }
    }

    private void register(RpcServiceIdentity service) {
        if (!recoveryEnabled) {
            return;
        }
        RpcProviderLease session = registry.register(
                registrations.get(service)
        );
        leases.put(service, session);
        availability.available(service);
    }

    private Map<String, String> registrationMetadata(
            RpcServiceIdentity service
    ) {
        Map<String, String> metadata = new LinkedHashMap<>(
                metadataMerger.merge(service, properties.getMetadata())
        );
        metadata.put("egon.rpc.transport", "grpc");
        metadata.put("egon.rpc.serialization", "protobuf");
        metadata.put("egon.rpc.runtime-version", runtimeVersion);
        return metadata;
    }

    private RpcProviderLeaseIdentity leaseIdentity(
            RpcServiceIdentity service,
            RpcProviderLease lease) {
        return new RpcProviderLeaseIdentity(
                service,
                lease.instanceId(),
                lease.leaseId()
        );
    }

}
