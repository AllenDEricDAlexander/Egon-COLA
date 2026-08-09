package top.egon.cola.component.gateway.starter.discovery;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Type;
import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestLocation;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestSchemaField;
import top.egon.cola.component.gateway.starter.annotation.GatewayResponseSchema;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaShape;
import top.egon.cola.component.gateway.starter.discovery.mcp.McpExposureMapper;
import top.egon.cola.component.gateway.starter.discovery.rpc.RpcGatewayDefinitionContributor;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.contract.RpcContractCatalog;
import top.egon.cola.component.rpc.contract.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.RpcContractSnapshot;
import top.egon.cola.component.rpc.contract.RpcMethodDescriptor;
import top.egon.cola.component.rpc.contract.RpcMethodSnapshot;
import top.egon.cola.component.rpc.contract.RpcType;
import top.egon.cola.component.rpc.provider.RpcServiceIdentity;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RpcGatewayDefinitionContributorTest {

    @Test
    void discoversMcpRequestAndResponseSchemasFromProtobuf()
            throws Exception {
        GatewayInterfaceDefinitionReport.Operation operation = operation(
                Contract.class
        );

        assertThat(operation.requestSchema())
                .containsEntry("x-egon-schema-model",
                        "gateway-operation-request/v2")
                .containsEntry("type", "object")
                .containsEntry("messageType", "google.protobuf.Type");
        assertThat(operation.responseSchema())
                .containsEntry("x-egon-schema-model",
                        "gateway-operation-response/v2")
                .containsEntry("type", "object")
                .containsEntry("messageType", "google.protobuf.Type");
        assertThat(properties(operation.requestSchema()))
                .containsKeys("name", "fields", "oneofs", "syntax");
        assertThat(schema(properties(operation.requestSchema()).get("fields")))
                .containsEntry("type", "array")
                .containsKey("items");
        assertThat(operation.attributes())
                .containsEntry("idempotent", true)
                .containsKey(McpExposureMapper.ATTRIBUTE_NAME);
        assertThat(operation.descriptorSnapshot())
                .containsKeys("descriptorId", "sha256", "base64DescriptorSet");
    }

    @Test
    void rejectsJavaSchemaDeclarationsOnRpcMethods() {
        assertThatThrownBy(() -> operation(DeclaredSchemaContract.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "RPC schema is derived from Protobuf Descriptor"
                )
                .hasMessageContaining(
                        "requestSchemaFields and responseSchema "
                                + "must not be declared"
                );
    }

    private GatewayInterfaceDefinitionReport.Operation operation(
            Class<?> contractType) throws Exception {
        Method javaMethod = contractType.getDeclaredMethod("lookup", Type.class);
        Descriptors.MethodDescriptor protoMethod = protoMethod();
        String serviceName = "test.Catalog";
        String fullMethodName = serviceName + "/Lookup";
        RpcContractDescriptor contract = new RpcContractDescriptor(
                contractType,
                serviceName,
                "default",
                "1.0.0",
                List.of(new RpcMethodDescriptor(
                        javaMethod,
                        "Lookup",
                        fullMethodName,
                        true,
                        null,
                        protoMethod
                ))
        );
        RpcContractSnapshot snapshot = new RpcContractSnapshot(
                serviceName,
                "default",
                "1.0.0",
                "test",
                "Catalog",
                new byte[]{1},
                "descriptor-sha",
                List.of(new RpcMethodSnapshot(
                        "Lookup",
                        fullMethodName,
                        "google.protobuf.Type",
                        "google.protobuf.Type",
                        RpcType.UNARY
                ))
        );
        RpcGatewayDefinitionContributor contributor =
                new RpcGatewayDefinitionContributor(
                        catalog(contract, snapshot),
                        properties()
                );
        return contributor.discover().getFirst()
                .interfaceGroup().operations().getFirst();
    }

    private RpcContractCatalog catalog(
            RpcContractDescriptor contract,
            RpcContractSnapshot snapshot) {
        return new RpcContractCatalog() {
            @Override
            public List<RpcContractDescriptor> contracts() {
                return List.of(contract);
            }

            @Override
            public Optional<RpcContractDescriptor> find(
                    RpcServiceIdentity serviceIdentity) {
                return Optional.empty();
            }

            @Override
            public List<RpcContractSnapshot> snapshots() {
                return List.of(snapshot);
            }

            @Override
            public Optional<RpcContractSnapshot> findSnapshot(
                    RpcServiceIdentity serviceIdentity) {
                return Optional.empty();
            }
        };
    }

    private Descriptors.MethodDescriptor protoMethod()
            throws Descriptors.DescriptorValidationException {
        DescriptorProtos.FileDescriptorProto file =
                DescriptorProtos.FileDescriptorProto.newBuilder()
                        .setName("rpc-gateway-definition-test.proto")
                        .setPackage("test")
                        .addDependency(Type.getDescriptor().getFile().getName())
                        .addService(DescriptorProtos.ServiceDescriptorProto
                                .newBuilder()
                                .setName("Catalog")
                                .addMethod(DescriptorProtos.MethodDescriptorProto
                                        .newBuilder()
                                        .setName("Lookup")
                                        .setInputType(".google.protobuf.Type")
                                        .setOutputType(".google.protobuf.Type")))
                        .build();
        return Descriptors.FileDescriptor.buildFrom(
                file,
                new Descriptors.FileDescriptor[]{
                        Type.getDescriptor().getFile()
                }
        ).findServiceByName("Catalog").findMethodByName("Lookup");
    }

    private GatewayReportingProperties properties() {
        GatewayReportingProperties properties =
                new GatewayReportingProperties();
        properties.setApplicationCode("orders");
        properties.setBizCode("trade");
        properties.setEnv("test");
        properties.setNamespace("default");
        properties.setArtifactVersion("1.0.0");
        return properties;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> properties(Map<String, Object> schema) {
        return (Map<String, Object>) schema.get("properties");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> schema(Object value) {
        return (Map<String, Object>) value;
    }

    @GatewayInterfaceGroup(
            businessDomainCode = "trade",
            businessDomainName = "交易域",
            entityDomainCode = "order",
            entityDomainName = "订单",
            code = "rpc-orders",
            name = "RPC 订单",
            mcpServerCode = "trade-mcp"
    )
    private interface Contract {

        @EgonRpcMethod(name = "Lookup", idempotent = true)
        @GatewayOperation(
                idempotent = true,
                registerMcp = true,
                mcpName = "rpc_order_lookup"
        )
        Type lookup(Type request);
    }

    @GatewayInterfaceGroup(
            businessDomainCode = "trade",
            businessDomainName = "交易域",
            entityDomainCode = "order",
            entityDomainName = "订单",
            code = "declared-rpc-orders",
            name = "显式 Schema RPC 订单",
            mcpServerCode = "trade-mcp"
    )
    private interface DeclaredSchemaContract {

        @EgonRpcMethod(name = "Lookup", idempotent = true)
        @GatewayOperation(
                idempotent = true,
                registerMcp = true,
                mcpName = "declared_rpc_order_lookup",
                requestSchemaFields = @GatewayRequestSchemaField(
                        location = GatewayRequestLocation.BODY,
                        schema = Type.class,
                        shape = GatewaySchemaShape.OBJECT
                ),
                responseSchema = @GatewayResponseSchema(
                        schema = Type.class,
                        shape = GatewaySchemaShape.OBJECT
                )
        )
        Type lookup(Type request);
    }
}
