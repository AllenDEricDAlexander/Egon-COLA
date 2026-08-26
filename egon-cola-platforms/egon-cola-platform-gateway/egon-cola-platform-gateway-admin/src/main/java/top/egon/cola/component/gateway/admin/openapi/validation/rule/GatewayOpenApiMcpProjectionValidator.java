package top.egon.cola.component.gateway.admin.openapi.validation.rule;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.gateway.admin.openapi.validation.GatewayOpenApiValidationResult;
import top.egon.cola.component.gateway.admin.openapi.validation.GatewayOpenApiValidationRule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Validates only explicitly enabled x-egon.mcp projections.
 */
@Slf4j
@Component("gatewayOpenApiMcpProjectionValidator")
@Qualifier("gatewayOpenApiValidationRules")
@RequiredArgsConstructor
public class GatewayOpenApiMcpProjectionValidator
        implements GatewayOpenApiValidationRule {

    private static final java.util.Set<String> METHODS = java.util.Set.of(
            "get", "put", "post", "delete", "options", "head", "patch", "trace"
    );

    private static final java.util.Set<String> RISK_LEVELS = java.util.Set.of(
            "LOW", "MEDIUM", "HIGH", "CRITICAL"
    );

    @Override
    public int order() {
        return 60;
    }

    @Override
    public GatewayOpenApiValidationResult validate(
            GatewayOpenApiDocumentDTO document) {
        if (document == null || document.documentJson() == null) {
            return invalid(
                    "GATEWAY_OPENAPI_DOCUMENT_MISSING",
                    "OpenAPI document is missing"
            );
        }
        JsonNode paths = document.documentJson().get("paths");
        if (paths == null || !paths.isObject()) {
            return GatewayOpenApiValidationResult.passed();
        }
        Iterator<JsonNode> pathItems = paths.elements();
        while (pathItems.hasNext()) {
            JsonNode pathItem = pathItems.next();
            if (!pathItem.isObject()) {
                continue;
            }
            Iterator<String> fields = pathItem.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                JsonNode operation = pathItem.get(field);
                if (!METHODS.contains(field) || !operation.isObject()) {
                    continue;
                }
                JsonNode extension = operation.path("x-egon").path("mcp");
                if (extension.isMissingNode() || extension.isNull()) {
                    continue;
                }
                if (!extension.isObject()) {
                    return invalid(
                            "GATEWAY_OPENAPI_MCP_EXTENSION",
                            "x-egon.mcp must be an object"
                    );
                }
                JsonNode enabled = extension.get("enabled");
                if (enabled != null && !enabled.isBoolean()) {
                    return invalid(
                            "GATEWAY_OPENAPI_MCP_EXTENSION",
                            "x-egon.mcp enabled must be boolean"
                    );
                }
                if (enabled != null && !enabled.asBoolean()) {
                    continue;
                }
                if (text(extension, "serverCode") == null) {
                    return invalid(
                            "GATEWAY_OPENAPI_MCP_SERVER",
                            "enabled MCP exposure requires serverCode"
                    );
                }
                String risk = text(extension, "riskLevel");
                if (risk == null || !RISK_LEVELS.contains(risk)) {
                    return invalid(
                            "GATEWAY_OPENAPI_MCP_RISK",
                            "enabled MCP exposure requires a valid riskLevel"
                    );
                }
                GatewayOpenApiValidationResult permissions = permissions(
                        extension
                );
                if (!permissions.valid()) {
                    return permissions;
                }
                GatewayOpenApiValidationResult restrictions = restrictions(
                        pathItem,
                        operation
                );
                if (!restrictions.valid()) {
                    return restrictions;
                }
            }
        }
        return GatewayOpenApiValidationResult.passed();
    }

    private static GatewayOpenApiValidationResult permissions(JsonNode extension) {
        JsonNode values = extension.get("permissions");
        if (values == null || values.isNull()) {
            return GatewayOpenApiValidationResult.passed();
        }
        if (!values.isArray()) {
            return invalid(
                    "GATEWAY_OPENAPI_MCP_PERMISSIONS",
                    "MCP permissions must be an ordered string array"
            );
        }
        List<String> permissions = new ArrayList<>();
        Iterator<JsonNode> nodes = values.elements();
        while (nodes.hasNext()) {
            JsonNode node = nodes.next();
            if (!node.isTextual() || node.asText().isBlank()) {
                return invalid(
                        "GATEWAY_OPENAPI_MCP_PERMISSIONS",
                        "MCP permissions must contain nonblank strings"
                );
            }
            permissions.add(node.asText().trim());
        }
        List<String> sorted = permissions.stream()
                .sorted(Comparator.naturalOrder())
                .toList();
        if (!sorted.equals(permissions)
                || sorted.stream().distinct().count() != sorted.size()) {
            return invalid(
                    "GATEWAY_OPENAPI_MCP_PERMISSIONS",
                    "MCP permissions must be sorted and unique"
            );
        }
        return GatewayOpenApiValidationResult.passed();
    }

    private static GatewayOpenApiValidationResult restrictions(
            JsonNode pathItem,
            JsonNode operation) {
        JsonNode requestBody = operation.get("requestBody");
        if (requestBody != null && requestBody.isObject()) {
            Iterator<String> contentTypes = requestBody.path("content")
                    .fieldNames();
            while (contentTypes.hasNext()) {
                if (contentTypes.next().toLowerCase(Locale.ROOT)
                        .startsWith("multipart/")) {
                    return invalid(
                            "GATEWAY_OPENAPI_MCP_MULTIPART",
                            "multipart MCP operations are not supported"
                    );
                }
            }
        }
        JsonNode responses = operation.get("responses");
        if (responses != null && responses.isObject()) {
            Iterator<JsonNode> values = responses.elements();
            while (values.hasNext()) {
                JsonNode response = values.next();
                if (!response.isObject()) {
                    continue;
                }
                Iterator<String> contentTypes = response.path("content")
                        .fieldNames();
                while (contentTypes.hasNext()) {
                    String contentType = contentTypes.next().toLowerCase(
                            Locale.ROOT
                    );
                    if (contentType.equals("text/event-stream")
                            || contentType.equals("application/x-ndjson")) {
                        return invalid(
                                "GATEWAY_OPENAPI_MCP_STREAMING",
                                "streaming MCP operations are not supported"
                        );
                    }
                }
            }
        }
        GatewayOpenApiValidationResult pathParameters =
                requiredHeaderOrCookie(pathItem.get("parameters"));
        if (!pathParameters.valid()) {
            return pathParameters;
        }
        return requiredHeaderOrCookie(operation.get("parameters"));
    }

    private static GatewayOpenApiValidationResult requiredHeaderOrCookie(
            JsonNode parameters) {
        if (parameters != null && parameters.isArray()) {
            Iterator<JsonNode> values = parameters.elements();
            while (values.hasNext()) {
                JsonNode parameter = values.next();
                if (parameter.isObject()
                        && parameter.path("required").asBoolean(false)
                        && ("header".equals(parameter.path("in").asText())
                        || "cookie".equals(parameter.path("in").asText()))) {
                    return invalid(
                            "GATEWAY_OPENAPI_MCP_REQUIRED_PARAMETER",
                            "required header or cookie MCP parameters are not supported"
                    );
                }
            }
        }
        return GatewayOpenApiValidationResult.passed();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() && !value.asText().isBlank()
                ? value.asText().trim()
                : null;
    }

    private static GatewayOpenApiValidationResult invalid(
            String code,
            String message) {
        return GatewayOpenApiValidationResult.invalid(code, message);
    }

}
