package top.egon.cola.component.gateway.test.rpc.provider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import top.egon.cola.component.rpc.provider.metadata.RpcProviderMetadataContributor;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class RpcProviderMetadataConfiguration {

    @Bean
    public RpcProviderMetadataContributor gatewayTestMetadata(
            Environment environment) {
        return ignored -> Map.of(
                "gateway.zone",
                environment.getProperty("gateway.test.zone", "zone-a"),
                "gateway.weight",
                environment.getProperty("gateway.test.weight", "100")
        );
    }
}
