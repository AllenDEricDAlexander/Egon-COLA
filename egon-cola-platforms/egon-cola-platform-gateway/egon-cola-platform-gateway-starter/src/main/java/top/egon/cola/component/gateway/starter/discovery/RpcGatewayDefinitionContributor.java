package top.egon.cola.component.gateway.starter.discovery;

import top.egon.cola.component.gateway.contract.identity.GatewayOperationKey;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestLocation;
import top.egon.cola.component.gateway.starter.annotation.GatewayRequestSchemaField;
import top.egon.cola.component.gateway.starter.annotation.GatewayResponseSchema;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaShape;
import top.egon.cola.component.rpc.contract.RpcContractCatalog;
import top.egon.cola.component.rpc.contract.RpcContractDescriptor;
import top.egon.cola.component.rpc.contract.RpcContractSnapshot;
import top.egon.cola.component.rpc.contract.RpcMethodDescriptor;
import top.egon.cola.component.rpc.contract.RpcMethodSnapshot;
import top.egon.cola.component.rpc.contract.RpcType;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RpcGatewayDefinitionContributor
        implements GatewayDefinitionContributor {

    private final ProtobufSchemaMapper schemaMapper =
            new ProtobufSchemaMapper();

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
                                    group,
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
            GatewayInterfaceGroup group,
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
        validateSchemaDeclarations(descriptor, annotation, method);
        boolean idempotent = GatewayOperationSemantics.idempotent(annotation);
        if (descriptor.idempotent() != idempotent) {
            throw new IllegalArgumentException(
                    "RPC idempotency mismatch for "
                            + method.fullMethodName()
                            + ": @EgonRpcMethod="
                            + descriptor.idempotent()
                            + ", @GatewayOperation="
                            + idempotent
            );
        }
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
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("rpcType", method.rpcType().name());
        attributes.put("descriptorSha256", snapshot.descriptorSha256());
        attributes.put("responseMode", "TRANSPARENT");
        attributes.put("idempotent", idempotent);
        Map<String, Object> mcpExposure = McpExposureMapper.map(
                group,
                annotation,
                method.fullMethodName(),
                false,
                List.of()
        );
        if (!mcpExposure.isEmpty()) {
            attributes.put(McpExposureMapper.ATTRIBUTE_NAME, mcpExposure);
        }
        Map<String, Object> requestSchema = schemaMapper.schema(
                descriptor.protoMethod().getInputType()
        );
        requestSchema.put(
                "x-egon-schema-model",
                "gateway-operation-request/v2"
        );
        Map<String, Object> responseSchema = schemaMapper.schema(
                descriptor.protoMethod().getOutputType()
        );
        responseSchema.put(
                "x-egon-schema-model",
                "gateway-operation-response/v2"
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
                GatewayOperationSemantics.tags(annotation),
                annotation != null && annotation.externalAccessible(),
                "SUPPORTED",
                new GatewayInterfaceDefinitionReport.ProviderService(
                        properties.getBizCode(),
                        properties.getApplicationCode(),
                        properties.getEnv(),
                        properties.getNamespace(),
                        "RPC",
                        snapshot.serviceName(),
                        snapshot.group(),
                        snapshot.version(),
                        "GRPC"
                ),
                List.of(),
                requestSchema,
                responseSchema,
                List.of(),
                descriptorSnapshot,
                attributes,
                descriptor.javaMethod().isAnnotationPresent(
                        Deprecated.class
                )
        );
    }

    private void validateSchemaDeclarations(
            RpcMethodDescriptor descriptor,
            GatewayOperation operation,
            RpcMethodSnapshot method) {
        GatewayRequestSchemaField[] requests = operation == null
                ? new GatewayRequestSchemaField[0]
                : operation.requestSchemaFields();
        if (operation != null && operation.registerMcp()
                && requests.length == 0) {
            throw invalid(method, "registerMcp requires one RPC_MESSAGE");
        }
        if (requests.length > 0) {
            if (requests.length != 1) {
                throw invalid(method, "RPC request must declare one RPC_MESSAGE");
            }
            GatewayRequestSchemaField request = requests[0];
            if (request.location() != GatewayRequestLocation.RPC_MESSAGE
                    || request.expanded()
                    || !request.name().isBlank()
                    || request.shape() != GatewaySchemaShape.OBJECT) {
                throw invalid(
                        method,
                        "RPC request declaration must be RPC_MESSAGE + OBJECT"
                );
            }
            Class<?> actual = descriptor.javaMethod().getParameterTypes()[0];
            if (!actual.equals(request.schema())) {
                throw invalid(
                        method,
                        "RPC request root class mismatch: declared="
                                + request.schema().getName() + ", actual="
                                + actual.getName()
                );
            }
        }
        GatewayResponseSchema response = operation == null
                ? null : operation.responseSchema();
        boolean explicitResponse = response != null
                && !defaultResponse(response);
        if (operation != null && operation.registerMcp()
                && !explicitResponse) {
            throw invalid(method, "registerMcp requires explicit responseSchema");
        }
        if (explicitResponse) {
            validateResponse(descriptor, response, method);
        }
    }

    private void validateResponse(
            RpcMethodDescriptor descriptor,
            GatewayResponseSchema response,
            RpcMethodSnapshot method) {
        Class<?> actual = descriptor.javaMethod().getReturnType();
        if (response.wrapper() == Void.class) {
            if (!response.payloadField().isBlank()) {
                throw invalid(method, "direct RPC response has no payloadField");
            }
            if (response.shape() != GatewaySchemaShape.OBJECT
                    || !actual.equals(response.schema())) {
                throw invalid(method, "RPC response root class mismatch");
            }
            return;
        }
        if (!actual.equals(response.wrapper())) {
            throw invalid(method, "RPC response wrapper mismatch");
        }
        if (response.payloadField().isBlank()) {
            throw invalid(method, "RPC response payloadField is required");
        }
        com.google.protobuf.Descriptors.FieldDescriptor payload =
                descriptor.protoMethod().getOutputType().getFields().stream()
                        .filter(field -> field.getJsonName().equals(
                                response.payloadField()
                        ) || field.getName().equals(response.payloadField()))
                        .findFirst()
                        .orElseThrow(() -> invalid(
                                method,
                                "RPC response payloadField does not exist: "
                                        + response.payloadField()
                        ));
        GatewaySchemaShape actualShape = payload.isMapField()
                ? GatewaySchemaShape.MAP
                : payload.isRepeated()
                ? GatewaySchemaShape.LIST
                : payload.getJavaType()
                == com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE
                ? GatewaySchemaShape.OBJECT : GatewaySchemaShape.VALUE;
        GatewaySchemaShape declaredShape = response.shape()
                == GatewaySchemaShape.AUTO ? actualShape : response.shape();
        if (actualShape != declaredShape) {
            throw invalid(method, "RPC response payload shape mismatch");
        }
        com.google.protobuf.Descriptors.FieldDescriptor value = payload;
        if (payload.isMapField()) {
            value = payload.getMessageType().findFieldByName("value");
        }
        if (value.getJavaType()
                == com.google.protobuf.Descriptors.FieldDescriptor.JavaType.MESSAGE) {
            if (!descriptorClassMatches(response.schema(), value.getMessageType())) {
                throw invalid(method, "RPC response payload class mismatch");
            }
        } else if (!scalarClass(value).equals(box(response.schema()))) {
            throw invalid(method, "RPC response scalar class mismatch");
        }
    }

    private boolean descriptorClassMatches(
            Class<?> type,
            com.google.protobuf.Descriptors.Descriptor descriptor) {
        try {
            Object value = type.getMethod("getDescriptor").invoke(null);
            return descriptor.equals(value);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    private Class<?> scalarClass(
            com.google.protobuf.Descriptors.FieldDescriptor field) {
        return switch (field.getJavaType()) {
            case BOOLEAN -> Boolean.class;
            case BYTE_STRING -> com.google.protobuf.ByteString.class;
            case DOUBLE -> Double.class;
            case ENUM -> String.class;
            case FLOAT -> Float.class;
            case INT -> Integer.class;
            case LONG -> Long.class;
            case STRING -> String.class;
            case MESSAGE -> throw new IllegalArgumentException(
                    "message field is not scalar"
            );
        };
    }

    private Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        return type;
    }

    private boolean defaultResponse(GatewayResponseSchema response) {
        return response.wrapper() == Void.class
                && response.payloadField().isBlank()
                && response.schema() == Void.class
                && response.shape() == GatewaySchemaShape.AUTO;
    }

    private IllegalArgumentException invalid(
            RpcMethodSnapshot method,
            String message) {
        return new IllegalArgumentException(
                "invalid RPC Gateway schema for "
                        + method.fullMethodName() + ": " + message
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
