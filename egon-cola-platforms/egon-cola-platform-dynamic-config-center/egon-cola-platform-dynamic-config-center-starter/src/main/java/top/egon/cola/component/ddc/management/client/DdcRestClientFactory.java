package top.egon.cola.component.ddc.management.client;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 创建使用 JDK HTTP 传输的 DDC {@link RestClient} 构建器。 /
 * Creates DDC {@link RestClient} builders backed by the JDK HTTP transport.
 */
public final class DdcRestClientFactory {

    /**
     * 禁止实例化纯静态工厂。 / Prevents instantiation of this static factory.
     */
    private DdcRestClientFactory() {
    }

    /**
     * 创建禁用重定向且应用超时与可选 mTLS 的 REST 客户端构建器。 /
     * Creates a REST client builder with redirects disabled, timeouts applied, and optional mTLS.
     *
     * @param connectTimeout    建立连接的超时时间 / connection timeout
     * @param readTimeout       读取响应的超时时间 / response-read timeout
     * @param transportSecurity 明文开发模式或 mTLS 传输配置 / plaintext-development or mTLS transport configuration
     * @return 已配置但尚未构建的 REST 客户端构建器 / configured but unbuilt REST client builder
     */
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
