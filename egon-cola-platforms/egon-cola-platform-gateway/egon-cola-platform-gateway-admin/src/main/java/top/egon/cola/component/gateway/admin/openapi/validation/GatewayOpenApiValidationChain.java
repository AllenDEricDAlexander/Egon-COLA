package top.egon.cola.component.gateway.admin.openapi.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;

import java.util.List;
import java.util.Objects;

/**
 * Ordered Chain of Responsibility for the six independent OpenAPI rules.
 *
 * <p>Validation is short-circuiting and side-effect free. A failure can be
 * persisted later as an INVALID snapshot, but this chain never writes it.</p>
 */
@Slf4j
@Validated
@Component("gatewayOpenApiValidationChain")
@RequiredArgsConstructor
public class GatewayOpenApiValidationChain {

    @Qualifier("gatewayOpenApiValidationRules")
    private final List<GatewayOpenApiValidationRule> rules;

    /**
     * Runs rules in stable order and returns the first classified failure.
     *
     * @param document bounded acquired document
     * @return validation result
     */
    public GatewayOpenApiValidationResult validate(
            GatewayOpenApiDocumentDTO document) {
        Objects.requireNonNull(document, "document");
        List<GatewayOpenApiValidationRule> orderedRules =
                Objects.requireNonNull(rules, "rules").stream()
                .map(rule -> Objects.requireNonNull(rule, "rule"))
                .sorted(java.util.Comparator.comparingInt(
                        GatewayOpenApiValidationRule::order
                ))
                .toList();
        java.util.Set<Integer> orders = new java.util.HashSet<>();
        for (GatewayOpenApiValidationRule rule : orderedRules) {
            if (rule.order() <= 0 || !orders.add(rule.order())) {
                throw new IllegalStateException(
                        "OpenAPI validation rule orders must be unique and positive"
                );
            }
        }
        for (GatewayOpenApiValidationRule rule : orderedRules) {
            GatewayOpenApiValidationResult result = rule.validate(document);
            if (!result.valid()) {
                log.warn(
                        "OpenAPI document validation failed code={} app={} "
                                + "build={} group={} instance={}",
                        result.code(),
                        document.candidate().applicationId(),
                        document.candidate().buildId(),
                        document.candidate().openapiGroup(),
                        document.candidate().instanceId()
                );
                return result;
            }
        }
        return GatewayOpenApiValidationResult.passed();
    }
}
