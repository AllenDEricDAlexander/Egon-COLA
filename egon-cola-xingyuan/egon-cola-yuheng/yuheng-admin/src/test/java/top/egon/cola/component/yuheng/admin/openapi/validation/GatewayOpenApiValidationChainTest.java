package top.egon.cola.component.yuheng.admin.openapi.validation;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import top.egon.cola.component.yuheng.admin.openapi.GatewayOpenApiValidationTestFixture;
import top.egon.cola.component.yuheng.admin.openapi.domain.dto.GatewayOpenApiDocumentDTO;
import top.egon.cola.component.yuheng.admin.openapi.validation.rule.GatewayOpenApi31Validator;
import top.egon.cola.component.yuheng.admin.openapi.validation.rule.GatewayOpenApiDocumentEnvelopeValidator;
import top.egon.cola.component.yuheng.admin.openapi.validation.rule.GatewayOpenApiEgonExtensionValidator;
import top.egon.cola.component.yuheng.admin.openapi.validation.rule.GatewayOpenApiLimitValidator;
import top.egon.cola.component.yuheng.admin.openapi.validation.rule.GatewayOpenApiMcpProjectionValidator;
import top.egon.cola.component.yuheng.admin.openapi.validation.rule.GatewayOpenApiReferenceValidator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class GatewayOpenApiValidationChainTest {

    @Test
    void rulesAreSortedAndStopAtTheFirstClassifiedFailure() {
        GatewayOpenApiValidationRule later = Mockito.mock(
                GatewayOpenApiValidationRule.class
        );
        GatewayOpenApiValidationRule first = Mockito.mock(
                GatewayOpenApiValidationRule.class
        );
        Mockito.when(later.order()).thenReturn(20);
        Mockito.when(first.order()).thenReturn(10);
        Mockito.when(first.validate(Mockito.any()))
                .thenReturn(GatewayOpenApiValidationResult.invalid(
                        "FIRST_RULE",
                        "first rule failed"
                ));
        GatewayOpenApiValidationChain chain =
                new GatewayOpenApiValidationChain(List.of(later, first));

        GatewayOpenApiValidationResult result = chain.validate(
                GatewayOpenApiValidationTestFixture.document()
        );

        assertThat(result.valid()).isFalse();
        assertThat(result.code()).isEqualTo("FIRST_RULE");
        verify(later, never()).validate(Mockito.any());
    }

    @Test
    void anAllPassingChainReturnsAValidatedResult() {
        GatewayOpenApiValidationChain chain =
                new GatewayOpenApiValidationChain(List.of(
                        new GatewayOpenApiDocumentEnvelopeValidator(),
                        new GatewayOpenApi31Validator(),
                        new GatewayOpenApiReferenceValidator(),
                        new GatewayOpenApiLimitValidator(
                                GatewayOpenApiValidationLimits.defaults()
                        ),
                        new GatewayOpenApiEgonExtensionValidator(),
                        new GatewayOpenApiMcpProjectionValidator()
                ));

        GatewayOpenApiValidationResult result = chain.validate(
                GatewayOpenApiValidationTestFixture.document()
        );

        assertThat(result).isEqualTo(GatewayOpenApiValidationResult.passed());
    }
}
