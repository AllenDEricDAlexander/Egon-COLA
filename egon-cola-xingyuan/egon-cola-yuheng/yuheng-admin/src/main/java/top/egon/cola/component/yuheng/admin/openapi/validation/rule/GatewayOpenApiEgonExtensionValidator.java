package top.egon.cola.component.yuheng.admin.openapi.validation.rule;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationResult;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationRule;

import java.util.Iterator;

/**
 * Validates versioned x-egon service and operation governance extensions.
 */
@Slf4j
@Component("gatewayOpenApiEgonExtensionValidator")
@Qualifier("gatewayOpenApiValidationRules")
@RequiredArgsConstructor
public class GatewayOpenApiEgonExtensionValidator
        implements GatewayOpenApiValidationRule {

    @Override
    public int order() {
        return 50;
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
        JsonNode root = document.documentJson();
        JsonNode service = root.get("x-egon-service");
        if (service == null || !service.isObject()
                || !versionOne(service.get("version"))) {
            return invalid(
                    "GATEWAY_OPENAPI_EGON_SERVICE",
                    "x-egon-service version 1 is required"
            );
        }
        if (!matches(service, "bizCode", document.candidate().bizCode())
                || !matches(
                service,
                "applicationCode",
                document.candidate().applicationCode()
        )
                || !matches(
                service,
                "artifactVersion",
                document.candidate().artifactVersion()
        )) {
            return invalid(
                    "GATEWAY_OPENAPI_SERVICE_MISMATCH",
                    "x-egon-service does not match the trusted Provider identity"
            );
        }
        if (!matches(service, "buildId", document.candidate().buildId())) {
            return invalid(
                    "GATEWAY_OPENAPI_BUILD_MISMATCH",
                    "x-egon-service buildId does not match the DDC manifest"
            );
        }
        if (!matches(
                service,
                "openapiGroup",
                document.candidate().openapiGroup()
        )) {
            return invalid(
                    "GATEWAY_OPENAPI_GROUP_MISMATCH",
                    "x-egon-service openapiGroup does not match the target"
            );
        }

        JsonNode paths = root.get("paths");
        if (paths != null && paths.isObject()) {
            Iterator<JsonNode> pathItems = paths.elements();
            while (pathItems.hasNext()) {
                JsonNode pathItem = pathItems.next();
                if (!pathItem.isObject()) {
                    continue;
                }
                Iterator<JsonNode> operations = pathItem.elements();
                Iterator<String> fields = pathItem.fieldNames();
                while (operations.hasNext() && fields.hasNext()) {
                    String field = fields.next();
                    JsonNode operation = operations.next();
                    if (!isHttpMethod(field) || !operation.isObject()) {
                        continue;
                    }
                    JsonNode extension = operation.get("x-egon");
                    if (extension != null
                            && (!extension.isObject()
                            || !versionOne(extension.get("version")))) {
                        return invalid(
                                "GATEWAY_OPENAPI_EXTENSION_VERSION",
                                "x-egon operation extension must use version 1"
                        );
                    }
                }
            }
        }
        return GatewayOpenApiValidationResult.passed();
    }

    private static boolean matches(
            JsonNode node,
            String field,
            String expected) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual()
                && expected.equals(value.asText().trim());
    }

    private static boolean versionOne(JsonNode value) {
        return value != null && value.isInt() && value.intValue() == 1;
    }

    private static boolean isHttpMethod(String value) {
        return java.util.Set.of(
                "get", "put", "post", "delete", "options", "head",
                "patch", "trace"
        ).contains(value);
    }

    private GatewayOpenApiValidationResult invalid(
            String code,
            String message) {
        return GatewayOpenApiValidationResult.invalid(code, message);
    }
}
