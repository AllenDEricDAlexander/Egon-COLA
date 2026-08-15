package top.egon.cola.component.rpc.ddc.contract;

import io.grpc.ServiceDescriptor;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.validation.RpcContractValidator;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcConfigRuntimeServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcManagementServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.DdcServiceRegistryServiceGrpc;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.HeartbeatServiceRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterConfigClientRequest;
import top.egon.cola.component.rpc.ddc.contract.proto.v1.RegisterServiceRequest;
import top.egon.cola.component.rpc.ddc.security.DdcRpcOperation;
import top.egon.cola.component.rpc.ddc.security.DdcRpcOperationResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DdcRpcContractDescriptorTest {

    private final RpcContractValidator validator =
            new RpcContractValidator();

    @Test
    void javaContractsMatchGeneratedGrpcDescriptorsExactly() {
        assertContract(
                DdcConfigRuntimeRpc.class,
                DdcConfigRuntimeServiceGrpc.getServiceDescriptor()
        );
        assertContract(
                DdcServiceRegistryRpc.class,
                DdcServiceRegistryServiceGrpc.getServiceDescriptor()
        );
        assertContract(
                DdcManagementRpc.class,
                DdcManagementServiceGrpc.getServiceDescriptor()
        );
    }

    @Test
    void appendsAdmissionTicketWithoutRenumberingExistingFields() {
        assertThat(RegisterConfigClientRequest.getDescriptor()
                .findFieldByName("admission_ticket").getNumber()).isEqualTo(10);
        assertThat(HeartbeatConfigClientRequest.getDescriptor()
                .findFieldByName("admission_ticket").getNumber()).isEqualTo(9);
        assertThat(RegisterServiceRequest.getDescriptor()
                .findFieldByName("admission_ticket").getNumber()).isEqualTo(9);
        assertThat(HeartbeatServiceRequest.getDescriptor()
                .findFieldByName("admission_ticket").getNumber()).isEqualTo(4);
    }

    @Test
    void mapsBusinessCatalogMethodsToAReadOnlyManagementOperation() {
        var operations = new DdcRpcOperationResolver().operationsByBareMethod();
        assertThat(operations)
                .containsEntry("GetBiz", DdcRpcOperation.MANAGEMENT_CATALOG_READ)
                .containsEntry("ListBizs", DdcRpcOperation.MANAGEMENT_CATALOG_READ)
                .containsEntry("GetApp", DdcRpcOperation.MANAGEMENT_CATALOG_READ)
                .containsEntry("ListApps", DdcRpcOperation.MANAGEMENT_CATALOG_READ);
    }

    private void assertContract(
            Class<?> contractType,
            ServiceDescriptor grpcService) {
        RpcContractDescriptor contract = validator.validate(contractType);
        List<String> javaMethods = contract.methods().stream()
                .map(method -> method.methodName())
                .sorted()
                .toList();
        List<String> grpcMethods = grpcService.getMethods().stream()
                .map(method -> method.getBareMethodName())
                .sorted()
                .toList();

        assertThat(contract.serviceName())
                .isEqualTo(grpcService.getName());
        assertThat(contract.group()).isEqualTo("ddc");
        assertThat(contract.version()).isEqualTo("1.0.0");
        assertThat(javaMethods).containsExactlyElementsOf(grpcMethods);
    }
}
