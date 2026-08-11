package top.egon.cola.component.rpc.ddc.contract;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfigRuntimeServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcManagementServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceRegistryServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceInstance;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatServiceRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterServiceRequest;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRpcGeneratedContractTest {

    @Test
    void generatesTheThreeApprovedUnaryServices() {
        assertThat(DdcConfigRuntimeServiceGrpc.getServiceDescriptor()
                .getMethods())
                .extracting(method -> method.getBareMethodName())
                .containsExactly(
                        "RegisterConfigClient",
                        "HeartbeatConfigClient",
                        "OfflineConfigClient",
                        "PullConfig",
                        "AcknowledgePublish"
                );
        assertThat(DdcServiceRegistryServiceGrpc.getServiceDescriptor()
                .getMethods())
                .extracting(method -> method.getBareMethodName())
                .containsExactly(
                        "RegisterService",
                        "HeartbeatService",
                        "DeregisterService",
                        "GetServiceInstances",
                        "GetServices"
                );
        assertThat(DdcManagementServiceGrpc.getServiceDescriptor()
                .getMethods())
                .extracting(method -> method.getBareMethodName())
                .containsExactly(
                        "FindConfig",
                        "UpsertConfig",
                        "DeleteConfig",
                        "PublishConfig",
                        "GetPublishTask",
                        "RetryPublishTask",
                        "GetConfigClients",
                        "GetScopeBindings",
                        "GetServiceKeys",
                        "GetInstances"
                );
    }

    @Test
    void generatesAdmissionTransportAndAuditAccessors() {
        assertThat(RegisterConfigClientRequest.newBuilder()
                .setAdmissionTicket("register-config").build().getAdmissionTicket())
                .isEqualTo("register-config");
        assertThat(HeartbeatConfigClientRequest.newBuilder()
                .setAdmissionTicket("heartbeat-config").build().getAdmissionTicket())
                .isEqualTo("heartbeat-config");
        assertThat(RegisterServiceRequest.newBuilder()
                .setAdmissionTicket("register-service").build().getAdmissionTicket())
                .isEqualTo("register-service");
        assertThat(HeartbeatServiceRequest.newBuilder()
                .setAdmissionTicket("heartbeat-service").build().getAdmissionTicket())
                .isEqualTo("heartbeat-service");
        DdcServiceInstance instance = DdcServiceInstance.newBuilder()
                .setResourceServerId("resource-order")
                .setResourceVersion(12L)
                .setCredentialId("kid-2026")
                .build();
        assertThat(instance.getResourceServerId()).isEqualTo("resource-order");
        assertThat(instance.getResourceVersion()).isEqualTo(12L);
        assertThat(instance.getCredentialId()).isEqualTo("kid-2026");
        assertThat(instance.getDescriptorForType().findFieldByName("admission_ticket"))
                .isNull();
    }
}
