package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Empty;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import top.egon.cola.component.gateway.contract.mcp.rule.McpRiskLevel;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestLocation;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestSchemaField;
import top.egon.cola.component.gateway.starter.annotation.GatewayResponseSchema;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaShape;
import top.egon.cola.component.gateway.starter.discovery.http.GatewayHttpOperationMapper;
import top.egon.cola.component.gateway.starter.discovery.mcp.McpExposureMapper;
import top.egon.cola.component.gateway.starter.discovery.rpc.RpcGatewayDefinitionContributor;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.contract.catalog.RpcContractCatalog;
import top.egon.cola.component.rpc.contract.descriptor.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.snapshot.RpcContractSnapshot;
import top.egon.cola.component.rpc.contract.descriptor.RpcMethodDescriptor;
import top.egon.cola.component.rpc.contract.snapshot.RpcMethodSnapshot;
import top.egon.cola.component.rpc.contract.descriptor.RpcType;
import top.egon.cola.component.rpc.contract.identity.RpcServiceIdentity;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpExposureMapperTest {

    @Test
    void emitsIdenticalNormalizedExposureForHttpAndRpc() throws Exception {
        GatewayInterfaceDefinitionReport.Operation http = httpOperation(
                HttpContract.class.getDeclaredMethod(
                        "lookup",
                        String.class,
                        String.class
                ),
                "GET",
                "/orders"
        );
        GatewayInterfaceDefinitionReport.Operation rpc = rpcOperation(
                RpcContract.class,
                true
        );

        assertThat(http.attributes().get(McpExposureMapper.ATTRIBUTE_NAME))
                .isEqualTo(rpc.attributes().get(
                        McpExposureMapper.ATTRIBUTE_NAME
                ))
                .isEqualTo(Map.of(
                        "registerMcp", true,
                        "mcpServerCode", "trade-mcp",
                        "mcpName", "order_get",
                        "requiredPermissions", List.of(
                                "order:read",
                                "tenant:read"
                        ),
                        "riskLevel", "HIGH",
                        "idempotent", true
                ));
        assertThat(http.attributes()).containsEntry("idempotent", true);
        assertThat(rpc.attributes()).containsEntry("idempotent", true);
        assertThat(http.tags()).containsExactly("query");
        assertThat(rpc.tags()).containsExactly("query");
    }

    @Test
    void omitsExposureWhenOperationDoesNotOptIn() throws Exception {
        GatewayInterfaceDefinitionReport.Operation operation = httpOperation(
                HttpContract.class.getDeclaredMethod("internal"),
                "GET",
                "/internal"
        );

        assertThat(operation.attributes())
                .doesNotContainKey(McpExposureMapper.ATTRIBUTE_NAME)
                .containsEntry("idempotent", false);
    }

    @Test
    void requiresServerNameAndValidPermissions() throws Exception {
        assertThatThrownBy(() -> map(
                MissingServerContract.class,
                "valid",
                false,
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mcpServerCode is required");
        assertThatThrownBy(() -> map(
                ValidGroup.class,
                "missingName",
                false,
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mcpName is required");
        assertThatThrownBy(() -> map(
                ValidGroup.class,
                "invalidPermission",
                false,
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid MCP permission");
    }

    @Test
    void rejectsUnsupportedHttpShapes() throws Exception {
        assertThatThrownBy(() -> map(
                ValidGroup.class,
                "valid",
                true,
                List.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("streaming operations are unsupported");
        assertThatThrownBy(() -> map(
                ValidGroup.class,
                "valid",
                false,
                List.of(parameter("file", "PART", false))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("multipart operations are unsupported");
        assertThatThrownBy(() -> map(
                ValidGroup.class,
                "valid",
                false,
                List.of(parameter("X-Tenant", "HEADER", true))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required HEADER parameter")
                .hasMessageContaining("X-Tenant");
        assertThatThrownBy(() -> map(
                ValidGroup.class,
                "valid",
                false,
                List.of(parameter("SESSION", "COOKIE", true))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required COOKIE parameter")
                .hasMessageContaining("SESSION");
    }

    @Test
    void permitsRequiredAuthorizationBecauseRuntimeInjectsIt()
            throws Exception {
        Map<String, Object> exposure = map(
                ValidGroup.class,
                "valid",
                false,
                List.of(parameter("authorization", "HEADER", true))
        );

        assertThat(exposure).containsEntry("registerMcp", true);
    }

    @Test
    void rejectsRpcIdempotencyMismatch() {
        assertThatThrownBy(() -> rpcOperation(MismatchedRpcContract.class, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RPC idempotency mismatch")
                .hasMessageContaining("@EgonRpcMethod=true")
                .hasMessageContaining("@GatewayOperation=false");
    }

    private Map<String, Object> map(
            Class<?> groupType,
            String methodName,
            boolean streaming,
            List<GatewayRequestParameter> parameters)
            throws Exception {
        GatewayOperation operation = ExposureMethods.class
                .getDeclaredMethod(methodName)
                .getAnnotation(GatewayOperation.class);
        return McpExposureMapper.map(
                groupType.getAnnotation(GatewayInterfaceGroup.class),
                operation,
                methodName,
                streaming,
                parameters
        );
    }

    private GatewayRequestParameter parameter(
            String name,
            String location,
            boolean required) {
        return new GatewayRequestParameter(
                GatewayRequestLocation.valueOf(location),
                name,
                required,
                false,
                new ObjectMapper().constructType(String.class),
                null,
                null
        );
    }

    private GatewayInterfaceDefinitionReport.Operation httpOperation(
            Method method,
            String httpMethod,
            String path) {
        GatewayHttpOperationMapper mapper = new GatewayHttpOperationMapper(
                properties(),
                new ObjectMapper()
        );
        GatewayDefinitionContributor.DiscoveredInterfaceGroup discovered =
                mapper.group(
                        HttpContract.class,
                        List.of(new GatewayHttpOperationMapper.Mapping(
                                new HandlerMethod(new HttpContract(), method),
                                Set.of(path),
                                Set.of(httpMethod),
                                Set.of(),
                                Set.of()
                        ))
                );
        return discovered.interfaceGroup().operations().getFirst();
    }

    private GatewayInterfaceDefinitionReport.Operation rpcOperation(
            Class<?> contractType,
            boolean rpcIdempotent) {
        try {
            Method javaMethod = contractType.getDeclaredMethod(
                    "lookup",
                    Empty.class
            );
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
                            rpcIdempotent,
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
                            "test.Request",
                            "test.Response",
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
        } catch (ReflectiveOperationException
                 | Descriptors.DescriptorValidationException exception) {
            throw new IllegalStateException(exception);
        }
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
                        .setName("mcp-exposure-test.proto")
                        .setPackage("test")
                        .addMessageType(message("Request"))
                        .addMessageType(message("Response"))
                        .addService(DescriptorProtos.ServiceDescriptorProto
                                .newBuilder()
                                .setName("Catalog")
                                .addMethod(DescriptorProtos
                                        .MethodDescriptorProto
                                        .newBuilder()
                                        .setName("Lookup")
                                        .setInputType(".test.Request")
                                        .setOutputType(".test.Response")))
                        .build();
        return Descriptors.FileDescriptor.buildFrom(
                file,
                new Descriptors.FileDescriptor[0]
        ).findServiceByName("Catalog").findMethodByName("Lookup");
    }

    private DescriptorProtos.DescriptorProto message(String name) {
        return DescriptorProtos.DescriptorProto.newBuilder()
                .setName(name)
                .build();
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

    @GatewayInterfaceGroup(
            businessDomainCode = "trade",
            businessDomainName = "Trade",
            entityDomainCode = "order",
            entityDomainName = "Order",
            code = "http-orders",
            name = "HTTP Orders",
            mcpServerCode = " trade-mcp "
    )
    private static final class HttpContract {

        @GatewayOperation(
                name = "Lookup",
                idempotent = true,
                registerMcp = true,
                mcpName = " order_get ",
                mcpRequiredPermissions = {
                        "tenant:read",
                        " order:read ",
                        "order:read"
                },
                mcpRiskLevel = McpRiskLevel.HIGH,
                tags = {"query"},
                requestSchemaFields = {
                        @GatewayRequestSchemaField(
                                location = GatewayRequestLocation.QUERY,
                                name = "id",
                                schema = String.class,
                                shape = GatewaySchemaShape.VALUE
                        ),
                        @GatewayRequestSchemaField(
                                location = GatewayRequestLocation.HEADER,
                                name = "Authorization",
                                schema = String.class,
                                shape = GatewaySchemaShape.VALUE
                        )
                },
                responseSchema = @GatewayResponseSchema(
                        schema = String.class,
                        shape = GatewaySchemaShape.VALUE
                )
        )
        String lookup(
                @RequestParam("id") String id,
                @RequestHeader("Authorization") String authorization) {
            return id;
        }

        @GatewayOperation
        String internal() {
            return "internal";
        }
    }

    @GatewayInterfaceGroup(
            businessDomainCode = "trade",
            businessDomainName = "Trade",
            entityDomainCode = "order",
            entityDomainName = "Order",
            code = "rpc-orders",
            name = "RPC Orders",
            mcpServerCode = " trade-mcp "
    )
    private interface RpcContract {

        @EgonRpcMethod(name = "Lookup", idempotent = true)
        @GatewayOperation(
                name = "Lookup",
                idempotent = true,
                registerMcp = true,
                mcpName = " order_get ",
                mcpRequiredPermissions = {
                        "tenant:read",
                        " order:read ",
                        "order:read"
                },
                mcpRiskLevel = McpRiskLevel.HIGH,
                tags = {"query"}
        )
        Empty lookup(Empty request);
    }

    @GatewayInterfaceGroup(
            businessDomainCode = "trade",
            businessDomainName = "Trade",
            entityDomainCode = "order",
            entityDomainName = "Order",
            code = "rpc-mismatch",
            name = "RPC Mismatch",
            mcpServerCode = "trade-mcp"
    )
    private interface MismatchedRpcContract {

        @EgonRpcMethod(name = "Lookup", idempotent = true)
        @GatewayOperation
        Empty lookup(Empty request);
    }

    @GatewayInterfaceGroup(
            businessDomainCode = "trade",
            businessDomainName = "Trade",
            entityDomainCode = "order",
            entityDomainName = "Order",
            code = "valid",
            name = "Valid",
            mcpServerCode = "trade-mcp"
    )
    private static final class ValidGroup {
    }

    @GatewayInterfaceGroup(
            businessDomainCode = "trade",
            businessDomainName = "Trade",
            entityDomainCode = "order",
            entityDomainName = "Order",
            code = "missing-server",
            name = "Missing Server"
    )
    private static final class MissingServerContract {
    }

    private static final class ExposureMethods {

        @GatewayOperation(registerMcp = true, mcpName = "valid")
        private void valid() {
        }

        @GatewayOperation(registerMcp = true)
        private void missingName() {
        }

        @GatewayOperation(
                registerMcp = true,
                mcpName = "invalid_permission",
                mcpRequiredPermissions = {"Order Read"}
        )
        private void invalidPermission() {
        }
    }
}
