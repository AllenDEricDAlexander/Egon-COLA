package top.egon.cola.component.yuheng.openapi.customizer;

import io.swagger.v3.oas.models.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springdoc.core.customizers.OperationCustomizer;
import top.egon.cola.component.yuheng.contract.mcp.rule.McpRiskLevel;
import top.egon.cola.component.yuheng.openapi.annotation.EgonApiCatalog;
import top.egon.cola.component.yuheng.openapi.annotation.EgonGatewayPolicy;
import top.egon.cola.component.yuheng.openapi.annotation.EgonMcpTool;
import top.egon.cola.component.yuheng.openapi.config.GatewayOpenApiProperties;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Maps the three Egon governance annotations to the versioned operation
 * extension while leaving standard OpenAPI fields owned by Springdoc.
 */
@Slf4j
@RequiredArgsConstructor
public class EgonOperationCustomizer implements OperationCustomizer {

    @Qualifier("gatewayOpenApiProperties")
    private final GatewayOpenApiProperties properties;

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        io.swagger.v3.oas.annotations.Operation operationAnnotation =
                AnnotatedElementUtils.findMergedAnnotation(
                        handlerMethod.getMethod(),
                        io.swagger.v3.oas.annotations.Operation.class
                );
        if (operationAnnotation == null
                || operationAnnotation.operationId().isBlank()
                || operation.getOperationId() == null
                || operation.getOperationId().isBlank()) {
            throw new IllegalArgumentException(
                    "catalogued OpenAPI operationId is required"
            );
        }

        EgonApiCatalog catalog =
                AnnotatedElementUtils.findMergedAnnotation(
                        handlerMethod.getBeanType(), EgonApiCatalog.class
                );
        if (catalog != null
                && !properties.getPublishedGroups().contains(
                        catalog.interfaceGroupCode()
                )) {
            throw new IllegalArgumentException(
                    "catalog interface group is not published: "
                            + catalog.interfaceGroupCode()
            );
        }

        Map<String, Object> extension = new LinkedHashMap<>();
        extension.put("version", 1);
        if (catalog != null) {
            extension.put("catalog", catalogExtension(catalog));
        }

        EgonGatewayPolicy policy =
                AnnotatedElementUtils.findMergedAnnotation(
                        handlerMethod.getMethod(), EgonGatewayPolicy.class
                );
        if (policy != null) {
            extension.put("policy", policyExtension(policy, handlerMethod));
        }

        EgonMcpTool mcpTool =
                AnnotatedElementUtils.findMergedAnnotation(
                        handlerMethod.getMethod(), EgonMcpTool.class
                );
        if (mcpTool != null && mcpTool.enabled()) {
            extension.put("mcp", mcpExtension(mcpTool, operation));
        }

        operation.addExtension("x-egon", extension);
        return operation;
    }

    private Map<String, Object> catalogExtension(EgonApiCatalog catalog) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("businessDomainCode", catalog.businessDomainCode());
        value.put("businessDomainName", catalog.businessDomainName());
        value.put("entityDomainCode", catalog.entityDomainCode());
        value.put("entityDomainName", catalog.entityDomainName());
        value.put("interfaceGroupCode", catalog.interfaceGroupCode());
        return value;
    }

    private Map<String, Object> policyExtension(
            EgonGatewayPolicy policy,
            HandlerMethod handlerMethod) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("owner", policy.owner());
        value.put("exposure", policy.exposure().name());
        value.put(
                "idempotent",
                resolveIdempotency(policy, handlerMethod)
        );
        return value;
    }

    private boolean resolveIdempotency(
            EgonGatewayPolicy policy,
            HandlerMethod handlerMethod) {
        return switch (policy.idempotency()) {
            case TRUE -> true;
            case FALSE -> false;
            case AUTO -> resolveHttpMethods(handlerMethod).stream()
                    .allMatch(this::isIdempotentMethod);
        };
    }

    private Set<RequestMethod> resolveHttpMethods(HandlerMethod handlerMethod) {
        Set<RequestMethod> methods = new TreeSet<>();
        RequestMapping typeMapping =
                AnnotatedElementUtils.findMergedAnnotation(
                        handlerMethod.getBeanType(), RequestMapping.class
                );
        RequestMapping methodMapping =
                AnnotatedElementUtils.findMergedAnnotation(
                        handlerMethod.getMethod(), RequestMapping.class
                );
        if (typeMapping != null) {
            methods.addAll(Arrays.asList(typeMapping.method()));
        }
        if (methodMapping != null && methodMapping.method().length > 0) {
            methods.clear();
            methods.addAll(Arrays.asList(methodMapping.method()));
        }
        return methods;
    }

    private boolean isIdempotentMethod(RequestMethod method) {
        return switch (method) {
            case GET, HEAD, OPTIONS, PUT, DELETE -> true;
            case POST, PATCH, TRACE -> false;
        };
    }

    private Map<String, Object> mcpExtension(
            EgonMcpTool mcpTool,
            Operation operation) {
        String serverCode = mcpTool.serverCode().trim();
        if (serverCode.isEmpty()) {
            throw new IllegalArgumentException(
                    "enabled MCP tool serverCode is required"
            );
        }
        String name = mcpTool.name().isBlank()
                ? operation.getOperationId()
                : mcpTool.name().trim();
        Set<String> permissions = new TreeSet<>();
        for (String permission : mcpTool.permissions()) {
            if (permission == null || permission.isBlank()) {
                throw new IllegalArgumentException(
                        "MCP permissions must be non-blank"
                );
            }
            permissions.add(permission.trim());
        }
        McpRiskLevel riskLevel = mcpTool.riskLevel();
        if (riskLevel == null) {
            throw new IllegalArgumentException(
                    "enabled MCP tool riskLevel is required"
            );
        }
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("serverCode", serverCode);
        value.put("name", name);
        value.put("permissions", List.copyOf(permissions));
        value.put("riskLevel", riskLevel.name());
        return value;
    }
}
