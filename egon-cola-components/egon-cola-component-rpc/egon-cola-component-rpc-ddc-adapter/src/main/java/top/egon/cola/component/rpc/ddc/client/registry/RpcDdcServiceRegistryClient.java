package top.egon.cola.component.rpc.ddc.client.registry;

import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.api.registry.DdcRegistrySubscription;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationResult;
import top.egon.cola.component.ddc.model.lease.DdcLeaseOperationStatus;
import top.egon.cola.component.ddc.model.lease.DdcLeaseSession;
import top.egon.cola.component.ddc.model.registry.*;
import top.egon.cola.component.ddc.state.DdcActiveRegistrationIndex;
import top.egon.cola.component.rpc.ddc.contract.DdcServiceRegistryRpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServiceInstancesRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.GetServicesRequest;
import top.egon.cola.component.rpc.ddc.mapping.DdcCommonProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcRegistryProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.rpc.ddc.security.DdcRpcOperation;

import java.util.function.Consumer;

/** DDC 注册中心 Port 的 Direct RPC 适配器。 / Direct RPC adapter for the DDC registry Port. */
public final class RpcDdcServiceRegistryClient implements DdcServiceRegistryClient {

    private final DdcServiceRegistryRpc rpc;
    private final DdcRegistryProtoMapper mapper;
    private final DdcCommonProtoMapper common;
    private final DdcRpcStatusExceptionMapper errors;
    private final DdcActiveRegistrationIndex registrations = new DdcActiveRegistrationIndex();
    private RegistrySubscriptions subscriptions;

    public RpcDdcServiceRegistryClient(
            DdcServiceRegistryRpc rpc,
            DdcRegistryProtoMapper mapper,
            DdcCommonProtoMapper common,
            DdcRpcStatusExceptionMapper errors) {
        this.rpc = rpc;
        this.mapper = mapper;
        this.common = common;
        this.errors = errors;
    }

    public void subscriptions(RegistrySubscriptions subscriptions) {
        if (this.subscriptions != null) throw new IllegalStateException("subscriptions already configured");
        this.subscriptions = subscriptions;
    }

    @Override
    public DdcLeaseSession register(DdcServiceRegistration registration) {
        return invoke(DdcRpcOperation.REGISTRY_REGISTER, () -> {
            var response = rpc.registerService(mapper.toRegisterRequest(registration));
            if (!response.hasSession()) throw new IllegalArgumentException("session is required");
            DdcLeaseSession session = common.fromProto(response.getSession());
            if (session.role() != registration.serviceKey().serviceKind().leaseRole()
                    || !registration.instanceId().equals(session.instanceId())) {
                throw new IllegalArgumentException(
                        "registry lease session does not match registration");
            }
            registrations.put(registration.serviceKey(), session);
            return session;
        });
    }

    @Override
    public DdcLeaseOperationResult heartbeat(String instanceId, String leaseId) {
        DdcServiceKey key = registrations.require(instanceId, leaseId);
        DdcServiceLeaseRequest request = lease(instanceId, leaseId, key);
        DdcLeaseOperationResult result = invoke(
                DdcRpcOperation.REGISTRY_HEARTBEAT, () -> {
            var response = rpc.heartbeatService(mapper.toHeartbeatRequest(request));
            if (!response.hasResult()) throw new IllegalArgumentException("result is required");
            return common.fromProto(response.getResult());
        });
        if (result.status() == DdcLeaseOperationStatus.NOT_FOUND
                || result.status() == DdcLeaseOperationStatus.LEASE_MISMATCH) {
            registrations.remove(leaseId);
        }
        return result;
    }

    @Override
    public DdcLeaseOperationResult deregister(String instanceId, String leaseId) {
        DdcServiceKey key = registrations.require(instanceId, leaseId);
        DdcServiceLeaseRequest request = lease(instanceId, leaseId, key);
        DdcLeaseOperationResult result = invoke(DdcRpcOperation.REGISTRY_DEREGISTER, () -> {
            var response = rpc.deregisterService(mapper.toDeregisterRequest(request));
            if (!response.hasResult()) throw new IllegalArgumentException("result is required");
            return common.fromProto(response.getResult());
        });
        if (result.status() != DdcLeaseOperationStatus.NOT_DELETED) {
            registrations.remove(leaseId);
        }
        return result;
    }

    @Override
    public DdcServiceSnapshot getInstances(DdcServiceKey serviceKey) {
        return invoke(DdcRpcOperation.REGISTRY_READ, () -> mapper.fromInstancesResponse(
                rpc.getServiceInstances(GetServiceInstancesRequest.newBuilder()
                        .setServiceKey(common.toProto(serviceKey)).build())));
    }

    @Override
    public DdcRegistrySubscription subscribe(DdcServiceKey serviceKey, Consumer<DdcServiceSnapshot> listener) {
        return requireSubscriptions().subscribe(serviceKey, listener);
    }

    @Override
    public DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query) {
        return invoke(DdcRpcOperation.REGISTRY_READ, () -> mapper.fromServicesResponse(
                rpc.getServices(GetServicesRequest.newBuilder()
                        .setQuery(common.toProto(query)).build())));
    }

    @Override
    public DdcRegistrySubscription subscribeServices(DdcServiceQuery query, Consumer<DdcServiceCatalogSnapshot> listener) {
        return requireSubscriptions().subscribeServices(query, listener);
    }

    private RegistrySubscriptions requireSubscriptions() {
        if (subscriptions == null) throw new IllegalStateException("DDC registry subscriptions are not configured");
        return subscriptions;
    }

    private DdcServiceLeaseRequest lease(String instanceId, String leaseId, DdcServiceKey key) {
        DdcServiceLeaseRequest request = new DdcServiceLeaseRequest();
        request.setInstanceId(instanceId); request.setLeaseId(leaseId); request.setServiceKey(key);
        return request;
    }

    private <T> T invoke(DdcRpcOperation operation, Invocation<T> invocation) {
        try { return invocation.call(); }
        catch (RuntimeException failure) { throw errors.restore(failure, operation); }
    }

    @FunctionalInterface private interface Invocation<T> { T call(); }

    public interface RegistrySubscriptions {
        DdcRegistrySubscription subscribe(DdcServiceKey key, Consumer<DdcServiceSnapshot> listener);
        DdcRegistrySubscription subscribeServices(DdcServiceQuery query, Consumer<DdcServiceCatalogSnapshot> listener);
    }
}
