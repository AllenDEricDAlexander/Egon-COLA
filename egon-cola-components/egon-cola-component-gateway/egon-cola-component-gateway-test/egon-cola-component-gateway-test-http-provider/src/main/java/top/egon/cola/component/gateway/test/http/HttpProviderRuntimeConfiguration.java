package top.egon.cola.component.gateway.test.http;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import top.egon.cola.component.ddc.registry.DdcServiceRegistryClient;
import top.egon.cola.component.gateway.provider.HttpProviderLeaseRuntime;
import top.egon.cola.component.gateway.provider.HttpProviderRuntimeProperties;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(DdcServiceRegistryClient.class)
public class HttpProviderRuntimeConfiguration {

    @Bean
    public HttpProviderLeaseRuntime httpProviderLeaseRuntime(
            DdcServiceRegistryClient registry,
            Environment environment) {
        String providerId = value(
                environment,
                "gateway.test.provider-id",
                "http-provider-default"
        );
        return new HttpProviderLeaseRuntime(
                registry,
                new HttpProviderRuntimeProperties(
                        true,
                        value(environment, "gateway.test.env", "test"),
                        value(
                                environment,
                                "gateway.test.namespace",
                                "gateway-test"
                        ),
                        providerId,
                        "gateway-test-http-provider",
                        "default",
                        "1.0.0",
                        "http",
                        value(
                                environment,
                                "gateway.test.advertised-host",
                                "127.0.0.1"
                        ),
                        environment.getProperty(
                                "gateway.test.advertised-port",
                                Integer.class,
                                0
                        ),
                        30,
                        10,
                        true,
                        Map.of(
                                "gateway.zone",
                                value(
                                        environment,
                                        "gateway.test.zone",
                                        "zone-a"
                                ),
                                "gateway.weight",
                                value(
                                        environment,
                                        "gateway.test.weight",
                                        "100"
                                ),
                                "gateway.definition-set",
                                value(
                                        environment,
                                        "gateway.test.definition-set",
                                        "pending-report"
                                )
                        ),
                        null
                )
        );
    }

    @Bean
    public ApplicationListener<WebServerInitializedEvent>
    httpProviderServerReadyListener(HttpProviderLeaseRuntime runtime) {
        return event -> runtime.onHttpServerReady(
                event.getWebServer().getPort()
        );
    }

    private String value(
            Environment environment,
            String key,
            String fallback) {
        return environment.getProperty(key, fallback);
    }
}
