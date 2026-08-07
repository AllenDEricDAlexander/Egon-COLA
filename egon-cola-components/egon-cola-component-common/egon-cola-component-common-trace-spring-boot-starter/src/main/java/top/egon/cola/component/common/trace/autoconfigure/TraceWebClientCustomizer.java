package top.egon.cola.component.common.trace.autoconfigure;

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import top.egon.cola.component.common.trace.TraceContext;

public class TraceWebClientCustomizer implements WebClientCustomizer {

    private final TraceProperties properties;

    public TraceWebClientCustomizer(TraceProperties properties) {
        this.properties = properties;
    }

    @Override
    public void customize(WebClient.Builder webClientBuilder) {
        webClientBuilder.filter(filter());
    }

    ExchangeFilterFunction filter() {
        return (request, next) -> Mono.deferContextual(contextView -> {
            if (!properties.getPropagation().isEnabled()
                    || !properties.getWebClient().isEnabled()) {
                return next.exchange(request);
            }
            TraceContext parent = contextView.getOrDefault(
                    TraceContext.class,
                    null
            );
            if (parent == null) {
                parent = TraceContext.currentOrCreate();
            }
            TraceContext child = parent.child();
            ClientRequest.Builder builder = ClientRequest.from(request);
            builder.headers(headers -> TraceHeaderSupport.inject(
                    headers,
                    child,
                    properties.getWebClient()
                            .isTakeOverExistingTraceparent()
            ));
            return next.exchange(builder.build());
        });
    }
}
