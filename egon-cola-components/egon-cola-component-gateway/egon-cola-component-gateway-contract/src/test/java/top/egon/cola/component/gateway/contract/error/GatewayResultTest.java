package top.egon.cola.component.gateway.contract.error;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayResultTest {

    @Test
    void successHasNoErrorAndFailureRequiresOne() {
        GatewayResult success = GatewayResult.success();
        GatewayError error = new GatewayError(
                "GATEWAY_ROUTE_NOT_FOUND",
                GatewayErrorCategory.ROUTE_NOT_FOUND,
                "No route matched the request",
                "trace-1",
                false,
                Map.of("operationKey", "order-service:http:GET:/orders")
        );
        GatewayResult failure = GatewayResult.failure(error);

        assertTrue(success.successful());
        assertTrue(success.error().isEmpty());
        assertFalse(failure.successful());
        assertEquals(error, failure.error().orElseThrow());
        assertThrows(
                NullPointerException.class,
                () -> GatewayResult.failure(null)
        );
    }

    @Test
    void errorDetailsAreDefensivelyCopied() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("policy", "rate-limit");
        GatewayError error = new GatewayError(
                "GATEWAY_POLICY_REJECTED",
                GatewayErrorCategory.POLICY_REJECTED,
                "Request rejected by gateway policy",
                "trace-2",
                true,
                details
        );

        details.put("providerAddress", "10.0.0.1:8080");

        assertEquals(Map.of("policy", "rate-limit"), error.details());
        assertThrows(
                UnsupportedOperationException.class,
                () -> error.details().put("other", "value")
        );
    }

    @Test
    void errorDetailsRejectNullKeysAndValues() {
        Map<String, String> nullKey = new LinkedHashMap<>();
        nullKey.put(null, "value");
        Map<String, String> nullValue = new LinkedHashMap<>();
        nullValue.put("key", null);

        assertThrows(
                IllegalArgumentException.class,
                () -> error(nullKey)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> error(nullValue)
        );
    }

    @Test
    void internalErrorUsesAStablePublicMessage() {
        GatewayError error = GatewayError.internal("trace-3");

        assertEquals("GATEWAY_INTERNAL_ERROR", error.code());
        assertEquals(GatewayErrorCategory.INTERNAL_ERROR, error.category());
        assertEquals("Gateway request failed", error.message());
        assertEquals("trace-3", error.traceId());
        assertFalse(error.retryable());
        assertTrue(error.details().isEmpty());
    }

    private GatewayError error(Map<String, String> details) {
        return new GatewayError(
                "GATEWAY_REQUEST_INVALID",
                GatewayErrorCategory.REQUEST_INVALID,
                "Invalid request",
                "trace-4",
                false,
                details
        );
    }
}
