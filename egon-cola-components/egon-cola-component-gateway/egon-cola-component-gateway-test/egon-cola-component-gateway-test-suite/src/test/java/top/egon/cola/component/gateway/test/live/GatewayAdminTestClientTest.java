package top.egon.cola.component.gateway.test.live;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayAdminTestClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsBearerAuthenticatedTypedAdminRequests() throws Exception {
        AtomicReference<RequestCapture> request = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> respond(exchange, request));
        server.start();
        GatewayAdminTestClient client = new GatewayAdminTestClient(
                baseUri(),
                "test-token"
        );

        assertThat(client.createApplication(Map.of(
                "applicationCode", "orders"
        )).required("id").asText()).isEqualTo("created-id");

        assertThat(request.get()).satisfies(capture -> {
            assertThat(capture.method()).isEqualTo("POST");
            assertThat(capture.path())
                    .isEqualTo("/api/v1/gateway/admin/applications");
            assertThat(capture.authorization())
                    .isEqualTo("Bearer test-token");
            assertThat(capture.body()).contains("\"applicationCode\":\"orders\"");
        });

        client.runtimeConsistency("group/one");
        assertThat(request.get().path()).isEqualTo(
                "/api/v1/gateway/admin/gateway-groups/group%2Fone/"
                        + "runtime-consistency"
        );

        client.getDraft("group/one");
        assertThat(request.get().method()).isEqualTo("GET");
        assertThat(request.get().path()).isEqualTo(
                "/api/v1/gateway/admin/gateway-groups/group%2Fone/draft"
        );

        client.providerInstances(
                "test",
                "gateway-live",
                "HTTP",
                "orders",
                "default",
                "1.0.0-live"
        );
        assertThat(request.get().query()).isEqualTo(
                "env=test&namespace=gateway-live&protocol=HTTP"
                        + "&serviceName=orders&group=default"
                        + "&version=1.0.0-live"
        );
    }

    @Test
    void reportsMethodUriStatusAndBodyForAdminFailure() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            byte[] body = "denied".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(403, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        GatewayAdminTestClient client = new GatewayAdminTestClient(
                baseUri(),
                "test-token"
        );

        assertThatThrownBy(() -> client.validateDraft("group-1"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("POST")
                .hasMessageContaining("403")
                .hasMessageContaining("denied");
    }

    private URI baseUri() {
        return URI.create(
                "http://127.0.0.1:" + server.getAddress().getPort()
        );
    }

    private void respond(
            HttpExchange exchange,
            AtomicReference<RequestCapture> capture) throws IOException {
        capture.set(new RequestCapture(
                exchange.getRequestMethod(),
                exchange.getRequestURI().getRawPath(),
                exchange.getRequestURI().getRawQuery(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                )
        ));
        byte[] body = "{\"id\":\"created-id\"}".getBytes(
                StandardCharsets.UTF_8
        );
        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json"
        );
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private record RequestCapture(
            String method,
            String path,
            String query,
            String authorization,
            String body
    ) {
    }
}
