package top.egon.cola.component.gateway.admin.openapi.converter;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import top.egon.cola.component.common.core.converter.BaseConverter;
import top.egon.cola.component.gateway.admin.openapi.GatewayOpenApiValidationTestFixture;
import top.egon.cola.component.gateway.admin.openapi.domain.dto.GatewayOpenApiDefinitionDTO;
import top.egon.cola.component.gateway.admin.reporting.service.GatewayReportCanonicalizer;
import top.egon.cola.component.gateway.admin.reporting.service.GatewayOperationSchemaValidator;
import top.egon.cola.component.gateway.contract.reporting.GatewayDefinitionSourceTypeEnum;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayOpenApiDefinitionConverterTest {

    private final GatewayOpenApi31ContractAdapter adapter =
            new GatewayOpenApi31ContractAdapter();

    private final GatewayOpenApiDefinitionConverter converter =
            Mappers.getMapper(GatewayOpenApiDefinitionConverter.class);

    @Test
    void mapsNormalizedDefinitionToReportV2AndPreservesCanonicalProvenance() {
        GatewayOpenApiDefinitionDTO source = adapter.adapt(
                GatewayOpenApiValidationTestFixture.document()
        );

        GatewayInterfaceDefinitionReport report = converter.toTarget(source);
        new GatewayReportCanonicalizer().verify(report);
        GatewayInterfaceDefinitionReport.InterfaceGroup group = report
                .businessDomains().getFirst().entityDomains().getFirst()
                .interfaceGroups().getFirst();

        assertThat(report.contractVersion()).isEqualTo("v2");
        assertThat(group.sourceType())
                .isEqualTo(GatewayDefinitionSourceTypeEnum.OPENAPI31);
        assertThat(group.protocol()).isEqualTo("HTTP");
        assertThat(group.attributes()).containsEntry(
                "openapiGroup",
                "orders"
        );
        assertThat(group.operations()).isNotEmpty();
        GatewayOperationSchemaValidator schemaValidator =
                new GatewayOperationSchemaValidator(new com.fasterxml.jackson.databind.ObjectMapper());
        group.operations().forEach(schemaValidator::validate);
    }

    @Test
    void converterImplementsBaseConverterAndRoundTripsNormalizedGraph() {
        assertThat(converter).isInstanceOf(BaseConverter.class);
        GatewayOpenApiDefinitionDTO source = adapter.adapt(
                GatewayOpenApiValidationTestFixture.document()
        );
        GatewayOpenApiDefinitionDTO roundTrip = converter.toSource(
                converter.toTarget(source)
        );

        assertThat(roundTrip.canonicalSha256())
                .isEqualTo(source.canonicalSha256());
        assertThat(roundTrip.businessDomains())
                .isEqualTo(source.businessDomains());
    }
}
