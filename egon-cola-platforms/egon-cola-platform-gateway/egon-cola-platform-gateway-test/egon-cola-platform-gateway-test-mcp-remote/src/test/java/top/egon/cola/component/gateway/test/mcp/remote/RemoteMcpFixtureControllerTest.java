package top.egon.cola.component.gateway.test.mcp.remote;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RemoteMcpFixtureControllerTest {

    @Test
    void acceptsJsonRpcNotificationsWithoutWritingAResponseBody() {
        var response = new RemoteMcpFixtureController().exchange(
                Map.of(
                        "jsonrpc", "2.0",
                        "method", "notifications/initialized"
                ),
                new HttpHeaders()
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }
}
