package top.egon.cola.component.gateway.starter.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import top.egon.cola.component.gateway.contract.identity.GatewayOperationKey;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.starter.GatewayReportingProperties;
import top.egon.cola.component.gateway.starter.annotation.GatewayInterfaceGroup;
import top.egon.cola.component.gateway.starter.annotation.GatewayOperation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class GatewayHttpOperationMapper {

    private final GatewayReportingProperties properties;

    private final GatewayRequestSchemaValidator requestSchemaValidator;

    private final GatewayResponseSchemaMapper responseSchemaMapper;

    GatewayHttpOperationMapper(
            GatewayReportingProperties properties,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.requestSchemaValidator = new GatewayRequestSchemaValidator(
                objectMapper
        );
        this.responseSchemaMapper = new GatewayResponseSchemaMapper(
                objectMapper
        );
    }

    GatewayDefinitionContributor.DiscoveredInterfaceGroup group(
            Class<?> beanType,
            List<Mapping> mappings) {
        GatewayInterfaceGroup annotation =
                AnnotatedElementUtils.findMergedAnnotation(
                        beanType,
                        GatewayInterfaceGroup.class
                );
        if (annotation == null) {
            return null;
        }
        Map<String, GatewayInterfaceDefinitionReport.Operation> operations =
                new LinkedHashMap<>();
        mappings.forEach(mapping -> {
            for (String method : mapping.methods()) {
                for (String path : mapping.paths()) {
                    GatewayInterfaceDefinitionReport.Operation operation =
                            operation(annotation, mapping, method, path);
                    GatewayInterfaceDefinitionReport.Operation previous =
                            operations.putIfAbsent(
                                    operation.operationKey(),
                                    operation
                            );
                    if (previous != null
                            && !previous.methodIdentity().equals(
                            operation.methodIdentity())) {
                        throw new IllegalArgumentException(
                                "duplicate HTTP operation key "
                                        + operation.operationKey()
                        );
                    }
                }
            }
        });
        return new GatewayDefinitionContributor.DiscoveredInterfaceGroup(
                annotation.businessDomainCode(),
                annotation.businessDomainName(),
                null,
                annotation.entityDomainCode(),
                annotation.entityDomainName(),
                null,
                new GatewayInterfaceDefinitionReport.InterfaceGroup(
                        annotation.code(),
                        annotation.name(),
                        annotation.description(),
                        "STARTER",
                        beanType.getName(),
                        "HTTP",
                        Map.of(
                                "declaredHosts",
                                properties.getDeclaredHosts()
                        ),
                        new ArrayList<>(operations.values())
                )
        );
    }

    private GatewayInterfaceDefinitionReport.Operation operation(
            GatewayInterfaceGroup group,
            Mapping mapping,
            String httpMethod,
            String path) {
        HandlerMethod handler = mapping.handler();
        GatewayOperation annotation =
                AnnotatedElementUtils.findMergedAnnotation(
                        handler.getMethod(),
                        GatewayOperation.class
                );
        boolean streaming = mapping.produces().stream()
                .anyMatch(value -> value.contains("text/event-stream"))
                || handler.getMethod().getReturnType().getName()
                .equals("reactor.core.publisher.Flux");
        GatewayRequestSchemaValidator.Result request =
                requestSchemaValidator.validate(handler, annotation, path);
        Map<String, Object> response = responseSchemaMapper.schema(
                handler.getMethod(),
                annotation
        );
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("httpMethod", httpMethod);
        attributes.put("path", path);
        attributes.put("consumes", mapping.consumes());
        attributes.put("produces", mapping.produces());
        attributes.put("responseMode", "TRANSPARENT");
        attributes.put("streaming", streaming);
        attributes.put(
                "idempotent",
                GatewayOperationSemantics.idempotent(annotation)
        );
        Map<String, Object> mcpExposure = McpExposureMapper.map(
                group,
                annotation,
                httpMethod + " " + path,
                streaming || "ANY".equals(httpMethod),
                request.parameters()
        );
        if (!mcpExposure.isEmpty()) {
            attributes.put(McpExposureMapper.ATTRIBUTE_NAME, mcpExposure);
        }
        return new GatewayInterfaceDefinitionReport.Operation(
                GatewayOperationKey.http(
                        properties.getApplicationCode(),
                        httpMethod,
                        path
                ).value(),
                "HTTP",
                httpMethod + " " + path,
                annotation == null || annotation.name().isBlank()
                        ? handler.getMethod().getName()
                        : annotation.name(),
                annotation == null ? "" : annotation.summary(),
                annotation == null ? "" : annotation.description(),
                annotation == null ? "" : annotation.owner(),
                GatewayOperationSemantics.tags(annotation),
                annotation != null && annotation.externalAccessible(),
                streaming ? "UNSUPPORTED" : "SUPPORTED",
                new GatewayInterfaceDefinitionReport.ProviderService(
                        properties.getBizCode(),
                        properties.getApplicationCode(),
                        properties.getEnv(),
                        properties.getNamespace(),
                        "HTTP",
                        properties.getApplicationCode(),
                        "default",
                        properties.getArtifactVersion(),
                        "HTTP"
                ),
                List.of(),
                request.schema(),
                response,
                List.of(),
                null,
                attributes,
                handler.getMethod().isAnnotationPresent(Deprecated.class)
        );
    }

    record Mapping(
            HandlerMethod handler,
            Set<String> paths,
            Set<String> methods,
            Set<String> consumes,
            Set<String> produces
    ) {
    }
}
