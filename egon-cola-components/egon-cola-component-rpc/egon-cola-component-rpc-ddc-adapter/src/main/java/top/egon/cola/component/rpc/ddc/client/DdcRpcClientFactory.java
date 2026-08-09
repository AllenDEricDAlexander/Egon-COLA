package top.egon.cola.component.rpc.ddc.client;

import top.egon.cola.component.ddc.api.client.DdcConfigClient;
import top.egon.cola.component.ddc.api.client.DdcManagementClient;
import top.egon.cola.component.ddc.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.ddc.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.rpc.consumer.RpcDirectClientFactory;
import top.egon.cola.component.rpc.consumer.RpcDirectClientHandle;
import top.egon.cola.component.rpc.consumer.RpcDirectClientSettings;
import top.egon.cola.component.rpc.context.RpcProcessIdentity;
import top.egon.cola.component.rpc.context.RpcClientInterceptorFactory;
import top.egon.cola.component.rpc.ddc.autoconfigure.DdcRpcProperties;
import top.egon.cola.component.rpc.ddc.client.config.RpcDdcConfigClient;
import top.egon.cola.component.rpc.ddc.client.management.RpcDdcManagementClient;
import top.egon.cola.component.rpc.ddc.client.registry.RpcDdcServiceRegistryClient;
import top.egon.cola.component.rpc.ddc.contract.DdcConfigRuntimeRpc;
import top.egon.cola.component.rpc.ddc.contract.DdcManagementRpc;
import top.egon.cola.component.rpc.ddc.contract.DdcServiceRegistryRpc;
import top.egon.cola.component.rpc.ddc.mapping.DdcCommonProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcConfigProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcManagementProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcRegistryProtoMapper;
import top.egon.cola.component.rpc.ddc.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.rpc.ddc.security.DdcRpcClientInterceptorFactory;
import top.egon.cola.component.rpc.ddc.security.DdcRpcCredential;

import java.util.List;

/**
 * 按 DDC 能力创建互不共享的 Direct RPC 客户端。
 * / Creates independently owned Direct RPC clients per DDC capability.
 */
public final class DdcRpcClientFactory {

    private final DdcRpcProperties rpcProperties;
    private final DdcProperties ddcProperties;
    private final RpcProcessIdentity processIdentity;
    private final RpcDirectClientFactory directFactory;

    public DdcRpcClientFactory(
            DdcRpcProperties rpcProperties,
            DdcProperties ddcProperties,
            RpcProcessIdentity processIdentity) {
        this(rpcProperties, ddcProperties, processIdentity,
                new RpcDirectClientFactory());
    }

    DdcRpcClientFactory(
            DdcRpcProperties rpcProperties,
            DdcProperties ddcProperties,
            RpcProcessIdentity processIdentity,
            RpcDirectClientFactory directFactory) {
        this.rpcProperties = require(rpcProperties, "rpcProperties");
        this.ddcProperties = require(ddcProperties, "ddcProperties");
        this.processIdentity = require(processIdentity, "processIdentity");
        this.directFactory = require(directFactory, "directFactory");
    }

    public DdcRpcClientHandle<DdcConfigClient> configClient() {
        DdcCommonProtoMapper common = commonMapper();
        DdcConfigProtoMapper mapper = new DdcConfigProtoMapper(
                common, ddcProperties.getMaxConfigBytes());
        RpcDirectClientHandle<DdcConfigRuntimeRpc> direct = create(
                DdcConfigRuntimeRpc.class,
                () -> rpcProperties.runtimeCredential());
        try {
            return new DdcRpcClientHandle<>(
                    new RpcDdcConfigClient(
                            direct.client(), mapper, common,
                            new DdcRpcStatusExceptionMapper(),
                            ddcProperties.getBizCode(),
                            ddcProperties.getEnv(),
                            ddcProperties.getAppCode()
                    ),
                    direct
            );
        } catch (RuntimeException failure) {
            direct.close();
            throw failure;
        }
    }

    public DdcRpcClientHandle<DdcServiceRegistryClient> registryClient() {
        DdcCommonProtoMapper common = commonMapper();
        RpcDirectClientHandle<DdcServiceRegistryRpc> direct = create(
                DdcServiceRegistryRpc.class,
                () -> rpcProperties.registryCredential());
        try {
            return new DdcRpcClientHandle<>(
                    new RpcDdcServiceRegistryClient(
                            direct.client(), new DdcRegistryProtoMapper(common),
                            common, new DdcRpcStatusExceptionMapper()
                    ),
                    direct
            );
        } catch (RuntimeException failure) {
            direct.close();
            throw failure;
        }
    }

    public DdcRpcClientHandle<DdcManagementClient> managementClient() {
        DdcCommonProtoMapper common = commonMapper();
        RpcDirectClientHandle<DdcManagementRpc> direct = create(
                DdcManagementRpc.class,
                () -> rpcProperties.managementCredential());
        try {
            return new DdcRpcClientHandle<>(
                    new RpcDdcManagementClient(
                            direct.client(),
                            new DdcManagementProtoMapper(
                                    common, ddcProperties.getMaxConfigBytes()),
                            new DdcRpcStatusExceptionMapper()
                    ),
                    direct
            );
        } catch (RuntimeException failure) {
            direct.close();
            throw failure;
        }
    }

    private <T> RpcDirectClientHandle<T> create(
            Class<T> contractType,
            CredentialSupplier credentialSupplier) {
        List<RpcClientInterceptorFactory> interceptors =
                rpcProperties.getAuth().isEnabled()
                ? List.of(new DdcRpcClientInterceptorFactory(
                        credentialSupplier.get()))
                : List.of();
        return directFactory.create(
                contractType,
                settings(),
                interceptors
        );
    }

    private RpcDirectClientSettings settings() {
        return new RpcDirectClientSettings(
                rpcProperties.requireTarget(),
                processIdentity,
                rpcProperties.transportSecurity(),
                rpcProperties.getDefaultTimeout().toMillis(),
                rpcProperties.getLoadBalancingPolicy(),
                rpcProperties.getMaxInboundMessageSize(),
                rpcProperties.getShutdownTimeout().toMillis(),
                Math.toIntExact(rpcProperties.getConnectTimeout().toMillis())
        );
    }

    private DdcCommonProtoMapper commonMapper() {
        return new DdcCommonProtoMapper(
                rpcProperties.getMaxInboundMessageSize()
        );
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    @FunctionalInterface
    private interface CredentialSupplier {
        DdcRpcCredential get();
    }
}
