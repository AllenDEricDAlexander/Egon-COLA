package top.egon.cola.component.rpc.tianshu.client;

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import top.egon.cola.component.rpc.consumer.direct.RpcDirectClientFactory;
import top.egon.cola.component.rpc.consumer.direct.RpcDirectClientHandle;
import top.egon.cola.component.rpc.consumer.direct.RpcDirectClientSettings;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInterceptorFactory;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.tianshu.autoconfigure.DdcRpcProperties;
import top.egon.cola.component.rpc.tianshu.client.ai.DdcAiProtoConverter;
import top.egon.cola.component.rpc.tianshu.client.ai.RpcDdcAiFeedbackClient;
import top.egon.cola.component.rpc.tianshu.client.ai.RpcDdcAiSyncClient;
import top.egon.cola.component.rpc.tianshu.client.ai.RpcDdcPromptClient;
import top.egon.cola.component.rpc.tianshu.client.ai.RpcDdcSkillClient;
import top.egon.cola.component.rpc.tianshu.client.config.RpcDdcConfigClient;
import top.egon.cola.component.rpc.tianshu.client.management.RpcDdcManagementClient;
import top.egon.cola.component.rpc.tianshu.client.registry.RpcDdcServiceRegistryClient;
import top.egon.cola.component.rpc.tianshu.contract.DdcAiResourceRpc;
import top.egon.cola.component.rpc.tianshu.contract.DdcConfigRuntimeRpc;
import top.egon.cola.component.rpc.tianshu.contract.DdcManagementRpc;
import top.egon.cola.component.rpc.tianshu.contract.DdcServiceRegistryRpc;
import top.egon.cola.component.rpc.tianshu.mapping.DdcCommonProtoMapper;
import top.egon.cola.component.rpc.tianshu.mapping.DdcConfigProtoMapper;
import top.egon.cola.component.rpc.tianshu.mapping.DdcManagementProtoMapper;
import top.egon.cola.component.rpc.tianshu.mapping.DdcRegistryProtoMapper;
import top.egon.cola.component.rpc.tianshu.mapping.DdcRpcStatusExceptionMapper;
import top.egon.cola.component.rpc.tianshu.security.DdcRpcClientInterceptorFactory;
import top.egon.cola.component.rpc.tianshu.security.DdcRpcCredential;
import top.egon.cola.component.tianshu.api.client.DdcAiFeedbackClient;
import top.egon.cola.component.tianshu.api.client.DdcAiSyncClient;
import top.egon.cola.component.tianshu.api.client.DdcConfigClient;
import top.egon.cola.component.tianshu.api.client.DdcManagementClient;
import top.egon.cola.component.tianshu.api.client.DdcPromptClient;
import top.egon.cola.component.tianshu.api.client.DdcServiceRegistryClient;
import top.egon.cola.component.tianshu.api.client.DdcSkillClient;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcProperties;
import top.egon.cola.component.tianshu.autoconfigure.properties.DdcTenantProperties;

import java.util.List;

/**
 * 按 Tianshu 能力创建互不共享的 Direct RPC 客户端。
 * / Creates independently owned Direct RPC clients per Tianshu capability.
 */
@Slf4j
public final class DdcRpcClientFactory {

    private final DdcRpcProperties rpcProperties;
    private final DdcProperties ddcProperties;
    private final DdcTenantProperties tenantProperties;
    private final RpcProcessIdentity processIdentity;
    private final RpcDirectClientFactory directFactory;
    private final DdcAiProtoConverter aiProtoConverter;

    public DdcRpcClientFactory(
            DdcRpcProperties rpcProperties,
            DdcProperties ddcProperties,
            DdcTenantProperties tenantProperties,
            RpcProcessIdentity processIdentity) {
        this(rpcProperties, ddcProperties, tenantProperties, processIdentity,
                new RpcDirectClientFactory(), Mappers.getMapper(DdcAiProtoConverter.class));
    }

    public DdcRpcClientFactory(
            DdcRpcProperties rpcProperties,
            DdcProperties ddcProperties,
            DdcTenantProperties tenantProperties,
            RpcProcessIdentity processIdentity,
            DdcAiProtoConverter aiProtoConverter) {
        this(rpcProperties, ddcProperties, tenantProperties, processIdentity,
                new RpcDirectClientFactory(), aiProtoConverter);
    }

    DdcRpcClientFactory(
            DdcRpcProperties rpcProperties,
            DdcProperties ddcProperties,
            DdcTenantProperties tenantProperties,
            RpcProcessIdentity processIdentity,
            RpcDirectClientFactory directFactory) {
        this(rpcProperties, ddcProperties, tenantProperties, processIdentity,
                directFactory, Mappers.getMapper(DdcAiProtoConverter.class));
    }

    DdcRpcClientFactory(
            DdcRpcProperties rpcProperties,
            DdcProperties ddcProperties,
            DdcTenantProperties tenantProperties,
            RpcProcessIdentity processIdentity,
            RpcDirectClientFactory directFactory,
            DdcAiProtoConverter aiProtoConverter) {
        this.rpcProperties = require(rpcProperties, "rpcProperties");
        this.ddcProperties = require(ddcProperties, "ddcProperties");
        this.tenantProperties = require(tenantProperties, "tenantProperties");
        this.processIdentity = require(processIdentity, "processIdentity");
        this.directFactory = require(directFactory, "directFactory");
        this.aiProtoConverter = require(aiProtoConverter, "aiProtoConverter");
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
                            common, new DdcRpcStatusExceptionMapper(),
                            tenantProperties.requireTenantId()
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

    public DdcRpcClientHandle<DdcPromptClient> promptClient() {
        RpcDirectClientHandle<DdcAiResourceRpc> direct = create(
                DdcAiResourceRpc.class,
                () -> rpcProperties.runtimeCredential());
        try {
            return new DdcRpcClientHandle<>(
                    new RpcDdcPromptClient(direct.client(), aiProtoConverter,
                            new DdcRpcStatusExceptionMapper(), ddcProperties),
                    direct);
        } catch (RuntimeException failure) {
            direct.close();
            throw failure;
        }
    }

    public DdcRpcClientHandle<DdcSkillClient> skillClient() {
        RpcDirectClientHandle<DdcAiResourceRpc> direct = create(
                DdcAiResourceRpc.class,
                () -> rpcProperties.runtimeCredential());
        try {
            return new DdcRpcClientHandle<>(
                    new RpcDdcSkillClient(direct.client(), aiProtoConverter,
                            new DdcRpcStatusExceptionMapper(), ddcProperties),
                    direct);
        } catch (RuntimeException failure) {
            direct.close();
            throw failure;
        }
    }

    public DdcRpcClientHandle<DdcAiFeedbackClient> feedbackClient() {
        RpcDirectClientHandle<DdcAiResourceRpc> direct = create(
                DdcAiResourceRpc.class,
                () -> rpcProperties.runtimeCredential());
        try {
            return new DdcRpcClientHandle<>(
                    new RpcDdcAiFeedbackClient(direct.client(), aiProtoConverter,
                            new DdcRpcStatusExceptionMapper()),
                    direct);
        } catch (RuntimeException failure) {
            direct.close();
            throw failure;
        }
    }

    public DdcRpcClientHandle<DdcAiSyncClient> syncClient() {
        RpcDirectClientHandle<DdcAiResourceRpc> direct = create(
                DdcAiResourceRpc.class,
                () -> rpcProperties.runtimeCredential());
        try {
            return new DdcRpcClientHandle<>(
                    new RpcDdcAiSyncClient(direct.client(), aiProtoConverter,
                            new DdcRpcStatusExceptionMapper(), ddcProperties),
                    direct);
        } catch (RuntimeException failure) {
            direct.close();
            throw failure;
        }
    }

    private <T> RpcDirectClientHandle<T> create(
            Class<T> contractType,
            CredentialSupplier credentialSupplier) {
        long tenantId = tenantProperties.requireTenantId();
        log.debug("Creating Tianshu RPC client contract={} tenantId={}",
                contractType.getSimpleName(), tenantId);
        List<RpcClientInterceptorFactory> interceptors =
                rpcProperties.getAuth().isEnabled()
                ? List.of(new DdcRpcClientInterceptorFactory(
                        credentialSupplier.get(), tenantId))
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
