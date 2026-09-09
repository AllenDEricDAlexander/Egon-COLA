package top.egon.cola.component.yuheng.admin.openapi.validation.rule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationResult;
import top.egon.cola.component.yuheng.admin.openapi.validation.GatewayOpenApiValidationRule;

import java.util.Locale;

/**
 * Validates the transport envelope before OpenAPI graph rules run.
 */
@Slf4j
@Component("gatewayOpenApiDocumentEnvelopeValidator")
@Qualifier("gatewayOpenApiValidationRules")
@RequiredArgsConstructor
public class GatewayOpenApiDocumentEnvelopeValidator
        implements GatewayOpenApiValidationRule {

    @Override
    public int order() {
        return 10;
    }

    @Override
    public GatewayOpenApiValidationResult validate(
            GatewayOpenApiDocumentDTO document) {
        if (document == null) {
            return invalid(
                    "YUHENG_OPENAPI_DOCUMENT_MISSING",
                    "OpenAPI document is missing"
            );
        }
        if (document.statusCode() < 200 || document.statusCode() >= 300) {
            return invalid(
                    "YUHENG_OPENAPI_HTTP_STATUS",
                    "provider returned an unacceptable HTTP status"
            );
        }
        String mediaType = document.contentType()
                .split(";", 2)[0]
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!"application/json".equals(mediaType)) {
            return invalid(
                    "YUHENG_OPENAPI_CONTENT_TYPE",
                    "provider response is not JSON"
            );
        }
        if (document.rawBytes().length == 0
                || document.rawBytes().length
                > GatewayOpenApiDocumentDTO.MAX_DOCUMENT_BYTES) {
            return invalid(
                    "YUHENG_OPENAPI_DOCUMENT_TOO_LARGE",
                    "provider OpenAPI document exceeds the supported limit"
            );
        }
        if (document.documentJson() == null
                || !document.documentJson().isObject()) {
            return invalid(
                    "YUHENG_OPENAPI_DOCUMENT_OBJECT",
                    "provider OpenAPI document must be a JSON object"
            );
        }
        return GatewayOpenApiValidationResult.passed();
    }

    private GatewayOpenApiValidationResult invalid(
            String code,
            String message) {
        return GatewayOpenApiValidationResult.invalid(code, message);
    }
}
