package top.egon.cola.component.gateway.starter.discovery;

import top.egon.cola.component.gateway.contract.identity.GatewayOperationKey;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.rpc.contract.RpcContractCatalog;
import top.egon.cola.component.rpc.contract.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.RpcContractSnapshot;
import top.egon.cola.component.rpc.contract.RpcMethodDescriptor;
import top.egon.cola.component.rpc.contract.RpcMethodSnapshot;
import top.egon.cola.component.rpc.contract.RpcType;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public final class RpcGatewayDefinitionContributor
        implements GatewayDefinitionContributor {

    private final RpcContractCatalog catalog;

    private final GatewayReportingProperties properties;

    public RpcGatewayDefinitionContributor(
            RpcContractCatalog catalog,
            GatewayReportingProperties properties) {
        this.catalog = catalog;
        this.properties = properties;
    }

    @Override
    public List<DiscoveredInterfaceGroup> discover() {
        List<DiscoveredInterfaceGroup> result = new ArrayList<>();
        for (RpcContractDescriptor contract : catalog.contracts()) {
            GatewayInterfaceGroup group =
                    contract.contractType().getAnnotation(
                            GatewayInterfaceGroup.class
                    );
            if (group == null) {
                continue;
            }
            RpcContractSnapshot snapshot = snapshot(contract);
            List<GatewayInterfaceDefinitionReport.Operation> operations =
                    snapshot.methods().stream()
                            .map(method -> operation(
                                    contract,
                                    snapshot,
                                    method
                            ))
                            .toList();
            result.add(new DiscoveredInterfaceGroup(
                    group.businessDomainCode(),
                    group.businessDomainName(),
                    null,
                    group.entityDomainCode(),
                    group.entityDomainName(),
                    null,
                    new GatewayInterfaceDefinitionReport.InterfaceGroup(
                            group.code(),
                            group.name(),
                            group.description(),
                            "STARTER",
                            null,
                            "RPC",
                            Map.of(
                                    "serviceName",
                                    snapshot.serviceName(),
                                    "group", snapshot.group(),
                                    "version", snapshot.version(),
                                    "protoPackage",
                                    snapshot.protoPackage(),
                                    "protoServiceName",
                                    snapshot.protoServiceName()
                            ),
                            operations
                    )
            ));
        }
        return List.copyOf(result);
    }

    private GatewayInterfaceDefinitionReport.Operation operation(
            RpcContractDescriptor contract,
            RpcContractSnapshot snapshot,
            RpcMethodSnapshot method) {
        if (method.rpcType() != RpcType.UNARY) {
            throw new IllegalArgumentException(
                    "RPC streaming is not supported: "
                            + method.fullMethodName()
            );
        }
        RpcMethodDescriptor descriptor = contract.methods()
                .stream()
                .filter(candidate -> candidate.fullMethodName()
                        .equals(method.fullMethodName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "RPC method descriptor is missing"
                ));
        GatewayOperation annotation = descriptor.javaMethod()
                .getAnnotation(GatewayOperation.class);
        Map<String, Object> descriptorSnapshot = Map.of(
                "descriptorId",
                snapshot.serviceName()
                        + ":"
                        + snapshot.group()
                        + ":"
                        + snapshot.version(),
                "sha256", snapshot.descriptorSha256(),
                "base64DescriptorSet",
                Base64.getEncoder().encodeToString(
                        snapshot.fileDescriptorSet()
                )
        );
        return new GatewayInterfaceDefinitionReport.Operation(
                GatewayOperationKey.rpc(
                        properties.getApplicationCode(),
                        snapshot.serviceName(),
                        snapshot.group(),
                        snapshot.version(),
                        method.fullMethodName()
                ).value(),
                "RPC",
                method.fullMethodName(),
                annotation == null || annotation.name().isBlank()
                        ? method.methodName()
                        : annotation.name(),
                annotation == null ? "" : annotation.summary(),
                annotation == null ? "" : annotation.description(),
                annotation == null ? "" : annotation.owner(),
                annotation == null
                        ? List.of()
                        : List.of(annotation.tags()),
                annotation != null && annotation.externalAccessible(),
                "SUPPORTED",
                new GatewayInterfaceDefinitionReport.ProviderService(
                        properties.getEnv(),
                        properties.getNamespace(),
                        "RPC",
                        snapshot.serviceName(),
                        snapshot.group(),
                        snapshot.version(),
                        "GRPC"
                ),
                List.of(),
                Map.of(
                        "type", "protobuf",
                        "messageType", method.requestType()
                ),
                Map.of(
                        "type", "protobuf",
                        "messageType", method.responseType()
                ),
                List.of(),
                descriptorSnapshot,
                Map.of(
                        "rpcType", method.rpcType().name(),
                        "descriptorSha256",
                        snapshot.descriptorSha256(),
                        "responseMode", "TRANSPARENT"
                ),
                descriptor.javaMethod().isAnnotationPresent(
                        Deprecated.class
                )
        );
    }

    private RpcContractSnapshot snapshot(
            RpcContractDescriptor contract) {
        return catalog.snapshots().stream()
                .filter(candidate -> candidate.serviceName()
                        .equals(contract.serviceName()))
                .filter(candidate -> candidate.group()
                        .equals(contract.group()))
                .filter(candidate -> candidate.version()
                        .equals(contract.version()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "validated RPC snapshot was not found for "
                                + contract.serviceName()
                ));
    }
}
