package top.egon.cola.component.gateway.admin.openapi.validation;

import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;

/**
 * Typed limits applied before OpenAPI graph adaptation.
 *
 * @param maximumDocumentBytes maximum raw response size
 * @param maximumOperations maximum operations in one Group document
 * @param maximumSchemaNodes maximum schema object nodes
 * @param maximumReferenceDepth maximum local reference pointer depth
 */
public record GatewayOpenApiValidationLimits(
        int maximumDocumentBytes,
        int maximumOperations,
        int maximumSchemaNodes,
        int maximumReferenceDepth
) {

    public GatewayOpenApiValidationLimits {
        if (maximumDocumentBytes <= 0
                || maximumDocumentBytes
                > GatewayOpenApiDocumentDTO.MAX_DOCUMENT_BYTES) {
            throw new IllegalArgumentException(
                    "maximumDocumentBytes is outside the supported range"
            );
        }
        if (maximumOperations <= 0
                || maximumSchemaNodes <= 0
                || maximumReferenceDepth <= 0) {
            throw new IllegalArgumentException(
                    "OpenAPI validation limits must be positive"
            );
        }
    }

    public static GatewayOpenApiValidationLimits defaults() {
        return new GatewayOpenApiValidationLimits(
                GatewayOpenApiDocumentDTO.MAX_DOCUMENT_BYTES,
                5_000,
                50_000,
                64
        );
    }
}
