package top.egon.cola.component.common.trace.autoconfigure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import top.egon.cola.component.common.trace.TraceContext;
import top.egon.cola.component.common.trace.TraceKeys;
import top.egon.cola.component.common.trace.TraceScope;
import top.egon.cola.component.common.trace.TraceState;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TraceClientPropagationTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void restClientInterceptorPropagatesChildTraceWithoutEgonTraceId() throws IOException {
        TraceRestClientCustomizer customizer = new TraceRestClientCustomizer(new TraceProperties());
        TraceState parent = TraceState.root("request-1");
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.test"));
        ClientHttpRequestExecution execution = (httpRequest, body) -> response();

        try (TraceScope ignored = TraceContext.open(parent)) {
            customizer.interceptor().intercept(request, new byte[0], execution);
        }

        assertThat(request.getHeaders().getFirst(TraceKeys.TRACEPARENT_HEADER))
                .startsWith("00-" + parent.traceId() + "-");
        assertThat(request.getHeaders().getFirst(TraceKeys.REQUEST_ID_HEADER))
                .isEqualTo("request-1");
        assertThat(request.getHeaders().getFirst("x-egon-trace-id")).isNull();
    }

    @Test
    void webClientFilterReadsReactorContextBeforeMdc() {
        TraceWebClientCustomizer customizer = new TraceWebClientCustomizer(new TraceProperties());
        TraceState reactorState = TraceState.root("reactor-request");
        TraceState mdcState = TraceState.root("mdc-request");
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        ExchangeFunction exchange = request -> {
            captured.set(request);
            return Mono.just(org.springframework.web.reactive.function.client.ClientResponse
                    .create(HttpStatus.OK)
                    .build());
        };
        ClientRequest request = ClientRequest
                .create(HttpMethod.GET, URI.create("https://example.test"))
                .build();

        try (TraceScope ignored = TraceContext.open(mdcState)) {
            StepVerifier.create(customizer.filter().filter(request, exchange)
                            .contextWrite(context -> TraceReactorContext.put(context, reactorState)))
                    .expectNextMatches(response -> response.statusCode().is2xxSuccessful())
                    .verifyComplete();
        }

        HttpHeaders headers = captured.get().headers();
        assertThat(headers.getFirst(TraceKeys.TRACEPARENT_HEADER))
                .startsWith("00-" + reactorState.traceId() + "-");
        assertThat(headers.getFirst(TraceKeys.REQUEST_ID_HEADER))
                .isEqualTo("reactor-request");
        assertThat(headers.getFirst("x-egon-trace-id")).isNull();
    }

    private ClientHttpResponse response() {
        return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
    }
}
