package top.egon.cola.archetype.source.agent.infrastructure.research.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;

/** Server-owned MCP SSE connection settings; values are never part of the research request. */
public record McpResearchToolProperties(
        @NotBlank String baseUri,
        @NotBlank String sseEndpoint,
        @NotBlank String apiKey,
        @NotNull Duration requestTimeout) {

    public McpResearchToolProperties {
        baseUri = normalize(baseUri);
        sseEndpoint = normalize(sseEndpoint);
        apiKey = normalize(apiKey);
        requestTimeout = requestTimeout == null ? Duration.ofSeconds(15) : requestTimeout;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
