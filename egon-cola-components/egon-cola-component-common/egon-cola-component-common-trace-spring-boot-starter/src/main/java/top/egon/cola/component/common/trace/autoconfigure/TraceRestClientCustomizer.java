package top.egon.cola.component.common.trace.autoconfigure;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import top.egon.cola.component.common.trace.TraceContext;

public class TraceRestClientCustomizer implements RestClientCustomizer {

    private final TraceProperties properties;

    public TraceRestClientCustomizer(TraceProperties properties) {
        this.properties = properties;
    }

    @Override
    public void customize(RestClient.Builder restClientBuilder) {
        restClientBuilder.requestInterceptor(interceptor());
    }

    ClientHttpRequestInterceptor interceptor() {
        return (request, body, execution) -> {
            if (properties.getPropagation().isEnabled()
                    && properties.getRestClient().isEnabled()) {
                TraceContext context = TraceHeaderSupport.outboundContext();
                TraceHeaderSupport.inject(
                        request.getHeaders(),
                        context,
                        properties.getRestClient()
                                .isTakeOverExistingTraceparent()
                );
            }
            return execution.execute(request, body);
        };
    }
}
