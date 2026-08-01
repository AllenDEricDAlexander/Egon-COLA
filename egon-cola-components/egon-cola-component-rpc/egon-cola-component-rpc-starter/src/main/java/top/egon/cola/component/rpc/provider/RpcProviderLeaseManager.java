package top.egon.cola.component.rpc.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.registry.DdcServiceKeyFactory;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RpcProviderLeaseManager {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RpcProviderLeaseManager.class);

    private final DdcServiceRegistryClient registryClient;

    private final RpcProviderAvailabilityRegistry availability;

    private final EgonRpcProperties.Provider properties;

    private final boolean secure;

    private final RpcProcessIdentity processIdentity;

    private final String runtimeVersion;

    private final RpcProviderMetadataMerger metadataMerger;

    private final DdcServiceKeyFactory serviceKeyFactory;

    private final Map<RpcServiceIdentity, DdcServiceRegistration> registrations =
            new ConcurrentHashMap<>();

    private final Map<RpcServiceIdentity, DdcLeaseSession> leases =
            new ConcurrentHashMap<>();

    private boolean recoveryEnabled;

    public RpcProviderLeaseManager(
            DdcServiceRegistryClient registryClient,
            RpcProviderAvailabilityRegistry availability,
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity,
            String runtimeVersion,
            DdcServiceKeyFactory serviceKeyFactory) {
        this(
                registryClient,
                availability,
                properties,
                processIdentity,
                runtimeVersion,
                new RpcProviderMetadataMerger(List.of()),
                serviceKeyFactory
        );
    }

    public RpcProviderLeaseManager(
            DdcServiceRegistryClient registryClient,
            RpcProviderAvailabilityRegistry availability,
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity,
            String runtimeVersion,
            RpcProviderMetadataMerger metadataMerger,
            DdcServiceKeyFactory serviceKeyFactory) {
        this.registryClient = registryClient;
        this.availability = availability;
        this.properties = properties.getProvider();
        this.secure = properties.getTls().isEnabled();
        this.processIdentity = processIdentity;
        this.runtimeVersion = runtimeVersion;
        this.metadataMerger = metadataMerger;
        this.serviceKeyFactory = serviceKeyFactory;
    }

    public void prepare(Iterable<RpcProviderBinding> providers,
                        String advertisedHost,
                        int advertisedPort) {
        Map<RpcServiceIdentity, DdcServiceRegistration> prepared =
                new LinkedHashMap<>();
        for (RpcProviderBinding provider : providers) {
            RpcServiceIdentity service = provider.serviceIdentity();
            var serviceKey = serviceKeyFactory.fromScope(
                    processIdentity.env(),
                    processIdentity.namespace(),
                    DdcServiceKind.RPC_PROVIDER,
                    service.serviceName(),
                    service.group(),
                    service.version(),
                    "grpc"
            );
            prepared.put(service, new DdcServiceRegistration(
                    processIdentity.instanceId() + ":" + service.registrySuffix(),
                    serviceKey,
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
                registryClient.deregister(
                        session.instanceId(),
                        session.leaseId()
                );
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

    public Map<RpcServiceIdentity, DdcLeaseSession> currentLeases() {
        return Map.copyOf(leases);
    }

    private void heartbeatAndRecover(RpcServiceIdentity service) {
        DdcLeaseSession session = leases.get(service);
        if (session == null) {
            recover(service);
            return;
        }
        try {
            DdcLeaseOperationResult result = registryClient.heartbeat(
                    session.instanceId(),
                    session.leaseId()
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
        DdcLeaseSession session = registryClient.register(
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

}
