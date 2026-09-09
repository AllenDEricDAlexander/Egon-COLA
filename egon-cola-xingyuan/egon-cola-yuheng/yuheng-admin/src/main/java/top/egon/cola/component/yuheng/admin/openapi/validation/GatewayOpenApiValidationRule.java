package top.egon.cola.component.yuheng.admin.openapi.validation;

import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;

/**
 * One ordered, side-effect-free OpenAPI document validation responsibility.
 */
public interface GatewayOpenApiValidationRule {

    /**
     * Returns the stable execution order for this rule.
     *
     * @return positive order
     */
    int order();

    /**
     * Validates one bounded document without network or persistence access.
     *
     * @param document acquired document
     * @return valid or classified failure
     */
    GatewayOpenApiValidationResult validate(
            GatewayOpenApiDocumentDTO document);
}
