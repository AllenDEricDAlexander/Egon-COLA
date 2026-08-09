package top.egon.cola.component.rpc.ddc.contract;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfigRuntimeServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcManagementServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceRegistryServiceGrpc;

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
}
