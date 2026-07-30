package top.egon.cola.component.gateway.engine.http.proxy;

import top.egon.cola.component.gateway.contract.rule.GatewayTransportResponseMode;
import top.egon.cola.component.gateway.engine.http.GatewayBodySizeLimiter;
import top.egon.cola.component.gateway.engine.http.GatewayHttpFlushMode;
import top.egon.cola.component.gateway.engine.http.GatewayOutboundHttpResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class GatewayHttpResponseSemantics {

    private final GatewayBodySizeLimiter limiter;

    GatewayHttpResponseSemantics(GatewayBodySizeLimiter limiter) {
        this.limiter = limiter;
    }

    GatewayOutboundHttpResponse apply(
            GatewayOutboundHttpResponse response,
            GatewayHttpProxyContext context) {
        GatewayOutboundHttpResponse limited = context.policy()
                .maxResponseBodyBytes()
                .isPresent()
                ? limiter.limitResponse(
                        response,
                        context.policy().maxResponseBodyBytes().getAsLong()
                )
                : response;
        GatewayTransportResponseMode mode = context.policy().responseMode();
        boolean sse = mode == GatewayTransportResponseMode.SSE
                || (mode == GatewayTransportResponseMode.AUTO_STREAM
                && eventStream(limited.headers()));
        if (!sse) {
            return limited;
        }
        Map<String, List<String>> headers = new LinkedHashMap<>();
        limited.headers().forEach((name, values) -> {
            if (!"content-length".equalsIgnoreCase(name)) {
                headers.put(name.toLowerCase(Locale.ROOT), values);
            }
        });
        headers.put("cache-control", List.of("no-cache, no-transform"));
        headers.put("x-accel-buffering", List.of("no"));
        return limited.withHeadersAndBody(headers, limited.body())
                .withFlushMode(GatewayHttpFlushMode.PER_BUFFER);
    }

    private boolean eventStream(Map<String, List<String>> headers) {
        return headers.entrySet().stream()
                .filter(entry -> "content-type".equalsIgnoreCase(
                        entry.getKey()
                ))
                .flatMap(entry -> entry.getValue().stream())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.startsWith("text/event-stream"));
    }
}
