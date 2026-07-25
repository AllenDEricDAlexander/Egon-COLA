package top.egon.cola.component.gateway.core.filter;

import org.reactivestreams.Publisher;
import top.egon.cola.component.gateway.core.exchange.GatewayExchange;
import top.egon.cola.component.gateway.core.exchange.GatewayResponse;
import top.egon.cola.component.gateway.core.reactive.GatewayPublishers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class DefaultGatewayFilterChain implements GatewayFilterChain {

    private final List<GatewayFilter> filters;

    private final int offset;

    public DefaultGatewayFilterChain(List<GatewayFilter> filters) {
        this(validateAndSort(filters), 0);
    }

    private DefaultGatewayFilterChain(List<GatewayFilter> filters, int offset) {
        this.filters = filters;
        this.offset = offset;
    }

    @Override
    public Publisher<GatewayResponse> filter(GatewayExchange exchange) {
        Objects.requireNonNull(exchange, "exchange");
        if (offset >= filters.size()) {
            return GatewayPublishers.just(exchange.response());
        }
        GatewayFilter current = filters.get(offset);
        DefaultGatewayFilterChain next = new DefaultGatewayFilterChain(
                filters,
                offset + 1
        );
        return GatewayPublishers.defer(
                () -> current.filter(exchange, next)
        );
    }

    private static List<GatewayFilter> validateAndSort(
            List<GatewayFilter> source) {
        List<GatewayFilter> sorted = new ArrayList<>(
                Objects.requireNonNull(source, "filters")
        );
        Set<String> ids = new HashSet<>();
        Set<String> positions = new HashSet<>();
        for (GatewayFilter filter : sorted) {
            Objects.requireNonNull(filter, "filter");
            if (filter.id() == null || filter.id().isBlank()
                    || !ids.add(filter.id())) {
                throw new IllegalArgumentException(
                        "filter id must be unique and non-blank"
                );
            }
            Objects.requireNonNull(filter.stage(), "filter stage");
            String position = filter.stage() + ":" + filter.order();
            if (!positions.add(position)) {
                throw new IllegalArgumentException(
                        "duplicate filter order at " + position
                );
            }
        }
        sorted.sort(Comparator
                .comparingInt((GatewayFilter filter) ->
                        filter.stage().order())
                .thenComparingInt(GatewayFilter::order));
        return List.copyOf(sorted);
    }
}
