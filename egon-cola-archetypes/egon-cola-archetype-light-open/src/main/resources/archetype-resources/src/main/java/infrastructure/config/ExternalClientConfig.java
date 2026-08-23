package ${package}.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.integrations.external-http.enabled", havingValue = "true")
public class ExternalClientConfig {
    @Bean("userRestClient")
    RestClient userRestClient(
            RestClient.Builder builder,
            @Value("${symbol_dollar}{app.integrations.external-http.user-base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean("teachingRestClient")
    RestClient teachingRestClient(
            RestClient.Builder builder,
            @Value("${symbol_dollar}{app.integrations.external-http.teaching-base-url}") String baseUrl) {
        return builder.clone().baseUrl(baseUrl).build();
    }
}
