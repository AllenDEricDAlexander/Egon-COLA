package top.egon.cola.component.gateway.starter.discovery.impl;

import top.egon.cola.component.gateway.contract.identity.GatewayOperationKey;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;
import top.egon.cola.component.gateway.starter.annotation.GatewayResponseSchema;
import top.egon.cola.component.gateway.starter.annotation.GatewaySchemaShape;
import top.egon.cola.component.gateway.starter.discovery.GatewayDefinitionContributor;
import top.egon.cola.component.gateway.starter.discovery.GatewayOperationSemantics;
import top.egon.cola.component.gateway.starter.discovery.mapper.McpExposureMapper;
import top.egon.cola.component.gateway.starter.discovery.mapper.ProtobufSchemaMapper;
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
 *
 * 基于已校验的 RPC 契约及其 Protobuf 描述符发现网关接口定义。
 */
public final class RpcGatewayDefinitionContributor
        implements GatewayDefinitionContributor {

    /** Mapper that derives request and response schemas from Protobuf types. 根据 Protobuf 类型推导请求和响应模式的映射器。 */
    private final ProtobufSchemaMapper schemaMapper =
            new ProtobufSchemaMapper();

    /** Catalog containing RPC contract descriptors and validated snapshots. 保存 RPC 契约描述符及已校验快照的目录。 */
    private final RpcContractCatalog catalog;

    /** Reporting properties used to identify the provider application. 用于标识提供方应用的报告配置。 */
    private final GatewayReportingProperties properties;

    /**
     * Creates an RPC Gateway definition contributor.
     *
     * 创建 RPC 网关定义贡献者。
     *
     * @param catalog    the RPC contract catalog，RPC 契约目录
     * @param properties the Gateway reporting properties，网关报告配置
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
     * 发现带注解的 RPC 契约，并将其已校验快照转换为网关接口分组。
     *
     * @return the discovered RPC interface groups，已发现的 RPC 接口分组
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
     * 将一个已校验的 RPC 方法快照映射为报告中的操作。
     *
     * @param group    the declaring interface group annotation，声明接口分组的注解
     * @param contract the RPC contract descriptor，RPC 契约描述符
     * @param snapshot the validated RPC contract snapshot，已校验的 RPC 契约快照
     * @param method   the validated RPC method snapshot，已校验的 RPC 方法快照
     * @return the reported RPC operation，报告中的 RPC 操作
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
     * 对于必须从 Protobuf 描述符推导模式的 RPC 操作，拒绝 Java 注解中显式声明的模式。
     *
     * @param operation the Gateway operation annotation, or {@code null}，网关操作注解，可为 {@code null}
     * @param method    the RPC method used to identify validation failures，用于标识校验失败的 RPC 方法
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
     * 判断响应模式注解是否仍保留全部默认值。
     *
     * @param response the response schema annotation，响应模式注解
     * @return {@code true} when no explicit response schema is declared，未声明显式响应模式时返回 {@code true}
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
     * 创建包含具体方法信息的无效 RPC 模式异常。
     *
     * @param method  the invalid RPC method，无效的 RPC 方法
     * @param message the validation failure detail，校验失败详情
     * @return the exception describing the invalid schema，描述无效模式的异常
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
     * 查找与 RPC 契约对应的已校验快照。
     *
     * @param contract the RPC contract descriptor，RPC 契约描述符
     * @return the matching validated contract snapshot，匹配的已校验契约快照
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
