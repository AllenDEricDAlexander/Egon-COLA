package top.egon.cola.component.rpc.ddc.security;

import io.grpc.Metadata;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.consumer.interceptor.RpcClientInvocation;
import top.egon.cola.component.rpc.context.identity.RpcProcessIdentity;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.ddc.contract.DdcConfigRuntimeRpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcScope;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.PullConfigRequest;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DdcRpcClientInterceptorFactoryTest {

    @Test
    void createsAllRequiredMetadataFromTheActualProtobufRequest()
            throws Exception {
        RpcClientInvocation invocation = invocation("pullConfig");
        DdcRpcClientInterceptorFactory factory =
                new DdcRpcClientInterceptorFactory(
                        new DdcRpcCredential("runtime-key", "secret"),
                        Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneOffset.UTC),
                        () -> "nonce-1"
                );

        Metadata metadata = factory.headers(invocation);

        assertThat(metadata.get(DdcRpcMetadataKeys.ACCESS_KEY))
                .isEqualTo("runtime-key");
        assertThat(metadata.get(DdcRpcMetadataKeys.TIMESTAMP))
                .isEqualTo("1700000000000");
        assertThat(metadata.get(DdcRpcMetadataKeys.NONCE)).isEqualTo("nonce-1");
        assertThat(metadata.get(DdcRpcMetadataKeys.CONTENT_SHA256))
                .hasSize(64)
                .matches("[0-9a-f]{64}");
        assertThat(metadata.get(DdcRpcMetadataKeys.SIGNATURE))
                .hasSize(64)
                .matches("[0-9a-f]{64}");
        assertThat(metadata.get(DdcRpcMetadataKeys.CONTRACT_VERSION))
                .isEqualTo("v1");
        assertThat(factory.create(invocation)).isNotNull();
        assertThat(factory.operationResolver().resolve(
                invocation.method().fullMethodName()))
                .isEqualTo(DdcRpcOperation.CONFIG_PULL);
    }

    @Test
    void resolvesTheCompleteApprovedOperationTableAndFailsClosed() {
        DdcRpcOperationResolver resolver = new DdcRpcOperationResolver();
        Map<String, DdcRpcOperation> expected = Map.ofEntries(
                Map.entry("RegisterConfigClient", DdcRpcOperation.SDK_REGISTER),
                Map.entry("HeartbeatConfigClient", DdcRpcOperation.SDK_HEARTBEAT),
                Map.entry("OfflineConfigClient", DdcRpcOperation.SDK_OFFLINE),
                Map.entry("PullConfig", DdcRpcOperation.CONFIG_PULL),
                Map.entry("AcknowledgePublish", DdcRpcOperation.PUBLISH_ACK),
                Map.entry("RegisterService", DdcRpcOperation.REGISTRY_REGISTER),
                Map.entry("HeartbeatService", DdcRpcOperation.REGISTRY_HEARTBEAT),
                Map.entry("DeregisterService", DdcRpcOperation.REGISTRY_DEREGISTER),
                Map.entry("GetServiceInstances", DdcRpcOperation.REGISTRY_READ),
                Map.entry("GetServices", DdcRpcOperation.REGISTRY_READ),
                Map.entry("FindConfig", DdcRpcOperation.MANAGEMENT_CONFIG_READ),
                Map.entry("UpsertConfig", DdcRpcOperation.MANAGEMENT_CONFIG_WRITE),
                Map.entry("DeleteConfig", DdcRpcOperation.MANAGEMENT_CONFIG_WRITE),
                Map.entry("PublishConfig", DdcRpcOperation.MANAGEMENT_PUBLISH),
                Map.entry("GetPublishTask", DdcRpcOperation.MANAGEMENT_TASK_READ),
                Map.entry("RetryPublishTask", DdcRpcOperation.MANAGEMENT_TASK_RETRY),
                Map.entry("GetConfigClients", DdcRpcOperation.MANAGEMENT_INSTANCE_READ),
                Map.entry("RevokeResourceAdmission",
                        DdcRpcOperation.MANAGEMENT_ADMISSION_REVOKE),
                Map.entry("GetScopeBindings", DdcRpcOperation.MANAGEMENT_SCOPE_READ),
                Map.entry("GetServiceKeys", DdcRpcOperation.MANAGEMENT_REGISTRY_READ),
                Map.entry("GetInstances", DdcRpcOperation.MANAGEMENT_REGISTRY_READ)
        );

        assertThat(resolver.operationsByBareMethod()).containsExactlyInAnyOrderEntriesOf(expected);
        assertThatThrownBy(() -> resolver.resolve(
                "egon.ddc.v1.DdcConfigRuntimeService/Unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown DDC RPC method");
    }

    @Test
    void credentialToStringNeverExposesTheSecret() {
        DdcRpcCredential credential = new DdcRpcCredential("key", "secret-value");

        assertThat(credential.toString())
                .contains("key")
                .doesNotContain("secret-value");
    }

    private RpcClientInvocation invocation(String javaMethodName)
            throws Exception {
        RpcContractDescriptor contract = new RpcContractValidator()
                .validate(DdcConfigRuntimeRpc.class);
        Method method = DdcConfigRuntimeRpc.class.getMethod(
                javaMethodName,
                PullConfigRequest.class
        );
        return new RpcClientInvocation(
                contract,
                contract.method(method),
                PullConfigRequest.newBuilder()
                        .setScope(DdcScope.newBuilder()
                                .setBizCode("retail")
                                .setEnv("prod")
                                .setAppCode("order"))
                        .build(),
                new RpcProcessIdentity(
                        "test", "test", "127.0.0.1", 1L, "instance-1")
        );
    }
}
