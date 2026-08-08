package top.egon.cola.component.gateway.starter.discovery;

import top.egon.cola.component.gateway.contract.identity.GatewayOperationKey;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
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

/**
 * Discovers Gateway interface definitions from validated RPC contracts and
 * their Protobuf descriptors.
 */
public final class RpcGatewayDefinitionContributor
        implements GatewayDefinitionContributor {

    /** Mapper that derives request and response schemas from Protobuf types. */
    private final ProtobufSchemaMapper schemaMapper =
            new ProtobufSchemaMapper();

    /** Catalog containing RPC contract descriptors and validated snapshots. */
    private final RpcContractCatalog catalog;

    /** Reporting properties used to identify the provider application. */
    private final GatewayReportingProperties properties;

    /**
     * Creates an RPC Gateway definition contributor.
     *
     * @param catalog    the RPC contract catalog
     * @param properties the Gateway reporting properties
     */
    public RpcGatewayDefinitionContributor(
            RpcContractCatalog catalog,
            GatewayReportingProperties properties) {
        this.catalog = catalog;
        this.properties = properties;
    }

    /**
     * Discovers annotated RPC contracts and maps their validated snapshots to
     * Gateway interface groups.
     *
     * @return the discovered RPC interface groups
     * @throws IllegalArgumentException if a required validated snapshot or
     *                                  descriptor is missing, or an operation
     *                                  declaration is inconsistent
     */
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

    /**
     * Maps one validated RPC method snapshot to a reported operation.
     *
     * @param group    the declaring interface group annotation
     * @param contract the RPC contract descriptor
     * @param snapshot the validated RPC contract snapshot
     * @param method   the validated RPC method snapshot
     * @return the reported RPC operation
     * @throws IllegalArgumentException if the method is streaming, its
     *                                  descriptor is missing, or its Gateway
     *                                  declaration conflicts with the RPC
     *                                  contract
     */
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
        rejectSchemaDeclarations(annotation, method);
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

    /**
     * Rejects Java annotation schemas for RPC operations whose schemas must be
     * derived from the Protobuf descriptor.
     *
     * @param operation the Gateway operation annotation, or {@code null}
     * @param method    the RPC method used to identify validation failures
     * @throws IllegalArgumentException if an explicit request or response
     *                                  schema is declared
     */
    private void rejectSchemaDeclarations(
            GatewayOperation operation,
            RpcMethodSnapshot method) {
        if (operation == null) {
            return;
        }
        GatewayResponseSchema response = operation.responseSchema();
        if (operation.requestSchemaFields().length > 0
                || !defaultResponse(response)) {
            throw invalid(
                    method,
                    "RPC schema is derived from Protobuf Descriptor; "
                            + "requestSchemaFields and responseSchema "
                            + "must not be declared"
            );
        }
    }

    /**
     * Determines whether a response schema annotation retains all defaults.
     *
     * @param response the response schema annotation
     * @return {@code true} when no explicit response schema is declared
     */
    private boolean defaultResponse(GatewayResponseSchema response) {
        return response.wrapper() == Void.class
                && response.payloadField().isBlank()
                && response.schema() == Void.class
                && response.shape() == GatewaySchemaShape.AUTO;
    }

    /**
     * Creates a method-specific invalid RPC schema exception.
     *
     * @param method  the invalid RPC method
     * @param message the validation failure detail
     * @return the exception describing the invalid schema
     */
    private IllegalArgumentException invalid(
            RpcMethodSnapshot method,
            String message) {
        return new IllegalArgumentException(
                "invalid RPC Gateway schema for "
                        + method.fullMethodName() + ": " + message
        );
    }

    /**
     * Finds the validated snapshot corresponding to an RPC contract.
     *
     * @param contract the RPC contract descriptor
     * @return the matching validated contract snapshot
     * @throws IllegalArgumentException if no matching snapshot exists
     */
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
