package top.egon.cola.component.gateway.core.exchange;

import top.egon.cola.component.gateway.contract.error.GatewayResult;

import java.util.Objects;

public record DefaultGatewayResponse(
        GatewayResult result,
        GatewayHeaders headers,
        GatewayBody body
) implements GatewayResponse {

    public DefaultGatewayResponse {
        result = Objects.requireNonNull(result, "result");
        headers = Objects.requireNonNull(headers, "headers");
        body = Objects.requireNonNull(body, "body");
    }

    public static DefaultGatewayResponse success(GatewayBody body) {
        return new DefaultGatewayResponse(
                GatewayResult.success(),
                ImmutableGatewayHeaders.empty(),
                body
        );
    }
}
