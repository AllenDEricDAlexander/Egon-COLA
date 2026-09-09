package top.egon.cola.component.yuheng.admin.openapi.validation.rule;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationLimits;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationResult;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationRule;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Applies bounded document, operation, schema and local-reference limits.
 */
@Slf4j
@Component("gatewayOpenApiLimitValidator")
@Qualifier("gatewayOpenApiValidationRules")
@RequiredArgsConstructor
public class GatewayOpenApiLimitValidator implements GatewayOpenApiValidationRule {

    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "put", "post", "delete", "options", "head", "patch", "trace"
    );

    @Qualifier("gatewayOpenApiValidationLimits")
    private final GatewayOpenApiValidationLimits limits;

    @Override
    public int order() {
        return 40;
    }

    @Override
    public GatewayOpenApiValidationResult validate(
            GatewayOpenApiDocumentDTO document) {
        if (document == null) {
            return GatewayOpenApiValidationResult.invalid(
                    "GATEWAY_OPENAPI_DOCUMENT_MISSING",
                    "OpenAPI document is missing"
            );
        }
        if (document.rawBytes().length > limits.maximumDocumentBytes()) {
            return invalid(
                    "GATEWAY_OPENAPI_DOCUMENT_TOO_LARGE",
                    "OpenAPI document exceeds the configured byte limit"
            );
        }
        JsonNode root = document.documentJson();
        int operations = operationCount(root);
        if (operations > limits.maximumOperations()) {
            return invalid(
                    "GATEWAY_OPENAPI_OPERATION_LIMIT",
                    "OpenAPI operation count exceeds the configured limit"
            );
        }
        int schemaNodes = schemaNodeCount(root);
        if (schemaNodes > limits.maximumSchemaNodes()) {
            return invalid(
                    "GATEWAY_OPENAPI_SCHEMA_LIMIT",
                    "OpenAPI schema count exceeds the configured limit"
            );
        }
        int referenceDepth = referenceDepth(root);
        if (referenceDepth > limits.maximumReferenceDepth()) {
            return invalid(
                    "GATEWAY_OPENAPI_REFERENCE_DEPTH",
                    "OpenAPI reference depth exceeds the configured limit"
            );
        }
        return GatewayOpenApiValidationResult.passed();
    }

    private static int operationCount(JsonNode root) {
        JsonNode paths = root == null ? null : root.get("paths");
        if (paths == null || !paths.isObject()) {
            return 0;
        }
        int count = 0;
        Iterator<JsonNode> items = paths.elements();
        while (items.hasNext()) {
            JsonNode item = items.next();
            if (!item.isObject()) {
                continue;
            }
            Iterator<String> fields = item.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (HTTP_METHODS.contains(field)
                        && item.get(field).isObject()) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int schemaNodeCount(JsonNode root) {
        JsonNode schemas = root == null || !root.isObject()
                ? null
                : root.path("components").path("schemas");
        if (schemas == null || !schemas.isObject()) {
            return 0;
        }
        Deque<JsonNode> nodes = new ArrayDeque<>();
        schemas.elements().forEachRemaining(nodes::add);
        int count = 0;
        while (!nodes.isEmpty()) {
            JsonNode node = nodes.remove();
            if (node.isObject()) {
                count++;
            }
            if (node.isContainerNode()) {
                node.elements().forEachRemaining(nodes::add);
            }
        }
        return count;
    }

    private static int referenceDepth(JsonNode root) {
        if (root == null) {
            return 0;
        }
        Deque<JsonNode> nodes = new ArrayDeque<>();
        Deque<Integer> depths = new ArrayDeque<>();
        nodes.push(root);
        depths.push(0);
        int max = 0;
        while (!nodes.isEmpty()) {
            JsonNode node = nodes.pop();
            int depth = depths.pop();
            if (node.isObject()) {
                JsonNode reference = node.get("$ref");
                if (reference != null && reference.isTextual()) {
                    max = Math.max(
                            max,
                            referenceChainDepth(root, reference.asText())
                    );
                }
                Iterator<JsonNode> values = node.elements();
                while (values.hasNext()) {
                    nodes.push(values.next());
                    depths.push(depth + 1);
                }
            } else if (node.isArray()) {
                Iterator<JsonNode> values = node.elements();
                while (values.hasNext()) {
                    nodes.push(values.next());
                    depths.push(depth + 1);
                }
            }
        }
        return max;
    }

    private static int referenceChainDepth(JsonNode root, String reference) {
        if (reference == null || !reference.startsWith("#/")) {
            return 0;
        }
        Set<String> visited = new HashSet<>();
        String current = reference;
        int depth = 0;
        while (current != null
                && current.startsWith("#/")
                && visited.add(current)) {
            JsonNode target = root.at(current.substring(1));
            if (target.isMissingNode()) {
                break;
            }
            depth++;
            JsonNode next = target.isObject()
                    ? target.get("$ref")
                    : null;
            current = next != null && next.isTextual()
                    ? next.asText()
                    : null;
        }
        return depth;
    }

    private GatewayOpenApiValidationResult invalid(
            String code,
            String message) {
        return GatewayOpenApiValidationResult.invalid(code, message);
    }

}
