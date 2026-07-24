package top.egon.cola.component.rpc.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.egon.cola.component.ddc.model.enums.DdcServiceKind;
import top.egon.cola.component.ddc.model.registry.DdcServiceKey;
import top.egon.cola.component.ddc.model.registry.DdcServiceRegistration;
import top.egon.cola.component.ddc.model.vo.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.vo.DdcLeaseSession;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;
import top.egon.cola.component.rpc.config.EgonRpcProperties;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcProviderLeaseManager {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RpcProviderLeaseManager.class);

    private final DdcServiceRegistryClient registryClient;

    private final RpcProviderAvailabilityRegistry availability;

    private final EgonRpcProperties.Provider properties;

    private final RpcProcessIdentity processIdentity;

    private final String runtimeVersion;

    private final Map<RpcServiceIdentity, DdcServiceRegistration> registrations =
            new ConcurrentHashMap<>();

    private final Map<RpcServiceIdentity, DdcLeaseSession> leases =
            new ConcurrentHashMap<>();

    public RpcProviderLeaseManager(
            DdcServiceRegistryClient registryClient,
            RpcProviderAvailabilityRegistry availability,
            EgonRpcProperties properties,
            RpcProcessIdentity processIdentity,
            String runtimeVersion) {
        this.registryClient = registryClient;
        this.availability = availability;
        this.properties = properties.getProvider();
        this.processIdentity = processIdentity;
        this.runtimeVersion = runtimeVersion;
    }

    public void prepare(Iterable<RpcProviderBinding> providers,
                        String advertisedHost,
                        int advertisedPort) {
        validateUserMetadata(properties.getMetadata());
        for (RpcProviderBinding provider : providers) {
            RpcServiceIdentity service = provider.serviceIdentity();
            DdcServiceKey serviceKey = new DdcServiceKey(
                    processIdentity.env(),
                    processIdentity.namespace(),
                    DdcServiceKind.RPC_PROVIDER,
                    service.serviceName(),
                    service.group(),
                    service.version(),
                    "grpc"
            );
            registrations.put(service, new DdcServiceRegistration(
                    processIdentity.instanceId() + ":" + service.registrySuffix(),
                    serviceKey,
                    advertisedHost,
                    advertisedPort,
                    false,
                    registrationMetadata(),
                    properties.getLeaseSeconds(),
                    properties.getHeartbeatIntervalSeconds()
            ));
            availability.unavailable(service);
        }
    }

    public void registerAll() {
        try {
            for (RpcServiceIdentity service : registrations.keySet()) {
                register(service);
            }
        } catch (RuntimeException exception) {
            deregisterAll();
            throw exception;
        }
    }

    public void heartbeatAndRecover() {
        registrations.keySet().forEach(this::heartbeatAndRecover);
    }

    public void deregisterAll() {
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
        try {
            register(service);
        } catch (RuntimeException exception) {
            availability.unavailable(service);
            LOGGER.warn("RPC Provider lease recovery failed for {}", service, exception);
        }
    }

    private void register(RpcServiceIdentity service) {
        DdcLeaseSession session = registryClient.register(
                registrations.get(service)
        );
        leases.put(service, session);
        availability.available(service);
    }

    private Map<String, String> registrationMetadata() {
        Map<String, String> metadata = new LinkedHashMap<>(
                properties.getMetadata()
        );
        metadata.put("egon.rpc.transport", "grpc");
        metadata.put("egon.rpc.serialization", "protobuf");
        metadata.put("egon.rpc.runtime-version", runtimeVersion);
        return metadata;
    }

    private void validateUserMetadata(Map<String, String> metadata) {
        if (metadata == null) {
            return;
        }
        metadata.keySet().forEach(key -> {
            String lower = key == null
                    ? ""
                    : key.toLowerCase(java.util.Locale.ROOT);
            if (lower.startsWith("ddc.")
                    || lower.startsWith("egon.internal.")
                    || lower.startsWith("egon.rpc.")) {
                throw new IllegalArgumentException(
                        "RPC Provider metadata uses a reserved prefix"
                );
            }
        });
    }
}
