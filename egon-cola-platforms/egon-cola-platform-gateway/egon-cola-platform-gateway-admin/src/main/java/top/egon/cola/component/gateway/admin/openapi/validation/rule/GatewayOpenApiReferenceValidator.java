package top.egon.cola.component.gateway.admin.openapi.validation.rule;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.gateway.admin.openapi.validation.GatewayOpenApiValidationResult;
import top.egon.cola.component.gateway.admin.openapi.validation.GatewayOpenApiValidationRule;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Rejects remote, file and other non-local OpenAPI references.
 */
@Slf4j
@Component("gatewayOpenApiReferenceValidator")
@Qualifier("gatewayOpenApiValidationRules")
@RequiredArgsConstructor
public class GatewayOpenApiReferenceValidator
        implements GatewayOpenApiValidationRule {

    @Override
    public int order() {
        return 30;
    }

    @Override
    public GatewayOpenApiValidationResult validate(
            GatewayOpenApiDocumentDTO document) {
        if (document == null || document.documentJson() == null) {
            return GatewayOpenApiValidationResult.invalid(
                    "GATEWAY_OPENAPI_DOCUMENT_MISSING",
                    "OpenAPI document is missing"
            );
        }
        JsonNode root = document.documentJson();
        Deque<JsonNode> nodes = new ArrayDeque<>();
        nodes.push(root);
        while (!nodes.isEmpty()) {
            JsonNode node = nodes.pop();
            if (node.isObject()) {
                JsonNode reference = node.get("$ref");
                if (reference != null) {
                    if (!reference.isTextual()
                            || !localReference(reference.asText())) {
                        return GatewayOpenApiValidationResult.invalid(
                                "GATEWAY_OPENAPI_EXTERNAL_REF",
                                "OpenAPI references must remain local"
                        );
                    }
                    if (root.at(
                            reference.asText().substring(1)
                    ).isMissingNode()) {
                        return GatewayOpenApiValidationResult.invalid(
                                "GATEWAY_OPENAPI_REF_NOT_FOUND",
                                "OpenAPI local reference target was not found"
                        );
                    }
                }
                Iterator<JsonNode> values = node.elements();
                while (values.hasNext()) {
                    nodes.push(values.next());
                }
            } else if (node.isArray()) {
                Iterator<JsonNode> values = node.elements();
                while (values.hasNext()) {
                    nodes.push(values.next());
                }
            }
        }
        return GatewayOpenApiValidationResult.passed();
    }

    private static boolean localReference(String value) {
        return value != null
                && value.startsWith("#/")
                && !value.contains("../")
                && !value.contains("/..")
                && !value.contains("\\");
    }
}
