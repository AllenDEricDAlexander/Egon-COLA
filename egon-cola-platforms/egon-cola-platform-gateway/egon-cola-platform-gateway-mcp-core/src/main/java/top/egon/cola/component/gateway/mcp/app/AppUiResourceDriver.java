package top.egon.cola.component.gateway.mcp.app;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import top.egon.cola.component.gateway.mcp.resource.McpResourceDriver;

import java.util.Objects;

/**
 * Resource driver for verified MCP App HTML artifacts.
 */
public final class AppUiResourceDriver implements McpResourceDriver {

    public static final String DRIVER_TYPE = "APP_UI";

    private final McpAppRuntime runtime;

    public AppUiResourceDriver(McpAppRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
    }

    @Override
    public String driverType() {
        return DRIVER_TYPE;
    }

    @Override
    public Publisher<Content> read(ReadRequest request) {
        return Mono.fromSupplier(() -> {
            McpAppRuntime.AppContent app = runtime.read(
                    request.serverCode(),
                    request.uri()
            );
            Content bounded = McpResourceDriver.bounded(
                    request,
                    app.content(),
                    true
            );
            return new Content(
                    bounded.uri(),
                    bounded.mimeType(),
                    bounded.data(),
                    true,
                    app.responseMetadata()
            );
        });
    }
}
