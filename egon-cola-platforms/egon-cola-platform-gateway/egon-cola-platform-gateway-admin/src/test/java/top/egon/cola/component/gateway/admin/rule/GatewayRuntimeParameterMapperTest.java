package top.egon.cola.component.gateway.admin.rule;

import org.junit.jupiter.api.Test;
import top.egon.cola.component.gateway.contract.reporting.GatewayInterfaceDefinitionReport;
import top.egon.cola.component.gateway.contract.rule.GatewayRuntimeParameter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRuntimeParameterMapperTest {

    @Test
    void mapsReportedParametersOntoTheRuntimeModel() {
        List<GatewayRuntimeParameter> parameters =
                GatewayRuntimeParameterMapper.map(List.of(reported()));

        assertThat(parameters).containsExactly(new GatewayRuntimeParameter(
                "orderId",
                "PATH",
                true,
                "java.lang.String",
                null,
                "the order identifier"
        ));
    }

    @Test
    void mapsParametersThatCameBackFromTheDefinitionJsonColumn() {
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("name", "size");
        stored.put("location", "query");
        stored.put("required", false);
        stored.put("javaTypeDisplay", "java.lang.Integer");
        stored.put("defaultValue", "20");
        stored.put("description", "page size");
        stored.put("schema", Map.of("type", "integer"));
        stored.put("constraints", Map.of("maximum", 100));

        List<GatewayRuntimeParameter> parameters =
                GatewayRuntimeParameterMapper.map(List.of(stored));

        assertThat(parameters).containsExactly(new GatewayRuntimeParameter(
                "size",
                "QUERY",
                false,
                "java.lang.Integer",
                "20",
                "page size"
        ));
    }

    @Test
    void toleratesAbsentBlankAndMalformedEntries() {
        Map<String, Object> unnamed = new HashMap<>();
        unnamed.put("location", "QUERY");

        assertThat(GatewayRuntimeParameterMapper.map(null)).isEmpty();
        assertThat(GatewayRuntimeParameterMapper.map("not-a-list")).isEmpty();
        assertThat(GatewayRuntimeParameterMapper.map(List.of(
                unnamed,
                Map.of("name", "orderId"),
                "junk"
        ))).isEmpty();
    }

    @Test
    void readsRequiredWhetherItIsStoredAsBooleanOrText() {
        assertThat(GatewayRuntimeParameterMapper.map(List.of(Map.of(
                "name", "orderId",
                "location", "PATH",
                "required", "true"
        )))).singleElement()
                .extracting(GatewayRuntimeParameter::required)
                .isEqualTo(true);
    }

    private GatewayInterfaceDefinitionReport.Parameter reported() {
        return new GatewayInterfaceDefinitionReport.Parameter(
                "orderId",
                "PATH",
                true,
                "java.lang.String",
                Map.of("type", "string"),
                null,
                Map.of(),
                "the order identifier"
        );
    }
}
