package top.egon.cola.component.yuheng.admin.openapi.validation.rule;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationResult;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationRule;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Validates the supported OpenAPI 3.1 envelope and operation essentials.
 */
@Slf4j
@Component("gatewayOpenApi31Validator")
@Qualifier("gatewayOpenApiValidationRules")
@RequiredArgsConstructor
public class GatewayOpenApi31Validator implements GatewayOpenApiValidationRule {

    private static final Set<String> HTTP_METHODS = Set.of(
            "get",
            "put",
            "post",
            "delete",
            "options",
            "head",
            "patch",
            "trace"
    );

    @Override
    public int order() {
        return 20;
    }

    @Override
    public GatewayOpenApiValidationResult validate(
            GatewayOpenApiDocumentDTO document) {
        if (document == null || document.documentJson() == null) {
            return invalid(
                    "YUHENG_OPENAPI_DOCUMENT_MISSING",
                    "OpenAPI document is missing"
            );
        }
        JsonNode root = document.documentJson();
        String version = text(root, "openapi");
        if (version == null || !version.matches("3\\.1\\.\\d+")) {
            return invalid(
                    "YUHENG_OPENAPI_SPEC_VERSION",
                    "OpenAPI document must use version 3.1.x"
            );
        }
        JsonNode info = root.get("info");
        if (info == null || !info.isObject()
                || blank(text(info, "title"))
                || blank(text(info, "version"))) {
            return invalid(
                    "YUHENG_OPENAPI_INFO",
                    "OpenAPI info.title and info.version are required"
            );
        }
        JsonNode paths = root.get("paths");
        if (paths == null || !paths.isObject() || paths.isEmpty()) {
            return invalid(
                    "YUHENG_OPENAPI_PATHS",
                    "OpenAPI paths must contain at least one path"
            );
        }
        Set<String> operationIds = new HashSet<>();
        int operations = 0;
        Iterator<JsonNode> pathItems = paths.elements();
        Iterator<String> pathNames = paths.fieldNames();
        while (pathItems.hasNext() && pathNames.hasNext()) {
            String path = pathNames.next();
            JsonNode pathItem = pathItems.next();
            if (!path.startsWith("/") || path.contains("..")
                    || !pathItem.isObject()) {
                return invalid(
                        "YUHENG_OPENAPI_PATH",
                        "OpenAPI path keys must be absolute templates"
                );
            }
            Iterator<String> fields = pathItem.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (!HTTP_METHODS.contains(field)) {
                    continue;
                }
                JsonNode operation = pathItem.get(field);
                if (operation == null || !operation.isObject()) {
                    return invalid(
                            "YUHENG_OPENAPI_OPERATION",
                            "OpenAPI operations must be JSON objects"
                    );
                }
                String operationId = text(operation, "operationId");
                if (blank(operationId)) {
                    return invalid(
                            "YUHENG_OPENAPI_OPERATION_ID",
                            "every OpenAPI operation requires operationId"
                    );
                }
                if (!operationIds.add(operationId)) {
                    return invalid(
                            "YUHENG_OPENAPI_OPERATION_ID",
                            "operationId must be unique within one Group"
                    );
                }
                JsonNode responses = operation.get("responses");
                if (responses == null || !responses.isObject()
                        || responses.isEmpty()) {
                    return invalid(
                            "YUHENG_OPENAPI_RESPONSES",
                            "every OpenAPI operation requires responses"
                    );
                }
                operations++;
            }
        }
        if (operations == 0) {
            return invalid(
                    "YUHENG_OPENAPI_OPERATIONS",
                    "OpenAPI paths must contain an HTTP operation"
            );
        }
        return GatewayOpenApiValidationResult.passed();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isTextual()
                ? value.asText().trim()
                : null;
    }

    private static boolean blank(String value) {
        return value == null || value.isEmpty();
    }

    private GatewayOpenApiValidationResult invalid(
            String code,
            String message) {
        return GatewayOpenApiValidationResult.invalid(code, message);
    }
}
