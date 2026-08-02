package top.egon.cola.component.gateway.mcp.resource;

import reactor.core.publisher.Mono;

import java.util.Base64;

import static top.egon.cola.component.gateway.mcp.resource.McpResourceDriver.bounded;
import static top.egon.cola.component.gateway.mcp.resource.McpResourceDriver.rejected;

public final class StaticBlobResourceDriver implements McpResourceDriver {

    public static final String DRIVER_TYPE = "STATIC_BLOB";

    @Override
    public String driverType() {
        return DRIVER_TYPE;
    }

    @Override
    public Mono<Content> read(ReadRequest request) {
        String encoded = request.configuration().get("base64");
        if (encoded == null || encoded.isBlank()) {
            throw rejected("MCP static blob content is not configured");
        }
        try {
            return Mono.just(bounded(
                    request,
                    Base64.getDecoder().decode(encoded),
                    false
            ));
        } catch (IllegalArgumentException failure) {
            throw rejected("MCP static blob content is invalid");
        }
    }
}
