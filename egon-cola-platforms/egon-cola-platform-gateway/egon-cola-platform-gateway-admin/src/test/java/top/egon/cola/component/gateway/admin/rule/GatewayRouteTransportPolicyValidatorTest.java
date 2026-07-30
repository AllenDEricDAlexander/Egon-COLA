package top.egon.cola.component.gateway.admin.rule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import top.egon.cola.component.gateway.contract.protocol.GatewayProtocol;
import top.egon.cola.component.gateway.core.route.GatewayResponseMode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRouteTransportPolicyValidatorTest {

    private final GatewayRouteTransportPolicyValidator validator =
            new GatewayRouteTransportPolicyValidator();

    @Test
    void reportsMissingHostWithoutApplyingWildcardFallback() {
        Map<String, Object> content = route("POST", Map.of());
        content.remove("host");
        List<GatewayRouteTransportPolicyValidator.ValidationIssue> issues =
                validator.validate(
                        content,
                        GatewayProtocol.HTTP,
                        GatewayResponseMode.TRANSPARENT
                );

        assertThat(issues).contains(new GatewayRouteTransportPolicyValidator
                .ValidationIssue(
                "host",
                "ROUTE_HOST_REQUIRED",
                "Host is required"
        ));
        assertThat(content.values()).doesNotContain("*");
    }

    @Test
    void reportsEveryUnknownTransportEnumAtItsFieldPath() {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("profile", "FUTURE_PROFILE");
        policy.put("transportProtocol", "TCP");
        policy.put("requestBodyMode", "BUFFERED");
        policy.put("responseMode", "CACHED");

        List<String> paths = validator.validate(
                        route("POST", policy),
                        GatewayProtocol.HTTP,
                        GatewayResponseMode.TRANSPARENT
                ).stream()
                .map(GatewayRouteTransportPolicyValidator.ValidationIssue::path)
                .toList();

        assertThat(paths).containsExactly(
                "transportPolicy.profile",
                "transportPolicy.transportProtocol",
                "transportPolicy.requestBodyMode",
                "transportPolicy.responseMode"
        );
    }

    @ParameterizedTest
    @MethodSource("numericRanges")
    void acceptsDocumentedNumericBoundaries(
            String field,
            long minimum,
            long maximum) {
        assertThat(validator.validate(
                route("POST", Map.of(field, minimum)),
                GatewayProtocol.HTTP,
                GatewayResponseMode.TRANSPARENT
        )).isEmpty();
        assertThat(validator.validate(
                route("POST", Map.of(field, maximum)),
                GatewayProtocol.HTTP,
                GatewayResponseMode.TRANSPARENT
        )).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("numericRanges")
    void rejectsValuesOutsideDocumentedNumericBoundaries(
            String field,
            long minimum,
            long maximum) {
        assertThat(validator.validate(
                route("POST", Map.of(field, minimum - 1)),
                GatewayProtocol.HTTP,
                GatewayResponseMode.TRANSPARENT
        )).anySatisfy(issue -> {
            assertThat(issue.path()).isEqualTo("transportPolicy." + field);
            assertThat(issue.code())
                    .isEqualTo("TRANSPORT_VALUE_OUT_OF_RANGE");
        });
        assertThat(validator.validate(
                route("POST", Map.of(field, maximum + 1)),
                GatewayProtocol.HTTP,
                GatewayResponseMode.TRANSPARENT
        )).anySatisfy(issue -> {
            assertThat(issue.path()).isEqualTo("transportPolicy." + field);
            assertThat(issue.code())
                    .isEqualTo("TRANSPORT_VALUE_OUT_OF_RANGE");
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCombinations")
    void rejectsProtocolUnsafeTransportCombinations(
            String description,
            String method,
            GatewayProtocol protocol,
            GatewayResponseMode operationResponseMode,
            Map<String, Object> policy,
            String path,
            String code) {
        assertThat(validator.validate(
                route(method, policy),
                protocol,
                operationResponseMode
        )).anySatisfy(issue -> {
            assertThat(issue.path()).isEqualTo(path);
            assertThat(issue.code()).isEqualTo(code);
        });
    }

    @Test
    void rejectsNonBooleanPolicyFlagsAtTheirFieldPaths() {
        assertThat(validator.validate(
                route("POST", Map.of("retryEnabled", "false")),
                GatewayProtocol.HTTP,
                GatewayResponseMode.TRANSPARENT
        )).contains(new GatewayRouteTransportPolicyValidator.ValidationIssue(
                "transportPolicy.retryEnabled",
                "TRANSPORT_BOOLEAN_INVALID",
                "retryEnabled must be a boolean"
        ));
    }

    @Test
    void rejectsNonStringRouteTextFieldsWithoutStringifyingThem() {
        Map<String, Object> content = route("POST", Map.of());
        content.put("host", Map.of("tenant", "x"));
        content.put("httpMethod", false);

        assertThat(validator.validate(
                content,
                GatewayProtocol.HTTP,
                GatewayResponseMode.TRANSPARENT
        )).contains(
                new GatewayRouteTransportPolicyValidator.ValidationIssue(
                        "host",
                        "ROUTE_HOST_INVALID",
                        "Host must be a string"
                ),
                new GatewayRouteTransportPolicyValidator.ValidationIssue(
                        "httpMethod",
                        "ROUTE_METHOD_INVALID",
                        "HTTP Method must be a string"
                )
        );
    }

    @Test
    void acceptsWebSocketForATransparentHttpOperation() {
        assertThat(validator.validate(
                route("GET", Map.of(
                        "profile", "OPENAI_HTTP",
                        "transportProtocol", "WEBSOCKET"
                )),
                GatewayProtocol.HTTP,
                GatewayResponseMode.TRANSPARENT
        )).isEmpty();
    }

    private static Stream<Arguments> numericRanges() {
        return Stream.of(
                Arguments.of("maxRequestBodyBytes", 1L, 1_073_741_824L),
                Arguments.of("connectTimeoutMs", 100L, 60_000L),
                Arguments.of("responseHeaderTimeoutMs", 1_000L, 600_000L),
                Arguments.of("streamIdleTimeoutMs", 1_000L, 1_800_000L),
                Arguments.of("totalTimeoutMs", 1_000L, 7_200_000L),
                Arguments.of("websocketIdleTimeoutMs", 1_000L, 7_200_000L),
                Arguments.of("websocketMaxFrameBytes", 1_024L, 67_108_864L)
        );
    }

    private static Stream<Arguments> invalidCombinations() {
        return Stream.of(
                invalid(
                        "WebSocket requires GET",
                        "POST",
                        GatewayProtocol.HTTP,
                        GatewayResponseMode.TRANSPARENT,
                        Map.of("transportProtocol", "WEBSOCKET"),
                        "httpMethod",
                        "WEBSOCKET_GET_REQUIRED"
                ),
                invalid(
                        "RPC rejects WebSocket",
                        "GET",
                        GatewayProtocol.RPC,
                        GatewayResponseMode.TRANSPARENT,
                        Map.of("transportProtocol", "WEBSOCKET"),
                        "transportPolicy.transportProtocol",
                        "RPC_TRANSPORT_UNSUPPORTED"
                ),
                invalid(
                        "RPC rejects streaming request",
                        "POST",
                        GatewayProtocol.RPC,
                        GatewayResponseMode.TRANSPARENT,
                        Map.of("requestBodyMode", "STREAMING"),
                        "transportPolicy.requestBodyMode",
                        "RPC_TRANSPORT_UNSUPPORTED"
                ),
                invalid(
                        "RPC rejects auto stream",
                        "POST",
                        GatewayProtocol.RPC,
                        GatewayResponseMode.TRANSPARENT,
                        Map.of("responseMode", "AUTO_STREAM"),
                        "transportPolicy.responseMode",
                        "RPC_TRANSPORT_UNSUPPORTED"
                ),
                invalid(
                        "RPC rejects SSE",
                        "POST",
                        GatewayProtocol.RPC,
                        GatewayResponseMode.TRANSPARENT,
                        Map.of("responseMode", "SSE"),
                        "transportPolicy.responseMode",
                        "RPC_TRANSPORT_UNSUPPORTED"
                ),
                invalid(
                        "RPC rejects binary stream",
                        "POST",
                        GatewayProtocol.RPC,
                        GatewayResponseMode.TRANSPARENT,
                        Map.of("responseMode", "BINARY_STREAM"),
                        "transportPolicy.responseMode",
                        "RPC_TRANSPORT_UNSUPPORTED"
                ),
                invalid(
                        "RPC rejects OpenAI profile defaults",
                        "POST",
                        GatewayProtocol.RPC,
                        GatewayResponseMode.TRANSPARENT,
                        Map.of("profile", "OPENAI_HTTP"),
                        "transportPolicy.profile",
                        "RPC_TRANSPORT_UNSUPPORTED"
                ),
                invalid(
                        "wrapped rejects WebSocket",
                        "GET",
                        GatewayProtocol.HTTP,
                        GatewayResponseMode.WRAPPED,
                        Map.of("transportProtocol", "WEBSOCKET"),
                        "transportPolicy.transportProtocol",
                        "WRAPPED_TRANSPORT_UNSUPPORTED"
                ),
                invalid(
                        "wrapped rejects streaming request",
                        "POST",
                        GatewayProtocol.HTTP,
                        GatewayResponseMode.WRAPPED,
                        Map.of("requestBodyMode", "STREAMING"),
                        "transportPolicy.requestBodyMode",
                        "WRAPPED_TRANSPORT_UNSUPPORTED"
                ),
                invalid(
                        "wrapped rejects auto stream",
                        "POST",
                        GatewayProtocol.HTTP,
                        GatewayResponseMode.WRAPPED,
                        Map.of("responseMode", "AUTO_STREAM"),
                        "transportPolicy.responseMode",
                        "WRAPPED_TRANSPORT_UNSUPPORTED"
                ),
                invalid(
                        "wrapped rejects SSE aggregation",
                        "POST",
                        GatewayProtocol.HTTP,
                        GatewayResponseMode.WRAPPED,
                        Map.of("responseMode", "SSE"),
                        "transportPolicy.responseMode",
                        "WRAPPED_TRANSPORT_UNSUPPORTED"
                ),
                invalid(
                        "wrapped rejects binary aggregation",
                        "POST",
                        GatewayProtocol.HTTP,
                        GatewayResponseMode.WRAPPED,
                        Map.of("responseMode", "BINARY_STREAM"),
                        "transportPolicy.responseMode",
                        "WRAPPED_TRANSPORT_UNSUPPORTED"
                ),
                invalid(
                        "wrapped rejects OpenAI profile defaults",
                        "POST",
                        GatewayProtocol.HTTP,
                        GatewayResponseMode.WRAPPED,
                        Map.of("profile", "OPENAI_HTTP"),
                        "transportPolicy.profile",
                        "WRAPPED_TRANSPORT_UNSUPPORTED"
                )
        );
    }

    private static Arguments invalid(
            String description,
            String method,
            GatewayProtocol protocol,
            GatewayResponseMode operationResponseMode,
            Map<String, Object> policy,
            String path,
            String code) {
        return Arguments.of(
                description,
                method,
                protocol,
                operationResponseMode,
                policy,
                path,
                code
        );
    }

    private Map<String, Object> route(
            String method,
            Map<String, Object> policy) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("host", "ai.example.com");
        content.put("httpMethod", method);
        content.put("pathPattern", "/v1/**");
        content.put("accessZones", List.of("PUBLIC"));
        if (!policy.isEmpty()) {
            content.put("transportPolicy", policy);
        }
        return content;
    }
}
