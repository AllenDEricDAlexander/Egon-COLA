package top.egon.cola.component.yuheng.mcp.engine.http.service;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import top.egon.cola.component.yuheng.mcp.engine.config.McpGatewayEngineProperties;
import top.egon.cola.component.yuheng.runtime.http.service.GatewayOutboundHttpResponse;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class McpGatewayHttpServerTest {

    @Test
    void drainsCompletedRequestsAndClosesOnlyItsOwnListener() {
        var handler = mock(McpGatewayHttpDataPlaneHandlerAdapter.class);
        when(handler.handle(any(), any())).thenReturn(Mono.just(GatewayOutboundHttpResponse.text(200, "mcp")));
        var first = new McpGatewayHttpServer(properties(), handler);
        var second = new McpGatewayHttpServer(properties(), handler);
        try {
            first.start();
            second.start();
            assertNotEquals(first.publicPort(), second.publicPort());
            assertEquals("mcp", read(first));
            first.beginDrain();
            assertTrue(first.awaitDrain());
            first.close();
            assertFalse(first.accepting());
            assertEquals("mcp", read(second));
        } finally {
            first.close();
            second.close();
        }
    }

    @Test
    void boundsDrainAndCancelsAnOpenSseBody() throws Exception {
        var subscribed = new CountDownLatch(1);
        var cancelled = new CountDownLatch(1);
        var handler = mock(McpGatewayHttpDataPlaneHandlerAdapter.class);
        when(handler.handle(any(), any())).thenReturn(Mono.just(new GatewayOutboundHttpResponse(
                200, Map.of(), Flux.<org.springframework.core.io.buffer.DataBuffer>never()
                .doOnSubscribe(ignored -> subscribed.countDown()).doFinally(ignored -> cancelled.countDown()))));
        var server = new McpGatewayHttpServer(properties(), handler);
        server.start();
        var request = HttpClient.create().get().uri("http://127.0.0.1:" + server.publicPort() + "/mcp/orders")
                .responseContent().then().onErrorResume(ignored -> Mono.empty()).subscribe();
        try {
            assertTrue(subscribed.await(3, TimeUnit.SECONDS));
            server.beginDrain();
            assertFalse(server.awaitDrain());
            assertTrue(cancelled.await(3, TimeUnit.SECONDS));
            assertTrue(server.awaitDrain());
        } finally {
            request.dispose();
            server.close();
        }
    }

    @Test
    void handlerFailureReleasesDrainAccounting() {
        var handler = mock(McpGatewayHttpDataPlaneHandlerAdapter.class);
        when(handler.handle(any(), any())).thenReturn(Mono.error(new IllegalStateException("test failure")));
        var server = new McpGatewayHttpServer(properties(), handler);
        try {
            server.start();
            Integer status = HttpClient.create().get()
                    .uri("http://127.0.0.1:" + server.publicPort() + "/mcp/orders")
                    .responseSingle((response, body) -> body.thenReturn(response.status().code()))
                    .block(Duration.ofSeconds(3));
            assertEquals(500, status);
            server.beginDrain();
            assertTrue(server.awaitDrain());
        } finally {
            server.close();
        }
    }

    private String read(McpGatewayHttpServer server) {
        return HttpClient.create().get().uri("http://127.0.0.1:" + server.publicPort() + "/mcp/orders")
                .responseSingle((response, body) -> body.asString()).block(Duration.ofSeconds(3));
    }

    private McpGatewayEngineProperties.ListenerProperties properties() {
        return new McpGatewayEngineProperties.ListenerProperties(true, "127.0.0.1", 0, 1024,
                Duration.ofMillis(100),
                new McpGatewayEngineProperties.TlsProperties(false, true, null, null, null, false));
    }
}
