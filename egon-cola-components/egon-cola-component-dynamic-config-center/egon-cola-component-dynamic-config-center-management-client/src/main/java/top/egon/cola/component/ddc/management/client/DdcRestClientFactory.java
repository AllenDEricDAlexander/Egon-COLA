package top.egon.cola.component.ddc.management.client;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

public final class DdcRestClientFactory {

    private DdcRestClientFactory() {
    }

    public static RestClient.Builder create(
            Duration connectTimeout,
            Duration readTimeout,
            DdcClientTransportSecurity transportSecurity) {
        HttpClient.Builder httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER);
        if (transportSecurity.enabled()) {
            httpClient.sslContext(transportSecurity.sslContext());
        }
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient.build());
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
