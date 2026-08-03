package top.egon.cola.component.gateway.mcp.resource;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static top.egon.cola.component.gateway.mcp.resource.McpResourceDriver.bounded;
import static top.egon.cola.component.gateway.mcp.resource.McpResourceDriver.rejected;

public final class StaticTextResourceDriver implements McpResourceDriver {

    public static final String DRIVER_TYPE = "STATIC_TEXT";

    @Override
    public String driverType() {
        return DRIVER_TYPE;
    }

    @Override
    public Mono<Content> read(ReadRequest request) {
        String content = request.configuration().get("content");
        if (content == null) {
            content = request.configuration().get("text");
        }
        if (content == null) {
            throw rejected("MCP static text content is not configured");
        }
        return Mono.just(bounded(
                request,
                content.getBytes(StandardCharsets.UTF_8),
                true
        ));
    }
}
