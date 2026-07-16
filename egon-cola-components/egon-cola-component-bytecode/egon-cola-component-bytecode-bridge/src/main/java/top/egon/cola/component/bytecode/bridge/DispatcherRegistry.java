package top.egon.cola.component.bytecode.bridge;

import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class DispatcherRegistry {

    private static final Object MONITOR = new Object();
    private static final Map<ClassLoader, Entry> ENTRIES = new WeakHashMap<>();
    private static final AtomicLong REGISTRATION_IDS = new AtomicLong();
    private static volatile AgentBridgeStatus agentStatus = AgentBridgeStatus.disabled();

    private DispatcherRegistry() {
    }

    public static DispatcherRegistration register(
            ClassLoader applicationLoader,
            String runtimeVersion,
            BytecodeRuntimeDispatcher dispatcher
    ) {
        Objects.requireNonNull(applicationLoader, "applicationLoader");
        Objects.requireNonNull(runtimeVersion, "runtimeVersion");
        Objects.requireNonNull(dispatcher, "dispatcher");
        if (dispatcher.protocolMajor() != BridgeProtocol.MAJOR) {
            throw new IllegalArgumentException("Bridge protocol major mismatch: bridge="
                    + BridgeProtocol.MAJOR + ", runtime=" + dispatcher.protocolMajor());
        }
        if (dispatcher.protocolMinor() < 0) {
            throw new IllegalArgumentException("Bridge protocol minor must not be negative");
        }
        Set<BridgeCapability> capabilities = immutableCapabilities(dispatcher.capabilities());
        long registrationId = REGISTRATION_IDS.incrementAndGet();
        synchronized (MONITOR) {
            Entry existing = ENTRIES.get(applicationLoader);
            if (existing != null && existing.dispatcher() != null) {
                throw new IllegalStateException(
                        "A live bytecode runtime dispatcher is already registered for this ClassLoader");
            }
            Map<Long, CallSiteMetadata> callSites = existing == null
                    ? Map.of() : existing.callSites;
            Map<Long, MethodMetadata> methods = existing == null
                    ? Map.of() : existing.methods;
            Map<Long, ObservationMetadata> observations = existing == null
                    ? Map.of() : existing.observations;
            ENTRIES.put(applicationLoader, new Entry(
                    registrationId,
                    new WeakReference<>(dispatcher),
                    dispatcher.protocolMajor(),
                    dispatcher.protocolMinor(),
                    runtimeVersion,
                    capabilities,
                    callSites,
                    methods,
                    observations
            ));
        }

        WeakReference<ClassLoader> loaderReference = new WeakReference<>(applicationLoader);
        return new DispatcherRegistration(
                () -> unregister(loaderReference, registrationId));
    }

    public static BridgeStatus status(ClassLoader applicationLoader) {
        if (applicationLoader == null) {
            return BridgeStatus.unregistered(0, 0);
        }
        synchronized (MONITOR) {
            Entry entry = ENTRIES.get(applicationLoader);
            if (entry == null) {
                return BridgeStatus.unregistered(0, 0);
            }
            BytecodeRuntimeDispatcher dispatcher = entry.dispatcher();
            if (dispatcher == null) {
                return BridgeStatus.unregistered(entry.callSites.size(), entry.methods.size());
            }
            return new BridgeStatus(
                    true,
                    entry.protocolMajor,
                    entry.protocolMinor,
                    entry.runtimeVersion,
                    entry.capabilities,
                    entry.callSites.size(),
                    entry.methods.size()
            );
        }
    }

    public static void publishAgentStatus(AgentBridgeStatus status) {
        agentStatus = Objects.requireNonNull(status, "status");
    }

    public static AgentBridgeStatus agentStatus() {
        return agentStatus;
    }

    public static void registerCallSite(ClassLoader loader, CallSiteMetadata metadata) {
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(metadata, "metadata");
        synchronized (MONITOR) {
            Entry entry = entryForMetadata(loader);
            CallSiteMetadata existing = entry.callSites.get(metadata.callSiteId());
            if (existing != null && !existing.equals(metadata)) {
                throw new IllegalStateException(
                        "Call-site ID collision for " + metadata.callSiteId());
            }
            entry.callSites.putIfAbsent(metadata.callSiteId(), metadata);
        }
    }

    public static Optional<CallSiteMetadata> callSite(ClassLoader loader, long callSiteId) {
        if (loader == null) {
            return Optional.empty();
        }
        synchronized (MONITOR) {
            Entry entry = ENTRIES.get(loader);
            return entry == null
                    ? Optional.empty() : Optional.ofNullable(entry.callSites.get(callSiteId));
        }
    }

    public static void registerMethod(ClassLoader loader, MethodMetadata metadata) {
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(metadata, "metadata");
        synchronized (MONITOR) {
            Entry entry = entryForMetadata(loader);
            MethodMetadata existing = entry.methods.get(metadata.methodId());
            if (existing != null && !existing.equals(metadata)) {
                throw new IllegalStateException("Method ID collision for " + metadata.methodId());
            }
            entry.methods.putIfAbsent(metadata.methodId(), metadata);
        }
    }

    public static Optional<MethodMetadata> method(ClassLoader loader, long methodId) {
        if (loader == null) {
            return Optional.empty();
        }
        synchronized (MONITOR) {
            Entry entry = ENTRIES.get(loader);
            return entry == null
                    ? Optional.empty() : Optional.ofNullable(entry.methods.get(methodId));
        }
    }

    public static void registerObservation(ClassLoader loader, ObservationMetadata metadata) {
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(metadata, "metadata");
        synchronized (MONITOR) {
            Entry entry = entryForMetadata(loader);
            ObservationMetadata existing = entry.observations.get(metadata.methodId());
            if (existing != null && !existing.equals(metadata)) {
                throw new IllegalStateException(
                        "Observation metadata collision for " + metadata.methodId());
            }
            entry.observations.putIfAbsent(metadata.methodId(), metadata);
        }
    }

    public static Optional<ObservationMetadata> observation(ClassLoader loader, long methodId) {
        if (loader == null) {
            return Optional.empty();
        }
        synchronized (MONITOR) {
            Entry entry = ENTRIES.get(loader);
            return entry == null
                    ? Optional.empty() : Optional.ofNullable(entry.observations.get(methodId));
        }
    }

    static Optional<BytecodeRuntimeDispatcher> dispatcher(
            Class<?> callerClass,
            BridgeCapability capability
    ) {
        if (callerClass == null || callerClass.getClassLoader() == null) {
            return Optional.empty();
        }
        synchronized (MONITOR) {
            Entry entry = ENTRIES.get(callerClass.getClassLoader());
            if (entry == null || !entry.capabilities.contains(capability)) {
                return Optional.empty();
            }
            return Optional.ofNullable(entry.dispatcher());
        }
    }

    private static Entry entryForMetadata(ClassLoader loader) {
        Entry entry = ENTRIES.get(loader);
        if (entry != null) {
            return entry;
        }
        Entry created = new Entry(
                0L,
                new WeakReference<>(null),
                BridgeProtocol.MAJOR,
                BridgeProtocol.MINOR,
                "",
                Set.of(),
                Map.of(),
                Map.of(),
                Map.of()
        );
        ENTRIES.put(loader, created);
        return created;
    }

    private static void unregister(
            WeakReference<ClassLoader> loaderReference,
            long registrationId
    ) {
        ClassLoader loader = loaderReference.get();
        if (loader == null) {
            return;
        }
        synchronized (MONITOR) {
            Entry entry = ENTRIES.get(loader);
            if (entry == null || entry.registrationId != registrationId) {
                return;
            }
            if (entry.callSites.isEmpty() && entry.methods.isEmpty()
                    && entry.observations.isEmpty()) {
                ENTRIES.remove(loader);
            } else {
                ENTRIES.put(loader, entry.withoutDispatcher());
            }
        }
    }

    private static Set<BridgeCapability> immutableCapabilities(
            Set<BridgeCapability> capabilities
    ) {
        if (capabilities == null || capabilities.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(EnumSet.copyOf(capabilities));
    }

    private static final class Entry {

        private final long registrationId;
        private final WeakReference<BytecodeRuntimeDispatcher> dispatcher;
        private final int protocolMajor;
        private final int protocolMinor;
        private final String runtimeVersion;
        private final Set<BridgeCapability> capabilities;
        private final Map<Long, CallSiteMetadata> callSites;
        private final Map<Long, MethodMetadata> methods;
        private final Map<Long, ObservationMetadata> observations;

        private Entry(
                long registrationId,
                WeakReference<BytecodeRuntimeDispatcher> dispatcher,
                int protocolMajor,
                int protocolMinor,
                String runtimeVersion,
                Set<BridgeCapability> capabilities,
                Map<Long, CallSiteMetadata> callSites,
                Map<Long, MethodMetadata> methods,
                Map<Long, ObservationMetadata> observations
        ) {
            this.registrationId = registrationId;
            this.dispatcher = dispatcher;
            this.protocolMajor = protocolMajor;
            this.protocolMinor = protocolMinor;
            this.runtimeVersion = runtimeVersion;
            this.capabilities = Set.copyOf(capabilities);
            this.callSites = new LinkedHashMap<>(callSites);
            this.methods = new LinkedHashMap<>(methods);
            this.observations = new LinkedHashMap<>(observations);
        }

        private BytecodeRuntimeDispatcher dispatcher() {
            return dispatcher.get();
        }

        private Entry withoutDispatcher() {
            return new Entry(
                    0L,
                    new WeakReference<>(null),
                    BridgeProtocol.MAJOR,
                    BridgeProtocol.MINOR,
                    "",
                    Set.of(),
                    callSites,
                    methods,
                    observations
            );
        }
    }
}
