package top.egon.cola.component.gateway.test.mcp.remote;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StableConformanceConfigurationTest {

    @Test
    void exposesTheOfficialStableConformanceCapabilityNames() {
        assertEquals("/conformance/stable", StableConformanceConfiguration.MCP_ENDPOINT);
        assertEquals(Set.of(
                "test_simple_text",
                "test_image_content",
                "test_audio_content",
                "test_embedded_resource",
                "test_multiple_content_types",
                "test_tool_with_logging",
                "test_error_handling",
                "test_tool_with_progress",
                "test_sampling",
                "test_elicitation",
                "test_elicitation_sep1034_defaults",
                "json_schema_2020_12_tool",
                "test_elicitation_sep1330_enums"
        ), StableConformanceConfiguration.toolNames());
    }
}
