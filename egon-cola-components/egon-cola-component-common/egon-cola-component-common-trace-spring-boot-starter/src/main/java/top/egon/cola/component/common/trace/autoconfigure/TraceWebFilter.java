package top.egon.cola.component.common.trace.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.TraceKeys;
import top.egon.cola.component.common.trace.TracePropagation;
import top.egon.cola.component.common.trace.TraceScope;
import top.egon.cola.component.common.trace.TraceState;

public class TraceWebFilter implements WebFilter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TraceWebFilter.class);

    private final TraceProperties properties;

    public TraceWebFilter(TraceProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             WebFilterChain chain) {
        String path = exchange.getRequest().getPath()
                .pathWithinApplication()
                .value();
        if (properties.getWebflux().getExcludedPaths().stream()
                .anyMatch(pattern -> PatternMatchUtils.simpleMatch(pattern, path))) {
            return chain.filter(exchange);
        }
        long startedAt = System.nanoTime();
        TracePropagation.Extracted extracted = TraceHeaderSupport.extract(
                exchange.getRequest().getHeaders(),
                properties
        );
        TraceState state = extracted.state();
        if (properties.getPropagation().isResponseHeaders()
                && properties.getWebflux().isResponseHeaders()) {
            exchange.getResponse().getHeaders()
                    .set(TraceKeys.TRACEPARENT_HEADER, state.traceparent());
            if (state.requestId() != null) {
                exchange.getResponse().getHeaders()
                        .set(TraceKeys.REQUEST_ID_HEADER, state.requestId());
            }
        }
        return Mono.using(
                        () -> TraceContext.open(state),
                        ignored -> chain.filter(exchange)
                                .contextWrite(context -> TraceReactorContext.put(context, state)),
                        TraceScope::close
                )
                .doFinally(signalType -> {
                    if (properties.getWebflux().isAccessLog()) {
                        logAccess(exchange, state, startedAt);
                    }
                });
    }

    private void logAccess(ServerWebExchange exchange,
                           TraceState state,
                           long startedAt) {
        long costMs = (System.nanoTime() - startedAt) / 1_000_000L;
        String path = exchange.getRequest().getPath()
                .pathWithinApplication()
                .value();
        if (properties.getWebflux().isRecordQuery()
                && exchange.getRequest().getURI().getRawQuery() != null) {
            path = path + "?" + exchange.getRequest().getURI().getRawQuery();
        }
        if (properties.getWebflux().isRecordHeaders()
                || properties.getWebflux().isRecordRequestBody()
                || properties.getWebflux().isRecordResponseBody()) {
            LOGGER.debug(
                    "trace access verbose logging is disabled by default "
                            + "unless application code supplies a safe logger"
            );
        }
        String responseBytes = exchange.getResponse().getHeaders()
                .getFirst(HttpHeaders.CONTENT_LENGTH);
        LOGGER.info(
                "trace_access protocol=HTTP method={} path={} status={} cost_ms={} "
                        + "traceId={} spanId={} requestId={} clientIp={} "
                        + "errorCode={} responseBytes={}",
                exchange.getRequest().getMethod(),
                path,
                exchange.getResponse().getStatusCode(),
                costMs,
                state.traceId(),
                state.spanId(),
                state.requestId(),
                clientIp(exchange),
                null,
                responseBytes
        );
    }

    private String clientIp(ServerWebExchange exchange) {
        if (!properties.getWebflux().isTrustedProxyHeaders()) {
            return exchange.getRequest().getRemoteAddress() == null
                    ? null
                    : exchange.getRequest().getRemoteAddress()
                    .getAddress()
                    .getHostAddress();
        }
        String value = exchange.getRequest().getHeaders()
                .getFirst("X-Forwarded-For");
        if (value == null || value.isBlank()) {
            return null;
        }
        int comma = value.indexOf(',');
        return comma < 0 ? value.trim() : value.substring(0, comma).trim();
    }
}
