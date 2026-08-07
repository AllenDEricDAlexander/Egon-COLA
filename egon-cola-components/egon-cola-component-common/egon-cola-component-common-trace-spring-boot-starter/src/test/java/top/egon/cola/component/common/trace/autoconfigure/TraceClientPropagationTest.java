package top.egon.cola.component.common.trace.autoconfigure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import top.egon.cola.component.common.trace.TraceContext;

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
    void restClientInterceptorPropagatesChildContext() throws IOException {
        TraceRestClientCustomizer customizer =
                new TraceRestClientCustomizer(new TraceProperties());
        TraceContext parent = TraceContext.root("request-1");
        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.GET,
                URI.create("https://example.test")
        );
        ClientHttpRequestExecution execution =
                (httpRequest, body) -> response();

        try (TraceContext.Scope ignored = parent.open()) {
            customizer.interceptor().intercept(
                    request,
                    new byte[0],
                    execution
            );
        }

        assertThat(request.getHeaders().getFirst(
                TraceContext.TRACEPARENT_HEADER
        )).startsWith("00-" + parent.traceId() + "-");
        assertThat(request.getHeaders().getFirst(
                TraceContext.REQUEST_ID_HEADER
        )).isEqualTo("request-1");
        assertThat(request.getHeaders().getFirst(
                TraceContext.LEGACY_TRACE_ID_HEADER
        )).isNull();
    }

    @Test
    void restClientInterceptorKeepsValidExistingTraceparent() throws IOException {
        TraceRestClientCustomizer customizer =
                new TraceRestClientCustomizer(new TraceProperties());
        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.GET,
                URI.create("https://example.test")
        );
        String existing = "00-4bf92f3577b34da6a3ce929d0e0e4736-"
                + "00f067aa0ba902b7-01";
        request.getHeaders().set(TraceContext.TRACEPARENT_HEADER, existing);

        customizer.interceptor().intercept(
                request,
                new byte[0],
                (httpRequest, body) -> response()
        );

        assertThat(request.getHeaders().getFirst(
                TraceContext.TRACEPARENT_HEADER
        )).isEqualTo(existing);
    }

    @Test
    void webClientFilterReadsReactorContextBeforeThreadMdc() {
        TraceWebClientCustomizer customizer =
                new TraceWebClientCustomizer(new TraceProperties());
        TraceContext reactorContext = TraceContext.root("reactor-request");
        TraceContext threadContext = TraceContext.root("thread-request");
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

        try (TraceContext.Scope ignored = threadContext.open()) {
            StepVerifier.create(customizer.filter().filter(request, exchange)
                            .contextWrite(context -> context.put(
                                    TraceContext.class,
                                    reactorContext
                            )))
                    .expectNextMatches(response ->
                            response.statusCode().is2xxSuccessful())
                    .verifyComplete();
        }

        HttpHeaders headers = captured.get().headers();
        assertThat(headers.getFirst(TraceContext.TRACEPARENT_HEADER))
                .startsWith("00-" + reactorContext.traceId() + "-");
        assertThat(headers.getFirst(TraceContext.REQUEST_ID_HEADER))
                .isEqualTo("reactor-request");
    }

    private ClientHttpResponse response() {
        return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
    }
}
